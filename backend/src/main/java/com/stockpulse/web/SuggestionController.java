package com.stockpulse.web;

import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.SuggestionStatus;
import com.stockpulse.domain.TriggerReason;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import com.stockpulse.service.AgenticRecommendationLoop;
import com.stockpulse.service.ProductService;
import com.stockpulse.service.SuggestionService;
import com.stockpulse.service.advisor.CommerceAdvisorRegistry;
import com.stockpulse.web.dto.SuggestionDecisionRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;

@RestController
public class SuggestionController {

    private final PricingSuggestionRepository pricingRepo;
    private final ReorderSuggestionRepository reorderRepo;
    private final SuggestionService suggestionService;
    private final ProductService productService;
    private final AgenticRecommendationLoop agenticLoop;
    private final CommerceAdvisorRegistry advisorRegistry;
    private final Executor taskExecutor;

    public SuggestionController(PricingSuggestionRepository pricingRepo,
                                ReorderSuggestionRepository reorderRepo,
                                SuggestionService suggestionService,
                                ProductService productService,
                                AgenticRecommendationLoop agenticLoop,
                                CommerceAdvisorRegistry advisorRegistry,
                                @Qualifier("taskExecutor") Executor taskExecutor) {
        this.pricingRepo = pricingRepo;
        this.reorderRepo = reorderRepo;
        this.suggestionService = suggestionService;
        this.productService = productService;
        this.agenticLoop = agenticLoop;
        this.advisorRegistry = advisorRegistry;
        this.taskExecutor = taskExecutor;
    }

    @GetMapping("/pricing-suggestions")
    public List<PricingSuggestion> getPricingSuggestions(
            @RequestParam(required = false) SuggestionStatus status) {
        if (status != null) {
            return pricingRepo.findByStatusOrderByCreatedAtDesc(status);
        }
        return pricingRepo.findAll();
    }

    @GetMapping("/reorder-suggestions")
    public List<ReorderSuggestion> getReorderSuggestions(
            @RequestParam(required = false) SuggestionStatus status) {
        if (status != null) {
            return reorderRepo.findByStatusOrderByCreatedAtDesc(status);
        }
        return reorderRepo.findAll();
    }

    @PatchMapping("/pricing-suggestions/{id}")
    public PricingSuggestion resolvePricing(@PathVariable String id,
                                            @Valid @RequestBody SuggestionDecisionRequest payload) {
        return suggestionService.resolvePricingSuggestion(id, payload.accept());
    }

    @PatchMapping("/reorder-suggestions/{id}")
    public ReorderSuggestion resolveReorder(@PathVariable String id,
                                            @Valid @RequestBody SuggestionDecisionRequest payload) {
        return suggestionService.resolveReorderSuggestion(id, payload.accept());
    }

    @PostMapping("/products/{id}/suggest-pricing")
    public PricingSuggestion suggestPricing(@PathVariable String id) {
        Product product = productService.require(id);
        return agenticLoop.requestPricing(product, TriggerReason.MANUAL);
    }

    @PostMapping("/products/{id}/suggest-reorder")
    public ReorderSuggestion suggestReorder(@PathVariable String id) {
        Product product = productService.require(id);
        return agenticLoop.requestReorder(product, TriggerReason.MANUAL);
    }

    @PostMapping(value = "/products/{id}/suggest-pricing/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPricing(@PathVariable String id) {
        Product product = productService.require(id);
        SseEmitter emitter = new SseEmitter(30_000L);
        taskExecutor.execute(() -> {
            try {
                emitter.send(SseEmitter.event().name("meta").data(
                        "strategy=" + advisorRegistry.getActiveStrategy() + "; product=" + product.getName()));
                PricingSuggestion suggestion = agenticLoop.requestPricing(product, TriggerReason.MANUAL);
                String reasoning = suggestion.getReasoning() == null ? "" : suggestion.getReasoning();
                for (String token : reasoning.split(" ")) {
                    emitter.send(SseEmitter.event().name("token").data(token));
                    Thread.sleep(18);
                }
                emitter.send(SseEmitter.event().name("suggestion").data(suggestion));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (IOException ignored) {
                    // closed
                }
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }
}
