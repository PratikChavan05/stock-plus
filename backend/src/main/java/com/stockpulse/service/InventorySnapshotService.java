package com.stockpulse.service;

import com.stockpulse.domain.InventorySnapshot;
import com.stockpulse.domain.Product;
import com.stockpulse.repository.InventorySnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventorySnapshotService {

    private final InventorySnapshotRepository repository;

    public void capture(Product product, String source) {
        repository.save(InventorySnapshot.builder()
                .product(product)
                .stockLevel(product.getStockLevel())
                .demandVelocity(product.getDemandVelocity())
                .source(source)
                .build());
    }
}
