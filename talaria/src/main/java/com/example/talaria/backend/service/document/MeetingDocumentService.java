// src/main/java/com/example/talaria/backend/service/document/MeetingDocumentService.java
package com.example.talaria.backend.service.document;

import com.example.talaria.backend.controller.MeetingDocumentController;
import com.example.talaria.backend.mapper.document.MeetingDocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingDocumentService {

    private final MeetingDocumentMapper mapper;

    // ✅ 두 리스트를 서비스에서 병합 (UNION/CASE 제거의 효과)
    public List<Map<String, Object>> listByMeeting(long meetingId) {
        List<Map<String, Object>> originals = mapper.selectOriginalDocsByMeeting(meetingId);
        List<Map<String, Object>> translations = mapper.selectTranslationDocsByMeeting(meetingId);

        List<Map<String, Object>> merged = new ArrayList<>();
        if (originals != null) merged.addAll(originals);
        if (translations != null) merged.addAll(translations);

        // added_at DESC, meeting_doc_id DESC
        merged.sort((a, b) -> {
            // added_at
            String aAt = a.get("added_at") == null ? null : String.valueOf(a.get("added_at"));
            String bAt = b.get("added_at") == null ? null : String.valueOf(b.get("added_at"));
            int cmp = 0;
            if (aAt != null && bAt != null) {
                cmp = bAt.compareTo(aAt);
            } else if (aAt == null && bAt != null) {
                cmp = 1;
            } else if (aAt != null) {
                cmp = -1;
            }
            if (cmp != 0) return cmp;

            long aId = a.get("meeting_doc_id") == null ? 0L : Long.parseLong(a.get("meeting_doc_id").toString());
            long bId = b.get("meeting_doc_id") == null ? 0L : Long.parseLong(b.get("meeting_doc_id").toString());
            return Long.compare(bId, aId);
        });

        return merged;
    }

    @Transactional
    public Map<String, Object> attachDocuments(long userId, long meetingId,
                                               MeetingDocumentController.AttachReq request) {
        String status = mapper.selectMeetingStatus(meetingId);
        if (status == null) throw new IllegalArgumentException("회의를 찾을 수 없습니다.");
        if ("ENDED".equals(status)) throw new IllegalStateException("종료된 회의에는 문서를 추가할 수 없습니다.");

        int originalCount = 0;
        int translationCount = 0;

        if (request.getDocIds() != null) {
            for (Long docId : request.getDocIds()) {
                try {
                    long owned = mapper.countDocOwnedByUser(docId, userId);
                    if (owned > 0) {
                        mapper.insertOriginalDoc(meetingId, docId, userId);
                        originalCount++;
                    }
                } catch (Exception e) {
                    log.warn("원문 추가 실패: docId={}", docId, e);
                }
            }
        }

        if (request.getTransDocIds() != null) {
            for (Long transDocId : request.getTransDocIds()) {
                try {
                    long owned = mapper.countTransDocOwnedByUser(transDocId, userId);
                    if (owned > 0) {
                        mapper.insertTranslationDoc(meetingId, transDocId, userId);
                        translationCount++;
                    }
                } catch (Exception e) {
                    log.warn("번역 추가 실패: transDocId={}", transDocId, e);
                }
            }
        }

        return Map.of(
            "originalAdded", originalCount,
            "translationAdded", translationCount,
            "totalAdded", originalCount + translationCount
        );
    }

    public void detachDocument(long meetingId, long docId) {
        mapper.deleteOriginalDoc(meetingId, docId);
        mapper.deleteTranslationDoc(meetingId, docId);
    }

    public Map<String, Object> getOriginalDownloadInfo(long userId, long meetingId, long docId) {
        var info = mapper.selectOriginalForDownload(userId, meetingId, docId);
        if (info == null || info.isEmpty()) throw new IllegalArgumentException("문서를 찾을 수 없습니다.");
        return info;
    }

    // ✅ 번역 다운로드 info
    public Map<String, Object> getTranslationDownloadInfo(long userId, long meetingId, long transDocId) {
        var info = mapper.selectTranslationForDownload(userId, meetingId, transDocId);
        if (info == null || info.isEmpty()) throw new IllegalArgumentException("문서를 찾을 수 없습니다.");
        return info;
    }
}
