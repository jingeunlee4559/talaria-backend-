package com.example.talaria.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
@Profile("!test") // 테스트 프로필에서는 실행 안 함(선택)
@RequiredArgsConstructor
public class StorageWarmup implements ApplicationRunner {

    @Value("${storage.documents.root}")
    private String docRoot;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path root = Paths.get(docRoot).normalize().toAbsolutePath();
        Files.createDirectories(root); // 없으면 자동 생성
        boolean writable = Files.isWritable(root);
        log.info("DOC_ROOT={} (exists={}, writable={})", root, Files.exists(root), writable);
        if (!writable) {
            throw new IllegalStateException("documents root not writable: " + root);
        }
    }
}
