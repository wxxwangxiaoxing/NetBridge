package com.netbridge.module.agent.controller;

import com.netbridge.framework.web.api.ApiResponse;
import com.netbridge.module.agent.api.dto.AgentRegisterResponse;
import com.netbridge.module.agent.api.dto.AgentTaskResponse;
import com.netbridge.module.agent.api.request.AgentHeartbeatRequest;
import com.netbridge.module.agent.api.request.AgentLogRequest;
import com.netbridge.module.agent.api.request.AgentRegisterRequest;
import com.netbridge.module.agent.api.request.AgentTaskPullRequest;
import com.netbridge.module.agent.api.request.AgentTaskResultRequest;
import com.netbridge.module.agent.service.AgentOnboardingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentOnboardingService agentOnboardingService;

    public AgentController(AgentOnboardingService agentOnboardingService) {
        this.agentOnboardingService = agentOnboardingService;
    }

    @PostMapping("/register")
    public ApiResponse<AgentRegisterResponse> register(@Valid @RequestBody AgentRegisterRequest request) {
        return ApiResponse.success(agentOnboardingService.register(request));
    }

    @PostMapping("/heartbeat")
    public ApiResponse<Boolean> heartbeat(@Valid @RequestBody AgentHeartbeatRequest request) {
        agentOnboardingService.heartbeat(request);
        return ApiResponse.success(true);
    }

    @PostMapping("/task")
    public ApiResponse<AgentTaskResponse> pullTask(@Valid @RequestBody AgentTaskPullRequest request) {
        return ApiResponse.success(agentOnboardingService.pullTask(request));
    }

    @PostMapping("/task/{id}/result")
    public ApiResponse<Boolean> reportTaskResult(@PathVariable Long id, @Valid @RequestBody AgentTaskResultRequest request) {
        agentOnboardingService.reportTaskResult(id, request);
        return ApiResponse.success(true);
    }

    @PostMapping("/log")
    public ApiResponse<Boolean> reportLog(@Valid @RequestBody AgentLogRequest request) {
        agentOnboardingService.reportLog(request);
        return ApiResponse.success(true);
    }
}
