package com.Ayurnexa.Ayurnexa.dto.payment;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class PaymentDTO {

    private PaymentDTO() {}

    public record CreateRequest(
            String orderId,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotBlank String paymentMethod,
            @NotBlank @Size(max = 128) String idempotencyKey,
            String customerName,
            String customerContact,
            String customerEmail) {}

    public record UpdateRequest(
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotBlank String paymentMethod) {}

    public record Response(
            String id,
            String paymentId,
            String paymentLinkId,
            String orderId,
            BigDecimal amount,
            String currency,
            String paymentStatus,
            String paymentMethod,
            String paymentLinkUrl,
            Instant createdAt,
            Instant updatedAt) {}
}