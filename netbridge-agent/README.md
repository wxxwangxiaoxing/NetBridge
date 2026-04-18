# netbridge-agent

NetBridge 的 Go Agent 第一版骨架，当前已实现：

- YAML 配置加载
- 首次注册与本地状态持久化
- 周期性心跳
- 周期性拉取任务
- 任务结果回传
- Agent 日志上报

当前任务执行器还是安全 stub：

- `CHECK`
- `START`
- `STOP`
- `RESTART`

当前已经补了控制器分层：

- `internal/service`：系统服务控制
- `internal/tailscale`：Tailscale 状态探测
- `internal/tunnel`：Tunnel 状态探测

但实现仍属于第一版：

- Linux 下会尝试调用 `systemctl`
- 会尝试探测 `tailscale` 和 `cloudflared` 命令
- 监控采集在 Linux 下优先读取 `/proc`
- 没有这些命令或不在 Linux 上时，会自动退化为 `unknown` / no-op

当前访问层已经支持“命令模板”模式：

- `tailscale.statusCommand / ensureCommand / teardownCommand`
- `tunnel.statusCommand / ensureCommand / teardownCommand`

这样 Agent 不需要在代码里提前写死某一种 `tailscale serve` 或 `cloudflared tunnel` 用法，可以先由配置决定具体命令。

同时也补了第一版“默认策略模式”：

- `tailscale.mode`
- `tunnel.mode`

不写模板时，Agent 会先尝试使用这些内建策略。

对于 `cloudflared` 的 service 模式，Agent 现在还会把运行态写到本地：

- [tunnel-state.json](D:/project/backend/NetBridge/netbridge-agent/data/tunnel-state.json)

这个状态文件用于避免重复执行 `service install / uninstall`，让默认模式先具备基础幂等性。

`tailscale` 默认模式现在也会把访问态写到本地：

- [tailscale-state.json](D:/project/backend/NetBridge/netbridge-agent/data/tailscale-state.json)

这个状态文件用于记录当前是否已经配置过 `serve/funnel`，以及当前模式和对外端口，避免重复执行同一套访问配置。

## 服务映射

Agent 现在支持通过配置把平台里的服务名映射到本机 systemd unit：

```yaml
serviceMappings:
  mysql-main:
    unit: "mysql"
  redis-main:
    unit: "redis"
  nginx-public:
    unit: "nginx"
    checkCommand: "systemctl is-active nginx"
```

这意味着后端里的 `serviceName` 可以保持产品语义，而 Agent 本地执行时会解析成真实 unit 名称。

还可以继续细化自定义命令：

```yaml
serviceMappings:
  custom-worker:
    startCommand: "/usr/local/bin/workerctl start"
    stopCommand: "/usr/local/bin/workerctl stop"
    restartCommand: "/usr/local/bin/workerctl restart"
    checkCommand: "/usr/local/bin/workerctl health"
```

## 访问类型执行策略

- `accessType: tailscale`
  Agent 会优先执行 `tailscale.ensureCommand`
  如果未配置，再按 `tailscale.mode` 走内建策略
- `accessType: cloudflare` 或 `accessType: tunnel`
  Agent 会优先执行 `tunnel.ensureCommand`
  如果未配置，再按 `tunnel.mode` 走内建策略
- 当前命令模板支持这些占位符：
  - `{serviceId}`
  - `{serverId}`
  - `{serviceName}`
  - `{action}`
  - `{advertisePort}`
  - `{targetHost}`
  - `{port}`
  - `{protocol}`
  - `{accessType}`
  - `{domain}`
  - `{tunnelToken}`
  - `{configPath}`

### Tailscale 默认模式

- `serve-http`
  默认执行 `tailscale serve --bg --http=<advertisePort> <target>`
- `serve-https`
  默认执行 `tailscale serve --bg --https=<advertisePort> <target>`
- `serve-tcp`
  默认执行 `tailscale serve --bg --tcp=<advertisePort> tcp://<targetHost>:<port>`
- `funnel`
  默认执行 `tailscale funnel --bg --https=<advertisePort> <target>`

说明：

- `advertisePort` 没填时，`funnel` 默认走 `443`
- 其他模式默认跟随服务自己的 `port`
- 关闭时会自动生成对应的 `off` 命令
- Agent 会持久化当前 `serve/funnel` 访问态
- 如果模式或对外端口变化，Agent 会先尝试卸载旧配置，再应用新配置
- 任务结果现在会带回 `tailscaleAction`

### Cloudflared 默认模式

- `service-token`
  默认执行 `cloudflared service install <token>`
- `service-config`
  默认执行 `cloudflared --config <path> service install`

说明：

- 这两种模式都更偏“主机级隧道守护进程”
- 关闭时会默认执行 `cloudflared service uninstall`
- Agent 会持久化 `cloudflared` 的安装态，避免重复安装
- Linux 下会优先尝试 `systemctl is-active cloudflared`
- Windows 下会优先尝试 `sc.exe query cloudflared`
- 如果你的场景需要更细的单服务命令，仍然建议直接写 `ensureCommand / teardownCommand`
- 任务结果现在会带回 `tunnelAction`

示例：

```yaml
tailscale:
  mode: "serve-http"
  binary: "tailscale"
  advertisePort: 0

tunnel:
  binary: "cloudflared"
  mode: "service-config"
  configPath: "/etc/cloudflared/config.yml"
```

如果你想覆盖默认模式，也可以直接写模板：

```yaml
tailscale:
  ensureCommand: "tailscale serve --bg --https={advertisePort} {protocol}://{targetHost}:{port}"
  teardownCommand: "tailscale serve --https={advertisePort} off"

tunnel:
  ensureCommand: "cloudflared --config {configPath} service install"
  teardownCommand: "cloudflared service uninstall"
```

## 运行

```bash
go run ./cmd/agent -config ./configs/agent.yaml
```

首次运行前请在 `configs/agent.yaml` 里填入：

- `serverUrl`
- `installToken`

首次注册成功后，Agent 会把 `serverId/agentId` 保存到：

```text
data/agent-state.json
```

隧道运行态会保存到：

```text
data/tunnel-state.json
```

Tailscale 访问态会保存到：

```text
data/tailscale-state.json
```

任务执行结果里的访问层字段现已扩展：

- `serviceStatus`
- `serviceAction`
- `tailscaleAction`
- `tunnelAction`
- `rollbackApplied`

当前动作状态常见值包括：

- `configured`
- `removed`
- `unchanged`
- `skipped`
- `failed`
- `checked`

现在 `serviceAction` 也会跟 `tailscaleAction / tunnelAction` 一样结构化返回：

- `CHECK` 时:
  - `serviceAction.action = observe`
- `START / STOP / RESTART` 时:
  - `serviceAction.action = start|stop|restart`
- 如果服务控制本身失败：
  - `serviceAction.status = failed`

服务层结果现在直接来自服务管理器本身，而不是执行器手工拼接：

- [manager.go](D:/project/backend/NetBridge/netbridge-agent/internal/service/manager.go)

也就是说，服务层和访问层现在已经统一成同一种“组件自己产出结构化动作结果”的模式了。

这样一次任务结果里，现在能同时看到：

- 服务层是否成功
- 端口层是否通
- Tailscale 当前状态和动作结果
- Cloudflared 当前状态和动作结果
- 是否触发了基础回滚

现在 `CHECK` 任务也会返回访问层观察结果：

- `tailscaleAction.action = observe`
- `tunnelAction.action = observe`

现在 `STOP` 任务在访问层失败时，也会尝试做一次基础回滚：

- 如果 `tailscale teardown` 已成功，但后续 `cloudflared teardown` 失败
- Agent 会尝试重新执行一次 `tailscale ensure`
- 若回滚成功，会把 `rollbackApplied = true` 带回结果

现在服务层也补了一层基础回滚：

- `START / RESTART` 时，如果服务已经成功启动，但访问层失败
  Agent 会尝试执行一次 `service stop` 回滚
- `STOP` 时，如果服务已经成功停止，但访问层失败
  Agent 会尝试执行一次 `service start` 回滚

对应结果会体现在：

- `serviceAction.action = rollback-stop | rollback-start`
- `rollbackApplied = true`

## 安装脚本

当前仓库已提供：

- Linux:
  - [scripts/install.sh](D:/project/backend/NetBridge/netbridge-agent/scripts/install.sh)
  - [scripts/uninstall.sh](D:/project/backend/NetBridge/netbridge-agent/scripts/uninstall.sh)
  - [scripts/netbridge-agent.service](D:/project/backend/NetBridge/netbridge-agent/scripts/netbridge-agent.service)
- Windows:
  - [scripts/install.ps1](D:/project/backend/NetBridge/netbridge-agent/scripts/install.ps1)
  - [scripts/uninstall.ps1](D:/project/backend/NetBridge/netbridge-agent/scripts/uninstall.ps1)

Linux 示例：

```bash
INSTALL_DIR=/opt/netbridge-agent \
BINARY_SOURCE=./netbridge-agent \
CONFIG_SOURCE=./configs/agent.yaml \
bash ./scripts/install.sh
```

Windows 示例：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install.ps1 `
  -BinarySource .\netbridge-agent.exe `
  -ConfigSource .\configs\agent.yaml
```

## 下一步建议

- 细化 `cloudflared` 的模式切换补偿逻辑
- 增加 Tailscale 模式切换时的更细粒度补偿
- 继续细化失败后的补偿回滚策略
- 把访问层结果进一步和后端展示字段收敛
- 把服务层和访问层结果进一步和后端展示字段收敛
- 增加 Windows / macOS 兼容实现
- 在有 Go 工具链的环境里补 `go build` / `gofmt` 校验
