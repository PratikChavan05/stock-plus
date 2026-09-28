package com.stockpulse.web.dto;

import com.stockpulse.domain.ProductCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateProductRequest(
        String id,
        @NotBlank String sku,
        @NotBlank String name,
        @NotNull ProductCategory category,
        @NotNull @DecimalMin("0.01") BigDecimal currentPrice,
        @Min(0) int stockLevel,
        @Min(0) int reorderThreshold,
        Integer demandVelocity,
        BigDecimal costPrice,
        Double marginFloor,
        String supplierId
) {}
