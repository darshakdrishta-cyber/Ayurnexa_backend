package com.Ayurnexa.Ayurnexa.dto.offer;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public final class OfferDTO {

    private OfferDTO() {}

    public record CreateRequest(
            @NotBlank(message = "offername is required")
            String offername,

            @NotBlank(message = "offerlink is required")
            String offerlink,

            String additionallink,

            @JsonProperty("is_active")
            Boolean isActive
    ) {}

    public record UpdateRequest(
            String offername,
            String offerlink,
            String additionallink,
            @JsonProperty("is_active")
            Boolean isActive
    ) {}

    public record Response(
            String offerId,
            String offername,
            String offerlink,
            String additionallink,
            @JsonProperty("is_active")
            boolean isActive,
            Instant offercreated,
            Instant offerdeleted,
            Instant updatedAt
    ) {}
}
