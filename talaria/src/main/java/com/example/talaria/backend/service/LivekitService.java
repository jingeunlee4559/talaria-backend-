package com.example.talaria.backend.service;

import com.example.talaria.backend.config.LivekitConfig;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import io.livekit.server.CanPublish;
import io.livekit.server.CanSubscribe;
import io.livekit.server.CanPublishData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;                                  

@Service
@RequiredArgsConstructor
public class LivekitService {
  private final LivekitConfig cfg;

  /** 기존 메서드(그대로 유지) */
  public String createJoinToken(String room, String identity, String name, long ttlSeconds) {
    AccessToken at = new AccessToken(cfg.getApiKey(), cfg.getApiSecret());
    at.setIdentity(identity);
    at.setName(name);
    at.setTtl(ttlSeconds * 1000);
    at.addGrants(
        new RoomJoin(true),
        new RoomName(room),
        new CanPublish(true),
        new CanSubscribe(true),
        new CanPublishData(true)
    );
    return at.toJwt();
  }

  /** 메타데이터 포함 버전 (신규) */
  public String createJoinTokenWithMetadata(String room, String identity, String name, long ttlSeconds,
                                            Map<String, Object> metadata) {
    AccessToken at = new AccessToken(cfg.getApiKey(), cfg.getApiSecret());
    at.setIdentity(identity);
    at.setName(name);
    at.setTtl(ttlSeconds * 1000);
    at.addGrants(
        new RoomJoin(true),
        new RoomName(room),
        new CanPublish(true),
        new CanSubscribe(true),
        new CanPublishData(true)
    );
    if (metadata != null && !metadata.isEmpty()) {
      try {
        String json = new ObjectMapper().writeValueAsString(metadata);
        at.setMetadata(json); // LiveKit 토큰 metadata에 주입
      } catch (Exception ignore) {}
    }
    return at.toJwt();
  }

  /** STT 봇용 예시 (수신 전용, 필요시 동일 패턴으로 오버로드 추가 가능) */
  public String createBotToken(String room, String identity, String name, long ttlSeconds) {
    AccessToken at = new AccessToken(cfg.getApiKey(), cfg.getApiSecret());
    at.setIdentity(identity);
    at.setName(name);
    at.setTtl(ttlSeconds * 1000);
    at.addGrants(
        new RoomJoin(true),
        new RoomName(room),
        new CanPublish(false),
        new CanSubscribe(true),
        new CanPublishData(false)
    );
    return at.toJwt();
  }
}
