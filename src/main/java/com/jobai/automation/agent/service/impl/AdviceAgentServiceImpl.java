package com.jobai.automation.agent.service.impl;

import com.jobai.automation.agent.dto.AgentRequest;
import com.jobai.automation.agent.dto.AgentResponse;
import com.jobai.automation.agent.dto.MessageDto;
import com.jobai.automation.agent.service.AdviceAgentService;
import com.jobai.automation.config.AiConfig;
import com.jobai.automation.resume.service.ResumeService;
import com.jobai.automation.service.AiService;
import org.springframework.stereotype.Service;

@Service
public class AdviceAgentServiceImpl implements AdviceAgentService {

    private final ResumeService resumeService;
    private final AiConfig aiConfig;
    private final AiService aiService;

    public AdviceAgentServiceImpl(ResumeService resumeService, AiConfig aiConfig, AiService aiService) {
        this.resumeService = resumeService;
        this.aiConfig = aiConfig;
        this.aiService = aiService;
    }

    @Override
    public AgentResponse suggest(AgentRequest request) {
        String userInput = request.message() == null ? "" : request.message().trim();

        Long userId = null;
        try {
            userId = request.userId() == null || request.userId().isBlank()
                    ? null : Long.parseLong(request.userId());
        } catch (Exception ignored) {}

        String resumeText = "";
        if (userId != null) {
            try {
                var resume = resumeService.getMyResume(userId);
                if (resume != null) {
                    StringBuilder sb = new StringBuilder();
                    if (resume.personalInfo() != null) sb.append("【个人信息】\n").append(resume.personalInfo()).append("\n\n");
                    if (resume.education() != null) sb.append("【教育背景】\n").append(resume.education()).append("\n\n");
                    if (resume.workExperience() != null) sb.append("【工作经历】\n").append(resume.workExperience()).append("\n\n");
                    if (resume.projectExperience() != null) sb.append("【项目经验】\n").append(resume.projectExperience()).append("\n\n");
                    resumeText = sb.toString();
                }
            } catch (Exception ignored) {}
        }

        if (resumeText.isBlank()) {
            String noResumeAdvice = "📋 **简历竞争力分析建议**\n\n" +
                    "您好！我注意到您还没有上传或完善简历。\n\n" +
                    "要进行竞争力分析和建议，我需要先了解您的简历信息。\n\n" +
                    "👉 **下一步操作**：\n" +
                    "请先在简历模块上传或填写您的简历，然后再向我询问竞争力分析、投递建议等问题。\n\n" +
                    "期待了解您的背景后，为您提供更精准的建议！";
            return new AgentResponse(
                    java.util.List.of(new MessageDto("assistant", noResumeAdvice)),
                    java.util.List.of(),
                    null
            );
        }

        try {
            String model = (request.model() != null && !request.model().isBlank())
                    ? request.model() : aiConfig.getModelForAdviceAgent();

            String systemPrompt = "你是一位专业的职业发展顾问和简历分析师。请根据用户的简历和输入问题，提供专业、详细且可操作的职业建议。\n\n" +
                    "请按以下结构返回分析报告（使用 Markdown 格式）：\n\n" +
                    "## 一、用户当前水平总结\n" +
                    "[基于简历分析用户的技能水平、工作年限、核心技术能力等]\n\n" +
                    "## 二、当前就业行情简要\n" +
                    "[分析目标岗位/方向的市场需求、薪资范围、竞争程度等]\n\n" +
                    "## 三、竞争力分析\n" +
                    "[与同龄人或同岗位候选人相比的优势与劣势]\n\n" +
                    "## 四、三条提升建议\n" +
                    "1. [具体可操作的技能提升建议]\n" +
                    "2. [具体可操作的经验提升建议]\n" +
                    "3. [具体可操作的简历/面试提升建议]\n\n" +
                    "## 五、投递策略建议\n" +
                    "[目标公司类型、投递时机、注意事项等]\n\n" +
                    "请用专业但友好的语气回复，语言简洁有条理，重点突出可执行性。";

            String userPrompt = "用户问题：" + (userInput.isBlank() ? "请对我的简历进行全面的竞争力分析和发展建议" : userInput) + "\n\n" +
                    "【用户简历】\n" + resumeText;

            String adviceContent = aiService.chatWithTemperature(systemPrompt, userPrompt, model, 2000, 0.7);

            if (adviceContent == null || adviceContent.isBlank()) {
                adviceContent = "抱歉，暂时无法生成分析建议，请稍后重试。";
            }

            return new AgentResponse(
                    java.util.List.of(new MessageDto("assistant", adviceContent)),
                    java.util.List.of(),
                    null
            );

        } catch (Exception ex) {
            String errorAdvice = "⚠️ 生成建议时遇到问题：\n\n" + ex.getMessage() + "\n\n请稍后重试，或尝试简化您的问题。";
            return new AgentResponse(
                    java.util.List.of(new MessageDto("assistant", errorAdvice)),
                    java.util.List.of(),
                    null
            );
        }
    }
}