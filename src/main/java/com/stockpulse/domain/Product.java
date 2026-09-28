package com.stockpulse.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    private String id; // Using string as specified in the seed script PRD-001

    @Column(unique = true, nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory category;

    @Column(name = "current_price", nullable = false)
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

    // Sprint 2 Extensibility hooks
    @Column(name = "cost_price")
    private BigDecimal costPrice;

    @Column(name = "supplier_id")
    private String supplierId;
}
