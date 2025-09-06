package com.sysu.smartjob.service;

import com.sysu.smartjob.websocket.NotificationWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 通知服务
 * 负责发送各种实时通知
 */
@Slf4j
@Service
public class NotificationService {

    @Autowired
    private NotificationWebSocketHandler webSocketHandler;

    /**
     * 发送报告生成完成通知
     * @param userId 用户ID
     * @param sessionId 面试会话ID
     * @param message 通知消息
     */
    public void sendReportGeneratedNotification(Long userId, Long sessionId, String message) {
        log.info("Sending report generated notification to user {}: sessionId={}", userId, sessionId);
        webSocketHandler.sendReportGeneratedNotification(userId, sessionId, message);
    }

    /**
     * 检查用户是否在线
     * @param userId 用户ID
     * @return 是否在线
     */
    public boolean isUserOnline(Long userId) {
        return webSocketHandler.isUserOnline(userId);
    }

    /**
     * 获取当前在线用户数
     * @return 在线用户数
     */
    public int getOnlineUserCount() {
        return webSocketHandler.getConnectionCount();
    }
}