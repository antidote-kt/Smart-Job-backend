package com.sysu.smartjob;

import com.sysu.smartjob.ai.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

@SpringBootTest
class SmartJobBackendApplicationTests {
	@Autowired
	private ChatService chatService;

	@Test
	void contextLoads() {


	}
	@Test
	void chatTest() {
		String res = chatService.chat(1, "你是谁");
		System.out.println(res);
	}
}
