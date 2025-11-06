package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.calendar.CalendarEventRequest;
import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.calendar.CalendarCommandService;
import com.example.talaria.backend.service.calendar.CalendarQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/calendar/events")
public class CalendarController {

    private final CalendarQueryService queryService;
    private final CalendarCommandService commandService;

    /**
     * 캘린더 이벤트 목록 조회
     */
    @GetMapping
    public ResponseEntity<?> listByUser(
            @PathVariable("userId") long userId,
            @RequestParam(name = "eventId", required = false) String eventId,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        try {
            Long eventIdNullable = (eventId == null || eventId.isBlank()) ? null : Long.parseLong(eventId);
            
            // 문자열을 LocalDateTime으로 변환
            LocalDateTime fromDateTime = (from == null || from.isBlank()) ? null : LocalDateTime.parse(from);
            LocalDateTime toDateTime = (to == null || to.isBlank()) ? null : LocalDateTime.parse(to);
            
            PageResponse response = queryService.listByUser(userId, eventIdNullable, fromDateTime, toDateTime, page, size);
            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            log.error("Invalid eventId format: {}", eventId, e);
            return ResponseEntity.badRequest().body(Map.of("error", "잘못된 이벤트 ID 형식입니다."));
        } catch (DateTimeParseException e) {
            log.error("Invalid date format. from: {}, to: {}", from, to, e);
            return ResponseEntity.badRequest().body(Map.of("error", "잘못된 날짜 형식입니다. ISO 8601 형식을 사용해주세요. (예: 2025-11-03T00:00:00)"));
        } catch (Exception e) {
            log.error("Error listing calendar events for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "이벤트 목록 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 캘린더 이벤트 생성
     */
    @PostMapping
    public ResponseEntity<?> createEvent(
            @PathVariable("userId") long userId,
            @RequestBody CalendarEventRequest request
    ) {
        try {
            log.info("Creating calendar event for user: {}, request: {}", userId, request);
            
            Map<String, Object> createdEvent = commandService.createEvent(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);
            
        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", 400);
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            log.error("Error creating calendar event for user: {}", userId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "이벤트 생성 중 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("status", 500);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 특정 이벤트 조회
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<?> getEvent(
        @PathVariable("userId") long userId,
        @PathVariable("eventId") long eventId
    ) {
        try {
            Map<String, Object> event = queryService.getEventById(userId, eventId);
            if (event == null || event.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "이벤트를 찾을 수 없습니다."));
            }
            return ResponseEntity.ok(event);
        } catch (Exception e) {
            log.error("Error getting calendar event. userId: {}, eventId: {}", userId, eventId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "이벤트 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     *  캘린더 이벤트 수정
     */
    @PutMapping("/{eventId}")
    public ResponseEntity<?> updateEvent(
            @PathVariable("userId") long userId,
            @PathVariable("eventId") long eventId,
            @RequestBody CalendarEventRequest request
    ) {
        try {
            log.info("Updating calendar event for user: {}, eventId: {}, request: {}", userId, eventId, request);
            
            Map<String, Object> updatedEvent = commandService.updateEvent(userId, eventId, request);
            
            if (updatedEvent == null || updatedEvent.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "이벤트를 찾을 수 없습니다."));
            }
            
            return ResponseEntity.ok(updatedEvent);
            
        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage(), "status", 400));
            
        } catch (Exception e) {
            log.error("Error updating calendar event for user: {}, eventId: {}", userId, eventId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "이벤트 수정 중 오류가 발생했습니다: " + e.getMessage(), "status", 500));
        }
    }

    /**
     *  캘린더 이벤트 삭제
     */
    @DeleteMapping("/{eventId}")
    public ResponseEntity<?> deleteEvent(
            @PathVariable("userId") long userId,
            @PathVariable("eventId") long eventId
    ) {
        try {
            log.info("Deleting calendar event for user: {}, eventId: {}", userId, eventId);
            
            boolean deleted = commandService.deleteEvent(userId, eventId);
            
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "이벤트를 찾을 수 없습니다."));
            }
            
            return ResponseEntity.ok(Map.of(
                    "message", "이벤트가 성공적으로 삭제되었습니다.",
                    "eventId", eventId
            ));
            
        } catch (Exception e) {
            log.error("Error deleting calendar event for user: {}, eventId: {}", userId, eventId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "이벤트 삭제 중 오류가 발생했습니다: " + e.getMessage(), "status", 500));
        }
    }

    /**
     * 전역 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
        log.error("Unhandled exception in CalendarController", e);
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "서버 오류가 발생했습니다.");
        errorResponse.put("message", e.getMessage());
        errorResponse.put("status", 500);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}