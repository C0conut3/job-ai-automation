package com.jobai.automation.agent.dto; // moved to agent package directory

import java.util.List;

/**
 * 统一 Agent 返回格式�?
 * - conversation: 对话内容（多�?role/content�?
 * - jobs: 推荐的岗位列表（摘要�?
 * - analysis: 结构化分析结�?
 */
public record AgentResponse(
        List<MessageDto> conversation,
        List<JobSummaryDto> jobs,
        AnalysisResultDto analysis
) {}
