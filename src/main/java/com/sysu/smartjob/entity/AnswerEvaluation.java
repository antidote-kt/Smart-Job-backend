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
public class AnswerEvaluation {
    private Long id;
    
    private Long qaRecordId;
    
    private Double professionalScore;
    
    private Double logicScore;
    
    private Double completenessScore;
    
    private Double overallScore;
    
    private String strengths;
    
    private String weaknesses;
    
    private String suggestions;
    
    private String aiFeedback;
    
    private LocalDateTime evaluationTime;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}