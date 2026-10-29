package com.Ayurnexa.Ayurnexa.Webhook;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Checks that a webhook really came from Razorpay.
 * Razorpay sends X-Razorpay-Signature = HMAC-SHA256(raw request body, webhook secret) as hex.
 *
 * The webhook secret is the one you type in when creating the webhook in the Razorpay dashboard.
 * It is NOT the same as razorpay.key-secret.
 *   razorpay.webhook-secret=xxxxx
 */
@Component
public class RazorpayWebhookVerifier {

    private final byte[] secret;

    public RazorpayWebhookVerifier(@Value("${razorpay.webhook-secret}") String webhookSecret) {
        this.secret = webhookSecret.getBytes(StandardCharsets.UTF_8);
    }

    /** @param payload the raw request body, exactly as received (not re-serialized JSON) */
    public boolean isValid(String payload, String signature) {
        if (payload == null || signature == null || signature.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));

            // constant-time comparison
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not verify webhook signature", e);
        }
    }
}