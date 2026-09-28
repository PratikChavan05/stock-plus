package com.stockpulse.service.advisor;

import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.ReorderSuggestion;

/**
 * Single commerce contract used by on-demand HTTP endpoints and the async agentic loop.
 * Sprint 2: add CompetitorAwareCommerceAdvisor as another @Component implementing this.
 */
public interface CommerceAdvisor {

    String getName();

    PricingSuggestion suggestPricing(CommerceContext context);

    ReorderSuggestion suggestReorder(CommerceContext context);
}
