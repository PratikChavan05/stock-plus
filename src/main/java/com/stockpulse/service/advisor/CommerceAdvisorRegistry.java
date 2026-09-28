package com.stockpulse.service.advisor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CommerceAdvisorRegistry {

    private final Map<String, CommerceAdvisor> advisors;
    
    @Value("${commerce.strategy:rule-based}")
    private String activeStrategy;

    public CommerceAdvisorRegistry(List<CommerceAdvisor> advisorList) {
        this.advisors = advisorList.stream()
                .collect(Collectors.toMap(CommerceAdvisor::getName, Function.identity()));
    }

    public CommerceAdvisor getActiveAdvisor() {
        return advisors.getOrDefault(activeStrategy, advisors.get("rule-based"));
    }
    
    public void setActiveStrategy(String strategy) {
        this.activeStrategy = strategy;
    }
    
    public String getActiveStrategy() {
        return this.activeStrategy;
    }
}
