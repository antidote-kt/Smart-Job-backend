package com.sysu.smartjob.vo;

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
public class InterviewQuestionVO {
    private Long id;
    
    private String questionText;
    
    private String userAnswer;
    
    private BigDecimal aiScore;
    
    private String aiFeedback;
    
    private LocalDateTime answerTime;
    
    private LocalDateTime createdAt;
}