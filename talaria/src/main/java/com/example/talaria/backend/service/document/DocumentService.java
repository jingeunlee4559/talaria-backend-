package com.example.talaria.backend.service.document;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.mapper.document.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentMapper mapper;

    private static final String TABLE = "tl_documents";
    private static final String PK    = "doc_id";
    private static final int    MAX_RETRY = 50;

    @Value("${storage.documents.root:./data/uploads/docs}")
    private String docsRootDir;

    // ========================= Query Methods =========================

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
        if (row == null || row.isEmpty()) {
            throw new RuntimeException("Document not found or not owned");
        }
        return row;
    }

    // ========================= Upload Methods =========================
    
    @Transactional
    public Map<String, Object> createFromMultipart(long userId, Long meetingId,
                                                   MultipartFile file, String originalLang,
                                                   String subdir) throws IOException {
        return createFromMultipart(userId, meetingId, file, originalLang, subdir, false);
    }

    @Transactional
    public Map<String, Object> createFromMultipart(long userId, Long meetingId,
                                                   MultipartFile file, String originalLang,
                                                   String subdir,
                                                   boolean useContextRag) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is empty");
        }

        String safeName = sanitizeFilename(file.getOriginalFilename());
        String ext = getExtension(safeName);
        String mimeType = (file.getContentType() != null) ? file.getContentType() : "application/octet-stream";
        long size = file.getSize();

        String sha256 = sha256Hex(file.getInputStream());

        String dir = (StringUtils.hasText(subdir) ? normalizeRelDir(subdir) + "/" : "");
        String requestedRelativePath = dir + safeName;

        Map<String, Object> row = create(
                userId, meetingId, safeName, requestedRelativePath,
                mimeType, size, sha256, originalLang, null,
                useContextRag
        );

        String decidedRelPath = (String) row.get("filepath");
        if (!StringUtils.hasText(decidedRelPath)) {
            throw new IllegalStateException("Decided filepath not returned from DB");
        }
        writeToDisk(file, decidedRelPath);

        return row;
    }

    @Transactional
    public Map<String, Object> create(long userId, Long meetingId,
                                      String originalName, String filepath, String mimeType,
                                      long size, String sha256, String originalLang, String storageKey,
                                      boolean useContextRag) {

        String decidedPath = filepath;

        try {
            mapper.insert(TABLE, userId, meetingId, originalName, decidedPath,
                    mimeType, size, sha256, originalLang, storageKey, useContextRag);
        } catch (DataIntegrityViolationException ex) {
            decidedPath = autoRenameAndInsert(userId, meetingId, originalName, filepath,
                    mimeType, size, sha256, originalLang, storageKey, useContextRag);
        }

        long id = mapper.lastInsertId();
        return mapper.selectOneByUser(TABLE, PK, userId, id);
    }

    public int delete(long userId, long docId) {
        return mapper.deleteByUser(TABLE, PK, userId, docId);
    }

    // ========================= File Content Reading =========================

    public Map<String, Object> getDocumentContent(long userId, long docId) throws IOException {
        Map<String, Object> row = mapper.selectOneByUser(TABLE, PK, userId, docId);
        if (row == null || row.isEmpty()) {
            throw new SecurityException("Document not found or access denied");
        }

        String rel = asString(row.get("filepath"));
        String originalName = asString(row.get("original_name"));
        String mimeType = asString(row.get("mime_type"));
        String originalLang = asString(row.get("original_lang"));

        if (rel == null || rel.isBlank()) {
            throw new IllegalStateException("Filepath not found in DB");
        }

        Path root = Paths.get(docsRootDir).toAbsolutePath().normalize();
        Path target = root.resolve(rel.replace('\\', '/')).normalize();

        if (!target.startsWith(root)) {
            throw new SecurityException("Invalid path");
        }

        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            throw new IOException("File not found");
        }

        String content;
        try {
            content = readFileContent(target, mimeType);
        } catch (UnsupportedOperationException e) {
            log.warn("Unsupported file type: {}", target.getFileName(), e);
            throw e;
        }

        return Map.of(
                "doc_id", docId,
                "original_name", originalName,
                "mime_type", mimeType,
                "original_lang", originalLang,
                "original_content", content,
                "has_translation", false,
                "translated_content", ""
        );
    }

    /**
     * Extract text from various file formats
     */
    private String readFileContent(Path file, String mimeType) throws IOException {
        long size = Files.size(file);
        if (size > 50 * 1024 * 1024) {
            throw new UnsupportedOperationException("File too large (max 50MB)");
        }

        String fileName = file.getFileName().toString().toLowerCase();

        try {
            // PDF
            if (fileName.endsWith(".pdf") || 
                (mimeType != null && mimeType.equals("application/pdf"))) {
                return extractTextFromPDF(file);
            }

            // DOCX (Office 2007+)
            if (fileName.endsWith(".docx") || 
                (mimeType != null && mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
                return extractTextFromDOCX(file);
            }

            // DOC (Old Word)
            if (fileName.endsWith(".doc") || 
                (mimeType != null && mimeType.equals("application/msword"))) {
                return extractTextFromDOC(file);
            }

            // HWP - Not supported yet
            if (fileName.endsWith(".hwp")) {
                throw new UnsupportedOperationException(
                    "HWP file preview is not currently supported. Please download the file to view it.");
            }

            // Text files
            if (mimeType != null && (
                    mimeType.startsWith("text/") ||
                    mimeType.equals("application/json") ||
                    mimeType.equals("application/xml")
            )) {
                return Files.readString(file, StandardCharsets.UTF_8);
            }

            if (fileName.endsWith(".txt") || fileName.endsWith(".md") || 
                fileName.endsWith(".csv") || fileName.endsWith(".json")) {
                return Files.readString(file, StandardCharsets.UTF_8);
            }

            throw new UnsupportedOperationException("Unsupported file format: " + fileName);

        } catch (UnsupportedOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to read file: {}", file.getFileName(), e);
            throw new IOException("Error reading file: " + e.getMessage(), e);
        }
    }

    /**
     * Extract text from PDF using PDFBox 2.x
     */
    private String extractTextFromPDF(Path file) throws IOException {
        try (PDDocument document = PDDocument.load(file.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            
            int totalPages = document.getNumberOfPages();
            if (totalPages > 50) {
                stripper.setEndPage(50);
                log.info("PDF has {} pages, extracting first 50", totalPages);
            }
            
            String text = stripper.getText(document);
            
            if (totalPages > 50) {
                text += "\n\n... [Showing first 50 of " + totalPages + " pages]";
            }
            
            return text;
        }
    }

    /**
     * Extract text from DOCX (Office 2007+)
     */
    private String extractTextFromDOCX(Path file) throws IOException {
        try (InputStream fis = Files.newInputStream(file);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    /**
     * Extract text from DOC (Old Word format)
     */
    private String extractTextFromDOC(Path file) throws IOException {
        try (InputStream fis = Files.newInputStream(file);
             HWPFDocument document = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    // ========================= File Storage Utils =========================

    private void writeToDisk(MultipartFile file, String decidedRelPath) throws IOException {
        Path root = Paths.get(docsRootDir).toAbsolutePath().normalize();
        Path target = root.resolve(decidedRelPath).normalize();

        if (!target.startsWith(root)) {
            throw new SecurityException("Invalid path traversal detected: " + decidedRelPath);
        }

        Files.createDirectories(target.getParent());

        Path tmp = Files.createTempFile(target.getParent(), ".upload-", ".part");
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.deleteIfExists(tmp);
            throw e;
        }

        Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    private static String sanitizeFilename(String original) {
        if (!StringUtils.hasText(original)) return "unnamed";
        String base = Paths.get(original).getFileName().toString();
        base = base.replaceAll("[\\p{Cntrl}]", "");
        base = base.replaceAll("[<>:\"\\\\/|?*]", "_");
        if (!StringUtils.hasText(base)) base = "unnamed";
        if (base.length() > 200) base = base.substring(0, 200);
        return base;
    }

    private static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot > 0 && dot < filename.length() - 1) ? filename.substring(dot + 1) : "";
    }

    private static String normalizeRelDir(String subdir) {
        String s = subdir.trim().replace('\\', '/');
        s = s.replaceAll("^/+", "").replaceAll("/+$", "");
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

    // ================= Auto-rename logic =================

    private String autoRenameAndInsert(long userId, Long meetingId, String originalName,
                                       String originalFilepath, String mimeType, long size,
                                       String sha256, String originalLang, String storageKey,
                                       boolean useContextRag) {

        PathParts parts = PathParts.parse(originalFilepath);
        int n = (parts.existingSuffix == null) ? 1 : (parts.existingSuffix + 1);

        for (int i = 0; i < MAX_RETRY; i++, n++) {
            String candidate = parts.build(n);
            try {
                mapper.insert(TABLE, userId, meetingId, originalName, candidate,
                        mimeType, size, sha256, originalLang, storageKey, useContextRag);
                return candidate;
            } catch (DataIntegrityViolationException ignore) {
            }
        }
        throw new IllegalStateException("Auto-rename failed: max retries exceeded (" + MAX_RETRY + ")");
    }

    private static class PathParts {
        final String dir;
        final String base;
        final String ext;
        final Integer existingSuffix;

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

    // ========================= Toggle =========================

    @Transactional
    public Map<String, Object> toggleUseContextRag(long userId, long docId) {
        int updated = mapper.toggleUseContextRag(TABLE, userId, docId);
        if (updated <= 0) {
            throw new IllegalArgumentException("Document not found or access denied");
        }
        return mapper.selectOneByUser(TABLE, PK, userId, docId);
    }

    // ========================= Download =========================

    public record DownloadFile(Path path, String downloadName, String mimeType) {}

    public DownloadFile getFileForDownload(long userId, long docId) {
        Map<String, Object> row = mapper.selectOneByUser(TABLE, PK, userId, docId);
        if (row == null || row.isEmpty()) {
            throw new SecurityException("Document not found or access denied");
        }

        String rel = asString(row.get("filepath"));
        String originalName = asString(row.get("original_name"));
        String mimeType = asString(row.get("mime_type"));

        if (rel == null || rel.isBlank()) {
            throw new IllegalStateException("Filepath not found in DB");
        }

        Path root = Paths.get(docsRootDir).toAbsolutePath().normalize();
        Path target = root.resolve(rel.replace('\\', '/')).normalize();

        if (!target.startsWith(root)) {
            throw new SecurityException("Invalid path");
        }

        String downloadName = (originalName != null && !originalName.isBlank())
                ? originalName
                : target.getFileName().toString();

        return new DownloadFile(target, downloadName, (mimeType == null || mimeType.isBlank())
                ? "application/octet-stream" : mimeType);
    }

    private static String asString(Object v) {
        return (v == null) ? null : String.valueOf(v);
    }
}