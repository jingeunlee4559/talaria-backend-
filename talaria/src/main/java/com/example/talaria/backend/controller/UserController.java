package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.user.LoginRequestDto;
import com.example.talaria.backend.model.User;
import com.example.talaria.backend.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users", description = "사용자 인증/조회 API")
@Validated
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    public UserController(UserService userService) { this.userService = userService; }

    // === 로그인 ===
    @Operation(
      summary = "로그인",
      description = "이메일/비밀번호로 로그인합니다. (단순 비교, 해시 미적용)",
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        content = @Content(
          schema = @Schema(implementation = LoginRequestDto.class),
          examples = @ExampleObject(
            name = "예시",
            value = """
                    {
                      "email": "user1@example.com",
                      "password": "1234"
                    }
                    """
          )
        )
      )
    )
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto) {
        User u = userService.login(loginRequestDto.getEmail());
        if (u == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Login Failed: User not found");
        }
        if (!loginRequestDto.getPassword().equals(u.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Login Failed: Incorrect password");
        }
        return ResponseEntity.ok(UserInfoRes.from(u)); // 비번 제외 안전 응답
    }

    // === 로그아웃 ===
    @Operation(summary = "로그아웃", description = "서버 세션을 무효화합니다.")
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.ok("Logout Successful");
    }

    // === 이메일로 사용자 조회(신규) ===
    @Operation(summary = "이메일로 사용자 조회", description = "비밀번호를 제외한 정보만 반환")
    @GetMapping("/by-email")
    public ResponseEntity<?> getByEmail(
        @Parameter(
          name = "email", in = ParameterIn.QUERY, required = true,
          description = "조회할 사용자 이메일",
          schema = @Schema(type = "string", format = "email"),
          example = "user1@example.com"
        )
        @RequestParam("email") @NotBlank @Email String email
    ) {
        User u = userService.getByEmail(email);
        if (u == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        return ResponseEntity.ok(UserInfoRes.from(u));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsersByKeyword(@RequestParam("keyword") String keyword) {
        List<User> users = userService.searchByKeyword(keyword);
        List<UserInfoRes> responseData = users.stream()
                                              .map(UserInfoRes::from)
                                              .collect(Collectors.toList());
        return ResponseEntity.ok(responseData);
    }

    // === 응답 DTO(비밀번호 제외) ===
    @Data
    public static class UserInfoRes {
        @Schema(description = "사용자 ID", example = "1")
        private Long userId;
        @Schema(description = "이메일", example = "user1@example.com")
        private String email;
        @Schema(description = "이름", example = "정재")
        private String name;
        @Schema(description = "로케일", example = "ko")
        private String locale;
        @Schema(description = "역할", example = "USER")
        private String role;
        @Schema(description = "프로필 이미지 URL", example = "https://.../avatar.png")
        private String profileImageUrl;

 public static UserInfoRes from(User u) {
        if (u == null) { // 만약을 위한 방어 코드
            return null;
        }

        UserInfoRes r = new UserInfoRes();
        r.setUserId(u.getUserId());
        r.setEmail(u.getEmail());
        r.setName(u.getName());
        r.setLocale(u.getLocale());
        
        // ✅ 이 부분이 NullPointerException을 막아주는 핵심입니다!
        // User의 role 필드가 Enum 타입이라고 가정
        if (u.getRole() != null) {
            // .name()은 Enum의 이름을 String으로 안전하게 가져옵니다.
            r.setRole(u.getRole().name()); 
        } else {
            // role 필드가 null이면, DTO의 role도 null로 설정합니다.
            r.setRole(null); 
        }

        r.setProfileImageUrl(u.getProfileImageUrl());
        return r;
    }

    }
}
