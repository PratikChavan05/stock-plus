package com.stockpulse.web.dto;

import jakarta.validation.constraints.Min;

public record OrderRequest(@Min(1) Integer quantity) {}
