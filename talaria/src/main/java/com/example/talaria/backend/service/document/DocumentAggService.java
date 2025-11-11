package com.example.talaria.backend.service.document;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.mapper.document.DocumentAggMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentAggService {

    private final DocumentAggMapper mapper;

    // ===== Flat: 문서×번역 조인 결과(행 단위) =====
    @Transactional(readOnly = true)
    public PageResponse listCombinedFlat(long userId, Long meetingId, Long docId,
                                         String targetLang, String provider, int page, int size) {
        if (size <= 0) size = 20;
        if (size > 200) size = 200;
        if (page < 0) page = 0;
        int offset = page * size;

        long total = mapper.countCombinedByUser(userId, meetingId, docId, targetLang, provider);
        List<Map<String, Object>> items = mapper.selectCombinedPageByUser(
                userId, meetingId, docId, targetLang, provider, size, offset);
        return new PageResponse(items, page, size, total);
    }

    // ===== Grouped: 문서 레벨 페이징 + 번역 배열 =====
    @Transactional(readOnly = true)
    public PageResponse listCombinedGrouped(long userId, Long meetingId, Long docId,
                                            String targetLang, String provider, int page, int size) {
        if (size <= 0) size = 20;
        if (size > 100) size = 100;
        if (page < 0) page = 0;
        int offset = page * size;

        long totalDocs = mapper.countDistinctDocsByUser(userId, meetingId, docId);
        if (totalDocs == 0) {
            return new PageResponse(Collections.emptyList(), page, size, 0);
        }

        List<Map<String, Object>> docs = mapper.selectDocsPageByUser(userId, meetingId, docId, size, offset);
        if (docs.isEmpty()) {
            return new PageResponse(Collections.emptyList(), page, size, totalDocs);
        }

        // 대상 doc_ids 수집
        List<Long> docIds = docs.stream()
                .map(m -> ((Number)m.get("doc_id")).longValue())
                .collect(Collectors.toList());

        // 번역들 일괄 조회 (필터 적용)
        List<Map<String, Object>> trans = mapper.selectTransByDocIds(userId, docIds, targetLang, provider);

        // original_doc_id로 그룹핑
        Map<Long, List<Map<String, Object>>> transMap = trans.stream()
                .collect(Collectors.groupingBy(m -> ((Number)m.get("original_doc_id")).longValue()));

        // 각 문서에 translations 필드로 부착
        List<Map<String, Object>> merged = new ArrayList<>(docs.size());
        for (Map<String, Object> doc : docs) {
            Long id = ((Number)doc.get("doc_id")).longValue();
            List<Map<String, Object>> tlist = transMap.getOrDefault(id, Collections.emptyList());
            Map<String, Object> row = new LinkedHashMap<>(doc);
            row.put("translations", tlist);
            merged.add(row);
        }

        return new PageResponse(merged, page, size, totalDocs);
    }
}
