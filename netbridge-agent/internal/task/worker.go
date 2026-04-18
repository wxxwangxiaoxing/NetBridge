package task

import (
	"context"

	"netbridge-agent/internal/executor"
	"netbridge-agent/pkg/model"
)

type Worker struct {
	executor *executor.Executor
}

func NewWorker(executor *executor.Executor) *Worker {
	return &Worker{executor: executor}
}

func (w *Worker) Execute(ctx context.Context, task *model.PullTaskResponse) (model.TaskResult, string) {
	return w.executor.Execute(ctx, task)
}
