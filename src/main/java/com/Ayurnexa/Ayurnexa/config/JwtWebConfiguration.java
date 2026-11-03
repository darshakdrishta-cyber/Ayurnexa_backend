package com.Ayurnexa.Ayurnexa.config;

import com.Ayurnexa.Ayurnexa.security.JwtAnnotationInterceptor;
import com.Ayurnexa.Ayurnexa.security.ApiKeyInterceptor;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class JwtWebConfiguration implements WebMvcConfigurer {

    private final JwtAnnotationInterceptor jwtAnnotationInterceptor;
    private final ApiKeyInterceptor apiKeyInterceptor;
    private final String[] allowedOriginPatterns;

    public JwtWebConfiguration(JwtAnnotationInterceptor jwtAnnotationInterceptor,
                               ApiKeyInterceptor apiKeyInterceptor,
                               @Value("${app.cors.allowed-origins:*}") String allowedOrigins) {
        this.jwtAnnotationInterceptor = jwtAnnotationInterceptor;
        this.apiKeyInterceptor = apiKeyInterceptor;
        List<String> origins = new ArrayList<>();
        for (String origin : allowedOrigins.split(",")) {
            String trimmedOrigin = origin.trim();
            if (!trimmedOrigin.isEmpty()) {
                origins.add(trimmedOrigin);
            }
        }
        this.allowedOriginPatterns = origins.toArray(String[]::new);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/payments/razorpay/webhook");
        registry.addInterceptor(jwtAnnotationInterceptor);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("Authorization", "Content-Type", "X-API-Key", "X-Idempotency-Key")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(java.util.Arrays.asList(allowedOriginPatterns));
        configuration.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(java.util.Arrays.asList("Authorization", "Content-Type", "X-API-Key", "X-Idempotency-Key"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}