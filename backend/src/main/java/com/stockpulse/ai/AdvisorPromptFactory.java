package com.stockpulse.ai;

import com.stockpulse.service.advisor.CommerceContext;
import org.springframework.stereotype.Component;

/**
 * Two merchandising situations, two prompts. Trigger is not a field tacked onto one template.
 */
@Component
public class AdvisorPromptFactory {

    public String pricingPrompt(CommerceContext ctx) {
        return switch (ctx.triggerReason()) {
            case INVENTORY_LOW -> inventoryLowPricingPrompt(ctx);
            case DEMAND_SPIKE -> demandSpikePricingPrompt(ctx);
            default -> manualPricingPrompt(ctx);
        };
    }

    public String reorderPrompt(CommerceContext ctx) {
        return switch (ctx.triggerReason()) {
            case INVENTORY_LOW -> inventoryLowReorderPrompt(ctx);
            case DEMAND_SPIKE -> demandSpikeReorderPrompt(ctx);
            default -> manualReorderPrompt(ctx);
        };
    }

    private String productBlock(CommerceContext ctx) {
        var p = ctx.product();
        String margin = "";
        if (p.getCostPrice() != null) {
            margin = "\nCost price (sprint-2 placeholder, do not violate a healthy margin): " + p.getCostPrice();
        }
        return """
                Product: %s
                SKU: %s
                Category: %s
                Current price: %s
                Stock level: %d
                Reorder threshold: %d
                Demand velocity (orders last 24h): %d
                Category average velocity: %.2f
                Velocity vs peers: %.2fx%s
                """.formatted(
                p.getName(),
                p.getSku(),
                p.getCategory(),
                p.getCurrentPrice(),
                p.getStockLevel(),
                p.getReorderThreshold(),
                p.getDemandVelocity(),
                ctx.categoryAverageVelocity(),
                ratio(p.getDemandVelocity(), ctx.categoryAverageVelocity()),
                margin
        );
    }

    private String inventoryLowPricingPrompt(CommerceContext ctx) {
        return """
                You are a merchandising advisor for ShopStream. Inventory just crossed below the reorder threshold.
                This is NOT a demand-spike situation. Low stock is an ambiguous call:

                - RAISE price to protect remaining units and slow sell-through while replenishment is in flight.
                - Or CLEARANCE (decrease) if the item looks like dead stock that should be moved before it ages out.
                - HOLD only if the current price already balances both risks.

                Explain the tradeoff in reasoning. Do not just output a number.

                %s
                Trigger: INVENTORY_LOW (stock %d is below threshold %d).

                Reply JSON only:
                {"recommendedPrice":29.99,"direction":"INCREASE|DECREASE|HOLD","confidence":0.82,"reasoning":"..."}
                Constraints: recommendedPrice must be a positive number. direction must be INCREASE, DECREASE, or HOLD.
                """.formatted(
                productBlock(ctx),
                ctx.product().getStockLevel(),
                ctx.product().getReorderThreshold()
        );
    }

    private String demandSpikePricingPrompt(CommerceContext ctx) {
        return """
                You are a merchandising advisor for ShopStream. Demand velocity just spiked versus category peers.
                This is NOT a low-stock clearance decision. The merchandising question is how to capitalize:

                - A modest INCREASE captures willingness to pay on a trending SKU without shocking customers.
                - HOLD if the spike looks noisy or the item is already premium versus category.
                - DECREASE is rarely right on a spike unless you are fueling a campaign; say so if you choose it.

                Include velocity vs category average in your reasoning.

                %s
                Trigger: DEMAND_SPIKE (velocity %d vs category average %.2f).

                Reply JSON only:
                {"recommendedPrice":29.99,"direction":"INCREASE|DECREASE|HOLD","confidence":0.82,"reasoning":"..."}
                Constraints: recommendedPrice must be a positive number. direction must be INCREASE, DECREASE, or HOLD.
                """.formatted(
                productBlock(ctx),
                ctx.product().getDemandVelocity(),
                ctx.categoryAverageVelocity()
        );
    }

    private String manualPricingPrompt(CommerceContext ctx) {
        return """
                You are a merchandising advisor. A merchandiser requested an on-demand pricing review.
                Weigh stock health and demand versus category peers. Recommend INCREASE, DECREASE, or HOLD.

                %s
                Trigger: %s (manual / on-demand).

                Reply JSON only:
                {"recommendedPrice":29.99,"direction":"INCREASE|DECREASE|HOLD","confidence":0.82,"reasoning":"..."}
                """.formatted(productBlock(ctx), ctx.triggerReason());
    }

    private String inventoryLowReorderPrompt(CommerceContext ctx) {
        var p = ctx.product();
        int baseline = Math.max(1, (p.getReorderThreshold() * 3) - p.getStockLevel());
        return """
                You are a replenishment advisor. Stock is below the reorder threshold.
                Recommend inbound quantity to cover typical demand until the next receipt.
                A simple baseline is (threshold × 3) − current stock = %d. Improve on it using velocity, but stay realistic.

                %s
                Trigger: INVENTORY_LOW.

                Reply JSON only:
                {"recommendedQuantity":150,"confidence":0.78,"reasoning":"...","suggestedLeadTimeDays":7}
                Constraints: recommendedQuantity must be a positive integer.
                """.formatted(baseline, productBlock(ctx));
    }

    private String demandSpikeReorderPrompt(CommerceContext ctx) {
        return """
                You are a replenishment advisor. Demand has spiked versus category peers.
                Size inbound quantity for a hotter sell-through than usual — more than a quiet restock, not an infinite warehouse fill.

                %s
                Trigger: DEMAND_SPIKE.

                Reply JSON only:
                {"recommendedQuantity":150,"confidence":0.78,"reasoning":"...","suggestedLeadTimeDays":7}
                Constraints: recommendedQuantity must be a positive integer.
                """.formatted(productBlock(ctx));
    }

    private String manualReorderPrompt(CommerceContext ctx) {
        return """
                You are a replenishment advisor. A merchandiser requested an on-demand reorder review.

                %s
                Trigger: %s.

                Reply JSON only:
                {"recommendedQuantity":150,"confidence":0.78,"reasoning":"...","suggestedLeadTimeDays":7}
                """.formatted(productBlock(ctx), ctx.triggerReason());
    }

    private double ratio(int velocity, double avg) {
        if (avg <= 0) {
            return velocity;
        }
        return velocity / avg;
    }
}
