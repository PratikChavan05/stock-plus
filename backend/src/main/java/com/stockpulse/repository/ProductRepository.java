package com.stockpulse.repository;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.ProductCategory;
import com.stockpulse.domain.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByStatus(ProductStatus status);
    List<Product> findByCategory(ProductCategory category);
    List<Product> findByStatusAndCategory(ProductStatus status, ProductCategory category);

    @Query("SELECT COALESCE(AVG(p.demandVelocity), 0) FROM Product p WHERE p.category = :category")
    double averageDemandVelocity(@Param("category") ProductCategory category);
}
