package com.stockpulse.web;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.ProductCategory;
import com.stockpulse.domain.ProductStatus;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:4200"})
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductService productService;

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        if (product.getStatus() == null) product.setStatus(ProductStatus.ACTIVE);
        return productRepository.save(product);
    }

    @GetMapping
    public List<Product> getProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) ProductCategory category) {
        if (status != null && category != null) {
            return productRepository.findByStatusAndCategory(status, category);
        } else if (status != null) {
            return productRepository.findByStatus(status);
        } else if (category != null) {
            return productRepository.findByCategory(category);
        }
        return productRepository.findAll();
    }

    @PatchMapping("/{id}/stock")
    public Product updateStock(@PathVariable String id, @RequestBody Map<String, Integer> payload) {
        return productService.updateStock(id, payload.get("stockLevel"));
    }

    @PostMapping("/{id}/orders")
    public Product simulateOrder(@PathVariable String id, @RequestBody Map<String, Integer> payload) {
        return productService.simulateOrder(id, payload.get("quantity"));
    }
}
