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
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
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
    @PatchMapping(value = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "유저 정보 수정",
        description = "name / locale / profileImageUrl 수정, 비밀번호 변경은 currentPassword + newPassword 동시 전달"
    )
    public ResponseEntity<?> updateUser(
            @PathVariable("userId") long userId,
            @Valid @RequestBody UpdateUserReq req   // <= Spring의 RequestBody 사용
    ) {
        try {
            User updated = userService.updateUserPartial(
                    userId,
                    req.getName(),
                    req.getLocale(),
                    req.getProfileImageUrl(),
                    req.getCurrentPassword(),
                    req.getNewPassword()
            );
            return ResponseEntity.ok(UserInfoRes.from(updated));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(iae.getMessage());
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(se.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Update failed");
        }
    }
    @Operation(summary = "사용자 ID로 조회", description = "비밀번호를 제외한 사용자 정보를 반환합니다.")
    @GetMapping("/{userId}")
    public ResponseEntity<?> getById(
            @Parameter(name = "userId", in = ParameterIn.PATH, required = true, description = "조회할 사용자 ID")
            @PathVariable("userId") long userId
    ) {
        User u = userService.getById(userId);
        if (u == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        return ResponseEntity.ok(UserInfoRes.from(u));
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
            
            if (u.getRole() != null) {
                r.setRole(u.getRole().name()); 
            } else {
                r.setRole(null); 
            }

            r.setProfileImageUrl(u.getProfileImageUrl());
            return r;
        }

    }
    /** 수정 요청 DTO */
    @Data
    public static class UpdateUserReq {
        @Size(min = 1, max = 64, message = "name은 1~64자")
        private String name;

        @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "locale 형식 예: ko 또는 en-US")
        private String locale;

        @Size(max = 255, message = "profileImageUrl 최대 255자") // 스키마에 맞춰 255로
        private String profileImageUrl;

        @Size(min = 1, max = 128)
        private String currentPassword;

        @Size(min = 1, max = 128, message = "새 비밀번호는 1자 이상")
        private String newPassword;
    }
}
