## 1. 项目骨架搭建

- [x] 1.1 创建 wh-backend/ 目录结构及 pom.xml（Spring Boot 2.7.18、MyBatis Plus 3.5.5、Flowable 6.8.0、Druid、Knife4j、JWT、Hutool、MinIO SDK）
- [x] 1.2 创建 WhApplication.java 入口类（@SpringBootApplication、@EnableTransactionManagement、@EnableScheduling、@EnableAsync、@MapperScan）
- [x] 1.3 创建 application.yml（spring profiles、app 配置、mybatis-plus 配置）
- [x] 1.4 创建 application-sqlite.yml（Druid SQLite 数据源配置）
- [x] 1.5 创建 application-dev.yml（开发环境覆盖配置）
- [x] 1.6 创建 wh-frontend/ 目录结构及 package.json（Vue 3、Vite 5、Element Plus、Pinia、Axios、Vue Router）
- [x] 1.7 创建 vite.config.js（Vue 插件、Element Plus 自动导入、/api 代理到 :8080、路径别名 @）
- [x] 1.8 创建 .env.local（SPRING_PROFILES_ACTIVE=sqlite,dev、DB_URL、SERVER_PORT）
- [x] 1.9 创建根 package.json（npm run dev 同时起前后端、npm run dev:fe、npm run dev:be、npm run dev:db:migrate）
- [x] 1.10 创建 deploy/docker-compose.yml（Redis 7-alpine + MinIO latest，OrbStack 兼容）
- [x] 1.11 创建 deploy/nginx.conf（/api 代理到 :8080，静态文件指向 dist）
- [x] 1.12 创建 wh-frontend/src/main.js（Vue 实例、Pinia、Router、Element Plus 注册）
- [x] 1.13 创建 wh-frontend/src/App.vue（根组件，仅 router-view）

## 2. 公共层（common）

- [x] 2.1 创建 BaseEntity.java（ID UUID、审计字段、preInsert/preUpdate、@TableLogic、@Version）
- [x] 2.2 创建 R.java 统一响应包装类（code、message、data、timestamp）
- [x] 2.3 创建 ServiceException.java 自定义异常
- [x] 2.4 创建 GlobalExceptionHandler.java（@RestControllerAdvice，统一处理 ServiceException、参数校验、系统异常）
- [x] 2.5 创建 SecurityUtils.java（获取当前用户 ID 工具方法）
- [x] 2.6 创建 MyBatisPlusConfig.java（分页拦截器 SQLite、乐观锁拦截器、MetaObjectHandler 自动填充）
- [x] 2.7 创建 Knife4jConfig.java（Swagger/OpenAPI 文档配置）
- [x] 2.8 创建 WebMvcConfig.java（CORS、静态资源配置）

## 3. Bootstrap 机制（database-bootstrap）

- [x] 3.1 创建 SqliteBootstrap.java（ApplicationRunner，扫描 db/sqlite/*.sql，按文件名排序执行，wh_bootstrap_marker 追踪幂等）
- [x] 3.2 创建 resources/db/sqlite/ 目录
- [x] 3.3 创建 001-system-foundation.sql（sys_user、sys_role、sys_permission、sys_user_role、sys_role_permission 建表 + 索引）
- [x] 3.4 创建 002-wh-sequence.sql（wh_sequence 表建表 + 种子数据 PM_CHARTER 序列）
- [x] 3.5 创建 010-pm-charter.sql（pm_project_charter 建表 + 索引，适配 UUID + BaseEntity）
- [x] 3.6 创建 011-pm-integration.sql（pm_management_plan、pm_plan_baseline、pm_work_log、pm_issue_log、pm_knowledge_base、pm_change_request、pm_project_closure 建表 + 索引）
- [x] 3.7 创建 012-pm-scope.sql（pm_requirement、pm_rtm、pm_scope_statement、pm_wbs_element、pm_scope_verification、pm_scope_variance 建表 + 索引）
- [x] 3.8 创建 013-pm-schedule.sql（pm_activity、pm_activity_dependency、pm_critical_path、pm_activity_estimation、pm_schedule_variance、pm_schedule_alert 建表 + 索引）
- [x] 3.9 创建 014-pm-cost.sql（pm_cost_estimate、pm_budget、pm_actual_cost、pm_evm_analysis 建表 + 索引）
- [x] 3.10 创建 015-pm-quality.sql（pm_quality_audit、pm_quality_defect 建表 + 索引）
- [x] 3.11 创建 016-pm-resource.sql（pm_resource_estimate、pm_resource_assignment、pm_resource_calendar、pm_team_skill、pm_training_record、pm_resource_utilization 建表 + 索引）
- [x] 3.12 创建 017-pm-communication.sql（pm_communication_log、pm_meeting、pm_report、pm_comm_effectiveness 建表 + 索引）
- [x] 3.13 创建 018-pm-risk.sql（pm_risk_register、pm_risk_assessment、pm_risk_quantitative、pm_risk_response、pm_risk_monitor 建表 + 索引）
- [x] 3.14 创建 019-pm-procurement.sql（pm_vendor、pm_procurement_order、pm_procurement_variance、pm_procurement_closure 建表 + 索引）
- [x] 3.15 创建 020-pm-stakeholder.sql（pm_stakeholder、pm_stakeholder_engagement_plan、pm_stakeholder_interaction、pm_stakeholder_satisfaction 建表 + 索引）
- [x] 3.16 创建 021-pm-audit.sql（pm_audit_log 建表 + 索引）
- [x] 3.17 创建 099-pm-charter-seed.sql（10 条章程种子数据，覆盖全状态，PROCESS_INSTANCE_ID=NULL）
- [x] 3.18 创建用户种子数据 SQL（10 用户，BCrypt 密码 Admin@123，整合到 001-system-foundation.sql 或独立文件）
- [x] 3.19 验证所有 SQL 文件幂等性（手动执行两次无报错无重复数据）

## 4. 编号生成服务（sequence-generation）

- [x] 4.1 创建 WhSequence.java 实体类（SEQ_NAME、SEQ_VALUE、SEQ_PREFIX、SEQ_YEAR）
- [x] 4.2 创建 WhSequenceDao.java Mapper 接口
- [x] 4.3 创建 SequenceService.java（generateCode 方法，SQLite 事务中 SELECT → 判断年份重置 → UPDATE +1 → 返回格式化编号）

## 5. 认证链路（system-auth）

- [x] 5.1 创建 JwtTokenProvider.java（生成 JWT、解析 Claims、验证 Token）
- [x] 5.2 创建 JwtAuthenticationFilter.java（从 Authorization 头提取 Token，验证后设置 SecurityContext）
- [x] 5.3 创建 SecurityConfig.java（放行 /api/auth/login、其他 /api/** 需 JWT 认证、禁用 CSRF）
- [x] 5.4 创建 AuthController.java（POST /api/auth/login 登录、POST /api/auth/logout 登出、GET /api/auth/info 获取用户信息）
- [x] 5.5 创建 SysUserBo.java（用户名密码校验、用户信息查询）
- [x] 5.6 创建前端 src/utils/request.js（Axios 实例、请求拦截器附加 Token、响应拦截器处理 401/403/500）
- [x] 5.7 创建前端 src/store/user.js（Pinia Store：token、userInfo、permissions、login/logout/getUserInfo actions）
- [x] 5.8 创建前端 src/router/index.js（路由配置、全局前置守卫：token 校验 → 获取用户信息 → 权限检查）
- [x] 5.9 创建前端 src/views/login/index.vue（登录页：用户名/密码表单、登录按钮、错误提示）
- [x] 5.10 创建前端 src/api/auth.js（loginApi、logoutApi、getUserInfoApi）

## 6. 系统管理（system-administration）

- [x] 6.1 创建 SysUser.java、SysRole.java、SysPermission.java、SysUserRole.java、SysRolePermission.java 实体类
- [x] 6.2 创建对应 Dao.java Mapper 接口
- [x] 6.3 创建 SysUserBo.java（用户 CRUD、角色关联查询）
- [x] 6.4 创建 SysUserController.java（用户/角色/权限 CRUD API）
- [x] 6.5 创建前端 src/api/system.js（系统管理 API 函数）

## 7. 审批引擎（approval-engine）

- [x] 7.1 创建 FlowableConfig.java（Flowable 配置：database-schema-update=true、async-executor-activate=false）
- [x] 7.2 创建 WhApprovalInstance.java 实体类
- [x] 7.3 创建 WhApprovalRecord.java 实体类
- [x] 7.4 创建 WhApprovalFlow.java 实体类
- [x] 7.5 创建 WhApprovalNode.java 实体类
- [x] 7.6 创建对应 Dao.java Mapper 接口
- [x] 7.7 创建 ApprovalCompletedCallback.java 接口（getFlowCode、onApproved、onRejected）
- [x] 7.8 创建 ApprovalCallbackRegistry.java（Spring @Component，@Autowired Map 自动收集回调）
- [x] 7.9 创建 FlowableProcessEndListener.java（监听 Flowable 流程结束事件，查找回调并调用）
- [x] 7.10 创建 WhApprovalBo.java（启动审批流程、查询审批记录）
- [x] 7.11 创建 WhApprovalController.java（审批提交、审批通过、审批驳回 API）
- [x] 7.12 创建 resources/bpmn/pm-charter-approval.bpmn20.xml（开始 → SPONSOR 审批 userTask → 排他网关 → 结束）
- [x] 7.13 创建 resources/mapper/approval/ 目录及 XML 映射文件

## 8. 项目章程后端（project-charter）

- [x] 8.1 创建 WhPmCharter.java 实体类（继承 BaseEntity，所有业务字段，PROCESS_INSTANCE_ID）
- [x] 8.2 创建 WhPmCharterDao.java Mapper 接口
- [x] 8.3 创建 CharterCreateRequest.java / CharterUpdateRequest.java / CharterSubmitRequest.java 请求 DTO
- [x] 8.4 创建 CharterVO.java 响应视图对象（含 createByName 等非持久化字段）
- [x] 8.5 创建 WhPmCharterBo.java（创建/更新/查询/分页/提交审批逻辑，含编号生成调用、状态校验）
- [x] 8.6 创建 WhPmCharterController.java（GET /api/pm/charters、POST /api/pm/charters、GET /api/pm/charters/{id}、PUT /api/pm/charters/{id}、POST /api/pm/charters/{id}/submit）
- [x] 8.7 创建 CharterApprovalCallback.java（@Component 实现 ApprovalCompletedCallback，onApproved/onRejected 逻辑）
- [x] 8.8 创建 resources/mapper/pm/charter/WhPmCharterDao.xml（复杂查询如分页列表、带创建人姓名）
- [x] 8.9 在 application.yml 中新增 app.sqlite.bootstrap-pm-charter 配置开关

## 9. 项目章程前端

- [x] 9.1 创建 src/api/pm/charter.js（getCharterListApi、createCharterApi、getCharterDetailApi、updateCharterApi、submitCharterApi）
- [x] 9.2 创建 src/views/pm/charter/index.vue（列表页：el-table、搜索栏、状态筛选、分页、操作列含编辑/删除/提交/查看）
- [x] 9.3 创建 src/views/pm/charter/detail.vue（详情页：el-descriptions 展示全部字段、审批历史时间线 el-timeline）
- [x] 9.4 创建 src/views/pm/charter/form.vue（新增/编辑页：el-steps 分步表单 → 基本信息 → 项目目标(动态行) → 干系人(动态行) → 预览确认）
- [x] 9.5 创建前端路由配置（/pm/charter、/pm/charter/detail/:id、/pm/charter/form，含 meta.perm 权限标注）
- [x] 9.6 在路由配置中注册 PM 模块路由组

## 10. MinIO 基础设施（minio-storage）

- [x] 10.1 创建 MinioConfig.java（读取 endpoint/accessKey/secretKey，创建 MinioClient Bean）
- [x] 10.2 在 MinioConfig @PostConstruct 中实现 bucket 自动创建（幂等检查 + 创建）
- [x] 10.3 在 application.yml 中配置 MinIO 参数（开发环境默认 minioadmin/minioadmin123）

## 11. 集成验证

- [x] 11.1 启动 Docker 服务（orb launch 或 orb up），验证 Redis :6379 和 MinIO :9000 可用
- [x] 11.2 执行 npm run dev，验证前后端同时启动
- [x] 11.3 验证 Bootstrap SQL 执行成功（50+ 表创建，种子数据插入）
- [x] 11.4 验证登录功能（使用 admin/Admin@123 登录，获取 Token）
- [x] 11.5 验证章程创建（创建章程，验证编号自动生成 CHARTER-2026-001）
- [x] 11.6 验证章程列表/详情页面（10 条种子数据显示正常）
- [x] 11.7 验证 Flowable 审批流程（提交章程 → SPONSOR 审批 → 状态更新为 APPROVED）
- [x] 11.8 验证权限拦截（非 PM 用户不可创建章程，非 SPONSOR 不可审批）
