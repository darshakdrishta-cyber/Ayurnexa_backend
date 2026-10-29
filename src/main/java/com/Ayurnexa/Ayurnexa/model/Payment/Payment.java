package com.Ayurnexa.Ayurnexa.model.Payment;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "payments")
public class Payment {

    @Id
    private String id;

    @Indexed(unique = true, sparse = true)
    private String paymentId;          // Gateway Payment ID (e.g., pay_123456789)

    @Indexed(unique = true, sparse = true)
    private String paymentLinkId;      // Razorpay Payment Link ID (plink_...)

    @Indexed(unique = true)
    private String idempotencyKey;     // Prevents duplicate charges/requests

    @Indexed
    private String orderId;            // Links to Order.getOrderId() or Order.getId()

    @Indexed
    private String userId;             // Who made the payment

    private BigDecimal amount;         // Payment amount

    private String currency = "INR";   // Currency (INR, USD, etc.)

    private String paymentStatus;      // PENDING, SUCCESS, FAILED, REFUNDED

    private String paymentMethod;      // UPI, CARD, NETBANKING, WALLET

    private String paymentLinkUrl;     // URL if generated via a payment link service

    private String gatewayResponse;    // Optional: store raw/error response from gateway if needed

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public Payment() {}

    // Getters and Setters
    public String getId() { return id; }
    
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getPaymentLinkId() { return paymentLinkId; }
    public void setPaymentLinkId(String paymentLinkId) { this.paymentLinkId = paymentLinkId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public BigDecimal getamount() { return amount; }
    public void setamount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentLinkUrl() { return paymentLinkUrl; }
    public void setPaymentLinkUrl(String paymentLinkUrl) { this.paymentLinkUrl = paymentLinkUrl; }

    public String getGatewayResponse() { return gatewayResponse; }
    public void setGatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}