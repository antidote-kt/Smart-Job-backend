package com.sysu.smartjob.ai;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatServiceFactory {
    @Autowired
    private ChatModel qwenChatModel;
    @Autowired
    private StreamingChatModel streamingChatModel;
    @Autowired
    private McpToolProvider mcpToolProvider;
    @Bean
    public ChatService chatService(){
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
        ChatService chatService = AiServices.builder(ChatService.class)
                .chatModel(qwenChatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .toolProvider(mcpToolProvider)
                .build();
        return chatService;
    }
}
