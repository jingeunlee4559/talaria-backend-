package com.example.talaria.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.example.talaria.backend.mapper")
public class TalariaBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(TalariaBackendApplication.class, args);
    }
}
