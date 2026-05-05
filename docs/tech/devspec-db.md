# WH 管理系统 - 数据库架构设计与表设计规范

> 项目：WH 管理系统（统一 ERP+CRM+PM+HR 管理平台）
> 数据库：SQLite 3（唯一运行时数据库）
> 分支：`dev`（主开发分支）

---

## 一、数据库架构总览

### 1.1 技术选型

| 项目 | 选型 | 说明 |
|------|------|------|
| 数据库 | SQLite 3 | 嵌入式关系型数据库，零配置 |
| JDBC 驱动 | `sqlite-jdbc` | Xerial 官方驱动 |
| 连接池 | Alibaba Druid | 校验语句 `SELECT 1` |
| ORM | MyBatis Plus 3.5.5 | 自动 SQLite 方言分页 |
| 迁移方式 | SQL 脚本 + 启动时 Bootstrap | 脚本按序号递增执行 |
| 数据库位置 | `data/wh.sqlite`（开发/测试） | 生产环境可配置路径 |

### 1.2 架构定位

SQLite 在 WH 中的角色：

```
┌─────────────────────────────────────────────────┐
│                   应用层                         │
│  Spring Boot 2.7.18 + MyBatis Plus 3.5.5        │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐ │
│  │ Controller│ → │   BO    │ → │     DAO        │ │
│  └──────────┘ └──────────┘ └──────────────────┘ │
│                                     ↓            │
│                          MyBatis Plus 拦截器      │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐ │
│  │  动态表名  │ │  分页方言 │ │   乐观锁拦截器    │ │
│  └──────────┘ └──────────┘ └──────────────────┘ │
└─────────────────────────────────────────────────┘
                    ↓ Druid 连接池
┌─────────────────────────────────────────────────┐
│              SQLite 3 数据库                      │
│  data/wh.sqlite（单文件，包含所有表、索引）       │
│  无外部数据库服务进程，嵌入 JVM 进程              │
└─────────────────────────────────────────────────┘
```

**核心理念：**
- **零运维**：无需安装/配置独立数据库服务
- **单文件**：整个数据库就是一个文件，备份/迁移极其简单
- **表 + 索引 + 业务代码**：不使用视图、触发器、存储过程、函数
- **应用层保证数据完整性**：外键约束在应用层实现

### 1.3 数据库文件组织

```
deploy/init-sql/sqlite/                    # 迁移脚本（权威路径）
├── migration-20260408-wh-expense-master.sql
├── migration-20260409-wh-payment-application.sql
├── migration-20260410-wh-erp-pm-timesheet.sql
└── ...（按序号递增，80+ 脚本）

wh-backend/src/main/resources/db/sqlite/  # 启动时 Bootstrap 脚本
├── expense-master-sqlite-ddl.sql
├── pm-timesheet-sqlite-ddl.sql
└── ...

wh-backend/src/main/resources/mapper/     # MyBatis XML 映射
├── contract/
├── opportunity/
├── erp/
└── ...
```

### 1.4 配置文件

**application-sqlite.yml：**

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: org.sqlite.JDBC
    url: ${DB_URL:jdbc:sqlite:data/wh.sqlite}
    username: ""
    password: ""
    druid:
      initial-size: 1
      min-idle: 1
      max-active: 20
      max-wait: 60000
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false

app:
  database-id: sqlite
  database-type: sqlite
  sqlite:
    bootstrap-expense-master: ${AUTO_BOOTSTRAP_EXPENSE_MASTER:true}
    bootstrap-expense-report: ${AUTO_BOOTSTRAP_EXPENSE_REPORT:true}
    bootstrap-pm-timesheet: ${AUTO_BOOTSTRAP_PM_TIMESHEET:true}
    bootstrap-pm-project-foundation: ${AUTO_BOOTSTRAP_PM_PROJECT_FOUNDATION:true}
    bootstrap-hr-hiring: ${AUTO_BOOTSTRAP_HR_HIRING:true}

mybatis-plus:
  configuration:
    jdbc-type-for-null: VARCHAR
```

### 1.5 环境变量

| 变量 | 说明 | 示例 |
|------|------|------|
| `SPRING_PROFILES_ACTIVE` | 激活的 Spring 配置 | `sqlite,dev` |
| `DB_URL` | SQLite 数据库文件路径 | `jdbc:sqlite:/data/wh.sqlite` |
| `SERVER_PORT` | 后端端口 | `8080` |

---

## 二、数据库设计原则

### 2.1 设计约束

| 原则 | 说明 |
|------|------|
| **仅使用表和索引** | 不依赖视图、存储过程、函数、触发器、事件 |
| **业务逻辑在代码中** | 所有规则、权限、状态回算放在后端业务代码与 SQL 查询层 |
| **应用层外键** | 不使用数据库级 FOREIGN KEY 约束，通过列命名约定和索引保证关联关系 |
| **逻辑删除统一** | 所有业务数据通过 `DEL_FLAG` 标志逻辑删除 |
| **UUID 主键** | 应用层生成 32 位 UUID，不使用自增 ID |
| **可重复执行** | 迁移脚本必须幂等（使用 `CREATE TABLE IF NOT EXISTS` 等） |

### 2.2 SQLite 特性利用

| 特性 | 使用方式 |
|------|----------|
| 动态类型（Type Affinity） | 列声明类型作为亲和类型指导存储 |
| ROWID 隐式主键 | SQLite 自动维护 ROWID，但业务上使用 UUID 主键 |
| LIMIT/OFFSET | MyBatis Plus 自动使用 LIMIT/OFFSET 分页 |
| 内置函数 | `COALESCE`、`SUBSTR`、`INSTR`、`strftime` 等 ANSI 兼容函数 |
| WAL 模式 | 生产环境可开启 `PRAGMA journal_mode=WAL` 提升并发写入性能 |

### 2.3 SQLite 限制与规避

| 限制 | 规避方案 |
|------|----------|
| 无 ALTER TABLE DROP COLUMN（SQLite < 3.35.0） | 新版本已支持，但仍避免 ALTER，优先新增表 |
| 无外键强制（默认关闭） | 应用层验证，不依赖 SQLite PRAGMA foreign_keys |
| 单文件并发写入限制 | WAL 模式 + 连接池控制，写操作串行化 |
| 无序列（SEQUENCE） | 应用层 UUID 生成 |
| 无 SYSDATE/SYSTIMESTAMP | 使用 `datetime('now')` 或应用层设置时间戳 |
| 无 NUMBER 类型 | 使用 `REAL`（浮点）或 `INTEGER`（整数），金额用 `TEXT` 存储精确小数 |

---

## 三、表设计规范

### 3.1 表命名规范

**格式：** `wh_<模块>_<实体>`（全小写，下划线分隔）

```
wh_customer          — 客户表
wh_contract          — 合同表
wh_opportunity       — 商机表
wh_payment_application — 付款申请表
wh_erp_pm_timesheet  — 项目工时表（erp/pm 子模块）
wh_expense_report    — 费用报销表
sys_user              — 系统用户表（无前缀）
sys_role              — 系统角色表
sys_permission        — 系统权限表
```

**命名规则：**
- 全小写字母 + 下划线
- 业务表以 `wh_` 为前缀
- 系统表以 `sys_` 为前缀
- 关联表/中间表：`wh_<主模块>_<从模块>_rel` 或 `<主表>_line`（明细）
- **SQLite 标识符无字节限制**，但建议不超过 40 字符以保持可读性

### 3.2 列设计规范

#### 3.2.1 公共审计列（BaseEntity）

每张业务表**必须**包含以下标准列：

| 列名 | SQLite 类型 | 默认值 | 说明 |
|------|-------------|--------|------|
| `ID` | TEXT NOT NULL | — | 主键，32 位 UUID（无连字符） |
| `CREATE_BY` | TEXT | NULL | 创建人用户 ID |
| `CREATE_DATE` | TEXT | `datetime('now', 'localtime')` | 创建时间（ISO 8601 格式） |
| `UPDATE_BY` | TEXT | NULL | 更新人用户 ID |
| `UPDATE_DATE` | TEXT | `datetime('now', 'localtime')` | 更新时间 |
| `REMARKS` | TEXT | NULL | 备注（最长 500 字符） |
| `DEL_FLAG` | TEXT | `'0'` | 逻辑删除标志（`'0'`=正常 / `'1'`=已删除） |
| `VER_NO` | INTEGER | `0` | 乐观锁版本号 |
| `SYS_CODE` | TEXT | NULL | 系统编码 |

**Java 实体实现：**

```java
@Data
public abstract class BaseEntity implements Serializable {

    @TableId(value = "ID", type = IdType.ASSIGN_UUID)
    private String id;  // 32 位 UUID

    @TableField("CREATE_BY")
    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "CREATE_DATE", fill = FieldFill.INSERT)
    private Date createDate;

    @TableField("UPDATE_BY")
    private String updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "UPDATE_DATE", fill = FieldFill.INSERT_UPDATE)
    private Date updateDate;

    @TableField("REMARKS")
    private String remarks;

    @TableLogic
    @TableField("DEL_FLAG")
    private String delFlag;  // "0" = 正常, "1" = 已删除

    @Version
    @TableField("VER_NO")
    private Integer verNo;   // 乐观锁

    @TableField("SYS_CODE")
    private String sysCode;

    // 非持久化字段
    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private String updateByName;

    public void preInsert(String userId) {
        this.createBy = userId;
        this.updateBy = userId;
        this.createDate = new Date();
        this.updateDate = new Date();
        this.delFlag = "0";
        this.verNo = 0;
    }

    public void preUpdate(String userId) {
        this.updateBy = userId;
        this.updateDate = new Date();
    }
}
```

#### 3.2.2 业务列类型映射

| Java 类型 | SQLite 类型 | 示例 | 说明 |
|-----------|-------------|------|------|
| `String` | TEXT | `VARCHAR(32)` → TEXT | 主键/外键/短编码 |
| `String` | TEXT | `VARCHAR(50)` → TEXT | 用户名、手机号 |
| `String` | TEXT | `VARCHAR(64)` → TEXT | 编号、系统标识 |
| `String` | TEXT | `VARCHAR(100)` → TEXT | 名称、标题 |
| `String` | TEXT | `VARCHAR(255)` → TEXT | 描述、地址 |
| `String` | TEXT | `VARCHAR(500)` → TEXT | 备注、较长文本 |
| `String` | TEXT | `VARCHAR(1000)` → TEXT | 长描述 |
| `String` | TEXT | — | 大文本（BPMN XML、请求参数） |
| `BigDecimal` | TEXT | — | 金额（精确小数，用 TEXT 存储避免浮点误差） |
| `BigDecimal` | REAL | — | 工时、小数值 |
| `Integer` | INTEGER | — | 整数、排序号 |
| `Long` | INTEGER | — | 长整数 |
| `Date` | TEXT | — | 日期（`yyyy-MM-dd` 格式） |
| `Date` | TEXT | — | 日期时间（ISO 8601 格式） |
| `Boolean` | TEXT | — | 标志位（`'Y'`/`'N'`、`'0'`/`'1'`） |
| `byte[]` | BLOB | — | 二进制数据（文件附件） |

**类型说明：**
- SQLite 使用**类型亲和性**（Type Affinity），列声明的类型只是存储类别的建议
- `TEXT` 亲和性可存储字符串，SQLite 会按文本排序比较
- `INTEGER` 亲和性存储为整数（1-8 字节有符号）
- `REAL` 亲和性存储为 IEEE 754 浮点数（8 字节）
- `BLOB` 亲和性按原样存储（不转换）
- 金额类数据建议用 `TEXT` 存储精确小数，避免 `REAL` 的浮点误差；也可用 `INTEGER` 存储"分"单位

#### 3.2.3 标志列命名约定

| 标志 | 类型 | 取值 | 说明 |
|------|------|------|------|
| `DEL_FLAG` | TEXT | `'0'` / `'1'` | 逻辑删除（固定值） |
| `ACTIVE_FLAG` | TEXT | `'Y'` / `'N'` | 启用/停用 |
| `VISIBLE` | TEXT | `'Y'` / `'N'` | 可见/隐藏 |
| `IS_*` 前缀 | TEXT | `'Y'` / `'N'` | 布尔标志（如 `IS_MANUAL_TASK`） |
| `*_FLAG` 后缀 | TEXT | `'Y'` / `'N'` | 通用标志（如 `NO_VOUCHER_FLAG`） |

### 3.3 约束设计

#### 3.3.1 主键约束

```sql
CREATE TABLE wh_payment_application (
    ID TEXT NOT NULL,
    -- ...
    CONSTRAINT PK_WH_PAY_APP PRIMARY KEY (ID)
);
```

- 主键列使用 `TEXT` 类型（UUID 字符串）
- 约束命名：`PK_<表缩写>`（如 `PK_WH_PAY_APP`）

#### 3.3.2 检查约束（CHECK）

```sql
CREATE TABLE wh_erp_pm_timesheet (
    -- ...
    STATUS TEXT DEFAULT 'DRAFT' NOT NULL,
    BELONG_MONTH INTEGER NOT NULL,
    CONSTRAINT CK_WH_ERP_PM_TS_STATUS CHECK (STATUS IN (
        'DRAFT','SUBMITTED','APPROVED','REJECTED','WITHDRAWN','ADJUSTING'
    )),
    CONSTRAINT CK_WH_ERP_PM_TS_MONTH CHECK (BELONG_MONTH BETWEEN 1 AND 12)
);
```

- 约束命名：`CK_<表缩写>_<字段>`
- 用于枚举值范围、数值边界校验

#### 3.3.3 唯一约束

```sql
CREATE TABLE wh_contract (
    -- ...
    CONTRACT_CODE TEXT,
    CONSTRAINT UK_WH_CONTRACT_CODE UNIQUE (CONTRACT_CODE)
);
```

- 约束命名：`UK_<表缩写>_<字段>`
- 业务唯一键（如合同编号、报销单号）

#### 3.3.4 非空约束

```sql
ID TEXT NOT NULL,
USER_ID TEXT NOT NULL,
WORK_CONTENT TEXT NOT NULL,
HOURS REAL NOT NULL,
```

- 直接在列定义中使用 `NOT NULL`
- 必填字段必须声明

### 3.4 索引设计

#### 3.4.1 索引命名

**格式：** `IDX_<表缩写>_<列名>`

```sql
CREATE INDEX IDX_PM_TS_USER_PERIOD
    ON wh_erp_pm_timesheet (USER_ID, BELONG_YEAR, BELONG_MONTH, DEL_FLAG);

CREATE INDEX IDX_PM_TSL_HEAD
    ON wh_erp_pm_timesheet_line (TIMESHEET_ID, DEL_FLAG, WORK_DATE);

CREATE INDEX IDX_WH_PAYAPP_APPLY_NO
    ON wh_payment_application (APPLY_NO);

CREATE INDEX IDX_EXP_ITEM_PURPOSE
    ON wh_expense_item (PURPOSE_ID);
```

**命名规则：**
- 单列索引：`IDX_<表缩写>_<列名>`
- 复合索引：`IDX_<表缩写>_<主要列1>_<主要列2>`
- 按查询频率排序：高频率查询列在前

#### 3.4.2 索引设计原则

| 场景 | 是否建索引 | 说明 |
|------|-----------|------|
| 外键列 | 是 | 关联查询频繁 |
| WHERE 条件列 | 是 | 高选择性列优先 |
| ORDER BY 列 | 是 | 排序列可提升性能 |
| 复合查询 | 是 | 复合索引按查询条件顺序 |
| 低选择性列 | 否 | 如性别、状态种类很少时 |
| 频繁更新的列 | 否 | 写入代价高于读取收益 |
| 小表（< 1000 行） | 否 | 全表扫描更快 |

#### 3.4.3 DEL_FLAG 索引策略

所有包含逻辑删除标志的索引**将 DEL_FLAG 放在最后**：

```sql
-- 正确：DEL_FLAG 在最后，WHERE 条件通常包含 del_flag = '0'
CREATE INDEX IDX_PM_TSL_HEAD ON wh_erp_pm_timesheet_line (TIMESHEET_ID, WORK_DATE, DEL_FLAG);

-- 也正确：当 DEL_FLAG 是第一筛选条件时
CREATE INDEX IDX_PM_TS_USER_PERIOD ON wh_erp_pm_timesheet (USER_ID, BELONG_YEAR, BELONG_MONTH, DEL_FLAG);
```

### 3.5 外键关系设计

**WH 不使用数据库级 FOREIGN KEY 约束。** 关联关系通过以下方式保证：

1. **列命名约定**：外键列以 `_ID` 结尾（如 `CUSTOMER_ID`、`CONTRACT_ID`）
2. **索引支持**：在外键列上建立索引加速关联查询
3. **应用层验证**：BO 层验证外键引用存在性
4. **逻辑删除传播**：主表删除时，子表记录通过业务逻辑处理

```sql
-- 无 FOREIGN KEY 约束，仅有索引
CREATE INDEX IDX_PM_STK_PROJECT ON wh_erp_pm_stakeholder (PROJECT_ID);
CREATE INDEX IDX_PM_TSL_HEAD ON wh_erp_pm_timesheet_line (TIMESHEET_ID, DEL_FLAG, WORK_DATE);
```

---

## 四、数据迁移脚本规范

### 4.1 脚本组织

**权威路径：** `deploy/init-sql/sqlite/`

**命名格式：** `migration-YYYYMMDD-<description>.sql`

```
migration-20260408-wh-expense-master.sql      — 费用主数据
migration-20260409-wh-payment-application.sql — 付款申请
migration-20260410-wh-erp-pm-timesheet.sql    — 项目工时
migration-20260458-wh-contract-revenue-item-line-tax.sql — 合同收入行税率
```

### 4.2 脚本内容规范

#### 4.2.1 建表模板

```sql
-- 付款申请（财务信息 / FI-02 类业务台账）
-- 幂等：表以存在性判断；菜单以 PERM_CODE / ID 判断

CREATE TABLE IF NOT EXISTS wh_payment_application (
    ID              TEXT NOT NULL,
    APPLY_NO        TEXT,
    APPLY_TITLE     TEXT,
    APPLY_DATE      TEXT,
    APPLICANT_USER_ID TEXT,
    APPLICANT_NAME  TEXT,
    LEGAL_ENTITY_NAME TEXT,
    CURRENCY_CODE   TEXT DEFAULT 'CNY',
    PAY_AMOUNT      TEXT,
    PAY_DATE        TEXT,
    PAYEE_NAME      TEXT,
    BANK_NAME       TEXT,
    BANK_ACCOUNT_NO TEXT,
    CREATE_BY       TEXT,
    CREATE_DATE     TEXT DEFAULT (datetime('now', 'localtime')),
    UPDATE_BY       TEXT,
    UPDATE_DATE     TEXT DEFAULT (datetime('now', 'localtime')),
    REMARKS         TEXT,
    DEL_FLAG        TEXT DEFAULT '0',
    VER_NO          INTEGER DEFAULT 0 NOT NULL,
    SYS_CODE        TEXT,
    CONSTRAINT PK_WH_PAY_APP PRIMARY KEY (ID)
);

CREATE INDEX IF NOT EXISTS IDX_WH_PAYAPP_APPLY_NO
    ON wh_payment_application (APPLY_NO);

CREATE INDEX IF NOT EXISTS IDX_WH_PAYAPP_APPLY_DATE
    ON wh_payment_application (APPLY_DATE);
```

#### 4.2.2 种子数据模板

```sql
-- 权限种子（幂等插入）
INSERT INTO sys_permission (
    ID, PARENT_ID, PERM_NAME, PERM_CODE, PERM_TYPE, PATH, COMPONENT,
    ICON, SORT_ORDER, VISIBLE, CREATE_BY, CREATE_DATE, DEL_FLAG, VER_NO
)
SELECT 'P091', 'P016', '付款申请', 'finance:payment-application', 'M',
       '/finance-recovery/payment-application', 'finance-recovery/payment-application',
       'WalletFilled', 5, 'Y', '1', datetime('now', 'localtime'), '0', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE ID = 'P091')
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission
      WHERE PERM_CODE = 'finance:payment-application' AND DEL_FLAG = '0'
  );

-- 角色权限关联（幂等插入）
INSERT INTO sys_role_permission (ID, ROLE_ID, PERMISSION_ID)
SELECT 'RP091', 'R001', 'P091'
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE ID = 'RP091')
  AND EXISTS (
      SELECT 1 FROM sys_permission WHERE ID = 'P091' AND DEL_FLAG = '0'
  );
```

#### 4.2.3 UPSERT 模板（INSERT OR REPLACE / INSERT OR IGNORE）

```sql
-- 用途种子数据（UPSERT 模式）
INSERT OR IGNORE INTO wh_expense_purpose (
    ID, PURPOSE_NAME, SORT_ORDER, REQUIRE_PRIOR_EXPENSE_FLAG,
    WORKFLOW_MODE, OFFICIAL_DOC_FLOW_CODE, OFFICIAL_LINK_ONCE_FLAG,
    PAYMENT_METHOD, NO_VOUCHER_FLAG, ACTIVE_FLAG,
    CREATE_BY, UPDATE_BY, DEL_FLAG, VER_NO, CREATE_DATE, UPDATE_DATE
) VALUES (
    'e100000000000000000000000000001', '差旅', 100, 'N',
    'OFFICIAL_DOC', 'CY-01', 'Y',
    '银行存款', 'N', 'Y',
    '1', '1', '0', 0, datetime('now', 'localtime'), datetime('now', 'localtime')
);
```

#### 4.2.4 增量修改模板（ALTER TABLE）

```sql
-- 添加列（SQLite 3.35.0+ 支持 DROP COLUMN）
ALTER TABLE wh_contract ADD COLUMN TAX_RATE REAL;
ALTER TABLE wh_contract ADD COLUMN PRODUCT_CATEGORY TEXT;

-- 添加索引
CREATE INDEX IF NOT EXISTS IDX_CONTRACT_TAX_RATE
    ON wh_contract (TAX_RATE);
```

### 4.3 幂等性要求

所有迁移脚本**必须可重复执行**：

| 操作 | 幂等写法 |
|------|----------|
| 建表 | `CREATE TABLE IF NOT EXISTS` |
| 建索引 | `CREATE INDEX IF NOT EXISTS` |
| 建约束 | 先检查 `sqlite_master` 不存在再执行 |
| 插入种子 | `INSERT ... WHERE NOT EXISTS` 或 `INSERT OR IGNORE` |
| UPSERT | `INSERT OR REPLACE` 或 `INSERT ... ON CONFLICT` |
| 添加列 | `ALTER TABLE ... ADD COLUMN`（SQLite 天然幂等，重复列名会报错） |

### 4.4 执行方式

```bash
# 全量迁移（自动检测）
npm run dev:db:migrate

# JDBC 模式（推荐）
npm run dev:db:migrate:jdbc

# 模块专用迁移
npm run dev:db:migrate:sqlite:payment
npm run dev:db:migrate:sqlite:cert
npm run dev:db:migrate:sqlite:seal
npm run dev:db:migrate:sqlite:stock-doc
```

---

## 五、UUID 主键策略

### 5.1 应用层 UUID 生成

**不依赖数据库序列或自增 ID。** 所有主键在应用层生成：

```java
@TableId(value = "ID", type = IdType.ASSIGN_UUID)
private String id;
```

**ID 格式：** 32 位 UUID 字符串（无连字符），如 `F00000000000000000000000000EDR1`

### 5.2 种子数据 ID 约定

迁移脚本中的种子数据使用**有前缀的固定 ID**，便于识别和调试：

| 前缀 | 含义 | 示例 |
|------|------|------|
| `F` + 数字 | 流程定义 ID | `F00000000000000000000000000EDR1` |
| `N` + 数字 | 审批节点 ID | `N00000000000000000000000000EDR1` |
| `P` + 数字 | 权限 ID | `P091` |
| `RP` + 数字 | 角色-权限关联 ID | `RP091` |
| `R` + 数字 | 角色 ID | `R001` |
| `e` + 数字 | 费用用途 ID | `e100000000000000000000000000001` |

### 5.3 为什么不使用自增 ID

| 对比项 | UUID | 自增 ID |
|--------|------|---------|
| 分布式友好 | 是 | 否（需协调） |
| 前端暴露安全 | 是（不可推测） | 否（可推测总数） |
| 种子数据可控 | 是（固定 ID） | 否（依赖插入顺序） |
| 合并数据冲突 | 无 | 可能冲突 |
| 索引大小 | 较大（32 字符） | 小（4-8 字节） |
| 插入性能 | 可接受（UUID 非连续） | 最优（连续） |

WH 选择 UUID 的权衡：牺牲少量插入性能，换取分布式安全和数据合并能力。

---

## 六、逻辑删除设计

### 6.1 数据库层

```sql
DEL_FLAG TEXT DEFAULT '0'
```

- `'0'` = 正常（未删除）
- `'1'` = 已删除（逻辑删除）

### 6.2 应用层

```java
@TableLogic
@TableField("DEL_FLAG")
private String delFlag;
```

MyBatis Plus 自动在所有查询中添加 `WHERE DEL_FLAG = '0'` 条件。

### 6.3 查询模式

```sql
-- 业务查询（MyBatis Plus 自动追加 del_flag = '0'）
SELECT * FROM wh_contract WHERE DEL_FLAG = '0' AND CUSTOMER_ID = ?

-- 历史/回收站查询（需手动绕过）
SELECT * FROM wh_contract WHERE DEL_FLAG = '1'
```

### 6.4 索引策略

所有业务索引都包含 `DEL_FLAG` 列，确保过滤已删除记录时走索引：

```sql
CREATE INDEX IDX_PM_TS_USER_PERIOD
    ON wh_erp_pm_timesheet (USER_ID, BELONG_YEAR, BELONG_MONTH, DEL_FLAG);
```

---

## 七、日期与时间设计

### 7.1 存储格式

SQLite 推荐使用 **TEXT 类型** 存储日期时间，格式为 ISO 8601：

| 类型 | 格式 | 示例 |
|------|------|------|
| 纯日期 | `yyyy-MM-dd` | `2026-05-02` |
| 日期时间 | `yyyy-MM-dd HH:mm:ss` | `2026-05-02 14:30:00` |
| ISO 8601（推荐） | `yyyy-MM-ddTHH:mm:ss` | `2026-05-02T14:30:00` |

### 7.2 默认值

```sql
CREATE_DATE TEXT DEFAULT (datetime('now', 'localtime'))
UPDATE_DATE TEXT DEFAULT (datetime('now', 'localtime'))
```

### 7.3 常用日期函数

| 函数 | 说明 | 示例 |
|------|------|------|
| `datetime('now')` | 当前 UTC 时间 | — |
| `datetime('now', 'localtime')` | 当前本地时间 | — |
| `date('now')` | 当前日期 | — |
| `strftime('%Y-%m', CREATE_DATE)` | 格式化日期 | 提取年月 |
| `julianday(END_DATE) - julianday(START_DATE)` | 日期差（天） | 计算间隔 |

### 7.4 MyBatis Plus 自动填充

```java
@Bean
public MetaObjectHandler metaObjectHandler() {
    return new MetaObjectHandler() {
        @Override
        public void insertFill(MetaObject metaObject) {
            this.strictInsertFill(metaObject, "createDate", Date.class, new Date());
            this.strictInsertFill(metaObject, "updateDate", Date.class, new Date());
        }
        @Override
        public void updateFill(MetaObject metaObject) {
            this.strictUpdateFill(metaObject, "updateDate", Date.class, new Date());
        }
    };
}
```

应用层自动填充，不依赖数据库默认值。

---

## 八、MyBatis Plus 适配

### 8.1 分页配置

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(
        new PaginationInnerInterceptor(DbType.SQLITE)
    );
    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
    return interceptor;
}
```

SQLite 使用 `LIMIT/OFFSET` 分页：

```sql
SELECT * FROM wh_contract WHERE DEL_FLAG = '0' ORDER BY CREATE_DATE DESC
LIMIT 10 OFFSET 20
```

### 8.2 数据库 ID 提供者

```java
@Bean
public VendorDatabaseIdProvider databaseIdProvider() {
    Properties properties = new Properties();
    properties.setProperty("SQLite", "sqlite");
    VendorDatabaseIdProvider provider = new VendorDatabaseIdProvider();
    provider.setProperties(properties);
    return provider;
}
```

用于 MyBatis XML 中的条件分支：

```xml
<choose>
    <when test="_databaseId == 'sqlite'">
        COALESCE(u.real_name, u.username)
    </when>
    <otherwise>
        -- 其他数据库的写法
    </otherwise>
</choose>
```

### 8.3 常用 SQLite SQL 函数映射

| 原 Oracle 函数 | SQLite 替代 |
|---------------|-------------|
| `NVL(a, b)` | `COALESCE(a, b)` |
| `SYSTIMESTAMP` | `datetime('now', 'localtime')` |
| `SYSDATE` | `date('now')` 或 `datetime('now')` |
| `TO_CHAR(date, 'YYYY-MM-DD')` | `strftime('%Y-%m-%d', date)` |
| `TO_DATE(str, 'YYYY-MM-DD')` | `date(str)` 或直接存储 TEXT |
| `INSTR(str, sub)` | `INSTR(str, sub)`（SQLite 内置） |
| `SUBSTR(str, start, len)` | `SUBSTR(str, start, len)` |
| `TRUNC(date)` | `date(date)` |
| `a \|\| b`（连接符） | `a \|\| b`（SQLite 也支持） 或 `COALESCE(a, '') \|\| COALESCE(b, '')` |
| `CASE WHEN ... THEN ... END` | `CASE WHEN ... THEN ... END`（SQLite 支持） |
| `NULLS LAST` | 不支持，需用 `CASE` 模拟 |
| `ROWNUM` | `LIMIT n` |

---

## 九、典型表设计示例

### 9.1 主表 + 明细表模式

**主表：wh_contract（合同主表）**

```sql
CREATE TABLE IF NOT EXISTS wh_contract (
    ID                  TEXT NOT NULL,
    CONTRACT_CODE       TEXT,
    CONTRACT_NAME       TEXT NOT NULL,
    CUSTOMER_ID         TEXT NOT NULL,
    CONTRACT_TYPE       TEXT,
    SIGN_DATE           TEXT,
    START_DATE          TEXT,
    END_DATE            TEXT,
    CONTRACT_AMOUNT     TEXT,
    CURRENCY_CODE       TEXT DEFAULT 'CNY',
    STATUS              TEXT DEFAULT 'DRAFT' NOT NULL,
    OWNER_USER_ID       TEXT,
    DEPT_ID             TEXT,
    PROCESS_INSTANCE_ID TEXT,
    CREATE_BY           TEXT,
    CREATE_DATE         TEXT DEFAULT (datetime('now', 'localtime')),
    UPDATE_BY           TEXT,
    UPDATE_DATE         TEXT DEFAULT (datetime('now', 'localtime')),
    REMARKS             TEXT,
    DEL_FLAG            TEXT DEFAULT '0',
    VER_NO              INTEGER DEFAULT 0 NOT NULL,
    SYS_CODE            TEXT,
    CONSTRAINT PK_WH_CONTRACT PRIMARY KEY (ID),
    CONSTRAINT CK_WH_CONTRACT_STATUS CHECK (STATUS IN (
        'DRAFT','SUBMITTED','APPROVED','REJECTED','EXECUTING','CLOSED','TERMINATED'
    ))
);

CREATE INDEX IF NOT EXISTS IDX_CONTRACT_CUSTOMER
    ON wh_contract (CUSTOMER_ID, DEL_FLAG);
CREATE INDEX IF NOT EXISTS IDX_CONTRACT_STATUS
    ON wh_contract (STATUS, CREATE_DATE);
CREATE INDEX IF NOT EXISTS IDX_CONTRACT_CODE
    ON wh_contract (CONTRACT_CODE);
CREATE INDEX IF NOT EXISTS IDX_CONTRACT_OWNER
    ON wh_contract (OWNER_USER_ID, DEL_FLAG);
```

**明细表：wh_contract_product（合同产品明细）**

```sql
CREATE TABLE IF NOT EXISTS wh_contract_product (
    ID                  TEXT NOT NULL,
    CONTRACT_ID         TEXT NOT NULL,
    PRODUCT_ID          TEXT,
    PRODUCT_NAME        TEXT NOT NULL,
    SPECIFICATION       TEXT,
    QUANTITY            REAL,
    UNIT_PRICE          TEXT,
    AMOUNT              TEXT,
    TAX_RATE            REAL,
    SORT_ORDER          INTEGER DEFAULT 0,
    CREATE_BY           TEXT,
    CREATE_DATE         TEXT DEFAULT (datetime('now', 'localtime')),
    UPDATE_BY           TEXT,
    UPDATE_DATE         TEXT DEFAULT (datetime('now', 'localtime')),
    REMARKS             TEXT,
    DEL_FLAG            TEXT DEFAULT '0',
    VER_NO              INTEGER DEFAULT 0 NOT NULL,
    SYS_CODE            TEXT,
    CONSTRAINT PK_WH_CONTRACT_PROD PRIMARY KEY (ID)
);

CREATE INDEX IF NOT EXISTS IDX_CONTRACT_PROD_CONTRACT
    ON wh_contract_product (CONTRACT_ID, DEL_FLAG, SORT_ORDER);
```

### 9.2 审批集成表模式

```sql
CREATE TABLE IF NOT EXISTS wh_erp_pm_timesheet (
    ID                  TEXT NOT NULL,
    USER_ID             TEXT NOT NULL,
    DOC_NO              TEXT,
    BELONG_YEAR         INTEGER NOT NULL,
    BELONG_MONTH        INTEGER NOT NULL,
    WEEK_START          TEXT NOT NULL,
    WEEK_END            TEXT NOT NULL,
    STATUS              TEXT DEFAULT 'DRAFT' NOT NULL,
    TOTAL_HOURS         REAL,
    PROCESS_INSTANCE_ID TEXT,        -- Flowable 流程实例 ID
    SUBMIT_DATE         TEXT,
    APPROVE_DATE        TEXT,
    CREATE_BY           TEXT,
    CREATE_DATE         TEXT DEFAULT (datetime('now', 'localtime')),
    UPDATE_BY           TEXT,
    UPDATE_DATE         TEXT DEFAULT (datetime('now', 'localtime')),
    REMARKS             TEXT,
    DEL_FLAG            TEXT DEFAULT '0',
    VER_NO              INTEGER DEFAULT 0 NOT NULL,
    SYS_CODE            TEXT,
    CONSTRAINT PK_WH_ERP_PM_TS PRIMARY KEY (ID),
    CONSTRAINT CK_WH_ERP_PM_TS_STATUS CHECK (STATUS IN (
        'DRAFT','SUBMITTED','APPROVED','REJECTED','WITHDRAWN','ADJUSTING'
    )),
    CONSTRAINT CK_WH_ERP_PM_TS_MONTH CHECK (BELONG_MONTH BETWEEN 1 AND 12)
);

CREATE INDEX IF NOT EXISTS IDX_PM_TS_USER_PERIOD
    ON wh_erp_pm_timesheet (USER_ID, BELONG_YEAR, BELONG_MONTH, DEL_FLAG);
CREATE INDEX IF NOT EXISTS IDX_PM_TS_WEEK
    ON wh_erp_pm_timesheet (USER_ID, WEEK_START, WEEK_END, DEL_FLAG);
CREATE INDEX IF NOT EXISTS IDX_PM_TS_PROC
    ON wh_erp_pm_timesheet (PROCESS_INSTANCE_ID);
```

### 9.3 系统表模式（权限/角色）

```sql
CREATE TABLE IF NOT EXISTS sys_user (
    ID                  TEXT NOT NULL,
    USERNAME            TEXT NOT NULL,
    PASSWORD            TEXT NOT NULL,
    REAL_NAME           TEXT,
    DEPT_ID             TEXT,
    EMPLOYEE_NO         TEXT,
    PHONE               TEXT,
    EMAIL               TEXT,
    EMPLOYMENT_STATUS   TEXT DEFAULT 'ACTIVE' NOT NULL,
    MUST_CHANGE_PWD     TEXT DEFAULT 'N' NOT NULL,
    CREATE_BY           TEXT,
    CREATE_DATE         TEXT DEFAULT (datetime('now', 'localtime')),
    UPDATE_BY           TEXT,
    UPDATE_DATE         TEXT DEFAULT (datetime('now', 'localtime')),
    REMARKS             TEXT,
    DEL_FLAG            TEXT DEFAULT '0',
    VER_NO              INTEGER DEFAULT 0 NOT NULL,
    SYS_CODE            TEXT,
    CONSTRAINT PK_SYS_USER PRIMARY KEY (ID)
);

CREATE TABLE IF NOT EXISTS sys_role (
    ID                  TEXT NOT NULL,
    ROLE_NAME           TEXT NOT NULL,
    ROLE_CODE           TEXT NOT NULL,
    CREATE_BY           TEXT,
    CREATE_DATE         TEXT DEFAULT (datetime('now', 'localtime')),
    UPDATE_BY           TEXT,
    UPDATE_DATE         TEXT DEFAULT (datetime('now', 'localtime')),
    REMARKS             TEXT,
    DEL_FLAG            TEXT DEFAULT '0',
    VER_NO              INTEGER DEFAULT 0 NOT NULL,
    CONSTRAINT PK_SYS_ROLE PRIMARY KEY (ID)
);

CREATE TABLE IF NOT EXISTS sys_permission (
    ID                  TEXT NOT NULL,
    PARENT_ID           TEXT,
    PERM_NAME           TEXT NOT NULL,
    PERM_CODE           TEXT NOT NULL,
    PERM_TYPE           TEXT NOT NULL,  -- 'M'=菜单, 'B'=按钮, 'A'=接口
    PATH                TEXT,
    COMPONENT           TEXT,
    ICON                TEXT,
    SORT_ORDER          INTEGER DEFAULT 0,
    VISIBLE             TEXT DEFAULT 'Y',
    CREATE_BY           TEXT,
    CREATE_DATE         TEXT DEFAULT (datetime('now', 'localtime')),
    DEL_FLAG            TEXT DEFAULT '0',
    VER_NO              INTEGER DEFAULT 0 NOT NULL,
    CONSTRAINT PK_SYS_PERM PRIMARY KEY (ID)
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    ID                  TEXT NOT NULL,
    USER_ID             TEXT NOT NULL,
    ROLE_ID             TEXT NOT NULL,
    CONSTRAINT PK_SYS_UR PRIMARY KEY (ID)
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    ID                  TEXT NOT NULL,
    ROLE_ID             TEXT NOT NULL,
    PERMISSION_ID       TEXT NOT NULL,
    CONSTRAINT PK_SYS_RP PRIMARY KEY (ID)
);

CREATE INDEX IF NOT EXISTS IDX_UR_USER ON sys_user_role (USER_ID);
CREATE INDEX IF NOT EXISTS IDX_RP_ROLE ON sys_role_permission (ROLE_ID);
CREATE INDEX IF NOT EXISTS IDX_PERM_PARENT ON sys_permission (PARENT_ID);
CREATE INDEX IF NOT EXISTS IDX_PERM_CODE ON sys_permission (PERM_CODE);
```

---

## 十、性能优化指南

### 10.1 SQLite 优化 PRAGMA

```sql
-- 开启 WAL 模式（推荐生产使用）
PRAGMA journal_mode=WAL;

-- 同步模式（平衡性能与安全性）
PRAGMA synchronous=NORMAL;

-- 缓存大小（页数，默认 2000 页 ≈ 8MB）
PRAGMA cache_size=-64000;  -- 64MB

-- 临时存储（使用内存而非磁盘）
PRAGMA temp_store=MEMORY;
```

**应用层配置（启动时执行）：**

```java
@Component
public class SqliteInitializer implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
            stmt.execute("PRAGMA cache_size=-64000");
            stmt.execute("PRAGMA temp_store=MEMORY");
        }
    }
}
```

### 10.2 索引优化

- **覆盖索引**：将查询所需列全部放入索引，避免回表
- **复合索引顺序**：等值条件在前，范围条件在后
- **避免过度索引**：每个表索引不超过 5-8 个

### 10.3 查询优化

```sql
-- 推荐：使用索引覆盖查询
SELECT ID, CONTRACT_CODE, CONTRACT_NAME FROM wh_contract
WHERE CUSTOMER_ID = ? AND DEL_FLAG = '0';

-- 推荐：LIMIT 限制返回
SELECT * FROM wh_contract WHERE DEL_FLAG = '0'
ORDER BY CREATE_DATE DESC LIMIT 20;

-- 避免：全表扫描
SELECT COUNT(*) FROM wh_contract WHERE REMARKS LIKE '%搜索词%';
```

---

## 十一、数据库备份与恢复

### 11.1 备份

```bash
# 简单文件拷贝（需停止应用或使用 WAL 模式）
cp data/wh.sqlite data/wh-backup-$(date +%Y%m%d).sqlite

# SQLite 在线备份
sqlite3 data/wh.sqlite ".backup 'data/wh-backup-$(date +%Y%m%d).sqlite'"
```

### 11.2 恢复

```bash
# 恢复备份
cp data/wh-backup-20260502.sqlite data/wh.sqlite
```

### 11.3 导出/导入

```bash
# 导出 SQL
sqlite3 data/wh.sqlite ".dump" > backup.sql

# 导入 SQL
sqlite3 data/wh.sqlite < backup.sql
```

---

## 十二、数据库迁移检查清单（Oracle → SQLite）

### 12.1 DDL 迁移

| 检查项 | Oracle | SQLite |
|--------|--------|--------|
| 表创建 | `CREATE TABLE` | `CREATE TABLE IF NOT EXISTS` |
| 主键 | `VARCHAR2(32)` | `TEXT` |
| 数值 | `NUMBER(p,s)` | `INTEGER` / `REAL` / `TEXT` |
| 日期 | `TIMESTAMP DEFAULT SYSTIMESTAMP` | `TEXT DEFAULT (datetime('now', 'localtime'))` |
| 索引 | `CREATE INDEX` | `CREATE INDEX IF NOT EXISTS` |
| 约束 | `CONSTRAINT PK_... PRIMARY KEY` | 同左（SQLite 支持） |
| CHECK | `CONSTRAINT CK_... CHECK` | 同左（SQLite 支持） |

### 12.2 DML 迁移

| 检查项 | Oracle | SQLite |
|--------|--------|--------|
| 插入种子 | `INSERT ... WHERE NOT EXISTS ... FROM dual` | `INSERT ... WHERE NOT EXISTS`（无 FROM dual） |
| UPSERT | `MERGE INTO ... USING ... ON ...` | `INSERT OR REPLACE` / `INSERT OR IGNORE` |
| 幂等建表 | `BEGIN EXECUTE IMMEDIATE ... EXCEPTION ... END;` | `CREATE TABLE IF NOT EXISTS` |
| 查询空行 | `SELECT ... FROM dual` | `SELECT ...`（无 FROM） |
| NVL | `NVL(a, b)` | `COALESCE(a, b)` |
| 字符串连接 | `a \|\| b` | `a \|\| b`（相同） |
| 日期格式化 | `TO_CHAR(date, 'YYYY-MM-DD')` | `strftime('%Y-%m-%d', date)` |

### 12.3 应用层迁移

| 检查项 | Oracle | SQLite |
|--------|--------|--------|
| JDBC 驱动 | `oracle.jdbc.OracleDriver` | `org.sqlite.JDBC` |
| JDBC URL | `jdbc:oracle:thin:@//host:port/service` | `jdbc:sqlite:/path/to/db.sqlite` |
| 连接池校验 | `SELECT 1 FROM DUAL` | `SELECT 1` |
| MyBatis 方言 | `DbType.ORACLE` | `DbType.SQLITE` |
| NULL 绑定 | `jdbc-type-for-null: VARCHAR` | 保留（SQLite 也受益） |
| Profile | `oracle,dev` | `sqlite,dev` |
| 数据库 ID | `oracle` | `sqlite` |

---

## 十三、核心表清单

### 13.1 系统管理

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `sys_user` | 用户表 | USERNAME, REAL_NAME, DEPT_ID |
| `sys_role` | 角色表 | ROLE_NAME, ROLE_CODE |
| `sys_permission` | 权限表 | PERM_NAME, PERM_CODE, PERM_TYPE |
| `sys_user_role` | 用户-角色关联 | USER_ID, ROLE_ID |
| `sys_role_permission` | 角色-权限关联 | ROLE_ID, PERMISSION_ID |
| `sys_dept` | 部门表 | DEPT_NAME, PARENT_ID |
| `sys_dict` | 字典表 | DICT_CODE, DICT_NAME |

### 13.2 CRM（客户管理）

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `wh_customer` | 客户表 | CUSTOMER_NAME, INDUSTRY, STATUS |
| `wh_customer_contact` | 联系人表 | CUSTOMER_ID, CONTACT_NAME, PHONE |
| `wh_opportunity` | 商机表 | OPPORTUNITY_NAME, CUSTOMER_ID, EXPECTED_AMOUNT |
| `wh_contract` | 合同表 | CONTRACT_CODE, CUSTOMER_ID, CONTRACT_AMOUNT |
| `wh_contract_product` | 合同产品明细 | CONTRACT_ID, PRODUCT_NAME, QUANTITY |
| `wh_contract_revenue_item` | 合同收入行 | CONTRACT_ID, REVENUE_AMOUNT, TAX_RATE |
| `wh_payment_plan` | 回款计划 | CONTRACT_ID, PLAN_AMOUNT, PLAN_DATE |
| `wh_payment_application` | 付款申请 | APPLY_NO, PAY_AMOUNT, PAYEE_NAME |

### 13.3 PM（项目管理）

| 表名                      | 说明   | 关键字段                                            |
| ----------------------- | ---- | ----------------------------------------------- |
| `wh_pm_project`        | 项目基础 | PROJECT_CODE, PROJECT_NAME, MANAGER_ID          |
| `wh_pm_timesheet`      | 工时表  | USER_ID, BELONG_YEAR, BELONG_MONTH, TOTAL_HOURS |
| `wh_pm_timesheet_line` | 工时明细 | TIMESHEET_ID, PROJECT_ID, WORK_DATE, HOURS      |
| `wh_pm_milestone`      | 里程碑  | PROJECT_ID, MILESTONE_NAME, PLAN_DATE           |
| `wh_pm_stakeholder`    | 干系人  | PROJECT_ID, USER_ID, ROLE                       |
| `wh_pm_task`           | 任务   | PROJECT_ID, TASK_NAME, START_DATE, END_DATE     |

### 13.4 财务

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `wh_expense_purpose` | 费用用途 | PURPOSE_NAME, WORKFLOW_MODE |
| `wh_expense_item` | 费用项目 | LINE_TYPE, PURPOSE_ID, ITEM_NAME |
| `wh_expense_report` | 费用报销单 | REPORT_NO, TOTAL_AMOUNT, STATUS |
| `wh_finance_invoice` | 发票登记 | INVOICE_NO, AMOUNT, TAX_AMOUNT |
| `wh_fund_weekly` | 资金周报 | REPORT_DATE, TOTAL_CASH, TOTAL_RECEIVABLE |

### 13.5 HR（人力资源）

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `wh_hr_employee` | 员工档案 | EMPLOYEE_NO, REAL_NAME, DEPT_ID |
| `wh_hr_offer` | 录用通知 | CANDIDATE_NAME, POSITION, SALARY |
| `wh_hr_probation` | 试用期管理 | EMPLOYEE_ID, START_DATE, END_DATE |
| `wh_hr_social` | 社保公积金 | EMPLOYEE_ID, SOCIAL_BASE, FUND_BASE |
| `wh_hr_resignation` | 离职管理 | EMPLOYEE_ID, RESIGN_DATE, REASON |

### 13.6 行政

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `wh_admin_office_card_apply` | 名片申请 | APPLICANT_ID, CONTENT |
| `wh_admin_office_meal_apply` | 用餐申请 | APPLICANT_ID, DATE, COUNT |
| `wh_admin_office_flight_apply` | 机票申请 | APPLICANT_ID, DEPARTURE, DESTINATION |
| `wh_admin_office_equipment_apply` | 设备申请 | APPLICANT_ID, EQUIPMENT_TYPE |
| `wh_erp_seal_application` | 用印申请 | SEAL_TYPE, LEGAL_ENTITY, FILE_PATH |
| `wh_vehicle` | 车辆管理 | PLATE_NO, BRAND, STATUS |

### 13.7 审批与治理

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `wh_approval_instance` | 审批实例 | BIZ_ID, FLOW_TYPE, STATUS |
| `wh_approval_record` | 审批记录 | INSTANCE_ID, APPROVER, ACTION |
| `wh_approval_flow` | 流程定义 | FLOW_NAME, FLOW_TYPE, BPMN_XML |
| `governance_alert_policy` | 告警策略 | POLICY_NAME, TRIGGER_CONDITION |
| `governance_workflow` | 工作流定义 | WORKFLOW_NAME, BPMN_XML |
