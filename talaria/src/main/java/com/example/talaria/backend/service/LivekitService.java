package com.example.talaria.backend.service;

import com.example.talaria.backend.config.LivekitConfig;
import io.livekit.server.AccessToken;
import io.livekit.server.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LivekitService {
  private final LivekitConfig cfg;

  /** LiveKit 참가 토큰 생성 */
  public String createJoinToken(String room, String identity, String name, long ttlSeconds) {
    AccessToken at = new AccessToken(cfg.getApiKey(), cfg.getApiSecret());
    at.setIdentity(identity);
    at.setName(name);
    at.setTtl(ttlSeconds * 1000);
    at.addGrants(new RoomJoin(true), new RoomName(room));
    return at.toJwt();
  }
}
