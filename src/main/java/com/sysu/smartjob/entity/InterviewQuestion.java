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
public class InterviewQuestion {
    private Long id;
    
    private Long interviewId;
    
    private String questionText;
    
    private String userAnswer;
    
    private LocalDateTime answerTime;
}