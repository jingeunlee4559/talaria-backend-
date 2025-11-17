// src/main/java/com/example/talaria/backend/service/asset/FrontAssetStorage.java
package com.example.talaria.backend.service.asset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Objects;
import java.util.Set;

@Service
public class FrontAssetStorage {

    private final Path publicRoot;   // .../talaria-frontend/public
    private final Path baseDir;      // .../talaria-frontend/public/img
    private final String webBase;    // "/img" (항상 선행 슬래시)
    private static final Set<String> ALLOWED = Set.of(".png",".jpg",".jpeg",".webp",".gif");

    public FrontAssetStorage(
            @Value("${talaria.files.frontPublicRoot}") String frontPublicRoot,
            @Value("${talaria.files.cultureThumbBaseDir:/img}") String cultureThumbBaseDir
    ) {
        this.publicRoot = Paths.get(frontPublicRoot).toAbsolutePath().normalize();

        // cultureThumbBaseDir 정규화
        String normBase = cultureThumbBaseDir == null ? "/img" : cultureThumbBaseDir.replace('\\','/');
        if (normBase.isBlank()) normBase = "/img";
        if (!normBase.startsWith("/")) normBase = "/" + normBase;

        this.webBase = normBase;                          // 예: "/img"
        this.baseDir = publicRoot.resolve(normBase.substring(1)).normalize(); // 예: public/img
    }

    /** 파일 저장 → 웹 경로 반환(예: "/img/1731812345678_filename.png") */
    public String saveCultureThumb(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        Files.createDirectories(baseDir);

        String original = StringUtils.cleanPath(
                Objects.requireNonNullElse(file.getOriginalFilename(), "upload")
        );

        String ext = extOf(original).toLowerCase();
        if (!ALLOWED.contains(ext)) ext = ".png";

        String stem = stripExt(original)
                .replaceAll("[\\s]+", "_")
                .replaceAll("[^0-9A-Za-z._-]", "");
        if (stem.isBlank()) stem = "thumb";

        String filename = System.currentTimeMillis() + "_" + stem + ext;
        Path dest = baseDir.resolve(filename).normalize();

        // public 밖으로 이탈 방지
        if (!dest.toAbsolutePath().normalize().startsWith(publicRoot)) {
            throw new IOException("Invalid path");
        }

        file.transferTo(dest.toFile());

        // ▶ 항상 "/img/파일명" 형태로 반환
        return webBase + "/" + filename;
    }

    private static String extOf(String name) {
        int i = name.lastIndexOf('.');
        return (i >= 0 ? name.substring(i) : "");
    }

    private static String stripExt(String name) {
        int i = name.lastIndexOf('.');
        return (i >= 0 ? name.substring(0, i) : name);
    }
}
