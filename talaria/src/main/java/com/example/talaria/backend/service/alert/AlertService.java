package com.example.talaria.backend.service.alert;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.mapper.alert.AlertMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertMapper mapper;

    private static final String TABLE = "tl_alerts";
    private static final String PK    = "id";

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

        //  단건 재조회
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

    // (선택) 뱃지용 미열람 카운트
    @Transactional(readOnly = true)
    public long countUnread(long userId) {
        return mapper.countUnreadByUser(TABLE, userId);
    }
}
