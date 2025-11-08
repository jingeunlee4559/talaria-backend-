package com.example.talaria.backend.mapper.user;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.example.talaria.backend.model.User;

@Mapper
public interface UserMapper {
    User findByEmail(@Param("email") String email);
    List<User> searchByKeyword(@Param("keyword") String keyword);
    Map<String, Object> findById(@Param("id") long id);
    String findLocaleById(@Param("id") long id);
    int updateLocale(@Param("id") long id, @Param("locale") String locale);
}
