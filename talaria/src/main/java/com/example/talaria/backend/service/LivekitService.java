package com.example.talaria.backend.service;

import com.example.talaria.backend.config.LivekitConfig;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import io.livekit.server.CanPublish;
import io.livekit.server.CanSubscribe;
import io.livekit.server.CanPublishData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LivekitService {
  private final LivekitConfig cfg;

  /** LiveKit 참가 토큰 생성 (권한을 세부적으로 명시) */
  public String createJoinToken(String room, String identity, String name, long ttlSeconds) {
    AccessToken at = new AccessToken(cfg.getApiKey(), cfg.getApiSecret());
    at.setIdentity(identity);
    at.setName(name);
    at.setTtl(ttlSeconds * 1000);

    // 권한(Grant)들을 개별 클래스로 추가
    // - RoomJoin / RoomName 은 룸 참가용
    // - CanPublish / CanSubscribe / CanPublishData 는 권한 제어용
    at.addGrants(
        new RoomJoin(true),
        new RoomName(room),
        new CanPublish(true),
        new CanSubscribe(true),
        new CanPublishData(true)
    );
    String jwt = at.toJwt();
    System.out.println("[TOKEN] " + at.toJwt().substring(0,50) + "...");
    System.out.println("[LIVEKIT JWT FULL] " + jwt);
    return at.toJwt();
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
}
