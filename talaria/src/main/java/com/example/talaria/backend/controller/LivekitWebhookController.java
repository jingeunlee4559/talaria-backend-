package com.example.talaria.backend.controller;

import com.example.talaria.backend.config.LivekitConfig;
import io.livekit.server.WebhookReceiver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/livekit")
@RequiredArgsConstructor
public class LivekitWebhookController {
  private final LivekitConfig cfg;

  @PostMapping(value = "/webhook", consumes = "application/webhook+json")
  public ResponseEntity<Void> webhook(@RequestBody String body,
                                      @RequestHeader("Authorization") String authHeader) {
    WebhookReceiver receiver = new WebhookReceiver(cfg.getApiKey(), cfg.getApiSecret());
    var event = receiver.receive(body, authHeader); // 서명 검증 + 파싱
    // switch(event.getEvent()) { ... } // room_started, participant_joined 등 처리
    return ResponseEntity.ok().build();
  }
}
