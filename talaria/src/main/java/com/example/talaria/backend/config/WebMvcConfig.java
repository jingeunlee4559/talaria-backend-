package com.example.talaria.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
      .allowedOriginPatterns(
        "http://localhost:*", "https://localhost:*",
        "http://127.0.0.1:*", "https://127.0.0.1:*",
        "http://192.168.*:*", "https://192.168.*:*",
        "http://10.*:*",      "https://10.*:*"
      )
      .allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS")
      .allowedHeaders("*")
      .allowCredentials(true)
      .maxAge(3600);
  }
}

