package com.stockpulse.service.advisor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CommerceAdvisorRegistry {

    private final Map<String, CommerceAdvisor> advisors;
    private volatile String activeStrategy;

    public CommerceAdvisorRegistry(List<CommerceAdvisor> advisorList,
                                   @Value("${commerce.strategy:rule-based}") String activeStrategy) {
        this.advisors = advisorList.stream()
                .collect(Collectors.toMap(CommerceAdvisor::getName, Function.identity()));
        this.activeStrategy = activeStrategy;
    }

    public CommerceAdvisor getActiveAdvisor() {
        return require(activeStrategy);
    }

    public CommerceAdvisor require(String name) {
        CommerceAdvisor advisor = advisors.get(name);
        if (advisor == null) {
            advisor = advisors.get("rule-based");
        }
        if (advisor == null) {
            throw new IllegalStateException("No commerce advisors registered");
        }
        return advisor;
    }

    public synchronized void setActiveStrategy(String strategy) {
        if (!advisors.containsKey(strategy)) {
            throw new IllegalArgumentException("Unknown strategy: " + strategy + ". Known: " + advisors.keySet());
        }
        this.activeStrategy = strategy;
    }

    public String getActiveStrategy() {
        return activeStrategy;
    }

    public Set<String> availableStrategies() {
        return advisors.keySet();
    }
}
