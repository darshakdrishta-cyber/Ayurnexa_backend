package com.Ayurnexa.Ayurnexa.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class PayuPaymentService {

    private static final String UAT_TOKEN_URL = "https://uat-accounts.payu.in/oauth/token";
    private static final String LIVE_TOKEN_URL = "https://accounts.payu.in/oauth/token";
    private static final String UAT_PAYMENT_LINKS_URL = "https://uatoneapi.payu.in/payment-links/";
    private static final String LIVE_PAYMENT_LINKS_URL = "https://oneapi.payu.in/payment-links/";

    private final String clientId;
    private final String clientSecret;
    private final String merchantId;
    private final String tokenUrl;
    private final String paymentLinksUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private String accessToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public PayuPaymentService(
            @Value("${payu.client-id:}") String clientId,
            @Value("${payu.client-secret:}") String clientSecret,
            @Value("${payu.merchant-id:}") String merchantId,
            @Value("${payu.environment:uat}") String environment,
            ObjectMapper objectMapper) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.merchantId = merchantId;
        this.objectMapper = objectMapper;

        if ("uat".equalsIgnoreCase(environment)) {
            this.tokenUrl = UAT_TOKEN_URL;
            this.paymentLinksUrl = UAT_PAYMENT_LINKS_URL;
        } else if ("production".equalsIgnoreCase(environment)) {
            this.tokenUrl = LIVE_TOKEN_URL;
            this.paymentLinksUrl = LIVE_PAYMENT_LINKS_URL;
        } else {
            throw new IllegalArgumentException("PAYU_ENVIRONMENT must be uat or production");
        }

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(15_000);
        this.restTemplate = new RestTemplate(factory);
    }

    public PaymentLinkResult createPaymentLink(BigDecimal amount, String description) {
        requireConfiguration();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("subAmount", amount);
        body.put("isPartialPaymentAllowed", false);
        body.put("description", description);
        body.put("source", "API");

        HttpHeaders headers = paymentHeaders();
        try {
            String responseBody = restTemplate.postForObject(
                    paymentLinksUrl,
                    new HttpEntity<>(body, headers),
                    String.class);
            JsonNode result = parseResult(responseBody);
            String invoiceNumber = result.path("invoiceNumber").asText();
            String paymentLink = result.path("paymentLink").asText();
            if (invoiceNumber.isBlank() || paymentLink.isBlank()) {
                throw new PayuException("PayU did not return a payment link");
            }
            return new PaymentLinkResult(invoiceNumber, paymentLink);
        } catch (HttpStatusCodeException exception) {
            throw new PayuException(
                    "PayU rejected the payment-link request (HTTP " + exception.getStatusCode().value() + ")",
                    exception);
        } catch (RestClientException exception) {
            throw new PayuException("Could not reach PayU payment links", exception);
        }
    }

    public PaymentLinkStatus getPaymentLinkStatus(String invoiceNumber) {
        requireConfiguration();
        String encodedInvoice = UriUtils.encodePathSegment(invoiceNumber, java.nio.charset.StandardCharsets.UTF_8);
        URI uri = URI.create(paymentLinksUrl + encodedInvoice);
        try {
            String responseBody = restTemplate.exchange(
                    uri,
                    org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(paymentHeaders()),
                    String.class).getBody();
            JsonNode result = parseResult(responseBody);
            BigDecimal collected = decimalValue(result.path("totalAmountCollected"));
            String status = result.path("status").asText();
            return new PaymentLinkStatus(status, collected);
        } catch (HttpStatusCodeException exception) {
            throw new PayuException(
                    "PayU rejected the payment-status request (HTTP " + exception.getStatusCode().value() + ")",
                    exception);
        } catch (RestClientException exception) {
            throw new PayuException("Could not reach PayU payment links", exception);
        }
    }

    private HttpHeaders paymentHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());
        headers.set("merchantId", merchantId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private synchronized String getAccessToken() {
        if (accessToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return accessToken;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("grant_type", "client_credentials");
        form.add("scope", "create_payment_links read_payment_links");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try {
            String responseBody = restTemplate.postForObject(
                    tokenUrl,
                    new HttpEntity<>(form, headers),
                    String.class);
            if (responseBody == null || responseBody.isBlank()) {
                throw new PayuException("PayU OAuth returned an empty response");
            }
            JsonNode response = objectMapper.readTree(responseBody);
            String newToken = response.path("access_token").asText();
            long expiresIn = response.path("expires_in").asLong(0);
            if (newToken.isBlank() || expiresIn <= 0) {
                throw new PayuException("PayU OAuth response did not contain a valid access token");
            }
            accessToken = newToken;
            tokenExpiresAt = Instant.now().plusSeconds(Math.max(1, expiresIn - 30));
            return accessToken;
        } catch (HttpStatusCodeException exception) {
            throw new PayuException(
                    "PayU OAuth authentication failed (HTTP " + exception.getStatusCode().value() + ")",
                    exception);
        } catch (RestClientException exception) {
            throw new PayuException("Could not reach PayU OAuth", exception);
        } catch (tools.jackson.core.JacksonException exception) {
            throw new PayuException("PayU OAuth returned an invalid response", exception);
        }
    }

    private JsonNode parseResult(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new PayuException("PayU returned an empty response");
        }
        try {
            JsonNode response = objectMapper.readTree(responseBody);
            if (response.path("status").canConvertToInt() && response.path("status").asInt() != 0) {
                throw new PayuException("PayU reported that the request failed");
            }
            JsonNode result = response.path("result");
            if (!result.isObject()) {
                throw new PayuException("PayU response did not contain a result");
            }
            return result;
        } catch (tools.jackson.core.JacksonException exception) {
            throw new PayuException("PayU returned an invalid response", exception);
        }
    }

    private void requireConfiguration() {
        if (clientId.isBlank() || clientSecret.isBlank() || merchantId.isBlank()) {
            throw new PayuException(
                    "PayU is not configured; set PAYU_CLIENT_ID, PAYU_CLIENT_SECRET, and PAYU_MERCHANT_ID");
        }
    }

    private static BigDecimal decimalValue(JsonNode value) {
        if (value.isNumber()) {
            return value.decimalValue();
        }
        if (value.isTextual()) {
            try {
                return new BigDecimal(value.asText());
            } catch (NumberFormatException exception) {
                throw new PayuException("PayU returned an invalid collected amount", exception);
            }
        }
        return BigDecimal.ZERO;
    }

    public record PaymentLinkResult(String invoiceNumber, String paymentLink) {}

    public record PaymentLinkStatus(String status, BigDecimal totalAmountCollected) {}

    public static class PayuException extends RuntimeException {
        public PayuException(String message) {
            super(message);
        }

        public PayuException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
