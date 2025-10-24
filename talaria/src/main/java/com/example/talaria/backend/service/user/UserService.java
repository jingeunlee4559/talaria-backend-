package com.example.talaria.backend.service.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.talaria.backend.mapper.user.UserMapper;
import com.example.talaria.backend.model.User;

@Service
public class UserService {

    private final UserMapper userMapper;

    @Autowired
    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 로그인 처리를 위한 메소드
     * @param email 로그인 시도하는 사용자 이름
     * @return 로그인 성공 시 User 객체, 실패 시 null
     */
    public User login(String email) {
        // 이름으로 사용자를 조회하고, 존재하면 User 객체를 그대로 반환
        return userMapper.findByEmail(email);
    }
}