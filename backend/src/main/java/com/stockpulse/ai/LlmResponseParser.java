package com.stockpulse.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.domain.SuggestionDirection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class LlmResponseParser {

    private final ObjectMapper objectMapper;
    private final int maxPriceMultiple;

    public LlmResponseParser(ObjectMapper objectMapper,
                             @Value("${commerce.max-price-multiple:10}") int maxPriceMultiple) {
        this.objectMapper = objectMapper;
        this.maxPriceMultiple = maxPriceMultiple;
    }

    public PricingAdvice parsePricing(String raw, BigDecimal currentPrice) {
        JsonNode node = read(raw);
        BigDecimal recommended = decimal(node, "recommendedPrice");
        if (recommended == null || recommended.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("recommendedPrice must be positive");
        }
        BigDecimal ceiling = currentPrice.multiply(BigDecimal.valueOf(maxPriceMultiple));
        BigDecimal floor = currentPrice.multiply(BigDecimal.valueOf(0.1));
        if (recommended.compareTo(ceiling) > 0 || recommended.compareTo(floor) < 0) {
            throw new IllegalArgumentException("recommendedPrice outside sane bounds vs current " + currentPrice);
        }
        SuggestionDirection direction = parseDirection(node.path("direction").asText(null), currentPrice, recommended);
        double confidence = clampConfidence(node.path("confidence").asDouble(0.7));
        String reasoning = node.path("reasoning").asText("No reasoning provided");
        return new PricingAdvice(recommended.setScale(2, RoundingMode.HALF_UP), direction, confidence, reasoning);
    }

    public ReorderAdvice parseReorder(String raw) {
        JsonNode node = read(raw);
        if (!node.has("recommendedQuantity") || !node.get("recommendedQuantity").isNumber()) {
            throw new IllegalArgumentException("recommendedQuantity must be a positive integer");
        }
        int qty = node.get("recommendedQuantity").asInt();
        if (qty < 1) {
            throw new IllegalArgumentException("recommendedQuantity must be a positive integer");
        }
        double confidence = clampConfidence(node.path("confidence").asDouble(0.7));
        String reasoning = node.path("reasoning").asText("No reasoning provided");
        Integer lead = node.has("suggestedLeadTimeDays") ? node.get("suggestedLeadTimeDays").asInt() : 7;
        return new ReorderAdvice(qty, confidence, reasoning, lead);
    }

    private JsonNode read(String raw) {
        try {
            String json = stripFences(raw);
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unparseable LLM JSON", e);
        }
    }

    private static String stripFences(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.startsWith("```")) {
            int firstNl = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNl > 0 && lastFence > firstNl) {
                text = text.substring(firstNl + 1, lastFence).trim();
            }
        }
        return text;
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isNumber()) {
            return null;
        }
        return new BigDecimal(value.asText());
    }

    private static SuggestionDirection parseDirection(String raw, BigDecimal current, BigDecimal recommended) {
        if (raw != null && !raw.isBlank()) {
            try {
                return SuggestionDirection.valueOf(raw.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // fall through to inferred direction
            }
        }
        int cmp = recommended.compareTo(current);
        if (cmp > 0) {
            return SuggestionDirection.INCREASE;
        }
        if (cmp < 0) {
            return SuggestionDirection.DECREASE;
        }
        return SuggestionDirection.HOLD;
    }

    private static double clampConfidence(double value) {
        if (Double.isNaN(value)) {
            return 0.5;
        }
        return Math.min(1.0, Math.max(0.0, value));
    }

    public record PricingAdvice(BigDecimal recommendedPrice, SuggestionDirection direction, double confidence, String reasoning) {}

    public record ReorderAdvice(int recommendedQuantity, double confidence, String reasoning, Integer suggestedLeadTimeDays) {}
}
