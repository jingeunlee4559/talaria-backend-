// src/main/java/com/example/talaria/backend/mapper/document/MeetingDocumentMapper.java
package com.example.talaria.backend.mapper.document;

import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

public interface MeetingDocumentMapper {

    String selectMeetingStatus(@Param("meetingId") long meetingId);

    // ✅ 목록 조회: 원문 / 번역 분리 (UNION/CASE 제거)
    List<Map<String, Object>> selectOriginalDocsByMeeting(@Param("meetingId") long meetingId);
    List<Map<String, Object>> selectTranslationDocsByMeeting(@Param("meetingId") long meetingId);

    // 첨부/삭제
    int insertOriginalDoc(@Param("meetingId") long meetingId,
                          @Param("docId") long docId,
                          @Param("addedByUserId") long addedByUserId);

    int insertTranslationDoc(@Param("meetingId") long meetingId,
                             @Param("transDocId") long transDocId,
                             @Param("addedByUserId") long addedByUserId);

    int deleteOriginalDoc(@Param("meetingId") long meetingId,
                          @Param("docId") long docId);

    int deleteTranslationDoc(@Param("meetingId") long meetingId,
                             @Param("transDocId") long transDocId);

    // 소유권 확인
    long countDocOwnedByUser(@Param("docId") long docId,
                             @Param("userId") long userId);

    long countTransDocOwnedByUser(@Param("transDocId") long transDocId,
                                  @Param("userId") long userId);

    // 페이지네이션 (필요 시)
    long countByUserAndMeeting(@Param("userId") long userId,
                               @Param("meetingId") long meetingId);

    List<Map<String, Object>> selectPageByUserAndMeeting(@Param("userId") long userId,
                                                         @Param("meetingId") long meetingId,
                                                         @Param("limit") int limit,
                                                         @Param("offset") int offset);

    // 다운로드
    Map<String, Object> selectOriginalForDownload(@Param("userId") long userId,
                                                  @Param("meetingId") long meetingId,
                                                  @Param("docId") long docId);

    Map<String, Object> selectTranslationForDownload(@Param("userId") long userId,
                                                     @Param("meetingId") long meetingId,
                                                     @Param("transDocId") long transDocId);
}
