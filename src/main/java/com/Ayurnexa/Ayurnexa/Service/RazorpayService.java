package com.Ayurnexa.Ayurnexa.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

import com.Ayurnexa.Ayurnexa.model.Payment.Payment;
import com.Ayurnexa.Ayurnexa.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class RazorpayService {

    private static final String PAYMENT_LINKS_URL = "https://api.razorpay.com/v1/payment_links";

    private final String keyId;
    private final String keySecret;
    private final String callbackUrl;
    private final long linkExpiryMinutes;
    private final RestTemplate restTemplate;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    public RazorpayService(
            @Value("${razorpay.key-id}") String keyId,
            @Value("${razorpay.key-secret}") String keySecret,
            @Value("${razorpay.callback-url:}") String callbackUrl,
            @Value("${razorpay.link-expiry-minutes:30}") long linkExpiryMinutes,
            PaymentRepository paymentRepository,
            ObjectMapper objectMapper) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.callbackUrl = callbackUrl;
        this.linkExpiryMinutes = linkExpiryMinutes;
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(15_000);
        this.restTemplate = new RestTemplate(factory);
    }

    public PaymentLinkResult createUpiPaymentLink(
            String referenceId,
            BigDecimal amount,
            String currency,
            String description,
            String customerName,
            String customerContact,
            String customerEmail) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", toMinorUnits(amount));
        body.put("currency", currency);
        body.put("accept_partial", false);
        body.put("reference_id", referenceId);
        body.put("description", description);
        body.put("notify", Map.of("sms", false, "email", false));
        body.put("reminder_enable", false);
        body.put("expire_by", Instant.now().plus(linkExpiryMinutes, ChronoUnit.MINUTES).getEpochSecond());

        if (hasText(callbackUrl)) {
            body.put("callback_url", callbackUrl);
            body.put("callback_method", "get");
        }

        Map<String, Object> customer = new LinkedHashMap<>();
        if (hasText(customerName)) customer.put("name", customerName);
        if (hasText(customerContact)) customer.put("contact", customerContact);
        if (hasText(customerEmail)) customer.put("email", customerEmail);
        if (!customer.isEmpty()) body.put("customer", customer);

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(keyId, keySecret);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            Map<?, ?> response = restTemplate.postForObject(
                    PAYMENT_LINKS_URL,
                    new HttpEntity<>(body, headers),
                    Map.class);
            Object shortUrlValue = response == null ? null : response.get("short_url");
            if (!(shortUrlValue instanceof String shortUrl) || shortUrl.isBlank()) {
                throw new RazorpayException(
                        "Razorpay returned no payment link",
                        response == null ? null : String.valueOf(response),
                        null);
            }

            return new PaymentLinkResult(
                    stringValue(response == null ? null : response.get("id")),
                    shortUrl,
                    stringValue(response == null ? null : response.get("status")),
                    String.valueOf(response));
        } catch (HttpStatusCodeException exception) {
            throw new RazorpayException(
                    "Razorpay rejected the request (" + exception.getStatusCode() + ")",
                    exception.getResponseBodyAsString(),
                    exception);
        } catch (RestClientException exception) {
            throw new RazorpayException(
                    "Could not reach Razorpay: " + exception.getMessage(),
                    null,
                    exception);
        }
    }

    private static long toMinorUnits(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public record PaymentLinkResult(String linkId, String shortUrl, String status, String rawResponse) {}

    public static class RazorpayException extends RuntimeException {
        private final String responseBody;

        public RazorpayException(String message, String responseBody, Throwable cause) {
            super(message, cause);
            this.responseBody = responseBody;
        }

        public String getResponseBody() {
            return responseBody;
        }
    }

    public void handle(String payload) {
        final JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid Razorpay webhook JSON", exception);
        }

        String event = root.path("event").asText();
        if (!"payment_link.paid".equals(event)) {
            return;
        }

        JsonNode linkEntity = root.path("payload").path("payment_link").path("entity");
        JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
        String linkId = linkEntity.path("id").asText();
        String referenceId = linkEntity.path("reference_id").asText();
        String gatewayPaymentId = paymentEntity.path("id").asText();
        if (!hasText(linkId) || !hasText(referenceId) || !hasText(gatewayPaymentId)) {
            throw new IllegalArgumentException("Razorpay payment_link.paid webhook is missing required IDs");
        }

        Payment payment = paymentRepository.findById(referenceId)
                .orElseThrow(() -> new IllegalArgumentException("Payment reference not found"));
        if (!linkId.equals(payment.getPaymentLinkId())) {
            throw new IllegalArgumentException("Razorpay payment link does not match payment record");
        }

        long amountPaidMinor = linkEntity.path("amount_paid").asLong(-1);
        long expectedAmountMinor = toMinorUnits(payment.getamount());
        if (amountPaidMinor != expectedAmountMinor) {
            throw new IllegalArgumentException("Razorpay paid amount does not match payment amount");
        }

        if (PaymentService.SUCCESS.equals(payment.getPaymentStatus())) {
            if (gatewayPaymentId.equals(payment.getPaymentId())) return;
            throw new IllegalArgumentException("Payment record is already completed with another payment ID");
        }
        if (!PaymentService.PENDING.equals(payment.getPaymentStatus())) {
            throw new IllegalArgumentException("Payment record is not pending");
        }

        payment.setPaymentId(gatewayPaymentId);
        payment.setPaymentMethod(paymentEntity.path("method").asText().toUpperCase(java.util.Locale.ROOT));
        payment.setPaymentStatus(PaymentService.SUCCESS);
        payment.setGatewayResponse(payload);
        paymentRepository.save(payment);
    }
}
