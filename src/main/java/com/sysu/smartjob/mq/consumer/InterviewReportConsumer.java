package com.sysu.smartjob.mq.consumer;

import com.sysu.smartjob.config.RabbitMQConfig;
import com.sysu.smartjob.service.InterviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 面试报告生成消息消费者
 */
@Component
@Slf4j
public class InterviewReportConsumer {
    
    @Autowired
    private InterviewService interviewService;
    
    /**
     * 监听面试报告生成队列
     * @param interviewId 面试ID
     */
    @RabbitListener(queues = RabbitMQConfig.INTERVIEW_REPORT_QUEUE)
    public void handleInterviewReportGeneration(Long interviewId) {
        log.info("收到面试报告生成消息，面试ID: {}", interviewId);
        
        try {
            // 直接调用现有的报告生成方法
            interviewService.generateInterviewReport(interviewId);
            log.info("面试报告生成完成，面试ID: {}", interviewId);
        } catch (Exception e) {
            log.error("面试报告生成失败，面试ID: {}", interviewId, e);
            // RabbitMQ会根据配置进行重试
            throw e;
        }
    }
}