package com.stockpulse.repository;

import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.SuggestionStatus;
import com.stockpulse.domain.TriggerReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PricingSuggestionRepository extends JpaRepository<PricingSuggestion, String> {
    Optional<PricingSuggestion> findFirstByProductIdAndStatusAndTriggerReason(
            String productId, SuggestionStatus status, TriggerReason triggerReason);

    List<PricingSuggestion> findByStatusOrderByCreatedAtDesc(SuggestionStatus status);

    List<PricingSuggestion> findByProductIdOrderByCreatedAtDesc(String productId);

    boolean existsByProductIdAndStatus(String productId, SuggestionStatus status);
}
