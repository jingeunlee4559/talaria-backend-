package com.example.talaria.backend.service.alert;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.mapper.alert.AlertMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertMapper mapper;

    // 스키마 상수
    private static final String TABLE = "tl_alerts";
    private static final String PK    = "id";

    public PageResponse listByUser(long userId, Long alertId, String status, int page, int size) {
        if (size > 100) size = 100;
        int offset = page * size;
        long total = mapper.countByUser(TABLE, PK, userId, alertId, status);
        List<Map<String, Object>> items =
            mapper.selectPageByUser(TABLE, PK, userId, alertId, status, size, offset);
        return new PageResponse(items, page, size, total);
    }

    public Map<String, Object> create(long userId, String type, String title, String body, String payloadJson) {
        mapper.insert(TABLE, userId, type, title, body, payloadJson);
        long id = mapper.lastInsertId();
        // 단건 재조회
        List<Map<String, Object>> rows =
            mapper.selectPageByUser(TABLE, PK, userId, id, null, 1, 0);
        return rows.isEmpty() ? Map.of("id", id) : rows.get(0);
    }

    public int markDelivered(long userId, long id) {
        return mapper.markDelivered(TABLE, userId, id, LocalDateTime.now());
    }

    public int markSeen(long userId, long id) {
        return mapper.markSeen(TABLE, userId, id, LocalDateTime.now());
    }

    public int markAllSeen(long userId) {
        return mapper.markAllSeen(TABLE, userId, LocalDateTime.now());
    }
}
