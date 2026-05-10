package com.jobai.automation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class JobAiAutomationApplicationTests {

    @Test
    void contextLoads() {
        // 使用 H2 内存库验证 Spring 上下文可启动
    }
}
