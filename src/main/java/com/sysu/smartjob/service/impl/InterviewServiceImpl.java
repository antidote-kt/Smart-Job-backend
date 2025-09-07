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
import com.sysu.smartjob.service.NotificationService;
import com.sysu.smartjob.utils.JsonParseUtil;
import com.sysu.smartjob.utils.RedisUtil;
import com.sysu.smartjob.constant.RedisKeyConstant;
import com.sysu.smartjob.context.BaseContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

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

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private MessageProducerService messageProducerService;

    @Autowired
    private NotificationService notificationService;


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
        
        // 清除缓存保持一致性
        clearInterviewCache(interview.getUserId(), interviewId);
        log.debug("面试状态更新，清除缓存，ID: {}", interviewId);
        
        return interview;
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
    
    private void saveQuestion(Long interviewId, String questionText) {
        InterviewQuestion question = InterviewQuestion.builder()
                .interviewId(interviewId)
                .questionText(questionText)
                .build();

        questionMapper.insert(question);
        
        // 清除面试题目缓存，确保前端能获取到最新问题
        Interview interview = interviewMapper.findById(interviewId);
        if (interview != null) {
            clearInterviewCache(interview.getUserId(), interviewId);
            log.debug("新问题已保存，清除缓存，面试ID: {}", interviewId);
        }
    }

    private record QuestionGenerationContext(Interview interview, List<InterviewQuestion> existingQuestions,
                                             String prompt) {
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
        
        log.info("生成的评估提示词长度：{}", evaluatePrompt != null ? evaluatePrompt.length() : 0);
        log.debug("评估提示词内容：{}", evaluatePrompt);
        
        if (evaluatePrompt == null || evaluatePrompt.trim().isEmpty()) {
            log.error("评估提示词为空，问题文本：{}，用户答案：{}", 
                    question.getQuestionText(), dto.getUserAnswer());
            throw new InterviewException("评估提示词生成失败");
        }
        
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
        
        // 清除缓存保持一致性
        clearInterviewCache(interview.getUserId(), interviewId);
        log.debug("面试进度更新，清除缓存，ID: {}", interviewId);
    }

    private record AnswerSubmissionContext(InterviewQuestion question, AnswerSubmitDTO dto, String evaluatePrompt) {
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
        
        log.info("即将更新面试状态，面试ID：{}，状态：{}，结束时间：{}", interviewId, interview.getStatus(), interview.getEndTime());
        interviewMapper.update(interview);
        log.info("面试状态已更新完成，面试ID：{}", interviewId);
        
        // 清除面试缓存（保持数据一致性）
        clearInterviewCache(interview.getUserId(), interviewId);
        log.debug("清除面试缓存，ID: {}", interviewId);
        
        // 异步生成面试报告
        try {
            messageProducerService.sendInterviewReportGenerationMessage(interviewId);
            log.info("已发送面试报告生成消息，面试ID: {}", interviewId);
        } catch (Exception e) {
            log.error("发送面试报告生成消息失败，面试ID: {}", interviewId, e);
            // 消息发送失败不影响面试结束流程
        }
        
        return interview;
    }

    @Override
    public List<Interview> getUserInterviews(Long userId) {
        // 1. 先从缓存获取
        String pattern = RedisKeyConstant.getUserInterviewsPattern(userId);
        Set<String> cacheKeys = redisUtil.keys(pattern);
        
        if (!cacheKeys.isEmpty()) {
            List<Object> cachedObjects = redisUtil.multiGet(cacheKeys);
            List<Interview> cachedInterviews = new ArrayList<>();
            
            for (Object obj : cachedObjects) {
                if (obj instanceof Interview) {
                    cachedInterviews.add((Interview) obj);
                }
            }
            
            if (!cachedInterviews.isEmpty()) {
                log.debug("从缓存获取用户面试列表，用户ID: {}, 数量: {}", userId, cachedInterviews.size());
                return cachedInterviews;
            }
        }
        
        // 2. 缓存未命中，查数据库
        Interview query = Interview.builder().userId(userId).build();
        List<Interview> interviews = interviewMapper.findByCondition(query);
        
        // 3. 缓存结果（每个面试单独缓存）
        if (interviews != null && !interviews.isEmpty()) {
            for (Interview interview : interviews) {
                String cacheKey = RedisKeyConstant.getUserInterviewKey(userId, interview.getId());
                redisUtil.set(cacheKey, interview, 60); // 缓存60分钟
            }
            log.debug("用户面试列表已缓存，用户ID: {}, 数量: {}", userId, interviews.size());
        }
        
        return interviews;
    }

    @Override
    public Interview getInterviewById(Long interviewId) {
        // 获取当前用户ID
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            // 如果无法获取用户ID，直接查数据库
            return interviewMapper.findById(interviewId);
        }
        
        // 1. 先从缓存获取
        String cacheKey = RedisKeyConstant.getUserInterviewKey(userId, interviewId);
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof Interview) {
            log.debug("从缓存获取面试信息，用户ID: {}, 面试ID: {}", userId, interviewId);
            return (Interview) cached;
        }
        
        // 2. 缓存未命中，查数据库
        Interview interview = interviewMapper.findById(interviewId);
        
        // 3. 缓存结果（如果查到数据且属于当前用户）
        if (interview != null && userId.equals(interview.getUserId())) {
            redisUtil.set(cacheKey, interview, 30); // 缓存30分钟
            log.debug("面试信息已缓存，用户ID: {}, 面试ID: {}", userId, interviewId);
        }
        
        return interview;
    }

    @Override
    public List<InterviewQuestion> getInterviewQuestions(Long interviewId) {
        // 获取当前用户ID
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            // 如果无法获取用户ID，直接查数据库
            InterviewQuestion query = InterviewQuestion.builder().interviewId(interviewId).build();
            return questionMapper.findByCondition(query);
        }
        
        // 1. 先从缓存获取
        String cacheKey = RedisKeyConstant.getInterviewQuestionsKey(userId, interviewId);
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<InterviewQuestion> cachedQuestions = (List<InterviewQuestion>) cached;
            log.debug("从缓存获取面试题目列表，用户ID: {}, 面试ID: {}, 数量: {}", userId, interviewId, cachedQuestions.size());
            return cachedQuestions;
        }
        
        // 2. 缓存未命中，查数据库
        InterviewQuestion query = InterviewQuestion.builder().interviewId(interviewId).build();
        List<InterviewQuestion> questions = questionMapper.findByCondition(query);
        
        // 3. 缓存结果
        if (questions != null) {
            redisUtil.set(cacheKey, questions, 30); // 缓存30分钟
            log.debug("面试题目列表已缓存，用户ID: {}, 面试ID: {}, 数量: {}", userId, interviewId, questions.size());
        }
        
        return questions;
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
        
        // 清除相关缓存（保持完整性）
        Long userId = BaseContext.getCurrentId();
        clearInterviewCache(userId, interviewId);
        log.debug("面试删除，清除缓存，ID: {}", interviewId);
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
        
        // 发送报告生成完成通知
        try {
            Interview interview = interviewMapper.findById(interviewId);
            if (interview != null) {
                String notificationMessage = String.format("您的面试报告已生成完成，综合得分：%.1f分", avgOverall);
                notificationService.sendReportGeneratedNotification(interview.getUserId(), interviewId, notificationMessage);
                log.info("报告生成通知已发送，用户ID：{}，面试ID：{}", interview.getUserId(), interviewId);
            }
        } catch (Exception e) {
            // 通知发送失败不应该影响主流程
            log.error("发送报告生成通知失败，面试ID：{}", interviewId, e);
        }
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
        
        // 先获取该面试的所有问题
        InterviewQuestion questionQuery = InterviewQuestion.builder().interviewId(interviewId).build();
        List<InterviewQuestion> questions = questionMapper.findByCondition(questionQuery);
        
        // 然后获取这些问题的所有评估
        List<AnswerEvaluation> allEvaluations = new ArrayList<>();
        for (InterviewQuestion question : questions) {
            if (question.getId() != null) {
                AnswerEvaluation evalQuery = AnswerEvaluation.builder().qaRecordId(question.getId()).build();
                List<AnswerEvaluation> evaluations = answerEvaluationMapper.findByCondition(evalQuery);
                allEvaluations.addAll(evaluations);
            }
        }
        
        return allEvaluations;
    }
    
    @Override
    public Flux<String> getNextQuestionStream(Long interviewId) {
        return Flux.create(sink -> {
            try {
                QuestionGenerationContext context = prepareQuestionGeneration(interviewId);
                StringBuilder fullQuestion = new StringBuilder();

                // 验证提示词不为空
                if (context.prompt == null || context.prompt.trim().isEmpty()) {
                    log.error("问题生成提示词为空，面试ID：{}", interviewId);
                    sink.error(new InterviewException("问题生成提示词为空"));
                    return;
                }
                
                log.info("问题生成提示词长度：{}", context.prompt.length());

                // 使用流式AI生成
                chatService.chatStream(interviewId.intValue(), context.prompt)
                    .doOnNext(chunk -> {
                        log.info("AI生成原始片段: {}", chunk);
                        fullQuestion.append(chunk);
                        sink.next(chunk);
                    })
                    .doOnComplete(() -> {
                        log.info("问题生成完成，面试ID：{}", interviewId);
                        
                        // 异步保存完整问题到数据库
                        try {
                            saveQuestion(interviewId, fullQuestion.toString());
                            
                            // 清除面试缓存（因为状态可能变化）
                            Long userId = BaseContext.getCurrentId();
                            clearInterviewCache(userId, interviewId);
                            
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
    
    /**
     * 清除面试相关的所有缓存
     * @param userId 用户ID
     * @param interviewId 面试ID
     */
    private void clearInterviewCache(Long userId, Long interviewId) {
        if (userId == null) {
            return;
        }
        
        // 删除面试详情缓存
        String interviewKey = RedisKeyConstant.getUserInterviewKey(userId, interviewId);
        redisUtil.delete(interviewKey);
        
        // 删除面试题目缓存
        String questionsKey = RedisKeyConstant.getInterviewQuestionsKey(userId, interviewId);
        redisUtil.delete(questionsKey);
    }
}