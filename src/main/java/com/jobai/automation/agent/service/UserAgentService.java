package com.jobai.automation.agent.service; // moved to agent package directory

import com.jobai.automation.agent.dto.AgentRequest;
import com.jobai.automation.agent.dto.AgentResponse;

public interface UserAgentService {

    /**
     * 主控入口，根据请求分发到不同�?Agent
     */
    AgentResponse handle(AgentRequest request);
}
