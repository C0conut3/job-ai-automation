package com.jobai.automation.agent.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobai.automation.agent.dto.AgentRequest;
import com.jobai.automation.agent.dto.AgentResponse;
import com.jobai.automation.agent.dto.MessageDto;
import com.jobai.automation.agent.service.CommonAgentService;
import com.jobai.automation.config.AiConfig;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class CommonAgentServiceImpl implements CommonAgentService {

    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public CommonAgentServiceImpl(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
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
            String url = aiConfig.getBaseUrl();
            if (!url.endsWith("/")) url = url + "/";
            url = url + "v1/chat/completions";

            String systemPrompt = "你是一位专业的职业顾问和技术导师，专注于帮助求职者提升技能和职业发展。\n\n" +
                    "请针对以下问题提供简洁、专业且实用的回答：\n\n" +
                    "擅长领域包括：\n" +
                    "- 面试技巧与准备\n" +
                    "- 技术面试题解答\n" +
                    "- 编程语言与技术知识\n" +
                    "- 职业发展规划\n" +
                    "- 学习路线建议\n" +
                    "- 简历优化建议\n" +
                    "- 职场沟通技巧\n\n" +
                    "请用友好、专业的语气回复，语言简洁明了，重点突出可执行性。";

            String userPrompt = userInput;

            var root = objectMapper.createObjectNode();
            root.put("model", model);
            root.put("max_tokens", 1500);
            var messages = objectMapper.createArrayNode();
            var m1 = objectMapper.createObjectNode();
            m1.put("role", "system");
            m1.put("content", systemPrompt);
            var m2 = objectMapper.createObjectNode();
            m2.put("role", "user");
            m2.put("content", userPrompt);
            messages.add(m1);
            messages.add(m2);
            root.set("messages", messages);
            root.put("temperature", 0.7);

            String body = objectMapper.writeValueAsString(root);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Authorization", "Bearer " + aiConfig.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new RuntimeException("AI API error: " + response.statusCode() + " - " + response.body());
            }

            JsonNode resJson = objectMapper.readTree(response.body());
            JsonNode choices = resJson.path("choices");
            String answerContent = "";
            if (choices.isArray() && choices.size() > 0) {
                answerContent = choices.get(0).path("message").path("content").asText("");
            }

            if (answerContent == null || answerContent.isBlank()) {
                answerContent = "抱歉，暂时无法回答这个问题，请稍后重试。";
            }

            return new AgentResponse(
                    java.util.List.of(new MessageDto("assistant", answerContent)),
                    java.util.List.of(),
                    null
            );

        } catch (Exception ex) {
            String errorMessage = "⚠️ 回答问题时遇到问题：\n\n" + ex.getMessage() + "\n\n请稍后重试。";
            return new AgentResponse(
                    java.util.List.of(new MessageDto("assistant", errorMessage)),
                    java.util.List.of(),
                    null
            );
        }
    }
}