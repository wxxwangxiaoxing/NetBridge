package main

import (
	"context"
	"flag"
	"os"
	"os/signal"
	"syscall"

	"netbridge-agent/internal/app"
	"netbridge-agent/internal/config"
	"netbridge-agent/internal/logger"
)

func main() {
	configPath := flag.String("config", "configs/agent.yaml", "path to agent config")
	flag.Parse()

	cfg, err := config.Load(*configPath)
	if err != nil {
		panic(err)
	}

	log := logger.New(cfg.LogLevel)
	application, err := app.New(cfg, log)
	if err != nil {
		log.Errorf("failed to initialize app: %v", err)
		os.Exit(1)
	}

	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	if err := application.Run(ctx); err != nil {
		log.Errorf("agent exited with error: %v", err)
		os.Exit(1)
	}
}
