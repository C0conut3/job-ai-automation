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
import com.jobai.automation.service.AiService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserAgentServiceImpl implements UserAgentService {

    private final PreferenceAgentService preferenceAgentService;
    private final AdviceAgentService adviceAgentService;
    private final CommonAgentService commonAgentService;
    private final AiConfig aiConfig;
    private final AiService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserAgentServiceImpl(PreferenceAgentService preferenceAgentService,
                                AdviceAgentService adviceAgentService,
                                CommonAgentService commonAgentService,
                                AiConfig aiConfig,
                                AiService aiService) {
        this.preferenceAgentService = preferenceAgentService;
        this.adviceAgentService = adviceAgentService;
        this.commonAgentService = commonAgentService;
        this.aiConfig = aiConfig;
        this.aiService = aiService;
    }

    @Override
    public AgentResponse handle(AgentRequest request) {
        if (request.message() != null && !request.message().isBlank()) {
            String jsonOut;
            try {
                String system = "你是意图分类助手。将用户输入归类为三类之一：DELIVERY, ANALYSIS, OTHER。\n" +
                        "DELIVERY：用户主动请求找工作、搜索岗位、投递职位或请求为自己推荐岗位。关键词：找工作、招聘、投递、帮我推荐、帮我找、我要找、求职、应聘、工作机会\n" +
                        "ANALYSIS：用户询问自己的水平、竞争力、投递建议、匹配度或简历优化。关键词：水平、竞争力、匹配、建议、分析、评估、能力、简历、优化\n" +
                        "OTHER：其他问题，包括：询问市场上热门岗位/行业趋势（如\"什么岗位热门\"）、面试题、技术知识、代码问题、杂项问答等。\n" +
                        "规则：\n" +
                        "1. 如果用户只是询问\"什么岗位热门\"、\"哪些岗位吃香\"等市场趋势问题，不是请求推荐给自己，则归类为OTHER\n" +
                        "2. 只有用户明确请求为自己推荐岗位时才归类为DELIVERY\n" +
                        "3. 必须严格返回 JSON 格式\n" +
                        "4. 只返回一个 JSON 对象\n" +
                        "5. 不要输出任何其他文本或解释\n" +
                        "6. category 值只能是 DELIVERY、ANALYSIS 或 OTHER\n" +
                        "输出格式示例：{\"category\": \"OTHER\", \"content\": \"用户原始输入内容\"}";

                String user = request.message();
                String model = (request.model() != null && !request.model().isBlank()) ? request.model() : aiConfig.getModelForUserAgent();

                String assistantText = aiService.chatWithTemperature(system, user, model, 150, 0.0);

                int s = assistantText.indexOf('{');
                int e = assistantText.lastIndexOf('}');
                jsonOut = (s >= 0 && e > s) ? assistantText.substring(s, e + 1) : assistantText;

                JsonNode parsed = objectMapper.readTree(jsonOut);
                String cat = parsed.path("category").asText("");
                String content = parsed.path("content").asText("");
                if (cat == null || !(cat.equals("DELIVERY") || cat.equals("ANALYSIS") || cat.equals("OTHER"))) {
                    String lower = user.toLowerCase();
                    // DELIVERY: 用户主动请求找工作、投递或请求推荐给自己
                    boolean isDelivery = lower.contains("找工作") || lower.contains("招聘") || lower.contains("投递") || 
                                         lower.contains("帮我推荐") || lower.contains("帮我找") || lower.contains("我要找") ||
                                         lower.contains("求职") || lower.contains("应聘") || lower.contains("工作机会") ||
                                         (lower.contains("推荐") && lower.contains("我"));
                    
                    // ANALYSIS: 用户询问自己的水平、竞争力等
                    boolean isAnalysis = lower.contains("水平") || lower.contains("竞争力") || lower.contains("匹配") || 
                                         lower.contains("建议") || lower.contains("分析") || lower.contains("评估") ||
                                         lower.contains("能力") || lower.contains("简历") || lower.contains("优化");
                    
                    if (isDelivery) {
                        cat = "DELIVERY";
                    } else if (isAnalysis) {
                        cat = "ANALYSIS";
                    } else {
                        // OTHER: 包括询问热门岗位、技术问题、面试题等
                        cat = "OTHER";
                    }
                    var outNode = objectMapper.createObjectNode();
                    outNode.put("category", cat);
                    outNode.put("content", (content == null || content.isBlank()) ? user : content);
                    jsonOut = objectMapper.writeValueAsString(outNode);
                }

            } catch (Exception ex) {
                // AI返回格式错误，使用关键词匹配进行fallback分类
                String lower = request.message().toLowerCase();
                String cat;
                // DELIVERY: 用户主动请求找工作、投递或请求推荐给自己
                boolean isDelivery = lower.contains("找工作") || lower.contains("招聘") || lower.contains("投递") || 
                                     lower.contains("帮我推荐") || lower.contains("帮我找") || lower.contains("我要找") ||
                                     lower.contains("求职") || lower.contains("应聘") || lower.contains("工作机会") ||
                                     (lower.contains("推荐") && lower.contains("我"));
                
                // ANALYSIS: 用户询问自己的水平、竞争力等
                boolean isAnalysis = lower.contains("水平") || lower.contains("竞争力") || lower.contains("匹配") || 
                                     lower.contains("建议") || lower.contains("分析") || lower.contains("评估") ||
                                     lower.contains("能力") || lower.contains("简历") || lower.contains("优化");
                
                if (isDelivery) {
                    cat = "DELIVERY";
                } else if (isAnalysis) {
                    cat = "ANALYSIS";
                } else {
                    // OTHER: 包括询问热门岗位、技术问题、面试题等
                    cat = "OTHER";
                }
                try {
                    var outNode = objectMapper.createObjectNode();
                    outNode.put("category", cat);
                    outNode.put("content", request.message());
                    jsonOut = objectMapper.writeValueAsString(outNode);
                } catch (Exception e) {
                    jsonOut = "{\"category\":\"OTHER\",\"content\":\"" + request.message().replaceAll("\"","\\\"") + "\"}";
                }
            }

            try {
                JsonNode parsed2 = objectMapper.readTree(jsonOut);
                String finalCat = parsed2.path("category").asText("");
                String finalContent = parsed2.path("content").asText("");
                if ("DELIVERY".equals(finalCat)) {
                    AgentRequest prefReq = new AgentRequest(request.userId(), AgentCategory.PREFERENCE, (finalContent == null || finalContent.isBlank()) ? request.message() : finalContent, request.model(), request.sessionId());
                    return preferenceAgentService.analyze(prefReq);
                } else if ("ANALYSIS".equals(finalCat)) {
                    AgentRequest adviceReq = new AgentRequest(request.userId(), AgentCategory.ADVICE, (finalContent == null || finalContent.isBlank()) ? request.message() : finalContent, request.model(), request.sessionId());
                    return adviceAgentService.suggest(adviceReq);
                } else if ("OTHER".equals(finalCat)) {
                    AgentRequest commonReq = new AgentRequest(request.userId(), AgentCategory.COMMON, (finalContent == null || finalContent.isBlank()) ? request.message() : finalContent, request.model(), request.sessionId());
                    return commonAgentService.ask(commonReq);
                }
            } catch (Exception ignore) {
            }

            MessageDto msg = new MessageDto("assistant", jsonOut);
            return new AgentResponse(List.of(msg), List.of(), null);
        }

        if (request.category() == AgentCategory.PREFERENCE) {
            return preferenceAgentService.analyze(request);
        } else if (request.category() == AgentCategory.ADVICE) {
            return adviceAgentService.suggest(request);
        } else {
            return commonAgentService.ask(request);
        }
    }
}