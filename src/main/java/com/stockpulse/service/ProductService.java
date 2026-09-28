package com.stockpulse.service;

import com.stockpulse.domain.Product;
import com.stockpulse.event.DemandSpikeEvent;
import com.stockpulse.event.InventoryLowEvent;
import com.stockpulse.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Product updateStock(String productId, int newStock) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        product.setStockLevel(newStock);
        if (newStock == 0) {
            product.setStatus(com.stockpulse.domain.ProductStatus.OUT_OF_STOCK);
        }
        Product saved = productRepository.save(product);
        
        // Trigger loop if stock drops below threshold
        if (saved.getStockLevel() < saved.getReorderThreshold()) {
            eventPublisher.publishEvent(new InventoryLowEvent(this, saved.getId()));
        }
        
        return saved;
    }

    @Transactional
    public Product simulateOrder(String productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        int currentStock = product.getStockLevel();
        if (currentStock < quantity) {
            throw new RuntimeException("Insufficient stock");
        }
        
        product.setStockLevel(currentStock - quantity);
        product.setDemandVelocity(product.getDemandVelocity() + quantity);
        
        if (product.getStockLevel() == 0) {
            product.setStatus(com.stockpulse.domain.ProductStatus.OUT_OF_STOCK);
        }
        
        Product saved = productRepository.save(product);
        
        // Triggers
        if (saved.getStockLevel() < saved.getReorderThreshold()) {
            eventPublisher.publishEvent(new InventoryLowEvent(this, saved.getId()));
        }
        
        // Spike condition (e.g. velocity > 10)
        if (saved.getDemandVelocity() > 10) {
            eventPublisher.publishEvent(new DemandSpikeEvent(this, saved.getId()));
        }
        
        return saved;
    }
}
