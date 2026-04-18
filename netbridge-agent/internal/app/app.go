package app

import (
	"context"
	"fmt"
	"time"

	"netbridge-agent/internal/api"
	"netbridge-agent/internal/command"
	"netbridge-agent/internal/config"
	"netbridge-agent/internal/executor"
	"netbridge-agent/internal/heartbeat"
	"netbridge-agent/internal/logger"
	"netbridge-agent/internal/monitor"
	svcctl "netbridge-agent/internal/service"
	"netbridge-agent/internal/task"
	"netbridge-agent/internal/tailscale"
	"netbridge-agent/internal/tunnel"
	"netbridge-agent/pkg/constant"
	"netbridge-agent/pkg/model"
)

type App struct {
	cfg       *config.Config
	log       *logger.Logger
	client    *api.Client
	state     model.AgentState
	store     *StateStore
	worker    *task.Worker
}

func New(cfg *config.Config, log *logger.Logger) (*App, error) {
	client := api.NewClient(cfg.ServerURL)
	store := NewStateStore(cfg.DataDir)
	commandRunner := command.NewOSRunner()
	serviceManager := svcctl.NewManager(commandRunner, log, cfg.ServiceMappings)
	tailscaleClient := tailscale.NewClient(commandRunner, log, cfg.Tailscale, cfg.DataDir)
	tunnelController := tunnel.NewController(commandRunner, log, cfg.Tunnel, cfg.DataDir)
	state := model.AgentState{}
	if saved, err := store.Load(); err != nil {
		return nil, err
	} else if saved != nil {
		state = *saved
	}
	return &App{
		cfg:    cfg,
		log:    log,
		client: client,
		state:  state,
		store:  store,
		worker: task.NewWorker(executor.New(serviceManager, tailscaleClient, tunnelController)),
	}, nil
}

func (a *App) Run(ctx context.Context) error {
	if err := a.ensureRegistered(ctx); err != nil {
		return err
	}
	if err := a.runHeartbeat(ctx); err != nil {
		a.log.Errorf("initial heartbeat failed: %v", err)
	}
	if err := a.runTaskCycle(ctx); err != nil {
		a.log.Errorf("initial task poll failed: %v", err)
	}

	heartbeatTicker := time.NewTicker(a.cfg.HeartbeatInterval())
	defer heartbeatTicker.Stop()
	taskTicker := time.NewTicker(a.cfg.TaskPollInterval())
	defer taskTicker.Stop()

	for {
		select {
		case <-ctx.Done():
			a.log.Infof("agent shutdown requested")
			return nil
		case <-heartbeatTicker.C:
			if err := a.runHeartbeat(ctx); err != nil {
				a.log.Errorf("heartbeat failed: %v", err)
			}
		case <-taskTicker.C:
			if err := a.runTaskCycle(ctx); err != nil {
				a.log.Errorf("task cycle failed: %v", err)
			}
		}
	}
}

func (a *App) ensureRegistered(ctx context.Context) error {
	if a.state.ServerID != 0 && a.state.AgentID != "" {
		a.log.Infof("using existing registration: serverId=%d agentId=%s", a.state.ServerID, a.state.AgentID)
		return nil
	}
	if a.cfg.InstallToken == "" {
		return fmt.Errorf("installToken is required for first registration")
	}
	snapshot := monitor.Snapshot()
	req := model.RegisterRequest{
		Token:        a.cfg.InstallToken,
		Hostname:     snapshot.Hostname,
		OS:           snapshot.OS,
		IP:           snapshot.IP,
		AgentVersion: a.cfg.AgentVersion,
		CPU:          snapshot.CPU,
		Memory:       snapshot.Memory,
		Disk:         snapshot.Disk,
		Arch:         snapshot.Arch,
	}
	resp, err := a.client.Register(ctx, req)
	if err != nil {
		return fmt.Errorf("register agent: %w", err)
	}
	a.state = model.AgentState{
		ServerID: resp.ServerID,
		AgentID:  resp.AgentID,
		Hostname: resp.Hostname,
	}
	if err := a.store.Save(a.state); err != nil {
		return err
	}
	a.log.Infof("registered agent successfully: serverId=%d agentId=%s", a.state.ServerID, a.state.AgentID)
	return nil
}

func (a *App) runHeartbeat(ctx context.Context) error {
	req := heartbeat.BuildRequest(a.cfg, a.state, monitor.Metrics())
	if err := a.client.Heartbeat(ctx, req); err != nil {
		return err
	}
	a.log.Debugf("heartbeat sent")
	return nil
}

func (a *App) runTaskCycle(ctx context.Context) error {
	taskResp, err := a.client.PullTask(ctx, model.PullTaskRequest{
		AgentID:  a.state.AgentID,
		ServerID: a.state.ServerID,
	})
	if err != nil {
		return err
	}
	if taskResp == nil {
		return nil
	}
	a.log.Infof("received task: id=%d type=%s", taskResp.ID, taskResp.Type)

	result, status := a.worker.Execute(ctx, taskResp)
	req := model.TaskResultRequest{
		AgentID:    a.state.AgentID,
		Status:     status,
		TaskResult: result,
	}
	if status != constant.StatusSuccess {
		req.ErrorMessage = result.Message
	}
	if err := a.client.ReportTaskResult(ctx, taskResp.ID, req); err != nil {
		_ = a.reportLog(ctx, taskResp, constant.LevelError, fmt.Sprintf("report task result failed: %v", err))
		return err
	}
	_ = a.reportLog(ctx, taskResp, constant.LevelInfo, fmt.Sprintf("task %d finished with status %s", taskResp.ID, status))
	return nil
}

func (a *App) reportLog(ctx context.Context, taskResp *model.PullTaskResponse, level, content string) error {
	req := model.LogRequest{
		AgentID:   a.state.AgentID,
		ServerID:  a.state.ServerID,
		ServiceID: taskResp.Payload.ServiceID,
		TaskID:    taskResp.ID,
		Level:     level,
		Content:   content,
		Source:    constant.SourceAgent,
	}
	return a.client.ReportLog(ctx, req)
}
