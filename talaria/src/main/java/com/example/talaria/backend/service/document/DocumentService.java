package com.example.talaria.backend.service.document;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.mapper.document.DocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentMapper mapper;

    private static final String TABLE = "tl_documents";
    private static final String PK    = "doc_id";

    public PageResponse listByUser(long userId, Long meetingId, int page, int size) {
        if (size > 100) size = 100;
        int offset = page * size;
        long total = mapper.countByUser(TABLE, PK, userId, meetingId);
        List<Map<String, Object>> items =
            mapper.selectPageByUser(TABLE, PK, userId, meetingId, size, offset);
        return new PageResponse(items, page, size, total);
    }

    public Map<String, Object> getOneByUser(long userId, long docId) {
        var row = mapper.selectOneByUser(TABLE, PK, userId, docId);
        if (row == null || row.isEmpty()) throw new RuntimeException("Document not found or not owned");
        return row;
    }

    public Map<String, Object> create(long userId, Long meetingId,
                                      String originalName, String filepath, String mimeType,
                                      long size, String sha256, String originalLang, String storageKey) {
        try {
            mapper.insert(TABLE, userId, meetingId, originalName, filepath, mimeType, size, sha256, originalLang, storageKey);
        } catch (DataIntegrityViolationException ex) {
            // UNIQUE(filepath) 위반 등
            throw new IllegalStateException("Duplicate filepath or constraint violation", ex);
        }
        long id = mapper.lastInsertId();
        var list = mapper.selectPageByUser(TABLE, PK, userId, null, 1, 0);
        // 방금 입력건 재조회(가장 최근) 또는 필요한 경우 selectOneByUser로 정확 조회
        return mapper.selectOneByUser(TABLE, PK, userId, id);
    }

    public int delete(long userId, long docId) {
        return mapper.deleteByUser(TABLE, PK, userId, docId);
    }
}
