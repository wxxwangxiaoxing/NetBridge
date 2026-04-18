# NetBridge 前端联调契约（当前版本）

## 1. 通用返回结构

### 普通接口

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 失败接口

```json
{
  "code": 404,
  "message": "task not found",
  "data": null
}
```

### 当前通用错误码约定

- `200`: 成功
- `400`: 请求错误
- `401`: 未登录或 token 无效
- `403`: 无权限
- `404`: 资源不存在
- `409`: 状态冲突或重复操作
- `422`: 参数校验失败
- `500`: 服务内部错误

### 分页接口

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "pageNo": 1,
    "pageSize": 10,
    "total": 100,
    "list": []
  }
}
```

## 2. 已统一为分页结构的接口

- `GET /api/server/list`
- `GET /api/service/list`
- `GET /api/task/list`
- `GET /api/log/list`
- `GET /api/monitor/server/list`
- `GET /api/monitor/service/list`

## 3. 服务器详情接口

### `GET /api/server/{id}/detail`

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "userId": 1,
    "hostname": "dev-server",
    "ip": "1.2.3.4",
    "tailscaleIp": "100.64.0.2",
    "status": "online",
    "serviceTotal": 3,
    "services": [],
    "recentTaskTotal": 12,
    "recentTasks": [],
    "recentLogTotal": 45,
    "recentLogs": []
  }
}
```

### 前端可直接使用字段

- `serviceTotal`
- `recentTaskTotal`
- `recentLogTotal`
- `services`
- `recentTasks`
- `recentLogs`

## 4. 服务详情接口

### `GET /api/service/{id}/detail`

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "serverId": 1,
    "name": "mysql-main",
    "type": "MySQL",
    "protocol": "TCP",
    "port": 3306,
    "targetHost": "127.0.0.1",
    "accessType": "tailscale",
    "status": "active",
    "domain": null,
    "accessUrl": "mysql -h 100.64.0.2 -P 3306 -u root -p",
    "healthStatus": "healthy",
    "recentTaskTotal": 8,
    "recentTasks": [],
    "recentLogTotal": 20,
    "recentLogs": [],
    "metrics": []
  }
}
```

### 前端可直接使用字段

- `accessUrl`
- `healthStatus`
- `recentTaskTotal`
- `recentLogTotal`
- `metrics`

## 5. 任务接口重点字段

### `GET /api/task/list`

每条任务同时保留原始 JSON 字段和结构化字段：

- `payload`
- `payloadObj`
- `result`
- `resultObj`

### `payloadObj`

```json
{
  "serviceId": 1,
  "serverId": 1,
  "action": "CHECK",
  "serviceName": "mysql-main",
  "port": 3306,
  "protocol": "TCP",
  "accessType": "tailscale",
  "targetHost": "127.0.0.1",
  "domain": null
}
```

### `resultObj`

```json
{
  "success": true,
  "accessUrl": "mysql -h 100.64.0.2 -P 3306 -u root -p",
  "endpoint": "100.64.0.2:3306",
  "message": "check success",
  "serviceStatus": "active",
  "portStatus": "open",
  "tunnelStatus": "inactive",
  "tailscaleStatus": "connected",
  "serviceAction": {
    "component": "service",
    "action": "observe",
    "status": "active",
    "detail": "service mysql-main observed as active"
  },
  "tunnelAction": {
    "component": "cloudflared",
    "action": "observe",
    "mode": "service-config",
    "status": "inactive",
    "detail": "cloudflared is installed but not running"
  },
  "tailscaleAction": {
    "component": "tailscale",
    "action": "observe",
    "mode": "serve-http",
    "status": "connected",
    "detail": "tailscale is reachable"
  },
  "rollbackApplied": false,
  "responseTimeMs": 12,
  "successCount": 1,
  "errorCount": 0,
  "checkedAt": "2026-04-17T10:00:00"
}
```

### 当前推荐展示优先级

- 访问地址：
  优先用 `resultObj.accessUrl`
  兜底用 `resultObj.endpoint`

- 连通性检测展示：
  - `resultObj.serviceStatus`
  - `resultObj.portStatus`
  - `resultObj.tunnelStatus`
  - `resultObj.tailscaleStatus`
  - `resultObj.responseTimeMs`

- 任务执行链展示：
  - `resultObj.serviceAction`
  - `resultObj.tailscaleAction`
  - `resultObj.tunnelAction`
  - `resultObj.rollbackApplied`

## 6. 日志接口重点字段

### `GET /api/log/list`

每条日志包含：

- `id`
- `serverId`
- `serviceId`
- `taskId`
- `agentId`
- `level`
- `source`
- `content`
- `createdAt`

## 7. 监控接口重点字段

### 服务器监控

- `GET /api/monitor/server/list`
- `GET /api/monitor/server/{id}`

字段：

- `serverId`
- `cpuUsage`
- `memoryUsage`
- `diskUsage`
- `networkInBytes`
- `networkOutBytes`
- `loadAverage`
- `collectedAt`

### 服务监控

- `GET /api/monitor/service/list`
- `GET /api/monitor/service/{id}`

字段：

- `serviceId`
- `serverId`
- `status`
- `responseTimeMs`
- `successCount`
- `errorCount`
- `collectedAt`

## 8. 当前约定说明

- 当前接口以“前端可直接渲染”为优先
- 任务仍保留 `payload/result` 原始 JSON 字段，便于排错
- 结构化字段优先用于业务展示
- 如后续前端确定字段名称，不建议频繁改动同义字段

## 9. WebSocket 联调约定

### 连接端点

- `GET /ws`

### 认证方式

- STOMP `CONNECT` 时必须带 JWT
- 推荐放在连接头：

```json
{
  "Authorization": "Bearer <jwt-token>"
}
```

- 也兼容：

```json
{
  "token": "<jwt-token>"
}
```

### 用户隔离订阅地址

- `/user/queue/server/status`
- `/user/queue/task/status`
- `/user/queue/service/status`

### 订阅限制

- 当前只允许订阅上面 3 个用户队列
- 不应再订阅公共 `/topic/...` 地址
- 未携带 JWT 或订阅未授权地址时，服务端会拒绝连接或订阅

### 消息基础结构

```json
{
  "type": "task_status",
  "entityId": 101,
  "serverId": 1,
  "serviceId": 2,
  "taskId": 101,
  "status": "running",
  "healthStatus": null,
  "message": "task claimed by agent",
  "timestamp": "2026-04-18T10:00:00"
}
```

### 当前推送范围

- 服务器状态：
  - Agent 注册
  - Agent 心跳
- 任务状态：
  - 创建
  - 领取
  - 更新
  - 重试
  - 取消
  - 超时
  - 结果回传
- 服务状态：
  - 创建
  - 更新
  - 删除
  - 任务联动后的状态变化
