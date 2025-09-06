package com.sysu.smartjob.interceptor;

import com.sysu.smartjob.constant.JwtClaimsConstant;
import com.sysu.smartjob.property.JwtProperties;
import com.sysu.smartjob.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 认证拦截器
 * 在 WebSocket 握手阶段验证用户身份
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                 WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            // 从查询参数获取 token
            String query = request.getURI().getQuery();
            if (query == null || !query.contains("token=")) {
                log.warn("WebSocket handshake failed: No token provided");
                return false;
            }

            String token = extractTokenFromQuery(query);
            if (token == null || token.isEmpty()) {
                log.warn("WebSocket handshake failed: Empty token");
                return false;
            }

            // 验证 token 并提取用户信息
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());

            // 将用户信息存储到 WebSocket 会话属性中
            attributes.put("userId", userId);
            attributes.put("token", token);

            log.info("WebSocket handshake successful for user: {}", userId);
            return true;

        } catch (Exception e) {
            log.error("WebSocket authentication failed", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                             WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket handshake completed with error", exception);
        } else {
            log.debug("WebSocket handshake completed successfully");
        }
    }

    /**
     * 从查询字符串中提取 token
     */
    private String extractTokenFromQuery(String query) {
        String[] params = query.split("&");
        for (String param : params) {
            if (param.startsWith("token=")) {
                return param.substring(6); // "token=".length() = 6
            }
        }
        return null;
    }
}