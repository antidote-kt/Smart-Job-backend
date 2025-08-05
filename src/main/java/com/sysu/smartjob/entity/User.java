package com.sysu.smartjob.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String username;
    private String email;
    private String password;
    private String nickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer status; // 1-正常 0-禁用
}