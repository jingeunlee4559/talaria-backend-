package com.example.talaria.backend.controller;

import com.example.talaria.backend.config.LivekitConfig;
import com.example.talaria.backend.service.LivekitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/livekit")
@RequiredArgsConstructor
public class LivekitController {
  private final LivekitService service;
  private final LivekitConfig cfg;

  public record TokenReq(String room, String identity, String name, Long ttlSec) {}
  public record TokenRes(String wsUrl, String token, String room, String identity) {}

  @PostMapping("/token")
  public TokenRes issue(@RequestBody TokenReq req) {
    long ttl = (req.ttlSec() != null ? req.ttlSec() : 600L); // 10분 기본
    String jwt = service.createJoinToken(req.room(), req.identity(), req.name(), ttl);
    return new TokenRes(cfg.getWsUrl(), jwt, req.room(), req.identity());
  }
}
