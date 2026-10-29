package com.Ayurnexa.Ayurnexa.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.Ayurnexa.Ayurnexa.model.cart.Cart;

public interface CartRepository extends MongoRepository<Cart, String> {

    Optional<Cart> findByUserId(String userId);

    void deleteByUserId(String userId);
}