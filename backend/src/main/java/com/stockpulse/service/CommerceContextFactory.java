package com.stockpulse.service;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.TriggerReason;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.service.advisor.CommerceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommerceContextFactory {

    private final ProductRepository productRepository;

    public CommerceContext forProduct(Product product, TriggerReason reason) {
        double average = productRepository.averageDemandVelocity(product.getCategory());
        if (average <= 0) {
            average = 1.0;
        }
        return new CommerceContext(product, reason, average);
    }
}
