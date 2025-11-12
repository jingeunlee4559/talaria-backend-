package com.example.talaria.backend.service.user;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return userMapper.findByEmail(email);
    }

    /* 키워드 검색 */
    public List<User> searchByKeyword(String keyword) {
        return userMapper.searchByKeyword(keyword);
    }

    /* 사용자 ID로 조회 (User 반환) */
    public User getById(long userId) {
        return userMapper.selectById(userId);
    }

    @Transactional
    public User updateUserPartial(long userId,
                                  String name,
                                  String locale,
                                  String profileImageUrl,
                                  String currentPassword,
                                  String newPassword) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new IllegalArgumentException("User not found");

        // 비밀번호 변경 의사 확인
        String passwordToUpdate = null;
        if (newPassword != null && !newPassword.isBlank()) {
            if (currentPassword == null || !currentPassword.equals(u.getPasswordHash())) {
                throw new SecurityException("Current password mismatch");
            }
            // 주의: 현재 구조상 평문 비교/저장. 추후 해시 적용 권장.
            passwordToUpdate = newPassword;
        }

        // 동일 값은 null 처리하여 불필요한 UPDATE 방지
        String nameUpd = (name != null && !name.isBlank() && !name.equals(u.getName())) ? name : null;
        String locUpd  = (locale != null && !locale.isBlank() && !locale.equals(u.getLocale())) ? locale : null;
        String imgUpd  = (profileImageUrl != null && !profileImageUrl.isBlank()
                         && !profileImageUrl.equals(u.getProfileImageUrl())) ? profileImageUrl : null;

        if (nameUpd == null && locUpd == null && imgUpd == null && passwordToUpdate == null) {
            return u; // 변경 없음
        }

        userMapper.updatePartialById(userId, nameUpd, locUpd, imgUpd, passwordToUpdate);
        return userMapper.selectById(userId);
    }
}
