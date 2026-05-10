package com.jobai.automation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    @Value("${ai.openai.model:gpt-3.5-turbo}")
    private String model;

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

    @Value("${ai.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
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

    public String getBaseUrl() {
        return baseUrl;
    }
}