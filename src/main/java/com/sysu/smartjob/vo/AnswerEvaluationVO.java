package com.sysu.smartjob.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnswerEvaluationVO {
    private Long id;
    private Long qaRecordId;
    private Double professionalScore;
    private Double logicScore;
    private Double completenessScore;
    private Double overallScore;
    private String aiFeedback;
    private LocalDateTime evaluationTime;
}