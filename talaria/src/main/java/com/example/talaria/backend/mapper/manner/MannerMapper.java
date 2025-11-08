package com.example.talaria.backend.mapper.manner;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface MannerMapper {

    long countRules(
            @Param("table") String table,
            @Param("active") Integer active,
            @Param("agentCodes") List<String> agentCodes,
            @Param("speakerLocale") String speakerLocale,
            @Param("listenerLocale") String listenerLocale,
            @Param("culturePair") String culturePair,
            @Param("versionTag") String versionTag
    );

    List<Map<String, Object>> selectRules(
            @Param("table") String table,
            @Param("active") Integer active,
            @Param("agentCodes") List<String> agentCodes,
            @Param("speakerLocale") String speakerLocale,
            @Param("listenerLocale") String listenerLocale,
            @Param("culturePair") String culturePair,
            @Param("versionTag") String versionTag,
            @Param("size") int size,
            @Param("offset") int offset
    );

    /**
     * 쿨다운 체크 & 마킹(허용 시 last_alert_at=NOW()로 갱신/삽입)
     * @return 영향 행 수 (0=거부, 1/2=허용)
     */
    int checkAndMarkEvent(
            @Param("ruleId") long ruleId,
            @Param("sessionId") Long sessionId,
            @Param("speakerId") String speakerId,
            @Param("cooldownSec") int cooldownSec
    );

    Map<String, Object> findEvent(
            @Param("ruleId") long ruleId,
            @Param("sessionId") Long sessionId,
            @Param("speakerId") String speakerId
    );
}
