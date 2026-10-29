package com.Ayurnexa.Ayurnexa.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Ayurnexa.Ayurnexa.Service.RazorpayService;
import com.Ayurnexa.Ayurnexa.Webhook.RazorpayWebhookVerifier;

/**
 * Called by Razorpay's servers, not by our frontend.
 * Public URL to put in the Razorpay dashboard: https://<your-domain>/api/payments/razorpay/webhook
 *
 * This path must be allowed without login in Spring Security (and excluded from CSRF).
 * Trust comes from the signature check instead.
 */
@RestController
@RequestMapping("/api/payments/razorpay")
public class RazorpayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookController.class);

    private final RazorpayWebhookVerifier verifier;
    private final RazorpayService webhookService;

    public RazorpayWebhookController(RazorpayWebhookVerifier verifier, RazorpayService webhookService) {
        this.verifier = verifier;
        this.webhookService = webhookService;
    }

    // The body is taken as a raw String: the signature is computed over the exact bytes Razorpay sent.
    @PostMapping("/webhook")
    public ResponseEntity<String> receive(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        if (!verifier.isValid(payload, signature)) {
            log.warn("Rejected webhook with invalid signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid signature");
        }

        try {
            webhookService.handle(payload);
        } catch (IllegalArgumentException e) {
            // Malformed body: retrying won't help
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        // Any other exception (e.g. database down) becomes a 500, so Razorpay will retry later.

        return ResponseEntity.ok("ok");
    }
}