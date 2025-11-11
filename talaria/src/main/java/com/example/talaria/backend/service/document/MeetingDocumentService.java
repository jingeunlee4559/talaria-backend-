package com.example.talaria.backend.service.document;

import com.example.talaria.backend.mapper.document.MeetingDocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingDocumentService {

    private final MeetingDocumentMapper mapper;

    public List<Map<String, Object>> listByMeeting(long meetingId) {
        return mapper.selectDocsByMeeting(meetingId);
    }

    @Transactional
    public Map<String, Object> attachDocs(long userId, long meetingId, List<Long> docIds) {
        String status = mapper.selectMeetingStatus(meetingId);
        if (status == null) {
            throw new IllegalArgumentException("회의를 찾을 수 없습니다.");
        }
        if ("ENDED".equalsIgnoreCase(status)) {
            throw new IllegalStateException("종료된 회의에는 문서를 추가할 수 없습니다.");
        }

        int success = 0;
        List<Long> duplicated = new ArrayList<>();
        List<Long> notOwned  = new ArrayList<>();

        for (Long docId : docIds) {
            if (docId == null) continue;

            Long owned = mapper.countDocOwnedByUser(docId, userId);
            if (owned == null || owned == 0L) {
                notOwned.add(docId);
                continue;
            }

            try {
                success += mapper.insertMeetingDoc(meetingId, docId, userId);
            } catch (DataIntegrityViolationException ex) {
                duplicated.add(docId); // UNIQUE (meeting_id, doc_id) 충돌 → 중복
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attachedCount", success);
        result.put("duplicated", duplicated);
        result.put("notOwned", notOwned);
        result.put("meeting_id", meetingId);
        return result;
    }

    @Transactional
    public Map<String, Object> detachDoc(long meetingId, long docId) {
        int n = mapper.deleteMeetingDoc(meetingId, docId);
        return Map.of("deleted", n);
    }

    // 배치 해제 (선택)
    @Transactional
    public Map<String, Object> detachDocs(long meetingId, List<Long> docIds) {
        int success = 0;
        for (Long docId : docIds) {
            if (docId == null) continue;
            success += mapper.deleteMeetingDoc(meetingId, docId);
        }
        return Map.of("deletedCount", success, "meeting_id", meetingId, "docIds", docIds);
    }
}
