package com.stockpulse.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class LLMGateway {

    @Value("${llm.provider}") private String provider;
    @Value("${llm.api-key:}") private String apiKey;
    @Value("${llm.model}") private String model;
    @Value("${llm.base-url}") private String baseUrl;
    private final RestClient http = RestClient.create();

    public String callLLM(String prompt) {
        if ("rule-based".equalsIgnoreCase(provider)) {
            return ""; // fallback to rule-based directly handled by caller
        }
        return switch (provider.toLowerCase()) {
            case "gemini" -> callGemini(prompt);
            case "groq"   -> callOpenAICompatible(prompt, baseUrl + "/openai/v1/chat/completions");
            case "ollama" -> callOpenAICompatible(prompt, baseUrl + "/v1/chat/completions");
            default      -> throw new IllegalStateException("Unknown provider: " + provider);
        };
    }

    private String callGemini(String prompt) { 
        // Mock implementation for demo or if actual API fails
        return "{\"recommendedPrice\": 29.99, \"direction\": \"INCREASE\", \"confidence\": 0.8, \"reasoning\": \"Demand is high.\"}";
    }
    
    private String callOpenAICompatible(String prompt, String url) { 
        return "{}"; 
    }
}
