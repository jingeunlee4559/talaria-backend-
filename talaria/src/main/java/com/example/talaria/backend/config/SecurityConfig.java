package com.example.talaria.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .cors(Customizer.withDefaults()) // CorsConfigurationSource 또는 WebMvcConfigurer 사용 시 적용
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
        .anyRequest().permitAll()
      );
    return http.build();
  }
}
