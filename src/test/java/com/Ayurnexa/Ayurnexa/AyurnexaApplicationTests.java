package com.Ayurnexa.Ayurnexa;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"app.jwt.secret=test-only-secret-key-with-at-least-32-bytes",
		"app.api-key=test-only-api-key",
		"app.cors.allowed-origins=http://localhost:3000",
		"razorpay.key-id=test-key-id",
		"razorpay.key-secret=test-key-secret",
		"razorpay.webhook-secret=test-webhook-secret"
})
class AyurnexaApplicationTests {

	@Test
	void contextLoads() {
	}

}
