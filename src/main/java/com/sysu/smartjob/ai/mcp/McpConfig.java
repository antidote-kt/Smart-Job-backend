package com.sysu.smartjob.ai.mcp;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {
    @Value("${bigmodel.api-key}")
    private String apiKey;
    @Bean
    public McpToolProvider mcpToolProvider(){
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("https://open.bigmodel.cn/api/mcp/web_search/sse?Authorization=" + apiKey)
                .logRequests(true) // 如果你想在日志中查看流量
                .logResponses(true)
                .build();
        McpClient mcpClient = new DefaultMcpClient.Builder()
                .key("McpClient")
                .transport(transport)
                .build();
        McpToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(mcpClient)
                .build();
        return toolProvider;
    }
}
