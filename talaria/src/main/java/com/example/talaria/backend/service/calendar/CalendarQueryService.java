package com.example.talaria.backend.service.calendar;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.mapper.calendar.CalendarMapper;
import com.example.talaria.backend.exception.UserNotFoundException; // 필요 시 별도 예외 정의
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CalendarQueryService {
    private final CalendarMapper mapper;

    // 🔧 여기를 설계서 값으로 교체
    private static final String TABLE = "tl_calendar";   // 예시: 실제 테이블명으로
    private static final String PK    = "calendar_id";   // 예시: 실제 PK 컬럼명으로

    public Map<String, Object> findById(long id) {
        var row = mapper.selectById(TABLE, PK, id);
        if (row == null || row.isEmpty()) throw new UserNotFoundException(id); // 필요 시 전용 예외로
        return row;
    }

    public PageResponse findPage(int page, int size) {
        if (size > 100) size = 100;
        int offset = page * size;
        long total = mapper.count(TABLE);
        List<Map<String, Object>> items = mapper.selectPage(TABLE, PK, size, offset);
        return new PageResponse(items, page, size, total);
    }
}
