package com.example.talaria.backend.controller;

import com.example.talaria.backend.service.asset.FrontAssetStorage;
import com.example.talaria.backend.service.meeting.MeetingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/assets")
public class AssetController {

    private final FrontAssetStorage storage;
    private final MeetingService meetingService;

    /** ① 파일만 저장 → URL/상대경로 반환 */
    @Operation(summary = "문화 썸네일 업로드(파일만 저장)")
    @PostMapping(path = "/api/assets/culture-thumb",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadThumb(
            @Parameter(description = "업로드 이미지 파일", required = true)
            @RequestPart("file") MultipartFile file) {
        try {
            String rel = storage.saveCultureThumb(file); // 예: "img/17318123_foo.png"
            if (rel == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "empty_file"));
            }
            return ResponseEntity.ok(Map.of(
                    "relPath", rel,
                    "url", "/" + rel
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "upload_failed",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * ② 저장 + 특정 회의에 즉시 반영 + 비공개 설정까지 업데이트
     *
     * - thumbnail: @RequestPart("file")
     * - isPrivate: @RequestParam("isPrivate") (nullable, 안 보내면 기존 값 유지)
     * - privatePassword: @RequestParam("privatePassword") (nullable)
     */
    @PostMapping(
            path = "/users/{userId}/meetings/{id}/thumb",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadAndAttachThumb(@PathVariable long userId,
                                                  @PathVariable long id,
                                                  @RequestPart("file") MultipartFile file,
                                                  @RequestParam(value = "isPrivate", required = false) Boolean isPrivate,
                                                  @RequestParam(value = "privatePassword", required = false) String privatePassword) {
        try {
            // isPrivate=true를 명시적으로 요청했는데 비밀번호가 비어 있으면 400
            if (Boolean.TRUE.equals(isPrivate)
                    && (privatePassword == null || privatePassword.isBlank())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "missing_password",
                        "message", "비공개 회의는 privatePassword가 필수입니다."
                ));
            }

            String rel = storage.saveCultureThumb(file);
            if (rel == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "empty_file"));
            }

            // 썸네일 + 비공개 정보 동시 업데이트
            int updated = meetingService.updateThumbAndPrivacy(
                    userId,
                    id,
                    rel,
                    isPrivate,                              // null 이면 is_private 그대로 유지
                    Boolean.TRUE.equals(isPrivate)
                            ? privatePassword
                            : null                           // 공개로 바꾸면 비밀번호는 null로
            );

            return ResponseEntity.ok(Map.of(
                    "updated", updated,
                    "relPath", rel.startsWith("/") ? rel : ("/" + rel),
                    "url", rel.startsWith("/") ? rel : ("/" + rel),
                    "isPrivate", isPrivate
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "upload_failed",
                    "message", e.getMessage()
            ));
        }
    }
}
