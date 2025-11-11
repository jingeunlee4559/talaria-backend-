package com.example.talaria.backend.service;

import com.example.talaria.backend.config.LivekitConfig;
import io.livekit.server.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LivekitService {
  private final LivekitConfig cfg;

  /** 메타데이터 없이 토큰 발급 (기존) */
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

  /** 메타데이터 포함 토큰 발급 — JWT payload의 metadata에 meetings_id 포함 */
  public String createJoinTokenWithMeta(
      String room,
      String identity,
      String name,
      long ttlSeconds,
      String appLocale,     // 사용자 locale (예: ko-KR)
      String targetLocale,  // 회의 기본 locale (예: ko-KR)
      Long userId,
      Long meetingsId       // ✅ 추가: JWT metadata에 넣을 meetings_id
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

    // JWT payload의 "metadata" 클레임(JSON 문자열)에 meetings_id 포함 (snake_case)
    String metadataJson = String.format(
        "{" +
          "\"appLocale\":\"%s\"," +
          "\"targetLocale\":\"%s\"," +
          "\"username\":\"%s\"," +
          "\"userId\":%s," +
          "\"identity\":\"%s\"," +
          "\"meetings_id\":%s" +
        "}",
        safe(appLocale),
        safe(targetLocale),
        safe(name),
        (userId == null ? "null" : String.valueOf(userId)),
        safe(identity),
        (meetingsId == null ? "null" : String.valueOf(meetingsId))
    );
    at.setMetadata(metadataJson);

    return at.toJwt();
  }

  /** STT 봇용 (참고) */
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

  private static String safe(String s) {
    return (s == null) ? "" : s.replace("\"","\\\"");
  }
}
