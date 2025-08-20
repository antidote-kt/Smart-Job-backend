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
import com.sysu.smartjob.utils.JsonParseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

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
                .status(InterviewStatusConstant.STATUS_CREATED) // 创建时状态为CREATED
                .answeredQuestions(0)
                .updatedAt(LocalDateTime.now())
                .build();

        interviewMapper.insert(interview);
        return interview;
    }

    @Transactional
    @Override
    public Interview startInterview(Long interviewId) {
        Interview interview = interviewMapper.findById(interviewId);

        interview.setStatus(InterviewStatusConstant.STATUS_IN_PROGRESS);
        interview.setStartTime(LocalDateTime.now());
        interview.setUpdatedAt(LocalDateTime.now());

        interviewMapper.update(interview);
        return interview;
    }

    @Override
    public InterviewQuestion getNextQuestion(Long interviewId) {
        QuestionGenerationContext context = prepareQuestionGeneration(interviewId);
        String questionText = chatService.chat(interviewId.intValue(), context.prompt);
        return saveQuestion(interviewId, questionText);
    }
    
    private QuestionGenerationContext prepareQuestionGeneration(Long interviewId) {
        Interview interview = interviewMapper.findById(interviewId);

        if (interview == null || interview.getStatus() != InterviewStatusConstant.STATUS_IN_PROGRESS) {
            throw new InterviewException(MessageConstant.INTERVIEW_STATUS_ERROR);
        }

        if (interview.getAnsweredQuestions() >= interview.getTotalQuestions()) {
            throw new InterviewException(MessageConstant.INTERVIEW_FINISHED);
        }

        // 获取已有问题，避免重复
        InterviewQuestion existingQuery = InterviewQuestion.builder().interviewId(interviewId).build();
        List<InterviewQuestion> existingQuestions = questionMapper.findByCondition(existingQuery);

        String questionPrompt = getPrompt(existingQuestions, interview);
        
        return new QuestionGenerationContext(interview, existingQuestions, questionPrompt);
    }
    
    private InterviewQuestion saveQuestion(Long interviewId, String questionText) {
        InterviewQuestion question = InterviewQuestion.builder()
                .interviewId(interviewId)
                .questionText(questionText)
                .build();

        questionMapper.insert(question);
        return question;
    }
    
    private static class QuestionGenerationContext {
        final Interview interview;
        final List<InterviewQuestion> existingQuestions;
        final String prompt;
        
        QuestionGenerationContext(Interview interview, List<InterviewQuestion> existingQuestions, String prompt) {
            this.interview = interview;
            this.existingQuestions = existingQuestions;
            this.prompt = prompt;
        }
    }

    private static String getPrompt(List<InterviewQuestion> existingQuestions, Interview interview) {
        StringBuilder previousQuestions = new StringBuilder();
        if (!existingQuestions.isEmpty()) {
            previousQuestions.append("已提问的题目：\n");
            for (int i = 0; i < existingQuestions.size(); i++) {
                previousQuestions.append(String.format("%d. %s\n", i + 1, existingQuestions.get(i).getQuestionText()));
            }
            previousQuestions.append("\n请生成一个不同的新问题，避免重复上述题目内容。\n");
        }

        // 使用实际已存在问题数量计算下一题序号
        int nextQuestionNumber = existingQuestions.size() + 1;
        
        String questionPrompt = String.format(
                PromptConstant.IMPROVED_PROMPT,
                interview.getCompany() != null ? interview.getCompany() : "通用公司", // %s公司
                interview.getPosition(), // %s岗位
                nextQuestionNumber, // 第%d道 - 使用实际问题数量
                PromptConstant.getDifficultyText(interview.getDifficultyLevel()), // 难度等级：%s
                PromptConstant.getInterviewTypeText(interview.getInterviewType()), // 面试类型：%s
                previousQuestions, // 已提问题目：%s
                interview.getPosition(), // 搜索"%s岗位面试题"
                interview.getCompany() != null ? interview.getCompany() : "通用公司", // "%s公司面试经验"
                PromptConstant.getDifficultyText(interview.getDifficultyLevel()) // 难度等级：%s
        );
        return questionPrompt;
    }

    @Transactional
    @Override
    public AnswerEvaluationVO submitAnswer(AnswerSubmitDTO dto) {
        log.info("开始提交答案，问题ID：{}", dto.getQuestionId());
        
        AnswerSubmissionContext context = prepareAnswerSubmission(dto);
        String aiResponse = chatService.chat(context.question.getInterviewId().intValue(), context.evaluatePrompt);
        
        AnswerEvaluation evaluation = processEvaluationResponse(context, aiResponse);
        updateInterviewProgress(context.question.getInterviewId());
        
        AnswerEvaluationVO vo = new AnswerEvaluationVO();
        BeanUtils.copyProperties(evaluation, vo);
        
        log.info("答案提交完成，总分：{}", vo.getOverallScore());
        return vo;
    }
    
    private AnswerSubmissionContext prepareAnswerSubmission(AnswerSubmitDTO dto) {
        log.info("准备答案提交，问题ID：{}，答案长度：{}", dto.getQuestionId(), 
                dto.getUserAnswer() != null ? dto.getUserAnswer().length() : 0);
                
        InterviewQuestion query = InterviewQuestion.builder().id(dto.getQuestionId()).build();
        InterviewQuestion question = questionMapper.findById(query);
        
        if (question == null) {
            log.error("问题不存在，问题ID：{}", dto.getQuestionId());
            throw new InterviewException(MessageConstant.QUESTION_NOT_EXISTS);
        }
        
        log.info("找到问题，面试ID：{}", question.getInterviewId());

        String evaluatePrompt = String.format(
                PromptConstant.EVALUATE_PROMPT,
                question.getQuestionText(),
                dto.getUserAnswer()
        );
        
        return new AnswerSubmissionContext(question, dto, evaluatePrompt);
    }
    
    private AnswerEvaluation processEvaluationResponse(AnswerSubmissionContext context, String aiResponse) {
        log.info("处理AI评估响应，长度：{}", aiResponse != null ? aiResponse.length() : 0);
        
        // 使用JSON解析AI响应
        JsonParseUtil.EvaluationResult evaluationResult = JsonParseUtil.parseEvaluation(aiResponse);
        
        double professionalScore = evaluationResult.professionalScore;
        double logicScore = evaluationResult.logicScore;
        double completenessScore = evaluationResult.completenessScore;
        String feedback = evaluationResult.feedback;

        // 更新问题记录
        context.question.setUserAnswer(context.dto.getUserAnswer());
        context.question.setAnswerTime(LocalDateTime.now());
        questionMapper.update(context.question);

        // 创建详细评估记录
        AnswerEvaluation evaluation = AnswerEvaluation.builder()
                .qaRecordId(context.question.getId())
                .professionalScore(professionalScore)
                .logicScore(logicScore)
                .completenessScore(completenessScore)
                .overallScore(Math.round((professionalScore + logicScore + completenessScore) / 3.0 * 10.0) / 10.0)
                .aiFeedback(feedback)
                .evaluationTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        answerEvaluationMapper.insert(evaluation);
        return evaluation;
    }
    
    private void updateInterviewProgress(Long interviewId) {
        Interview interview = interviewMapper.findById(interviewId);
        interview.setAnsweredQuestions(interview.getAnsweredQuestions() + 1);
        interview.setUpdatedAt(LocalDateTime.now());
        interviewMapper.update(interview);
    }
    
    private static class AnswerSubmissionContext {
        final InterviewQuestion question;
        final AnswerSubmitDTO dto;
        final String evaluatePrompt;
        
        AnswerSubmissionContext(InterviewQuestion question, AnswerSubmitDTO dto, String evaluatePrompt) {
            this.question = question;
            this.dto = dto;
            this.evaluatePrompt = evaluatePrompt;
        }
    }
    @Transactional
    @Override
    public Interview finishInterview(Long interviewId) {
        Interview interview = interviewMapper.findById(interviewId);
        
        if (interview == null) {
            throw new InterviewException(MessageConstant.INTERVIEW_NOT_EXISTS);
        }
        
        // 检查面试状态，只有进行中的面试才能结束
        if (interview.getStatus() != InterviewStatusConstant.STATUS_IN_PROGRESS) {
            log.warn("面试状态不正确，无法结束。当前状态：{}，面试ID：{}", interview.getStatus(), interviewId);
            if (interview.getStatus() == InterviewStatusConstant.STATUS_COMPLETED) {
                // 如果面试已经完成，直接返回
                return interview;
            }
            throw new InterviewException("面试状态不正确，无法结束");
        }

        InterviewQuestion questionQuery = InterviewQuestion.builder().interviewId(interviewId).build();
        List<InterviewQuestion> questions = questionMapper.findByCondition(questionQuery);
        
        // 由于移除了aiScore字段，这里使用答案评估表中的分数
        double totalScore = 0.0;
        int scoredQuestions = 0;
        
        for (InterviewQuestion question : questions) {
            if (question.getUserAnswer() != null && !question.getUserAnswer().trim().isEmpty()) {
                // 从答案评估表中获取分数
                AnswerEvaluation query = AnswerEvaluation.builder().qaRecordId(question.getId()).build();
                List<AnswerEvaluation> evaluations = answerEvaluationMapper.findByCondition(query);
                if (!evaluations.isEmpty()) {
                    AnswerEvaluation evaluation = evaluations.getFirst();
                    if (evaluation.getOverallScore() != null) {
                        totalScore += evaluation.getOverallScore();
                        scoredQuestions++;
                    }
                }
            }
        }
        
        double avgScore = scoredQuestions == 0 ? 0.0 : 
                Math.round((totalScore / scoredQuestions) * 10.0) / 10.0;

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
        return interviewMapper.findById(interviewId);
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
        log.info("开始生成面试报告，面试ID：{}", interviewId);
        
        // 获取该面试的所有问题
        InterviewQuestion questionQuery = InterviewQuestion.builder().interviewId(interviewId).build();
        List<InterviewQuestion> questions = questionMapper.findByCondition(questionQuery);
        
        log.info("找到面试问题数量：{}", questions.size());
        
        if (questions.isEmpty()) {
            log.warn("该面试暂无问题数据，面试ID：{}", interviewId);
            throw new InterviewException("该面试暂无评估数据");
        }

        // 获取每个问题的详细评估
        List<AnswerEvaluation> allEvaluations = questions.stream()
            .flatMap(q -> {
                AnswerEvaluation evalQuery = AnswerEvaluation.builder().qaRecordId(q.getId()).build();
                List<AnswerEvaluation> evils = answerEvaluationMapper.findByCondition(evalQuery);
                log.debug("问题ID：{}，评估记录数：{}", q.getId(), evils.size());
                return evils.stream();
            })
            .toList();

        log.info("找到评估记录数量：{}", allEvaluations.size());

        if (allEvaluations.isEmpty()) {
            log.error("该面试暂无详细评估数据，面试ID：{}，但有{}个问题", interviewId, questions.size());
            throw new InterviewException("该面试缺少评估数据，无法生成报告");
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
        
        // 使用JSON解析AI响应
        JsonParseUtil.ReportResult reportResult = JsonParseUtil.parseReport(aiResponse);
        
        // 生成报告实体
        InterviewReport report = new InterviewReport();
        report.setPerformanceAnalysis(reportResult.performanceAnalysis);
        report.setSkillAssessment(reportResult.skillAssessment);
        report.setImprovementSuggestions(reportResult.improvementSuggestions);
        report.setStrongPoints(reportResult.strongPoints);
        report.setWeakPoints(reportResult.weakPoints);
        
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
        Interview interview = interviewMapper.findById(interviewId);
        
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
        Interview interview = interviewMapper.findById(interviewId);
        
        if (interview == null) {
            throw new InterviewException("面试不存在");
        }
        
        // 从answer_evaluation表中获取详细评估
        AnswerEvaluation evalQuery = AnswerEvaluation.builder().qaRecordId(interviewId).build();
        return answerEvaluationMapper.findByCondition(evalQuery);
    }
    
    @Override
    public Flux<String> getNextQuestionStream(Long interviewId) {
        return Flux.create(sink -> {
            try {
                QuestionGenerationContext context = prepareQuestionGeneration(interviewId);
                StringBuilder fullQuestion = new StringBuilder();

                // 使用流式AI生成
                chatService.chatStream(interviewId.intValue(), context.prompt)
                    .doOnNext(chunk -> {
                        log.info("AI生成原始片段: {}", chunk);
                        log.info("片段字节数: {}", chunk.getBytes().length);
                        log.info("片段UTF-8字节: {}", java.util.Arrays.toString(chunk.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                        fullQuestion.append(chunk);
                        sink.next(chunk);
                    })
                    .doOnComplete(() -> {
                        log.info("问题生成完成，面试ID：{}", interviewId);
                        
                        // 异步保存完整问题到数据库
                        try {
                            saveQuestion(interviewId, fullQuestion.toString());
                            // 发送完成标志
                            sink.next("[DONE]");
                        } catch (Exception e) {
                            log.error("保存问题失败", e);
                        }
                        
                        sink.complete();
                    })
                    .doOnError(error -> {
                        log.error("生成问题失败", error);
                        sink.error(error);
                    })
                    .subscribe();
                    
            } catch (Exception e) {
                log.error("创建问题流失败", e);
                sink.error(e);
            }
        });
    }
}