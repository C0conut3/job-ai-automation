package com.jobai.automation.agent.service.impl;

import com.jobai.automation.agent.dto.AgentRequest;
import com.jobai.automation.agent.dto.AgentResponse;
import com.jobai.automation.agent.dto.MessageDto;
import com.jobai.automation.agent.service.CommonAgentService;
import com.jobai.automation.config.AiConfig;
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

    public CommonAgentServiceImpl(AiConfig aiConfig, AiService aiService) {
        this.aiConfig = aiConfig;
        this.aiService = aiService;
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

            // System prompt: 智能判断是否需要读取上下文
            String systemPrompt = "你是一位专业的职业顾问和技术导师，专注于帮助求职者提升技能和职业发展。\n\n" +
                    "【工具使用规则】：\n" +
                    "- read_file(path): 读取指定文件内容，用于获取历史对话上下文\n" +
                    "- write_file(path, content): 写入文件（系统自动调用，无需你使用）\n" +
                    "- list_directory(path): 列出目录内容\n\n" +
                    "【上下文读取策略】：\n" +
                    "1. 智能判断：根据用户问题决定是否需要读取历史上下文\n" +
                    "2. 需要读取的情况：\n" +
                    "   - 用户提到\"之前\"、\"上次\"、\"刚才\"、\"之前说的\"、\"继续\"等词\n" +
                    "   - 用户的问题明显需要结合历史对话才能理解（如\"这个职位怎么样\"、\"再详细说说\"）\n" +
                    "   - 用户直接引用之前讨论的内容\n" +
                    "3. 不需要读取的情况：\n" +
                    "   - 用户提出的是独立的、完整的问题（如\"什么是Java？\"、\"如何准备面试？\"）\n" +
                    "   - 问题本身包含足够的信息，无需上下文即可回答\n" +
                    "4. 上下文文件路径：mcp_context/context_" + userId + "_" + sessionId + ".txt\n" +
                    "5. 如果读取上下文文件为空或不存在，说明是新对话，直接回答即可\n\n" +
                    "【回答原则】：\n" +
                    "1. 如果读取了上下文，回答时要自然地结合历史对话内容\n" +
                    "2. 如果没有读取上下文，直接回答当前问题即可\n" +
                    "3. 聚焦就业相关问题，包括面试、技术、职业规划等\n\n" +
                    "【擅长领域】：\n" +
                    "- 面试技巧与准备\n" +
                    "- 技术面试题解答\n" +
                    "- 编程语言与技术知识\n" +
                    "- 职业发展规划\n" +
                    "- 学习路线建议\n" +
                    "- 简历优化建议\n\n" +
                    "请用友好、专业的语气回复，保持对话自然流畅。";

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