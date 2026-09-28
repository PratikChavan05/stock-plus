package com.stockpulse.service;

import com.stockpulse.domain.*;
import com.stockpulse.event.DemandSpikeEvent;
import com.stockpulse.event.InventoryLowEvent;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import com.stockpulse.service.advisor.CommerceAdvisor;
import com.stockpulse.service.advisor.CommerceAdvisorRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AgenticRecommendationLoop {

    private final ProductRepository productRepository;
    private final PricingSuggestionRepository pricingRepo;
    private final ReorderSuggestionRepository reorderRepo;
    private final CommerceAdvisorRegistry advisorRegistry;

    @Async
    @EventListener
    public void handleInventoryLow(InventoryLowEvent event) {
        processSignal(event.getProductId(), TriggerReason.INVENTORY_LOW);
    }

    @Async
    @EventListener
    public void handleDemandSpike(DemandSpikeEvent event) {
        processSignal(event.getProductId(), TriggerReason.DEMAND_SPIKE);
    }

    private void processSignal(String productId, TriggerReason reason) {
        Optional<Product> optProduct = productRepository.findById(productId);
        if (optProduct.isEmpty()) return;

        Product product = optProduct.get();
        CommerceAdvisor advisor = advisorRegistry.getActiveAdvisor();

        // 1. Check if pending suggestions already exist to maintain idempotency
        boolean pricingPending = pricingRepo.findFirstByProductIdAndStatusAndTriggerReason(
                productId, SuggestionStatus.PENDING, reason).isPresent();
        
        if (!pricingPending) {
            PricingSuggestion pSuggestion = advisor.suggestPricing(product, reason);
            if (pSuggestion != null) {
                pricingRepo.save(pSuggestion);
            }
        }

        boolean reorderPending = reorderRepo.findFirstByProductIdAndStatusAndTriggerReason(
                productId, SuggestionStatus.PENDING, reason).isPresent();

        if (!reorderPending) {
            ReorderSuggestion rSuggestion = advisor.suggestReorder(product, reason);
            if (rSuggestion != null) {
                reorderRepo.save(rSuggestion);
            }
        }
        
        // Update product status
        product.setStatus(ProductStatus.PRICE_REVIEW_PENDING);
        productRepository.save(product);
    }
}
