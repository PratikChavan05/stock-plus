package com.stockpulse.web;

import com.stockpulse.domain.*;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import com.stockpulse.service.SuggestionService;
import com.stockpulse.service.advisor.CommerceAdvisor;
import com.stockpulse.service.advisor.CommerceAdvisorRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:4200"})
@RequiredArgsConstructor
public class SuggestionController {

    private final PricingSuggestionRepository pricingRepo;
    private final ReorderSuggestionRepository reorderRepo;
    private final SuggestionService suggestionService;
    private final ProductRepository productRepo;
    private final CommerceAdvisorRegistry advisorRegistry;

    @GetMapping("/pricing-suggestions")
    public List<PricingSuggestion> getPricingSuggestions() {
        return pricingRepo.findAll();
    }

    @GetMapping("/reorder-suggestions")
    public List<ReorderSuggestion> getReorderSuggestions() {
        return reorderRepo.findAll();
    }

    @PatchMapping("/pricing-suggestions/{id}")
    public void resolvePricing(@PathVariable String id, @RequestBody Map<String, Boolean> payload) {
        suggestionService.resolvePricingSuggestion(id, payload.get("accept"));
    }

    @PatchMapping("/reorder-suggestions/{id}")
    public void resolveReorder(@PathVariable String id, @RequestBody Map<String, Boolean> payload) {
        suggestionService.resolveReorderSuggestion(id, payload.get("accept"));
    }

    @PostMapping("/products/{id}/suggest-pricing")
    public PricingSuggestion suggestPricing(@PathVariable String id) {
        Product product = productRepo.findById(id).orElseThrow();
        CommerceAdvisor advisor = advisorRegistry.getActiveAdvisor();
        PricingSuggestion suggestion = advisor.suggestPricing(product, TriggerReason.MANUAL);
        if (suggestion != null) {
            return pricingRepo.save(suggestion);
        }
        return null;
    }

    @PostMapping("/products/{id}/suggest-reorder")
    public ReorderSuggestion suggestReorder(@PathVariable String id) {
        Product product = productRepo.findById(id).orElseThrow();
        CommerceAdvisor advisor = advisorRegistry.getActiveAdvisor();
        ReorderSuggestion suggestion = advisor.suggestReorder(product, TriggerReason.MANUAL);
        if (suggestion != null) {
            return reorderRepo.save(suggestion);
        }
        return null;
    }
}
