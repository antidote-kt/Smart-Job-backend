package com.sysu.smartjob.service.impl;

import com.sysu.smartjob.ai.ChatService;
import com.sysu.smartjob.constant.MessageConstant;
import com.sysu.smartjob.constant.InterviewStatusConstant;
import com.sysu.smartjob.constant.PromptConstant;
import com.sysu.smartjob.dto.AnswerSubmitDTO;
import com.sysu.smartjob.dto.InterviewCreateDTO;
import com.sysu.smartjob.vo.AnswerEvaluationVO;
import com.sysu.smartjob.entity.AnswerEvaluation;
import com.sysu.smartjob.entity.Interview;
import com.sysu.smartjob.entity.InterviewQuestion;
import com.sysu.smartjob.entity.InterviewReport;
import com.sysu.smartjob.exception.InterviewException;
import com.sysu.smartjob.mapper.InterviewMapper;
import com.sysu.smartjob.mapper.AnswerEvaluationMapper;
import com.sysu.smartjob.mapper.InterviewQuestionMapper;
import com.sysu.smartjob.mapper.InterviewReportMapper;
import com.sysu.smartjob.service.InterviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private AnswerEvaluationMapper answerEvaluationMapper;

    @Autowired
    private InterviewReportMapper interviewReportMapper;


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
                .status(InterviewStatusConstant.STATUS_IN_PROGRESS)
                .answeredQuestions(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        interviewMapper.insert(interview);
        return interview;
    }

    @Transactional
    @Override
    public Interview startInterview(Long interviewId) {
        Interview query = Interview.builder().id(interviewId).build();
        Interview interview = interviewMapper.findById(query);
        
        interview.setStatus(InterviewStatusConstant.STATUS_IN_PROGRESS);
        interview.setStartTime(LocalDateTime.now());
        interview.setUpdatedAt(LocalDateTime.now());
        
        interviewMapper.update(interview);
        return interview;
    }

    @Override
    public InterviewQuestion getNextQuestion(Long interviewId) {
        Interview query = Interview.builder().id(interviewId).build();
        Interview interview = interviewMapper.findById(query);
        
        if (interview == null || interview.getStatus() != InterviewStatusConstant.STATUS_IN_PROGRESS) {
            throw new InterviewException(MessageConstant.INTERVIEW_STATUS_ERROR);
        }

        if (interview.getAnsweredQuestions() >= interview.getTotalQuestions()) {
            throw new InterviewException(MessageConstant.INTERVIEW_FINISHED);
        }

        String questionPrompt = String.format(
                PromptConstant.PROMPT,
                interview.getPosition(),
                interview.getCompany() != null ? interview.getCompany() : "通用公司",
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

    @Transactional
    @Override
    public AnswerEvaluationVO submitAnswer(AnswerSubmitDTO dto) {
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
        
        double professionalScore = 0.0;
        double logicScore = 0.0;
        double completenessScore = 0.0;
        String feedback = aiResponse;
        
        if (parts.length == 2) {
            String[] scores = parts[0].split(",");
            if (scores.length == 3) {
                try {
                    professionalScore = Double.parseDouble(scores[0].trim());
                    logicScore = Double.parseDouble(scores[1].trim());
                    completenessScore = Double.parseDouble(scores[2].trim());
                    feedback = parts[1].trim();
                } catch (NumberFormatException e) {
                    log.warn("AI评分格式异常: {}", aiResponse);
                }
            }
        }

        question.setUserAnswer(dto.getUserAnswer());
        question.setAiScore((professionalScore + logicScore + completenessScore) / 3.0);
        question.setAiFeedback(feedback);
        question.setAnswerTime(LocalDateTime.now());

        // 创建详细评估记录
        AnswerEvaluation evaluation = AnswerEvaluation.builder()
                .qaRecordId(question.getId())
                .professionalScore(professionalScore)
                .logicScore(logicScore)
                .completenessScore(completenessScore)
                .overallScore((professionalScore + logicScore + completenessScore) / 3.0)
                .aiFeedback(feedback)
                .evaluationTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        answerEvaluationMapper.insert(evaluation);
        
        questionMapper.update(question);

        Interview interviewQuery = Interview.builder().id(question.getInterviewId()).build();
        Interview interview = interviewMapper.findById(interviewQuery);
        interview.setAnsweredQuestions(interview.getAnsweredQuestions() + 1);
        interview.setUpdatedAt(LocalDateTime.now());
        
        interviewMapper.update(interview);
        
        AnswerEvaluationVO vo = new AnswerEvaluationVO();
        BeanUtils.copyProperties(evaluation, vo);
        return vo;
    }
    @Transactional
    @Override
    public Interview finishInterview(Long interviewId) {
        Interview query = Interview.builder().id(interviewId).build();
        Interview interview = interviewMapper.findById(query);
        
        if (interview == null) {
            throw new InterviewException(MessageConstant.INTERVIEW_NOT_EXISTS);
        }

        InterviewQuestion questionQuery = InterviewQuestion.builder().interviewId(interviewId).build();
        List<InterviewQuestion> questions = questionMapper.findByCondition(questionQuery);
        
        double totalScore = questions.stream()
                .filter(q -> q.getAiScore() != null)
                .mapToDouble(InterviewQuestion::getAiScore)
                .sum();
        
        double avgScore = questions.isEmpty() ? 0.0 : 
                totalScore / questions.size();

        interview.setStatus(InterviewStatusConstant.STATUS_COMPLETED);
        interview.setOverallScore(avgScore);
        interview.setEndTime(LocalDateTime.now());
        interview.setUpdatedAt(LocalDateTime.now());
        
        interviewMapper.update(interview);
        
        // 自动生成面试报告
        generateInterviewReport(interviewId);
        
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
    @Transactional
    public void deleteInterview(Long interviewId) {
        // 获取所有相关问题
        InterviewQuestion questionQuery = InterviewQuestion.builder().interviewId(interviewId).build();
        List<InterviewQuestion> questions = questionMapper.findByCondition(questionQuery);
        
        // 删除每个问题的评估记录
        for (InterviewQuestion question : questions) {
            AnswerEvaluation evalQuery = AnswerEvaluation.builder().qaRecordId(question.getId()).build();
            List<AnswerEvaluation> evaluations = answerEvaluationMapper.findByCondition(evalQuery);
            for (AnswerEvaluation evaluation : evaluations) {
                answerEvaluationMapper.deleteById(evaluation.getId());
            }
            questionMapper.deleteById(question.getId());
        }
        
        // 删除该面试的报告
        InterviewReport reportQuery = InterviewReport.builder().sessionId(interviewId).build();
        List<InterviewReport> reports = interviewReportMapper.findByCondition(reportQuery);
        for (InterviewReport report : reports) {
            interviewReportMapper.deleteById(report.getId());
        }
        
        // 删除面试记录
        interviewMapper.deleteById(interviewId);
    }

    @Override
    public void generateInterviewReport(Long interviewId) {
        // 获取该面试的所有问题
        InterviewQuestion questionQuery = InterviewQuestion.builder().interviewId(interviewId).build();
        List<InterviewQuestion> questions = questionMapper.findByCondition(questionQuery);
        
        if (questions.isEmpty()) {
            throw new InterviewException("该面试暂无评估数据");
        }

        // 获取每个问题的详细评估
        List<AnswerEvaluation> allEvaluations = questions.stream()
            .flatMap(q -> {
                AnswerEvaluation evalQuery = AnswerEvaluation.builder().qaRecordId(q.getId()).build();
                return answerEvaluationMapper.findByCondition(evalQuery).stream();
            })
            .toList();

        if (allEvaluations.isEmpty()) {
            throw new InterviewException("该面试暂无详细评估数据");
        }

        // 计算各维度平均分数
        double avgProfessional = allEvaluations.stream()
            .mapToDouble(AnswerEvaluation::getProfessionalScore)
            .average().orElse(0.0);
            
        double avgLogic = allEvaluations.stream()
            .mapToDouble(AnswerEvaluation::getLogicScore)
            .average().orElse(0.0);
            
        double avgCompleteness = allEvaluations.stream()
            .mapToDouble(AnswerEvaluation::getCompletenessScore)
            .average().orElse(0.0);
            
        double avgOverall = allEvaluations.stream()
            .mapToDouble(AnswerEvaluation::getOverallScore)
            .average().orElse(0.0);

        // 生成面试报告提示词
        String reportPrompt = String.format(
            PromptConstant.INTERVIEW_REPORT_PROMPT,
            interviewId,
            avgOverall,
            avgProfessional,
            avgLogic,
            avgCompleteness
        );

        // 调用AI生成报告
        String aiResponse = chatService.chat(interviewId.intValue(), reportPrompt);
        
        // 解析AI响应生成报告
        InterviewReport report = parseReportResponse(aiResponse);
        
        // 设置基本信息
        report.setSessionId(interviewId);
        report.setOverallScore(avgOverall);
        report.setProfessionalScore(avgProfessional);
        report.setLogicScore(avgLogic);
        report.setCompletenessScore(avgCompleteness);
        report.setGeneratedAt(LocalDateTime.now());
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());

        // 保存到数据库
        interviewReportMapper.insert(report);
        
        log.info("面试报告生成完成，面试ID：{}，综合得分：{}", interviewId, avgOverall);
    }

    @Override
    public InterviewReport getInterviewReport(Long interviewId) {
        Interview query = Interview.builder().id(interviewId).build();
        Interview interview = interviewMapper.findById(query);
        
        if (interview == null) {
            throw new InterviewException("面试不存在");
        }
        
        // 直接返回已生成的报告
        InterviewReport reportQuery = InterviewReport.builder().sessionId(interviewId).build();
        List<InterviewReport> reports = interviewReportMapper.findByCondition(reportQuery);
        
        if (reports.isEmpty()) {
            throw new InterviewException("面试报告尚未生成");
        }
        
        return reports.getFirst();
    }

    @Override
    public List<AnswerEvaluation> getInterviewEvaluations(Long interviewId) {
        Interview query = Interview.builder().id(interviewId).build();
        Interview interview = interviewMapper.findById(query);
        
        if (interview == null) {
            throw new InterviewException("面试不存在");
        }
        
        // 从answer_evaluation表中获取详细评估
        AnswerEvaluation evalQuery = AnswerEvaluation.builder().qaRecordId(interviewId).build();
        return answerEvaluationMapper.findByCondition(evalQuery);
    }


    /**
     * 解析AI报告响应
     */
    private InterviewReport parseReportResponse(String aiResponse) {
        InterviewReport report = new InterviewReport();
        
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            setDefaultReport(report);
            return report;
        }
        
        try {
            // 更健壮的解析，处理多种格式
            String content = aiResponse.trim();
            
            report.setPerformanceAnalysis(extractSection(content, PromptConstant.REPORT_LABEL_PERFORMANCE, PromptConstant.REPORT_LABEL_SKILL));
            report.setSkillAssessment(extractSection(content, PromptConstant.REPORT_LABEL_SKILL, PromptConstant.REPORT_LABEL_IMPROVEMENT));
            report.setImprovementSuggestions(extractSection(content, PromptConstant.REPORT_LABEL_IMPROVEMENT, PromptConstant.REPORT_LABEL_STRENGTHS));
            report.setStrongPoints(extractSection(content, PromptConstant.REPORT_LABEL_STRENGTHS, PromptConstant.REPORT_LABEL_WEAKNESSES));
            report.setWeakPoints(extractSection(content, PromptConstant.REPORT_LABEL_WEAKNESSES, null));
            
            // 检查是否有空值，如果有则使用默认值
            if (isEmpty(report.getPerformanceAnalysis()) || 
                isEmpty(report.getSkillAssessment()) || 
                isEmpty(report.getImprovementSuggestions())) {
                setDefaultReport(report);
            }
            
        } catch (Exception e) {
            log.warn("解析AI报告响应失败，使用默认值: {}", e.getMessage());
            setDefaultReport(report);
        }
        
        return report;
    }
    
    private void setDefaultReport(InterviewReport report) {
        report.setPerformanceAnalysis(PromptConstant.DEFAULT_PERFORMANCE_ANALYSIS);
        report.setSkillAssessment(PromptConstant.DEFAULT_SKILL_ASSESSMENT);
        report.setImprovementSuggestions(PromptConstant.DEFAULT_IMPROVEMENT_SUGGESTIONS);
        report.setStrongPoints(PromptConstant.DEFAULT_STRONG_POINTS);
        report.setWeakPoints(PromptConstant.DEFAULT_WEAK_POINTS);
    }
    
    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty() || "暂无内容".equals(str);
    }
    
    private String extractSection(String content, String startLabel, String endLabel) {
        String startMarker = startLabel + ":";
        int startIndex = content.indexOf(startMarker);
        if (startIndex == -1) {
            return "暂无内容";
        }
        
        startIndex += startMarker.length();
        
        int endIndex;
        if (endLabel != null) {
            String endMarker = endLabel + ":";
            endIndex = content.indexOf(endMarker, startIndex);
            if (endIndex == -1) {
                endIndex = content.length();
            }
        } else {
            endIndex = content.length();
        }
        
        String section = content.substring(startIndex, endIndex).trim();
        return section.isEmpty() ? "暂无内容" : section;
    }
}