package com.jobai.automation.service;

import com.jobai.automation.config.AiConfig;
import com.jobai.automation.metrics.LlmMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final ChatClient chatClient;
    private final AiConfig aiConfig;
    private final LlmMetricsCollector metricsCollector;

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
            log.info("调用AI (Tool Calling模式) - 模型: {}, maxTokens: {}, temperature: {}", model, maxTokens, temperature);

            DashScopeChatOptions options = DashScopeChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .build();

            String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(options)
                .advisors(new SimpleLoggerAdvisor())
                .tools()
                .call()
                .content();

            long inputTokens = estimateTokens(systemPrompt + userPrompt);
            long outputTokens = estimateTokens(response);
            metricsCollector.recordCall(model, inputTokens, outputTokens);

            log.info("AI Tool Calling调用完成 - 模型: {}, 输入Token: {}, 输出Token: {}", model, inputTokens, outputTokens);
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
