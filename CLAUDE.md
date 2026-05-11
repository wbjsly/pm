# CLAUDE.md

## 架构

后端基于 Spring Boot 2.7 + Java 11，前端基于 Vue 3 + Vite + Element Plus。

## 后端

**分层：BO 模式（非传统 Service）**

Controller 直接注入带有 `@Service` 注解的 BO 类（`com.wh.bo.pm.WhPm*Bo`）。BO 类包含所有业务逻辑和 DAO 调用，没有 Service 接口层。`com.wh.dao` 下存放 MyBatis-Plus 的 Mapper，`com.wh.entity` 下的实体类继承 `BaseEntity`（UUID 主键、逻辑删除、乐观锁、自动填充的审计字段）。

**数据库：** SQLite（`data/wh.sqlite`，持久化）。SQLite 启动时通过 `SqliteBootstrap` 执行迁移，扫描 `db/sqlite/*.sql` 文件并按文件名排序执行。所有业务实体共享通过 Druid 配置的单一 `@Primary` SQLite 数据源。

**关键集成：** Flowable 6.8 用于审批工作流。MinIO 用于交付物附件。JWT（jjwt）+ Spring Security 用于认证（公开接口：`/api/auth/**`；其余接口均需 Bearer token）。

**API 规范：** 所有端点返回 `R<T>` 包装类。Controller 位于 `com.wh.controller` 下，按 `pm/*`（业务）和 `system/*`（管理）组织。通过 Knife4j 查看接口文档，地址为 `/doc.html`。

## 前端

**页面：** 路由守卫检查 JWT；`<keep-alive>` 在标签页之间保持页面状态。

**状态管理（Pinia）：** `useUserStore`（token、用户信息、权限）和 `useTabStore`（已打开的标签页，Dashboard 始终保持首位）。

**API 层：** `src/utils/request.js` — Axios 实例，基础路径 `/api`（代理到 `:8080`），15 秒超时，自动携带 Bearer token，收到 401 时重定向到 `/login`。