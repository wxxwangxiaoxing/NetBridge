# NetBridge API 接口设计文档

## 1. 接口概述

### 1.1 接口风格
- 采用 RESTful API 设计风格
- 使用 JSON 格式进行数据交换
- 标准 HTTP 状态码

### 1.2 认证方式
- 使用 JWT (JSON Web Token) 进行身份认证
- Token 有效期为 24 小时
- 每次请求需要在请求头中携带 `Authorization: Bearer {token}`

### 1.3 基础路径
- 所有 API 接口的基础路径为 `/api`

## 2. 用户系统接口

### 2.1 用户注册

**请求**
- 路径: `/api/user/register`
- 方法: `POST`
- 参数:
  | 字段名 | 类型 | 必填 | 描述 |
  | :--- | :--- | :--- | :--- |
  | `username` | `string` | 是 | 用户名 |
  | `password` | `string` | 是 | 密码 |
  | `email` | `string` | 是 | 邮箱 |

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "注册成功",
    "data": {
      "id": 1,
      "username": "user1",
      "email": "user1@example.com"
    }
  }
  ```
- 失败:
  ```json
  {
    "code": 400,
    "message": "用户名已存在"
  }
  ```

### 2.2 用户登录

**请求**
- 路径: `/api/user/login`
- 方法: `POST`
- 参数:
  | 字段名 | 类型 | 必填 | 描述 |
  | :--- | :--- | :--- | :--- |
  | `username` | `string` | 是 | 用户名 |
  | `password` | `string` | 是 | 密码 |

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "登录成功",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "user": {
        "id": 1,
        "username": "user1",
        "email": "user1@example.com",
        "role": "user"
      }
    }
  }
  ```
- 失败:
  ```json
  {
    "code": 401,
    "message": "用户名或密码错误"
  }
  ```

### 2.3 获取用户信息

**请求**
- 路径: `/api/user/info`
- 方法: `GET`
- 认证: 需要 JWT Token

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "获取成功",
    "data": {
      "id": 1,
      "username": "user1",
      "email": "user1@example.com",
      "role": "user",
      "status": 1,
      "created_at": "2023-01-01T00:00:00Z"
    }
  }
  ```

## 3. 服务器管理接口

### 3.1 获取服务器列表

**请求**
- 路径: `/api/server/list`
- 方法: `GET`
- 认证: 需要 JWT Token
- 参数:
  | 字段名 | 类型 | 必填 | 描述 |
  | :--- | :--- | :--- | :--- |
  | `page` | `number` | 否 | 页码，默认 1 |
  | `size` | `number` | 否 | 每页数量，默认 10 |

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "获取成功",
    "data": {
      "list": [
        {
          "id": 1,
          "hostname": "server-1",
          "os": "Ubuntu 20.04",
          "ip": "1.2.3.4",
          "tailscale_ip": "100.100.100.1",
          "status": "online",
          "agent_version": "1.0.0",
          "cpu": "4 cores",
          "memory": "8GB",
          "disk": "100GB",
          "last_heartbeat": "2023-01-01T00:00:00Z",
          "created_at": "2023-01-01T00:00:00Z"
        }
      ],
      "total": 1,
      "page": 1,
      "size": 10
    }
  }
  ```

### 3.2 生成服务器安装命令

**请求**
- 路径: `/api/server/generate-command`
- 方法: `POST`
- 认证: 需要 JWT Token
- 参数:
  | 字段名 | 类型 | 必填 | 描述 |
  | :--- | :--- | :--- | :--- |
  | `hostname` | `string` | 是 | 服务器主机名 |

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "生成成功",
    "data": {
      "command": "curl -fsSL https://netbridge.com/install.sh | bash -s -- --token=xxxxx",
      "token": "xxxxx",
      "expires_at": "2023-01-01T00:05:00Z"
    }
  }
  ```

### 3.3 获取服务器详情

**请求**
- 路径: `/api/server/{id}`
- 方法: `GET`
- 认证: 需要 JWT Token

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "获取成功",
    "data": {
      "id": 1,
      "hostname": "server-1",
      "os": "Ubuntu 20.04",
      "ip": "1.2.3.4",
      "tailscale_ip": "100.100.100.1",
      "status": "online",
      "agent_version": "1.0.0",
      "cpu": "4 cores",
      "memory": "8GB",
      "disk": "100GB",
      "last_heartbeat": "2023-01-01T00:00:00Z",
      "created_at": "2023-01-01T00:00:00Z",
      "services": [
        {
          "id": 1,
          "name": "MySQL",
          "type": "MySQL",
          "protocol": "TCP",
          "port": 3306,
          "status": "active",
          "access_type": "tailscale"
        }
      ]
    }
  }
  ```

### 3.4 删除服务器

**请求**
- 路径: `/api/server/{id}`
- 方法: `DELETE`
- 认证: 需要 JWT Token

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "删除成功"
  }
  ```

### 3.5 重命名服务器

**请求**
- 路径: `/api/server/{id}/rename`
- 方法: `PUT`
- 认证: 需要 JWT Token
- 参数:
  | 字段名 | 类型 | 必填 | 描述 |
  | :--- | :--- | :--- | :--- |
  | `hostname` | `string` | 是 | 新的主机名 |

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "重命名成功",
    "data": {
      "id": 1,
      "hostname": "new-hostname"
    }
  }
  ```

## 4. 服务管理接口

### 4.1 获取服务列表

**请求**
- 路径: `/api/service/list`
- 方法: `GET`
- 认证: 需要 JWT Token
- 参数:
  | 字段名 | 类型 | 必填 | 描述 |
  | :--- | :--- | :--- | :--- |
  | `server_id` | `number` | 否 | 服务器ID |
  | `page` | `number` | 否 | 页码，默认 1 |
  | `size` | `number` | 否 | 每页数量，默认 10 |

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "获取成功",
    "data": {
      "list": [
        {
          "id": 1,
          "server_id": 1,
          "name": "MySQL",
          "type": "MySQL",
          "protocol": "TCP",
          "port": 3306,
          "domain": null,
          "enabled": 1,
          "status": "active",
          "access_type": "tailscale",
          "created_at": "2023-01-01T00:00:00Z"
        }
      ],
      "total": 1,
      "page": 1,
      "size": 10
    }
  }
  ```

### 4.2 添加服务

**请求**
- 路径: `/api/service/add`
- 方法: `POST`
- 认证: 需要 JWT Token
- 参数:
  | 字段名 | 类型 | 必填 | 描述 |
  | :--- | :--- | :--- | :--- |
  | `server_id` | `number` | 是 | 服务器ID |
  | `name` | `string` | 是 | 服务名称 |
  | `type` | `string` | 是 | 服务类型（MySQL/Redis/Nacos/Web/SSH） |
  | `protocol` | `string` | 是 | 协议（TCP/HTTP） |
  | `port` | `number` | 是 | 服务端口 |
  | `domain` | `string` | 否 | 绑定域名（HTTP服务） |
  | `access_type` | `string` | 是 | 访问类型（tailscale/tunnel） |

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "添加成功",
    "data": {
      "id": 1,
      "server_id": 1,
      "name": "MySQL",
      "type": "MySQL",
      "protocol": "TCP",
      "port": 3306,
      "status": "inactive"
    }
  }
  ```

### 4.3 启动服务

**请求**
- 路径: `/api/service/{id}/start`
- 方法: `POST`
- 认证: 需要 JWT Token

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "启动成功",
    "data": {
      "id": 1,
      "status": "active",
      "access_url": "mysql -h 100.100.100.1 -P 3306 -u root -p"
    }
  }
  ```

### 4.4 停止服务

**请求**
- 路径: `/api/service/{id}/stop`
- 方法: `POST`
- 认证: 需要 JWT Token

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "停止成功",
    "data": {
      "id": 1,
      "status": "inactive"
    }
  }
  ```

### 4.5 删除服务

**请求**
- 路径: `/api/service/{id}`
- 方法: `DELETE`
- 认证: 需要 JWT Token

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "删除成功"
  }
  ```

### 4.6 检测服务连通性

**请求**
- 路径: `/api/service/{id}/check`
- 方法: `GET`
- 认证: 需要 JWT Token

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "检测成功",
    "data": {
      "id": 1,
      "port_status": "open",
      "tunnel_status": "active",
      "tailscale_status": "connected"
    }
  }
  ```

## 5. Agent 接口

### 5.1 Agent 注册

**请求**
- 路径: `/api/agent/register`
- 方法: `POST`
- 参数:
  | 字段名 | 类型 | 必填 | 描述 |
  | :--- | :--- | :--- | :--- |
  | `token` | `string` | 是 | 注册令牌 |
  | `hostname` | `string` | 是 | 主机名 |
  | `os` | `string` | 是 | 操作系统 |
  | `ip` | `string` | 是 | 公网IP |
  | `agent_version` | `string` | 是 | Agent版本 |
  | `cpu` | `string` | 是 | CPU信息 |
  | `memory` | `string` | 是 | 内存信息 |
  | `disk` | `string` | 是 | 磁盘信息 |

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "注册成功",
    "data": {
      "server_id": 1,
      "agent_id": "agent-001",
      "tailscale_auth_key": "tskey-auth-xxx",
      "cloudflare_token": "xxx"
    }
  }
  ```

### 5.2 Agent 心跳

**请求**
- 路径: `/api/agent/heartbeat`
- 方法: `POST`
- 参数:
  | 字段名 | 类型 | 必填 | 描述 |
  | :--- | :--- | :--- | :--- |
  | `agent_id` | `string` | 是 | Agent ID |
  | `server_id` | `number` | 是 | 服务器ID |
  | `status` | `string` | 是 | 状态（online/offline/error） |
  | `tailscale_ip` | `string` | 是 | Tailscale IP |

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "心跳成功"
  }
  ```

### 5.3 Agent 拉取任务

**请求**
- 路径: `/api/agent/task`
- 方法: `GET`
- 参数:
  | 字段名 | 类型 | 必填 | 描述 |
  | :--- | :--- | :--- | :--- |
  | `agent_id` | `string` | 是 | Agent ID |
  | `server_id` | `number` | 是 | 服务器ID |

**响应**
- 有任务:
  ```json
  {
    "code": 200,
    "message": "获取任务成功",
    "data": {
      "id": 1,
      "type": "START",
      "payload": {
        "service_id": 1,
        "service_name": "MySQL",
        "port": 3306
      }
    }
  }
  ```
- 无任务:
  ```json
  {
    "code": 200,
    "message": "无任务",
    "data": null
  }
  ```

### 5.4 Agent 上报任务结果

**请求**
- 路径: `/api/agent/task/{id}/result`
- 方法: `POST`
- 参数:
  | 字段名 | 类型 | 必填 | 描述 |
  | :--- | :--- | :--- | :--- |
  | `agent_id` | `string` | 是 | Agent ID |
  | `status` | `string` | 是 | 任务状态（success/failed） |
  | `result` | `object` | 否 | 任务结果 |
  | `error_message` | `string` | 否 | 错误信息 |

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "上报成功"
  }
  ```

### 5.5 Agent 上报日志

**请求**
- 路径: `/api/agent/log`
- 方法: `POST`
- 参数:
  | 字段名 | 类型 | 必填 | 描述 |
  | :--- | :--- | :--- | :--- |
  | `agent_id` | `string` | 是 | Agent ID |
  | `server_id` | `number` | 是 | 服务器ID |
  | `level` | `string` | 是 | 日志级别（INFO/WARN/ERROR/DEBUG） |
  | `content` | `string` | 是 | 日志内容 |
  | `source` | `string` | 是 | 日志来源（agent/tunnel/system） |

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "上报成功"
  }
  ```

## 6. 任务管理接口

### 6.1 获取任务列表

**请求**
- 路径: `/api/task/list`
- 方法: `GET`
- 认证: 需要 JWT Token
- 参数:
  | 字段名 | 类型 | 必填 | 描述 |
  | :--- | :--- | :--- | :--- |
  | `server_id` | `number` | 否 | 服务器ID |
  | `status` | `string` | 否 | 任务状态 |
  | `page` | `number` | 否 | 页码，默认 1 |
  | `size` | `number` | 否 | 每页数量，默认 10 |

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "获取成功",
    "data": {
      "list": [
        {
          "id": 1,
          "server_id": 1,
          "type": "START",
          "status": "success",
          "created_at": "2023-01-01T00:00:00Z",
          "completed_at": "2023-01-01T00:00:05Z"
        }
      ],
      "total": 1,
      "page": 1,
      "size": 10
    }
  }
  ```

### 6.2 获取任务详情

**请求**
- 路径: `/api/task/{id}`
- 方法: `GET`
- 认证: 需要 JWT Token

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "获取成功",
    "data": {
      "id": 1,
      "server_id": 1,
      "type": "START",
      "status": "success",
      "payload": {
        "service_id": 1,
        "service_name": "MySQL",
        "port": 3306
      },
      "result": {
        "access_url": "mysql -h 100.100.100.1 -P 3306 -u root -p"
      },
      "created_at": "2023-01-01T00:00:00Z",
      "completed_at": "2023-01-01T00:00:05Z"
    }
  }
  ```

## 7. 监控接口

### 7.1 获取服务器监控数据

**请求**
- 路径: `/api/monitor/server/{id}`
- 方法: `GET`
- 认证: 需要 JWT Token
- 参数:
  | 字段名 | 类型 | 必填 | 描述 |
  | :--- | :--- | :--- | :--- |
  | `time_range` | `string` | 否 | 时间范围（1h/1d/1w/1m） |

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "获取成功",
    "data": {
      "cpu": [
        {"time": "2023-01-01T00:00:00Z", "value": 10},
        {"time": "2023-01-01T00:05:00Z", "value": 15}
      ],
      "memory": [
        {"time": "2023-01-01T00:00:00Z", "value": 50},
        {"time": "2023-01-01T00:05:00Z", "value": 55}
      ],
      "disk": [
        {"time": "2023-01-01T00:00:00Z", "value": 30},
        {"time": "2023-01-01T00:05:00Z", "value": 30}
      ]
    }
  }
  ```

### 7.2 获取服务监控数据

**请求**
- 路径: `/api/monitor/service/{id}`
- 方法: `GET`
- 认证: 需要 JWT Token
- 参数:
  | 字段名 | 类型 | 必填 | 描述 |
  | :--- | :--- | :--- | :--- |
  | `time_range` | `string` | 否 | 时间范围（1h/1d/1w/1m） |

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "获取成功",
    "data": {
      "status": [
        {"time": "2023-01-01T00:00:00Z", "value": 1},
        {"time": "2023-01-01T00:05:00Z", "value": 1}
      ],
      "response_time": [
        {"time": "2023-01-01T00:00:00Z", "value": 10},
        {"time": "2023-01-01T00:05:00Z", "value": 12}
      ]
    }
  }
  ```

## 8. 日志接口

### 8.1 获取日志列表

**请求**
- 路径: `/api/log/list`
- 方法: `GET`
- 认证: 需要 JWT Token
- 参数:
  | 字段名 | 类型 | 必填 | 描述 |
  | :--- | :--- | :--- | :--- |
  | `server_id` | `number` | 否 | 服务器ID |
  | `service_id` | `number` | 否 | 服务ID |
  | `level` | `string` | 否 | 日志级别 |
  | `source` | `string` | 否 | 日志来源 |
  | `page` | `number` | 否 | 页码，默认 1 |
  | `size` | `number` | 否 | 每页数量，默认 10 |

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "获取成功",
    "data": {
      "list": [
        {
          "id": 1,
          "server_id": 1,
          "service_id": 1,
          "level": "INFO",
          "content": "Service MySQL started successfully",
          "source": "agent",
          "created_at": "2023-01-01T00:00:00Z"
        }
      ],
      "total": 1,
      "page": 1,
      "size": 10
    }
  }
  ```

### 8.2 获取操作日志

**请求**
- 路径: `/api/log/operation`
- 方法: `GET`
- 认证: 需要 JWT Token
- 参数:
  | 字段名 | 类型 | 必填 | 描述 |
  | :--- | :--- | :--- | :--- |
  | `user_id` | `number` | 否 | 用户ID |
  | `action` | `string` | 否 | 操作类型 |
  | `target_type` | `string` | 否 | 目标类型 |
  | `page` | `number` | 否 | 页码，默认 1 |
  | `size` | `number` | 否 | 每页数量，默认 10 |

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "获取成功",
    "data": {
      "list": [
        {
          "id": 1,
          "user_id": 1,
          "action": "add_service",
          "target_type": "service",
          "target_id": 1,
          "details": {"service_name": "MySQL"},
          "ip": "1.2.3.4",
          "created_at": "2023-01-01T00:00:00Z"
        }
      ],
      "total": 1,
      "page": 1,
      "size": 10
    }
  }
  ```

## 9. 配置接口

### 9.1 获取系统配置

**请求**
- 路径: `/api/config`
- 方法: `GET`
- 认证: 需要 JWT Token

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "获取成功",
    "data": {
      "tailscale_auth_key": "tskey-auth-xxx",
      "cloudflare_token": "xxx",
      "domain_suffix": "netbridge.com"
    }
  }
  ```

### 9.2 更新系统配置

**请求**
- 路径: `/api/config`
- 方法: `PUT`
- 认证: 需要 JWT Token
- 参数:
  | 字段名 | 类型 | 必填 | 描述 |
  | :--- | :--- | :--- | :--- |
  | `key` | `string` | 是 | 配置键 |
  | `value` | `string` | 是 | 配置值 |

**响应**
- 成功:
  ```json
  {
    "code": 200,
    "message": "更新成功",
    "data": {
      "key": "tailscale_auth_key",
      "value": "tskey-auth-xxx"
    }
  }
  ```

## 10. WebSocket 接口

### 10.1 服务器状态实时更新

**连接路径**
- `/ws/server/status`
- 需要在查询参数中携带 JWT Token: `?token={token}`

**消息格式**
- 服务端推送:
  ```json
  {
    "type": "server_status",
    "data": {
      "server_id": 1,
      "status": "online",
      "last_heartbeat": "2023-01-01T00:00:00Z"
    }
  }
  ```

### 10.2 任务状态实时更新

**连接路径**
- `/ws/task/status`
- 需要在查询参数中携带 JWT Token: `?token={token}`

**消息格式**
- 服务端推送:
  ```json
  {
    "type": "task_status",
    "data": {
      "task_id": 1,
      "status": "success",
      "completed_at": "2023-01-01T00:00:05Z"
    }
  }
  ```

### 10.3 服务状态实时更新

**连接路径**
- `/ws/service/status`
- 需要在查询参数中携带 JWT Token: `?token={token}`

**消息格式**
- 服务端推送:
  ```json
  {
    "type": "service_status",
    "data": {
      "service_id": 1,
      "status": "active",
      "access_url": "mysql -h 100.100.100.1 -P 3306 -u root -p"
    }
  }
  ```

## 11. 错误码定义

| 错误码 | 描述 | HTTP状态码 |
| :--- | :--- | :--- |
| 200 | 成功 | 200 |
| 400 | 请求参数错误 | 400 |
| 401 | 未授权 | 401 |
| 403 | 禁止访问 | 403 |
| 404 | 资源不存在 | 404 |
| 500 | 服务器内部错误 | 500 |
| 501 | 服务暂不可用 | 503 |

## 12. 接口版本管理

- 接口版本通过 URL 路径进行管理，如 `/api/v1/user/login`
- 本文档描述的是 v1 版本接口
- 版本升级时，会保持向后兼容

## 13. 接口安全

- 所有接口均使用 HTTPS 加密传输
- 敏感接口需要进行 CSRF 防护
- 接口访问频率限制
- 输入参数验证
- 输出数据脱敏