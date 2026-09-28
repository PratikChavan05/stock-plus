package com.stockpulse.service.advisor;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.TriggerReason;

/**
 * Shared input for HTTP on-demand endpoints and the async agentic loop.
 * Category average is computed once so pricing and reorder see the same peer context.
 */
public record CommerceContext(
        Product product,
        TriggerReason triggerReason,
        double categoryAverageVelocity
) {
    public boolean stockBelowThreshold() {
        return product.getStockLevel() < product.getReorderThreshold();
    }
}
