package com.sysu.smartjob.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewCreateDTO {
    private String title;
    
    private String position;
    
    private String company;
    
    private Integer interviewType;
    
    private Integer difficultyLevel;
    
    private Integer totalQuestions;
}