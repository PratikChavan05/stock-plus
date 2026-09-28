package com.stockpulse.service;

import com.stockpulse.domain.*;
import com.stockpulse.repository.PriceHistoryRepository;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SuggestionService {

    private final PricingSuggestionRepository pricingRepo;
    private final ReorderSuggestionRepository reorderRepo;
    private final ProductRepository productRepo;
    private final PriceHistoryRepository priceHistoryRepository;
    private final InventorySnapshotService snapshotService;

    @Transactional
    public PricingSuggestion resolvePricingSuggestion(String id, boolean accept) {
        PricingSuggestion suggestion = pricingRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pricing suggestion not found"));
        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Suggestion already " + suggestion.getStatus());
        }
        Product product = suggestion.getProduct();
        if (accept) {
            suggestion.setStatus(SuggestionStatus.ACCEPTED);
            priceHistoryRepository.save(PriceHistory.builder()
                    .product(product)
                    .oldPrice(product.getCurrentPrice())
                    .newPrice(suggestion.getRecommendedPrice())
                    .suggestionId(suggestion.getId())
                    .build());
            product.setCurrentPrice(suggestion.getRecommendedPrice());
        } else {
            suggestion.setStatus(SuggestionStatus.REJECTED);
        }
        refreshLifecycle(product);
        productRepo.save(product);
        return pricingRepo.save(suggestion);
    }

    @Transactional
    public ReorderSuggestion resolveReorderSuggestion(String id, boolean accept) {
        ReorderSuggestion suggestion = reorderRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reorder suggestion not found"));
        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Suggestion already " + suggestion.getStatus());
        }
        Product product = suggestion.getProduct();
        if (accept) {
            suggestion.setStatus(SuggestionStatus.ACCEPTED);
            product.setStockLevel(product.getStockLevel() + suggestion.getRecommendedQuantity());
            if (product.getStatus() == ProductStatus.OUT_OF_STOCK && product.getStockLevel() > 0) {
                product.setStatus(ProductStatus.ACTIVE);
            }
            snapshotService.capture(product, "REORDER_ACCEPT");
        } else {
            suggestion.setStatus(SuggestionStatus.REJECTED);
        }
        refreshLifecycle(product);
        productRepo.save(product);
        return reorderRepo.save(suggestion);
    }

    /**
     * Pricing and reorder checkpoints are independent. Product leaves PRICE_REVIEW_PENDING
     * only when no pricing suggestion is still waiting — a rejected reorder does not unblock price.
     */
    private void refreshLifecycle(Product product) {
        if (product.getStockLevel() == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
            return;
        }
        boolean pricingOpen = pricingRepo.existsByProductIdAndStatus(product.getId(), SuggestionStatus.PENDING);
        product.setStatus(pricingOpen ? ProductStatus.PRICE_REVIEW_PENDING : ProductStatus.ACTIVE);
    }
}
