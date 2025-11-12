package com.example.talaria.backend.mapper.user;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.talaria.backend.model.User;

@Mapper
public interface UserMapper {

    // 단건 조회
    User selectById(@Param("userId") long userId);

    // 이메일 조회 / 로그인
    User findByEmail(@Param("email") String email);

    // 키워드 검색
    List<User> searchByKeyword(@Param("keyword") String keyword);

    // locale 단건 조회/수정
    String findLocaleById(@Param("userId") long userId);
    int updateLocale(@Param("userId") long userId, @Param("locale") String locale);

    // 부분 업데이트
    int updatePartialById(@Param("userId") long userId,
                          @Param("name") String name,
                          @Param("locale") String locale,
                          @Param("profileImageUrl") String profileImageUrl,
                          @Param("passwordHash") String passwordHash);
}
