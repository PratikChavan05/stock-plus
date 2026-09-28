package com.stockpulse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Product {

    @Id
    private String id;

    @Column(unique = true, nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory category;

    @Column(name = "current_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "stock_level", nullable = false)
    private int stockLevel;

    @Column(name = "reorder_threshold", nullable = false)
    private int reorderThreshold;

    @Column(name = "demand_velocity", nullable = false)
    private int demandVelocity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    /** Sprint 2: AI must not recommend below floor once margin rules land. */
    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "margin_floor")
    private Double marginFloor;

    @Column(name = "supplier_id")
    private String supplierId;

    public void applyStockLevel(int stock) {
        this.stockLevel = Math.max(stock, 0);
        if (this.stockLevel == 0) {
            this.status = ProductStatus.OUT_OF_STOCK;
        }
    }
}
