package com.sysu.smartjob.service.impl;

import com.sysu.smartjob.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * MQ消息发送服务
 */
@Service
@Slf4j
public class MessageProducerService {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    /**
     * 发送面试报告生成消息
     * @param interviewId 面试ID
     */
    public void sendInterviewReportGenerationMessage(Long interviewId) {
        try {
            // 直接发送面试ID到队列
            rabbitTemplate.convertAndSend(RabbitMQConfig.INTERVIEW_REPORT_QUEUE, interviewId);
            log.info("发送面试报告生成消息成功，面试ID: {}", interviewId);
        } catch (Exception e) {
            log.error("发送面试报告生成消息失败，面试ID: {}", interviewId, e);
        }
    }
}