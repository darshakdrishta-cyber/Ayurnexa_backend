package com.Ayurnexa.Ayurnexa.dto.product;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ProductDTO {

    private ProductDTO() {}

    public record CreateRequest(
            @NotBlank @Size(max = 120) String productName,
            @NotBlank @Size(max = 2000) String productDescription,
            @NotBlank @Size(max = 80) String productCategory,
            @NotBlank @Size(max = 40) String productWeight,
            @NotBlank @Size(max = 60) String form,
            @Size(max = 500) String productImage,
            @NotNull Boolean isActive,
            @NotNull @DecimalMin(value = "0.01") BigDecimal productPrice) {}

    public record UpdateRequest(
            @Size(max = 120) String productName,
            @Size(max = 2000) String productDescription,
            @Size(max = 80) String productCategory,
            @Size(max = 40) String productWeight,
            @Size(max = 60) String form,
            @Size(max = 500) String productImage,
            Boolean isActive,
            @DecimalMin(value = "0.01") BigDecimal productPrice) {}

    public record Response(
            String productId,
            String productName,
            String productDescription,
            String productCategory,
            String productWeight,
            String form,
            String productImage,
            Boolean isActive,
            BigDecimal productPrice,
            Instant createdAt,
            Instant updatedAt) {}
}
