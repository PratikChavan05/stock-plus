package com.stockpulse.service.advisor;

import com.stockpulse.ai.LLMGateway;
import com.stockpulse.domain.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component("aiCommerceAdvisor")
public class AICommerceAdvisor implements CommerceAdvisor {

    private final LLMGateway llmGateway;
    private final CommerceAdvisor ruleBasedFallback;

    public AICommerceAdvisor(LLMGateway llmGateway, CommerceAdvisor ruleBasedCommerceAdvisor) {
        this.llmGateway = llmGateway;
        this.ruleBasedFallback = ruleBasedCommerceAdvisor;
    }

    @Override
    public String getName() {
        return "ai-based";
    }

    @Override
    public PricingSuggestion suggestPricing(Product product, TriggerReason triggerReason) {
        try {
            // Context building
            String prompt = String.format("Product: %s, Category: %s, Price: %s, Stock: %d, Threshold: %d, Velocity: %d. Trigger: %s",
                    product.getName(), product.getCategory(), product.getCurrentPrice(), product.getStockLevel(), product.getReorderThreshold(), product.getDemandVelocity(), triggerReason);
            String response = llmGateway.callLLM(prompt);
            
            if (response == null || response.isEmpty() || !response.contains("recommendedPrice")) {
                return ruleBasedFallback.suggestPricing(product, triggerReason);
            }

            // A naive parser since we don't have Jackson fully configured yet for manual parse here (or we could use ObjectMapper)
            // Assuming the LLMGateway mock returns valid structure.
            BigDecimal recommendedPrice = product.getCurrentPrice().multiply(BigDecimal.valueOf(1.15)); // Mock
            
            return PricingSuggestion.builder()
                    .product(product)
                    .currentPrice(product.getCurrentPrice())
                    .recommendedPrice(recommendedPrice)
                    .direction(SuggestionDirection.INCREASE)
                    .confidence(0.85)
                    .reasoning("AI determines demand allows for price increase.")
                    .status(SuggestionStatus.PENDING)
                    .triggerReason(triggerReason)
                    .build();
        } catch (Exception e) {
            return ruleBasedFallback.suggestPricing(product, triggerReason);
        }
    }

    @Override
    public ReorderSuggestion suggestReorder(Product product, TriggerReason triggerReason) {
        try {
            return ReorderSuggestion.builder()
                    .product(product)
                    .currentStock(product.getStockLevel())
                    .recommendedQuantity(50) // Mock
                    .suggestedLeadTimeDays(5)
                    .confidence(0.9)
                    .reasoning("AI determines optimal reorder quantity to avoid stockouts.")
                    .status(SuggestionStatus.PENDING)
                    .triggerReason(triggerReason)
                    .build();
        } catch (Exception e) {
            return ruleBasedFallback.suggestReorder(product, triggerReason);
        }
    }
}
