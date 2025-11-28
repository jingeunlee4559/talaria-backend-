package com.example.talaria.backend.mapper.document;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface MeetingDocumentMapper {

    // 회의 상태 조회 (예: SCHEDULED / ONGOING / ENDED 등)
    String selectMeetingStatus(@Param("meetingId") long meetingId);  

    // ========== 회의별 문서 목록 (원본 / 번역 분리) ==========

    // 원본 문서 목록
    List<Map<String, Object>> selectOriginalDocsByMeeting(@Param("meetingId") long meetingId);

    // 번역 문서 목록
    List<Map<String, Object>> selectTranslationDocsByMeeting(@Param("meetingId") long meetingId);

    // ========== 회의에 문서 첨부/삭제 ==========

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

    // ========== 소유권 확인 (업로더/소유자 기준) ==========

    long countDocOwnedByUser(@Param("docId") long docId,
                             @Param("userId") long userId);

    long countTransDocOwnedByUser(@Param("transDocId") long transDocId,
                                  @Param("userId") long userId);

    // ========== 사용자+회의 기준 페이지네이션 (필요 시) ==========

    long countByUserAndMeeting(@Param("userId") long userId,
                               @Param("meetingId") long meetingId);

    List<Map<String, Object>> selectPageByUserAndMeeting(@Param("userId") long userId,
                                                         @Param("meetingId") long meetingId,
                                                         @Param("limit") int limit,
                                                         @Param("offset") int offset);

    // ========== 다운로드용 (기존 플로우) ==========

    Map<String, Object> selectOriginalForDownload(@Param("userId") long userId,
                                                  @Param("meetingId") long meetingId,
                                                  @Param("docId") long docId);

    Map<String, Object> selectTranslationForDownload(@Param("userId") long userId,
                                                     @Param("meetingId") long meetingId,
                                                     @Param("transDocId") long transDocId);

    // ========== ★ 회의방 미리보기 전용 링크 확인용 ==========

    /**
     * 회의에 연결된 원본 문서 존재 여부/메타 조회
     * - tl_meeting_documents 기준:
     *   meeting_id = #{meetingId}
     *   doc_type   = 'ORIGINAL'
     *   doc_id     = #{docId}
     */
    Map<String, Object> selectOriginalForMeeting(@Param("meetingId") long meetingId,
                                                 @Param("docId") long docId);

    /**
     * 회의에 연결된 번역 문서 존재 여부/메타 조회
     * - tl_meeting_documents 기준:
     *   meeting_id  = #{meetingId}
     *   doc_type    = 'TRANSLATION'
     *   trans_doc_id = #{transDocId}
     */
    Map<String, Object> selectTranslationForMeeting(@Param("meetingId") long meetingId,
                                                    @Param("transDocId") long transDocId);
}
