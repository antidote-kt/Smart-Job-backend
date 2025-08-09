package com.sysu.smartjob.vo;

import lombok.Data;

@Data
public class InterviewReportVO {
    private Long id;
    private Long interviewId;
    private Double overallScore;
    private String overallFeedback;
    private String strengths;
    private String weaknesses;
    private String suggestions;
}