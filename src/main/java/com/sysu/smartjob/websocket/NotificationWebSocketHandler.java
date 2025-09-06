package com.sysu.smartjob.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 通知 WebSocket 处理器
 * 单向通信：只发送通知，不处理客户端消息
 */
@Slf4j
@Component
public class NotificationWebSocketHandler implements WebSocketHandler {

    // 存储用户ID到WebSocket会话的映射
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        
        if (userId != null) {
            userSessions.put(userId, session);
            log.info("User {} connected, total connections: {}", userId, userSessions.size());
        } else {
            log.warn("Session without userId, closing");
            session.close();
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        // 单向通信 - 忽略客户端消息
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        log.error("WebSocket error for user {}", userId, exception);
        
        if (userId != null) {
            userSessions.remove(userId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.remove(userId);
            log.info("User {} disconnected, total connections: {}", userId, userSessions.size());
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 发送报告生成完成通知
     */
    public void sendReportGeneratedNotification(Long userId, Long sessionId, String message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                ReportNotification notification = new ReportNotification(
                    "REPORT_GENERATED", sessionId, message, System.currentTimeMillis()
                );

                String jsonMessage = objectMapper.writeValueAsString(notification);
                session.sendMessage(new TextMessage(jsonMessage));
                
                log.info("Notification sent to user {}: sessionId={}", userId, sessionId);
            } catch (Exception e) {
                log.error("Failed to send notification to user {}", userId, e);
            }
        } else {
            log.debug("User {} not connected", userId);
        }
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * 获取连接数
     */
    public int getConnectionCount() {
        return userSessions.size();
    }

    /**
     * 简化的通知消息类
     */
    public record ReportNotification(String type, Long sessionId, String message, Long timestamp) {}
}