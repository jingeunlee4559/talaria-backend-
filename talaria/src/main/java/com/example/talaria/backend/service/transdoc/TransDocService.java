package com.example.talaria.backend.service.transdoc;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.mapper.transdoc.TransDocMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransDocService {

    private final TransDocMapper mapper;
    private static final String TABLE = "tl_trans_docs";

    @Transactional(readOnly = true)
    public PageResponse listByDoc(long userId, long docId, String targetLang, String provider, int page, int size) {
        if (size <= 0) size = 20;
        if (size > 100) size = 100;
        if (page < 0) page = 0;

        int offset = page * size;
        long total = mapper.countByUserAndDoc(TABLE, userId, docId, targetLang, provider);
        List<Map<String, Object>> items =
                mapper.selectPageByUserAndDoc(TABLE, userId, docId, targetLang, provider, size, offset);
        return new PageResponse(items, page, size, total);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOneByRequestId(long userId, String requestId) {
        Map<String, Object> row = mapper.selectOneByRequestId(TABLE, userId, requestId);
        if (row == null || row.isEmpty()) {
            // 500 방지: 404 반환
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Translation not found or not owned");
        }
        return row;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLatestByComposite(long userId, long docId, String targetLang, String provider) {
        Map<String, Object> row = mapper.selectLatestByComposite(TABLE, userId, docId, targetLang, provider);
        if (row == null || row.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Translation not found for given keys");
        }
        return row;
    }

    @Transactional
    public int deleteByRequestId(long userId, String requestId) {
        return mapper.deleteByRequestId(TABLE, userId, requestId);
    }

    @Transactional
    public int deleteByComposite(long userId, long docId, String targetLang, String provider) {
        return mapper.deleteByComposite(TABLE, userId, docId, targetLang, provider);
    }

    @Transactional(readOnly = true)
    public PageResponse listByUser(long userId, String targetLang, String provider, int page, int size) {
        if (size <= 0) size = 20;
        if (size > 100) size = 100;
        if (page < 0) page = 0;

        int offset = page * size;
        long total = mapper.countByUser(TABLE, userId, targetLang, provider);
        List<Map<String, Object>> items =
                mapper.selectPageByUser(TABLE, userId, targetLang, provider, size, offset);
        return new PageResponse(items, page, size, total);
    }
}
