package com.sysu.smartjob.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 */
@Configuration
public class RabbitMQConfig {
    
    /**
     * 面试报告生成队列名
     */
    public static final String INTERVIEW_REPORT_QUEUE = "interview.report.queue";
    
    /**
     * 定义面试报告生成队列
     */
    @Bean
    public Queue interviewReportQueue() {
        // 持久化队列，服务重启后消息不丢失,name:INTERVIEW_REPORT_QUEUE,durable: true
        return new Queue(INTERVIEW_REPORT_QUEUE, true);
    }
}