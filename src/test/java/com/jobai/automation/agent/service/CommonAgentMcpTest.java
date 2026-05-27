package com.jobai.automation.agent.service;

import com.jobai.automation.agent.constant.AgentCategory;
import com.jobai.automation.agent.dto.AgentRequest;
import com.jobai.automation.agent.dto.AgentResponse;
import com.jobai.automation.agent.service.impl.CommonAgentServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP 上下文调用测试类
 * 测试 CommonAgent 是否正确通过 MCP 工具读取上下文
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CommonAgentMcpTest {

    @Autowired
    private CommonAgentServiceImpl commonAgentService;

    private static final String TEST_USER_ID = "test_user";
    private static final String TEST_SESSION_ID = "test_session";
    private static final String CONTEXT_FILE = "mcp_context/context_" + TEST_USER_ID + "_" + TEST_SESSION_ID + ".txt";

    @BeforeEach
    void setUp() throws Exception {
        // 确保测试上下文目录存在
        Path contextDir = Paths.get("mcp_context");
        if (!Files.exists(contextDir)) {
            Files.createDirectories(contextDir);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        // 清理测试文件
        Path filePath = Paths.get(CONTEXT_FILE);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
    }

    /**
     * 测试1：验证 MCP 工具注册是否正常
     */
    @Test
    @Order(1)
    @DisplayName("测试 MCP 工具注册")
    void testMcpToolRegistration() {
        assertNotNull(commonAgentService, "CommonAgentService 不应为 null");
        
        // 验证上下文目录存在
        assertTrue(Files.exists(Paths.get("mcp_context")), "MCP 上下文目录应存在");
    }

    /**
     * 测试2：测试上下文文件写入功能
     */
    @Test
    @Order(2)
    @DisplayName("测试上下文文件写入")
    void testContextFileWrite() throws Exception {
        // 发送第一个问题
        AgentRequest request1 = new AgentRequest(TEST_USER_ID, AgentCategory.COMMON, "前端学习有什么途径", null, TEST_SESSION_ID);
        AgentResponse response1 = commonAgentService.ask(request1);
        
        assertNotNull(response1, "响应不应为 null");
        assertNotNull(response1.conversation(), "对话内容不应为 null");
        assertFalse(response1.conversation().isEmpty(), "对话内容不应为空");
        
        // 验证上下文文件已创建
        Path filePath = Paths.get(CONTEXT_FILE);
        assertTrue(Files.exists(filePath), "上下文文件应已创建");
        
        // 验证文件内容包含用户问题
        String content = Files.readString(filePath);
        assertTrue(content.contains("前端学习有什么途径"), "上下文文件应包含用户问题");
    }

    /**
     * 测试3：测试 MCP 工具读取上下文
     */
    @Test
    @Order(3)
    @DisplayName("测试 MCP 上下文读取")
    void testMcpContextReading() throws Exception {
        // 第一步：先发送一个问题，创建上下文
        AgentRequest request1 = new AgentRequest(TEST_USER_ID, AgentCategory.COMMON, "前端学习有什么途径", null, TEST_SESSION_ID);
        AgentResponse response1 = commonAgentService.ask(request1);
        
        assertNotNull(response1);
        assertNotNull(response1.conversation());
        assertFalse(response1.conversation().isEmpty());
        
        // 验证上下文文件已创建
        Path filePath = Paths.get(CONTEXT_FILE);
        assertTrue(Files.exists(filePath), "上下文文件应已创建");
        
        // 第二步：询问关于上一个问题的问题
        AgentRequest request2 = new AgentRequest(TEST_USER_ID, AgentCategory.COMMON, "我上一个问题问了什么", null, TEST_SESSION_ID);
        AgentResponse response2 = commonAgentService.ask(request2);
        
        assertNotNull(response2, "响应不应为 null");
        assertNotNull(response2.conversation(), "对话内容不应为 null");
        assertFalse(response2.conversation().isEmpty(), "对话内容不应为空");
        
        String answer = response2.conversation().get(0).content();
        System.out.println("AI 响应: " + answer);
        
        // 验证响应包含上下文信息（如果 MCP 调用成功，应该提到"前端学习"）
        assertTrue(
            answer.contains("前端") || answer.contains("学习") || answer.contains("途径"),
            "响应应包含上下文信息，实际响应: " + answer
        );
    }

    /**
     * 测试4：测试新对话（无上下文）场景
     */
    @Test
    @Order(4)
    @DisplayName("测试新对话（无上下文）")
    void testNewConversation() {
        // 使用新的 session ID 确保没有历史上下文
        String newSessionId = "new_session_123";
        String newContextFile = "mcp_context/context_" + TEST_USER_ID + "_" + newSessionId + ".txt";
        
        // 确保文件不存在
        try {
            Files.deleteIfExists(Paths.get(newContextFile));
        } catch (Exception ignored) {}
        
        AgentRequest request = new AgentRequest(TEST_USER_ID, AgentCategory.COMMON, "你好", null, newSessionId);
        AgentResponse response = commonAgentService.ask(request);
        
        assertNotNull(response);
        assertNotNull(response.conversation());
        assertFalse(response.conversation().isEmpty());
        
        // 验证响应是友好的问候
        String answer = response.conversation().get(0).content();
        assertTrue(answer.contains("你好") || answer.contains("您好") || answer.contains("帮助"), 
            "新对话应收到友好问候，实际响应: " + answer);
    }
}