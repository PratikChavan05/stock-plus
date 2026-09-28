package com.stockpulse.repository;

import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.SuggestionStatus;
import com.stockpulse.domain.TriggerReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReorderSuggestionRepository extends JpaRepository<ReorderSuggestion, String> {
    Optional<ReorderSuggestion> findFirstByProductIdAndStatusAndTriggerReason(
            String productId, SuggestionStatus status, TriggerReason triggerReason);
}
