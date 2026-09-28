package com.stockpulse.web.dto;

import jakarta.validation.constraints.NotNull;

public record SuggestionDecisionRequest(@NotNull Boolean accept) {}
