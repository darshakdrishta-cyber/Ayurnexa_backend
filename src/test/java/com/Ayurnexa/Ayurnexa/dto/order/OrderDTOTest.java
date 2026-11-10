package com.Ayurnexa.Ayurnexa.dto.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class OrderDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsPaymentRecordIdForOrderCreation() throws Exception {
        OrderDTO.CreateRequest request = objectMapper.readValue(
                """
                {"paymentRecordId":"local-payment-id","items":[{"productId":"product-1","quantity":1}]}
                """,
                OrderDTO.CreateRequest.class);

        assertThat(request.paymentRecordId()).isEqualTo("local-payment-id");
    }

    @Test
    void acceptsLegacyPaymentIdAsPaymentRecordId() throws Exception {
        OrderDTO.CreateRequest request = objectMapper.readValue(
                """
                {"paymentId":"local-payment-id","items":[{"productId":"product-1","quantity":1}]}
                """,
                OrderDTO.CreateRequest.class);

        assertThat(request.paymentRecordId()).isEqualTo("local-payment-id");
    }
}
