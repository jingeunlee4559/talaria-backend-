package com.example.talaria.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;

@Configuration
public class AppConfig {

    @Value("${talaria.aiBaseUrl:http://127.0.0.1:8000}")
    private String aiBaseUrl;

    // ⚠️ 기존 List<String> → String 으로 변경하고 직접 split 처리
    @Value("${talaria.cors.origins:*}")
    private String corsOriginsRaw;

    @Bean
    public WebClient aiWebClient() {
        return WebClient.builder()
                .baseUrl(aiBaseUrl)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // "http://localhost:3000,http://127.0.0.1:3000" 형태를 리스트로 분리
        List<String> origins = Arrays.stream(corsOriginsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        cfg.setAllowedOrigins(origins.isEmpty() ? List.of("*") : origins);
        cfg.addAllowedHeader(CorsConfiguration.ALL);
        cfg.addAllowedMethod(CorsConfiguration.ALL);
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
