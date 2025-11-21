package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.asset.FrontAssetStorage;
import com.example.talaria.backend.service.meeting.MeetingService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/meetings")
public class MeetingController {

    private final MeetingService service;
    private final FrontAssetStorage frontAssetStorage; // 프론트 퍼블릭 디렉터리 저장
    private final ObjectMapper objectMapper;          // ★ 문자열 JSON 파싱용

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

    // (2) multipart/form-data로 생성 (파일 업로드 포함, req는 문자열 JSON)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createWithThumb(
            @PathVariable("userId") long userId,
            @RequestPart(name = "req", required = false) String reqJson,             // ★ 문자열
            @RequestPart(name = "cultureThumb", required = false) MultipartFile cultureThumb
    ) {
        try {
            // reqJson → DTO 변환
            CreateMeetingReq req;
            if (reqJson == null || reqJson.isBlank()) {
                throw new ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "req 파트(JSON 문자열)가 필요합니다."
                );
            }
            try {
                req = objectMapper.readValue(reqJson, CreateMeetingReq.class);
            } catch (Exception parseEx) {
                throw new ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "req JSON 파싱 실패: " + parseEx.getMessage()
                );
            }

            // title 필수 검증 (예전처럼 @NotBlank 대체)
            if (req.getTitle() == null || req.getTitle().isBlank()) {
                throw new ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "title은 공백일 수 없습니다."
                );
            }

            // 파일 저장 (null/빈파일이면 null 리턴)
            String savedRel = frontAssetStorage.saveCultureThumb(cultureThumb);

            var created = service.create(
                    userId,
                    req.getTitle(),
                    req.getDefaultLang(),
                    req.getScheduledAt(),
                    normalizeMode(req.getMeetingMode()),
                    (savedRel != null ? savedRel : emptyToNull(req.getCultureThumbRelPath())),
                    normalizeStatus(req.getStatus()),
                    emptyToNull(req.getCultureDesc()),
                    req.getIsPrivate(),
                    emptyToNull(req.getPrivatePassword())
            );
            return ResponseEntity.ok(created);
        } catch (ResponseStatusException ex) {
            // 400 등 명시적 에러
            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of(
                            "status", ex.getStatusCode().value(),
                            "error", ex.getReason()
                    ));
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

    // 부분 수정(제목/언어/시간/상태 등)
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

        // mode 정제
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

        @Pattern(regexp = "^[BC]$", message = "meetingMode must be 'B' or 'C'") // null → 기본 B
        private String meetingMode;

        @Size(max = 512)
        private String cultureThumbRelPath;

        @Size(max = 500)
        private String cultureDesc;

        // C 모드 비공개 설정
        private Boolean isPrivate; // true면 비공개, false/null이면 공개

        @Size(max = 128)
        private String privatePassword;
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
