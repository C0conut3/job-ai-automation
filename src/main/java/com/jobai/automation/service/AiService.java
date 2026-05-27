package com.jobai.automation.service;

import com.jobai.automation.config.AiConfig;
import com.jobai.automation.metrics.LlmMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final ChatClient chatClient;
    private final AiConfig aiConfig;
    private final LlmMetricsCollector metricsCollector;
    @Autowired(required = false)
    private SyncMcpToolCallbackProvider mcpToolCallbackProvider;

    public AiService(@Qualifier("openAiChatClient") ChatClient chatClient, 
                     AiConfig aiConfig,
                     LlmMetricsCollector metricsCollector) {
        this.chatClient = chatClient;
        this.aiConfig = aiConfig;
        this.metricsCollector = metricsCollector;
    }

    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, aiConfig.getModel(), 1000);
    }

    public String chat(String systemPrompt, String userPrompt, String model) {
        return chat(systemPrompt, userPrompt, model, 1000);
    }

    public String chat(String systemPrompt, String userPrompt, String model, int maxTokens) {
        return chatWithTemperature(systemPrompt, userPrompt, model, maxTokens, 0.7);
    }

    public String chatWithTemperature(String systemPrompt, String userPrompt, String model, int maxTokens, double temperature) {
        try {
            log.info("调用AI - 模型: {}, maxTokens: {}, temperature: {}", model, maxTokens, temperature);

            DashScopeChatOptions options = DashScopeChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .build();

            String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(options)
                .advisors(new SimpleLoggerAdvisor())
                .call()
                .content();

            // 估算Token消耗并记录（实际应从API响应中获取准确值）
            long inputTokens = estimateTokens(systemPrompt + userPrompt);
            long outputTokens = estimateTokens(response);
            metricsCollector.recordCall(model, inputTokens, outputTokens);

            log.info("AI调用完成 - 模型: {}, 输入Token: {}, 输出Token: {}", model, inputTokens, outputTokens);
            return response;
        } catch (Exception e) {
            log.error("AI chat error: {}", e.getMessage(), e);
            throw new RuntimeException("AI chat error: " + e.getMessage(), e);
        }
    }

    public String simpleChat(String userPrompt) {
        return chat("你是一个有用的助手。", userPrompt);
    }

    public String simpleChat(String userPrompt, String model) {
        return chat("你是一个有用的助手。", userPrompt, model);
    }

    public String chatWithTools(String systemPrompt, String userPrompt, String model, int maxTokens, double temperature) {
        try {
            log.info("========== MCP Tool Calling 开始 ==========");
            log.info("调用AI (Tool Calling模式) - 模型: {}, maxTokens: {}, temperature: {}", model, maxTokens, temperature);
            log.info("System Prompt长度: {}, User Prompt: {}", systemPrompt.length(), userPrompt);

            log.info("检查MCP工具配置...");
            log.info("ChatClient已注册默认工具回调: {}", chatClient != null);
            
            // 记录MCP工具回调信息
            if (mcpToolCallbackProvider != null) {
                var toolCallbacks = mcpToolCallbackProvider.getToolCallbacks();
                log.info("MCP工具回调数量: {}", toolCallbacks != null ? toolCallbacks.length : 0);
                if (toolCallbacks != null) {
                    for (var callback : toolCallbacks) {
                        log.info("  - 注册的工具: {}", callback.getToolDefinition().name());
                    }
                }
            } else {
                log.warn("MCP工具回调 provider 为 null!");
            }

            // 通义千问启用工具调用的正确方式：
            // 当工具通过 SyncMcpToolCallbackProvider 注册后，Spring AI 会自动启用 Function Calling
            DashScopeChatOptions options = DashScopeChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .build();

            log.info("准备发送Tool Calling请求...");
            String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(options)
                .advisors(new SimpleLoggerAdvisor())
                .tools()
                .call()
                .content();

            log.info("MCP Tool Calling请求完成");
            log.info("AI响应长度: {}, 内容预览: {}", 
                response != null ? response.length() : 0,
                response != null && response.length() > 200 ? response.substring(0, 200) + "..." : response);

            long inputTokens = estimateTokens(systemPrompt + userPrompt);
            long outputTokens = estimateTokens(response);
            metricsCollector.recordCall(model, inputTokens, outputTokens);

            log.info("AI Tool Calling调用完成 - 模型: {}, 输入Token: {}, 输出Token: {}", model, inputTokens, outputTokens);
            log.info("========== MCP Tool Calling 结束 ==========");
            return response;
        } catch (Exception e) {
            log.error("AI chat with tools error: {}", e.getMessage(), e);
            throw new RuntimeException("AI chat with tools error: " + e.getMessage(), e);
        }
    }

    /**
     * 估算Token数量（粗略估算，中文字符按2个Token计算）
     */
    private long estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 中文字符按1.5个Token估算，英文字符按0.5个Token估算
        long chineseChars = text.chars().filter(c -> c >= 0x4E00 && c <= 0x9FFF).count();
        long otherChars = text.length() - chineseChars;
        return (long) (chineseChars * 1.5 + otherChars * 0.5);
    }
}
