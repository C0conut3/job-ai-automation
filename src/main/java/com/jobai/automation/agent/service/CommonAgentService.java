package com.jobai.automation.agent.service; // moved to agent package directory

import com.jobai.automation.agent.dto.AgentRequest;
import com.jobai.automation.agent.dto.AgentResponse;

public interface CommonAgentService {

    /**
     * 通用问答，用于回答用户与简�?求职相关的自然语言问题
     */
    AgentResponse ask(AgentRequest request);
}
