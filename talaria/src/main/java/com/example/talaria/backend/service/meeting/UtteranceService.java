package com.example.talaria.backend.service.meeting;

import com.example.talaria.backend.mapper.meeting.UtteranceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UtteranceService {

    private final UtteranceMapper utteranceMapper;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listByMeeting(long userId, long meetingId) {
        return utteranceMapper.selectByMeetingForApi(meetingId);
    }
}
