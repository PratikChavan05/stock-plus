package com.stockpulse.service;

import com.stockpulse.domain.*;
import com.stockpulse.event.DemandSpikeEvent;
import com.stockpulse.event.InventoryLowEvent;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import com.stockpulse.service.advisor.CommerceAdvisor;
import com.stockpulse.service.advisor.CommerceAdvisorRegistry;
import com.stockpulse.service.advisor.CommerceContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class AgenticRecommendationLoop {

    private static final Logger log = LoggerFactory.getLogger(AgenticRecommendationLoop.class);

    private final ProductRepository productRepository;
    private final PricingSuggestionRepository pricingRepo;
    private final ReorderSuggestionRepository reorderRepo;
    private final CommerceAdvisorRegistry advisorRegistry;
    private final CommerceContextFactory contextFactory;

    @Async
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleInventoryLow(InventoryLowEvent event) {
        processSignal(event.getProductId(), TriggerReason.INVENTORY_LOW);
    }

    @Async
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleDemandSpike(DemandSpikeEvent event) {
        processSignal(event.getProductId(), TriggerReason.DEMAND_SPIKE);
    }

    @Transactional
    public PricingSuggestion requestPricing(Product product, TriggerReason reason) {
        CommerceContext ctx = contextFactory.forProduct(product, reason);
        PricingSuggestion existing = pricingRepo.findFirstByProductIdAndStatusAndTriggerReason(
                product.getId(), SuggestionStatus.PENDING, reason).orElse(null);
        if (existing != null) {
            return existing;
        }
        return persistPricing(ctx);
    }

    @Transactional
    public ReorderSuggestion requestReorder(Product product, TriggerReason reason) {
        CommerceContext ctx = contextFactory.forProduct(product, reason);
        ReorderSuggestion existing = reorderRepo.findFirstByProductIdAndStatusAndTriggerReason(
                product.getId(), SuggestionStatus.PENDING, reason).orElse(null);
        if (existing != null) {
            return existing;
        }
        return persistReorder(ctx);
    }

    @Transactional
    protected void processSignal(String productId, TriggerReason reason) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return;
        }
        CommerceContext ctx = contextFactory.forProduct(product, reason);
        CommerceAdvisor advisor = advisorRegistry.getActiveAdvisor();
        log.info("Agentic loop {} on {} via {}", reason, productId, advisor.getName());

        persistPricingIfNew(product, reason, ctx);
        persistReorderIfNew(product, reason, ctx);

        if (product.getStockLevel() == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        } else {
            product.setStatus(ProductStatus.PRICE_REVIEW_PENDING);
        }
        productRepository.save(product);
    }

    private void persistPricingIfNew(Product product, TriggerReason reason, CommerceContext ctx) {
        if (pricingRepo.findFirstByProductIdAndStatusAndTriggerReason(
                product.getId(), SuggestionStatus.PENDING, reason).isPresent()) {
            return;
        }
        persistPricing(ctx);
    }

    private void persistReorderIfNew(Product product, TriggerReason reason, CommerceContext ctx) {
        if (reorderRepo.findFirstByProductIdAndStatusAndTriggerReason(
                product.getId(), SuggestionStatus.PENDING, reason).isPresent()) {
            return;
        }
        persistReorder(ctx);
    }

    private PricingSuggestion persistPricing(CommerceContext ctx) {
        PricingSuggestion suggestion = advisorRegistry.getActiveAdvisor().suggestPricing(ctx);
        if (suggestion == null) {
            suggestion = advisorRegistry.require("rule-based").suggestPricing(ctx);
        }
        return pricingRepo.save(suggestion);
    }

    private ReorderSuggestion persistReorder(CommerceContext ctx) {
        ReorderSuggestion suggestion = advisorRegistry.getActiveAdvisor().suggestReorder(ctx);
        if (suggestion == null) {
            suggestion = advisorRegistry.require("rule-based").suggestReorder(ctx);
        }
        return reorderRepo.save(suggestion);
    }
}
