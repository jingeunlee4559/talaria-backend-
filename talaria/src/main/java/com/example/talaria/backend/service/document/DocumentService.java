package com.example.talaria.backend.service.document;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.mapper.document.DocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentMapper mapper;

    private static final String TABLE = "tl_documents";
    private static final String PK    = "doc_id";
    private static final int    MAX_RETRY = 50; // 자동 리네임 최대 재시도

    // 기본 저장 루트 (application.yaml에서 주입). 미지정 시 개발용 디폴트
    @Value("${app.upload.docs-dir:talaria-backend/talaria/src/main/resources/uploadfile/docs}")
    private String docsRootDir;

    // ========================= 조회/단건 =========================

    public PageResponse listByUser(long userId, Long meetingId, int page, int size) {
        if (size > 100) size = 100;
        int offset = page * size;
        long total = mapper.countByUser(TABLE, PK, userId, meetingId);
        List<Map<String, Object>> items =
            mapper.selectPageByUser(TABLE, PK, userId, meetingId, size, offset);
        return new PageResponse(items, page, size, total);
    }

    public Map<String, Object> getOneByUser(long userId, long docId) {
        var row = mapper.selectOneByUser(TABLE, PK, userId, docId);
        if (row == null || row.isEmpty()) throw new RuntimeException("Document not found or not owned");
        return row;
    }

    // ========================= 업로드(멀티파트) =========================

    /**
     * 멀티파트 파일을 받아 DB에 기록(자동 리네임 반영) 후, 실제 파일을 docsRootDir 아래에 저장.
     * - subdir: 선택. 예) "user/42/docs" (null이면 루트 바로 아래)
     */
    @Transactional
    public Map<String, Object> createFromMultipart(long userId, Long meetingId,
                                                   MultipartFile file, String originalLang,
                                                   String subdir) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is empty");
        }

        String safeName = sanitizeFilename(file.getOriginalFilename());
        String ext = getExtension(safeName);
        String mimeType = (file.getContentType() != null) ? file.getContentType() : "application/octet-stream";
        long size = file.getSize();

        // 클라이언트가 sha256을 주지 않는 케이스 대비 서버 측 계산
        String sha256 = sha256Hex(file.getInputStream());

        // 클라이언트가 원하는 상대경로(디렉토리 포함) 구성
        String dir = (StringUtils.hasText(subdir) ? normalizeRelDir(subdir) + "/" : "");
        String requestedRelativePath = dir + safeName; // 예: "user/42/docs/report.pdf"

        // DB insert (자동 리네임 포함) → 최종 filepath 결정
        Map<String, Object> row = create(userId, meetingId, safeName, requestedRelativePath, mimeType, size, sha256, originalLang, /*storageKey*/ null);

        // DB에 최종 확정된 filepath 사용하여 실제 파일 저장
        String decidedRelPath = (String) row.get("filepath");
        if (!StringUtils.hasText(decidedRelPath)) {
            throw new IllegalStateException("Decided filepath not returned from DB");
        }
        writeToDisk(file, decidedRelPath); // 실패 시 예외 → 트랜잭션 롤백

        // storage_key 컬럼을 스토리지 실제 경로로 갱신하고 싶으면 여기서 별도 update 수행 가능
        // (Mapper에 update 추가 필요) 예: mapper.updateStorageKey(TABLE, (Long)row.get("doc_id"), decidedRelPath);

        return row;
    }

    // ========================= 업로드(DB insert + 자동 리네임) =========================

    /**
     * filepath UNIQUE 위반 시 서버가 자동으로 (n) suffix를 붙여 재시도하여 저장합니다.
     * - 예) report.pdf -> report(1).pdf -> report(2).pdf ...
     * - 확장자 없는 경우: README -> README(1) ...
     */
    @Transactional
    public Map<String, Object> create(long userId, Long meetingId,
                                      String originalName, String filepath, String mimeType,
                                      long size, String sha256, String originalLang, String storageKey) {

        String decidedPath = filepath;

        // 1) 먼저 원래 경로로 시도
        try {
            mapper.insert(TABLE, userId, meetingId, originalName, decidedPath, mimeType, size, sha256, originalLang, storageKey);
        } catch (DataIntegrityViolationException ex) {
            // 2) 충돌이면 자동 리네임으로 재시도
            decidedPath = autoRenameAndInsert(userId, meetingId, originalName, filepath, mimeType, size, sha256, originalLang, storageKey);
        }

        long id = mapper.lastInsertId();
        // 최종 저장된 레코드 반환 (여기에 filepath가 포함되어 있으므로 createFromMultipart에서 사용)
        return mapper.selectOneByUser(TABLE, PK, userId, id);
    }

    public int delete(long userId, long docId) {
        return mapper.deleteByUser(TABLE, PK, userId, docId);
    }

    // ========================= 파일 저장 유틸 =========================

    private void writeToDisk(MultipartFile file, String decidedRelPath) throws IOException {
        Path root = Paths.get(docsRootDir).toAbsolutePath().normalize();
        Path target = root.resolve(decidedRelPath).normalize();

        // 경로 역탐색 방지
        if (!target.startsWith(root)) {
            throw new SecurityException("Invalid path traversal detected: " + decidedRelPath);
        }

        // 상위 디렉토리 생성
        Files.createDirectories(target.getParent());

        // 원자적 쓰기: 임시파일에 저장 후 move
        Path tmp = Files.createTempFile(target.getParent(), ".upload-", ".part");
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.deleteIfExists(tmp);
            throw e;
        }

        // 기존 동일 파일이 우연히 있을 수 있으니 REPLACE_EXISTING
        Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    private static String sanitizeFilename(String original) {
        if (!StringUtils.hasText(original)) return "unnamed";
        String base = Paths.get(original).getFileName().toString();  // 경로 제거
        // 제어문자/금지문자 제거
        base = base.replaceAll("[\\p{Cntrl}]", "");
        base = base.replaceAll("[<>:\"\\\\/|?*]", "_"); // Windows 금지문자
        if (!StringUtils.hasText(base)) base = "unnamed";
        // 너무 긴 이름 방지 (예: 200자 제한)
        if (base.length() > 200) base = base.substring(0, 200);
        return base;
    }

    private static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot > 0 && dot < filename.length() - 1) ? filename.substring(dot + 1) : "";
    }

    /** 사용자가 보낸 subdir를 상대 경로로 정규화 (역탐색 차단) */
    private static String normalizeRelDir(String subdir) {
        String s = subdir.trim().replace('\\', '/');
        s = s.replaceAll("^/+", "").replaceAll("/+$", ""); // 앞/뒤 슬래시 제거
        // 위험 패턴 제거
        if (s.contains("..")) throw new SecurityException("Invalid subdir");
        if (s.contains("//")) s = s.replaceAll("/+", "/");
        return s;
    }

    private static String sha256Hex(InputStream in) throws IOException {
        try (in) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) md.update(buf, 0, r);
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            throw new IOException("Failed to compute sha256", e);
        }
    }

    // ================= 자동 리네임 유틸/로직 =================

    private String autoRenameAndInsert(long userId, Long meetingId, String originalName,
                                       String originalFilepath, String mimeType, long size,
                                       String sha256, String originalLang, String storageKey) {

        PathParts parts = PathParts.parse(originalFilepath);
        int n = (parts.existingSuffix == null) ? 1 : (parts.existingSuffix + 1);

        for (int i = 0; i < MAX_RETRY; i++, n++) {
            String candidate = parts.build(n);
            try {
                mapper.insert(TABLE, userId, meetingId, originalName, candidate, mimeType, size, sha256, originalLang, storageKey);
                return candidate;
            } catch (DataIntegrityViolationException ignore) {
                // 충돌 → 다음 번호
            }
        }
        throw new IllegalStateException("파일명 자동 리네임 실패: 재시도 한도 초과 (" + MAX_RETRY + ")");
    }

    /** "a/b/report(3).pdf" -> dir="a/b/", base="report", ext="pdf", existingSuffix=3 */
    private static class PathParts {
        final String dir;             // 끝에 '/' 포함 또는 빈 문자열
        final String base;            // 괄호-번호 제거된 파일명(확장자 제외)
        final String ext;             // 확장자(없으면 빈 문자열)
        final Integer existingSuffix; // 기존 (n)

        private static final Pattern SUFFIX_RE = Pattern.compile("^(.*)\\((\\d+)\\)$");

        private PathParts(String dir, String base, String ext, Integer existingSuffix) {
            this.dir = dir;
            this.base = base;
            this.ext = ext;
            this.existingSuffix = existingSuffix;
        }

        static PathParts parse(String filepath) {
            int slash = filepath.lastIndexOf('/');
            String dir = (slash >= 0) ? filepath.substring(0, slash + 1) : "";
            String name = (slash >= 0) ? filepath.substring(slash + 1) : filepath;

            int dot = name.lastIndexOf('.');
            String ext = (dot > 0) ? name.substring(dot + 1) : "";
            String stem = (dot > 0) ? name.substring(0, dot) : name;

            Integer suffix = null;
            String base = stem;
            Matcher m = SUFFIX_RE.matcher(stem);
            if (m.matches()) {
                base = m.group(1);
                suffix = Integer.parseInt(m.group(2));
            }
            return new PathParts(dir, base, ext, suffix);
        }

        String build(int n) {
            String stem = base + "(" + n + ")";
            return dir + (ext.isEmpty() ? stem : (stem + "." + ext));
        }
    }
    @Transactional
    public Map<String, Object> toggleUseContextRag(long userId, long docId) {
        int updated = mapper.toggleUseContextRag(TABLE, userId, docId);
        if (updated <= 0) {
            throw new IllegalArgumentException("문서를 찾을 수 없거나 권한이 없습니다.");
        }
        return mapper.selectOneByUser(TABLE, PK ,userId, docId);
    }

}
