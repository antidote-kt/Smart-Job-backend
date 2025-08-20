package com.sysu.smartjob.service;

import com.sysu.smartjob.dto.AnswerSubmitDTO;
import com.sysu.smartjob.dto.InterviewCreateDTO;
import com.sysu.smartjob.entity.AnswerEvaluation;
import com.sysu.smartjob.entity.Interview;
import com.sysu.smartjob.entity.InterviewQuestion;
import com.sysu.smartjob.entity.InterviewReport;
import com.sysu.smartjob.vo.AnswerEvaluationVO;
import reactor.core.publisher.Flux;

import java.util.List;

public interface InterviewService {
    
    Interview createInterview(InterviewCreateDTO dto, Long userId);
    
    Interview startInterview(Long interviewId);
    
    InterviewQuestion getNextQuestion(Long interviewId);
    
    AnswerEvaluationVO submitAnswer(AnswerSubmitDTO dto);
    
    Interview finishInterview(Long interviewId);
    
    List<Interview> getUserInterviews(Long userId);
    
    Interview getInterviewById(Long interviewId);
    
    List<InterviewQuestion> getInterviewQuestions(Long interviewId);
    
    void deleteInterview(Long interviewId);
    
    InterviewReport getInterviewReport(Long interviewId);
    
    List<AnswerEvaluation> getInterviewEvaluations(Long interviewId);
    
    void generateInterviewReport(Long interviewId);
    
    Flux<String> getNextQuestionStream(Long interviewId);


}