package com.stockpulse.service.advisor;

import com.stockpulse.ai.AdvisorPromptFactory;
import com.stockpulse.ai.LLMGateway;
import com.stockpulse.ai.LlmResponseParser;
import com.stockpulse.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class AICommerceAdvisor implements CommerceAdvisor {

    private static final Logger log = LoggerFactory.getLogger(AICommerceAdvisor.class);

    private final LLMGateway llmGateway;
    private final AdvisorPromptFactory prompts;
    private final LlmResponseParser parser;
    private final CommerceAdvisor ruleBasedFallback;

    public AICommerceAdvisor(LLMGateway llmGateway,
                             AdvisorPromptFactory prompts,
                             LlmResponseParser parser,
                             @Qualifier("ruleBasedCommerceAdvisor") CommerceAdvisor ruleBasedFallback) {
        this.llmGateway = llmGateway;
        this.prompts = prompts;
        this.parser = parser;
        this.ruleBasedFallback = ruleBasedFallback;
    }

    @Override
    public String getName() {
        return "ai-based";
    }

    @Override
    public PricingSuggestion suggestPricing(CommerceContext context) {
        try {
            if (!llmGateway.isConfigured()) {
                return annotateFallback(ruleBasedFallback.suggestPricing(context), "LLM not configured");
            }
            String raw = llmGateway.callLLM(prompts.pricingPrompt(context));
            LlmResponseParser.PricingAdvice advice = parser.parsePricing(raw, context.product().getCurrentPrice());
            return PricingSuggestion.builder()
                    .product(context.product())
                    .currentPrice(context.product().getCurrentPrice())
                    .recommendedPrice(advice.recommendedPrice())
                    .direction(advice.direction())
                    .confidence(advice.confidence())
                    .reasoning(advice.reasoning())
                    .status(SuggestionStatus.PENDING)
                    .triggerReason(context.triggerReason())
                    .build();
        } catch (Exception e) {
            log.warn("AI pricing failed for {}: {} — using rule-based fallback",
                    context.product().getId(), e.getMessage());
            return annotateFallback(ruleBasedFallback.suggestPricing(context), e.getMessage());
        }
    }

    @Override
    public ReorderSuggestion suggestReorder(CommerceContext context) {
        try {
            if (!llmGateway.isConfigured()) {
                return annotateFallback(ruleBasedFallback.suggestReorder(context), "LLM not configured");
            }
            String raw = llmGateway.callLLM(prompts.reorderPrompt(context));
            LlmResponseParser.ReorderAdvice advice = parser.parseReorder(raw);
            return ReorderSuggestion.builder()
                    .product(context.product())
                    .currentStock(context.product().getStockLevel())
                    .recommendedQuantity(advice.recommendedQuantity())
                    .suggestedLeadTimeDays(advice.suggestedLeadTimeDays())
                    .confidence(advice.confidence())
                    .reasoning(advice.reasoning())
                    .status(SuggestionStatus.PENDING)
                    .triggerReason(context.triggerReason())
                    .build();
        } catch (Exception e) {
            log.warn("AI reorder failed for {}: {} — using rule-based fallback",
                    context.product().getId(), e.getMessage());
            return annotateFallback(ruleBasedFallback.suggestReorder(context), e.getMessage());
        }
    }

    private PricingSuggestion annotateFallback(PricingSuggestion suggestion, String cause) {
        suggestion.setReasoning("[Rule fallback: " + cause + "] " + suggestion.getReasoning());
        suggestion.setConfidence(Math.min(suggestion.getConfidence(), 0.6));
        return suggestion;
    }

    private ReorderSuggestion annotateFallback(ReorderSuggestion suggestion, String cause) {
        suggestion.setReasoning("[Rule fallback: " + cause + "] " + suggestion.getReasoning());
        suggestion.setConfidence(Math.min(suggestion.getConfidence(), 0.6));
        return suggestion;
    }
}
