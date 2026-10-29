package com.Ayurnexa.Ayurnexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.Ayurnexa.Ayurnexa.model.Payment.Payment;

public interface PaymentRepository extends MongoRepository<Payment, String> {

    Optional<Payment> findByPaymentId(String paymentId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByOrderId(String orderId);

    List<Payment> findByUserIdOrderByCreatedAtDesc(String userId);

    boolean existsByOrderIdAndPaymentStatus(String orderId, String paymentStatus);
}
