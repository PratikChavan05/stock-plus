package com.stockpulse.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Provider-specific HTTP for Gemini, Groq, Ollama.
 * Returns raw text — parsing, validation, and fallback live in the AI advisor.
 */
@Component
public class LLMGateway {

    private final ObjectMapper objectMapper;
    private final RestClient http;

    @Value("${llm.provider}")
    private String provider;
    @Value("${llm.api-key:}")
    private String apiKey;
    @Value("${llm.model}")
    private String model;
    @Value("${llm.base-url}")
    private String baseUrl;

    public LLMGateway(ObjectMapper objectMapper, @Value("${llm.timeout-ms:8000}") int timeoutMs) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    public boolean isConfigured() {
        if (provider == null) {
            return false;
        }
        String p = provider.toLowerCase();
        if ("ollama".equals(p)) {
            return true;
        }
        return apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("your_");
    }

    public String callLLM(String prompt) {
        if (!isConfigured()) {
            throw new IllegalStateException("LLM is not configured");
        }
        return switch (provider.toLowerCase()) {
            case "gemini" -> callGemini(prompt);
            case "groq" -> callOpenAICompatible(prompt, trimSlash(baseUrl) + "/openai/v1/chat/completions");
            case "ollama" -> callOpenAICompatible(prompt, trimSlash(baseUrl) + "/v1/chat/completions");
            default -> throw new IllegalStateException("Unknown provider: " + provider);
        };
    }

    private String callGemini(String prompt) {
        String url = trimSlash(baseUrl) + "/v1beta/models/" + model + ":generateContent?key=" + apiKey;
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "responseMimeType", "application/json"
                )
        );
        String raw = http.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
        return extractGeminiText(raw);
    }

    private String callOpenAICompatible(String prompt, String url) {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.3,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a commerce merchandising advisor. Reply with JSON only."),
                        Map.of("role", "user", "content", prompt)
                )
        );
        var request = http.post().uri(url).contentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) {
            request = request.header("Authorization", "Bearer " + apiKey);
        }
        String raw = request.body(body).retrieve().body(String.class);
        return extractOpenAiText(raw);
    }

    private String extractGeminiText(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (text.isMissingNode() || text.asText().isBlank()) {
                throw new IllegalStateException("Empty Gemini response");
            }
            return text.asText();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse Gemini payload", e);
        }
    }

    private String extractOpenAiText(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode text = root.path("choices").path(0).path("message").path("content");
            if (text.isMissingNode() || text.asText().isBlank()) {
                throw new IllegalStateException("Empty chat completion");
            }
            return text.asText();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse OpenAI-compatible payload", e);
        }
    }

    private static String trimSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
