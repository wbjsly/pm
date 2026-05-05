## ADDED Requirements

### Requirement: JWT 认证

系统 SHALL 使用 JWT（jjwt 0.11.5）进行用户认证。登录成功后签发包含用户 ID、用户名、角色列表的 JWT Token。Token 有效期为 24 小时。所有受保护的 API 端点 SHALL 要求请求头中包含有效的 JWT Token（格式：Authorization: Bearer <token>）。

#### Scenario: 成功登录获取 Token

- **WHEN** 用户使用正确的用户名和密码调用 POST /api/auth/login
- **THEN** 系统返回包含 JWT Token 和用户信息的响应

#### Scenario: 使用无效密码登录

- **WHEN** 用户使用正确的用户名但错误的密码调用登录接口
- **THEN** 系统返回 401 错误

#### Scenario: 携带有效 Token 访问受保护接口

- **WHEN** 请求头中包含有效的 JWT Token
- **THEN** 系统验证 Token 成功，允许访问受保护的 API

#### Scenario: 携带无效 Token 访问受保护接口

- **WHEN** 请求头中包含过期或伪造的 JWT Token
- **THEN** 系统返回 401 错误

### Requirement: JWT 黑名单

系统 SHALL 支持将已登出的 Token 加入 Redis 黑名单。登出后原 Token 不可再使用。黑名单 TTL 与 Token 有效期一致（24 小时）。

#### Scenario: 用户登出后 Token 失效

- **WHEN** 用户调用登出接口
- **THEN** 当前 Token 被加入 Redis 黑名单，后续使用该 Token 的请求返回 401

### Requirement: Spring Security 配置

系统 SHALL 使用 Spring Security 配置以下规则：/api/auth/login 和 /api/auth/register 端点无需认证；所有其他 /api/** 端点需要 JWT 认证；JWT 认证通过 Spring Security 过滤器链实现。

#### Scenario: 访问公开端点无需 Token

- **WHEN** 请求 POST /api/auth/login 且未携带 Token
- **THEN** 请求被正常处理

#### Scenario: 访问受保护端点无 Token

- **WHEN** 请求 GET /api/pm/charters 且未携带 Token
- **THEN** 系统返回 401 错误

### Requirement: 前端登录页

前端 SHALL 提供登录页面（/login），包含用户名和密码输入框。登录成功后将 Token 存储到 localStorage（键名 wh_token），并跳转到首页仪表盘。Axios 请求拦截器 SHALL 自动从 localStorage 读取 Token 并附加到请求头。

#### Scenario: 前端登录成功

- **WHEN** 用户在登录页输入正确的用户名和密码并提交
- **THEN** Token 存储到 localStorage，页面跳转到 /dashboard

#### Scenario: 前端登录后请求自动携带 Token

- **WHEN** 登录成功后发起 API 请求
- **THEN** 请求头自动包含 Authorization: Bearer <token>

### Requirement: 路由权限守卫

前端路由 SHALL 支持 meta.perm 字段定义权限标识。路由守卫 SHALL 在导航时检查用户权限，无权限时返回 403 页面。

#### Scenario: 有权限访问路由

- **WHEN** 用户访问具有 pm:charter:list 权限的路由
- **THEN** 路由导航成功

#### Scenario: 无权限访问路由

- **WHEN** 用户访问不具有对应权限的路由
- **THEN** 页面显示 403 无权限
