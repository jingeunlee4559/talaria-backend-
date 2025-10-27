package com.example.talaria.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.*;

import java.util.Arrays;
import java.util.List;

@Configuration
public class AppConfig {

    @Value("${talaria.aiBaseUrl:http://127.0.0.1:8000}")
    private String aiBaseUrl;

    // ⚠️ 기존 List<String> → String 으로 변경하고 직접 split 처리
    @Value("${talaria.cors.origins:*}")
    private String corsOriginsRaw;

    // @Bean
    // public WebClient aiWebClient() {
    //     return WebClient.builder()
    //             .baseUrl(aiBaseUrl)
    //             .build();
    // }

}
