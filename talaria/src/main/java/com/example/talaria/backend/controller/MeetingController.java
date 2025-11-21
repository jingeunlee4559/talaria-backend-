package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.asset.FrontAssetStorage;
import com.example.talaria.backend.service.meeting.MeetingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/meetings")
public class MeetingController {

    private final MeetingService service;
    private final FrontAssetStorage frontAssetStorage; // 프론트 퍼블릭 디렉터리 저장

    // 목록 조회 (호스트 기준)
    @GetMapping
    public ResponseEntity<PageResponse> list(
            @PathVariable("userId") long userId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(name = "page", defaultValue = "0")  int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(service.listByHost(userId, status, from, to, page, size));
    }

    // 단건 조회 (호스트 기준)
    @GetMapping("/{id}")
    public ResponseEntity<Map<String,Object>> getOne(
            @PathVariable("userId") long userId,
            @PathVariable("id") long id) {
        var row = service.getOneByHost(userId, id);
        return row == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(row);
    }

    // 초대코드로 조회 (호스트 제약 없음)
    @GetMapping("/by-invite/{code}")
    public ResponseEntity<Map<String,Object>> getByInvite(@PathVariable("code") String code) {
        var row = service.getByInviteCode(code);
        return row == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(row);
    }

    // (1) JSON 바디로 생성 (파일 업로드 없음)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createJson(
            @PathVariable("userId") long userId,
            @RequestBody @Valid CreateMeetingReq req) {
        try {
            var created = service.create(
                    userId,
                    req.getTitle(),
                    req.getDefaultLang(),
                    req.getScheduledAt(),
                    normalizeMode(req.getMeetingMode()),                // 'B' or 'C'
                    emptyToNull(req.getCultureThumbRelPath()),         // 파일 없이 경로만 전달 가능
                    normalizeStatus(req.getStatus()),
                    emptyToNull(req.getCultureDesc()),                 // culture_desc
                    req.getIsPrivate(),                                // 비공개 여부
                    emptyToNull(req.getPrivatePassword())              // 비밀번호
            );
            return ResponseEntity.ok(created);
        } catch (IllegalStateException e) {
            var pd = ProblemDetail.forStatus(409);
            pd.setTitle("Conflict");
            pd.setDetail(e.getMessage());
            pd.setInstance(URI.create("/api/users/" + userId + "/meetings"));
            return ResponseEntity.status(409).body(Map.of(
                    "type", "about:blank",
                    "title", pd.getTitle(),
                    "status", 409,
                    "detail", pd.getDetail(),
                    "instance", pd.getInstance().toString()
            ));
        }
    }

    // (2) multipart/form-data로 생성 (파일 업로드 포함)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createWithThumb(
            @PathVariable("userId") long userId,
            @Valid @ModelAttribute CreateMeetingReq req,                       // 텍스트 파트들
            @RequestPart(name = "cultureThumb", required = false) MultipartFile cultureThumb // 파일 파트
    ) {
        try {
            // 파일 저장 (null/빈파일이면 null 리턴)
            String savedRel = frontAssetStorage.saveCultureThumb(cultureThumb);

            var created = service.create(
                    userId,
                    req.getTitle(),
                    req.getDefaultLang(),
                    req.getScheduledAt(),
                    normalizeMode(req.getMeetingMode()),
                    // 파일이 올라오면 저장된 경로 사용, 아니면 사용자가 form-data로 준 경로 사용
                    (savedRel != null ? savedRel : emptyToNull(req.getCultureThumbRelPath())),
                    normalizeStatus(req.getStatus()),
                    emptyToNull(req.getCultureDesc()),                 // culture_desc
                    req.getIsPrivate(),                                // 비공개 여부
                    emptyToNull(req.getPrivatePassword())              // 비밀번호
            );
            return ResponseEntity.ok(created);
        } catch (IllegalStateException e) {
            var pd = ProblemDetail.forStatus(409);
            pd.setTitle("Conflict");
            pd.setDetail(e.getMessage());
            pd.setInstance(URI.create("/api/users/" + userId + "/meetings"));
            return ResponseEntity.status(409).body(Map.of(
                    "type","about:blank","title",pd.getTitle(),"status",409,
                    "detail",pd.getDetail(),"instance",pd.getInstance().toString()
            ));
        } catch (Exception ex) {
            var pd = ProblemDetail.forStatus(500);
            pd.setTitle("Server Error");
            pd.setDetail("Upload failed: " + ex.getMessage());
            pd.setInstance(URI.create("/api/users/" + userId + "/meetings"));
            return ResponseEntity.status(500).body(Map.of(
                    "type","about:blank","title",pd.getTitle(),"status",500,
                    "detail",pd.getDetail(),"instance",pd.getInstance().toString()
            ));
        }
    }

    // 부분 수정(제목/언어/시간/상태 등) — 필요 시 cultureDesc도 PATCH에 포함 가능
    @PatchMapping("/{id}")
    public ResponseEntity<Map<String,Object>> patch(
            @PathVariable("userId") long userId,
            @PathVariable("id") long id,
            @RequestBody PatchMeetingReq req) {
        int n = service.updatePartial(userId, id,
                req.getTitle(), req.getStatus(), req.getInviteCode(),
                req.getDefaultLang(), req.getScheduledAt(), req.getEndedAt());
        return ResponseEntity.ok(Map.of("updated", n));
    }

    // 시작(STATUS: SCHEDULED -> ONGOING)
    @PatchMapping("/{id}/start")
    public ResponseEntity<Map<String,Object>> start(
            @PathVariable("userId") long userId, @PathVariable("id") long id) {
        int n = service.start(userId, id);
        return ResponseEntity.ok(Map.of("updated", n));
    }

    // 종료(STATUS: * -> ENDED, ended_at 세팅)
    @PatchMapping("/{id}/end")
    public ResponseEntity<Map<String,Object>> end(
            @PathVariable("userId") long userId, @PathVariable("id") long id) {
        int n = service.end(userId, id);
        return ResponseEntity.ok(Map.of("updated", n));
    }

    // 초대코드 재발급
    @PostMapping("/{id}/invite/regenerate")
    public ResponseEntity<Map<String,Object>> regenerateInvite(
            @PathVariable("userId") long userId, @PathVariable("id") long id) {
        int n = service.regenerateInvite(userId, id);
        return ResponseEntity.ok(Map.of("updated", n));
    }

    // 내가 호스트이거나 참가자인 모든 회의 조회
    @GetMapping("/all")
    public ResponseEntity<PageResponse> listAll(
        @PathVariable("userId") long userId,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "from", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(name = "to", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
        @RequestParam(name = "mode", required = false) String mode,
        @RequestParam(name = "page", defaultValue = "0")  int page,
        @RequestParam(name = "size", defaultValue = "20") int size) {

        // mode 정제: null/빈문자 → null, "b"/"c" → "B"/"C", 그 외 값은 400
        String normalizedMode = null;
        if (mode != null && !mode.isBlank()) {
            normalizedMode = mode.trim().toUpperCase();
            if (!normalizedMode.equals("B") && !normalizedMode.equals("C")) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "mode 파라미터는 B 또는 C 만 허용됩니다."
                );
            }
        }

        return ResponseEntity.ok(
            service.listByUserIncludingParticipant(userId, status, from, to, normalizedMode, page, size)
        );
    }
    // 비공개 회의 비밀번호 검증
    @PostMapping("/{meetingId}/check-password")
    public ResponseEntity<Map<String, Object>> checkPrivatePassword(
            @PathVariable("userId") long userId,
            @PathVariable("meetingId") long meetingId,
            @RequestBody Map<String, String> body
    ) {
        String password = body.get("password");
        boolean ok = service.checkPrivatePassword(meetingId, password);

        return ResponseEntity.ok(Map.of("valid", ok));
    }


    // ===== DTO =====
    @Data
    public static class CreateMeetingReq {
        @NotBlank @Size(max = 200)
        private String title;

        @Size(max = 10)
        private String defaultLang = "ko";

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime scheduledAt; // null 허용

        @Pattern(regexp = "^(SCHEDULED|ONGOING|ENDED)$",
                 message = "status must be SCHEDULED, ONGOING, or ENDED")
        private String status;

        @Pattern(regexp = "^[BC]$", message = "meetingMode must be 'B' or 'C'")
        private String meetingMode;                // null → 기본 B

        @Size(max = 512)
        private String cultureThumbRelPath;        // 파일 없이 경로만 줄 때 사용

        @Size(max = 500)
        private String cultureDesc;                // culture_desc

        // C 모드 비공개 설정
        private Boolean isPrivate;                 // true면 비공개, false/null이면 공개

        @Size(max = 128)
        private String privatePassword;            // 비공개방 비밀번호 (C 모드 + isPrivate=true일 때 사용)
    }

    @Data
    public static class PatchMeetingReq {
        @Size(max = 200)
        private String title;
        private String status; // 'SCHEDULED' | 'ONGOING' | 'ENDED'
        @Size(max = 255)
        private String inviteCode;
        @Size(max = 10)
        private String defaultLang;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime scheduledAt;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime endedAt;
        // 필요 시 cultureDesc / isPrivate / privatePassword 도 PATCH에 포함 가능
    }

    // ===== helpers =====
    private static String normalizeMode(String m) {
        if (m == null) return "B";
        String u = m.trim().toUpperCase();
        return (u.equals("C")) ? "C" : "B";
    }
    private static String normalizeStatus(String s) {
        if (s == null) return "SCHEDULED";
        String u = s.trim().toUpperCase();
        return (u.equals("SCHEDULED") || u.equals("ONGOING") || u.equals("ENDED")) ? u : "SCHEDULED";
    }
    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
