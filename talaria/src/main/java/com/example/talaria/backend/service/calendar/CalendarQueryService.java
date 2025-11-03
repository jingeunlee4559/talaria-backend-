package com.example.talaria.backend.service.calendar;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.mapper.calendar.CalendarMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CalendarQueryService {

    private final CalendarMapper mapper;

    private static final String TABLE = "tl_calender";
    private static final String PK = "event_id";

    /**
     * 사용자의 캘린더 이벤트 목록 조회
     */
    public PageResponse listByUser(long userId, Long eventId, LocalDateTime from, LocalDateTime to,
                                   int page, int size) {
        if (size > 100) size = 100;
        int offset = page * size;

        long total = mapper.countByUser(TABLE, PK, userId, eventId, from, to);
        List<Map<String, Object>> items = mapper.selectPageByUser(TABLE, PK, userId, eventId, from, to, size, offset);

        return new PageResponse(items, page, size, total);
    }

    /**
     * 특정 이벤트 단일 조회
     */
    public Map<String, Object> getEventById(long userId, long eventId) {
        return mapper.selectOneById(TABLE, PK, eventId, userId);
    }
}