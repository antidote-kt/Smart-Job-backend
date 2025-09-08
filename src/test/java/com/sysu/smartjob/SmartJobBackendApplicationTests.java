package com.sysu.smartjob;

import com.sysu.smartjob.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(classes = TestConfig.class)
class SmartJobBackendApplicationTests {

	@Test
	void contextLoads() {
		// 测试 Spring 上下文是否能正常加载
	}
	
	// 注释掉需要外部依赖的测试
	// @Test
	// void chatTest() {
	// 	String res = chatService.chat(1, "你是谁");
	// 	System.out.println(res);
	// }
}
