package com.stockpulse.service;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.ProductStatus;
import com.stockpulse.event.DemandSpikeEvent;
import com.stockpulse.event.InventoryLowEvent;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.web.dto.CreateProductRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final InventorySnapshotService snapshotService;

    @Value("${commerce.demand-spike-multiplier:3.0}")
    private double demandSpikeMultiplier;

    @Transactional
    public Product create(CreateProductRequest request) {
        String id = request.id() == null || request.id().isBlank()
                ? "PRD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
                : request.id();
        Product product = Product.builder()
                .id(id)
                .sku(request.sku())
                .name(request.name())
                .category(request.category())
                .currentPrice(request.currentPrice())
                .stockLevel(request.stockLevel())
                .reorderThreshold(request.reorderThreshold())
                .demandVelocity(request.demandVelocity() == null ? 0 : request.demandVelocity())
                .status(request.stockLevel() == 0 ? ProductStatus.OUT_OF_STOCK : ProductStatus.ACTIVE)
                .costPrice(request.costPrice())
                .marginFloor(request.marginFloor())
                .supplierId(request.supplierId())
                .build();
        Product saved = productRepository.save(product);
        snapshotService.capture(saved, "CREATE");
        return saved;
    }

    @Transactional
    public Product updateStock(String productId, int newStock) {
        Product product = require(productId);
        if (newStock < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stockLevel cannot be negative");
        }
        product.applyStockLevel(newStock);
        if (product.getStockLevel() > 0 && product.getStatus() == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.ACTIVE);
        }
        Product saved = productRepository.save(product);
        snapshotService.capture(saved, "STOCK_PATCH");
        publishSignals(saved);
        return saved;
    }

    @Transactional
    public Product simulateOrder(String productId, int quantity) {
        if (quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be at least 1");
        }
        Product product = require(productId);
        if (product.getStockLevel() < quantity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock");
        }
        product.applyStockLevel(product.getStockLevel() - quantity);
        product.setDemandVelocity(product.getDemandVelocity() + quantity);
        Product saved = productRepository.save(product);
        snapshotService.capture(saved, "ORDER");
        publishSignals(saved);
        return saved;
    }

    public Product require(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + productId));
    }

    private void publishSignals(Product saved) {
        if (saved.getStockLevel() < saved.getReorderThreshold()) {
            eventPublisher.publishEvent(new InventoryLowEvent(this, saved.getId()));
        }
        double categoryAvg = productRepository.averageDemandVelocity(saved.getCategory());
        if (categoryAvg <= 0) {
            categoryAvg = 1.0;
        }
        if (saved.getDemandVelocity() > demandSpikeMultiplier * categoryAvg) {
            eventPublisher.publishEvent(new DemandSpikeEvent(this, saved.getId()));
        }
    }
}
