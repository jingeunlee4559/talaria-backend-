package com.example.talaria.backend.mapper.user;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.example.talaria.backend.model.User;

@Mapper
public interface UserMapper {
    User findByEmail(@Param("email") String email);
    List<User> searchByKeyword(String keyword);
}
