package com.Ayurnexa.Ayurnexa.Service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.Ayurnexa.Ayurnexa.dto.payment.PaymentDTO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.Ayurnexa.Ayurnexa.model.Payment.Payment;
import com.Ayurnexa.Ayurnexa.repository.PaymentRepository;

@Service
public class PaymentService {

    // Payment statuses
    public static final String PENDING = "PENDING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String REFUNDED = "REFUNDED";

    // Payment methods supported right now
    public static final String COD = "COD";
    public static final String UPI = "UPI";

    private final PaymentRepository paymentRepository;
    private final PayuPaymentService payuPaymentService;

    public PaymentService(PaymentRepository paymentRepository, PayuPaymentService payuPaymentService) {
        this.paymentRepository = paymentRepository;
        this.payuPaymentService = payuPaymentService;
    }

    /**
     * Creates and saves a payment for an order.
     *
     * COD -> saved as PENDING (cash is collected on delivery; mark SUCCESS then).
     * UPI -> saved as PENDING, a PayU hosted payment link is created and stored in paymentLinkUrl.
     *
     * IMPORTANT: "amount" must be the order total read from the database by the caller,
     * never a value sent by the client.
     *
     * Calling again with the same idempotencyKey returns the existing payment instead of
     * creating a second one. To retry after a FAILED payment, use a new idempotencyKey.
     */
    public PaymentDTO.Response createPayment(String userId, PaymentDTO.CreateRequest request) {
        String orderId = request.orderId();
        BigDecimal amount = request.amount();
        String paymentMethod = request.paymentMethod();
        String idempotencyKey = request.idempotencyKey();

        // ---- validate ----
        requireText(userId, "userId");
        requireText(idempotencyKey, "idempotencyKey");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        requireText(paymentMethod, "paymentMethod");
        String method = paymentMethod.trim().toUpperCase(Locale.ROOT);
        if (!COD.equals(method) && !UPI.equals(method)) {
            throw new IllegalArgumentException("Unsupported payment method: " + paymentMethod
                    + " (supported: COD, UPI)");
        }

        // ---- idempotency: same key -> same payment ----
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return toResponse(ownedBy(existing.get(), userId));
        }

        if (paymentRepository.existsByOrderIdAndPaymentStatus(orderId, SUCCESS)) {
            throw new IllegalStateException("Order " + orderId + " is already paid");
        }

        // ---- save as PENDING first so the payment has a stable local reference ----
        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setOrderId(orderId);
        payment.setamount(amount);
        payment.setCurrency("INR");
        payment.setPaymentMethod(method);
        payment.setPaymentStatus(PENDING);
        payment.setIdempotencyKey(idempotencyKey);

        try {
            payment = paymentRepository.save(payment);
        } catch (DuplicateKeyException e) {
            // Two identical requests raced; the unique index on idempotencyKey let only one in.
            return toResponse(ownedBy(paymentRepository.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> e), userId));
        }

        if (COD.equals(method)) {
            return toResponse(payment);
        }

        // ---- online: create the PayU hosted payment link ----
        try {
            PayuPaymentService.PaymentLinkResult link = payuPaymentService.createPaymentLink(
                    amount, "Payment for order " + orderId);
            payment.setGatewayReferenceId(link.invoiceNumber());
            payment.setPaymentLinkUrl(link.paymentLink());
            return toResponse(paymentRepository.save(payment));
        } catch (PayuPaymentService.PayuException e) {
            payment.setPaymentStatus(FAILED);
            payment.setGatewayResponse(e.getMessage());
            paymentRepository.save(payment);
            throw e;
        }
    }

    // ---------- reads ----------

    public PaymentDTO.Response getPaymentForUser(String userId, String id) {
        return toResponse(ownedBy(findPayment(id), userId));
    }

    public List<PaymentDTO.Response> getPaymentsByUser(String userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(PaymentService::toResponse)
                .toList();
    }

    public PaymentDTO.Response updatePayment(String userId, String id, PaymentDTO.UpdateRequest request) {
        Payment payment = ownedBy(findPayment(id), userId);
        if (!PENDING.equals(payment.getPaymentStatus()) || hasText(payment.getPaymentLinkUrl())) {
            throw new IllegalStateException("Only pending payments without a PayU payment link can be edited");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        String method = request.paymentMethod().trim().toUpperCase(Locale.ROOT);
        if (!COD.equals(method) && !UPI.equals(method)) {
            throw new IllegalArgumentException("Unsupported payment method: " + request.paymentMethod());
        }

        payment.setamount(request.amount());
        payment.setPaymentMethod(method);
        if (UPI.equals(method)) {
            PayuPaymentService.PaymentLinkResult link = payuPaymentService.createPaymentLink(
                    payment.getamount(), "Payment " + payment.getId());
            payment.setGatewayReferenceId(link.invoiceNumber());
            payment.setPaymentLinkUrl(link.paymentLink());
        }
        return toResponse(paymentRepository.save(payment));
    }

    public void deletePayment(String userId, String id) {
        Payment payment = ownedBy(findPayment(id), userId);
        if (!PENDING.equals(payment.getPaymentStatus()) || hasText(payment.getPaymentLinkUrl())) {
            throw new IllegalStateException("Only pending payments without a PayU payment link can be deleted");
        }
        paymentRepository.delete(payment);
    }

    public PaymentDTO.Response verifyPayuPayment(String userId, String id) {
        Payment payment = ownedBy(findPayment(id), userId);
        if (!UPI.equals(payment.getPaymentMethod()) || !hasText(payment.getGatewayReferenceId())) {
            throw new IllegalStateException("Payment does not have a PayU payment link");
        }
        if (SUCCESS.equals(payment.getPaymentStatus())) {
            return toResponse(payment);
        }

        PayuPaymentService.PaymentLinkStatus status =
                payuPaymentService.getPaymentLinkStatus(payment.getGatewayReferenceId());
        if (payment.getamount().compareTo(status.totalAmountCollected()) == 0) {
            payment.setPaymentStatus(SUCCESS);
            payment.setGatewayResponse("PayU payment link status: " + status.status());
            payment = paymentRepository.save(payment);
        }
        return toResponse(payment);
    }

    // ---------- helpers ----------

    private static Payment ownedBy(Payment payment, String userId) {
        if (!userId.equals(payment.getUserId())) {
            // same message as "not found" so we don't reveal that the payment exists
            throw new NoSuchElementException("Payment not found");
        }
        return payment;
    }

    public Payment findPayment(String id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Payment not found"));
    }

    public static PaymentDTO.Response toResponse(Payment payment) {
        return new PaymentDTO.Response(
            payment.getId(), payment.getPaymentId(), payment.getGatewayReferenceId(), payment.getOrderId(), payment.getamount(),
                payment.getCurrency(), payment.getPaymentStatus(), payment.getPaymentMethod(),
                payment.getPaymentLinkUrl(), payment.getCreatedAt(), payment.getUpdatedAt());
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}