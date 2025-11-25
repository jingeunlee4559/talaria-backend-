package com.example.talaria.backend.service.meeting;

import com.example.talaria.backend.mapper.meeting.MeetingSummaryMapper;
import com.example.talaria.backend.mapper.meeting.UtteranceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MeetingSummaryQueryService {

    private final MeetingSummaryMapper meetingSummaryMapper;
    private final UtteranceMapper utteranceMapper;

    @Transactional(readOnly = true)
    public Map<String, Object> getSummaryWithUtterances(
            long meetingId,
            String lang
    ) {
        // 1) tl_meeting_summaries 조회
        Map<String, Object> summary =
                meetingSummaryMapper.selectByMeetingAndLang(meetingId, lang);

        // 요약 데이터가 없을 때 처리 (필요 없어도 유지해 두면 안전)
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
        result.put("riskLevel",   summary.get("risk_level"));
        result.put("topics",      summary.get("topics"));
        result.put("decisions",   summary.get("decisions"));
        result.put("actionItems", summary.get("action_items"));
        result.put("risks",       summary.get("risks"));
        result.put("improvements", summary.get("improvements"));
        result.put("nextSteps",   summary.get("next_steps"));
        result.put("createdAt",   summary.get("created_at"));
        result.put("updatedAt",   summary.get("updated_at"));

        // 발화 목록
        result.put("utterances", utterances);

        return result;
    }
}
