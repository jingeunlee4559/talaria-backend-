package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.dto.user.LoginRequestDto;
import com.example.talaria.backend.model.User;
// import com.example.talaria.backend.service.user.UserQueryService;
import com.example.talaria.backend.service.user.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
// @RequiredArgsConstructor
public class UserController {

    // private final UserQueryService userQueryService;

    private final UserService userService;

    // @GetMapping("/{id}")
    // public ResponseEntity<Map<String, Object>> getById(@PathVariable("id") long id) {
    //     return ResponseEntity.ok(userQueryService.findById(id));
    // }

    // @GetMapping
    // public ResponseEntity<PageResponse> getPage(
    //         @RequestParam(defaultValue = "0") int page,
    //         @RequestParam(defaultValue = "20") int size
    // ) {
    //     return ResponseEntity.ok(userQueryService.findPage(page, size));
    // }

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto) {
    // 1️⃣ 이메일로 사용자 조회
    User loggedInUser = userService.login(loginRequestDto.getEmail());

    // 2️⃣ 사용자 존재 확인
    if (loggedInUser == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Login Failed: User not found");
    }

    // 3️⃣ 비밀번호 비교 (해시 사용하지 않는 단순 비교)
    if (!loginRequestDto.getPassword().equals(loggedInUser.getPasswordHash())) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Login Failed: Incorrect password");
    }

    // 4️⃣ 로그인 성공 → 사용자 정보 반환
    // ※ 세션 생성 X
    return ResponseEntity.ok(loggedInUser);
}

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false); 
        if (session != null) {
            session.invalidate(); // 세션 무효화
        }
        return ResponseEntity.ok("Logout Successful");
    }
}
