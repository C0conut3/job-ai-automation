package com.jobai.automation.agent.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobai.automation.agent.dto.AgentRequest;
import com.jobai.automation.agent.dto.AgentResponse;
import com.jobai.automation.agent.dto.JobSummaryDto;
import com.jobai.automation.agent.dto.AnalysisResultDto;
import com.jobai.automation.agent.dto.MessageDto;
import com.jobai.automation.agent.service.PreferenceAgentService;
import com.jobai.automation.config.AiConfig;
import com.jobai.automation.job.domain.Job;
import com.jobai.automation.job.domain.JobCategory;
import com.jobai.automation.job.domain.JobStatus;
import com.jobai.automation.job.repository.JobCategoryRepository;
import com.jobai.automation.job.repository.JobRepository;
import com.jobai.automation.resume.service.ResumeService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class PreferenceAgentServiceImpl implements PreferenceAgentService {

    private final JobCategoryRepository categoryRepository;
    private final JobRepository jobRepository;
    private final ResumeService resumeService;
    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public PreferenceAgentServiceImpl(JobCategoryRepository categoryRepository,
                                       JobRepository jobRepository,
                                       ResumeService resumeService,
                                       AiConfig aiConfig) {
        this.categoryRepository = categoryRepository;
        this.jobRepository = jobRepository;
        this.resumeService = resumeService;
        this.aiConfig = aiConfig;
    }

    @Transactional(readOnly = true)
    @Override
    public AgentResponse analyze(AgentRequest request) {
        String userInput = request.message() == null ? "" : request.message().trim();
        try {
            // 1. load supported categories
            List<JobCategory> categories = categoryRepository.findAllByOrderBySortOrderAscIdAsc();
            if (categories.isEmpty()) {
                return new AgentResponse(List.of(), List.of(), new AnalysisResultDto("no-categories","系统中没有可用的岗位类别"));
            }

            // build name list
            List<String> categoryNames = new ArrayList<>();
            for (JobCategory c : categories) categoryNames.add(c.getName());

            // 2. try extract category from user input
            String chosenCategory = null;
            String lower = userInput.toLowerCase(Locale.ROOT);
            for (String name : categoryNames) {
                if (name == null) continue;
                if (lower.contains(name.toLowerCase(Locale.ROOT))) {
                    chosenCategory = name;
                    break;
                }
            }

            // 3. if not found in user input directly, try to map user's preference to system categories using AI
            // This respects user's input preference first before falling back to resume analysis
            String model = (request.model() != null && !request.model().isBlank()) ? request.model() : aiConfig.getModelForPreferenceAgent();
            String url = aiConfig.getBaseUrl(); if (!url.endsWith("/")) url = url + "/"; url = url + "v1/chat/completions";
            
            if (chosenCategory == null && !userInput.isBlank()) {
                // User has input but it doesn't match system categories directly
                // Ask AI to map user's preference to system categories
                String system = "你是一个岗位分类助手。下面给出系统支持的岗位类别列表，请根据用户的输入选择最匹配的一项并只返回该类别名称（完全相同，区分大小写不敏感）。\n可选类别：" + String.join(", ", categoryNames) + ".\n回复必须为纯文本，直接返回类别名称，不要解释。";
                String user = "用户输入：" + userInput + "\n\n请从上面的可选类别中选择最匹配的一项。";

                var root = objectMapper.createObjectNode();
                root.put("model", model);
                root.put("max_tokens", 60);
                var messages = objectMapper.createArrayNode();
                var m1 = objectMapper.createObjectNode(); m1.put("role","system"); m1.put("content", system);
                var m2 = objectMapper.createObjectNode(); m2.put("role","user"); m2.put("content", user);
                messages.add(m1); messages.add(m2);
                root.set("messages", messages); root.put("temperature", 0);
                String body = objectMapper.writeValueAsString(root);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(20))
                        .header("Content-Type","application/json; charset=utf-8")
                        .header("Authorization","Bearer " + aiConfig.getApiKey())
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> resp = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (resp.statusCode() / 100 != 2) throw new RuntimeException("AI error: " + resp.statusCode() + " " + resp.body());
                JsonNode resJson = objectMapper.readTree(resp.body());
                String reply = "";
                var choices = resJson.path("choices");
                if (choices.isArray() && choices.size() > 0) reply = choices.get(0).path("message").path("content").asText("");
                if (reply != null) {
                    String rLower = reply.toLowerCase(Locale.ROOT);
                    for (String name : categoryNames) {
                        if (rLower.contains(name.toLowerCase(Locale.ROOT))) { chosenCategory = name; break; }
                    }
                }
            }

            // 4. if still not found, analyze user's resume as fallback
            if (chosenCategory == null) {
                Long userId = null;
                try { userId = request.userId() == null || request.userId().isBlank() ? null : Long.parseLong(request.userId()); } catch (Exception ignored) {}
                String resumeText = "";
                if (userId != null) {
                    try {
                        var resume = resumeService.getMyResume(userId);
                        if (resume != null) {
                            StringBuilder sb = new StringBuilder();
                            if (resume.personalInfo() != null) sb.append(resume.personalInfo()).append("\n");
                            if (resume.education() != null) sb.append(resume.education()).append("\n");
                            if (resume.workExperience() != null) sb.append(resume.workExperience()).append("\n");
                            if (resume.projectExperience() != null) sb.append(resume.projectExperience()).append("\n");
                            resumeText = sb.toString();
                        }
                    } catch (Exception ignored) {}
                }

                if (resumeText.isBlank()) {
                    // no resume and no category: return helpful prompt
                    return new AgentResponse(List.of(new MessageDto("assistant", "请先上传或完善简历，以便进行岗位推荐。")), List.of(), new AnalysisResultDto("no-resume","请上传或完善简历后重试"));
                }

                // Try to infer category from resume
                String system = "你是一个岗位分类助手。下面给出系统支持的岗位类别列表，请仅从中选择最匹配的一项并只返回该类别名称（完全相同，区分大小写不敏感）。\n可选类别：" + String.join(", ", categoryNames) + ".\n回复必须为纯文本，直接返回类别名称，不要解释。";
                String user = "请根据以下简历内容判断最匹配的岗位类别（必须从上面的列表中选择）:\n" + resumeText;

                var root = objectMapper.createObjectNode();
                root.put("model", model);
                root.put("max_tokens", 60);
                var messages = objectMapper.createArrayNode();
                var m1 = objectMapper.createObjectNode(); m1.put("role","system"); m1.put("content", system);
                var m2 = objectMapper.createObjectNode(); m2.put("role","user"); m2.put("content", user);
                messages.add(m1); messages.add(m2);
                root.set("messages", messages); root.put("temperature", 0);
                String body = objectMapper.writeValueAsString(root);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(20))
                        .header("Content-Type","application/json; charset=utf-8")
                        .header("Authorization","Bearer " + aiConfig.getApiKey())
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> resp = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (resp.statusCode() / 100 != 2) throw new RuntimeException("AI error: " + resp.statusCode() + " " + resp.body());
                JsonNode resJson = objectMapper.readTree(resp.body());
                String reply = "";
                var choices = resJson.path("choices");
                if (choices.isArray() && choices.size() > 0) reply = choices.get(0).path("message").path("content").asText("");
                if (reply != null) {
                    // try match one category name within reply
                    String rLower = reply.toLowerCase(Locale.ROOT);
                    for (String name : categoryNames) {
                        if (rLower.contains(name.toLowerCase(Locale.ROOT))) { chosenCategory = name; break; }
                    }
                }

                // If category still null, perform a resume competitiveness analysis and return it
                if (chosenCategory == null) {
                    try {
                        String sys2 = "你是一个简历分析师。根据用户提供的简历内容和用户问题，给出简洁的总结（summary）和详细分析（detail），主要评估该候选人的竞争力、优劣势及可改进点。回复必须返回一个 JSON 对象：{\"summary\": \"...\", \"detail\": \"...\"}，不要返回其他文本。";
                        String user2 = "用户问题：" + (userInput == null || userInput.isBlank() ? "请对简历进行评估" : userInput) + "\n\n简历内容：" + resumeText;

                        var root2 = objectMapper.createObjectNode();
                        root2.put("model", model);
                        root2.put("max_tokens", 800);
                        var messages2 = objectMapper.createArrayNode();
                        var mm1 = objectMapper.createObjectNode(); mm1.put("role","system"); mm1.put("content", sys2);
                        var mm2 = objectMapper.createObjectNode(); mm2.put("role","user"); mm2.put("content", user2);
                        messages2.add(mm1); messages2.add(mm2);
                        root2.set("messages", messages2); root2.put("temperature", 0);
                        String body2 = objectMapper.writeValueAsString(root2);

                        HttpRequest req2 = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .timeout(Duration.ofSeconds(40))
                                .header("Content-Type","application/json; charset=utf-8")
                                .header("Authorization","Bearer " + aiConfig.getApiKey())
                                .POST(HttpRequest.BodyPublishers.ofString(body2, StandardCharsets.UTF_8))
                                .build();

                        HttpResponse<String> r2 = httpClient.send(req2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                        if (r2.statusCode() / 100 != 2) throw new RuntimeException("AI error: " + r2.statusCode() + " " + r2.body());
                        String reply2 = "";
                        JsonNode j2 = objectMapper.readTree(r2.body());
                        var ch2 = j2.path("choices");
                        if (ch2.isArray() && ch2.size() > 0) reply2 = ch2.get(0).path("message").path("content").asText("");

                        // try extract JSON
                        int ss = reply2.indexOf('{');
                        int ee = reply2.lastIndexOf('}');
                        String json = (ss >=0 && ee > ss) ? reply2.substring(ss, ee+1) : reply2;
                        String summary = "";
                        String detail = reply2;
                        try {
                            JsonNode parsed = objectMapper.readTree(json);
                            summary = parsed.path("summary").asText("");
                            detail = parsed.path("detail").asText(detail);
                        } catch (Exception ex) {
                            // fallback: put whole reply in detail
                            summary = "简历分析结果";
                            detail = reply2;
                        }

                        MessageDto info = new MessageDto("assistant", "已完成简历分析，以下是评估结果：");
                        return new AgentResponse(List.of(info), List.of(), new AnalysisResultDto(summary, detail));

                    } catch (Exception ex) {
                        return new AgentResponse(List.of(new MessageDto("assistant", "无法从输入或简历中推断出合适的岗位类别，请尝试提供更明确的偏好信息。")), List.of(), new AnalysisResultDto("error", ex.getMessage()));
                    }
                }
            }

            // 5. ensure we have chosenCategory
            if (chosenCategory == null) {
                return new AgentResponse(List.of(new MessageDto("assistant", "未找到匹配的岗位类别，请尝试其他关键词。")), List.of(), new AnalysisResultDto("no-match","未能匹配类别"));
            }

            // 6. find JobCategory entity
            JobCategory selected = null;
            for (JobCategory c : categoryRepository.findAllByOrderBySortOrderAscIdAsc()) {
                if (c.getName() != null && c.getName().equalsIgnoreCase(chosenCategory)) { selected = c; break; }
            }
            if (selected == null) {
                return new AgentResponse(List.of(new MessageDto("assistant", "匹配到的类别不在系统中，请选择其他类别。")), List.of(), new AnalysisResultDto("no-category","匹配类别不在系统中"));
            }

            Long selectedCategoryId = selected.getId();

            // 7. query up to 10 jobs in this category that are PUBLISHED
            Specification<Job> spec = (root, query, cb) -> {
                List<Predicate> preds = new ArrayList<>();
                preds.add(cb.equal(root.get("status"), JobStatus.PUBLISHED));
                preds.add(cb.equal(root.get("category").get("id"), selectedCategoryId));
                return cb.and(preds.toArray(new Predicate[0]));
            };
            var page = jobRepository.findAll(spec, PageRequest.of(0,10));
            List<JobSummaryDto> jobs = new ArrayList<>();
            for (Job j : page.getContent()) {
                String categoryName = j.getCategory() != null ? j.getCategory().getName() : null;
                jobs.add(new JobSummaryDto(j.getId(), j.getTitle(), j.getCompanyName(), categoryName, j.getDescription() == null ? "" : j.getDescription()));
            }

            var analysis = new AnalysisResultDto(chosenCategory, "根据输入/简历推断的类别：" + chosenCategory);
            // return AgentResponse with friendly message instead of JSON
            MessageDto info = new MessageDto("assistant", "我现在为您推荐" + chosenCategory + "相关的岗位，以下是匹配度较高的职位：");
            return new AgentResponse(List.of(info), jobs, analysis);

        } catch (Exception ex) {
            return new AgentResponse(List.of(new MessageDto("assistant", "操作过程中出现错误：" + ex.getMessage())), List.of(), new AnalysisResultDto("error", ex.getMessage()));
        }
    }
}
