package tunnel

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"time"
)

type RuntimeState struct {
	ServiceInstalled bool   `json:"serviceInstalled"`
	Mode             string `json:"mode,omitempty"`
	ConfigPath       string `json:"configPath,omitempty"`
	TokenConfigured  bool   `json:"tokenConfigured,omitempty"`
	UpdatedAt        string `json:"updatedAt,omitempty"`
}

type RuntimeStore struct {
	path string
}

func NewRuntimeStore(dataDir string) *RuntimeStore {
	return &RuntimeStore{path: filepath.Join(dataDir, "tunnel-state.json")}
}

func (s *RuntimeStore) Load() (*RuntimeState, error) {
	content, err := os.ReadFile(s.path)
	if err != nil {
		if os.IsNotExist(err) {
			return &RuntimeState{}, nil
		}
		return nil, fmt.Errorf("read tunnel state: %w", err)
	}
	var state RuntimeState
	if err := json.Unmarshal(content, &state); err != nil {
		return nil, fmt.Errorf("decode tunnel state: %w", err)
	}
	return &state, nil
}

func (s *RuntimeStore) Save(state RuntimeState) error {
	if err := os.MkdirAll(filepath.Dir(s.path), 0o755); err != nil {
		return fmt.Errorf("mkdir tunnel state dir: %w", err)
	}
	state.UpdatedAt = time.Now().Format(time.RFC3339)
	content, err := json.MarshalIndent(state, "", "  ")
	if err != nil {
		return fmt.Errorf("encode tunnel state: %w", err)
	}
	if err := os.WriteFile(s.path, content, 0o644); err != nil {
		return fmt.Errorf("write tunnel state: %w", err)
	}
	return nil
}
