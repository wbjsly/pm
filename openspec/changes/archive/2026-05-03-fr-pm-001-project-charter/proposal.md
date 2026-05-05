## Why

WH 管理系统目前处于规格文档阶段（`srs_pm.md` 定义了 49 个功能需求），但尚未有任何实际实现代码。FR-PM-001 项目立项（项目章程）是 PM 模块的第一个功能需求，也是整个系统的基石——所有后续 PM 功能（范围、进度、成本、质量等 48 个需求）都依赖于项目章程的存在。本变更将完成从规格文档到可运行系统的跨越：搭建完整项目骨架，实现项目章程的完整 CRUD 与 Flowable 审批链路，同时为后续所有功能建立可复用的基础设施（认证、数据库、审批引擎）。

## What Changes

- **搭建完整项目骨架**：Spring Boot 2.7.18 后端 + Vue 3 前端 + Docker Compose (OrbStack) 基础设施，支持 `npm run dev` 同时启动前后端
- **50 张 PM 模块数据库表**：将 SRS 中的 DDL 从自增整数主键全面适配为 UUID 主键 + BaseEntity 审计字段 + 逻辑删除 + 索引，拆分为 14 个模块 SQL 文件
- **自定义 SQL Bootstrap 机制**：启动时自动执行 SQL 脚本（幂等），通过 `wh_bootstrap_marker` 表追踪执行状态
- **序列模拟服务**：`wh_sequence` 表 + SequenceService 实现 `CHARTER-YYYY-NNN` 编号自动生成
- **认证链路**：JWT 签发/校验 + Spring Security 配置 + BCrypt 密码存储 + 前端登录页 + Axios 拦截器
- **系统管理**：用户/角色/权限 CRUD + 种子数据（10 用户、8 角色、完整权限矩阵），统一初始密码 Admin@123
- **Flowable 审批引擎集成**：`ApprovalCompletedCallback` 回调接口 + Spring @Component 自动注册机制 + `PM_CHARTER_APPROVAL` BPMN 流程定义
- **FR-PM-001 完整后端**：Entity → DAO → BO → Controller 分层，含提交审批/审批通过/审批驳回 API
- **FR-PM-001 完整前端**：列表页（搜索/筛选/分页）+ 详情页（el-descriptions + 审批时间线）+ 新增/编辑页（分步表单）
- **Redis + MinIO 基础设施**：docker-compose.yml 配置，MinIO 启动时自动创建 bucket
- **10 条章程种子数据**：覆盖 DRAFT/PENDING/APPROVED/REJECTED/CLOSED 全状态周期

## Capabilities

### New Capabilities

- `project-charter`: 项目章程的创建、查询、更新、删除、提交审批、审批通过、审批驳回全生命周期管理
- `project-charter-approval`: 项目章程 Flowable 审批流程（PM 提交 → SPONSOR 审批通过/驳回），含回调接口与自动注册机制
- `system-auth`: 用户认证（JWT 签发/校验/黑名单）、登录 API、前端登录页、路由权限守卫
- `system-administration`: 系统用户/角色/权限管理 CRUD，角色-用户关联、角色-权限关联，种子数据初始化
- `database-bootstrap`: SQLite 自定义 SQL Bootstrap 执行器，幂等脚本执行，`wh_bootstrap_marker` 追踪
- `sequence-generation`: 基于 `wh_sequence` 表的业务编号生成服务，支持按年重置、前缀格式化
- `approval-engine`: 通用审批引擎基础设施（Flowable 6.8.0 集成），`ApprovalCompletedCallback` 接口定义与回调注册中心
- `minio-storage`: MinIO 对象存储基础设施，启动时自动创建 bucket（幂等）

### Modified Capabilities

<!-- 无现有规格需要修改 -->

## Impact

- **全新文件**：整个项目从零开始创建，预计 ~93 个新文件，~9000 行代码
- **数据库**：新建 `data/wh.sqlite` 文件，50 张业务表 + 5 张系统表 + 2 张基础设施表
- **Docker**：需启动 Redis + MinIO 容器（OrbStack）
- **前端**：新增 `wh-frontend/` 目录，含 Vue Router、Pinia、Axios 拦截器、登录页、章程页面
- **后端**：新增 `wh-backend/` 目录，含 Maven 项目结构、公共层、配置层、system/approval/pm 模块
- **依赖**：pom.xml 引入 Spring Boot 2.7.18、MyBatis Plus 3.5.5、Flowable 6.8.0、Druid、Knife4j、JWT 等
- **端口**：后端 :8080，前端 :4175，Redis :6379，MinIO API :9000 / Console :9001
