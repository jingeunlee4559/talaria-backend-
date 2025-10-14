package com.example.talaria.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "livekit")
@Getter @Setter
public class LivekitConfig {
  private String wsUrl;   // 프론트 접속용 (wss)
  private String apiUrl;  // 서버 API용 (https)
  private String apiKey;
  private String apiSecret;
}
