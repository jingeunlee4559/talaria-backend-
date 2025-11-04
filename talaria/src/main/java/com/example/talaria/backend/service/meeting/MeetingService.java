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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingMapper mapper;
     private final ParticipantService participantService;

    private static final String TABLE = "tl_meetings";
    private static final String PK    = "meetings_id";
    private static final String DEFAULT_STATUS = "SCHEDULED";

    // 목록
    public PageResponse listByHost(long hostId, String status, LocalDateTime from, LocalDateTime to,
                                   int page, int size) {
        if (size > 100) size = 100;
        int offset = page * size;
        long total = mapper.countByHost(TABLE, hostId, status, from, to);
        List<Map<String,Object>> items =
            mapper.selectPageByHost(TABLE, hostId, status, from, to, size, offset);
        return new PageResponse(items, page, size, total);
    }

    // 단건
    public Map<String,Object> getOneByHost(long hostId, long id) {
        return mapper.selectOneByHost(TABLE, PK, hostId, id);
    }

    // 초대코드로 조회
    public Map<String,Object> getByInviteCode(String code) {
        return mapper.selectByInviteCode(TABLE, code);
    }

    // 최근 활성 회의(6h 등) 재사용
    public Map<String,Object> findRecentActiveByHostAndTitle(long hostId, String title, Duration window) {
        LocalDateTime since = LocalDateTime.now().minus(window);
        return mapper.selectRecentActiveByHostAndTitle(TABLE, hostId, title, since);
    }

    // (Controller에서 쓰는 기본 생성 진입점) null scheduledAt 보정
    @Transactional
    public Map<String,Object> create(long hostId, String title, String defaultLang, LocalDateTime scheduledAt) {
        if (scheduledAt == null) scheduledAt = LocalDateTime.now();
        return createNew(hostId, title, defaultLang, scheduledAt);
    }

    // 새 회의 생성(백엔드 invite_code 생성 + UNIQUE 충돌 시 재시도)
    @Transactional
    public Map<String,Object> createNew(long hostId, String title, String defaultLang, LocalDateTime scheduledAt) {
        if (scheduledAt == null) scheduledAt = LocalDateTime.now();
        String invite = generateInviteCode(10);
        for (int i = 0; i < 10; i++) {
            try {
                mapper.insert(TABLE, hostId, title, DEFAULT_STATUS, invite, defaultLang, scheduledAt);
                long id = mapper.lastInsertId();
                return getOneByHost(hostId, id);
            } catch (DataIntegrityViolationException dup) {
                invite = generateInviteCode(10);
            }
        }
        throw new IllegalStateException("Failed to generate unique invite_code");
    }

    // 부분 수정
    public int updatePartial(long hostId, long id, String title, String status,
                             String inviteCode, String defaultLang,
                             LocalDateTime scheduledAt, LocalDateTime endedAt) {
        return mapper.updatePartial(TABLE, PK, id, hostId, title, status, inviteCode, defaultLang, scheduledAt, endedAt);
    }

    // 시작/종료/재발급
    public int start(long hostId, long id) {
        return mapper.markOngoing(TABLE, PK, id, hostId);
    }

    public int end(long hostId, long id) {
        return mapper.markEnded(TABLE, PK, id, hostId, LocalDateTime.now());
    }

    public int regenerateInvite(long hostId, long id) {
        String invite = generateInviteCode(10);
        for (int i = 0; i < 10; i++) {
            try {
                return mapper.setInviteCode(TABLE, PK, id, hostId, invite);
            } catch (DataIntegrityViolationException dup) {
                invite = generateInviteCode(10);
            }
        }
        throw new IllegalStateException("Failed to set unique invite_code");
    }

    // 랜덤 초대코드
    private static final char[] ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final SecureRandom RND = new SecureRandom();
    private static String generateInviteCode(int len) {
        char[] out = new char[len];
        for (int i = 0; i < len; i++) out[i] = ALPHANUM[RND.nextInt(ALPHANUM.length)];
        return new String(out);
    }

 // 사용자가 호스트이거나 참가자인 모든 회의 조회 (참가자 정보 포함)
  public PageResponse listByUserIncludingParticipant(
    long userId, String status, LocalDateTime from, LocalDateTime to,
    int page, int size) {
    
    if (size > 100) size = 100;
    int offset = page * size;
    
    long total = mapper.countByUserIncludingParticipant(userId, status, from, to);
    List<Map<String,Object>> items = mapper.selectByUserIncludingParticipant(
        userId, status, from, to, size, offset
    );
    
    // 각 회의에 대해 참가자 정보 및 호스트 정보 정리
    for (Map<String, Object> meeting : items) {
        Long meetingId = toLong(meeting.get("meetings_id"));
        
        // 호스트 정보를 별도 객체로 구조화
        Map<String, Object> host = new HashMap<>();
        host.put("user_id", meeting.get("host_id"));
        host.put("name", meeting.get("host_name"));
        host.put("email", meeting.get("host_email"));
        host.put("profile_image_url", meeting.get("host_profile_image_url"));
        host.put("locale", meeting.get("host_locale"));
        
        meeting.put("host", host);
        
        // 원본 host 필드 제거 (선택사항)
        meeting.remove("host_name");
        meeting.remove("host_email");
        meeting.remove("host_profile_image_url");
        meeting.remove("host_locale");
        
        // 참가자 정보 추가
        if (meetingId != null) {
            List<Map<String, Object>> participants = 
                participantService.getParticipantsWithUserInfo(meetingId);
            meeting.put("participants", participants);
            meeting.put("participant_count", participants.size());
        } else {
            meeting.put("participants", new ArrayList<>());
            meeting.put("participant_count", 0);
        }
    }
    
    return new PageResponse(items, page, size, total);
}

// BigInteger, Integer 등을 안전하게 Long으로 변환
private Long toLong(Object obj) {
    if (obj == null) return null;
    if (obj instanceof Long) return (Long) obj;
    if (obj instanceof Integer) return ((Integer) obj).longValue();
    if (obj instanceof java.math.BigInteger) return ((java.math.BigInteger) obj).longValue();
    if (obj instanceof Number) return ((Number) obj).longValue();
    return null;
}
}



