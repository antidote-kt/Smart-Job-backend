package com.sysu.smartjob.service;

import com.sysu.smartjob.dto.AnswerSubmitDTO;
import com.sysu.smartjob.dto.InterviewCreateDTO;
import com.sysu.smartjob.entity.Interview;
import com.sysu.smartjob.entity.InterviewQuestion;

import java.util.List;

public interface InterviewService {
    
    Interview createInterview(InterviewCreateDTO dto, Long userId);
    
    Interview startInterview(Long interviewId);
    
    InterviewQuestion getNextQuestion(Long interviewId);
    
    void submitAnswer(AnswerSubmitDTO dto);
    
    Interview finishInterview(Long interviewId);
    
    List<Interview> getUserInterviews(Long userId);
    
    Interview getInterviewById(Long interviewId);
    
    List<InterviewQuestion> getInterviewQuestions(Long interviewId);
    
    void deleteInterview(Long interviewId);
}