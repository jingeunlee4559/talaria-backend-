package com.example.talaria.backend.model;

import java.time.LocalDateTime;
import lombok.Data;


@Data
public class User {

    // --- 수정된 부분 ---
    private Long userId;
    private String email;            
    private String passwordHash;    
    private String name;
    private String locale;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String profileImageUrl; 

}