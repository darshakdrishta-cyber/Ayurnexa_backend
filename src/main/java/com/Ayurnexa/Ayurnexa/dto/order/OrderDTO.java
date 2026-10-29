package com.Ayurnexa.Ayurnexa.dto.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public final class OrderDTO {

    private OrderDTO() {}

    public record ItemRequest(
            @NotBlank String productId,
            @Min(1) int quantity) {}

    public record CreateRequest(
            @NotBlank String paymentId,
            @NotEmpty List<@Valid ItemRequest> items,
            LocalDate expectedDeliveryDate) {}

    public record UpdateRequest(
            String orderStatus,
            String orderProgress,
            LocalDate expectedDeliveryDate,
            LocalDate deliveryDate,
            Boolean active) {}

    public record ItemResponse(
            String orderItemId,
            String productId,
            int quantity,
            BigDecimal unitPrice,
            Integer rating,
            String feedback) {}

    public record Response(
            String orderId,
            String userId,
            String paymentId,
            String paymentMethod,
            String paymentStatus,
            String orderStatus,
            String orderProgress,
            BigDecimal price,
            boolean active,
            LocalDate expectedDeliveryDate,
            LocalDate deliveryDate,
            List<ItemResponse> items,
            Instant createdAt,
            Instant updatedAt) {}
}