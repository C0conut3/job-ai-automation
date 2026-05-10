package com.jobai.automation.agent.dto; // moved to agent package directory

import com.jobai.automation.agent.constant.AgentCategory;

public record AgentRequest(
        String userId,
        AgentCategory category,
        String message,
        String model // 可选：覆盖默认模型，例如用于优化或解析
) {}
