package com.jobai.automation.agent.service; // moved to agent package directory

import com.jobai.automation.agent.dto.AgentRequest;
import com.jobai.automation.agent.dto.AgentResponse;

public interface PreferenceAgentService {

    /**
     * 对用户简�?行为进行偏好分析，返回统一 AgentResponse
     */
    AgentResponse analyze(AgentRequest request);
}
