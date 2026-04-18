package executor

import (
	"context"
	"fmt"
	"net"
	"time"

	"netbridge-agent/internal/monitor"
	svcctl "netbridge-agent/internal/service"
	"netbridge-agent/internal/tailscale"
	"netbridge-agent/internal/tunnel"
	"netbridge-agent/pkg/constant"
	"netbridge-agent/pkg/model"
)

type Executor struct {
	serviceManager *svcctl.Manager
	tailscale      *tailscale.Client
	tunnel         *tunnel.Controller
}

func New(serviceManager *svcctl.Manager, tailscaleClient *tailscale.Client, tunnelController *tunnel.Controller) *Executor {
	return &Executor{
		serviceManager: serviceManager,
		tailscale:      tailscaleClient,
		tunnel:         tunnelController,
	}
}

func (e *Executor) Execute(ctx context.Context, task *model.PullTaskResponse) (model.TaskResult, string) {
	switch task.Type {
	case "CHECK":
		endpoint := monitor.Endpoint(task.Payload.TargetHost, task.Payload.Port)
		serviceOp, _ := e.serviceManager.Observe(ctx, task.Payload.ServiceName)
		portState := portStatus(task.Payload.TargetHost, task.Payload.Port)
		tunnelOp := e.tunnel.Observe(ctx)
		tailscaleOp := e.tailscale.Observe(ctx)
		success := serviceOp.Status == "active" || portState == "open"
		return model.TaskResult{
			Success:         success,
			Endpoint:        endpoint,
			Message:         fmt.Sprintf("check executed: service=%s port=%s", serviceOp.Status, portState),
			ServiceStatus:   serviceOp.Status,
			PortStatus:      portState,
			TunnelStatus:    tunnelOp.Status,
			TailscaleStatus: tailscaleOp.Status,
			ServiceAction:   serviceOp,
			TunnelAction:    tunnelOp,
			TailscaleAction: tailscaleOp,
			ResponseTimeMs:  1,
			SuccessCount:    boolCount(success),
			ErrorCount:      boolCount(!success),
			CheckedAt:       time.Now().Format(time.RFC3339),
		}, statusFromBool(success)
	case "START":
		serviceOp, err := e.serviceManager.StartWithResult(ctx, task.Payload.ServiceName)
		if err != nil {
			result := failedResult(err)
			result.ServiceAction = serviceOp
			return result, constant.StatusFailed
		}
		tailscaleOp, tunnelOp, rollbackApplied, err := e.applyAccessStart(ctx, task.Payload)
		if err != nil {
			serviceOp, serviceStatus, serviceRollbackApplied := rollbackServiceAfterStartFailure(ctx, e.serviceManager, task.Payload.ServiceName, err)
			result := failedResult(err)
			result.ServiceStatus = serviceStatus
			result.ServiceAction = serviceOp
			result.TailscaleAction = tailscaleOp
			result.TunnelAction = tunnelOp
			result.RollbackApplied = rollbackApplied || serviceRollbackApplied
			result.TailscaleStatus = e.tailscale.Status(ctx)
			result.TunnelStatus = e.tunnel.Status(ctx)
			return result, constant.StatusFailed
		}
		accessURL := accessURL(task.Payload)
		serviceStatus := postActionServiceStatus(ctx, e.serviceManager, task.Payload.ServiceName)
		serviceOp.Status = serviceStatus
		return model.TaskResult{
			Success:         true,
			AccessURL:       accessURL,
			Endpoint:        accessURL,
			Message:         fmt.Sprintf("%s executed via service manager for accessType=%s", task.Type, task.Payload.AccessType),
			ServiceStatus:   serviceStatus,
			ServiceAction:   serviceOp,
			TailscaleAction: tailscaleOp,
			TunnelAction:    tunnelOp,
			TailscaleStatus: e.tailscale.Status(ctx),
			TunnelStatus:    e.tunnel.Status(ctx),
		}, constant.StatusSuccess
	case "STOP":
		serviceOp, err := e.serviceManager.StopWithResult(ctx, task.Payload.ServiceName)
		if err != nil {
			result := failedResult(err)
			result.ServiceAction = serviceOp
			return result, constant.StatusFailed
		}
		tailscaleOp, tunnelOp, rollbackApplied, err := e.applyAccessStop(ctx, task.Payload)
		if err != nil {
			serviceOp, serviceStatus, serviceRollbackApplied := rollbackServiceAfterStopFailure(ctx, e.serviceManager, task.Payload.ServiceName, err)
			result := failedResult(err)
			result.ServiceStatus = serviceStatus
			result.ServiceAction = serviceOp
			result.TailscaleAction = tailscaleOp
			result.TunnelAction = tunnelOp
			result.RollbackApplied = rollbackApplied || serviceRollbackApplied
			result.TailscaleStatus = e.tailscale.Status(ctx)
			result.TunnelStatus = e.tunnel.Status(ctx)
			return result, constant.StatusFailed
		}
		accessURL := accessURL(task.Payload)
		serviceStatus := postActionServiceStatus(ctx, e.serviceManager, task.Payload.ServiceName)
		serviceOp.Status = serviceStatus
		return model.TaskResult{
			Success:         true,
			AccessURL:       accessURL,
			Endpoint:        accessURL,
			Message:         fmt.Sprintf("%s executed via service manager for accessType=%s", task.Type, task.Payload.AccessType),
			ServiceStatus:   serviceStatus,
			ServiceAction:   serviceOp,
			TailscaleAction: tailscaleOp,
			TunnelAction:    tunnelOp,
			RollbackApplied: rollbackApplied,
			TailscaleStatus: e.tailscale.Status(ctx),
			TunnelStatus:    e.tunnel.Status(ctx),
		}, constant.StatusSuccess
	case "RESTART":
		serviceOp, err := e.serviceManager.RestartWithResult(ctx, task.Payload.ServiceName)
		if err != nil {
			result := failedResult(err)
			result.ServiceAction = serviceOp
			return result, constant.StatusFailed
		}
		tailscaleOp, tunnelOp, rollbackApplied, err := e.applyAccessStart(ctx, task.Payload)
		if err != nil {
			serviceOp, serviceStatus, serviceRollbackApplied := rollbackServiceAfterStartFailure(ctx, e.serviceManager, task.Payload.ServiceName, err)
			result := failedResult(err)
			result.ServiceStatus = serviceStatus
			result.ServiceAction = serviceOp
			result.TailscaleAction = tailscaleOp
			result.TunnelAction = tunnelOp
			result.RollbackApplied = rollbackApplied || serviceRollbackApplied
			result.TailscaleStatus = e.tailscale.Status(ctx)
			result.TunnelStatus = e.tunnel.Status(ctx)
			return result, constant.StatusFailed
		}
		accessURL := accessURL(task.Payload)
		serviceStatus := postActionServiceStatus(ctx, e.serviceManager, task.Payload.ServiceName)
		serviceOp.Status = serviceStatus
		return model.TaskResult{
			Success:         true,
			AccessURL:       accessURL,
			Endpoint:        accessURL,
			Message:         fmt.Sprintf("%s executed via service manager for accessType=%s", task.Type, task.Payload.AccessType),
			ServiceStatus:   serviceStatus,
			ServiceAction:   serviceOp,
			TailscaleAction: tailscaleOp,
			TunnelAction:    tunnelOp,
			TailscaleStatus: e.tailscale.Status(ctx),
			TunnelStatus:    e.tunnel.Status(ctx),
		}, constant.StatusSuccess
	default:
		return failedResult(fmt.Errorf("unsupported task type")), constant.StatusFailed
	}
}

func (e *Executor) applyAccessStart(ctx context.Context, payload model.TaskPayload) (model.AccessOperation, model.AccessOperation, bool, error) {
	tailscaleOp, err := e.tailscale.EnsureAccessWithResult(ctx, payload)
	if err != nil {
		return tailscaleOp, model.AccessOperation{}, false, err
	}
	tunnelOp, err := e.tunnel.EnsureAccessWithResult(ctx, payload)
	if err != nil {
		rollbackOp, rollbackErr := e.tailscale.TeardownAccessWithResult(ctx, payload)
		if rollbackErr != nil {
			tailscaleOp.Detail = tailscaleOp.Detail + "; rollback failed: " + rollbackErr.Error()
			return tailscaleOp, tunnelOp, false, err
		}
		tailscaleOp = rollbackOp
		return tailscaleOp, tunnelOp, true, err
	}
	return tailscaleOp, tunnelOp, false, nil
}

func (e *Executor) applyAccessStop(ctx context.Context, payload model.TaskPayload) (model.AccessOperation, model.AccessOperation, bool, error) {
	tailscaleOp, err := e.tailscale.TeardownAccessWithResult(ctx, payload)
	if err != nil {
		return tailscaleOp, model.AccessOperation{}, false, err
	}
	tunnelOp, err := e.tunnel.TeardownAccessWithResult(ctx, payload)
	if err != nil {
		rollbackOp, rollbackErr := e.tailscale.EnsureAccessWithResult(ctx, payload)
		if rollbackErr != nil {
			tailscaleOp.Detail = tailscaleOp.Detail + "; rollback failed: " + rollbackErr.Error()
			return tailscaleOp, tunnelOp, false, err
		}
		tailscaleOp = rollbackOp
		return tailscaleOp, tunnelOp, true, err
	}
	return tailscaleOp, tunnelOp, false, nil
}

func failedResult(err error) model.TaskResult {
	return model.TaskResult{
		Success: false,
		Message: err.Error(),
	}
}

func portStatus(host string, port int) string {
	if host == "" || port <= 0 {
		return "unknown"
	}
	conn, err := net.DialTimeout("tcp", monitor.Endpoint(host, port), 2*time.Second)
	if err != nil {
		return "closed"
	}
	_ = conn.Close()
	return "open"
}

func statusFromBool(ok bool) string {
	if ok {
		return constant.StatusSuccess
	}
	return constant.StatusFailed
}

func boolCount(ok bool) int64 {
	if ok {
		return 1
	}
	return 0
}

func accessURL(payload model.TaskPayload) string {
	if payload.Domain != "" {
		protocol := payload.Protocol
		if protocol == "" {
			protocol = "http"
		}
		return fmt.Sprintf("%s://%s", protocol, payload.Domain)
	}
	return monitor.Endpoint(payload.TargetHost, payload.Port)
}

func postActionServiceStatus(ctx context.Context, manager *svcctl.Manager, serviceName string) string {
	op, err := manager.Observe(ctx, serviceName)
	if err != nil || op.Status == "" {
		return "unknown"
	}
	return op.Status
}

func rollbackServiceAfterStartFailure(ctx context.Context, manager *svcctl.Manager, serviceName string, cause error) (model.AccessOperation, string, bool) {
	if err := manager.Stop(ctx, serviceName); err != nil {
		status := postActionServiceStatus(ctx, manager, serviceName)
		return model.AccessOperation{
			Component: "service",
			Action:    "rollback-stop",
			Status:    "failed",
			Detail:    fmt.Sprintf("access step failed (%v); service rollback stop failed: %v", cause, err),
		}, status, false
	}
	status := postActionServiceStatus(ctx, manager, serviceName)
	return model.AccessOperation{
		Component: "service",
		Action:    "rollback-stop",
		Status:    status,
		Detail:    fmt.Sprintf("access step failed (%v); service stopped as rollback", cause),
	}, status, true
}

func rollbackServiceAfterStopFailure(ctx context.Context, manager *svcctl.Manager, serviceName string, cause error) (model.AccessOperation, string, bool) {
	if err := manager.Start(ctx, serviceName); err != nil {
		status := postActionServiceStatus(ctx, manager, serviceName)
		return model.AccessOperation{
			Component: "service",
			Action:    "rollback-start",
			Status:    "failed",
			Detail:    fmt.Sprintf("access step failed (%v); service rollback start failed: %v", cause, err),
		}, status, false
	}
	status := postActionServiceStatus(ctx, manager, serviceName)
	return model.AccessOperation{
		Component: "service",
		Action:    "rollback-start",
		Status:    status,
		Detail:    fmt.Sprintf("access step failed (%v); service started as rollback", cause),
	}, status, true
}
