// src/main/java/com/example/talaria/backend/mapper/user/UserMapper.java
package com.example.talaria.backend.mapper.user;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {
    Map<String, Object> selectById(@Param("id") long id);
    long countAll();
    List<Map<String, Object>> selectPage(@Param("limit") int limit, @Param("offset") int offset);
}
