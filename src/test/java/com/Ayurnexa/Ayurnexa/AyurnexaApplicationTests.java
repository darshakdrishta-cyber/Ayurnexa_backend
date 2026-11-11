package com.Ayurnexa.Ayurnexa;

import com.Ayurnexa.Ayurnexa.Service.ProductService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"app.jwt.secret=test-only-secret-key-with-at-least-32-bytes",
		"app.api-key=test-only-api-key",
		"app.cors.allowed-origins=*",
		"payu.client-id=test-client-id",
		"payu.client-secret=test-client-secret",
		"payu.merchant-id=test-merchant-id",
		"payu.environment=uat"
})
@AutoConfigureMockMvc
class AyurnexaApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductService productService;

	@Test
	void contextLoads() {
	}

	@Test
	void productListDoesNotRequireApiKeyOrJwt() throws Exception {
		when(productService.listActive()).thenReturn(List.of());

		mockMvc.perform(MockMvcRequestBuilders.get("/api/products"))
				.andExpect(status().isOk());
	}

	@Test
	void uploadPreflightAllowsViteFrontend() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.options("/api/uploads")
				.header(HttpHeaders.ORIGIN, "http://localhost:5173")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type,x-api-key"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
	}

	@Test
	void uploadPreflightAllowsAnyOrigin() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.options("/api/uploads")
				.header(HttpHeaders.ORIGIN, "https://unrelated.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type,x-api-key"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://unrelated.example"));
	}

	@Test
	void publicUploadPreflightAllowsImageFetch() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.options("/api/uploads/public/random-token")
				.header(HttpHeaders.ORIGIN, "http://localhost:5173")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name()))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
						org.hamcrest.Matchers.containsString("GET")));
	}

	@Test
	void uploadRequiresApiKeyBeforeJwt() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/uploads")
				.file(new MockMultipartFile("file", "image.jpg", "image/jpeg", new byte[] {1}))
				.header("Authorization", "Bearer invalid-token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void uploadDoesNotRequireJwtWhenApiKeyIsValid() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/uploads")
				.file(new MockMultipartFile("file", "image.jpg", "image/jpeg", new byte[0]))
				.header("X-API-Key", "test-only-api-key"))
				.andExpect(status().isBadRequest());
	}

}
