package com.jobai.automation.agent.service.impl;

import com.jobai.automation.agent.dto.AgentRequest;
import com.jobai.automation.agent.dto.AgentResponse;
import com.jobai.automation.agent.dto.MessageDto;
import com.jobai.automation.agent.service.CommonAgentService;
import com.jobai.automation.config.AiConfig;
import com.jobai.automation.mcp.FilesystemMcpService;
import com.jobai.automation.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class CommonAgentServiceImpl implements CommonAgentService {

    private static final Logger log = LoggerFactory.getLogger(CommonAgentServiceImpl.class);
    private static final String BASE_DIR = "mcp_context";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AiConfig aiConfig;
    private final AiService aiService;
    private final FilesystemMcpService filesystemMcpService;

    public CommonAgentServiceImpl(AiConfig aiConfig, AiService aiService, FilesystemMcpService filesystemMcpService) {
        this.aiConfig = aiConfig;
        this.aiService = aiService;
        this.filesystemMcpService = filesystemMcpService;
    }

    @Override
    public AgentResponse ask(AgentRequest request) {
        String userInput = request.message() == null ? "" : request.message().trim();

        if (userInput.isBlank()) {
            return new AgentResponse(
                    java.util.List.of(new MessageDto("assistant", "请问您有什么问题？我可以帮您解答面试技巧、技术问题、职业发展等方面的疑问。")),
                    java.util.List.of(),
                    null
            );
        }

        try {
            String model = (request.model() != null && !request.model().isBlank())
                    ? request.model() : aiConfig.getModelForCommonAgent();

            String userId = request.userId() != null ? request.userId() : "default_user";
            String sessionId = request.sessionId() != null ? request.sessionId() : "default_session";
            String contextFile = "context_" + userId + "_" + sessionId + ".txt";

            // System prompt: 告知AI使用read_file读取上下文
            String systemPrompt = "你是一位专业的职业顾问和技术导师，专注于帮助求职者提升技能和职业发展。\n\n" +
                    "【上下文管理】：\n" +
                    "- 上下文文件名：context_{userId}_{sessionId}.txt，其中 userId=" + userId + ", sessionId=" + sessionId + "\n" +
                    "- 当需要了解历史对话时，调用 read_file(\"" + contextFile + "\") 读取上下文\n" +
                    "- 系统会在每次对话后自动保存，无需你调用写入工具\n\n" +
                    "【回答原则】：\n" +
                    "1. 优先以用户当前问题为主，上下文内容仅作为辅助参考\n" +
                    "2. 聚焦就业相关问题，包括面试、技术、职业规划等\n" +
                    "3. 如果用户问题与就业无关，礼貌地引导用户询问就业相关话题\n\n" +
                    "擅长领域包括：\n" +
                    "- 面试技巧与准备\n" +
                    "- 技术面试题解答\n" +
                    "- 编程语言与技术知识\n" +
                    "- 职业发展规划\n" +
                    "- 学习路线建议\n" +
                    "- 简历优化建议\n" +
                    "- 职场沟通技巧\n\n" +
                    "请用友好、专业的语气回复，语言简洁明了，重点突出可执行性。";

            // User prompt: 只传递当前问题，AI通过Tool Calling读取上下文
            String userPrompt = userInput;

            // 调用AI（启用Tool Calling，AI主动读取上下文）
            String answerContent = aiService.chatWithTools(systemPrompt, userPrompt, model, 1500, 0.7);

            if (answerContent == null || answerContent.isBlank()) {
                answerContent = "抱歉，暂时无法回答这个问题，请稍后重试。";
            }

            // 【Java代码控制】强制保存本次对话
            saveContext(contextFile, userInput, answerContent);

            return new AgentResponse(
                    java.util.List.of(new MessageDto("assistant", answerContent)),
                    java.util.List.of(),
                    null
            );

        } catch (Exception ex) {
            String errorMessage = "⚠️ 回答问题时遇到问题：\n\n" + ex.getMessage() + "\n\n请稍后重试。";
            log.error("Common agent error", ex);
            return new AgentResponse(
                    java.util.List.of(new MessageDto("assistant", errorMessage)),
                    java.util.List.of(),
                    null
            );
        }
    }

    // 【Java代码控制】保存对话到文件（追加模式）
    private void saveContext(String contextFile, String userInput, String answer) {
        try {
            Path filePath = Paths.get(BASE_DIR, contextFile);
            String timestamp = LocalDateTime.now().format(FORMATTER);
            String newEntry = String.format("[%s]\n用户: %s\n助手: %s\n\n", timestamp, userInput, answer);
            String existingContent = Files.exists(filePath) ? Files.readString(filePath) : "";
            String newContent = existingContent + newEntry;
            Files.writeString(filePath, newContent);
            log.debug("Saved context to {}", contextFile);
        } catch (Exception e) {
            log.warn("Failed to save context to {}: {}", contextFile, e.getMessage());
        }
    }
}