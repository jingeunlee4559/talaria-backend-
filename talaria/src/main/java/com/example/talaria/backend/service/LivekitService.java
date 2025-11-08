package com.example.talaria.backend.service;

import com.example.talaria.backend.config.LivekitConfig;
import io.livekit.server.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class LivekitService {
  private final LivekitConfig cfg;

  /** 기존: 메타데이터 없이 토큰 발급 */
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

  /** 신규: 메타데이터 포함 토큰 발급 (locale 등) */
  public String createJoinTokenWithMeta(
      String room,
      String identity,
      String name,
      long ttlSeconds,
      String appLocale,     // 사용자 locale (예: ko-KR)
      String targetLocale,  // 회의 대상/기본 locale (예: ko-KR)
      Long userId
  ) {
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

    // LiveKit AccessToken의 metadata 클레임(문자열) 세팅
    // SDK에 setMetadata(String) 가 있는 버전 사용 가정
    String metadataJson = String.format(
        "{\"appLocale\":\"%s\",\"targetLocale\":\"%s\",\"username\":\"%s\",\"userId\":%s,\"identity\":\"%s\"}",
        safe(appLocale), safe(targetLocale), safe(name),
        (userId == null ? "null" : String.valueOf(userId)),
        safe(identity)
    );
    at.setMetadata(metadataJson);

    String jwt = at.toJwt();
    System.out.println("[LIVEKIT JWT WITH META] " + jwt);
    return jwt;
  }

  /** STT 봇용 예시 (수신 전용) */
  public String createBotToken(String room, String identity, String name, long ttlSeconds) {
    AccessToken at = new AccessToken(cfg.getApiKey(), cfg.getApiSecret());
    at.setIdentity(identity);
    at.setName(name);
    at.setTtl(ttlSeconds * 1000);

    at.addGrants(
        new RoomJoin(true),
        new RoomName(room),
        new CanPublish(false),      // 발언 금지
        new CanSubscribe(true),     // 수신 허용
        new CanPublishData(false)   // 데이터 채널 금지 (필요시 true)
    );

    return at.toJwt();
  }

  private static String safe(String s) {
    return (s == null) ? "" : s.replace("\"","\\\"");
  }
}
