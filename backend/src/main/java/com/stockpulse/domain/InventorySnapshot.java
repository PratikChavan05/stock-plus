package com.stockpulse.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventorySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "stock_level", nullable = false)
    private int stockLevel;

    @Column(name = "demand_velocity", nullable = false)
    private int demandVelocity;

    @Column(nullable = false)
    private String source;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    @PrePersist
    public void prePersist() {
        if (this.capturedAt == null) {
            this.capturedAt = LocalDateTime.now();
        }
    }
}
