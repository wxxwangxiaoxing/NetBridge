package service

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

type Manager struct {
	runner   command.Runner
	log      *logger.Logger
	mappings map[string]config.ServiceMapping
}

func NewManager(runner command.Runner, log *logger.Logger, mappings map[string]config.ServiceMapping) *Manager {
	return &Manager{runner: runner, log: log, mappings: mappings}
}

func (m *Manager) Start(ctx context.Context, name string) error {
	_, err := m.StartWithResult(ctx, name)
	return err
}

func (m *Manager) StartWithResult(ctx context.Context, name string) (model.AccessOperation, error) {
	op := model.AccessOperation{Component: "service", Action: "start"}
	if err := m.runMappedCommand(ctx, name, "start"); err == nil {
		op.Status = "active"
		op.Detail = fmt.Sprintf("service %s started via mapped command", name)
		return op, nil
	} else if err != errNoMappedCommand {
		op.Status = "failed"
		op.Detail = err.Error()
		return op, err
	}
	if err := m.runSystemctl(ctx, "start", m.resolveUnit(name)); err != nil {
		op.Status = "failed"
		op.Detail = err.Error()
		return op, err
	}
	op.Status = "active"
	op.Detail = fmt.Sprintf("service %s started via system control", name)
	return op, nil
}

func (m *Manager) Stop(ctx context.Context, name string) error {
	_, err := m.StopWithResult(ctx, name)
	return err
}

func (m *Manager) StopWithResult(ctx context.Context, name string) (model.AccessOperation, error) {
	op := model.AccessOperation{Component: "service", Action: "stop"}
	if err := m.runMappedCommand(ctx, name, "stop"); err == nil {
		op.Status = "inactive"
		op.Detail = fmt.Sprintf("service %s stopped via mapped command", name)
		return op, nil
	} else if err != errNoMappedCommand {
		op.Status = "failed"
		op.Detail = err.Error()
		return op, err
	}
	if err := m.runSystemctl(ctx, "stop", m.resolveUnit(name)); err != nil {
		op.Status = "failed"
		op.Detail = err.Error()
		return op, err
	}
	op.Status = "inactive"
	op.Detail = fmt.Sprintf("service %s stopped via system control", name)
	return op, nil
}

func (m *Manager) Restart(ctx context.Context, name string) error {
	_, err := m.RestartWithResult(ctx, name)
	return err
}

func (m *Manager) RestartWithResult(ctx context.Context, name string) (model.AccessOperation, error) {
	op := model.AccessOperation{Component: "service", Action: "restart"}
	if err := m.runMappedCommand(ctx, name, "restart"); err == nil {
		op.Status = "active"
		op.Detail = fmt.Sprintf("service %s restarted via mapped command", name)
		return op, nil
	} else if err != errNoMappedCommand {
		op.Status = "failed"
		op.Detail = err.Error()
		return op, err
	}
	if err := m.runSystemctl(ctx, "restart", m.resolveUnit(name)); err != nil {
		op.Status = "failed"
		op.Detail = err.Error()
		return op, err
	}
	op.Status = "active"
	op.Detail = fmt.Sprintf("service %s restarted via system control", name)
	return op, nil
}

func (m *Manager) Check(ctx context.Context, name string) (string, error) {
	op, err := m.Observe(ctx, name)
	return op.Status, err
}

func (m *Manager) Observe(ctx context.Context, name string) (model.AccessOperation, error) {
	op := model.AccessOperation{Component: "service", Action: "observe"}
	resolved := m.resolveUnit(name)
	if err := m.runMappedCommand(ctx, name, "check"); err == nil {
		op.Status = "active"
		op.Detail = fmt.Sprintf("service %s reported active via mapped check", name)
		return op, nil
	} else if err != errNoMappedCommand {
		op.Status = "error"
		op.Detail = err.Error()
		return op, err
	}
	if runtime.GOOS != "linux" {
		op.Status = "unknown"
		op.Detail = fmt.Sprintf("service %s check is not implemented on %s", name, runtime.GOOS)
		return op, nil
	}
	result, err := m.runner.Run(ctx, "systemctl", "is-active", resolved)
	if err != nil {
		m.log.Debugf("service check failed for %s: %v", resolved, err)
		op.Status = "unknown"
		op.Detail = err.Error()
		return op, err
	}
	if result.Stdout == "" {
		op.Status = "unknown"
		op.Detail = fmt.Sprintf("service %s returned empty status", name)
		return op, nil
	}
	op.Status = result.Stdout
	op.Detail = fmt.Sprintf("service %s observed as %s", name, result.Stdout)
	return op, nil
}

func (m *Manager) runSystemctl(ctx context.Context, action, name string) error {
	if runtime.GOOS != "linux" {
		m.log.Infof("skip %s for service %s on %s", action, name, runtime.GOOS)
		return nil
	}
	_, err := m.runner.Run(ctx, "systemctl", action, name)
	if err != nil {
		return fmt.Errorf("systemctl %s %s: %w", action, name, err)
	}
	return nil
}

var errNoMappedCommand = fmt.Errorf("no mapped command")

func (m *Manager) resolveUnit(name string) string {
	key := strings.TrimSpace(name)
	if key == "" {
		return name
	}
	if mapping, ok := m.mappings[key]; ok && strings.TrimSpace(mapping.Unit) != "" {
		return mapping.Unit
	}
	return key
}

func (m *Manager) runMappedCommand(ctx context.Context, name, action string) error {
	key := strings.TrimSpace(name)
	if key == "" {
		return errNoMappedCommand
	}
	mapping, ok := m.mappings[key]
	if !ok {
		return errNoMappedCommand
	}
	commandLine := mappedCommand(mapping, action)
	if strings.TrimSpace(commandLine) == "" {
		return errNoMappedCommand
	}
	bin, args, err := command.Split(commandLine)
	if err != nil {
		return err
	}
	_, err = m.runner.Run(ctx, bin, args...)
	if err != nil {
		return fmt.Errorf("mapped %s command for %s: %w", action, key, err)
	}
	return nil
}

func mappedCommand(mapping config.ServiceMapping, action string) string {
	switch action {
	case "start":
		return mapping.StartCommand
	case "stop":
		return mapping.StopCommand
	case "restart":
		return mapping.RestartCommand
	case "check":
		return mapping.CheckCommand
	default:
		return ""
	}
}
