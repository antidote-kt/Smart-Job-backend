package com.sysu.smartjob.ai.mcp;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class McpConfig {
    
    @Value("${bigmodel.api-key}")
    private String apiKey;
    
    @Bean
    public McpToolProvider mcpToolProvider() {
        try {
            log.info("初始化MCP工具提供者，API Key: {}***", apiKey.substring(0, Math.min(10, apiKey.length())));
            
            // 构建URL，确保正确格式
            String sseUrl = "https://open.bigmodel.cn/api/mcp/web_search/sse?Authorization=" + apiKey;
            log.info("MCP SSE URL: {}", sseUrl.replaceAll("Authorization=.*", "Authorization=***"));
            
            McpTransport transport = new HttpMcpTransport.Builder()
                    .sseUrl(sseUrl)
                    .logRequests(true)
                    .logResponses(true)
                    .build();
                    
            McpClient mcpClient = new DefaultMcpClient.Builder()
                    .key("InterviewMcpClient")
                    .transport(transport)
                    .build();
                    
            McpToolProvider toolProvider = McpToolProvider.builder()
                    .mcpClients(mcpClient)
                    .build();
                    
            log.info("MCP工具提供者初始化成功");
            return toolProvider;
            
        } catch (Exception e) {
            log.error("MCP工具提供者初始化失败", e);
            throw new RuntimeException("Failed to initialize MCP tool provider", e);
        }
    }
}
