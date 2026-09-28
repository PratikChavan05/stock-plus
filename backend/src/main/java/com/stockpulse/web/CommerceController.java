package com.stockpulse.web;

import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import com.stockpulse.service.advisor.CommerceAdvisorRegistry;
import com.stockpulse.web.dto.StrategyUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/commerce")
@RequiredArgsConstructor
public class CommerceController {

    private final CommerceAdvisorRegistry registry;
    private final ProductRepository productRepository;
    private final PricingSuggestionRepository pricingRepo;
    private final ReorderSuggestionRepository reorderRepo;

    @GetMapping("/strategy")
    public Map<String, Object> strategy() {
        return Map.of(
                "active", registry.getActiveStrategy(),
                "available", registry.availableStrategies()
        );
    }

    @PutMapping("/strategy")
    public Map<String, Object> update(@Valid @RequestBody StrategyUpdateRequest request) {
        registry.setActiveStrategy(request.strategy());
        return strategy();
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return Map.of(
                "products", productRepository.findAll(),
                "pendingPricing", pricingRepo.findByStatusOrderByCreatedAtDesc(
                        com.stockpulse.domain.SuggestionStatus.PENDING),
                "pendingReorder", reorderRepo.findByStatusOrderByCreatedAtDesc(
                        com.stockpulse.domain.SuggestionStatus.PENDING),
                "strategy", strategy()
        );
    }
}
