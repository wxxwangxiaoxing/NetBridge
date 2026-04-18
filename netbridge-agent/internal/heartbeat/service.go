package heartbeat

import (
	"netbridge-agent/internal/config"
	"netbridge-agent/pkg/model"
)

func BuildRequest(cfg *config.Config, state model.AgentState, metrics model.MetricsSnapshot) model.HeartbeatRequest {
	return model.HeartbeatRequest{
		AgentID:         state.AgentID,
		ServerID:        state.ServerID,
		Status:          cfg.HeartbeatStatus,
		TailscaleIP:     cfg.TailscaleIP,
		CPUUsage:        metrics.CPUUsage,
		MemoryUsage:     metrics.MemoryUsage,
		DiskUsage:       metrics.DiskUsage,
		NetworkInBytes:  metrics.NetworkInBytes,
		NetworkOutBytes: metrics.NetworkOutBytes,
		LoadAverage:     metrics.LoadAverage,
	}
}
