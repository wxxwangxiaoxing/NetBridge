# 🚀 NetBridge（内网穿透管理平台）PRD

---

# 🎯 一、项目定位

## 📌 产品定义

NetBridge 是一个：

> 👉 **基于 Agent 的零信任内网穿透与远程运维平台**

融合：

* Tailscale（内网访问）
* Cloudflare Tunnel（公网访问）

实现：

> 用户无需复杂配置，一条命令即可完成内网服务公网/私网访问

### 核心价值主张

- **简单易用**：一键安装，无需复杂网络配置
- **安全可靠**：零信任架构，不收集服务器密码
- **功能强大**：支持 HTTP 和 TCP 服务，满足多种场景需求
- **实时监控**：提供服务器和服务的实时状态监控
- **自动化运维**：减少人工干预，提高运维效率

---

## 🎯 目标用户

| 用户类型 | 使用场景 | 痛点 | 解决方案 |
| ---- | -------------------------- | ---- | ---- |
| 开发者 | 远程访问 MySQL / Redis / Nacos | 无法远程访问内网开发环境 | 通过 Tailscale 实现安全内网访问 |
| 运维人员 | 管理多台服务器 | 服务器分散，管理困难 | 集中管理平台，实时监控 |
| 小团队 | 内网系统公网暴露 | 没有公网 IP，无法外部访问 | 通过 Cloudflare Tunnel 实现公网访问 |
| 企业 | 零信任访问入口 | 传统 VPN 配置复杂，安全性低 | 零信任架构，最小权限原则 |

---

## 🎯 产品目标（核心指标）

* 🚀 5分钟完成部署
* 🔐 无 root 密码收集
* 🌐 支持 HTTP + TCP 服务
* 📊 实时监控
* 🔁 自动化运维
* 📈 99.9% 服务可用性
* 🔒 企业级安全标准

---

# 🧠 二、核心设计原则

* ❌ 不收集服务器密码
* ✅ Agent 主动注册
* ✅ 最小权限原则
* ✅ 自动化优先
* ✅ 可扩展架构
* ✅ 安全第一
* ✅ 用户体验优先

---

# 🏗 三、总体架构设计

```text
用户浏览器
    │
    ▼
管理控制台（Web）
SpringBoot + Vue3
    │
    ▼
API网关 / WebSocket
    │
    ▼
Agent（Go）
    │
 ┌──┴──────────────┐
 │                 │
▼                 ▼
Tailscale       Cloudflare Tunnel
 │                 │
 └──────→ 内网服务 ←──────┘
```

### 架构说明

1. **管理控制台**：用户交互界面，提供服务器管理、服务配置、监控等功能
2. **API网关**：处理所有 API 请求，提供 WebSocket 实时通信
3. **Agent**：部署在目标服务器上的执行引擎，负责执行任务、上报状态
4. **Tailscale**：提供安全的内网访问能力
5. **Cloudflare Tunnel**：提供公网访问能力

---

# 🧩 四、核心功能模块（完整版）

---

## 1️⃣ 用户系统

### 功能：

* 注册 / 登录
* JWT 鉴权
* 多设备登录
* 多租户（预留）
* 密码重置
* 个人信息管理

### 流程：

1. 用户注册：填写用户名、密码、邮箱
2. 邮箱验证：发送验证邮件
3. 用户登录：使用用户名和密码登录
4. 生成 JWT Token：用于后续 API 请求认证

---

## 2️⃣ 服务器管理

### 功能：

* 添加服务器（生成安装命令）
* 在线状态（WebSocket实时）
* 删除 / 重命名
* 系统信息展示
* 服务器分组管理

### 状态定义：

| 状态      | 含义 | UI 展示 |
| ------- | -- | ---- |
| online  | 在线 | 🟢 绿色 |
| offline | 离线 | 🔴 红色 |
| error   | 异常 | 🟡 黄色 |

### 服务器信息展示：

* 主机名
* 操作系统
* 公网 IP
* Tailscale IP
* CPU 使用率
* 内存使用率
* 磁盘使用率
* Agent 版本
* 最后心跳时间

---

## 3️⃣ 一键安装引导（核心）

### 用户操作：

```bash
curl -fsSL https://netbridge.com/install.sh | bash -s -- --token=xxxxx
```

### 安装流程：

```text
1. 校验权限：检查是否具有 sudo 权限
2. 下载 Agent：从官方服务器下载最新版本
3. 注册服务器：向管理平台注册服务器信息
4. 加入 Tailscale：使用 Auth Key 加入 Tailscale 网络
5. 启动 Agent：设置为系统服务并启动
6. 返回结果：显示安装状态和结果
```

### 安装结果反馈（新增）

| 状态 | 展示 | 操作建议 |
| -- | -------- | ---- |
| 成功 | ✅ 绿色对勾 | 点击「查看服务器」进入详情页 |
| 失败 | ❌ 红色叉号 + 错误日志 | 根据错误信息排查问题后重试 |

---

## 4️⃣ 服务映射管理（核心模块）

### 支持类型：

| 类型      | 协议   | 说明        | 访问方式 |
| ------- | ---- | --------- | ---- |
| MySQL   | TCP  | 内网访问      | Tailscale |
| Redis   | TCP  | 内网访问      | Tailscale |
| Nacos   | HTTP | 公网访问      | Cloudflare Tunnel |
| Web App | HTTP | 公网访问      | Cloudflare Tunnel |
| SSH     | TCP  | 内网访问      | Tailscale |
| PostgreSQL | TCP | 内网访问 | Tailscale |
| MongoDB | TCP | 内网访问 | Tailscale |
| FTP     | TCP | 内网访问 | Tailscale |

### 核心能力：

* 添加服务：配置服务名称、类型、端口等信息
* 自动检测端口：检测端口是否可用
* 一键启停：方便管理服务状态
* 自动生成访问地址：根据服务类型生成相应的访问命令或 URL
* 服务分组：按功能或环境对服务进行分组

---

## 5️⃣ 服务访问层（增强）

👉 **关键产品能力（区别于普通工具）**

### 功能：

| 类型   | 行为       | 示例 |
| ---- | -------- | ---- |
| HTTP | 点击直接访问   | 点击链接打开 Web 应用 |
| TCP  | 自动生成连接命令 | `mysql -h 100.x.x.x -P 3306 -u root -p` |

### 访问安全：

* HTTP 服务：支持 HTTPS 加密
* TCP 服务：通过 Tailscale 安全网络访问
* 访问控制：基于用户权限的访问控制

---

## 6️⃣ 连通性检测（新增核心）

### 检测内容：

* 端口是否监听：检查服务端口是否正常开放
* Tunnel 是否正常：检查 Cloudflare Tunnel 连接状态
* Tailscale 状态：检查 Tailscale 网络连接状态
* 服务响应时间：检测服务响应速度

### 状态展示：

| 状态 | UI | 含义 |
| -- | -- | -- |
| 正常 | 🟢 绿色 | 所有检测项正常 |
| 异常 | 🔴 红色 | 至少一项检测项异常 |
| 警告 | 🟡 黄色 | 服务可访问但存在潜在问题 |

### 检测频率：

* 自动检测：每 5 分钟自动检测一次
* 手动检测：用户可手动触发检测

---

## 7️⃣ Tunnel 管理（自动化）

### 功能：

* 自动创建 Tunnel：无需用户登录 Cloudflare 控制台
* 自动绑定域名：支持自定义域名或使用默认域名
* 自动生成配置：根据服务类型生成最优配置
* 启停控制：通过管理控制台控制 Tunnel 状态
* 证书管理：自动处理 HTTPS 证书

### 域名管理：

* 支持自定义域名：用户可绑定自己的域名
* 支持子域名：自动生成子域名
* 域名验证：自动完成域名所有权验证

---

## 8️⃣ Tailscale 管理

### 功能：

* 自动加入网络：使用 Auth Key 自动加入 Tailscale 网络
* 显示 IP：展示服务器的 Tailscale IP 地址
* MagicDNS 支持：启用 MagicDNS 简化访问
* 网络策略：配置访问控制策略
* 设备管理：管理 Tailscale 网络中的设备

---

## 9️⃣ 监控系统

### 指标：

* **服务器指标**：
  - CPU 使用率
  - 内存使用率
  - 磁盘使用率
  - 网络流量
  - 负载情况

* **服务指标**：
  - 服务状态
  - 响应时间
  - 访问次数
  - 错误率

* **Agent 指标**：
  - Agent 状态
  - 心跳频率
  - 任务执行情况

### 监控界面：

* 实时仪表盘：展示关键指标
* 历史趋势：查看过去 24 小时、7 天、30 天的指标趋势
* 告警设置：设置指标阈值，超过阈值时触发告警

---

## 🔟 日志系统

### 类型：

* **Agent 日志**：记录 Agent 运行状态和执行情况
* **Tunnel 日志**：记录 Cloudflare Tunnel 的运行状态
* **操作日志**：记录用户在管理控制台的操作
* **服务日志**：记录服务的访问和错误信息

### 功能：

* 日志查询：支持按时间、级别、来源等条件查询
* 日志导出：支持导出日志为 CSV 或 JSON 格式
* 日志分析：自动分析日志中的错误和异常

---

## 1️⃣1️⃣ 任务调度系统（关键）

### 任务类型：

* 安装：安装 Agent 和相关组件
* 启动服务：启动指定服务
* 停止服务：停止指定服务
* 重启服务：重启指定服务
* 删除服务：删除指定服务
* 重启服务器：重启目标服务器
* 执行命令：在服务器上执行指定命令

### 状态流：

```text
pending → running → success / failed
```

### 任务管理：

* 任务队列：管理待执行的任务
* 任务历史：查看历史任务执行情况
* 任务详情：查看任务执行的详细步骤和结果

---

# 🤖 五、Agent 设计（核心）

## 架构定位

👉 Agent = **受控执行引擎**

### 设计理念

* 轻量级：最小化资源占用
* 安全：严格的命令执行控制
* 可靠：自动重连和故障恢复
* 高效：异步执行任务

---

## 核心能力

* 注册：向管理平台注册服务器信息
* 心跳：定期向管理平台发送心跳，保持连接
* 拉取任务：从管理平台拉取待执行的任务
* 执行任务：执行指定的任务，如启动服务、重启等
* 上报日志：向管理平台上报运行日志
* 状态监控：监控服务器和服务状态

---

## API 设计

### 注册

```http
POST /api/agent/register
Content-Type: application/json

{
  "token": "xxxxx",
  "hostname": "server-1",
  "os": "Ubuntu 20.04",
  "ip": "1.2.3.4",
  "agent_version": "1.0.0",
  "cpu": "4 cores",
  "memory": "8GB",
  "disk": "100GB"
}
```

### 心跳

```http
POST /api/agent/heartbeat
Content-Type: application/json

{
  "agent_id": "agent-001",
  "server_id": 1,
  "status": "online",
  "tailscale_ip": "100.100.100.1",
  "cpu_usage": 10,
  "memory_usage": 50,
  "disk_usage": 30
}
```

### 拉任务

```http
GET /api/agent/task?agent_id=agent-001&server_id=1
```

---

## 任务模型（增强）

```json
{
  "id": "task-001",
  "type": "START",
  "server_id": 1,
  "payload": {
    "service_id": 1,
    "service_name": "MySQL",
    "port": 3306,
    "access_type": "tailscale"
  },
  "steps": [
    "check_port",
    "start_service",
    "verify_status"
  ]
}
```

---

## 安全控制（必须）

* **命令白名单**：只允许执行预定义的安全命令
* **禁止任意 shell**：不允许执行任意 shell 命令
* **执行超时控制**：设置命令执行的最大时间
* **权限限制**：以低权限用户执行命令
* **网络隔离**：限制 Agent 的网络访问范围
* **加密通信**：所有通信通过 HTTPS 加密

---

# 🗄 六、数据库设计

## 详细表结构

### 1. user 表

| 字段名 | 数据类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY, AUTO_INCREMENT` | 用户ID |
| `username` | `VARCHAR(50)` | `UNIQUE, NOT NULL` | 用户名 |
| `password` | `VARCHAR(255)` | `NOT NULL` | 密码（加密存储） |
| `email` | `VARCHAR(100)` | `UNIQUE, NOT NULL` | 邮箱 |
| `role` | `VARCHAR(20)` | `DEFAULT 'user'` | 角色（user/admin） |
| `status` | `TINYINT` | `DEFAULT 1` | 状态（1:活跃, 0:禁用） |
| `created_at` | `DATETIME` | `DEFAULT CURRENT_TIMESTAMP` | 创建时间 |
| `updated_at` | `DATETIME` | `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 更新时间 |

### 2. server 表

| 字段名 | 数据类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY, AUTO_INCREMENT` | 服务器ID |
| `user_id` | `BIGINT` | `REFERENCES user(id)` | 所属用户ID |
| `hostname` | `VARCHAR(100)` | `NOT NULL` | 主机名 |
| `os` | `VARCHAR(50)` | `NOT NULL` | 操作系统 |
| `ip` | `VARCHAR(20)` | `NOT NULL` | 公网IP |
| `tailscale_ip` | `VARCHAR(20)` | `NULL` | Tailscale IP |
| `status` | `VARCHAR(20)` | `DEFAULT 'offline'` | 状态（online/offline/error） |
| `agent_version` | `VARCHAR(20)` | `NULL` | Agent版本 |
| `cpu` | `VARCHAR(50)` | `NULL` | CPU信息 |
| `memory` | `VARCHAR(50)` | `NULL` | 内存信息 |
| `disk` | `VARCHAR(50)` | `NULL` | 磁盘信息 |
| `last_heartbeat` | `DATETIME` | `NULL` | 最后心跳时间 |
| `created_at` | `DATETIME` | `DEFAULT CURRENT_TIMESTAMP` | 创建时间 |
| `updated_at` | `DATETIME` | `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 更新时间 |

### 3. service 表

| 字段名 | 数据类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY, AUTO_INCREMENT` | 服务ID |
| `server_id` | `BIGINT` | `REFERENCES server(id)` | 所属服务器ID |
| `name` | `VARCHAR(100)` | `NOT NULL` | 服务名称 |
| `type` | `VARCHAR(20)` | `NOT NULL` | 服务类型（MySQL/Redis/Nacos/Web/SSH） |
| `protocol` | `VARCHAR(10)` | `NOT NULL` | 协议（TCP/HTTP） |
| `port` | `INT` | `NOT NULL` | 服务端口 |
| `domain` | `VARCHAR(255)` | `NULL` | 绑定域名（HTTP服务） |
| `enabled` | `TINYINT` | `DEFAULT 0` | 是否启用（1:启用, 0:禁用） |
| `status` | `VARCHAR(20)` | `DEFAULT 'inactive'` | 状态（active/inactive/error） |
| `access_type` | `VARCHAR(20)` | `NOT NULL` | 访问类型（tailscale/tunnel） |
| `created_at` | `DATETIME` | `DEFAULT CURRENT_TIMESTAMP` | 创建时间 |
| `updated_at` | `DATETIME` | `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 更新时间 |

### 4. task 表

| 字段名 | 数据类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY, AUTO_INCREMENT` | 任务ID |
| `server_id` | `BIGINT` | `REFERENCES server(id)` | 所属服务器ID |
| `type` | `VARCHAR(20)` | `NOT NULL` | 任务类型（INSTALL/START/STOP/RESTART/DELETE） |
| `status` | `VARCHAR(20)` | `DEFAULT 'pending'` | 状态（pending/running/success/failed） |
| `payload` | `JSON` | `NULL` | 任务参数（JSON格式） |
| `result` | `JSON` | `NULL` | 任务结果（JSON格式） |
| `error_message` | `TEXT` | `NULL` | 错误信息 |
| `created_at` | `DATETIME` | `DEFAULT CURRENT_TIMESTAMP` | 创建时间 |
| `updated_at` | `DATETIME` | `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 更新时间 |
| `completed_at` | `DATETIME` | `NULL` | 完成时间 |

### 5. log 表

| 字段名 | 数据类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY, AUTO_INCREMENT` | 日志ID |
| `server_id` | `BIGINT` | `REFERENCES server(id)` | 所属服务器ID |
| `service_id` | `BIGINT` | `REFERENCES service(id)` | 所属服务ID（可为空） |
| `level` | `VARCHAR(10)` | `NOT NULL` | 日志级别（INFO/WARN/ERROR/DEBUG） |
| `content` | `TEXT` | `NOT NULL` | 日志内容 |
| `source` | `VARCHAR(50)` | `NOT NULL` | 日志来源（agent/tunnel/system） |
| `created_at` | `DATETIME` | `DEFAULT CURRENT_TIMESTAMP` | 创建时间 |

### 6. operation_log 表

| 字段名 | 数据类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY, AUTO_INCREMENT` | 操作记录ID |
| `user_id` | `BIGINT` | `REFERENCES user(id)` | 操作用户ID |
| `action` | `VARCHAR(50)` | `NOT NULL` | 操作类型 |
| `target_type` | `VARCHAR(20)` | `NOT NULL` | 操作目标类型（server/service/task） |
| `target_id` | `BIGINT` | `NOT NULL` | 操作目标ID |
| `details` | `JSON` | `NULL` | 操作详情（JSON格式） |
| `ip` | `VARCHAR(20)` | `NOT NULL` | 操作IP |
| `created_at` | `DATETIME` | `DEFAULT CURRENT_TIMESTAMP` | 操作时间 |

### 7. config 表

| 字段名 | 数据类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY, AUTO_INCREMENT` | 配置ID |
| `key` | `VARCHAR(50)` | `UNIQUE, NOT NULL` | 配置键 |
| `value` | `TEXT` | `NOT NULL` | 配置值 |
| `description` | `VARCHAR(255)` | `NULL` | 配置描述 |
| `created_at` | `DATETIME` | `DEFAULT CURRENT_TIMESTAMP` | 创建时间 |
| `updated_at` | `DATETIME` | `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 更新时间 |

---

# 🌐 七、网络设计

## 内网访问（Tailscale）

```text
开发者 → Tailscale 客户端 → Tailscale 网络 → 目标服务器 → 服务
```

### 优势：

* 安全：端到端加密
* 快速：直接点对点连接
* 可靠：自动穿透 NAT
* 简单：无需配置端口转发

## 公网访问（Cloudflare Tunnel）

```text
用户 → Cloudflare 边缘节点 → Cloudflare Tunnel → 目标服务器 → 服务
```

### 优势：

* 安全：隐藏服务器真实 IP
* 快速：利用 Cloudflare 全球 CDN
* 可靠：Cloudflare 99.99% 可用性
* 简单：无需公网 IP 和端口转发

---

# 🔐 八、安全设计（生产级）

## 核心策略

* **Token 一次性 + 5分钟过期**：防止 Token 被滥用
* **HTTPS 全链路**：所有通信加密
* **最小权限执行**：Agent 以最小权限运行
* **操作审计**：记录所有用户操作
* **多因素认证**：支持 MFA 登录
* **IP 白名单**：限制管理控制台访问 IP
* **定期安全审计**：定期进行安全检查和漏洞扫描

## Agent 安全

```text
只允许执行：
- systemctl：管理系统服务
- tailscale：管理 Tailscale 网络
- cloudflared：管理 Cloudflare Tunnel
- 预定义的安全命令
```

### 安全措施：

* **命令白名单**：只允许执行预定义的安全命令
* **执行环境隔离**：在安全的环境中执行命令
* **执行超时**：设置命令执行的最大时间
* **权限限制**：以低权限用户执行命令
* **网络隔离**：限制 Agent 的网络访问范围

## 数据安全

* **数据加密**：敏感数据加密存储
* **数据备份**：定期备份数据库
* **数据脱敏**：展示敏感数据时进行脱敏
* **访问控制**：基于角色的访问控制

---

# ⚙️ 九、技术选型

## 后端

| 技术 | 版本 | 用途 | 选型理由 |
| :--- | :--- | :--- | :--- |
| Spring Boot | 3.0+ | 应用框架 | 成熟稳定，生态丰富，适合企业级应用 |
| MyBatis Plus | 3.5+ | ORM框架 | 简化数据库操作，提供丰富的 CRUD 方法 |
| Redis | 7.0+ | 缓存、Session管理 | 高性能缓存，支持多种数据结构 |
| WebSocket | - | 实时通信 | 提供服务器状态实时更新 |
| JWT | - | 认证授权 | 无状态认证，便于水平扩展 |
| Spring Security | - | 安全框架 | 提供完整的安全解决方案 |

## 前端

| 技术 | 版本 | 用途 | 选型理由 |
| :--- | :--- | :--- | :--- |
| Vue | 3.0+ | 前端框架 | 响应式设计，组件化开发，性能优秀 |
| Element Plus | 2.0+ | UI组件库 | 丰富的组件，美观的界面，易于使用 |
| ECharts | 5.0+ | 数据可视化 | 强大的图表库，支持多种图表类型 |
| Axios | - | HTTP客户端 | 简洁易用，支持拦截器和请求取消 |
| Vue Router | - | 路由管理 | 官方路由方案，支持嵌套路由和路由守卫 |
| Pinia | - | 状态管理 | 轻量级状态管理，替代 Vuex |

## Agent

| 技术 | 版本 | 用途 | 选型理由 |
| :--- | :--- | :--- | :--- |
| Go | 1.20+ | 开发语言 | 编译型语言，性能优异，适合后台服务 |
| Tailscale | 最新版 | 内网访问 | 安全可靠，易于集成 |
| Cloudflared | 最新版 | 公网访问 | 稳定可靠，全球覆盖 |
| gRPC | - | 通信协议 | 高性能，适合服务间通信 |

## 数据库

| 技术 | 版本 | 用途 | 选型理由 |
| :--- | :--- | :--- | :--- |
| MySQL | 8.0+ | 主数据库 | 成熟稳定，生态丰富，适合关系型数据 |
| PostgreSQL | 14.0+ | 可选数据库 | 功能强大，支持 JSON 类型，适合复杂查询 |

## 基础设施

| 技术 | 版本 | 用途 | 选型理由 |
| :--- | :--- | :--- | :--- |
| Docker | 最新版 | 容器化部署 | 环境一致性，便于部署和扩展 |
| Kubernetes | 最新版 | 容器编排 | 自动化部署，弹性伸缩，高可用 |
| Nginx | 最新版 | 反向代理 | 高性能，稳定可靠，支持 HTTPS |
| Prometheus | 最新版 | 监控系统 | 强大的监控和告警能力 |
| Grafana | 最新版 | 监控可视化 | 美观的监控面板，易于配置 |

---

# 🚀 十、开发阶段规划（落地版）

## 第一阶段（2周）

### 目标：搭建基础框架，实现核心功能

| 任务 | 描述 | 负责人 | 时间 |
| :--- | :--- | :--- | :--- |
| 后端框架搭建 | 搭建 Spring Boot 项目，配置基本依赖 | 后端开发 | 2天 |
| 前端框架搭建 | 搭建 Vue3 项目，配置基本依赖 | 前端开发 | 2天 |
| 用户系统实现 | 实现注册、登录、JWT 鉴权 | 后端开发 | 3天 |
| Agent 基础功能 | 实现 Agent 注册、心跳、任务拉取 | Go 开发 | 3天 |
| Tailscale 集成 | 集成 Tailscale API，实现内网访问 | Go 开发 | 2天 |
| 基础面板 | 实现服务器列表、基础监控 | 前端开发 | 3天 |

## 第二阶段（2周）

### 目标：实现服务映射和公网访问

| 任务 | 描述 | 负责人 | 时间 |
| :--- | :--- | :--- | :--- |
| 服务映射管理 | 实现服务添加、编辑、删除 | 后端开发 | 3天 |
| Tunnel 集成 | 集成 Cloudflare Tunnel，实现公网访问 | Go 开发 | 3天 |
| 连通性检测 | 实现服务连通性检测功能 | 后端开发 | 2天 |
| 服务访问层 | 实现 HTTP 服务访问和 TCP 命令生成 | 前端开发 | 3天 |
| 数据库设计 | 完善数据库表结构和索引 | 后端开发 | 2天 |

## 第三阶段（3周）

### 目标：实现监控、日志和权限控制

| 任务 | 描述 | 负责人 | 时间 |
| :--- | :--- | :--- | :--- |
| 监控系统 | 实现服务器和服务监控 | 后端开发 | 4天 |
| 日志系统 | 实现日志收集和查询 | 后端开发 | 3天 |
| 权限控制 | 实现基于角色的权限控制 | 后端开发 | 3天 |
| 任务调度系统 | 实现任务队列和执行 | 后端开发 | 3天 |
| 前端优化 | 优化前端界面和用户体验 | 前端开发 | 4天 |

## 第四阶段（2周）

### 目标：优化和部署

| 任务 | 描述 | 负责人 | 时间 |
| :--- | :--- | :--- | :--- |
| 高可用设计 | 实现服务高可用部署 | 运维 | 3天 |
| 性能优化 | 优化系统性能和响应速度 | 后端开发 | 3天 |
| 安全性增强 | 加强系统安全性 | 全团队 | 2天 |
| 文档完善 | 完善技术文档和用户手册 | 全团队 | 2天 |
| 部署测试 | 部署到生产环境并进行测试 | 运维 | 3天 |

---

# 💰 十一、商业化设计

## SaaS模式

| 版本 | 价格 | 功能 | 适用场景 |
| :--- | :--- | :--- | :--- |
| 免费版 | 0元 | 1台服务器，基础功能 | 个人开发，小型项目 |
| Pro版 | 99元/月 | 5台服务器，所有功能，自定义域名 | 小团队，中型项目 |
| Enterprise版 | 299元/月 | 无限服务器，高级功能，专属支持 | 企业级应用，大型项目 |

## 企业版

| 版本 | 价格 | 功能 | 适用场景 |
| :--- | :--- | :--- | :--- |
| 私有部署 | 5000元/年 | 完整功能，私有部署 | 企业内部使用，数据敏感场景 |
| 定制版 | 面议 | 定制功能，专属支持 | 特殊行业，定制需求 |

## 增值服务

| 服务 | 价格 | 描述 |
| :--- | :--- | :--- |
| 技术支持 | 200元/小时 | 专业技术支持，问题排查 |
| 定制开发 | 面议 | 根据客户需求进行定制开发 |
| 培训服务 | 1000元/次 | 系统使用培训，最佳实践指导 |

---

# 🎨 十二、用户界面设计

## 设计风格

* **主色调**：蓝色系（#1890ff），代表专业和安全
* **辅助色**：绿色（#52c41a）表示成功，红色（#ff4d4f）表示错误，黄色（#faad14）表示警告
* **字体**：系统默认字体，确保跨平台一致性
* **布局**：响应式布局，适配桌面端和移动端
* **图标**：使用 Element Plus 图标库，保持风格统一

## 主要页面

### 1. 登录/注册页面
* 简洁的登录表单
* 注册流程引导
* 忘记密码功能

### 2. 控制台首页
* 服务器概览
* 服务状态总览
* 监控仪表盘
* 最近操作记录

### 3. 服务器管理页面
* 服务器列表
* 服务器详情
* 添加服务器向导
* 服务器分组管理

### 4. 服务管理页面
* 服务列表
* 服务详情
* 添加服务表单
* 服务访问配置

### 5. 监控页面
* 服务器监控图表
* 服务监控图表
* 历史数据查询
* 告警设置

### 6. 日志页面
* 日志列表
* 日志筛选和搜索
* 日志详情查看
* 日志导出

### 7. 系统设置页面
* 用户管理
* 安全设置
* 系统配置
* 关于我们

---

# 📖 十三、部署指南

## 前提条件

* 服务器要求：至少 2GB 内存，20GB 磁盘空间
* 操作系统：Ubuntu 18.04+，CentOS 7+，Debian 10+
* 网络要求：能够访问互联网
* 权限要求：具有 sudo 权限

## 管理平台部署

### Docker 部署

```bash
# 拉取镜像
docker pull netbridge/management:latest

# 运行容器
docker run -d \
  --name netbridge-management \
  -p 8080:8080 \
  -e DB_HOST=mysql \
  -e DB_PORT=3306 \
  -e DB_NAME=netbridge \
  -e DB_USER=root \
  -e DB_PASSWORD=password \
  -e JWT_SECRET=your-secret-key \
  netbridge/management:latest
```

### Kubernetes 部署

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: netbridge-management
  namespace: netbridge
spec:
  replicas: 2
  selector:
    matchLabels:
      app: netbridge-management
  template:
    metadata:
      labels:
        app: netbridge-management
    spec:
      containers:
      - name: netbridge-management
        image: netbridge/management:latest
        ports:
        - containerPort: 8080
        env:
        - name: DB_HOST
          value: mysql
        - name: DB_PORT
          value: "3306"
        - name: DB_NAME
          value: netbridge
        - name: DB_USER
          value: root
        - name: DB_PASSWORD
          value: password
        - name: JWT_SECRET
          value: your-secret-key
```

## Agent 部署

### 一键安装

```bash
curl -fsSL https://netbridge.com/install.sh | bash -s -- --token=xxxxx
```

### 手动安装

1. 下载 Agent 安装包
2. 解压安装包
3. 运行安装脚本
4. 启动 Agent 服务

---

# 📚 十四、用户手册

## 快速开始

### 1. 注册账号
* 访问管理控制台
* 点击「注册」按钮
* 填写注册信息
* 验证邮箱

### 2. 添加服务器
* 登录管理控制台
* 点击「服务器管理」→「添加服务器」
* 输入服务器主机名
* 复制生成的安装命令
* 在服务器上执行命令

### 3. 配置服务
* 选择已添加的服务器
* 点击「添加服务」
* 填写服务信息
* 点击「启动」按钮

### 4. 访问服务
* 对于 HTTP 服务，点击服务链接直接访问
* 对于 TCP 服务，复制生成的连接命令执行

## 常见问题

### 1. Agent 安装失败怎么办？
* 检查服务器是否具有 sudo 权限
* 检查服务器是否能够访问互联网
* 查看错误日志，根据错误信息排查问题
* 重试安装命令

### 2. 服务无法访问怎么办？
* 检查服务是否已启动
* 检查服务端口是否开放
* 检查连通性检测结果
* 查看相关日志

### 3. 如何修改服务配置？
* 进入服务详情页
* 点击「编辑」按钮
* 修改配置信息
* 点击「保存」按钮

### 4. 如何查看服务器监控数据？
* 进入服务器详情页
* 点击「监控」标签
* 查看实时监控数据和历史趋势

### 5. 如何设置告警？
* 进入「监控」页面
* 点击「告警设置」
* 配置告警规则和阈值
* 保存配置

---

# 🧠 十五、最终总结

NetBridge 本质是：

> 👉 **零信任访问 + 内网穿透 + 运维管理平台**

### 核心优势

1. **简单易用**：一键安装，无需复杂配置
2. **安全可靠**：零信任架构，多重安全措施
3. **功能强大**：支持多种服务类型，满足不同场景需求
4. **实时监控**：全面的监控和告警系统
5. **自动化运维**：减少人工干预，提高效率
6. **可扩展**：模块化设计，易于扩展和定制

### 应用场景

* **远程开发**：远程访问内网开发环境
* **服务器管理**：集中管理多台服务器
* **内网服务暴露**：将内网服务安全暴露到公网
* **零信任访问**：为企业提供安全的访问入口
* **DevOps 工具**：简化运维流程，提高开发效率

NetBridge 旨在为用户提供一个安全、高效、易用的内网穿透和远程运维解决方案，帮助用户轻松实现内网服务的外网访问和管理，无需复杂的网络配置和专业的技术知识。