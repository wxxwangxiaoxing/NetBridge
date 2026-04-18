package com.netbridge.module.agent.service;

import com.netbridge.module.agent.api.dto.AgentTaskResponse;
import com.netbridge.module.agent.api.dto.AgentRegisterResponse;
import com.netbridge.module.agent.api.request.AgentHeartbeatRequest;
import com.netbridge.module.agent.api.request.AgentLogRequest;
import com.netbridge.module.agent.api.request.AgentRegisterRequest;
import com.netbridge.module.agent.api.request.AgentTaskPullRequest;
import com.netbridge.module.agent.api.request.AgentTaskResultRequest;

public interface AgentOnboardingService {

    AgentRegisterResponse register(AgentRegisterRequest request);

    void heartbeat(AgentHeartbeatRequest request);

    AgentTaskResponse pullTask(AgentTaskPullRequest request);

    void reportTaskResult(Long taskId, AgentTaskResultRequest request);

    void reportLog(AgentLogRequest request);
}
