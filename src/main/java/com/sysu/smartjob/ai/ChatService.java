package com.sysu.smartjob.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface ChatService {
    @SystemMessage(fromResource = "system-prompt.txt")
    String chat(@MemoryId int memoryId, @UserMessage String userMessage);

    @SystemMessage(fromResource = "system-prompt.txt")
    Flux<String> chatStream(@MemoryId int memoryId, @UserMessage String userMessage);
}
