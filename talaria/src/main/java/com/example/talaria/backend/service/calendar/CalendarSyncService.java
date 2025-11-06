package com.example.talaria.backend.service.calendar;

import com.example.talaria.backend.mapper.calendar.CalendarMapper;
import com.example.talaria.backend.service.meeting.MeetingService;
import com.example.talaria.backend.service.meeting.ParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CalendarSyncService {

    private final MeetingService meetingService;           // 이미 존재
    private final ParticipantService participantService;   // 이미 존재
    private final CalendarMapper calendarMapper;           // 이미 존재

    // tl_calender의 type 값 — 프로젝트 ENUM 값에 맞추세요(예: 'meeting' 또는 'metting')
    private static final String TYPE_MEETING = "meeting";

    /** 회의 생성 → host + 참가자 일정 생성/갱신 */
    @Transactional
    public void upsertForAllParticipants(long meetingId) {
        Map<String,Object> m = meetingService.getById(meetingId);
        if (m == null || m.isEmpty()) return;

        // host + 참가자(user_id) 전원
        List<Map<String,Object>> participants = participantService.getParticipantsWithUserInfo(meetingId);
        Set<Long> userIds = new LinkedHashSet<>();
        // host
        userIds.add(toLong(m.get("host_id")));
        // participants
        for (Map<String,Object> row : participants) {
            Long uid = toLong(row.get("user_id"));
            if (uid != null) userIds.add(uid);
        }

        // 모두 upsert
        for (Long userId : userIds) {
            upsertOne(userId, m);
        }
    }

    /** 회의 정보 변경 → 모든 참가자 일정 업데이트 */
    @Transactional
    public void updateForAllParticipants(long meetingId) {
        Map<String,Object> m = meetingService.getById(meetingId);
        if (m == null || m.isEmpty()) return;

        Map<String,Object> params = new HashMap<>();
        params.put("table", "tl_calender");
        params.put("pk", "event_id");
        params.put("meetingId", meetingId);
        params.put("title", m.get("title"));
        params.put("startDate", toDate(m.get("scheduled_at")));
        params.put("endDate", toDate(m.get("scheduled_at")));
        params.put("startTime", toTime(m.get("scheduled_at")));
        params.put("endTime", null);
        params.put("location", null);
        params.put("description", "");

        calendarMapper.updateAllByMeetingId(
            (String) params.get("table"),
            (String) params.get("pk"),
            (Long) params.get("meetingId"),
            (String) params.get("title"),
            (LocalDate) params.get("startDate"),
            (LocalDate) params.get("endDate"),
            (LocalTime) params.get("startTime"),
            (LocalTime) params.get("endTime"),
            (String) params.get("location"),
            (String) params.get("description")
        );
    }

    /** 회의 취소 → 연동 일정 전체 삭제 */
    @Transactional
    public void deleteForAllParticipants(long meetingId) {
        calendarMapper.deleteByMeetingId("tl_calender", "event_id", meetingId);
    }

    /** 참가자 추가 → 해당 사용자 일정 upsert */
    @Transactional
    public void upsertForUser(long meetingId, long userId) {
        Map<String,Object> m = meetingService.getById(meetingId);
        if (m == null || m.isEmpty()) return;
        upsertOne(userId, m);
    }

    /** 참가자 제거 → 해당 사용자 일정 삭제 */
    @Transactional
    public void deleteForUser(long meetingId, long userId) {
        calendarMapper.deleteByUserAndMeeting("tl_calender", "event_id", userId, meetingId);
    }

    /** (내부) 단일 upsert: 중복 방지 위해 먼저 삭제 후 INSERT */
    private void upsertOne(Long userId, Map<String,Object> meeting) {
        if (userId == null) return;

        LocalDate date = toDate(meeting.get("scheduled_at"));
        LocalTime time = toTime(meeting.get("scheduled_at"));

        // 삭제 후 삽입(UNIQUE 유무와 관계없이 안전)
        calendarMapper.deleteByUserAndMeeting("tl_calender", "event_id", userId, toLong(meeting.get("meetings_id")));

        Map<String,Object> event = new HashMap<>();
        event.put("user_id", userId);
        event.put("title", meeting.get("title"));
        event.put("type", TYPE_MEETING);
        event.put("start_date", date);
        event.put("end_date", date);
        event.put("start_time", time);
        event.put("end_time", null);
        event.put("location", null);
        event.put("description", "");
        event.put("meeting_id", toLong(meeting.get("meetings_id")));

        calendarMapper.insertEvent("tl_calender", event);
    }

    /* ---------- 유틸 ---------- */

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long l) return l;
        if (o instanceof Integer i) return i.longValue();
        if (o instanceof Number n) return n.longValue();
        if (o instanceof java.math.BigInteger b) return b.longValue();
        return null;
    }

    private static LocalDate toDate(Object o) {
        LocalDateTime dt = toDateTime(o);
        return (dt != null) ? dt.toLocalDate() : LocalDate.now();
        }

    private static LocalTime toTime(Object o) {
        LocalDateTime dt = toDateTime(o);
        return (dt != null) ? dt.toLocalTime() : null;
    }

    private static LocalDateTime toDateTime(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDateTime ldt) return ldt;
        if (o instanceof Timestamp ts) return ts.toLocalDateTime();
        // MapTypeHandler 등으로 String일 수 있음: "2025-11-06T10:00:00" 또는 "2025-11-06 10:00:00"
        if (o instanceof CharSequence cs) {
            String s = cs.toString().replace('T', ' ');
            try { return LocalDateTime.parse(s.replace(' ', 'T')); } catch (Exception ignore) {}
            try { return LocalDateTime.parse(s, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); } catch (Exception ignore) {}
        }
        return null;
    }
}
