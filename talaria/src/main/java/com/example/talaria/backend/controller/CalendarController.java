package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.calendar.CalendarQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/calendar/events")
public class CalendarController {

    private final CalendarQueryService service;

    @GetMapping
    public ResponseEntity<PageResponse> listByUser(
            @PathVariable long userId,
            @RequestParam(required = false) String eventId,          // ← nullable 문자열로 받아 빈문자 처리
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long eventIdNullable = (eventId == null || eventId.isBlank()) ? null : Long.parseLong(eventId);
        return ResponseEntity.ok(service.listByUser(userId, eventIdNullable, from, to, page, size));
    }
}
