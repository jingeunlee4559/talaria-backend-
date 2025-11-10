package com.example.talaria.backend.mapper.document;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DocumentMapper {

    long countByUser(@Param("table") String table,
                     @Param("pk") String pk,
                     @Param("userId") long userId,
                     @Param("meetingId") Long meetingId);

    List<Map<String, Object>> selectPageByUser(@Param("table") String table,
                                               @Param("pk") String pk,
                                               @Param("userId") long userId,
                                               @Param("meetingId") Long meetingId,
                                               @Param("limit") int limit,
                                               @Param("offset") int offset);

    Map<String, Object> selectOneByUser(@Param("table") String table,
                                        @Param("pk") String pk,
                                        @Param("userId") long userId,
                                        @Param("docId") long docId);

    int insert(@Param("table") String table,
               @Param("userId") long userId,
               @Param("meetingId") Long meetingId,
               @Param("originalName") String originalName,
               @Param("filepath") String filepath,
               @Param("mimeType") String mimeType,
               @Param("size") long size,
               @Param("sha256") String sha256,
               @Param("originalLang") String originalLang,
               @Param("storageKey") String storageKey,
               @Param("useContextRag") boolean useContextRag);

    long lastInsertId();

    int deleteByUser(@Param("table") String table,
                     @Param("pk") String pk,
                     @Param("userId") long userId,
                     @Param("docId") long docId);

    int toggleUseContextRag(
                    @Param("table") String table,
                    @Param("userId") long userId,
                    @Param("docId") long docId);
}
