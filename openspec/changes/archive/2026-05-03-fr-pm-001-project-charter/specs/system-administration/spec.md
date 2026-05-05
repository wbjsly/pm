## ADDED Requirements

### Requirement: 系统用户管理

系统 SHALL 提供用户的 CRUD 操作。用户实体包含：ID（UUID）、用户名（唯一）、BCrypt 密码、真实姓名、部门 ID、员工号、手机号、邮箱、就职状态。用户名 SHALL 全局唯一。

#### Scenario: 创建新用户

- **WHEN** 管理员创建用户，提供唯一的用户名和密码
- **THEN** 用户创建成功，密码以 BCrypt 哈希存储

#### Scenario: 创建重复用户名的用户

- **WHEN** 尝试创建与现有用户同名的新用户
- **THEN** 系统返回 409 冲突错误

### Requirement: 角色管理

系统 SHALL 提供角色的 CRUD 操作。角色包含：ID（UUID）、角色名称、角色代码（唯一）。系统 SHALL 预置 8 个角色：系统管理员(ROLE_ADMIN)、项目经理(ROLE_PM)、项目发起人(ROLE_SPONSOR)、后端开发(ROLE_BE)、前端开发(ROLE_FE)、质量经理(ROLE_QA)、架构师(ROLE_ARCH)、业务分析师(ROLE_BA)。

#### Scenario: 角色预置

- **WHEN** 系统首次启动并执行 Bootstrap SQL
- **THEN** 8 个预置角色被插入到 sys_role 表中

### Requirement: 权限管理

系统 SHALL 提供权限的 CRUD 操作。权限包含：ID（UUID）、父级 ID、权限名称、权限代码、权限类型（M=菜单/B=按钮/A=接口）、路径、组件、图标、排序号、可见性。权限 SHALL 支持树形结构。

#### Scenario: 权限种子初始化

- **WHEN** 系统启动并执行 Bootstrap SQL
- **THEN** PM 模块的权限记录（如 pm:charter:list、pm:charter:edit 等）被插入

### Requirement: 用户-角色关联

系统 SHALL 支持为用户分配角色，一个用户可拥有多个角色。关联关系存储在 sys_user_role 表中。

#### Scenario: 为用户分配角色

- **WHEN** 将角色 R002(PM) 关联到用户 U000002
- **THEN** sys_user_role 中新增关联记录

### Requirement: 角色-权限关联

系统 SHALL 支持为角色分配权限，一个角色可拥有多个权限。关联关系存储在 sys_role_permission 表中。

#### Scenario: 角色拥有权限

- **WHEN** 将权限 P_PM_001 关联到角色 R002(PM)
- **THEN** sys_role_permission 中新增关联记录，拥有 R002 角色的用户可访问对应权限

### Requirement: 用户权限查询

系统 SHALL 提供 GET /api/auth/info 接口，返回当前登录用户的完整信息，包括用户 ID、用户名、真实姓名、角色列表和权限代码列表。

#### Scenario: 获取用户信息

- **WHEN** 已认证用户请求 GET /api/auth/info
- **THEN** 返回用户基本信息、角色列表和权限列表

### Requirement: 种子用户初始化

系统 SHALL 预置 10 个种子用户（admin/zhangsan/lisi/wangwu/zhaoliu/sunqi/zhouba/wujiu/zhengshi/fengshiyi），统一初始密码 Admin@123（BCrypt 哈希），并预置对应的角色关联。

#### Scenario: 种子用户可用

- **WHEN** 系统完成 Bootstrap SQL 执行
- **THEN** 10 个种子用户存在，可使用 Admin@123 密码登录
