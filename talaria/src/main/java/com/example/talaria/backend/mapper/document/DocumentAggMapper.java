package com.example.talaria.backend.mapper.document;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface DocumentAggMapper {

    // ===== Flat (문서×번역 조인 결과) =====
    long countCombinedByUser(@Param("userId") long userId,
                             @Param("meetingId") Long meetingId,
                             @Param("docId") Long docId,
                             @Param("targetLang") String targetLang,
                             @Param("provider") String provider);

    List<Map<String, Object>> selectCombinedPageByUser(@Param("userId") long userId,
                                                       @Param("meetingId") Long meetingId,
                                                       @Param("docId") Long docId,
                                                       @Param("targetLang") String targetLang,
                                                       @Param("provider") String provider,
                                                       @Param("limit") int limit,
                                                       @Param("offset") int offset);

    // ===== Grouped (문서 레벨 페이징 + 번역 배치 조회) =====
    long countDistinctDocsByUser(@Param("userId") long userId,
                                 @Param("meetingId") Long meetingId,
                                 @Param("docId") Long docId);

    List<Map<String, Object>> selectDocsPageByUser(@Param("userId") long userId,
                                                   @Param("meetingId") Long meetingId,
                                                   @Param("docId") Long docId,
                                                   @Param("limit") int limit,
                                                   @Param("offset") int offset);

    List<Map<String, Object>> selectTransByDocIds(@Param("userId") long userId,
                                                  @Param("docIds") List<Long> docIds,
                                                  @Param("targetLang") String targetLang,
                                                  @Param("provider") String provider);
}
