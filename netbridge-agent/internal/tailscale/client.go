package tailscale

import (
	"context"
	"fmt"
	"strconv"
	"strings"

	"netbridge-agent/internal/command"
	"netbridge-agent/internal/config"
	"netbridge-agent/internal/logger"
	"netbridge-agent/pkg/model"
)

type Client struct {
	runner          command.Runner
	log             *logger.Logger
	mode            string
	binary          string
	advertisePort   int
	statusCommand   string
	ensureCommand   string
	teardownCommand string
	store           *RuntimeStore
	state           RuntimeState
}

func NewClient(runner command.Runner, log *logger.Logger, cfg config.TailscaleConfig, dataDir string) *Client {
	store := NewRuntimeStore(dataDir)
	state, err := store.Load()
	if err != nil {
		log.Errorf("load tailscale runtime state failed: %v", err)
		state = &RuntimeState{}
	}
	return &Client{
		runner:          runner,
		log:             log,
		mode:            cfg.Mode,
		binary:          cfg.Binary,
		advertisePort:   cfg.AdvertisePort,
		statusCommand:   cfg.StatusCommand,
		ensureCommand:   cfg.EnsureCommand,
		teardownCommand: cfg.TeardownCommand,
		store:           store,
		state:           *state,
	}
}

func (c *Client) Status(ctx context.Context) string {
	if c.statusCommand != "" {
		if _, err := c.runTemplate(ctx, c.statusCommand, model.TaskPayload{}); err != nil {
			c.log.Debugf("tailscale status command unavailable: %v", err)
			return "unknown"
		}
		return "connected"
	}
	result, err := c.runner.Run(ctx, c.binary, "ip", "-4")
	if err != nil {
		c.log.Debugf("tailscale status unavailable: %v", err)
		return "unknown"
	}
	if strings.TrimSpace(result.Stdout) == "" {
		return "disconnected"
	}
	return "connected"
}

func (c *Client) Observe(ctx context.Context) model.AccessOperation {
	status := c.Status(ctx)
	op := model.AccessOperation{
		Component: "tailscale",
		Action:    "observe",
		Mode:      c.mode,
		Status:    status,
	}
	switch status {
	case "connected":
		op.Detail = "tailscale is reachable"
	case "disconnected":
		op.Detail = "tailscale is installed but no IPv4 address was returned"
	default:
		op.Detail = "tailscale status could not be determined"
	}
	return op
}

func (c *Client) EnsureAccess(ctx context.Context, payload model.TaskPayload) error {
	_, err := c.EnsureAccessWithResult(ctx, payload)
	return err
}

func (c *Client) EnsureAccessWithResult(ctx context.Context, payload model.TaskPayload) (model.AccessOperation, error) {
	op := model.AccessOperation{Component: "tailscale", Action: "ensure", Mode: c.mode}
	if payload.AccessType != "tailscale" {
		op.Status = "skipped"
		op.Detail = "accessType is not tailscale"
		return op, nil
	}
	if c.ensureCommand != "" {
		_, err := c.runTemplate(ctx, c.ensureCommand, payload)
		if err != nil {
			op.Status = "failed"
			op.Detail = err.Error()
			return op, fmt.Errorf("tailscale ensure access: %w", err)
		}
		c.markConfigured(c.resolveAdvertisePort(payload))
		op.Status = "configured"
		op.Detail = "configured via ensureCommand"
		return op, nil
	}
	if defaultCommand := c.defaultEnsureCommand(payload); defaultCommand != "" {
		if c.stateMatches(payload) && c.state.AccessConfigured {
			c.log.Debugf("tailscale access already configured for mode=%s, skip ensure", c.mode)
			op.Status = "unchanged"
			op.Detail = "already configured"
			return op, nil
		}
		if c.state.AccessConfigured && !c.stateMatches(payload) {
			if teardown := c.defaultTeardownCommand(payload); teardown != "" {
				if _, err := c.runTemplate(ctx, teardown, payload); err != nil {
					op.Status = "failed"
					op.Detail = "reconfigure teardown failed: " + err.Error()
					return op, fmt.Errorf("tailscale reconfigure teardown failed: %w", err)
				}
			}
			c.markUnconfigured(c.resolveAdvertisePort(payload))
		}
		_, err := c.runTemplate(ctx, defaultCommand, payload)
		if err != nil {
			op.Status = "failed"
			op.Detail = err.Error()
			return op, fmt.Errorf("tailscale ensure access: %w", err)
		}
		c.markConfigured(c.resolveAdvertisePort(payload))
		op.Status = "configured"
		op.Detail = "configured via default mode"
		return op, nil
	}
	_, err := c.runner.Run(ctx, c.binary, "status")
	if err != nil {
		op.Status = "failed"
		op.Detail = err.Error()
		return op, fmt.Errorf("tailscale ensure access: %w", err)
	}
	op.Status = "checked"
	op.Detail = "fallback status probe succeeded"
	return op, nil
}

func (c *Client) TeardownAccess(ctx context.Context, payload model.TaskPayload) error {
	_, err := c.TeardownAccessWithResult(ctx, payload)
	return err
}

func (c *Client) TeardownAccessWithResult(ctx context.Context, payload model.TaskPayload) (model.AccessOperation, error) {
	op := model.AccessOperation{Component: "tailscale", Action: "teardown", Mode: c.mode}
	if payload.AccessType != "tailscale" {
		op.Status = "skipped"
		op.Detail = "accessType is not tailscale"
		return op, nil
	}
	if c.teardownCommand != "" {
		_, err := c.runTemplate(ctx, c.teardownCommand, payload)
		if err != nil {
			op.Status = "failed"
			op.Detail = err.Error()
			return op, fmt.Errorf("tailscale teardown access: %w", err)
		}
		c.markUnconfigured(c.resolveAdvertisePort(payload))
		op.Status = "removed"
		op.Detail = "removed via teardownCommand"
		return op, nil
	}
	if defaultCommand := c.defaultTeardownCommand(payload); defaultCommand != "" {
		if !c.state.AccessConfigured {
			c.log.Debugf("tailscale access not marked configured, skip teardown")
			op.Status = "unchanged"
			op.Detail = "not marked configured"
			return op, nil
		}
		_, err := c.runTemplate(ctx, defaultCommand, payload)
		if err != nil {
			op.Status = "failed"
			op.Detail = err.Error()
			return op, fmt.Errorf("tailscale teardown access: %w", err)
		}
		c.markUnconfigured(c.resolveAdvertisePort(payload))
		op.Status = "removed"
		op.Detail = "removed via default mode"
	}
	return op, nil
}

func (c *Client) runTemplate(ctx context.Context, commandTemplate string, payload model.TaskPayload) (command.Result, error) {
	values := templateValues(payload)
	values["advertisePort"] = strconv.Itoa(c.resolveAdvertisePort(payload))
	name, args, err := command.Split(command.Expand(commandTemplate, values))
	if err != nil {
		return command.Result{}, err
	}
	return c.runner.Run(ctx, name, args...)
}

func templateValues(payload model.TaskPayload) map[string]string {
	return map[string]string{
		"serviceId":   strconv.FormatInt(int64(payload.ServiceID), 10),
		"serverId":    strconv.FormatInt(int64(payload.ServerID), 10),
		"serviceName": payload.ServiceName,
		"action":      payload.Action,
		"targetHost":  payload.TargetHost,
		"port":        strconv.Itoa(payload.Port),
		"protocol":    payload.Protocol,
		"accessType":  payload.AccessType,
		"domain":      payload.Domain,
	}
}

func (c *Client) defaultEnsureCommand(payload model.TaskPayload) string {
	targetURL := proxyTarget(payload)
	switch c.mode {
	case "serve-http":
		return c.binary + " serve --bg --http={advertisePort} " + targetURL
	case "serve-https":
		return c.binary + " serve --bg --https={advertisePort} " + targetURL
	case "serve-tcp":
		return c.binary + " serve --bg --tcp={advertisePort} tcp://{targetHost}:{port}"
	case "funnel":
		return c.binary + " funnel --bg --https={advertisePort} " + targetURL
	default:
		return ""
	}
}

func (c *Client) defaultTeardownCommand(payload model.TaskPayload) string {
	switch c.mode {
	case "serve-http":
		return c.binary + " serve --http={advertisePort} off"
	case "serve-https":
		return c.binary + " serve --https={advertisePort} off"
	case "serve-tcp":
		return c.binary + " serve --tcp={advertisePort} off"
	case "funnel":
		return c.binary + " funnel --https={advertisePort} off"
	default:
		return ""
	}
}

func proxyTarget(payload model.TaskPayload) string {
	host := payload.TargetHost
	if strings.TrimSpace(host) == "" {
		host = "127.0.0.1"
	}
	protocol := payload.Protocol
	if strings.TrimSpace(protocol) == "" {
		protocol = "http"
	}
	return fmt.Sprintf("%s://%s:%d", protocol, host, payload.Port)
}

func (c *Client) resolveAdvertisePort(payload model.TaskPayload) int {
	if c.advertisePort > 0 {
		return c.advertisePort
	}
	if c.mode == "funnel" {
		return 443
	}
	if payload.Port > 0 {
		return payload.Port
	}
	return 443
}

func (c *Client) stateMatches(payload model.TaskPayload) bool {
	return c.state.Mode == c.mode && c.state.AdvertisePort == c.resolveAdvertisePort(payload)
}

func (c *Client) markConfigured(advertisePort int) {
	c.state.AccessConfigured = true
	c.state.Mode = c.mode
	c.state.AdvertisePort = advertisePort
	if err := c.store.Save(c.state); err != nil {
		c.log.Errorf("save tailscale runtime state failed: %v", err)
	}
}

func (c *Client) markUnconfigured(advertisePort int) {
	c.state.AccessConfigured = false
	c.state.Mode = c.mode
	c.state.AdvertisePort = advertisePort
	if err := c.store.Save(c.state); err != nil {
		c.log.Errorf("save tailscale runtime state failed: %v", err)
	}
}
