package com.stockpulse.service.advisor;

import com.stockpulse.domain.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("ruleBasedCommerceAdvisor")
public class RuleBasedCommerceAdvisor implements CommerceAdvisor {

    private final double demandIncreaseMultiplier;

    public RuleBasedCommerceAdvisor(
            @Value("${commerce.demand-increase-multiplier:2.0}") double demandIncreaseMultiplier) {
        this.demandIncreaseMultiplier = demandIncreaseMultiplier;
    }

    @Override
    public String getName() {
        return "rule-based";
    }

    @Override
    public PricingSuggestion suggestPricing(CommerceContext context) {
        Product product = context.product();
        BigDecimal recommended = product.getCurrentPrice();
        SuggestionDirection direction = SuggestionDirection.HOLD;
        String reasoning = "Stock and demand are within normal parameters versus category peers.";

        if (context.stockBelowThreshold()) {
            recommended = scale(product.getCurrentPrice(), 1.10);
            direction = SuggestionDirection.INCREASE;
            reasoning = "Rule: stock " + product.getStockLevel() + " is below reorder threshold "
                    + product.getReorderThreshold() + ". Recommend a 10% increase to protect remaining inventory.";
        } else if (product.getDemandVelocity() > demandIncreaseMultiplier * Math.max(context.categoryAverageVelocity(), 0.1)) {
            recommended = scale(product.getCurrentPrice(), 1.05);
            direction = SuggestionDirection.INCREASE;
            reasoning = "Rule: demand velocity " + product.getDemandVelocity()
                    + " exceeds " + demandIncreaseMultiplier + "× category average "
                    + round(context.categoryAverageVelocity()) + ". Recommend a 5% increase.";
        }

        return PricingSuggestion.builder()
                .product(product)
                .currentPrice(product.getCurrentPrice())
                .recommendedPrice(recommended)
                .direction(direction)
                .confidence(1.0)
                .reasoning(reasoning)
                .status(SuggestionStatus.PENDING)
                .triggerReason(context.triggerReason())
                .build();
    }

    @Override
    public ReorderSuggestion suggestReorder(CommerceContext context) {
        Product product = context.product();
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
                .reasoning("Rule: replenish to 3× threshold. Quantity = (threshold × 3) − stock = "
                        + recommendedQuantity + ".")
                .status(SuggestionStatus.PENDING)
                .triggerReason(context.triggerReason())
                .build();
    }

    private static BigDecimal scale(BigDecimal price, double factor) {
        return price.multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP);
    }

    private static String round(double value) {
        return String.format("%.2f", value);
    }
}
