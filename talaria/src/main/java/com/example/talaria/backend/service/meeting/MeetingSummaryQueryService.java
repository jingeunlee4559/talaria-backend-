package com.example.talaria.backend.service.meeting;

import com.example.talaria.backend.mapper.meeting.MeetingSummaryMapper;
import com.example.talaria.backend.mapper.meeting.UtteranceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MeetingSummaryQueryService {

    private final MeetingSummaryMapper meetingSummaryMapper;
    private final UtteranceMapper utteranceMapper;

    /**
     * 단일 회의: 요약 + 발화 목록 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSummaryWithUtterances(
            long meetingId,
            String lang
    ) {
        // 1) tl_meeting_summaries 조회
        Map<String, Object> summary =
                meetingSummaryMapper.selectByMeetingAndLang(meetingId, lang);

        // 요약 데이터가 없을 때 처리
        if (summary == null) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("meetingId", meetingId);
            empty.put("lang", lang);
            empty.put("summaryId", null);
            empty.put("utterances", List.of());
            return empty;
        }

        // 2) tl_utterances 조회 (speaker_id, text)
        List<Map<String, Object>> utterances =
                utteranceMapper.selectByMeetingForApi(meetingId);

        // 3) 응답 구조 조립 – 모든 컬럼 평탄화
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("summaryId",        summary.get("summary_id"));
        result.put("meetingId",        summary.get("meeting_id"));
        result.put("lang",             summary.get("lang"));
        result.put("meetingDate",      summary.get("meeting_date"));
        result.put("meetingTitle",     summary.get("meeting_title"));
        result.put("participants",     summary.get("participants"));
        result.put("participantCount", summary.get("participant_count"));
        result.put("summary",          summary.get("summary"));
        result.put("meetingScore",     summary.get("meeting_score"));

        // 새로 추가된 필드들
        result.put("riskLevel",    summary.get("risk_level"));
        result.put("topics",       summary.get("topics"));
        result.put("decisions",    summary.get("decisions"));
        result.put("actionItems",  summary.get("action_items"));
        result.put("risks",        summary.get("risks"));
        result.put("improvements", summary.get("improvements"));
        result.put("nextSteps",    summary.get("next_steps"));
        result.put("createdAt",    summary.get("created_at"));
        result.put("updatedAt",    summary.get("updated_at"));

        // 발화 목록
        result.put("utterances", utterances);

        return result;
    }

    /**
     * 유저 기준: 모든 회의 요약 리스트 조회
     * GET /api/users/{userId}/meetings/summary/all?lang=ko
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllSummaries(
            long userId,
            String lang
    ) {
        // tl_meeting_summaries + tl_meetings JOIN 결과 (userId + lang 기준)
        List<Map<String, Object>> rows =
                meetingSummaryMapper.selectAllByUserAndLang(userId, lang);

        List<Map<String, Object>> results = new ArrayList<>();

        for (Map<String, Object> summary : rows) {
            Map<String, Object> item = new LinkedHashMap<>();

            item.put("summaryId",        summary.get("summary_id"));
            item.put("meetingId",        summary.get("meeting_id"));
            item.put("lang",             summary.get("lang"));
            item.put("meetingDate",      summary.get("meeting_date"));
            item.put("meetingTitle",     summary.get("meeting_title"));
            item.put("participants",     summary.get("participants"));
            item.put("participantCount", summary.get("participant_count"));
            item.put("summary",          summary.get("summary"));
            item.put("meetingScore",     summary.get("meeting_score"));

            item.put("riskLevel",        summary.get("risk_level"));
            item.put("topics",           summary.get("topics"));
            item.put("decisions",        summary.get("decisions"));
            item.put("actionItems",      summary.get("action_items"));
            item.put("risks",            summary.get("risks"));
            item.put("improvements",     summary.get("improvements"));
            item.put("nextSteps",        summary.get("next_steps"));
            item.put("createdAt",        summary.get("created_at"));
            item.put("updatedAt",        summary.get("updated_at"));

            results.add(item);
        }

        return results;
    }
}
