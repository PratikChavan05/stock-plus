package com.stockpulse.repository;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.ProductCategory;
import com.stockpulse.domain.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByStatus(ProductStatus status);
    List<Product> findByCategory(ProductCategory category);
    List<Product> findByStatusAndCategory(ProductStatus status, ProductCategory category);
}
