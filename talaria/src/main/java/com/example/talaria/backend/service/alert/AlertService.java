package com.example.talaria.backend.service.alert;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.mapper.alert.AlertMapper;
import com.example.talaria.backend.service.meeting.MeetingService;
import com.example.talaria.backend.service.meeting.ParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertMapper mapper;

    // ▼ 회의 정보/참가자 조회에 사용 (알림 생성용)
    private final MeetingService meetingService;
    private final ParticipantService participantService;

    private static final String TABLE = "tl_alerts";
    private static final String PK    = "id";

    
    private static final String TYPE_MEETING  = "MEETING";
    private static final String TITLE_MEETING = "회의 안내";

    /* ===================== 기존 API ===================== */

    @Transactional(readOnly = true)
    public PageResponse listByUser(long userId, Long alertId, String status, int page, int size) {
        if (size <= 0) size = 20;
        if (size > 100) size = 100;
        if (page < 0) page = 0;
        int offset = page * size;

        long total = mapper.countByUser(TABLE, PK, userId, alertId, status);
        List<Map<String, Object>> items =
                mapper.selectPageByUser(TABLE, PK, userId, alertId, status, size, offset);

        return new PageResponse(items, page, size, total);
    }

    @Transactional
    public Map<String, Object> create(long userId, String type, String title, String body, String payloadJson) {
        mapper.insert(TABLE, userId, type, title, body, payloadJson);
        long id = mapper.lastInsertId();

        Map<String, Object> row = mapper.selectOneByUser(TABLE, PK, userId, id);
        return (row != null && !row.isEmpty()) ? row : Map.of("id", id);
    }

    @Transactional
    public int markDelivered(long userId, long id) {
        return mapper.markDelivered(TABLE, PK, userId, id, LocalDateTime.now());
    }

    @Transactional
    public int markSeen(long userId, long id) {
        return mapper.markSeen(TABLE, PK, userId, id, LocalDateTime.now());
    }

    @Transactional
    public int markAllSeen(long userId) {
        return mapper.markAllSeen(TABLE, userId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public long countUnread(long userId) {
        return mapper.countUnreadByUser(TABLE, userId);
    }

    /* ===================== 추가: 회의 알림 생성 로직 ===================== */

    /**
     * 회의 생성 시: 주최자 + 참가자 전원에게
     * type="MEETING", title="회의 안내",
     * body="{회의제목} 회의가 예약되어있습니다.", payload='{"meeting_id":<id>}' 로 알림 생성
     */
    @Transactional
    public void notifyMeetingReservedForAllParticipants(long meetingId) {
        Map<String, Object> meeting = meetingService.getById(meetingId);
        if (meeting == null || meeting.isEmpty()) return;

        String meetingTitle = String.valueOf(meeting.get("title"));
        String body         = formatBody(meetingTitle);
        String payload      = payloadJson(meetingId);

        // host + participants (중복 제거)
        Set<Long> userIds = new LinkedHashSet<>();
        userIds.add(toLong(meeting.get("host_id")));
        for (Map<String, Object> p : participantService.getParticipantsWithUserInfo(meetingId)) {
            Long uid = toLong(p.get("user_id"));
            if (uid != null) userIds.add(uid);
        }
        userIds.remove(null);

        for (Long uid : userIds) {
            create(uid, TYPE_MEETING, TITLE_MEETING, body, payload);
        }
    }

    /**
     * 참가자 초대 시: 해당 사용자 1명에게만 알림 생성
     */
    @Transactional
    public void notifyMeetingReservedForUser(long meetingId, long userId) {
        Map<String, Object> meeting = meetingService.getById(meetingId);
        if (meeting == null || meeting.isEmpty()) return;

        String meetingTitle = String.valueOf(meeting.get("title"));
        String body         = formatBody(meetingTitle);
        String payload      = payloadJson(meetingId);

        create(userId, TYPE_MEETING, TITLE_MEETING, body, payload);
    }

    /* ===================== 내부 유틸 ===================== */

    private static String formatBody(String meetingTitle) {
        return  meetingTitle + "  회의가 예약되어있습니다.";
    }

    private static String payloadJson(long meetingId) {
        // tl_alerts.payload(JSON) 예: {"meeting_id":123}
        return "{\"meeting_id\":" + meetingId + "}";
    }

    private static Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long l) return l;
        if (obj instanceof Integer i) return i.longValue();
        if (obj instanceof java.math.BigInteger b) return b.longValue();
        if (obj instanceof Number n) return n.longValue();
        return null;
    }
}
