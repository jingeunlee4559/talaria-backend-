package com.example.talaria.backend.service.calendar;

import com.example.talaria.backend.dto.calendar.CalendarEventRequest;
import com.example.talaria.backend.mapper.calendar.CalendarMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarCommandService {

    private final CalendarMapper mapper;
    private final CalendarQueryService queryService;

    private static final String TABLE = "tl_calender";
    private static final String PK = "event_id";

    /**
     * 새 캘린더 이벤트 생성
     */
    @Transactional
    public Map<String, Object> createEvent(long userId, CalendarEventRequest request) {
        // 유효성 검사
        validateEventRequest(request);

        // 이벤트 데이터 구성
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("user_id", userId);
        eventMap.put("title", request.getTitle());
        eventMap.put("type", request.getType());
        eventMap.put("start_date", request.getStartDate());
        eventMap.put("end_date", request.getEndDate());
        eventMap.put("start_time", request.getStartTime());
        eventMap.put("end_time", request.getEndTime());
        eventMap.put("location", request.getLocation());
        eventMap.put("description", request.getDescription());
        eventMap.put("meeting_id", request.getMeetingId());

        // DB 삽입
        int inserted = mapper.insertEvent(TABLE, eventMap);
        
        if (inserted == 0) {
            throw new RuntimeException("이벤트 생성에 실패했습니다.");
        }

        // 생성된 이벤트 ID 가져오기
        Long eventId = ((Number) eventMap.get("event_id")).longValue();
        
        // 생성된 이벤트 조회하여 반환
        return queryService.getEventById(userId, eventId);
    }

    /**
     * 이벤트 수정
     */
    @Transactional
    public Map<String, Object> updateEvent(long userId, long eventId, CalendarEventRequest request) {
        log.info("Updating event. userId: {}, eventId: {}", userId, eventId);
        
        // 유효성 검사
        validateEventRequest(request);

        // 기존 이벤트 존재 여부 확인
        Map<String, Object> existingEvent = mapper.selectOneById(TABLE, PK, eventId, userId);
        if (existingEvent == null || existingEvent.isEmpty()) {
            log.warn("Event not found for update. userId: {}, eventId: {}", userId, eventId);
            return null;
        }

        // 업데이트할 데이터 구성
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("event_id", eventId);
        eventMap.put("user_id", userId);
        eventMap.put("title", request.getTitle());
        eventMap.put("type", request.getType());
        eventMap.put("start_date", request.getStartDate());
        eventMap.put("end_date", request.getEndDate());
        eventMap.put("start_time", request.getStartTime());
        eventMap.put("end_time", request.getEndTime());
        eventMap.put("location", request.getLocation());
        eventMap.put("description", request.getDescription());
        eventMap.put("meeting_id", request.getMeetingId());

        // DB 업데이트
        int updated = mapper.updateEvent(TABLE, PK, eventMap);
        
        if (updated == 0) {
            log.warn("No rows updated. userId: {}, eventId: {}", userId, eventId);
            return null;
        }

        log.info("Event updated successfully. userId: {}, eventId: {}", userId, eventId);

        // 업데이트된 이벤트 조회하여 반환
        return queryService.getEventById(userId, eventId);
    }

    /**
     *  이벤트 삭제
     */
    @Transactional
    public boolean deleteEvent(long userId, long eventId) {
        log.info("Deleting event. userId: {}, eventId: {}", userId, eventId);
        
        // 기존 이벤트 존재 여부 확인
        Map<String, Object> existingEvent = mapper.selectOneById(TABLE, PK, eventId, userId);
        if (existingEvent == null || existingEvent.isEmpty()) {
            log.warn("Event not found for deletion. userId: {}, eventId: {}", userId, eventId);
            return false;
        }

        // DB 삭제
        int deleted = mapper.deleteEvent(TABLE, PK, userId, eventId);
        
        if (deleted == 0) {
            log.warn("No rows deleted. userId: {}, eventId: {}", userId, eventId);
            return false;
        }

        log.info("Event deleted successfully. userId: {}, eventId: {}", userId, eventId);
        return true;
    }

    /**
     * 이벤트 요청 데이터 유효성 검사
     */
    private void validateEventRequest(CalendarEventRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("이벤트 제목은 필수입니다.");
        }

        if (request.getType() == null || request.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("이벤트 유형은 필수입니다.");
        }

        // type 유효성 검사
        String[] validTypes = {"meeting", "duedate", "project", "schedule", "vacation", "etc"};
        boolean isValidType = false;
        for (String validType : validTypes) {
            if (validType.equals(request.getType())) {
                isValidType = true;
                break;
            }
        }
        if (!isValidType) {
            throw new IllegalArgumentException("유효하지 않은 이벤트 유형입니다.");
        }

        if (request.getStartDate() == null) {
            throw new IllegalArgumentException("시작 날짜는 필수입니다.");
        }

        // 종료 날짜가 시작 날짜보다 이전인지 검사
        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("종료 날짜는 시작 날짜보다 이전일 수 없습니다.");
        }
    }
}