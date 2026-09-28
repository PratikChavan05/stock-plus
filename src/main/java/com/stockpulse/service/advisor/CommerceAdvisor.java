package com.stockpulse.service.advisor;

import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.TriggerReason;

public interface CommerceAdvisor {
    String getName();
    PricingSuggestion suggestPricing(Product product, TriggerReason triggerReason);
    ReorderSuggestion suggestReorder(Product product, TriggerReason triggerReason);
}
