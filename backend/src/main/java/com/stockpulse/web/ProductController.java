package com.stockpulse.web;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.ProductCategory;
import com.stockpulse.domain.ProductStatus;
import com.stockpulse.repository.InventorySnapshotRepository;
import com.stockpulse.repository.PriceHistoryRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.service.ProductService;
import com.stockpulse.web.dto.CreateProductRequest;
import com.stockpulse.web.dto.OrderRequest;
import com.stockpulse.web.dto.StockUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final InventorySnapshotRepository snapshotRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product createProduct(@Valid @RequestBody CreateProductRequest request) {
        return productService.create(request);
    }

    @GetMapping
    public List<Product> getProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) ProductCategory category) {
        if (status != null && category != null) {
            return productRepository.findByStatusAndCategory(status, category);
        }
        if (status != null) {
            return productRepository.findByStatus(status);
        }
        if (category != null) {
            return productRepository.findByCategory(category);
        }
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    public Product getProduct(@PathVariable String id) {
        return productService.require(id);
    }

    @PatchMapping("/{id}/stock")
    public Product updateStock(@PathVariable String id, @Valid @RequestBody StockUpdateRequest payload) {
        return productService.updateStock(id, payload.stockLevel());
    }

    @PostMapping("/{id}/orders")
    public Product simulateOrder(@PathVariable String id, @Valid @RequestBody(required = false) OrderRequest payload) {
        int quantity = payload == null || payload.quantity() == null ? 1 : payload.quantity();
        return productService.simulateOrder(id, quantity);
    }

    @GetMapping("/{id}/snapshots")
    public Object snapshots(@PathVariable String id) {
        productService.require(id);
        return snapshotRepository.findByProductIdOrderByCapturedAtDesc(id);
    }

    @GetMapping("/{id}/price-history")
    public Object priceHistory(@PathVariable String id) {
        productService.require(id);
        return priceHistoryRepository.findByProductIdOrderByChangedAtAsc(id);
    }

    @GetMapping("/{id}/peers")
    public Map<String, Object> peers(@PathVariable String id) {
        var product = productService.require(id);
        double avg = productRepository.averageDemandVelocity(product.getCategory());
        return Map.of(
                "productId", product.getId(),
                "category", product.getCategory(),
                "demandVelocity", product.getDemandVelocity(),
                "categoryAverageVelocity", avg
        );
    }
}
