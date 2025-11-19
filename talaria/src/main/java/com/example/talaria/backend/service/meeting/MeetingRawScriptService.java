package com.example.talaria.backend.service.meeting;

import com.example.talaria.backend.mapper.meeting.UtteranceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * tl_utterances 에서 meeting_id 로 발화 조회 →
 * 지정된 포맷으로 하나의 텍스트로 합침 →
 * 파이썬 서비스로 전송
 */
@Service
@RequiredArgsConstructor
public class MeetingRawScriptService {

    private final UtteranceMapper utteranceMapper;
    private final MeetingRawScriptClient rawScriptClient;

    /**
     * 프론트에서 받은 정보:
     *  - meetingId
     *  - meetingTitle
     *  - participants (참석자 이름 리스트)
     *
     * 처리:
     *  1) tl_utterances 조회
     *  2) 포맷:
     *     [회의 정보]
     *     주제: {회의제목}
     *     참석자: {참여자이름1, 참여자이름2, ...}
     *
     *     [스크립트]
     *     {speaker_id}: {text}
     *     ...
     *  3) raw_script text를 파이썬으로 전송
     */
    @Transactional(readOnly = true)
    public Map<String, Object> buildAndSendRawScript(
            long meetingId,
            String meetingTitle,
            List<String> participants
    ) {
        // 1) tl_utterances 조회
        List<Map<String, Object>> utterances =
                utteranceMapper.selectByMeetingForScript(meetingId);

        // 2) 하나의 text로 합치기
        String rawScriptText = buildRawScriptText(meetingTitle, participants, utterances);

        // 3) 파이썬 서비스로 전송
        Map<String, Object> pyResponse =
                rawScriptClient.sendRawScript(meetingId, rawScriptText);

        // 4) 프론트로 돌려줄 응답 구성 (필요시 구조 변경 가능)
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("meetingId", meetingId);
        result.put("meetingTitle", meetingTitle);
        result.put("participants", participants);
        result.put("rawScript", rawScriptText);
        result.put("pythonResponse", pyResponse);
        return result;
    }

    /**
     * 요청한 포맷대로 text 생성
     */
    private String buildRawScriptText(
            String meetingTitle,
            List<String> participants,
            List<Map<String, Object>> utterances
    ) {
        StringJoiner sj = new StringJoiner("\n");

        // [회의 정보]
        sj.add("[회의 정보]");
        sj.add("주제: " + meetingTitle);

        String participantsLine = "";
        if (participants != null && !participants.isEmpty()) {
            participantsLine = String.join(", ", participants);
        }
        sj.add("참석자: " + participantsLine);
        sj.add("");
        sj.add("[스크립트]");

        if (utterances != null) {
            for (Map<String, Object> row : utterances) {
                Object speakerObj = row.get("speaker_id");
                Object textObj = row.get("text");

                String speaker = speakerObj != null ? speakerObj.toString() : "";
                String text = textObj != null ? textObj.toString() : "";

                if (text.isBlank()) {
                    continue;
                }
                sj.add(speaker + ": " + text.trim());
            }
        }

        return sj.toString();
    }
}
