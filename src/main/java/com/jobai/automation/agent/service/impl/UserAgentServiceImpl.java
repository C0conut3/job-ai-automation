package com.jobai.automation.agent.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobai.automation.agent.constant.AgentCategory;
import com.jobai.automation.agent.dto.AgentRequest;
import com.jobai.automation.agent.dto.AgentResponse;
import com.jobai.automation.agent.dto.MessageDto;
import com.jobai.automation.agent.service.AdviceAgentService;
import com.jobai.automation.agent.service.CommonAgentService;
import com.jobai.automation.agent.service.PreferenceAgentService;
import com.jobai.automation.agent.service.UserAgentService;
import com.jobai.automation.config.AiConfig;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Service
public class UserAgentServiceImpl implements UserAgentService {

    private final PreferenceAgentService preferenceAgentService;
    private final AdviceAgentService adviceAgentService;
    private final CommonAgentService commonAgentService;
    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public UserAgentServiceImpl(PreferenceAgentService preferenceAgentService,
                                AdviceAgentService adviceAgentService,
                                CommonAgentService commonAgentService,
                                AiConfig aiConfig) {
        this.preferenceAgentService = preferenceAgentService;
        this.adviceAgentService = adviceAgentService;
        this.commonAgentService = commonAgentService;
        this.aiConfig = aiConfig;
    }

    @Override
    public AgentResponse handle(AgentRequest request) {
        // 如果有用户消息，优先做意图分类并返回固定 JSON（as conversation 内容）
        if (request.message() != null && !request.message().isBlank()) {
            String jsonOut;
            try {
                String system = "你是意图分类助手。将用户输入归类为三类之一：DELIVERY, ANALYSIS, OTHER。" +
                        "DELIVERY 表示用户想找工作、搜岗位、要投递或请求推荐岗位。" +
                        "ANALYSIS 表示用户询问水平、竞争力、投递建议或匹配度。" +
                        "OTHER 表示其他问题（面试题、知识、代码、杂项）。\n" +
                        "严格返回单个 JSON 对象，格式为：{\"category\": \"DELIVERY|ANALYSIS|OTHER\", \"content\": \"用户原始输入\"}。不要输出其他文本或注释。";

                String user = request.message();
                String model = (request.model() != null && !request.model().isBlank()) ? request.model() : aiConfig.getModelForUserAgent();
                String url = aiConfig.getBaseUrl();
                if (!url.endsWith("/")) url = url + "/";
                url = url + "v1/chat/completions";

                var root = objectMapper.createObjectNode();
                root.put("model", model);
                root.put("max_tokens", 150);
                var messages = objectMapper.createArrayNode();
                var m1 = objectMapper.createObjectNode();
                m1.put("role", "system");
                m1.put("content", system);
                var m2 = objectMapper.createObjectNode();
                m2.put("role", "user");
                m2.put("content", user);
                messages.add(m1);
                messages.add(m2);
                root.set("messages", messages);
                root.put("temperature", 0);

                String body = objectMapper.writeValueAsString(root);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(30))
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
                String assistantText = "";
                if (choices.isArray() && choices.size() > 0) {
                    assistantText = choices.get(0).path("message").path("content").asText("");
                } else {
                    throw new RuntimeException("AI returned no choices");
                }

                // 提取 JSON 子串
                int s = assistantText.indexOf('{');
                int e = assistantText.lastIndexOf('}');
                jsonOut = (s >= 0 && e > s) ? assistantText.substring(s, e + 1) : assistantText;

                // 验证并规范化 JSON
                JsonNode parsed = objectMapper.readTree(jsonOut);
                String cat = parsed.path("category").asText("");
                String content = parsed.path("content").asText("");
                if (cat == null || !(cat.equals("DELIVERY") || cat.equals("ANALYSIS") || cat.equals("OTHER"))) {
                    // 若模型未严格遵守格式，使用简单关键词映射回退
                    String lower = user.toLowerCase();
                    if (lower.contains("找") || lower.contains("招聘") || lower.contains("投递") || lower.contains("岗位") || lower.contains("推荐")) {
                        cat = "DELIVERY";
                    } else if (lower.contains("水平") || lower.contains("竞争力") || lower.contains("匹配") || lower.contains("建议")) {
                        cat = "ANALYSIS";
                    } else {
                        cat = "OTHER";
                    }
                    var outNode = objectMapper.createObjectNode();
                    outNode.put("category", cat);
                    outNode.put("content", (content == null || content.isBlank()) ? user : content);
                    jsonOut = objectMapper.writeValueAsString(outNode);
                }

            } catch (Exception ex) {
                // 出现异常则返回 OTHER
                try {
                    var outNode = objectMapper.createObjectNode();
                    outNode.put("category", "OTHER");
                    outNode.put("content", request.message());
                    jsonOut = objectMapper.writeValueAsString(outNode);
                } catch (Exception e) {
                    jsonOut = "{\"category\":\"OTHER\",\"content\":\"" + request.message().replaceAll("\"","\\\"") + "\"}";
                }
            }

            // 根据分类路由到不同的 Agent
            try {
                JsonNode parsed2 = objectMapper.readTree(jsonOut);
                String finalCat = parsed2.path("category").asText("");
                String finalContent = parsed2.path("content").asText("");
                if ("DELIVERY".equals(finalCat)) {
                    AgentRequest prefReq = new AgentRequest(request.userId(), AgentCategory.PREFERENCE, (finalContent == null || finalContent.isBlank()) ? request.message() : finalContent, request.model());
                    return preferenceAgentService.analyze(prefReq);
                } else if ("ANALYSIS".equals(finalCat)) {
                    AgentRequest adviceReq = new AgentRequest(request.userId(), AgentCategory.ADVICE, (finalContent == null || finalContent.isBlank()) ? request.message() : finalContent, request.model());
                    return adviceAgentService.suggest(adviceReq);
                } else if ("OTHER".equals(finalCat)) {
                    AgentRequest commonReq = new AgentRequest(request.userId(), AgentCategory.COMMON, (finalContent == null || finalContent.isBlank()) ? request.message() : finalContent, request.model());
                    return commonAgentService.ask(commonReq);
                }
            } catch (Exception ignore) {
                // ignore parsing errors here and fall back to returning the classification JSON
            }

            MessageDto msg = new MessageDto("assistant", jsonOut);
            return new AgentResponse(List.of(msg), List.of(), null);
        }

        // 没有 message 时按原先 category 路由
        if (request.category() == AgentCategory.PREFERENCE) {
            return preferenceAgentService.analyze(request);
        } else if (request.category() == AgentCategory.ADVICE) {
            return adviceAgentService.suggest(request);
        } else {
            return commonAgentService.ask(request);
        }
    }
}