package com.example.talaria.backend.service;

import com.example.talaria.backend.config.LivekitConfig;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomAdmin;
import io.livekit.server.RoomName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivekitAdminService {

  private final LivekitConfig cfg;
  private final RestTemplate rest = new RestTemplate();

  /** 현재 방 참가자 수(에러 시 0) */
  public int countParticipants(String room) {
    if (room == null || room.isBlank()) return 0;

    // ★ ws(s) → http(s) 자동 보정
    String apiBase = normalizeApiBase(cfg.getApiUrl(), cfg.getWsUrl());
    String url = apiBase + "/twirp/livekit.RoomService/ListParticipants";

    // ★ roomAdmin + room 지정 권한
    String jwt = buildRoomAdminJwt(room);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + jwt);

    HttpEntity<Map<String, Object>> req = new HttpEntity<>(Map.of("room", room), headers);

    try {
      log.info("[LK] ListParticipants url={} room={}", url, room);
      ResponseEntity<Map> resp = rest.exchange(url, HttpMethod.POST, req, Map.class);
      if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
        Object arr = resp.getBody().get("participants");
        if (arr instanceof List<?> list) return list.size();
      } else {
        log.warn("[LK] non-2xx: {} body={}", resp.getStatusCode(), resp.getBody());
      }
      return 0;
    } catch (RestClientResponseException e) {
      log.warn("[LK] HTTP {} body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
      return 0;
    } catch (Exception e) {
      log.warn("[LK] ListParticipants failed: {}", e.getMessage());
      return 0;
    }
  }

  /** 컨트롤러 호환용 (예전 메서드명) */
  public int countParticipantsByRoom(String room) {
    return countParticipants(room);
  }

  /** roomAdmin 권한(대상 room 지정) JWT */
  private String buildRoomAdminJwt(String room) {
    AccessToken at = new AccessToken(cfg.getApiKey(), cfg.getApiSecret());
    at.addGrants(new RoomAdmin(true), new RoomName(room));
    at.setTtl(60_000); // ms
    at.setIdentity("admin-" + room);
    at.setName("Talaria Admin");
    return at.toJwt();
  }

  /** apiUrl이 비었거나 ws(s):// 이더라도 안전하게 http(s) Base로 변환 */
  private static String normalizeApiBase(String apiUrl, String wsUrl) {
    String base = (apiUrl != null ? apiUrl.trim() : "");
    if (base.isEmpty() && wsUrl != null) base = wsUrl.trim();

    // ws(s) → http(s)
    if (base.startsWith("wss://")) base = "https://" + base.substring(6);
    else if (base.startsWith("ws://")) base = "http://" + base.substring(5);

    // 흔한 /rtc suffix 제거
    if (base.endsWith("/rtc")) base = base.substring(0, base.length() - 4);
    // 트레일링 슬래시 제거
    if (base.endsWith("/")) base = base.substring(0, base.length() - 1);

    return base;
  }
}
