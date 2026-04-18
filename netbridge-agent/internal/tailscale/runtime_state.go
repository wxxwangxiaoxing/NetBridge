package tailscale

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"time"
)

type RuntimeState struct {
	AccessConfigured bool   `json:"accessConfigured"`
	Mode             string `json:"mode,omitempty"`
	AdvertisePort    int    `json:"advertisePort,omitempty"`
	UpdatedAt        string `json:"updatedAt,omitempty"`
}

type RuntimeStore struct {
	path string
}

func NewRuntimeStore(dataDir string) *RuntimeStore {
	return &RuntimeStore{path: filepath.Join(dataDir, "tailscale-state.json")}
}

func (s *RuntimeStore) Load() (*RuntimeState, error) {
	content, err := os.ReadFile(s.path)
	if err != nil {
		if os.IsNotExist(err) {
			return &RuntimeState{}, nil
		}
		return nil, fmt.Errorf("read tailscale state: %w", err)
	}
	var state RuntimeState
	if err := json.Unmarshal(content, &state); err != nil {
		return nil, fmt.Errorf("decode tailscale state: %w", err)
	}
	return &state, nil
}

func (s *RuntimeStore) Save(state RuntimeState) error {
	if err := os.MkdirAll(filepath.Dir(s.path), 0o755); err != nil {
		return fmt.Errorf("mkdir tailscale state dir: %w", err)
	}
	state.UpdatedAt = time.Now().Format(time.RFC3339)
	content, err := json.MarshalIndent(state, "", "  ")
	if err != nil {
		return fmt.Errorf("encode tailscale state: %w", err)
	}
	if err := os.WriteFile(s.path, content, 0o644); err != nil {
		return fmt.Errorf("write tailscale state: %w", err)
	}
	return nil
}
