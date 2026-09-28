package com.stockpulse.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_suggestions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product product;

    @Column(name = "current_price", nullable = false)
    private BigDecimal currentPrice;

    @Column(name = "recommended_price", nullable = false)
    private BigDecimal recommendedPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionDirection direction;

    @Column(nullable = false)
    private Double confidence;

    @Column(length = 1000)
    private String reasoning;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_reason", nullable = false)
    private TriggerReason triggerReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
