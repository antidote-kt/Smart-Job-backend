package com.sysu.smartjob.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Interview {
    private Long id;
    
    private Long userId;
    
    private String title;
    
    private String position;
    
    private String company;
    
    private Integer interviewType;
    
    private Integer difficultyLevel;
    
    private Integer status;
    
    private Integer totalQuestions;
    
    private Integer answeredQuestions;
    
    private BigDecimal overallScore;
    
    private String aiFeedback;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}