# NetBridge 开发推进记录

## 当前阶段

当前按照 [PRD.md](D:/project/backend/NetBridge/docs/PRD.md) 进入 **第一阶段：基础框架 + 用户系统 + Agent基础能力预留**。

## 已完成

### 1. 多模块后端骨架

- 已建立 `netbridge-backend` 多 Maven 模块结构
- 已拆分 `dependencies / framework / module / server`
- 已补齐核心业务模块：
  - `user`
  - `server`
  - `service`
  - `agent`
  - `task`
  - `config`
  - `log`
  - `monitor`
  - `tunnel`
  - `tailscale`

### 2. 公共基础能力

- 已建立统一 Web 返回结构 `ApiResponse`
- 已建立分页结果模型 `PageResult`
- 已建立统一业务异常 `BusinessException`
- 已建立全局异常处理器
- 已接入参数校验能力
- 已接入 MyBatis Plus 分页插件

### 3. 数据库与运行基础

- 已输出数据库设计脚本 [database_schema.sql](D:/project/backend/NetBridge/docs/database_schema.sql)
- 已在 `netbridge-server` 接入 Flyway
- 已创建首个迁移脚本：
  [V1__init_schema.sql](D:/project/backend/NetBridge/netbridge-backend/netbridge-server/src/main/resources/db/migration/V1__init_schema.sql)
- 已补充基础 `application.yml` 数据源与 Flyway 配置

### 4. 核心模块 CRUD

以下模块已具备基础 CRUD 接口、实体、Mapper、Service：

- 用户
- 服务器
- 服务
- 任务

### 5. 用户认证第一版

- 已支持注册接口 `/api/user/register`
- 已支持登录接口 `/api/user/login`
- 已支持用户信息接口 `/api/user/info`
- 已支持密码加密存储
- 已支持 JWT 令牌生成
- 已支持 JWT 解析、登录用户上下文、受保护接口鉴权

### 6. 服务器接入入口第一版

- 已支持安装命令生成接口 `/api/server/generate-command`
- 已新增 `install_token` 表的实体、Mapper、Service
- 已支持 Agent 注册接口 `/api/agent/register`
- 已支持 Agent 心跳接口 `/api/agent/heartbeat`
- 已支持 Agent 拉取任务接口 `/api/agent/task`
- 已支持 Agent 回传任务结果接口 `/api/agent/task/{id}/result`
- 已支持 Agent 日志上报接口 `/api/agent/log`
- Agent 注册时会消费安装令牌并创建服务器记录
- Agent 心跳时会更新服务器状态、Tailscale IP 和最后心跳时间
- Agent 拉任务时会自动领取 `pending` 任务并更新为 `running`
- Agent 回传结果时会更新任务状态、结果和完成时间
- Agent 日志会上报到 `log` 表

### 7. 服务动作与任务联动第一版

- 已支持服务动作接口：
  - `/api/service/{id}/start`
  - `/api/service/{id}/stop`
  - `/api/service/{id}/restart`
  - `/api/service/{id}/check`
- 以上动作会创建 `task` 记录，而不是直接修改服务状态
- Agent 成功回传结果后，会按任务类型反向更新服务状态
- `START / RESTART` 成功后会将服务更新为 `active`
- `STOP` 成功后会将服务更新为 `inactive`
- `CHECK` 会更新 `health_status` 和 `last_check_at`

### 8. 展示层聚合与查询增强第一版

- 已支持服务器详情聚合接口 `/api/server/{id}/detail`
- 服务器详情会返回：
  - 服务器基础信息
  - 服务列表
  - 最近任务
  - 最近日志
- 已支持日志列表接口 `/api/log/list`
- 已支持日志详情接口 `/api/log/{id}`
- 已扩展任务与日志 DTO，便于详情页直接使用

### 9. 服务详情与监控查询第一版

- 已支持服务详情聚合接口 `/api/service/{id}/detail`
- 服务详情会返回：
  - 服务基础信息
  - 最近任务
  - 最近日志
  - 最近监控指标
- 已支持服务器监控列表/详情：
  - `/api/monitor/server/list`
  - `/api/monitor/server/{id}`
- 已支持服务监控列表/详情：
  - `/api/monitor/service/list`
  - `/api/monitor/service/{id}`
- 已新增 `service_metric` 对应实体、Mapper、Service 与查询接口

### 10. 监控写入链路第一版

- Agent 心跳已支持写入 `server_metric`
- Agent 心跳请求现已支持：
  - `cpuUsage`
  - `memoryUsage`
  - `diskUsage`
  - `networkInBytes`
  - `networkOutBytes`
  - `loadAverage`
- `CHECK` 任务结果已支持写入 `service_metric`
- Agent 任务结果请求现已支持：
  - `responseTimeMs`
  - `successCount`
  - `errorCount`
- `CHECK` 成功/失败时，会同时更新：
  - `service.health_status`
  - `service.last_check_at`
  - `service_metric`

### 11. 任务载荷与结果结构化第一版

- 已新增结构化任务载荷 DTO：`TaskPayloadDto`
- 已新增结构化任务结果 DTO：`TaskResultDto`
- 服务动作创建任务时，不再手写 JSON 字符串拼接，而是通过 DTO 序列化
- Agent 拉任务时，返回结构化 `payload`
- Agent 回传结果时，支持结构化 `taskResult`
- 任务查询接口现会解析并返回：
  - `payloadObj`
  - `resultObj`
- 目前数据库中仍保留 `payload/result` 的 JSON 字符串存储方式，以保证改动成本可控

### 12. 任务列表格式与执行协议收敛第一版

- 任务列表接口已改为统一分页结构 `PageResult`
- `TaskPayloadDto` 已补充：
  - `serverId`
  - `action`
- `TaskResultDto` 已补充：
  - `success`
  - `endpoint`
  - `portStatus`
  - `tunnelStatus`
  - `tailscaleStatus`
  - `checkedAt`
- 服务详情已补充：
  - `recentTaskTotal`
  - `recentLogTotal`
- 服务状态回写时，已兼容从 `accessUrl / endpoint` 提取访问地址

### 13. 主要列表接口分页格式统一第一版

- 以下列表接口已统一返回 `PageResult`
  - `/api/server/list`
  - `/api/service/list`
  - `/api/task/list`
  - `/api/log/list`
  - `/api/monitor/server/list`
  - `/api/monitor/service/list`
- 服务器详情已补充：
  - `serviceTotal`
  - `recentTaskTotal`
  - `recentLogTotal`
- 这一步主要用于减少前端分页适配成本

### 14. 前端联调契约文档第一版

- 已新增前端联调契约文档：
  [frontend_contract.md](D:/project/backend/NetBridge/docs/frontend_contract.md)
- 文档已覆盖：
  - 通用返回结构
  - 分页返回结构
  - 服务器详情接口
  - 服务详情接口
  - 任务结构化字段
  - 日志字段
  - 监控字段
- 当前可将该文档作为前后端联调的基础版本

### 15. 业务真实性增强第一版

- 已支持安装令牌过期治理
- 生成安装命令前会自动清理过期的 `unused` 安装令牌
- 已新增安装令牌清理接口：
  - `/api/server/install-token/cleanup`
- Agent 注册已支持幂等处理：
  - 同一安装令牌重复注册时，不会重复创建服务器
  - 同一用户同一主机重复注册时，会优先复用已有服务器记录
- 已支持任务重试接口：
  - `/api/task/{id}/retry`
- 任务重试当前约束为：
  - 仅允许 `failed` 任务重试
  - 默认最多重试 3 次
  - 重试时会重置执行结果、错误信息、执行时间和已分配 Agent

### 16. 任务生命周期与服务保护第一版

- 已支持任务取消接口：
  - `/api/task/{id}/cancel`
- 已支持任务超时清理接口：
  - `/api/task/timeout/cleanup`
- 当前超时策略为：
  - 运行超过 10 分钟的 `running` 任务会被标记为 `timed_out`
- 任务重试现已支持：
  - `failed`
  - `timed_out`
- Agent 回传任务结果时，会拒绝已 `cancelled` 或 `timed_out` 的任务结果覆盖
- 服务动作创建任务前，已增加同服务执行中任务并发保护
- 服务删除前，已增加保护：
  - 存在 `pending/running` 任务时禁止删除
  - 服务仍处于 `active/enabled` 时禁止删除

### 17. 任务超时定时化与服务更新保护第一版

- 已开启 Spring Scheduling
- 任务超时清理已支持后台定时执行
- 已新增配置项：
  - `netbridge.task.lifecycle.timeout-cleanup-enabled`
  - `netbridge.task.lifecycle.timeout-cleanup-fixed-delay-ms`
  - `netbridge.task.lifecycle.timeout-minutes`
- 任务超时清理逻辑已下沉到 `TaskService`
- 服务更新接口已增加执行中保护：
  - 存在 `pending/running` 任务时禁止更新服务配置

### 18. 关闭任务后的服务联动与安装令牌定时治理第一版

- 任务取消现已下沉到 `TaskService`
- 任务取消/超时后，服务状态会进一步联动：
  - `CHECK` 取消后更新 `health_status = unknown`
  - `CHECK` 超时后更新 `health_status = error`
  - 非 `CHECK` 任务超时后更新服务为 `status = error`
  - 非 `CHECK` 任务取消后更新服务为 `status = error`、`health_status = warning`
- 已新增安装令牌后台定时清理任务
- 已新增配置项：
  - `netbridge.install-token.cleanup-enabled`
  - `netbridge.install-token.cleanup-fixed-delay-ms`

### 19. 安装令牌审计与手动失效第一版

- 已支持安装令牌审计接口：
  - `/api/server/install-token/list`
  - `/api/server/install-token/{id}`
- 已支持安装令牌手动失效接口：
  - `/api/server/install-token/{id}/revoke`
- 当前约束为：
  - 仅允许当前登录用户查询自己的安装令牌
  - `used` 安装令牌不可手动失效
  - `expired` 安装令牌不可重复失效
  - `revoked` 安装令牌重复失效时返回当前状态

### 20. WebSocket 状态推送第一版

- 已接入 Spring WebSocket + STOMP 简单消息代理
- 已开放 WebSocket 端点：
  - `/ws`
- 已支持订阅主题：
  - `/topic/server/status`
  - `/topic/task/status`
  - `/topic/service/status`
- 当前已接入推送的状态变化包括：
  - Agent 注册、心跳带来的服务器状态变化
  - 任务创建、领取、更新、重试、取消、超时、结果回传
  - 服务创建、更新、删除，以及任务联动后的服务状态变化

### 21. WebSocket 鉴权与用户隔离第一版

- WebSocket 现已支持基于 JWT 的 STOMP `CONNECT` 鉴权
- 支持在连接头中传递：
  - `Authorization: Bearer <token>`
  - `token: <token>`
- 状态推送现已改为按用户队列发送，不再广播给所有订阅方
- 当前前端应订阅：
  - `/user/queue/server/status`
  - `/user/queue/task/status`
  - `/user/queue/service/status`
- WebSocket 现已限制订阅目标：
  - 仅允许订阅上述 3 个用户队列
- WebSocket 会话现已记录：
  - `userId`
  - `username`
- 已在前端契约文档中补充 WebSocket 联调说明：
  [frontend_contract.md](D:/project/backend/NetBridge/docs/frontend_contract.md)

### 22. netbridge-agent 第一版骨架

- 已创建 `netbridge-agent/` Go 工程
- 已补基础目录：
  - `cmd/agent`
  - `internal/app`
  - `internal/config`
  - `internal/api`
  - `internal/heartbeat`
  - `internal/task`
  - `internal/executor`
  - `internal/monitor`
  - `internal/logger`
  - `pkg/model`
  - `pkg/constant`
- 已实现第一版主循环：
  - 注册
  - 心跳
  - 拉任务
  - 回传结果
  - 日志上报
- 已支持本地状态持久化：
  - `data/agent-state.json`
- 已提供示例配置：
  [configs/agent.yaml](D:/project/backend/NetBridge/netbridge-agent/configs/agent.yaml)
- 当前任务执行器仍为安全 stub，不直接操作系统服务

### 23. netbridge-agent 执行器分层与跨平台收口第一版

- Agent 监控采集已拆分为：
  - 公共实现
  - Linux 实现
  - 非 Linux 兜底实现
- 当前 Linux 下已支持基础监控采集：
  - 内存
  - 磁盘
  - 网络流量
  - Load Average
- Agent 执行器已拆出控制层：
  - `internal/service`
  - `internal/tailscale`
  - `internal/tunnel`
- 当前 Linux 下会尝试调用：
  - `systemctl`
  - `tailscale`
  - `cloudflared`
- 在缺少命令或非 Linux 环境下，会自动退化为 `unknown` / no-op，避免直接编译或运行失败

### 24. netbridge-agent 服务映射与命令层解耦第一版

- Agent 已新增独立命令执行层：
  - `internal/command`
- 已消除 `service` 与 `executor` 之间的直接循环依赖风险
- Agent 配置已支持服务映射：
  - `serviceMappings.<serviceName>.unit`
- `CHECK` 任务现已补充基础 TCP 端口探测
- `CHECK` 成功判定当前基于：
  - systemd 服务状态为 `active`
  - 或目标端口可连通
- 当前服务控制仍优先面向 Linux + `systemctl`

### 25. netbridge-agent 访问类型策略第一版

- Agent 配置已新增：
  - `tailscale.binary`
  - `tunnel.binary`
- `START / STOP / RESTART` 执行时，现已按 `accessType` 分支接入：
  - `tailscale`
  - `cloudflare`
  - `tunnel`
- 当前策略仍为第一版命令探测与接入点：
  - `tailscale` 通过命令可用性与状态探测接入
  - `cloudflared` 通过命令可用性探测接入
- 服务映射现已支持更细的自定义命令：
  - `startCommand`
  - `stopCommand`
  - `restartCommand`
  - `checkCommand`

### 26. netbridge-agent 隧道命令模板与安装脚本第一版

- Agent 访问层现已支持可配置命令模板：
  - `tailscale.statusCommand`
  - `tailscale.ensureCommand`
  - `tailscale.teardownCommand`
  - `tunnel.statusCommand`
  - `tunnel.ensureCommand`
  - `tunnel.teardownCommand`
- 命令模板当前支持占位符：
  - `serviceId`
  - `serverId`
  - `serviceName`
  - `action`
  - `targetHost`
  - `port`
  - `protocol`
  - `accessType`
  - `domain`
- 这样当前 Agent 已能通过配置接入更具体的：
  - `tailscale serve / funnel`
  - `cloudflared access tcp / tunnel`
- Agent 安装脚本已补齐第一版：
  - Linux `install.sh / uninstall.sh / netbridge-agent.service`
  - Windows `install.ps1 / uninstall.ps1`
- `START / STOP / RESTART` 返回的访问地址现会优先基于 `protocol + domain` 生成，更贴近公网访问场景

### 27. netbridge-agent 默认隧道策略第一版

- Agent 已为常见隧道场景补充内建模式，不再只能手写命令模板
- `tailscale.mode` 当前支持：
  - `serve-http`
  - `serve-https`
  - `serve-tcp`
  - `funnel`
- `tunnel.mode` 当前支持：
  - `service-token`
  - `service-config`
- 当未显式配置 `ensureCommand / teardownCommand` 时：
  - Tailscale 会自动生成 `serve/funnel` 命令
  - Cloudflared 会自动生成 `service install/uninstall` 命令
- `tailscale.advertisePort` 已加入配置：
  - `funnel` 默认回退到 `443`
  - 其他模式默认回退到服务端口
- Cloudflared 已支持：
  - `tunnel.token`
  - `tunnel.configPath`
- 当前这一步的目标是让 Agent 在“常见默认场景”下先可运行，再逐步细化到更复杂的隧道编排策略

### 28. netbridge-agent 隧道运行态持久化第一版

- Agent 已为 `cloudflared` 增加本地运行态文件：
  - `data/tunnel-state.json`
- 当前持久化内容包括：
  - 是否已安装为服务
  - 当前模式
  - 当前配置路径
  - 是否使用 token 模式
- 在 `service-token / service-config` 模式下：
  - 若本地状态与当前配置一致且已安装，Agent 会跳过重复 `service install`
  - 卸载时若本地未标记为已安装，Agent 会跳过重复 `service uninstall`
- `cloudflared` 状态检测已增强：
  - Linux 优先尝试 `systemctl is-active cloudflared`
  - Windows 优先尝试 `sc.exe query cloudflared`
- 当前目标是先让默认 service 模式具备基础幂等性和运行态感知，后续再细化模式切换补偿与更复杂的多隧道编排

### 29. netbridge-agent Tailscale 访问态持久化第一版

- Agent 已为 `tailscale` 默认模式增加本地运行态文件：
  - `data/tailscale-state.json`
- 当前持久化内容包括：
  - 是否已配置访问态
  - 当前模式
  - 当前对外端口
- 在 `serve-http / serve-https / serve-tcp / funnel` 默认模式下：
  - 若本地状态与当前配置一致且已配置，Agent 会跳过重复 `serve/funnel`
  - 若模式或对外端口发生变化，Agent 会先执行一次默认 `off`，再应用新配置
- `cloudflared` 默认 service 模式也已补一层模式切换补偿：
  - 若本地标记为已安装但当前模式或配置发生变化，Agent 会先卸载旧 service，再安装新 service
- 当前目标是让两条默认访问链都先具备基础幂等性与配置切换补偿，再继续细化更复杂的多服务策略

### 30. netbridge-agent 访问层结果结构化第一版

- Agent 任务结果现已补充访问层结构化动作字段：
  - `tailscaleAction`
  - `tunnelAction`
  - `rollbackApplied`
- 当前访问层动作状态会区分：
  - `configured`
  - `removed`
  - `unchanged`
  - `skipped`
  - `failed`
  - `checked`
- `START / RESTART` 在访问层失败时，当前会尝试对已成功的 Tailscale 配置做一次基础回滚
- 这一步的目标是让后端和前端在联调任务结果时，能看见“访问层具体做了什么”，而不是只看到一条总成功/失败

### 31. netbridge-agent 访问层观察与停止回滚第一版

- `CHECK` 任务现已补充访问层观察结果：
  - `tailscaleAction.action = observe`
  - `tunnelAction.action = observe`
- `CHECK` 返回的：
  - `tailscaleStatus`
  - `tunnelStatus`
  当前已直接来源于访问层观察结果
- `STOP` 在访问层失败时，当前会做一层基础回滚：
  - 如果 `tailscale teardown` 已成功
  - 但后续 `cloudflared teardown` 失败
  - Agent 会尝试重新执行一次 `tailscale ensure`
  - 成功时会把 `rollbackApplied = true` 带回任务结果
- 当前目标是把访问层结果做得更可观测，让任务失败时能看见“失败发生在哪一层、有没有补偿”

### 32. netbridge-agent 服务层结果结构化第一版

- Agent 任务结果现已补充：
  - `serviceStatus`
  - `serviceAction`
- `serviceAction` 当前与访问层动作字段保持同一结构：
  - `component`
  - `action`
  - `status`
  - `detail`
- `CHECK` 任务时：
  - `serviceAction.action = observe`
- `START / STOP / RESTART` 任务时：
  - `serviceAction.action = start|stop|restart`
  - 执行后会再做一次服务状态检查，写回 `serviceStatus`
- 若服务控制本身失败：
  - 会直接返回 `serviceAction.status = failed`
- 当前目标是让一次任务结果同时覆盖：
  - 服务层
  - 端口层
  - Tailscale 层
  - Cloudflared 层
  这样后端和前端展示可以逐步统一到同一种结构

### 33. netbridge-agent 服务层基础回滚第一版

- `START / RESTART` 在服务成功启动后，若后续访问层失败：
  - Agent 会尝试执行一次 `service stop` 作为基础回滚
- `STOP` 在服务成功停止后，若后续访问层失败：
  - Agent 会尝试执行一次 `service start` 作为基础回滚
- 回滚结果会写回：
  - `serviceAction.action = rollback-stop | rollback-start`
  - `rollbackApplied = true`
- 当前目标是减少“服务层和访问层状态撕裂”的情况，让任务失败后尽量回到更接近原始的服务状态

### 34. netbridge-agent 服务管理结构化结果第一版

- `service.Manager` 现已补充结构化接口：
  - `StartWithResult`
  - `StopWithResult`
  - `RestartWithResult`
  - `Observe`
- 执行器现已直接复用服务管理器返回的结构化结果，不再手工拼接大部分服务层动作字段
- 当前这一步让：
  - 服务层
  - Tailscale 层
  - Cloudflared 层
  都开始朝“各自控制器原生返回结构化动作结果”的方向收敛
- 后续如果要继续补平台兼容或命令细节，改动面也会更集中在各自模块里

### 35. 后端任务结果契约与 Agent 结构对齐第一版

- 后端 `TaskResultDto` 已补充字段：
  - `serviceStatus`
  - `serviceAction`
  - `tunnelAction`
  - `tailscaleAction`
  - `rollbackApplied`
- 已新增后端 DTO：
  - `AccessOperationDto`
- 当前后端任务查询接口在解析 `resultObj` 时，已具备承接 Agent 新版结构化结果的字段模型
- 前端联调文档也已同步补充：
  - 服务层结果
  - 访问层动作结果
  - 回滚标记
- 当前目标是让：
  - Agent 回传结构
  - 后端 DTO 契约
  - 前端 `resultObj` 展示字段
  三者开始保持同一套命名和语义

### 36. 操作审计日志第一版

- 已新增 Flyway 迁移脚本：
  [V2__add_operation_log.sql](D:/project/backend/NetBridge/netbridge-backend/netbridge-server/src/main/resources/db/migration/V2__add_operation_log.sql)
- 已新增操作审计表：
  - `operation_log`
- 已新增操作审计实体、Mapper、Service：
  - `OperationLogEntity`
  - `OperationLogMapper`
  - `OperationLogService`
- 已新增审计查询接口：
  - `/api/log/operation/list`
  - `/api/log/operation/{id}`
- 当前已接入审计记录的关键操作包括：
  - 用户创建、登录、更新、删除
  - 安装令牌生成、手动失效
  - 服务器创建、更新、删除
  - 服务创建、更新、删除
  - 服务动作触发的任务创建
  - 任务创建、更新、重试、取消、删除
- 审计记录当前已包含：
  - 操作用户
  - 操作动作
  - 资源类型
  - 资源 ID
  - 操作结果
  - 操作详情
  - 来源 IP

### 37. 操作失败审计自动化第一版

- 已新增注解：
  - `OperationAudit`
- 已新增 AOP 切面：
  - `OperationAuditAspect`
- 当前已支持对标注接口自动记录失败审计
- 当前失败审计会自动写入：
  - 操作动作
  - 资源类型
  - 资源 ID（支持简单表达式提取）
  - 当前登录用户
  - 异常信息
- 当前已在以下关键写接口接入失败审计注解：
  - 用户创建、登录、更新、删除
  - 安装令牌生成、失效
  - 服务器创建、更新、删除
  - 服务创建、更新、删除、启停、重启、检测
  - 任务创建、更新、重试、取消、删除
- 这一版暂时保留原有成功审计写法，先把失败链统一收口，后续再视情况继续向注解/AOP 完全迁移

### 38. 统一业务错误码第一版

- 已新增统一错误码枚举：
  - `ErrorCode`
- `ApiResponse` 现已支持：
  - 按错误码返回失败结果
  - 按 `code + message` 返回失败结果
- `BusinessException` 现已支持：
  - 错误码
  - 快捷工厂方法：
    - `badRequest`
    - `unauthorized`
    - `forbidden`
    - `notFound`
    - `conflict`
    - `validation`
    - `internal`
- 全局异常处理现已按错误类型输出更稳定的 HTTP 语义码：
  - 参数校验 -> `422`
  - 认证失败 -> `401`
  - 资源不存在 -> `404`
  - 状态冲突 -> `409`
- JWT 过滤器和认证入口也已切到统一错误码输出
- 当前已优先在用户链路与任务链路中收口一批常见异常类型

### 39. 核心主链错误码收口第二版

- 已继续将统一错误码扩展到以下主链：
  - 服务器管理
  - 安装令牌管理
  - 服务管理
  - Agent 注册/心跳/任务结果回传
- 当前已明确区分的典型错误包括：
  - `401`：未登录、JWT 无效、Agent 身份不匹配
  - `404`：服务器/服务/任务/安装令牌不存在
  - `409`：安装令牌已使用或已过期、任务状态冲突、服务执行中冲突
  - `400`：请求状态不合法、缺少必要参数
  - `500`：序列化/反序列化等内部错误
- 这一轮之后，`user / task / server / service / install token / agent` 几条核心链路的错误语义已经明显更统一

### 40. 成功审计自动化第二版

- `OperationAuditAspect` 现已支持在成功后读取返回值，并通过 `#result` 参与表达式计算
- 当前成功审计已可从 `ApiResponse.data` 中提取资源 ID，用于自动写入 `operation_log`
- 目前已切到自动成功审计的核心接口包括：
  - 用户创建、登录、更新、删除
  - 服务器创建、更新、删除
  - 安装令牌失效
  - 服务创建、更新、删除、启停、重启、检测
  - 任务创建、更新、重试、取消、删除
- 当前控制器里大部分手写 `operationLogService.record(...)` 已经移除
- 暂时仍保留一处显式成功审计：
  - 安装命令生成
  因为当前返回体里没有直接返回安装令牌 ID

## 当前代码状态说明

当前项目已经从“纯文档设计”推进到“后端接口主链基本可联调”阶段，但还未完成完整业务闭环。

已具备：

- 多模块工程组织
- 基础建表迁移
- 用户认证主链第一版
- 服务器接入主链第一版
- 服务动作到任务执行主链第一版
- 日志、监控、详情聚合与前端契约第一版
- 安装令牌治理、Agent 注册幂等、任务重试第一版
- 任务生命周期控制与服务删除保护第一版
- 任务超时定时化与服务更新保护第一版
- 关闭任务后的服务联动与安装令牌定时治理第一版
- 安装令牌审计与手动失效第一版
- WebSocket 状态推送第一版
- WebSocket 鉴权与用户隔离第一版
- 操作审计日志第一版
- 操作失败审计自动化第一版
- netbridge-agent 第一版骨架
- netbridge-agent 执行器分层与跨平台收口第一版
- netbridge-agent 服务映射与命令层解耦第一版
- netbridge-agent 访问类型策略第一版

仍待完成：

- Tunnel / Tailscale 实际客户端集成
- 更严格的权限隔离与多租户边界
- 任务调度、重试、超时与补偿策略完善
- Agent 执行协议与版本兼容治理
- 日志、监控、配置模块的清理归档与运维治理
- 任务关闭后的更细粒度服务补偿策略
- 安装令牌使用审计与安全策略增强
- WebSocket 消息级权限与更细粒度订阅控制
- Agent 系统服务、真实监控采集与执行器落地
- Agent 平台兼容与真实命令执行策略完善
- Agent 服务映射增强与真实命令策略完善
- Agent 访问类型策略与真实隧道接入完善
- Agent 隧道命令模板的默认策略与状态持久化完善
- Agent 默认隧道模式的服务状态感知与幂等治理
- Agent 隧道运行态持久化扩展到 Tailscale
- Agent Tailscale/Cloudflared 模式切换补偿细化
- Agent 访问层失败补偿与结果结构继续细化
- Agent 访问层观察结果与后端展示字段进一步收敛
- Agent 服务层与访问层结果进一步和后端展示字段收敛
- Agent 服务层与访问层失败补偿进一步细化
- Agent 服务层与访问层结果产出进一步模块化
- 后端任务结果契约与 Agent/前端结果字段进一步收敛

## 推荐下一步

### 下一阶段：补齐平台真实性能力

- 增加任务超时、取消和更细粒度重试策略
- 增加安装令牌审计与定时清理能力
- 增加 Agent 注册冲突处理与版本升级策略
- 增加服务删除保护和任务执行中的并发约束
- 增加 WebSocket 消息级权限与更细粒度订阅控制
- 增加安装令牌审计增强与手动失效能力
- 推进 netbridge-agent 真实执行器与系统集成
- 推进 netbridge-agent 平台兼容与真实命令执行策略
- 推进 netbridge-agent 服务映射增强与执行策略完善
- 推进 netbridge-agent 访问类型策略与真实隧道接入
- 推进 netbridge-agent 隧道命令模板默认实现与幂等治理
- 推进 netbridge-agent 默认隧道模式的状态检测与补偿
- 推进 netbridge-agent 隧道运行态持久化扩展到 Tailscale
- 推进 netbridge-agent Tailscale/Cloudflared 模式切换补偿细化
- 推进 netbridge-agent 访问层失败补偿与结果结构继续细化
- 推进 netbridge-agent 访问层观察结果与后端展示字段进一步收敛
- 推进 netbridge-agent 服务层与访问层结果进一步和后端展示字段收敛
- 推进 netbridge-agent 服务层与访问层失败补偿进一步细化
- 推进 netbridge-agent 服务层与访问层结果产出进一步模块化
- 推进后端任务结果契约与 Agent/前端结果字段进一步收敛
- 推进 Tunnel / Tailscale 实际客户端接入

## 风险与阻塞

- 当前本机 Maven/JDK 绑定问题已修复，后端已完成编译和启动验证
- 当前 Maven 仍存在间歇性仓库/DNS 解析问题，可能影响增量依赖下载与再次编译
- 当前仍缺少本地构建验证与集成测试
- 当前 CRUD 与任务链路已可联调，但业务约束仍属于第一版实现
