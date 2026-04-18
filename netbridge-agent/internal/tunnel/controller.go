package tunnel

import (
	"context"
	"fmt"
	"runtime"
	"strings"

	"netbridge-agent/internal/command"
	"netbridge-agent/internal/config"
	"netbridge-agent/internal/logger"
	"netbridge-agent/pkg/model"
)

type Controller struct {
	runner          command.Runner
	log             *logger.Logger
	mode            string
	binary          string
	token           string
	configPath      string
	statusCommand   string
	ensureCommand   string
	teardownCommand string
	store           *RuntimeStore
	state           RuntimeState
}

func NewController(runner command.Runner, log *logger.Logger, cfg config.TunnelConfig, dataDir string) *Controller {
	store := NewRuntimeStore(dataDir)
	state, err := store.Load()
	if err != nil {
		log.Errorf("load tunnel runtime state failed: %v", err)
		state = &RuntimeState{}
	}
	return &Controller{
		runner:          runner,
		log:             log,
		mode:            cfg.Mode,
		binary:          cfg.Binary,
		token:           cfg.Token,
		configPath:      cfg.ConfigPath,
		statusCommand:   cfg.StatusCommand,
		ensureCommand:   cfg.EnsureCommand,
		teardownCommand: cfg.TeardownCommand,
		store:           store,
		state:           *state,
	}
}

func (c *Controller) Status(ctx context.Context) string {
	if c.statusCommand != "" {
		if _, err := c.runTemplate(ctx, c.statusCommand, model.TaskPayload{}); err != nil {
			c.log.Debugf("cloudflared status command unavailable: %v", err)
			return "unknown"
		}
		return "available"
	}
	if c.isManagedServiceMode() {
		return c.serviceStatus(ctx)
	}
	result, err := c.runner.Run(ctx, c.binary, "--version")
	if err != nil {
		c.log.Debugf("cloudflared status unavailable: %v", err)
		return "unknown"
	}
	if result.Stdout == "" {
		return "inactive"
	}
	return "available"
}

func (c *Controller) Observe(ctx context.Context) model.AccessOperation {
	status := c.Status(ctx)
	op := model.AccessOperation{
		Component: "cloudflared",
		Action:    "observe",
		Mode:      c.mode,
		Status:    status,
	}
	switch status {
	case "available":
		op.Detail = "cloudflared is available"
	case "inactive":
		op.Detail = "cloudflared is installed but not running"
	default:
		op.Detail = "cloudflared status could not be determined"
	}
	return op
}

func (c *Controller) EnsureAccess(ctx context.Context, payload model.TaskPayload) error {
	_, err := c.EnsureAccessWithResult(ctx, payload)
	return err
}

func (c *Controller) EnsureAccessWithResult(ctx context.Context, payload model.TaskPayload) (model.AccessOperation, error) {
	op := model.AccessOperation{Component: "cloudflared", Action: "ensure", Mode: c.mode}
	if payload.AccessType != "cloudflare" && payload.AccessType != "tunnel" {
		op.Status = "skipped"
		op.Detail = "accessType is not cloudflare/tunnel"
		return op, nil
	}
	if c.ensureCommand != "" {
		_, err := c.runTemplate(ctx, c.ensureCommand, payload)
		if err != nil {
			op.Status = "failed"
			op.Detail = err.Error()
			return op, fmt.Errorf("tunnel ensure access: %w", err)
		}
		c.markInstalled()
		op.Status = "configured"
		op.Detail = "configured via ensureCommand"
		return op, nil
	}
	if defaultCommand := c.defaultEnsureCommand(); defaultCommand != "" {
		if c.stateMatchesConfig() && c.state.ServiceInstalled {
			c.log.Debugf("cloudflared service already installed for mode=%s, skip ensure", c.mode)
			op.Status = "unchanged"
			op.Detail = "already installed"
			return op, nil
		}
		if c.state.ServiceInstalled && !c.stateMatchesConfig() {
			if teardown := c.defaultTeardownCommand(); teardown != "" {
				_, err := c.runTemplate(ctx, teardown, payload)
				if err != nil {
					op.Status = "failed"
					op.Detail = "reconfigure teardown failed: " + err.Error()
					return op, fmt.Errorf("tunnel reconfigure teardown failed: %w", err)
				}
			}
			c.markUninstalled()
		}
		_, err := c.runTemplate(ctx, defaultCommand, payload)
		if err != nil {
			op.Status = "failed"
			op.Detail = err.Error()
			return op, fmt.Errorf("tunnel ensure access: %w", err)
		}
		c.markInstalled()
		op.Status = "configured"
		op.Detail = "configured via default mode"
		return op, nil
	}
	_, err := c.runner.Run(ctx, c.binary, "--version")
	if err != nil {
		op.Status = "failed"
		op.Detail = err.Error()
		return op, fmt.Errorf("tunnel ensure access: %w", err)
	}
	op.Status = "checked"
	op.Detail = "fallback version probe succeeded"
	return op, nil
}

func (c *Controller) TeardownAccess(ctx context.Context, payload model.TaskPayload) error {
	_, err := c.TeardownAccessWithResult(ctx, payload)
	return err
}

func (c *Controller) TeardownAccessWithResult(ctx context.Context, payload model.TaskPayload) (model.AccessOperation, error) {
	op := model.AccessOperation{Component: "cloudflared", Action: "teardown", Mode: c.mode}
	if payload.AccessType != "cloudflare" && payload.AccessType != "tunnel" {
		op.Status = "skipped"
		op.Detail = "accessType is not cloudflare/tunnel"
		return op, nil
	}
	if c.teardownCommand != "" {
		_, err := c.runTemplate(ctx, c.teardownCommand, payload)
		if err != nil {
			op.Status = "failed"
			op.Detail = err.Error()
			return op, fmt.Errorf("tunnel teardown access: %w", err)
		}
		c.markUninstalled()
		op.Status = "removed"
		op.Detail = "removed via teardownCommand"
		return op, nil
	}
	if defaultCommand := c.defaultTeardownCommand(); defaultCommand != "" {
		if !c.state.ServiceInstalled {
			c.log.Debugf("cloudflared service not marked installed, skip teardown")
			op.Status = "unchanged"
			op.Detail = "not marked installed"
			return op, nil
		}
		_, err := c.runTemplate(ctx, defaultCommand, payload)
		if err != nil {
			op.Status = "failed"
			op.Detail = err.Error()
			return op, fmt.Errorf("tunnel teardown access: %w", err)
		}
		c.markUninstalled()
		op.Status = "removed"
		op.Detail = "removed via default mode"
	}
	return op, nil
}

func (c *Controller) runTemplate(ctx context.Context, commandTemplate string, payload model.TaskPayload) (command.Result, error) {
	values := templateValues(payload)
	values["tunnelToken"] = c.token
	values["configPath"] = c.configPath
	name, args, err := command.Split(command.Expand(commandTemplate, values))
	if err != nil {
		return command.Result{}, err
	}
	return c.runner.Run(ctx, name, args...)
}

func templateValues(payload model.TaskPayload) map[string]string {
	return map[string]string{
		"serviceId":   fmt.Sprintf("%d", payload.ServiceID),
		"serverId":    fmt.Sprintf("%d", payload.ServerID),
		"serviceName": payload.ServiceName,
		"action":      payload.Action,
		"targetHost":  payload.TargetHost,
		"port":        fmt.Sprintf("%d", payload.Port),
		"protocol":    payload.Protocol,
		"accessType":  payload.AccessType,
		"domain":      payload.Domain,
	}
}

func (c *Controller) defaultEnsureCommand() string {
	switch c.mode {
	case "service-token":
		if c.token == "" {
			return ""
		}
		return c.binary + " service install {tunnelToken}"
	case "service-config":
		if c.configPath == "" {
			return ""
		}
		return c.binary + " --config {configPath} service install"
	default:
		return ""
	}
}

func (c *Controller) defaultTeardownCommand() string {
	switch c.mode {
	case "service-token", "service-config":
		return c.binary + " service uninstall"
	default:
		return ""
	}
}

func (c *Controller) isManagedServiceMode() bool {
	return c.mode == "service-token" || c.mode == "service-config"
}

func (c *Controller) serviceStatus(ctx context.Context) string {
	switch runtime.GOOS {
	case "linux":
		result, err := c.runner.Run(ctx, "systemctl", "is-active", "cloudflared")
		if err != nil {
			if c.state.ServiceInstalled {
				return "inactive"
			}
			c.log.Debugf("cloudflared systemd status unavailable: %v", err)
			return "unknown"
		}
		if strings.TrimSpace(result.Stdout) == "active" {
			return "available"
		}
		return "inactive"
	case "windows":
		result, err := c.runner.Run(ctx, "sc.exe", "query", "cloudflared")
		if err != nil {
			if c.state.ServiceInstalled {
				return "inactive"
			}
			c.log.Debugf("cloudflared Windows service status unavailable: %v", err)
			return "unknown"
		}
		if strings.Contains(strings.ToUpper(result.Stdout), "RUNNING") {
			return "available"
		}
		return "inactive"
	default:
		if c.state.ServiceInstalled {
			return "available"
		}
		return "unknown"
	}
}

func (c *Controller) stateMatchesConfig() bool {
	return c.state.Mode == c.mode &&
		c.state.ConfigPath == c.configPath &&
		c.state.TokenConfigured == (c.token != "")
}

func (c *Controller) markInstalled() {
	c.state.ServiceInstalled = true
	c.state.Mode = c.mode
	c.state.ConfigPath = c.configPath
	c.state.TokenConfigured = c.token != ""
	if err := c.store.Save(c.state); err != nil {
		c.log.Errorf("save tunnel runtime state failed: %v", err)
	}
}

func (c *Controller) markUninstalled() {
	c.state.ServiceInstalled = false
	c.state.Mode = c.mode
	c.state.ConfigPath = c.configPath
	c.state.TokenConfigured = c.token != ""
	if err := c.store.Save(c.state); err != nil {
		c.log.Errorf("save tunnel runtime state failed: %v", err)
	}
}
