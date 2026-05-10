package com.jobai.automation.agent.dto; // moved to agent package directory

public record AnalysisResultDto(
        String summary,
        String detail // TODO: 可扩展为更丰富的结构（例如技能向量、匹配度等）
) {}
