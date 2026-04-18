package api

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"

	"netbridge-agent/pkg/model"
)

type Client struct {
	baseURL string
	http    *http.Client
}

func NewClient(baseURL string) *Client {
	return &Client{
		baseURL: strings.TrimRight(baseURL, "/"),
		http: &http.Client{
			Timeout: 15 * time.Second,
		},
	}
}

func (c *Client) Register(ctx context.Context, req model.RegisterRequest) (*model.RegisterResponse, error) {
	var data model.RegisterResponse
	if err := c.post(ctx, "/api/agent/register", req, &data); err != nil {
		return nil, err
	}
	return &data, nil
}

func (c *Client) Heartbeat(ctx context.Context, req model.HeartbeatRequest) error {
	return c.post(ctx, "/api/agent/heartbeat", req, nil)
}

func (c *Client) PullTask(ctx context.Context, req model.PullTaskRequest) (*model.PullTaskResponse, error) {
	var data model.PullTaskResponse
	if err := c.post(ctx, "/api/agent/task", req, &data); err != nil {
		if err == errEmptyData {
			return nil, nil
		}
		return nil, err
	}
	if data.ID == 0 {
		return nil, nil
	}
	return &data, nil
}

func (c *Client) ReportTaskResult(ctx context.Context, taskID model.Long, req model.TaskResultRequest) error {
	return c.post(ctx, fmt.Sprintf("/api/agent/task/%d/result", taskID), req, nil)
}

func (c *Client) ReportLog(ctx context.Context, req model.LogRequest) error {
	return c.post(ctx, "/api/agent/log", req, nil)
}

var errEmptyData = fmt.Errorf("empty response data")

func (c *Client) post(ctx context.Context, path string, payload any, out any) error {
	body, err := json.Marshal(payload)
	if err != nil {
		return fmt.Errorf("marshal request: %w", err)
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.baseURL+path, bytes.NewReader(body))
	if err != nil {
		return fmt.Errorf("build request: %w", err)
	}
	req.Header.Set("Content-Type", "application/json")
	resp, err := c.http.Do(req)
	if err != nil {
		return fmt.Errorf("do request: %w", err)
	}
	defer resp.Body.Close()
	respBody, err := io.ReadAll(resp.Body)
	if err != nil {
		return fmt.Errorf("read response: %w", err)
	}
	if resp.StatusCode >= 400 {
		return fmt.Errorf("http %d: %s", resp.StatusCode, string(respBody))
	}
	if out == nil {
		return nil
	}
	var wrapper struct {
		Code    int             `json:"code"`
		Message string          `json:"message"`
		Data    json.RawMessage `json:"data"`
	}
	if err := json.Unmarshal(respBody, &wrapper); err != nil {
		return fmt.Errorf("decode response wrapper: %w", err)
	}
	if wrapper.Code != 200 {
		return fmt.Errorf("api error: %s", wrapper.Message)
	}
	if len(wrapper.Data) == 0 || string(wrapper.Data) == "null" {
		return errEmptyData
	}
	if err := json.Unmarshal(wrapper.Data, out); err != nil {
		return fmt.Errorf("decode response data: %w", err)
	}
	return nil
}
