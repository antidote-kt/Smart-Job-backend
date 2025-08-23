package com.sysu.smartjob.controller;

import com.sysu.smartjob.constant.InterviewStatusConstant;
import com.sysu.smartjob.context.BaseContext;
import com.sysu.smartjob.dto.AnswerSubmitDTO;
import com.sysu.smartjob.dto.InterviewCreateDTO;
import com.sysu.smartjob.entity.AnswerEvaluation;
import com.sysu.smartjob.entity.Interview;
import com.sysu.smartjob.entity.InterviewQuestion;
import com.sysu.smartjob.entity.InterviewReport;
import com.sysu.smartjob.result.Result;
import com.sysu.smartjob.service.InterviewService;
import com.sysu.smartjob.vo.AnswerEvaluationVO;
import com.sysu.smartjob.vo.InterviewQuestionVO;
import com.sysu.smartjob.vo.InterviewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/interview")
public class InterviewController {

    @Autowired
    private InterviewService interviewService;

    @PostMapping("/create")
    public Result<InterviewVO> createInterview(@RequestBody InterviewCreateDTO dto) {
        log.info("创建面试，参数：{}", dto);
        Long userId = BaseContext.getCurrentId();
        Interview interview = interviewService.createInterview(dto, userId);
        
        InterviewVO vo = new InterviewVO();
        BeanUtils.copyProperties(interview, vo);
        
        return Result.success(vo, "面试创建成功");
    }

    @PostMapping("/{id}/start")
    public Result<InterviewVO> startInterview(@PathVariable Long id) {
        log.info("开始面试，面试ID：{}", id);
        Interview interview = interviewService.startInterview(id);
        
        InterviewVO vo = new InterviewVO();
        BeanUtils.copyProperties(interview, vo);
        
        return Result.success(vo, "面试开始");
    }



    @PostMapping("/submit-answer")
    public Result<AnswerEvaluationVO> submitAnswer(@RequestBody AnswerSubmitDTO dto) {
        log.info("提交答案，参数：{}", dto);
        AnswerEvaluationVO evaluation = interviewService.submitAnswer(dto);
        return Result.success(evaluation, "答案提交成功");
    }

    @PostMapping("/{id}/finish")
    public Result<InterviewVO> finishInterview(@PathVariable Long id) {
        log.info("结束面试，面试ID：{}", id);
        
        // 获取当前用户ID
        Long currentUserId = BaseContext.getCurrentId();
        log.info("当前用户ID：{}", currentUserId);
        
        // 验证面试是否属于当前用户
        Interview currentInterview = interviewService.getInterviewById(id);
        if (currentInterview == null) {
            return Result.error("面试不存在");
        }
        
        if (!currentInterview.getUserId().equals(currentUserId)) {
            return Result.error("无权限操作此面试");
        }
        
        Interview interview = interviewService.finishInterview(id);
        
        InterviewVO vo = new InterviewVO();
        BeanUtils.copyProperties(interview, vo);
        
        return Result.success(vo, "面试结束");
    }

    @GetMapping("/list")
    public Result<List<InterviewVO>> getUserInterviews() {
        Long userId = BaseContext.getCurrentId();
        log.info("获取用户面试列表，用户ID：{}", userId);
        List<Interview> interviews = interviewService.getUserInterviews(userId);
        
        List<InterviewVO> vos = interviews.stream()
                .map(interview -> {
                    InterviewVO vo = new InterviewVO();
                    BeanUtils.copyProperties(interview, vo);
                    return vo;
                })
                .collect(Collectors.toList());
        
        return Result.success(vos, "获取面试列表成功");
    }

    @GetMapping("/{id}")
    public Result<InterviewVO> getInterviewDetail(@PathVariable Long id) {
        log.info("获取面试详情，面试ID：{}", id);
        Interview interview = interviewService.getInterviewById(id);
        
        if (interview == null) {
            return Result.error("面试不存在");
        }
        
        log.info("面试详情，ID：{}，状态：{}，开始时间：{}，结束时间：{}", 
                 interview.getId(), interview.getStatus(), interview.getStartTime(), interview.getEndTime());
        
        InterviewVO vo = new InterviewVO();
        BeanUtils.copyProperties(interview, vo);
        
        log.info("转换后的VO，状态：{}，开始时间：{}，结束时间：{}", 
                 vo.getStatus(), vo.getStartTime(), vo.getEndTime());
        
        // 如果是已完成的面试，添加报告信息
        if (interview.getStatus() != null && interview.getStatus() == InterviewStatusConstant.STATUS_COMPLETED) {
            try {
                InterviewReport report = interviewService.getInterviewReport(id);
                vo.setReport(report);
            } catch (Exception e) {
                log.warn("获取面试报告失败，面试ID：{}，错误：{}", id, e.getMessage());
            }
        }
        
        return Result.success(vo, "获取面试详情成功");
    }

    @GetMapping("/{id}/questions")
    public Result<List<InterviewQuestionVO>> getInterviewQuestions(@PathVariable Long id) {
        log.info("获取面试题目，面试ID：{}", id);
        List<InterviewQuestion> questions = interviewService.getInterviewQuestions(id);
        
        List<InterviewQuestionVO> vos = questions.stream()
                .map(question -> {
                    InterviewQuestionVO vo = new InterviewQuestionVO();
                    BeanUtils.copyProperties(question, vo);
                    return vo;
                })
                .collect(Collectors.toList());
        
        return Result.success(vos, "获取面试题目成功");
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteInterview(@PathVariable Long id) {
        log.info("删除面试，面试ID：{}", id);
        interviewService.deleteInterview(id);
        return Result.success("删除成功");
    }

    @GetMapping("/{id}/report")
    public Result<InterviewReport> getInterviewReport(@PathVariable Long id) {
        log.info("获取面试报告，面试ID：{}", id);
        InterviewReport report = interviewService.getInterviewReport(id);
        return Result.success(report, "获取面试报告成功");
    }

    @GetMapping("/{id}/evaluations")
    public Result<List<AnswerEvaluation>> getInterviewEvaluations(@PathVariable Long id) {
        log.info("获取面试评估详情，面试ID：{}", id);
        List<AnswerEvaluation> evaluations = interviewService.getInterviewEvaluations(id);
        return Result.success(evaluations, "获取评估详情成功");
    }
    
    @GetMapping(value = "/{id}/next-question-stream", produces = "text/event-stream;charset=UTF-8")
    public ResponseEntity<Flux<ServerSentEvent<String>>> getNextQuestionStream(@PathVariable Long id, HttpServletResponse response) {
        log.info("获取下一题（流式），面试ID：{}", id);
        
        // 设置响应编码
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/event-stream;charset=UTF-8");
        
        Flux<ServerSentEvent<String>> flux = interviewService.getNextQuestionStream(id)
                .map(chunk -> {
                    // 确保chunk使用UTF-8编码
                    String utf8Chunk = new String(chunk.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
                    log.info("发送UTF-8编码片段: {}", utf8Chunk);
                    return ServerSentEvent.<String>builder()
                            .data(utf8Chunk)
                            .event("message")
                            .build();
                });
                
        return ResponseEntity.ok()
                .header("Content-Type", "text/event-stream;charset=UTF-8")
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .body(flux);
    }
    
}