package com.example.talaria.backend.service.meeting;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.mapper.meeting.MeetingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.example.talaria.backend.event.MeetingEvents.MeetingCreatedEvent;
import com.example.talaria.backend.event.MeetingEvents.MeetingUpdatedEvent;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingMapper mapper;
    private final ParticipantService participantService;
    private final ApplicationEventPublisher events;

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

    // 단건 (호스트로 제한)
    public Map<String,Object> getOneByHost(long hostId, long id) {
        return mapper.selectOneByHost(TABLE, PK, hostId, id);
    }

    //  단건 (meetings_id만으로 조회)
    public Map<String,Object> getById(long meetingsId) {
        return mapper.selectById(TABLE, PK, meetingsId);
    }

    // 초대코드로 조회
    public Map<String,Object> getByInviteCode(String code) {
        return mapper.selectByInviteCode(TABLE, code);
    }

    // 최근 활성 회의 재사용
    public Map<String,Object> findRecentActiveByHostAndTitle(long hostId, String title, Duration window) {
        LocalDateTime since = LocalDateTime.now().minus(window);
        return mapper.selectRecentActiveByHostAndTitle(TABLE, hostId, title, since);
    }

    /* =========================
       생성부 (오버로드 1) — 기존 진입점
       status/meetingMode/썸네일 반영
       ========================= */
    @Transactional
    public Map<String,Object> create(long hostId,
                                     String title,
                                     String defaultLang,
                                     LocalDateTime scheduledAt,
                                     String meetingMode,           // 'B'/'C'
                                     String cultureThumbRelPath,   // nullable
                                     String requestedStatus) {     // 'SCHEDULED'/'ONGOING'/'ENDED'
        if (scheduledAt == null) scheduledAt = LocalDateTime.now();
        String normStatus = normalizeStatus(requestedStatus);
        return createNew(hostId, title, defaultLang, scheduledAt, meetingMode, cultureThumbRelPath, null, normStatus);
    }

    /* =========================
       생성부 (오버로드 2) — cultureDesc 추가
       컨트롤러에서 cultureDesc까지 받는 경우 이 메서드를 호출
       ========================= */
    @Transactional
    public Map<String,Object> create(long hostId,
                                     String title,
                                     String defaultLang,
                                     LocalDateTime scheduledAt,
                                     String meetingMode,           // 'B'/'C'
                                     String cultureThumbRelPath,   // nullable
                                     String requestedStatus,       // 'SCHEDULED'/'ONGOING'/'ENDED'
                                     String cultureDesc            // nullable
    ) {
        if (scheduledAt == null) scheduledAt = LocalDateTime.now();
        String normStatus = normalizeStatus(requestedStatus);
        return createNew(hostId, title, defaultLang, scheduledAt, meetingMode, cultureThumbRelPath, cultureDesc, normStatus);
    }

    // 기존 호환 (status 미지정 → SCHEDULED)
    @Transactional
    public Map<String,Object> create(long hostId, String title, String defaultLang, LocalDateTime scheduledAt) {
        if (scheduledAt == null) scheduledAt = LocalDateTime.now();
        return createNew(hostId, title, defaultLang, scheduledAt, "B", null, null, DEFAULT_STATUS);
    }

    /* =========================
       실제 생성 — meeting_mode / culture_thumb_rel_path / culture_desc / status 적용
       ========================= */
    @Transactional
    public Map<String,Object> createNew(long hostId,
                                        String title,
                                        String defaultLang,
                                        LocalDateTime scheduledAt,
                                        String meetingMode,
                                        String cultureThumbRelPath,
                                        String cultureDesc,         // ★ 신규
                                        String status) {
        String mode  = (meetingMode != null && meetingMode.equalsIgnoreCase("C")) ? "C" : "B";
        String thumb = (cultureThumbRelPath == null || cultureThumbRelPath.isBlank()) ? null : cultureThumbRelPath.trim();
        if (thumb != null && !thumb.startsWith("/")) thumb = "/" + thumb; // URL 일관성: 항상 "/img/..."
        String normStatus = normalizeStatus(status);

        String invite = generateInviteCode(10);
        for (int i = 0; i < 10; i++) {
            try {
                // INSERT: (table, hostId, title, status, invite, defaultLang, scheduledAt, meetingMode, cultureThumbRelPath, cultureDesc)
                mapper.insert(TABLE, hostId, title, normStatus, invite, defaultLang, scheduledAt, mode, thumb, cultureDesc);
                long id = mapper.lastInsertId();
                Map<String,Object> created = getOneByHost(hostId, id);
                events.publishEvent(new MeetingCreatedEvent(id));
                return created;
            } catch (DataIntegrityViolationException dup) {
                // invite_code UNIQUE 충돌 시 재시도
                invite = generateInviteCode(10);
            }
        }
        throw new IllegalStateException("Failed to generate unique invite_code");
    }

    // 이전 호환 (B 모드, SCHEDULED)
    @Transactional
    public Map<String,Object> createNew(long hostId, String title, String defaultLang, LocalDateTime scheduledAt) {
        return createNew(hostId, title, defaultLang, scheduledAt, "B", null, null, DEFAULT_STATUS);
    }

    // 부분 수정
    @Transactional
    public int updatePartial(long hostId, long id, String title, String status,
                             String inviteCode, String defaultLang,
                             LocalDateTime scheduledAt, LocalDateTime endedAt) {
        int updated = mapper.updatePartial(TABLE, PK, id, hostId, title, status, inviteCode, defaultLang, scheduledAt, endedAt);
        if (updated > 0) {
            events.publishEvent(new MeetingUpdatedEvent(id));
        }
        return updated;
    }

    // 시작/종료/재발급
    @Transactional
    public int start(long hostId, long id) {
        int updated = mapper.markOngoing(TABLE, PK, id, hostId);
        if (updated > 0) {
            events.publishEvent(new MeetingUpdatedEvent(id));
        }
        return updated;
    }

    @Transactional
    public int end(long hostId, long id) {
        int updated = mapper.markEnded(TABLE, PK, id, hostId, LocalDateTime.now());
        if (updated > 0) {
            events.publishEvent(new MeetingUpdatedEvent(id));
        }
        return updated;
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

    // 사용자가 호스트이거나 참가자인 모든 회의 조회
    public PageResponse listByUserIncludingParticipant(
        long userId, String status, LocalDateTime from, LocalDateTime to,
        String mode,   // ★ 추가: B/C 또는 null
        int page, int size) {

        if (size > 100) size = 100;
        int offset = page * size;

        long total = mapper.countByUserIncludingParticipant(userId, status, from, to, mode);
        List<Map<String,Object>> items = mapper.selectByUserIncludingParticipant(
            userId, status, from, to, mode, size, offset
        );

        for (Map<String, Object> meeting : items) {
            Long meetingId = toLong(meeting.get("meetings_id"));

            Map<String, Object> host = new HashMap<>();
            host.put("user_id", meeting.get("host_id"));
            host.put("name", meeting.get("host_name"));
            host.put("email", meeting.get("host_email"));
            host.put("profile_image_url", meeting.get("host_profile_image_url"));
            host.put("locale", meeting.get("host_locale"));
            meeting.put("host", host);

            meeting.remove("host_name");
            meeting.remove("host_email");
            meeting.remove("host_profile_image_url");
            meeting.remove("host_locale");

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

    private static String normalizeStatus(String s) {
        if (s == null) return DEFAULT_STATUS;
        String u = s.trim().toUpperCase();
        return (u.equals("SCHEDULED") || u.equals("ONGOING") || u.equals("ENDED")) ? u : DEFAULT_STATUS;
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

    /* =========================
       썸네일 단독 업데이트 (/api/assets/.../thumb)
       ========================= */
    @Transactional
    public int updateThumb(long hostId, long meetingsId, String relPath) {
        if (relPath == null || relPath.isBlank()) return 0;
        String store = relPath.startsWith("/") ? relPath : ("/" + relPath); // 항상 "/img/..."로 저장
        int n = mapper.updateThumb(TABLE, PK, meetingsId, hostId, store);
        if (n > 0) events.publishEvent(new MeetingUpdatedEvent(meetingsId));
        return n;
    }
}
