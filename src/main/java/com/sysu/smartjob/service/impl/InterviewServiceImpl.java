package com.sysu.smartjob.service.impl;

import com.sysu.smartjob.ai.ChatService;
import com.sysu.smartjob.constant.MessageConstant;
import com.sysu.smartjob.constant.PromptConstant;
import com.sysu.smartjob.dto.AnswerSubmitDTO;
import com.sysu.smartjob.dto.InterviewCreateDTO;
import com.sysu.smartjob.entity.Interview;
import com.sysu.smartjob.entity.InterviewQuestion;
import com.sysu.smartjob.exception.InterviewException;
import com.sysu.smartjob.mapper.InterviewMapper;
import com.sysu.smartjob.mapper.InterviewQuestionMapper;
import com.sysu.smartjob.service.InterviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@Transactional
public class InterviewServiceImpl implements InterviewService {

    @Autowired
    private InterviewMapper interviewMapper;

    @Autowired
    private InterviewQuestionMapper questionMapper;

    @Autowired
    private ChatService chatService;

    @Override
    public Interview createInterview(InterviewCreateDTO dto, Long userId) {
        Interview interview = Interview.builder()
                .userId(userId)
                .title(dto.getTitle())
                .position(dto.getPosition())
                .company(dto.getCompany())
                .interviewType(dto.getInterviewType())
                .difficultyLevel(dto.getDifficultyLevel())
                .totalQuestions(dto.getTotalQuestions())
                .status(1)
                .answeredQuestions(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        interviewMapper.insert(interview);
        return interview;
    }

    @Override
    public Interview startInterview(Long interviewId) {
        Interview query = Interview.builder().id(interviewId).build();
        Interview interview = interviewMapper.findById(query);
        
        interview.setStatus(1);
        interview.setStartTime(LocalDateTime.now());
        interview.setUpdatedAt(LocalDateTime.now());
        
        interviewMapper.update(interview);
        return interview;
    }

    @Override
    public InterviewQuestion getNextQuestion(Long interviewId) {
        Interview query = Interview.builder().id(interviewId).build();
        Interview interview = interviewMapper.findById(query);
        
        if (interview == null || interview.getStatus() != 1) {
            throw new InterviewException(MessageConstant.INTERVIEW_STATUS_ERROR);
        }

        if (interview.getAnsweredQuestions() >= interview.getTotalQuestions()) {
            throw new InterviewException(MessageConstant.INTERVIEW_FINISHED);
        }

        String questionPrompt = String.format(
                PromptConstant.PROMPT,
                interview.getPosition(),
                PromptConstant.getDifficultyText(interview.getDifficultyLevel()),
                PromptConstant.getInterviewTypeText(interview.getInterviewType())
        );

        String questionText = chatService.chat(interviewId.intValue(), questionPrompt);

        InterviewQuestion question = InterviewQuestion.builder()
                .interviewId(interviewId)
                .questionText(questionText)
                .createdAt(LocalDateTime.now())
                .build();

        questionMapper.insert(question);
        return question;
    }

    @Override
    public void submitAnswer(AnswerSubmitDTO dto) {
        InterviewQuestion query = InterviewQuestion.builder().id(dto.getQuestionId()).build();
        InterviewQuestion question = questionMapper.findById(query);
        
        if (question == null) {
            throw new InterviewException(MessageConstant.QUESTION_NOT_EXISTS);
        }

        String evaluatePrompt = String.format(
                PromptConstant.EVALUATE_PROMPT,
                question.getQuestionText(),
                dto.getUserAnswer()
        );

        String aiResponse = chatService.chat(question.getInterviewId().intValue(), evaluatePrompt);
        String[] parts = aiResponse.split("\\|", 2);
        
        BigDecimal score = new BigDecimal("0");
        String feedback = aiResponse;
        
        if (parts.length == 2) {
            try {
                score = new BigDecimal(parts[0].trim());
                feedback = parts[1].trim();
            } catch (NumberFormatException e) {
                log.warn("AI评分格式异常: {}", aiResponse);
            }
        }

        question.setUserAnswer(dto.getUserAnswer());
        question.setAiScore(score);
        question.setAiFeedback(feedback);
        question.setAnswerTime(LocalDateTime.now());
        
        questionMapper.update(question);

        Interview interviewQuery = Interview.builder().id(question.getInterviewId()).build();
        Interview interview = interviewMapper.findById(interviewQuery);
        interview.setAnsweredQuestions(interview.getAnsweredQuestions() + 1);
        interview.setUpdatedAt(LocalDateTime.now());
        
        interviewMapper.update(interview);
    }

    @Override
    public Interview finishInterview(Long interviewId) {
        Interview query = Interview.builder().id(interviewId).build();
        Interview interview = interviewMapper.findById(query);
        
        if (interview == null) {
            throw new InterviewException(MessageConstant.INTERVIEW_NOT_EXISTS);
        }

        InterviewQuestion questionQuery = InterviewQuestion.builder().interviewId(interviewId).build();
        List<InterviewQuestion> questions = questionMapper.findByCondition(questionQuery);
        
        BigDecimal totalScore = questions.stream()
                .filter(q -> q.getAiScore() != null)
                .map(InterviewQuestion::getAiScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal avgScore = questions.isEmpty() ? BigDecimal.ZERO : 
                totalScore.divide(new BigDecimal(questions.size()), 2, BigDecimal.ROUND_HALF_UP);

        String overallFeedback = String.format(
                PromptConstant.OVERALL_FEEDBACK_PROMPT,
                questions.size(), avgScore
        );
        
        String aiFeedback = chatService.chat(interviewId.intValue(), overallFeedback);

        interview.setStatus(2);
        interview.setOverallScore(avgScore);
        interview.setAiFeedback(aiFeedback);
        interview.setEndTime(LocalDateTime.now());
        interview.setUpdatedAt(LocalDateTime.now());
        
        interviewMapper.update(interview);
        return interview;
    }

    @Override
    public List<Interview> getUserInterviews(Long userId) {
        Interview query = Interview.builder().userId(userId).build();
        return interviewMapper.findByCondition(query);
    }

    @Override
    public Interview getInterviewById(Long interviewId) {
        Interview query = Interview.builder().id(interviewId).build();
        return interviewMapper.findById(query);
    }

    @Override
    public List<InterviewQuestion> getInterviewQuestions(Long interviewId) {
        InterviewQuestion query = InterviewQuestion.builder().interviewId(interviewId).build();
        return questionMapper.findByCondition(query);
    }

    @Override
    public void deleteInterview(Long interviewId) {
        Interview query = Interview.builder().id(interviewId).build();
        interviewMapper.deleteById(query);
    }
}