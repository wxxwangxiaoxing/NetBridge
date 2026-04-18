# NetBridge Backend

## 模块结构

- `netbridge-dependencies`
  统一依赖版本和 Maven 插件配置。

- `netbridge-framework`
  放公共能力和可复用 starter。
  - `netbridge-common`：通用常量、基础对象、工具类
  - `netbridge-spring-boot-starter-web`：统一响应、异常处理、Web 基础配置
  - `netbridge-spring-boot-starter-mybatis`：MyBatis Plus 扫描与持久层基础配置
  - `netbridge-spring-boot-starter-security`：认证与密码编码骨架
  - `netbridge-spring-boot-starter-redis`：缓存基础能力
  - `netbridge-spring-boot-starter-swagger`：OpenAPI 文档配置

- `netbridge-module-user`
  用户、登录、权限、账号信息。

- `netbridge-module-server`
  服务器注册信息、状态、心跳视图。

- `netbridge-module-service`
  服务映射、端口、访问方式、启停控制。

- `netbridge-module-agent`
  Agent 注册、心跳、任务拉取与执行结果交互。

- `netbridge-module-task`
  任务编排、状态流转、执行历史。

- `netbridge-module-config`
  系统配置、密钥配置、平台参数。

- `netbridge-module-log`
  运行日志、操作日志、Agent 上报日志。

- `netbridge-module-monitor`
  服务器指标、服务可用性、监控面板查询。

- `netbridge-module-tunnel`
  Cloudflare Tunnel 相关能力、域名和通道状态。

- `netbridge-module-tailscale`
  Tailscale 设备、IP、MagicDNS 和网络接入能力。

- `netbridge-server`
  Spring Boot 启动模块，聚合所有业务模块。

## 业务模块约定

每个业务模块默认拆成两层：

- `*-api`
  放 DTO、VO、枚举、接口契约，避免把实现细节暴露给别的模块。

- `*-biz`
  放 Controller、Service、Entity、Mapper、转换器、业务实现。

## 推荐下一步

1. 在各 `*-biz` 模块下补 `entity / mapper / service / convert` 目录。
2. 将 [database_schema.sql](D:/project/backend/NetBridge/docs/database_schema.sql) 对应到实体和 Mapper。
3. 在 `netbridge-server` 引入 Flyway 或 Liquibase 管理建表脚本。
4. 逐步把当前 `ping` 示例接口替换成真实业务接口。
