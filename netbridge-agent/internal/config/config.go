package config

import (
	"errors"
	"fmt"
	"os"
	"time"

	"gopkg.in/yaml.v3"
)

type Config struct {
	ServerURL            string                    `yaml:"serverUrl"`
	InstallToken         string                    `yaml:"installToken"`
	DataDir              string                    `yaml:"dataDir"`
	LogLevel             string                    `yaml:"logLevel"`
	AgentVersion         string                    `yaml:"agentVersion"`
	HeartbeatIntervalSec int                       `yaml:"heartbeatIntervalSec"`
	TaskPollIntervalSec  int                       `yaml:"taskPollIntervalSec"`
	HeartbeatStatus      string                    `yaml:"heartbeatStatus"`
	TailscaleIP          string                    `yaml:"tailscaleIp"`
	Tailscale            TailscaleConfig           `yaml:"tailscale"`
	Tunnel               TunnelConfig              `yaml:"tunnel"`
	ServiceMappings      map[string]ServiceMapping `yaml:"serviceMappings"`
}

type TailscaleConfig struct {
	Mode            string `yaml:"mode"`
	Binary          string `yaml:"binary"`
	AdvertisePort   int    `yaml:"advertisePort"`
	StatusCommand   string `yaml:"statusCommand"`
	EnsureCommand   string `yaml:"ensureCommand"`
	TeardownCommand string `yaml:"teardownCommand"`
}

type TunnelConfig struct {
	Mode            string `yaml:"mode"`
	Binary          string `yaml:"binary"`
	Token           string `yaml:"token"`
	ConfigPath      string `yaml:"configPath"`
	StatusCommand   string `yaml:"statusCommand"`
	EnsureCommand   string `yaml:"ensureCommand"`
	TeardownCommand string `yaml:"teardownCommand"`
}

type ServiceMapping struct {
	Unit           string `yaml:"unit"`
	StartCommand   string `yaml:"startCommand"`
	StopCommand    string `yaml:"stopCommand"`
	RestartCommand string `yaml:"restartCommand"`
	CheckCommand   string `yaml:"checkCommand"`
}

func Load(path string) (*Config, error) {
	content, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("read config: %w", err)
	}
	var cfg Config
	if err := yaml.Unmarshal(content, &cfg); err != nil {
		return nil, fmt.Errorf("parse config: %w", err)
	}
	if cfg.ServerURL == "" {
		return nil, errors.New("serverUrl is required")
	}
	if cfg.DataDir == "" {
		cfg.DataDir = "data"
	}
	if cfg.LogLevel == "" {
		cfg.LogLevel = "info"
	}
	if cfg.AgentVersion == "" {
		cfg.AgentVersion = "0.1.0"
	}
	if cfg.HeartbeatIntervalSec <= 0 {
		cfg.HeartbeatIntervalSec = 30
	}
	if cfg.TaskPollIntervalSec <= 0 {
		cfg.TaskPollIntervalSec = 10
	}
	if cfg.HeartbeatStatus == "" {
		cfg.HeartbeatStatus = "online"
	}
	if cfg.Tailscale.Binary == "" {
		cfg.Tailscale.Binary = "tailscale"
	}
	if cfg.Tunnel.Binary == "" {
		cfg.Tunnel.Binary = "cloudflared"
	}
	return &cfg, nil
}

func (c *Config) HeartbeatInterval() time.Duration {
	return time.Duration(c.HeartbeatIntervalSec) * time.Second
}

func (c *Config) TaskPollInterval() time.Duration {
	return time.Duration(c.TaskPollIntervalSec) * time.Second
}
