package com.stockpulse.service.advisor;

import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.SuggestionDirection;
import com.stockpulse.domain.SuggestionStatus;
import com.stockpulse.domain.TriggerReason;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component("ruleBasedCommerceAdvisor")
public class RuleBasedCommerceAdvisor implements CommerceAdvisor {

    @Override
    public String getName() {
        return "rule-based";
    }

    @Override
    public PricingSuggestion suggestPricing(Product product, TriggerReason triggerReason) {
        BigDecimal recommendedPrice = product.getCurrentPrice();
        SuggestionDirection direction = SuggestionDirection.HOLD;
        String reasoning = "Stock and demand are within normal parameters.";

        if (product.getStockLevel() < product.getReorderThreshold()) {
            // Increase by 10%
            recommendedPrice = product.getCurrentPrice().multiply(BigDecimal.valueOf(1.10));
            direction = SuggestionDirection.INCREASE;
            reasoning = "Stock is below reorder threshold. Increasing price to protect inventory.";
        } else if (product.getDemandVelocity() > 10) { // arbitrary 2x cat avg for now
            // Increase by 5%
            recommendedPrice = product.getCurrentPrice().multiply(BigDecimal.valueOf(1.05));
            direction = SuggestionDirection.INCREASE;
            reasoning = "Demand velocity is spiking. Modest price increase to capitalize.";
        }

        return PricingSuggestion.builder()
                .product(product)
                .currentPrice(product.getCurrentPrice())
                .recommendedPrice(recommendedPrice)
                .direction(direction)
                .confidence(1.0)
                .reasoning(reasoning)
                .status(SuggestionStatus.PENDING)
                .triggerReason(triggerReason)
                .build();
    }

    @Override
    public ReorderSuggestion suggestReorder(Product product, TriggerReason triggerReason) {
        int recommendedQuantity = (product.getReorderThreshold() * 3) - product.getStockLevel();
        if (recommendedQuantity < 1) {
            recommendedQuantity = 1;
        }

        return ReorderSuggestion.builder()
                .product(product)
                .currentStock(product.getStockLevel())
                .recommendedQuantity(recommendedQuantity)
                .suggestedLeadTimeDays(7)
                .confidence(1.0)
                .reasoning("Rule-based replenishment based on threshold.")
                .status(SuggestionStatus.PENDING)
                .triggerReason(triggerReason)
                .build();
    }
}
