package com.example.talaria.backend.service.document;

import com.example.talaria.backend.mapper.document.DocumentMapper;
import com.example.talaria.backend.mapper.document.TransDocumentMapper;
import com.example.talaria.backend.mapper.document.MeetingDocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransDocumentService {

    private final DocumentMapper documentMapper;          // tl_documents
    private final TransDocumentMapper transDocMapper;     // tl_trans_docs
    private final MeetingDocumentMapper meetingDocMapper; // tl_meeting_documents

    @Value("${storage.transdocs.root:./data/uploads/transdocs}")
    private String transDocsRootDir;

    // 회의방 전용 미리보기/다운로드 공용 DTO
    public record DownloadFile(Path path, String downloadName, String mimeType) {}

    /**
     * 회의방 전용 번역 문서 파일 조회
     * - userId: 현재 요청 유저 (지금은 직접 사용 X, 나중에 참가자 검증 등에 활용 가능)
     * - meetingId: 해당 회의 문서인지 체크 (tl_meeting_documents 기준)
     * - transDocId: tl_trans_docs.trans_doc_id
     *
     * 권한 모델:
     *   1) tl_meeting_documents에서
     *      - meeting_id  = meetingId
     *      - doc_type    = 'TRANSLATION'
     *      - trans_doc_id = transDocId
     *      → 행이 존재해야 함
     *   2) tl_trans_docs에서 transDocId로 메타 조회
     *   3) tl_documents에서 original_doc_id로 원문 메타 조회
     */
    public DownloadFile getFileForMeetingPreview(long userId, long meetingId, long transDocId) {
        // 1) 이 번역 문서가 해당 회의에 실제로 붙어 있는지 확인
        Map<String, Object> linkRow =
                meetingDocMapper.selectTranslationForMeeting(meetingId, transDocId);
        // ▲ 여기 이름이 핵심: select**Translation**ForMeeting

        if (linkRow == null || linkRow.isEmpty()) {
            log.warn("Translated doc not linked to meeting: meetingId={}, transDocId={}",
                    meetingId, transDocId);
            throw new SecurityException("Translated document does not belong to this meeting");
        }

        // 2) tl_trans_docs에서 번역 메타 조회
        Map<String, Object> transRow =
                transDocMapper.selectById("tl_trans_docs", "trans_doc_id", transDocId);

        if (transRow == null || transRow.isEmpty()) {
            throw new SecurityException("Translated document not found");
        }

        Long originalDocId = asLong(transRow.get("original_doc_id"));
        if (originalDocId == null) {
            throw new IllegalStateException("original_doc_id is null for trans_doc_id=" + transDocId);
        }

        // 3) tl_documents에서 원본 문서 메타 조회
        Map<String, Object> docRow =
                documentMapper.selectById("tl_documents", "doc_id", originalDocId);

        if (docRow == null || docRow.isEmpty()) {
            throw new SecurityException("Original document not found");
        }

        // ★ 여기서는 tl_documents.meeting_id 는 신뢰하지 않고,
        //    이미 tl_meeting_documents 로 회의 소속 검증을 끝낸 것으로 간주

        // 4) 번역 파일 실제 경로 계산 (tl_trans_docs.translated_rel_path)
        String rel = asString(transRow.get("translated_rel_path"));
        if (rel == null || rel.isBlank()) {
            throw new IllegalStateException("translated_rel_path not found in tl_trans_docs");
        }

        Path root = Paths.get(transDocsRootDir).toAbsolutePath().normalize();
        Path target = root.resolve(rel.replace('\\', '/')).normalize();

        if (!target.startsWith(root)) {
            throw new SecurityException("Invalid translated file path");
        }

        // 5) 다운로드/미리보기용 파일명 구성
        String originalName = asString(docRow.get("original_name"));     // 예: 보고서.docx
        String targetLang   = asString(transRow.get("target_language")); // 예: en, ja, ko
        String baseName;

        if (originalName != null && !originalName.isBlank()) {
            // 확장자 제거
            baseName = originalName.replaceAll("\\.[^.]+$", "");
        } else {
            baseName = "translated";
        }

        String ext = getExtension(rel);
        if (ext == null || ext.isBlank()) {
            // 번역 파일이 pdf로 저장된다는 가정이면 여기서 pdf 고정
            ext = "pdf";
        }

        String downloadName;
        if (targetLang != null && !targetLang.isBlank()) {
            downloadName = baseName + "." + targetLang + "." + ext;  // 예: 보고서.en.pdf
        } else {
            downloadName = baseName + "." + ext;
        }

        // 6) MIME 타입 추론
        String mimeType;
        try {
            mimeType = Files.probeContentType(target);
        } catch (IOException e) {
            log.warn("Failed to probe content type for translated file: {}", target, e);
            mimeType = null;
        }
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = "application/octet-stream";
        }

        return new DownloadFile(target, downloadName, mimeType);
    }

    // ========= 내부 유틸 =========

    private static String asString(Object v) {
        return (v == null) ? null : String.valueOf(v);
    }

    private static Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String getExtension(String filenameOrPath) {
        if (filenameOrPath == null) return null;
        String name = filenameOrPath.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0 && slash < name.length() - 1) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot < name.length() - 1) {
            return name.substring(dot + 1).toLowerCase();
        }
        return null;
    }
}
