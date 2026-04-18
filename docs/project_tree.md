# NetBridge 项目目录树

```text
NetBridge/
├── LICENSE
├── README.md
├── .gitignore
├── docs/
│   ├── PRD.md
│   ├── technical_architecture.md
│   ├── api_design.md
│   ├── database_design.md
│   ├── database_schema.sql
│   ├── project_tree.md
│   ├── development_plan.md
│   ├── deployment_guide.md
│   └── api_examples/
│       ├── auth.http
│       ├── server.http
│       ├── service.http
│       └── agent.http
├── scripts/
│   ├── bootstrap.ps1
│   ├── bootstrap.sh
│   ├── start-local.ps1
│   └── package-agent.sh
├── deploy/
│   ├── docker/
│   │   ├── docker-compose.yml
│   │   ├── mysql/
│   │   │   └── init/
│   │   │       └── 001_netbridge.sql
│   │   ├── backend/
│   │   │   └── Dockerfile
│   │   ├── frontend/
│   │   │   └── Dockerfile
│   │   └── agent/
│   │       └── Dockerfile
│   ├── k8s/
│   │   ├── namespace.yaml
│   │   ├── mysql.yaml
│   │   ├── redis.yaml
│   │   ├── backend.yaml
│   │   ├── frontend.yaml
│   │   └── ingress.yaml
│   └── nginx/
│       └── default.conf
├── netbridge-backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/netbridge/
│       │   │       ├── NetBridgeApplication.java
│       │   │       ├── common/
│       │   │       │   ├── api/
│       │   │       │   │   ├── ApiResponse.java
│       │   │       │   │   └── PageResult.java
│       │   │       │   ├── config/
│       │   │       │   │   ├── SecurityConfig.java
│       │   │       │   │   ├── MybatisPlusConfig.java
│       │   │       │   │   ├── RedisConfig.java
│       │   │       │   │   ├── WebSocketConfig.java
│       │   │       │   │   └── OpenApiConfig.java
│       │   │       │   ├── constant/
│       │   │       │   ├── enumtype/
│       │   │       │   ├── exception/
│       │   │       │   ├── handler/
│       │   │       │   ├── util/
│       │   │       │   └── websocket/
│       │   │       ├── modules/
│       │   │       │   ├── auth/
│       │   │       │   │   ├── controller/
│       │   │       │   │   ├── dto/
│       │   │       │   │   ├── service/
│       │   │       │   │   └── vo/
│       │   │       │   ├── user/
│       │   │       │   │   ├── controller/
│       │   │       │   │   ├── dto/
│       │   │       │   │   ├── entity/
│       │   │       │   │   ├── mapper/
│       │   │       │   │   ├── service/
│       │   │       │   │   └── vo/
│       │   │       │   ├── server/
│       │   │       │   │   ├── controller/
│       │   │       │   │   ├── dto/
│       │   │       │   │   ├── entity/
│       │   │       │   │   ├── mapper/
│       │   │       │   │   ├── service/
│       │   │       │   │   └── vo/
│       │   │       │   ├── service/
│       │   │       │   │   ├── controller/
│       │   │       │   │   ├── dto/
│       │   │       │   │   ├── entity/
│       │   │       │   │   ├── mapper/
│       │   │       │   │   ├── service/
│       │   │       │   │   └── vo/
│       │   │       │   ├── agent/
│       │   │       │   │   ├── controller/
│       │   │       │   │   ├── dto/
│       │   │       │   │   ├── service/
│       │   │       │   │   └── vo/
│       │   │       │   ├── task/
│       │   │       │   │   ├── controller/
│       │   │       │   │   ├── dto/
│       │   │       │   │   ├── entity/
│       │   │       │   │   ├── mapper/
│       │   │       │   │   ├── service/
│       │   │       │   │   └── scheduler/
│       │   │       │   ├── monitor/
│       │   │       │   │   ├── controller/
│       │   │       │   │   ├── dto/
│       │   │       │   │   ├── entity/
│       │   │       │   │   ├── mapper/
│       │   │       │   │   └── service/
│       │   │       │   ├── log/
│       │   │       │   │   ├── controller/
│       │   │       │   │   ├── entity/
│       │   │       │   │   ├── mapper/
│       │   │       │   │   └── service/
│       │   │       │   ├── config/
│       │   │       │   │   ├── controller/
│       │   │       │   │   ├── entity/
│       │   │       │   │   ├── mapper/
│       │   │       │   │   └── service/
│       │   │       │   ├── tunnel/
│       │   │       │   │   ├── client/
│       │   │       │   │   ├── dto/
│       │   │       │   │   └── service/
│       │   │       │   └── tailscale/
│       │   │       │       ├── client/
│       │   │       │       ├── dto/
│       │   │       │       └── service/
│       │   │       └── infrastructure/
│       │   │           ├── cache/
│       │   │           ├── client/
│       │   │           ├── persistence/
│       │   │           └── security/
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       ├── application-prod.yml
│       │       ├── mapper/
│       │       ├── db/migration/
│       │       │   ├── V1__init_schema.sql
│       │       │   └── V2__seed_config.sql
│       │       └── logback-spring.xml
│       └── test/
│           └── java/com/netbridge/
│               ├── controller/
│               ├── service/
│               └── integration/
├── netbridge-web/
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── public/
│   └── src/
│       ├── main.ts
│       ├── App.vue
│       ├── api/
│       │   ├── request.ts
│       │   ├── auth.ts
│       │   ├── server.ts
│       │   ├── service.ts
│       │   ├── task.ts
│       │   └── monitor.ts
│       ├── assets/
│       │   ├── images/
│       │   ├── icons/
│       │   └── styles/
│       ├── components/
│       │   ├── common/
│       │   ├── layout/
│       │   ├── server/
│       │   ├── service/
│       │   └── charts/
│       ├── router/
│       │   └── index.ts
│       ├── stores/
│       │   ├── auth.ts
│       │   ├── user.ts
│       │   └── app.ts
│       ├── views/
│       │   ├── auth/
│       │   │   ├── LoginView.vue
│       │   │   └── RegisterView.vue
│       │   ├── dashboard/
│       │   │   └── DashboardView.vue
│       │   ├── server/
│       │   │   ├── ServerListView.vue
│       │   │   └── ServerDetailView.vue
│       │   ├── service/
│       │   │   ├── ServiceListView.vue
│       │   │   └── ServiceDetailView.vue
│       │   ├── task/
│       │   │   └── TaskListView.vue
│       │   ├── monitor/
│       │   │   └── MonitorView.vue
│       │   ├── log/
│       │   │   └── LogView.vue
│       │   └── setting/
│       │       └── SettingView.vue
│       ├── websocket/
│       │   ├── server-status.ts
│       │   ├── task-status.ts
│       │   └── service-status.ts
│       ├── types/
│       └── utils/
├── netbridge-agent/
│   ├── go.mod
│   ├── go.sum
│   ├── cmd/
│   │   └── agent/
│   │       └── main.go
│   ├── internal/
│   │   ├── app/
│   │   ├── config/
│   │   ├── logger/
│   │   ├── api/
│   │   ├── heartbeat/
│   │   ├── task/
│   │   ├── executor/
│   │   ├── tailscale/
│   │   ├── tunnel/
│   │   ├── monitor/
│   │   └── service/
│   ├── pkg/
│   │   ├── model/
│   │   ├── constant/
│   │   └── util/
│   ├── configs/
│   │   └── agent.yaml
│   ├── scripts/
│   │   ├── install.sh
│   │   ├── uninstall.sh
│   │   └── netbridge-agent.service
│   └── build/
│       └── package.sh
└── .github/
    └── workflows/
        ├── backend-ci.yml
        ├── frontend-ci.yml
        └── agent-ci.yml
```

## 目录说明

- `docs/`：产品、架构、接口、数据库、研发计划等文档沉淀。
- `scripts/`：项目初始化、本地启动、构建打包脚本。
- `deploy/`：本地 Docker、Kubernetes、Nginx 等部署资源。
- `netbridge-backend/`：Spring Boot 后端，按业务模块拆分。
- `netbridge-web/`：Vue3 管理控制台。
- `netbridge-agent/`：Go Agent，负责注册、心跳、任务执行、监控采集。
- `.github/workflows/`：CI 流水线配置。

## 推荐落地顺序

1. 先创建 `netbridge-backend/`，打通用户、服务器、服务、任务四个核心模块。
2. 再创建 `netbridge-agent/`，最先实现注册、心跳、拉任务。
3. 然后创建 `netbridge-web/`，优先完成登录、服务器列表、服务管理页面。
4. 最后补齐 `deploy/`、CI、监控和日志能力。
```
