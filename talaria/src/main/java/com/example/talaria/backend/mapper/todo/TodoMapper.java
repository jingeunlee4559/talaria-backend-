package com.example.talaria.backend.mapper.todo;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface TodoMapper {
    Map<String, Object> selectById(@Param("table") String table,
                                   @Param("pk") String pk,
                                   @Param("id") long id);

    long count(@Param("table") String table);

    List<Map<String, Object>> selectPage(@Param("table") String table,
                                         @Param("pk") String pk,
                                         @Param("limit") int limit,
                                         @Param("offset") int offset);
}
