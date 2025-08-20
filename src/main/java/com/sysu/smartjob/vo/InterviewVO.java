package com.sysu.smartjob.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import com.sysu.smartjob.entity.InterviewReport;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewVO {
    private Long id;
    
    private String title;
    
    private String position;
    
    private String company;
    
    private Integer interviewType;
    
    private Integer difficultyLevel;
    
    private Integer status;
    
    private Integer totalQuestions;
    
    private Integer answeredQuestions;
    
    private Double overallScore;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private InterviewReport report;
}