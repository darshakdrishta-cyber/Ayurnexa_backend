package com.Ayurnexa.Ayurnexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.Ayurnexa.Ayurnexa.model.product.Product;

public interface ProductRepository extends MongoRepository<Product, String> {
    Optional<Product> findByProductId(String productId);
    boolean existsByProductId(String productId);
    List<Product> findByIsActive(boolean isActive);
    List<Product> findByProductNameContainingIgnoreCase(String name);
}
