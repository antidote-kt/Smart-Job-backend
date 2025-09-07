package com.sysu.smartjob.ai;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
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
    @Autowired
    private ContentRetriever contentRetriever;
    @Bean
    public ChatService chatService(){
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(20);
        ChatService chatService = AiServices.builder(ChatService.class)
                .chatModel(qwenChatModel)
                .streamingChatModel(streamingChatModel)
                .contentRetriever(contentRetriever)
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                .toolProvider(mcpToolProvider)
                .build();
        return chatService;
    }
}
