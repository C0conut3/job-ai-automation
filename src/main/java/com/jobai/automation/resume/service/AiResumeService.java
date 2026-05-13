package com.jobai.automation.resume.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobai.automation.config.AiConfig;
import com.jobai.automation.resume.web.dto.ParsedResumeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class AiResumeService {

    private static final Logger log = LoggerFactory.getLogger(AiResumeService.class);
    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public AiResumeService(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
    }

    private String callChatApi(String model, String systemPrompt, String userPrompt) throws IOException, InterruptedException {
        String apiKey = aiConfig.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("OpenAI API key is not configured (ai.openai.api-key)");
        }

        String url = aiConfig.getBaseUrl();
        if (!url.endsWith("/")) url = url + "/";
        url = url + "v1/chat/completions";

        // build request body
        var root = objectMapper.createObjectNode();
        root.put("model", model != null && !model.isBlank() ? model : aiConfig.getModel());
        root.put("max_tokens", 800);
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
        root.put("temperature", 0);

        String body = objectMapper.writeValueAsString(root);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            throw new RuntimeException("OpenAI API error: " + response.statusCode() + " - " + response.body());
        }

        JsonNode resJson = objectMapper.readTree(response.body());
        JsonNode choices = resJson.path("choices");
        if (!choices.isArray() || choices.size() == 0) {
            throw new RuntimeException("OpenAI returned no choices: " + response.body());
        }
        JsonNode message = choices.get(0).path("message");
        String content = message.path("content").asText("");
        return content;
    }

    public ParsedResumeDto parseAndCategorizeResume(String parsedContent) {
        String system = "你是一个简历解析助手。目标是从提供的简历文本中提取结构化字段，并以严格的 JSON 格式返回：{\"name\": \"\", \"personalInfo\": \"\", \"education\": \"\", \"jobStatus\": \"\", \"workExperience\": \"\", \"projectExperience\": \"\"}。如果某个字段无法识别，请使用空字符串。不要添加额外的说明、注释或Markdown。输出必须是一个纯粹的JSON对象，不能包含前置或后置文本、代码块，且字符串必须以双引号结束。";
        String user = "原始简历文本：\n" + parsedContent;

        String reply = null;
        try {
            reply = callChatApi(aiConfig.getModelForParse(), system, user);
            // 记录原始回复用于调试
            log.info("AI raw reply: {}", reply);

            int s = reply.indexOf('{');
            int e = reply.lastIndexOf('}');
            String json = (s >= 0 && e > s) ? reply.substring(s, e + 1) : reply;

            // 预验证JSON格式
            if (!json.trim().startsWith("{") || !json.trim().endsWith("}")) {
                throw new RuntimeException("AI回复不是有效的JSON对象格式");
            }

            ParsedResumeDto dto = objectMapper.readValue(json, ParsedResumeDto.class);
            return dto;
        } catch (Exception ex) {
            String errorMessage = ex.getMessage();
            log.error("AI resume parse failed. error={}, reply={}", errorMessage, reply, ex);
            throw new RuntimeException("AI 解析失败: 返回内容不是合法的JSON，请检查模型输出是否完整。原始错误：" + errorMessage, ex);
        }
    }

    public String optimizeResume(String contentToOptimize) {
        String system = "你是一个简历优化助手。对给定的简历文本进行语言、结构和要点优化，返回优化后的文本，不要包含其他说明。";
        try {
            return callChatApi(aiConfig.getModelForOptimize(), system, contentToOptimize);
        } catch (Exception ex) {
            throw new RuntimeException("AI 优化失败: " + ex.getMessage(), ex);
        }
    }

    public ParsedResumeDto optimizeAndCategorizeResume(String rawContent) {
        return optimizeAndCategorizeResume(rawContent, null);
    }

    public ParsedResumeDto optimizeAndCategorizeResume(String rawContent, String modelOverride) {
        String system = "你是一个简历优化助手。对提供的简历文本进行语言、结构和要点优化。请分别输出以下字段：{\"name\": \"\", \"personalInfo\": \"\", \"education\": \"\", \"jobStatus\": \"\", \"workExperience\": \"\", \"projectExperience\": \"\"}。输出必须是严格的 JSON 对象且不要包含任何额外说明或 Markdown 代码块。若某字段无需修改，返回原文或空字符串。";
        String user = "原始简历文本：\n" + rawContent;
        String reply = null;
        try {
            reply = callChatApi(modelOverride != null && !modelOverride.isBlank() ? modelOverride : aiConfig.getModelForOptimize(), system, user);
            log.info("AI optimize raw reply: {}", reply);
            int s = reply.indexOf('{');
            int e = reply.lastIndexOf('}');
            String json = (s >= 0 && e > s) ? reply.substring(s, e + 1) : reply;
            if (!json.trim().startsWith("{") || !json.trim().endsWith("}")) {
                throw new RuntimeException("AI回复不是有效的JSON对象格式");
            }
            ParsedResumeDto dto = objectMapper.readValue(json, ParsedResumeDto.class);
            return dto;
        } catch (Exception ex) {
            log.error("AI optimize failed. error={}, reply={}", ex.getMessage(), reply, ex);
            throw new RuntimeException("AI 优化失败: 返回内容不是合法的JSON，请检查模型输出是否完整。原始错误：" + ex.getMessage(), ex);
        }
    }
}
