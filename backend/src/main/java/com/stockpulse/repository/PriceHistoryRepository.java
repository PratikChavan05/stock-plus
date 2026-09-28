package com.stockpulse.repository;

import com.stockpulse.domain.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, String> {
    List<PriceHistory> findByProductIdOrderByChangedAtAsc(String productId);
}
