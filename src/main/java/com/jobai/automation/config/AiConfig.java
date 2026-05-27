package com.jobai.automation.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    @Value("${spring.ai.dashscope.chat.options.model:qwen-plus}")
    private String model;

    @Value("${spring.ai.dashscope.base-url:https://dashscope.aliyuncs.com}")
    private String baseUrl;

    @Value("${ai.openai.model.parse:}")
    private String parseModel;

    @Value("${ai.openai.model.optimize:}")
    private String optimizeModel;

    @Value("${ai.openai.model.user-agent:}")
    private String userAgentModel;

    @Value("${ai.openai.model.preference-agent:}")
    private String preferenceAgentModel;

    @Value("${ai.openai.model.advice-agent:}")
    private String adviceAgentModel;

    @Value("${ai.openai.model.common-agent:}")
    private String commonAgentModel;

    @Autowired(required = false)
    private SyncMcpToolCallbackProvider mcpToolCallbackProvider;

    @Bean("openAiChatClient")
    public ChatClient openAiChatClient(ChatModel chatModel) {
        if (mcpToolCallbackProvider != null) {
            return ChatClient.builder(chatModel)
                    .defaultToolCallbacks(mcpToolCallbackProvider.getToolCallbacks())
                    .build();
        }
        return ChatClient.builder(chatModel).build();
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModelForParse() {
        return (parseModel != null && !parseModel.isBlank()) ? parseModel : model;
    }

    public String getModelForOptimize() {
        return (optimizeModel != null && !optimizeModel.isBlank()) ? optimizeModel : model;
    }

    public String getModelForUserAgent() {
        return (userAgentModel != null && !userAgentModel.isBlank()) ? userAgentModel : model;
    }

    public String getModelForPreferenceAgent() {
        return (preferenceAgentModel != null && !preferenceAgentModel.isBlank()) ? preferenceAgentModel : model;
    }

    public String getModelForAdviceAgent() {
        return (adviceAgentModel != null && !adviceAgentModel.isBlank()) ? adviceAgentModel : model;
    }

    public String getModelForCommonAgent() {
        return (commonAgentModel != null && !commonAgentModel.isBlank()) ? commonAgentModel : model;
    }
}
