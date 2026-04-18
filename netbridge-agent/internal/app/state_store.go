package app

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"

	"netbridge-agent/pkg/model"
)

type StateStore struct {
	path string
}

func NewStateStore(dataDir string) *StateStore {
	return &StateStore{path: filepath.Join(dataDir, "agent-state.json")}
}

func (s *StateStore) Load() (*model.AgentState, error) {
	content, err := os.ReadFile(s.path)
	if err != nil {
		if os.IsNotExist(err) {
			return nil, nil
		}
		return nil, fmt.Errorf("read state: %w", err)
	}
	var state model.AgentState
	if err := json.Unmarshal(content, &state); err != nil {
		return nil, fmt.Errorf("decode state: %w", err)
	}
	return &state, nil
}

func (s *StateStore) Save(state model.AgentState) error {
	if err := os.MkdirAll(filepath.Dir(s.path), 0o755); err != nil {
		return fmt.Errorf("mkdir state dir: %w", err)
	}
	content, err := json.MarshalIndent(state, "", "  ")
	if err != nil {
		return fmt.Errorf("encode state: %w", err)
	}
	if err := os.WriteFile(s.path, content, 0o644); err != nil {
		return fmt.Errorf("write state: %w", err)
	}
	return nil
}
