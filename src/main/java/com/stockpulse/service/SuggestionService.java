package com.stockpulse.service;

import com.stockpulse.domain.*;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SuggestionService {

    private final PricingSuggestionRepository pricingRepo;
    private final ReorderSuggestionRepository reorderRepo;
    private final ProductRepository productRepo;

    @Transactional
    public void resolvePricingSuggestion(String id, boolean accept) {
        PricingSuggestion suggestion = pricingRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("PricingSuggestion not found"));
        
        if (accept) {
            suggestion.setStatus(SuggestionStatus.ACCEPTED);
            Product product = suggestion.getProduct();
            product.setCurrentPrice(suggestion.getRecommendedPrice());
            product.setStatus(ProductStatus.ACTIVE);
            productRepo.save(product);
        } else {
            suggestion.setStatus(SuggestionStatus.REJECTED);
        }
        pricingRepo.save(suggestion);
    }

    @Transactional
    public void resolveReorderSuggestion(String id, boolean accept) {
        ReorderSuggestion suggestion = reorderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("ReorderSuggestion not found"));

        if (accept) {
            suggestion.setStatus(SuggestionStatus.ACCEPTED);
            Product product = suggestion.getProduct();
            product.setStockLevel(product.getStockLevel() + suggestion.getRecommendedQuantity());
            if (product.getStatus() == ProductStatus.OUT_OF_STOCK && product.getStockLevel() > 0) {
                 product.setStatus(ProductStatus.ACTIVE);
            }
            productRepo.save(product);
        } else {
            suggestion.setStatus(SuggestionStatus.REJECTED);
        }
        reorderRepo.save(suggestion);
    }
}
