package com.jobai.automation.service;

import com.jobai.automation.config.AiConfig;
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

    public AiService(@Qualifier("openAiChatClient") ChatClient chatClient, AiConfig aiConfig) {
        this.chatClient = chatClient;
        this.aiConfig = aiConfig;
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

            return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(options)
                .advisors(new SimpleLoggerAdvisor())
                .call()
                .content();
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
}