package com.example.talaria.backend.mapper.meeting;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface MeetingMapper {

    long countByHost(@Param("table") String table,
                     @Param("hostId") long hostId,
                     @Param("status") String status,
                     @Param("from") LocalDateTime from,
                     @Param("to") LocalDateTime to);

    List<Map<String,Object>> selectPageByHost(@Param("table") String table,
                                              @Param("hostId") long hostId,
                                              @Param("status") String status,
                                              @Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to,
                                              @Param("limit") int limit,
                                              @Param("offset") int offset);

    Map<String,Object> selectOneByHost(@Param("table") String table,
                                       @Param("pk") String pk,
                                       @Param("hostId") long hostId,
                                       @Param("id") long id);

    Map<String,Object> selectByInviteCode(@Param("table") String table,
                                          @Param("inviteCode") String inviteCode);

    Map<String,Object> selectRecentActiveByHostAndTitle(@Param("table") String table,
                                                        @Param("hostId") long hostId,
                                                        @Param("title") String title,
                                                        @Param("since") LocalDateTime since);

    int insert(@Param("table") String table,
               @Param("hostId") long hostId,
               @Param("title") String title,
               @Param("status") String status,
               @Param("inviteCode") String inviteCode,
               @Param("defaultLang") String defaultLang,
               @Param("scheduledAt") LocalDateTime scheduledAt);

    long lastInsertId();

    int updatePartial(@Param("table") String table,
                      @Param("pk") String pk,
                      @Param("id") long id,
                      @Param("hostId") long hostId,
                      @Param("title") String title,
                      @Param("status") String status,
                      @Param("inviteCode") String inviteCode,
                      @Param("defaultLang") String defaultLang,
                      @Param("scheduledAt") LocalDateTime scheduledAt,
                      @Param("endedAt") LocalDateTime endedAt);

    int markOngoing(@Param("table") String table,
                    @Param("pk") String pk,
                    @Param("id") long id,
                    @Param("hostId") long hostId);

    int markEnded(@Param("table") String table,
                  @Param("pk") String pk,
                  @Param("id") long id,
                  @Param("hostId") long hostId,
                  @Param("endedAt") LocalDateTime endedAt);

    int setInviteCode(@Param("table") String table,
                      @Param("pk") String pk,
                      @Param("id") long id,
                      @Param("hostId") long hostId,
                      @Param("inviteCode") String inviteCode);


  // 사용자가 호스트이거나 참가자인 모든 회의 조회
    List<Map<String,Object>> selectByUserIncludingParticipant(
        @Param("userId") long userId,
        @Param("status") String status,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    long countByUserIncludingParticipant(
        @Param("userId") long userId,
        @Param("status") String status,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );





}
