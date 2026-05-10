package com.jobai.automation.agent.service; // moved to agent package directory

import com.jobai.automation.agent.dto.AgentRequest;
import com.jobai.automation.agent.dto.AgentResponse;

public interface AdviceAgentService {

    /**
     * 根据用户简历和岗位/投递历史，给出投递建议或匹配岗位
     */
    AgentResponse suggest(AgentRequest request);
}
