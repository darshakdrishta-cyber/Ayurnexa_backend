package com.Ayurnexa.Ayurnexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.Ayurnexa.Ayurnexa.model.order.Order;

public interface OrderRepository extends MongoRepository<Order, String> {
    Optional<Order> findByOrderId(String orderId);
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<Order> findByPaymentId(String paymentId);
    boolean existsByItemsProductId(String productId);
}