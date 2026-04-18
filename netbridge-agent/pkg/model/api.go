package model

type APIResponse[T any] struct {
	Code    int    `json:"code"`
	Message string `json:"message"`
	Data    T      `json:"data"`
}

type AgentState struct {
	ServerID Long   `json:"serverId"`
	AgentID  string `json:"agentId"`
	Hostname string `json:"hostname"`
}

type Long = int64

type RegisterRequest struct {
	Token        string `json:"token"`
	Hostname     string `json:"hostname"`
	OS           string `json:"os"`
	IP           string `json:"ip"`
	AgentVersion string `json:"agentVersion"`
	CPU          string `json:"cpu"`
	Memory       string `json:"memory"`
	Disk         string `json:"disk"`
	Arch         string `json:"arch"`
}

type RegisterResponse struct {
	ServerID Long   `json:"serverId"`
	AgentID  string `json:"agentId"`
	Hostname string `json:"hostname"`
}

type HeartbeatRequest struct {
	AgentID         string  `json:"agentId"`
	ServerID        Long    `json:"serverId"`
	Status          string  `json:"status"`
	TailscaleIP     string  `json:"tailscaleIp,omitempty"`
	CPUUsage        float64 `json:"cpuUsage,omitempty"`
	MemoryUsage     float64 `json:"memoryUsage,omitempty"`
	DiskUsage       float64 `json:"diskUsage,omitempty"`
	NetworkInBytes  int64   `json:"networkInBytes,omitempty"`
	NetworkOutBytes int64   `json:"networkOutBytes,omitempty"`
	LoadAverage     float64 `json:"loadAverage,omitempty"`
}

type PullTaskRequest struct {
	AgentID  string `json:"agentId"`
	ServerID Long   `json:"serverId"`
}

type TaskPayload struct {
	ServiceID   Long   `json:"serviceId"`
	ServerID    Long   `json:"serverId"`
	Action      string `json:"action"`
	ServiceName string `json:"serviceName"`
	Port        int    `json:"port"`
	Protocol    string `json:"protocol"`
	AccessType  string `json:"accessType"`
	TargetHost  string `json:"targetHost"`
	Domain      string `json:"domain"`
}

type PullTaskResponse struct {
	ID      Long        `json:"id"`
	Type    string      `json:"type"`
	Payload TaskPayload `json:"payload"`
}

type TaskResult struct {
	Success          bool            `json:"success"`
	AccessURL        string          `json:"accessUrl,omitempty"`
	Endpoint         string          `json:"endpoint,omitempty"`
	Message          string          `json:"message,omitempty"`
	ServiceStatus    string          `json:"serviceStatus,omitempty"`
	PortStatus       string          `json:"portStatus,omitempty"`
	TunnelStatus     string          `json:"tunnelStatus,omitempty"`
	TailscaleStatus  string          `json:"tailscaleStatus,omitempty"`
	ServiceAction    AccessOperation `json:"serviceAction,omitempty"`
	TunnelAction     AccessOperation `json:"tunnelAction,omitempty"`
	TailscaleAction  AccessOperation `json:"tailscaleAction,omitempty"`
	RollbackApplied  bool            `json:"rollbackApplied,omitempty"`
	ResponseTimeMs   int             `json:"responseTimeMs,omitempty"`
	SuccessCount     int64           `json:"successCount,omitempty"`
	ErrorCount       int64           `json:"errorCount,omitempty"`
	CheckedAt        string          `json:"checkedAt,omitempty"`
}

type AccessOperation struct {
	Component string `json:"component,omitempty"`
	Action    string `json:"action,omitempty"`
	Mode      string `json:"mode,omitempty"`
	Status    string `json:"status,omitempty"`
	Detail    string `json:"detail,omitempty"`
}

type TaskResultRequest struct {
	AgentID      string     `json:"agentId"`
	Status       string     `json:"status"`
	TaskResult   TaskResult `json:"taskResult"`
	ErrorMessage string    `json:"errorMessage,omitempty"`
}

type LogRequest struct {
	AgentID   string `json:"agentId"`
	ServerID  Long   `json:"serverId"`
	ServiceID Long   `json:"serviceId,omitempty"`
	TaskID    Long   `json:"taskId,omitempty"`
	Level     string `json:"level"`
	Content   string `json:"content"`
	Source    string `json:"source"`
}

type SystemSnapshot struct {
	Hostname string
	OS       string
	Arch     string
	IP       string
	CPU      string
	Memory   string
	Disk     string
}

type MetricsSnapshot struct {
	CPUUsage        float64
	MemoryUsage     float64
	DiskUsage       float64
	NetworkInBytes  int64
	NetworkOutBytes int64
	LoadAverage     float64
}
