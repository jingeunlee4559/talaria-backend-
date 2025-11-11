package com.example.talaria.backend.mapper.transdoc;

import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

public interface TransDocMapper {

    long countByUserAndDoc(@Param("table") String table,
                           @Param("userId") long userId,
                           @Param("docId") long docId,
                           @Param("targetLang") String targetLang,
                           @Param("provider") String provider);

    List<Map<String, Object>> selectPageByUserAndDoc(@Param("table") String table,
                                                     @Param("userId") long userId,
                                                     @Param("docId") long docId,
                                                     @Param("targetLang") String targetLang,
                                                     @Param("provider") String provider,
                                                     @Param("limit") int limit,
                                                     @Param("offset") int offset);

    Map<String, Object> selectOneByRequestId(@Param("table") String table,
                                             @Param("userId") long userId,
                                             @Param("requestId") String requestId);

    Map<String, Object> selectLatestByComposite(@Param("table") String table,
                                                @Param("userId") long userId,
                                                @Param("docId") long docId,
                                                @Param("targetLang") String targetLang,
                                                @Param("provider") String provider);

    int deleteByRequestId(@Param("table") String table,
                          @Param("userId") long userId,
                          @Param("requestId") String requestId);

    int deleteByComposite(@Param("table") String table,
                          @Param("userId") long userId,
                          @Param("docId") long docId,
                          @Param("targetLang") String targetLang,
                          @Param("provider") String provider);

    long countByUser(@Param("table") String table,
                     @Param("userId") long userId,
                     @Param("targetLang") String targetLang,
                     @Param("provider") String provider);

    List<Map<String, Object>> selectPageByUser(@Param("table") String table,
                                               @Param("userId") long userId,
                                               @Param("targetLang") String targetLang,
                                               @Param("provider") String provider,
                                               @Param("limit") int limit,
                                               @Param("offset") int offset);
}
