package com.sysu.smartjob.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewReport {
    private Long id;
    
    private Long sessionId;
    
    private Double overallScore;
    
    private Double professionalScore;
    
    private Double logicScore;
    
    private Double completenessScore;
    
    private String performanceAnalysis;
    
    private String skillAssessment;
    
    private String improvementSuggestions;
    
    private String strongPoints;
    
    private String weakPoints;
    
    private LocalDateTime generatedAt;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}