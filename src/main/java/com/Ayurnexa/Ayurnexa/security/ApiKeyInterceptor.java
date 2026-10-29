package com.Ayurnexa.Ayurnexa.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private static final String API_KEY_HEADER = "X-API-Key";
    private final byte[] configuredKey;

    public ApiKeyInterceptor(@Value("${app.api-key}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API_KEY must be configured");
        }
        this.configuredKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String providedKey = request.getHeader(API_KEY_HEADER);
        if (providedKey == null || !MessageDigest.isEqual(
                configuredKey, providedKey.getBytes(StandardCharsets.UTF_8))) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "ApiKey");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid API key");
            return false;
        }
        return true;
    }
}