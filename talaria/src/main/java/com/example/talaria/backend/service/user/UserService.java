// src/main/java/com/example/talaria/backend/service/user/UserService.java
package com.example.talaria.backend.service.user;

import java.util.List;

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

    /* 로그인용 */
    public User login(String email) {
        return userMapper.findByEmail(email);
    }

    /* 이메일로 사용자 조회 */
    public User getByEmail(String email){ 
        return userMapper.findByEmail(email); }

public List<User> searchByKeyword(String keyword) {
        return userMapper.searchByKeyword(keyword);
    }
}
