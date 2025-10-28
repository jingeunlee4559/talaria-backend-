// src/main/java/com/example/talaria/backend/service/meeting/MeetingService.java
package com.example.talaria.backend.service.meeting;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.mapper.meeting.MeetingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingService {

  private final MeetingMapper mapper;

  private static final String TABLE = "tl_meetings";
  private static final String PK    = "meetings_id";
  private static final String DEFAULT_STATUS = "SCHEDULED";

  // ===== 조회 =====
  public PageResponse listByHost(long hostId, String status, LocalDateTime from, LocalDateTime to,
                                 int page, int size) {
    if (size > 100) size = 100;
    int offset = page * size;
    long total = mapper.countByHost(TABLE, hostId, status, from, to);
    List<Map<String,Object>> items =
        mapper.selectPageByHost(TABLE, hostId, status, from, to, size, offset);
    return new PageResponse(items, page, size, total);
  }

  public Map<String,Object> getOneByHost(long hostId, long id) {
    return mapper.selectOneByHost(TABLE, PK, hostId, id);
  }

  public Map<String,Object> getByInviteCode(String code) {
    return mapper.selectByInviteCode(TABLE, code);
  }

  /** 최근 window 내 동일 타이틀 + 진행/예정 회의 하나 재사용용 */
  public Map<String,Object> findRecentActiveByHostAndTitle(long hostId, String title, Duration window) {
    LocalDateTime since = LocalDateTime.now().minus(window);
    return mapper.selectRecentActiveByHostAndTitle(TABLE, hostId, title, since);
  }

  // ===== 생성 계열 =====

  /** 백엔드가 invite_code를 생성해서 새 회의 생성 */
  @Transactional
  public Map<String,Object> createNew(long hostId, String title, String defaultLang, LocalDateTime scheduledAt) {
    String invite = generateInviteCode(10);
    for (int i = 0; i < 10; i++) {
      try {
        mapper.insert(TABLE, hostId, title, DEFAULT_STATUS, invite, defaultLang, scheduledAt);
        long id = mapper.lastInsertId();
        return getOneByHost(hostId, id);
      } catch (DataIntegrityViolationException dup) {
        // invite_code UNIQUE 충돌 시 재시도
        invite = generateInviteCode(10);
      }
    }
    throw new IllegalStateException("Failed to generate unique invite_code");
  }

  /** 프론트/다른 로직에서 정한 invite_code로 '있으면 반환, 없으면 생성' */
  @Transactional
  public Map<String,Object> createIfAbsentByInviteCode(long hostId,
                                                       String title,
                                                       String defaultLang,
                                                       LocalDateTime scheduledAt,
                                                       String inviteCode) {
    // 1) 존재 시 바로 반환
    Map<String,Object> exist = mapper.selectByInviteCode(TABLE, inviteCode);
    if (exist != null) return exist;

    // 2) 없으면 생성 시도(경합 시 재조회)
    try {
      mapper.insert(TABLE, hostId, title, DEFAULT_STATUS, inviteCode, defaultLang, scheduledAt);
      long id = mapper.lastInsertId();
      return getOneByHost(hostId, id);
    } catch (DataIntegrityViolationException race) {
      Map<String,Object> again = mapper.selectByInviteCode(TABLE, inviteCode);
      if (again != null) return again;
      throw race;
    }
  }

  /**
   * 최근 window 내 동일 타이틀의 진행/예정 회의가 있으면 그 회의를 재사용,
   * 없으면 백엔드가 새 invite_code를 만들어 생성.
   * - LiveKit 토큰 발급 시 매번 DB가 새로 생기는 문제를 해소.
   */
  @Transactional
  public Map<String,Object> createOrReuse(long hostId,
                                          String title,
                                          String defaultLang,
                                          LocalDateTime scheduledAt,
                                          Duration reuseWindow) {
    Map<String,Object> recent = findRecentActiveByHostAndTitle(hostId, title, reuseWindow);
    if (recent != null) return recent; // 재사용

    // 없으면 신규 생성
    return createNew(hostId, title, defaultLang, scheduledAt);
  }

  // ===== 수정/상태 전이 =====

  /** 부분 수정(제목/상태/초대코드/언어/시간/종료시각) */
  public int updatePartial(long hostId, long id, String title, String status,
                           String inviteCode, String defaultLang,
                           LocalDateTime scheduledAt, LocalDateTime endedAt) {
    return mapper.updatePartial(TABLE, PK, id, hostId, title, status, inviteCode, defaultLang, scheduledAt, endedAt);
  }

  /** SCHEDULED -> ONGOING */
  public int start(long hostId, long id) {
    return mapper.markOngoing(TABLE, PK, id, hostId);
  }

  /** * -> ENDED + ended_at=now */
  public int end(long hostId, long id) {
    return mapper.markEnded(TABLE, PK, id, hostId, LocalDateTime.now());
  }

  /** 초대코드 재발급(UNIQUE 충돌 시 재시도) */
  public int regenerateInvite(long hostId, long id) {
    String invite = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    for (int i = 0; i < 10; i++) {
      try {
        return mapper.setInviteCode(TABLE, PK, id, hostId, invite);
      } catch (DataIntegrityViolationException dup) {
        invite = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
      }
    }
    throw new IllegalStateException("Failed to set unique invite_code");
  }

  // ===== 유틸 =====

  private static final char[] ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
  private static final SecureRandom RND = new SecureRandom();

  private static String generateInviteCode(int len) {
    char[] out = new char[len];
    for (int i = 0; i < len; i++) out[i] = ALPHANUM[RND.nextInt(ALPHANUM.length)];
    return new String(out);
  }
}
