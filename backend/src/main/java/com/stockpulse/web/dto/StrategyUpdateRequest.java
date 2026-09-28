package com.stockpulse.web.dto;

import jakarta.validation.constraints.NotBlank;

public record StrategyUpdateRequest(@NotBlank String strategy) {}
