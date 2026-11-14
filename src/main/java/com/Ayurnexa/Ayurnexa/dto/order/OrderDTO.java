package com.Ayurnexa.Ayurnexa.dto.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
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
            @JsonAlias("paymentId") @NotBlank String paymentRecordId,
            @NotEmpty List<@Valid ItemRequest> items,
            LocalDate expectedDeliveryDate,
            com.Ayurnexa.Ayurnexa.model.order.OrderAddress address) {}

    public record UpdateRequest(
            String orderStatus,
            String orderProgress,
            LocalDate expectedDeliveryDate,
            LocalDate deliveryDate,
            Boolean active,
            com.Ayurnexa.Ayurnexa.model.order.OrderAddress address) {}

    public record ProductSummary(
            String productId,
            String productName,
            String productDescription,
            String productCategory,
            String productWeight,
            String productType,
            String productImage,
            BigDecimal productPrice) {}

    public record ItemResponse(
            String orderItemId,
            String productId,
            int quantity,
            BigDecimal unitPrice,
            Integer rating,
            String feedback,
            ProductSummary product) {}

    public record Response(
            String orderId,
            String paymentMethod,
            String orderStatus,
            String orderProgress,
            BigDecimal price,
            LocalDate expectedDeliveryDate,
            LocalDate deliveryDate,
            com.Ayurnexa.Ayurnexa.model.order.OrderAddress address,
            List<ItemResponse> items) {}

    public record AdminUserSummary(
            String userId,
            String username,
            String email,
            String phoneNumber,
            com.Ayurnexa.Ayurnexa.model.User.Address address) {}

    public record AdminOrderResponse(
            String orderId,
            String paymentMethod,
            String orderStatus,
            String orderProgress,
            BigDecimal price,
            boolean active,
            LocalDate expectedDeliveryDate,
            LocalDate deliveryDate,
            com.Ayurnexa.Ayurnexa.model.order.OrderAddress address,
            List<ItemResponse> items,
            AdminUserSummary user,
            Instant createdAt,
            Instant updatedAt) {}
}