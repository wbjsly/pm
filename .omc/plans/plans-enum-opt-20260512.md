# 枚举字典化改造 - 实施计划

## RALPLAN-DR Summary

### Principles (5)
1. **历史数据零破坏** -- 字典 item_code 必须与现有数据库存储值完全一致（如 `DRAFT`, `APPROVED`），不修改任何表结构和已有数据
2. **类型常量集中，条目自由扩展** -- 后端只定义 `type_code` 常量（`DictTypes.java`），`item_code` 不在代码中定义，可在管理界面自由增删
3. **状态流转逻辑保留在代码中** -- `if ("DRAFT".equals(status))` 等流转判断不改为查字典，保持 BO 层业务逻辑可读性
4. **前端一次性加载，按 type_code 分组** -- Pinia store 在 `router.beforeEach` 中全量加载，各组件通过 getter 按 `type_code` 获取映射
5. **遵循现有代码模式** -- 新 Entity/DAO/BO/Controller 沿用 BaseEntity + MyBatis-Plus + constructor injection + `R<T>` 包装

### Decision Drivers (Top 3)
1. **迁移风险最小化** -- 9 个模块、20+ 个组件同时改造，必须保证每一步可独立验证，不出现运行时字典缺失导致的 NPE
2. **免发布新增枚举** -- 管理界面操作即可新增枚举项，无需改代码、打包、部署
3. **前后端映射一致性** -- 消除每个 Vue 组件各自定义 `statusLabel`/`statusTagType` 的重复代码

### Viable Options

| Option | Description | Pros | Cons |
|--------|------------|------|------|
| **A: 常量引用 + 字典查询混合** | 后端用 `DictTypes` 常量引用 `type_code`，`item_code` 字符串直接写在代码中（不查表），前端统一从 Pinia store 取值 | 改动最小，Spring 不启动也可编译通过，流转逻辑不受字典数据影响 | `item_code` 修改后代码中的判断会失效；管理界面新增的 item_code 代码中不知道 |
| **B: 全量字典驱动** | 后端所有状态判断改为查字典表（如 `dictBo.getItems("CHARTER_STATUS").contains(status)`），前端统一从 store 取值 | 真正实现管理界面新增即可生效，代码完全解耦 | 改动量巨大，每次状态判断都需查表/缓存，流转逻辑可读性下降，`GROUP BY status` 统计查询无法直接走字典 |

**Decision: Option A** -- 后端定义 `type_code` 常量引用，`item_code` 字符串字面量保留在代码中（状态流转判断保持 `"DRAFT".equals(status)` 风格），前端共享 Pinia store 统一管理 tag 类型和 label 映射。这是迁就"状态流转规则保留在代码中"这一约束的唯一务实方案。

**Option B 被否决的详细理由**: (a) 状态流转判断（如 `if (!"DRAFT".equals(status))`）有 60+ 处，全量改为查表会引入缓存一致性问题；(b) `GROUP BY status` 的 SQL 统计查询无法直接受益于字典表；(c) 新增一个枚举值需要改流转逻辑是合理的业务约束，不是技术债务。

---

## ADR (Architecture Decision Record)

### Decision
采用 **Option A（常量引用 + 字典查询混合模式）**：后端用 `DictTypes.java` 类集中定义所有 `type_code` 字符串常量，`item_code` 保持硬编码在 BO 流转逻辑中；前端创建 `useDictStore`（Pinia），在 `router.beforeEach` 中全量加载字典数据，各组件通过 `dictStore.statusTagType('CHARTER_STATUS', status)` 和 `dictStore.statusLabel('CHARTER_STATUS', status)` 统一获取映射。

### Drivers
- 现有 60+ 处状态流转判断（如 `if ("DRAFT".equals(status))`）是核心业务逻辑，不可为了"纯字典化"而牺牲可读性
- 1 处 `GROUP BY status` 的 MyBatis SQL（`WhPmCharterDao.java:19`）不能简单替换为字典查询
- `item_code` 的"新增免发布"需求本质上是管理界面能新增枚举项，代码中的流转判断只需覆盖核心状态。额外新增的状态（如"归档"）可在不修改流转逻辑的情况下用于显示
- 前端 20+ 个组件中重复定义映射是明确的技术债务，Pinia store 是最直接的解决方案

### Alternatives Considered
1. **全量字典驱动**（Option B）-- 否决理由见上
2. **Java Enum 类** -- 否决，因为需求明确要求管理界面可增删枚举值，Java Enum 无法运行时扩展
3. **只改前端不改后端** -- 否决，后端 Bo 中的 `type_code` 不一致（Charter 用 `PENDING_APPROVAL`，Budget 用 `PENDING`），需要 `DictTypes` 常量来统一约束

### Consequences
- **Positive**: 后端改动最小（只加常量类和字典管理 CRUD），前端去重效果显著（~20 个组件共享一个 store），历史数据完全兼容
- **Negative**: `item_code` 修改仍需代码配合（但 spec 明确接受此约束）；`type_code` 常量类需与数据库字典数据同步维护
- **Follow-ups**: 如后续有"免代码新增需要参与流转判断的状态"需求，再引入状态机配置表

---

## 枚举清单（代码库实测结果）

基于代码库全文扫描，需要迁移的枚举共计 **11 个 type_code，70+ 个 item_code**：

| # | type_code | 实体字段 | 当前 item_code 值 | 主要引用文件 |
|---|-----------|---------|-------------------|-------------|
| 1 | CHARTER_STATUS | WhPmCharter.status | DRAFT, PENDING_APPROVAL, APPROVED, REJECTED | WhPmCharterBo.java, CharterApprovalCallback.java |
| 2 | BUDGET_STATUS | WhPmBudget.status | DRAFT, PENDING, APPROVED, REJECTED | WhPmBudgetBo.java, BudgetApprovalCallback.java |
| 3 | DELIVERABLE_STATUS | WhPmDeliverable.status | DRAFT, PENDING_APPROVAL, APPROVED, REJECTED, DELIVERED | WhPmDeliverableBo.java, DeliverableApprovalCallback.java |
| 4 | WBS_ELEMENT_STATUS | WhPmWbsElement.status | NOT_STARTED, IN_DEVELOPMENT, TESTING, COMPLETED, SUSPENDED, CANCELLED | WhPmWbsElementBo.java |
| 5 | WBS_ELEMENT_TYPE | WhPmWbsElement.elementType | TASK | WhPmWbsElementBo.java |
| 6 | WORKLOG_STATUS | WhPmWorkLog.status | DRAFT, APPROVED, REJECTED | WhPmWorkLogBo.java |
| 7 | COST_WARNING_STATUS | WhPmCostWarning.status | ACTIVE, CLOSED | WhPmCostWarningBo.java |
| 8 | CALENDAR_DAY_TYPE | WhSysWorkCalendar.dayType | WORKDAY, WEEKEND, HOLIDAY | WhSysWorkCalendarBo.java |
| 9 | PRODUCT_STATUS | ErpProduct.status | ACTIVE, INACTIVE, PLACEHOLDER | ErpProductBo.java |
| 10 | MODULE_STATUS | ErpModule.status | ACTIVE, INACTIVE | ErpModuleBo.java |
| 11 | CHARTER_PROGRESS | WhPmCharter.progress | IN_PROGRESS, ACCEPTED, COMPLETED, SUSPENDED, CANCELLED | WhPmCharterBo.java |

**额外需要统一的前端 tag 类型映射**（每个 item 需要: `label` 中文名, `tagType`: primary/success/warning/danger/info/空字符串）：

---

## Phase 1: 数据库字典表 + 种子数据

**目标**: 创建 `wh_dict_type` 和 `wh_dict_item` 表，插入所有现有枚举值的种子数据

### 文件清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `wh-backend/src/main/java/com/wh/entity/system/WhDictType.java` | 字典类型实体 |
| 新建 | `wh-backend/src/main/java/com/wh/entity/system/WhDictItem.java` | 字典条目实体 |
| 新建 | `wh-backend/src/main/resources/db/sqlite/047-dict-table.sql` | 建表 + 种子数据 SQL |

### 详细 TODOs

#### Step 1.1: 创建字典类型实体
**文件**: `wh-backend/src/main/java/com/wh/entity/system/WhDictType.java`
- 继承 `BaseEntity`，使用 `@TableName("wh_dict_type")`
- 字段: `typeCode`（唯一）, `typeName`, `description`, `sortOrder`
- 遵循现有实体命名惯例（`@TableField("TYPE_CODE")` 大写列名）

#### Step 1.2: 创建字典条目实体
**文件**: `wh-backend/src/main/java/com/wh/entity/system/WhDictItem.java`
- 继承 `BaseEntity`，使用 `@TableName("wh_dict_item")`
- 字段: `typeCode`（关联 wh_dict_type）, `itemCode`, `itemLabel`, `itemValue`（扩展 JSON）, `tagType`（Element Plus tag 类型）, `sortOrder`, `status`（启用/停用）

#### Step 1.3: 创建数据库迁移 SQL
**文件**: `wh-backend/src/main/resources/db/sqlite/047-dict-table.sql`
- 创建 `wh_dict_type` 表（含唯一索引 `IDX_DICT_TYPE_CODE`）
- 创建 `wh_dict_item` 表（含联合索引 `IDX_DICT_ITEM_TYPE`）
- 插入 11 条 `wh_dict_type` 种子数据
- 插入全部 70+ 条 `wh_dict_item` 种子数据（item_code 必须与现有数据库存储值完全一致）
- 每个 `dict_item` 包含 `tag_type` 字段（如 `primary`, `success`, `warning`, `danger`, `info`）

### 验收检查点
- [ ] `SqliteBootstrap` 启动时能正确执行 `047-dict-table.sql`
- [ ] 表中有 11 条 `wh_dict_type` 记录
- [ ] 表中所有 `dict_item.item_code` 与现有业务数据中的实际值完全一致（如 `PENDING_APPROVAL` 而非 `PENDING`）

---

## Phase 2: 后端基础设施（DAO + BO + Controller + DictTypes 常量）

**目标**: 建立字典查询的完整后端链路，创建 `DictTypes.java` 常量类

### 文件清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `wh-backend/src/main/java/com/wh/constant/DictTypes.java` | 集中定义所有 type_code 常量 |
| 新建 | `wh-backend/src/main/java/com/wh/dao/system/WhDictTypeDao.java` | 字典类型 DAO |
| 新建 | `wh-backend/src/main/java/com/wh/dao/system/WhDictItemDao.java` | 字典条目 DAO |
| 新建 | `wh-backend/src/main/java/com/wh/bo/system/WhPmDictBo.java` | 字典 BO（查询逻辑 + 缓存） |
| 新建 | `wh-backend/src/main/java/com/wh/controller/system/SysDictController.java` | 字典管理 API |
| 新建 | `wh-frontend/src/api/system/dict.js` | 前端字典 API 封装 |

### 详细 TODOs

#### Step 2.1: 创建 DictTypes 常量类
**文件**: `wh-backend/src/main/java/com/wh/constant/DictTypes.java`
- 每个 type_code 定义一个 `public static final String` 常量
- 命名规则: `CHARTER_STATUS`, `BUDGET_STATUS`, `DELIVERABLE_STATUS`, `WBS_ELEMENT_STATUS`, `WBS_ELEMENT_TYPE`, `WORKLOG_STATUS`, `COST_WARNING_STATUS`, `CALENDAR_DAY_TYPE`, `PRODUCT_STATUS`, `MODULE_STATUS`, `CHARTER_PROGRESS`

#### Step 2.2: 创建 DAO 层
**文件**: `wh-backend/src/main/java/com/wh/dao/system/WhDictTypeDao.java`
- 继承 `BaseMapper<WhDictType>`
- 添加 `@Mapper` 注解

**文件**: `wh-backend/src/main/java/com/wh/dao/system/WhDictItemDao.java`
- 继承 `BaseMapper<WhDictItem>`
- 添加自定义查询: `selectByTypeCode(String typeCode)` （按 sort_order 排序，只查 status='1' 的启用项）

#### Step 2.3: 创建 BO 层
**文件**: `wh-backend/src/main/java/com/wh/bo/system/WhPmDictBo.java`
- 注入 `WhDictTypeDao` 和 `WhDictItemDao`
- 提供方法:
  - `getAllDicts()` -- 返回 `Map<String, List<WhDictItem>>`（按 type_code 分组），用于前端全量加载
  - `getItemsByType(String typeCode)` -- 按类型获取启用的条目列表
  - `saveType(WhDictType)` / `updateType(WhDictType)` / `deleteType(String id)` -- CRUD
  - `saveItem(WhDictItem)` / `updateItem(WhDictItem)` / `deleteItem(String id)` -- CRUD
- 使用 `@Service` 注解（遵循 BO 模式）

#### Step 2.4: 创建 Controller
**文件**: `wh-backend/src/main/java/com/wh/controller/system/SysDictController.java`
- 端点:
  - `GET /api/system/dict/all` -- 返回全部字典数据（供前端初始化加载）
  - `GET /api/system/dict/types` -- 字典类型列表
  - `POST /api/system/dict/types` -- 新增类型
  - `PUT /api/system/dict/types/{id}` -- 更新类型
  - `DELETE /api/system/dict/types/{id}` -- 删除类型
  - `GET /api/system/dict/items?typeCode=xxx` -- 按类型查条目
  - `POST /api/system/dict/items` -- 新增条目
  - `PUT /api/system/dict/items/{id}` -- 更新条目
  - `DELETE /api/system/dict/items/{id}` -- 删除条目
- 所有接口返回 `R<T>` 包装
- 权限实现机制（通过 `SecurityConfig.java` 的 `.antMatchers` 声明实现，**不引入** `@PreAuthorize` 或方法级注解）：
  - **重要**: 这是代码库中**首次引入**角色级访问控制（`.hasRole("ADMIN")`）。当前 `SecurityConfig.java`（第 34-38 行）仅使用 `.permitAll()` 和 `.authenticated()`，从未使用 `.hasRole()` 或 `.hasAuthority()`。因此这是**新增**的安全约束，不是"沿用已有模式"
  - **插入位置规则**: 新增的字典权限规则**必须放在**现有 `.antMatchers("/api/**").authenticated()` 这一行**之前**。Spring Security 按声明顺序匹配规则，若字典的 `.antMatchers` 声明在 `/api/**` 之后则永远不会生效（`/api/**` 已捕获所有 API 路径）
  - 具体权限规则：
    - `GET /api/system/dict/all` → `.antMatchers(HttpMethod.GET, "/api/system/dict/all").authenticated()`（所有登录用户均需加载字典数据用于 UI 渲染，非 ADMIN 用户也能看到中文标签）
    - 其余写操作 → `.antMatchers("/api/system/dict/**").hasRole("ADMIN")`（统一用 `/api/system/dict/**` 捕获所有 POST/PUT/DELETE，避免多行重复）
  - `SecurityConfig.java` 修改示例（标注插入位置）：
    ```java
    // 在 filterChain 方法的 authorizeRequests() 块中：
    .authorizeRequests()
        .antMatchers("/api/auth/**").permitAll()
        .antMatchers("/doc.html", "/webjars/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
        // ======== 字典管理权限（新增，插入此处）========
        .antMatchers(HttpMethod.GET, "/api/system/dict/all").authenticated()
        .antMatchers("/api/system/dict/**").hasRole("ADMIN")
        // ======== 以上为新增内容 ========
        .antMatchers("/api/**").authenticated()  // ← 必须在新规则之后，否则新规则永不生效
        .anyRequest().permitAll()
    ```

#### Step 2.5: 创建前端 API 封装
**文件**: `wh-frontend/src/api/system/dict.js`
- `getAllDictsApi()` -- 调用 `GET /api/system/dict/all`
- `getDictTypesApi()`, `saveDictTypeApi()`, `updateDictTypeApi()`, `deleteDictTypeApi()`
- `getDictItemsApi()`, `saveDictItemApi()`, `updateDictItemApi()`, `deleteDictItemApi()`

#### Step 2.6: 启动时 DictTypes 与字典表一致性校验
**文件**: `wh-backend/src/main/java/com/wh/bo/system/WhPmDictBo.java`
- 添加 `@EventListener(ApplicationReadyEvent.class)` 方法 `validateDictConsistency()`
- **为何不用 `@PostConstruct`**: `SqliteBootstrap` 在 `InitializingBean.afterPropertiesSet()` 中执行建表（`db/sqlite/*.sql`），而 `@PostConstruct` 可能先于 `afterPropertiesSet()` 执行，此时 `wh_dict_type`/`wh_dict_item` 表尚未创建，导致 `SQLException` 而非预期的 WARN 日志。`ApplicationReadyEvent` 确保在 Context 完全刷新（包括所有 `InitializingBean`）之后才执行校验
- 遍历 `DictTypes.java` 中定义的所有 type_code 常量（通过反射或手动列出），检查 `wh_dict_type` 表中是否有对应记录
- 不一致时打印 WARN 日志（不阻塞启动）：
  - `DictTypes 中定义的 type_code [xxx] 在 wh_dict_type 表中无对应记录，请检查种子数据`
- 反向校验：遍历 `wh_dict_type` 表中的所有 type_code，检查 `DictTypes` 中是否有对应常量
- 不一致时打印 WARN 日志：
  - `wh_dict_type 表中的 type_code [xxx] 在 DictTypes.java 中无对应常量，前端可能无法正确显示`
- **容错策略**: 校验失败不阻塞启动，仅记录日志。运维人员可通过日志发现并修复不一致

### 验收检查点
- [ ] `/api/system/dict/all` 对 `ROLE_AUTHENTICATED` 用户可访问，返回完整的 `Map<String, List<{itemCode, itemLabel, tagType}>>`
- [ ] DictTypes 常量类包含全部 11 个 type_code
- [ ] 通过 Knife4j `/doc.html` 可测试所有 CRUD 接口（写操作需 ROLE_ADMIN）
- [ ] 启动时 `validateDictConsistency()` 执行，日志输出校验结果（无 WARN 表示 DictTypes 与数据库一致）

---

## Phase 3: 后端 Bo 类引入 DictTypes 常量

**目标**: 在所有 Bo 类和 ApprovalCallback 类中通过 `import static` 引入 `DictTypes` 常量，仅替换 type_code 字符串字面量；item_code 比较（如 `"DRAFT".equals(status)`）保持不变

### 改动范围说明

Phase 3 的核心目的是确保所有 type_code 字符串字面量集中定义在 `DictTypes.java` 中，消除代码库中的硬编码 type_code 字符串。

**`DictTypes.java` 常量类**（已在 Phase 2 Step 2.1 完成）作为 type_code 的唯一集中定义点。代码库中其他位置如引用 type_code，应使用 `DictTypes.CHARTER_STATUS` 而非 `"CHARTER_STATUS"` 字符串字面量。

> **当前代码库扫描结论**: 对 12 个 Bo/ApprovalCallback 文件进行全文搜索，**未发现** type_code 作为独立字符串字面量使用的场景（如 `map.put("CHARTER_STATUS", ...)` 或 `"CHARTER_STATUS".equals(...)`）。type_code 仅在赋值给实体字段（如 `setStatus("DRAFT")`）时作为 item_code 出现——这些 item_code 字符串比较和赋值是业务逻辑的一部分，不在 Phase 3 替换范围内。
>
> **因此**: 对于没有 type_code 字符串字面量需要替换的 Bo 文件，**不添加** `import static com.wh.constant.DictTypes.*`——否则会产生未使用的 import 警告，与验收标准"无编译警告"直接矛盾。`DictTypes.java` 自身已是权威定义点，前端通过 `/api/system/dict/all` 获取 type_code。Phase 2 完成后可通过 `grep -rn '"CHARTER_STATUS"\|"BUDGET_STATUS"\|...' wh-backend/src/main/java` 做最终确认。

### 详细 TODOs

#### Step 3.1: 逐文件检查并替换 type_code 字面量（如有）
- 对每个 Bo/ApprovalCallback 文件，检查是否存在 type_code 字符串字面量（如 `"CHARTER_STATUS"` 作为独立字符串使用）
- **仅在**某文件存在 type_code 字符串字面量需要替换时，才添加 `import static com.wh.constant.DictTypes.*`（避免未使用的 import 警告）
- **不替换** 状态流转判断中的 item_code 字符串（如 `if ("DRAFT".equals(charter.getStatus()))` 保持原样）—— 这是业务逻辑
- **不替换** 实体字段赋值中的 item_code 字符串（如 `charter.setStatus("APPROVED")` 保持原样）—— 这是业务数据值
- **如有** type_code 字符串字面量（如构建 Map 的 key 为 `"CHARTER_STATUS"`），替换为 `DictTypes.CHARTER_STATUS`
- **当前扫描结论**: 12 个 Bo/ApprovalCallback 文件中未发现 type_code 字符串字面量，因此本步骤预计**零文件修改**。DictTypes.java 自身已是 type_code 的集中定义点

#### Step 3.2: 验证无遗漏 type_code 字面量
- 使用 grep 搜索代码库中所有 type_code 字符串字面量：
  ```bash
  grep -rn '"CHARTER_STATUS"\|"BUDGET_STATUS"\|"DELIVERABLE_STATUS"\|"WBS_ELEMENT_STATUS"\|"WBS_ELEMENT_TYPE"\|"WORKLOG_STATUS"\|"COST_WARNING_STATUS"\|"CALENDAR_DAY_TYPE"\|"PRODUCT_STATUS"\|"MODULE_STATUS"\|"CHARTER_PROGRESS"' wh-backend/src/main/java wh-backend/src/main/resources
  ```
  注意：`wh-backend/src/main/resources` 会覆盖 MyBatis XML 映射文件（`mapper/**/*.xml`）中可能存在的 type_code 字面量
- 每个匹配行必须满足以下之一：
  - 在 `DictTypes.java` 中的常量定义行
  - 已被 `import static` 替换为 `DictTypes.XXX` 引用
  - 是 SQL 文件中的建表/种子数据
  - 是 MyBatis XML 映射文件中的 SQL 语句（如 `WHERE TYPE_CODE = 'CHARTER_STATUS'`）

#### Step 3.3: CharterDao 统计查询（无需修改）
**文件**: `wh-backend/src/main/java/com/wh/dao/pm/WhPmCharterDao.java`
- `selectStatsByPmId` 中的 `GROUP BY STATUS` 查询不修改（SQL 层与字典表解耦）

### 验收检查点
- [ ] `DictTypes.java` 中集中定义了全部 11 个 type_code 常量（已在 Phase 2 完成）
- [ ] 代码库中不存在 type_code 字符串字面量（`grep -rn '"CHARTER_STATUS"\|"BUDGET_STATUS"\|...' wh-backend/src/main/java` 仅在 `DictTypes.java`、SQL 迁移文件中出现）
- [ ] 所有 Bo/ApprovalCallback 文件**无未使用的 import**（不强制添加 `import static` 到无 type_code 字面量的文件中）
- [ ] 现有单元测试（`WhPmWorkLogBoTest`, `WhSysWorkCalendarBoTest`）全部通过
- [ ] 所有审批流程的状态流转功能正常（Charter/Budget/Deliverable 提交-审批-驳回链路）

---

## Phase 4: 前端基础设施（Pinia Store + Router 加载）

**目标**: 创建全局字典 store，在路由守卫中全量加载，提供统一的 `statusLabel` 和 `statusTagType` 方法

### 文件清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `wh-frontend/src/store/dict.js` | 字典 Pinia store |
| 修改 | `wh-frontend/src/router/index.js` | 在 `router.beforeEach` 中加载字典 |

### 详细 TODOs

#### Step 4.1: 创建 useDictStore
**文件**: `wh-frontend/src/store/dict.js`
```javascript
// 伪代码结构（不写入实际代码，仅表达意图）
export const useDictStore = defineStore('dict', {
  state: () => ({
    dictMap: {},    // { CHARTER_STATUS: [{itemCode, itemLabel, tagType}, ...], ... }
    loaded: false
  }),
  getters: {
    // 核心方法：根据 typeCode + itemCode 获取 label
    label: (state) => (typeCode, itemCode) => { ... },
    // 核心方法：根据 typeCode + itemCode 获取 Element Plus tag type
    tagType: (state) => (typeCode, itemCode) => { ... },
    // 按 typeCode 获取选项列表（用于 el-option 遍历）
    options: (state) => (typeCode) => { ... }
  },
  actions: {
    async loadAll() { /* 调用 getAllDictsApi */ }
  }
})
```

#### Step 4.2: 修改路由守卫
**文件**: `wh-frontend/src/router/index.js`
- 在 `router.beforeEach` 中，`getUserInfo()` 之后添加字典加载:
  ```javascript
  if (!dictStore.loaded) {
    await dictStore.loadAll()
  }
  ```
- 加载失败不阻塞路由进入（字典缺失时用 itemCode 作为 fallback 显示）

### 验收检查点
- [ ] 登录后打开浏览器 DevTools Vue 面板，`dict` store 中有 11 个 key
- [ ] 刷新页面后 `loaded` 为 true 时不重复请求
- [ ] 字典接口失败时页面不白屏（fallback 显示原始 itemCode）

---

## Phase 5: 前端组件迁移

**目标**: 将 20+ 个 Vue 组件中的内联 `statusLabel`/`statusTagType` 映射替换为 `useDictStore` 调用

### 文件清单（按模块分组）

| 操作 | 文件路径 | 需替换的内联映射 |
|------|---------|-----------------|
| 修改 | `wh-frontend/src/views/dashboard/index.vue` | CHARTER_STATUS 映射 |
| 修改 | `wh-frontend/src/views/pm/charter/index.vue` | CHARTER_STATUS + CHARTER_PROGRESS 映射 |
| 修改 | `wh-frontend/src/views/pm/charter/detail.vue` | CHARTER_STATUS + WBS_ELEMENT_STATUS + CHARTER_PROGRESS + DELIVERABLE_STATUS 映射 |
| 修改 | `wh-frontend/src/views/pm/charter/form.vue` | CHARTER_STATUS + CHARTER_PROGRESS 映射，el-option 列表 |
| 修改 | `wh-frontend/src/views/pm/budget/index.vue` | BUDGET_STATUS 映射，el-option 列表 |
| 修改 | `wh-frontend/src/views/pm/budget/detail.vue` | BUDGET_STATUS 映射 |
| 修改 | `wh-frontend/src/views/pm/deliverable/index.vue` | DELIVERABLE_STATUS + CHARTER_PROGRESS 映射，el-option 列表 |
| 修改 | `wh-frontend/src/views/pm/deliverable/detail.vue` | DELIVERABLE_STATUS 映射 |
| 修改 | `wh-frontend/src/views/pm/deliverable/form.vue` | （移除硬编码 `'APPROVED'` 查询参数，改用常量后续优化） |
| 修改 | `wh-frontend/src/views/pm/wbs/index.vue` | WBS_ELEMENT_STATUS + CHARTER_STATUS + CHARTER_PROGRESS 映射，el-option 列表 |
| 修改 | `wh-frontend/src/views/pm/wbs/detail.vue` | WBS_ELEMENT_STATUS 映射 |
| 修改 | `wh-frontend/src/views/pm/product/index.vue` | PRODUCT_STATUS + MODULE_STATUS 映射，el-option 列表 |
| 修改 | `wh-frontend/src/views/pm/work-hours/index.vue` | CALENDAR_DAY_TYPE 判断 |
| 修改 | `wh-frontend/src/components/WorkHourDialog.vue` | WORKLOG_STATUS 映射（statusLabel/statusTagType 函数，第 126-133 行） |
| 修改 | `wh-frontend/src/views/system/calendar/index.vue` | CALENDAR_DAY_TYPE 映射，el-radio 列表 |

### 详细 TODOs

#### Step 5.1: 逐个组件迁移（按模块分组，每模块一个 commit）

**迁移模式**（以 charter/index.vue 为例）:

替换前:
```javascript
const statusTagType = (status) => {
  const map = { DRAFT: 'info', PENDING_APPROVAL: 'warning', APPROVED: 'success', REJECTED: 'danger', CLOSED: '' }
  return map[status] || 'info'
}
const statusLabel = (status) => {
  const map = { DRAFT: '草稿', PENDING_APPROVAL: '审批中', APPROVED: '已通过', REJECTED: '已驳回', CLOSED: '已关闭' }
  return map[status] || status
}
```

替换后:
```javascript
import { useDictStore } from '@/store/dict'
const dictStore = useDictStore()

// 模板中直接使用:
// {{ dictStore.label('CHARTER_STATUS', row.status) }}
// <el-tag :type="dictStore.tagType('CHARTER_STATUS', row.status)">

// el-option 列表:
// <el-option v-for="item in dictStore.options('CHARTER_STATUS')" :key="item.itemCode" :label="item.itemLabel" :value="item.itemCode" />
```

#### Step 5.2: el-option 动态列表替换
将所有硬编码的 `<el-option label="草稿" value="DRAFT" />` 替换为 `v-for` 遍历 `dictStore.options('CHARTER_STATUS')`

#### Step 5.3: 状态条件判断保留
模板中的 `v-if="row.status === 'DRAFT'"` 保持不变（这是业务逻辑，不是 UI 映射）

### 验收检查点
- [ ] 每个页面加载后状态标签正确显示中文（与改造前一致）
- [ ] 每个页面的 el-tag 颜色与改造前一致
- [ ] el-select 下拉选项从字典加载，选项完整
- [ ] 在管理界面新增一个状态条目后，刷新页面即可在下拉选项中看到新条目
- [ ] 在管理界面修改已有条目的 `tagType` 或 `itemLabel` 后，页面立即反映变更

---

## Phase 6: 字典管理页面

**目标**: 创建 `/system/dict` 管理界面，支持类型和条目的增删改查、排序、启用/停用

### 文件清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `wh-frontend/src/views/system/dict/index.vue` | 字典管理主页面 |
| 修改 | `wh-frontend/src/router/index.js` | 添加 `/system/dict` 路由 |
| 修改 | `wh-frontend/src/components/layout/SidebarMenu.vue` | 添加"字典管理"菜单项（需确认） |

### 详细 TODOs

#### Step 6.1: 创建管理页面
**文件**: `wh-frontend/src/views/system/dict/index.vue`
- 左侧：字典类型列表（el-table），支持新增/编辑/删除类型
- 右侧：选中类型后展示该类型下的条目列表（el-table）
- 条目表格列: itemCode, itemLabel, tagType, sortOrder, status (启用/停用开关), 操作（编辑/删除）
- 条目新增/编辑对话框: itemCode, itemLabel, tagType（下拉选择: primary/success/warning/danger/info）, sortOrder, status
- tagType 下拉提供颜色预览
- 删除类型/条目前需二次确认，如果类型下有条目则提示
- 参考 `views/system/cost-quota/index.vue` 的页面布局风格

#### Step 6.2: 添加路由
**文件**: `wh-frontend/src/router/index.js`
- 添加路由: `path: '/system/dict'`, `name: 'DictManager'`, `meta: { title: '字典管理', group: '系统管理', perm: 'ROLE_ADMIN' }`

#### Step 6.3: 添加菜单项
**文件**: `wh-frontend/src/components/layout/SidebarMenu.vue`
- 在"系统管理"分组下添加"字典管理"菜单项（如果菜单是动态生成则检查生成逻辑）

### 验收检查点
- [ ] 访问 `/system/dict` 可看到 11 个字典类型
- [ ] 点击类型可看到该类型下的所有条目
- [ ] 新增/编辑/删除条目功能正常
- [ ] 修改 itemLabel 后，业务页面刷新即可看到新标签（无需改代码）
- [ ] 停用某条目后，前端下拉列表不再显示该项
- [ ] 排序功能正常（前端按 sortOrder 升序展示）

---

## 总体时间估算

| Phase | 内容 | 预计人天 | 依赖 |
|-------|------|---------|------|
| Phase 1 | 数据库字典表 + 种子数据 | 0.5 | - |
| Phase 2 | 后端基础设施 | 1.5 | Phase 1 |
| Phase 3 | 后端 Bo 类迁移 | 1.0 | Phase 2 |
| Phase 4 | 前端基础设施 | 0.5 | Phase 2 |
| Phase 5 | 前端组件迁移 | 2.0 | Phase 4 |
| Phase 6 | 字典管理页面 | 1.0 | Phase 2 |
| 联调 + 验收 | 端到端测试 | 1.0 | Phase 5+6 |
| **合计** | | **7.5 人天** | |

## 一致性维护策略

### 以谁为准
- **item_code**: 以数据库已有业务数据中的值为准（历史数据不可变）。字典表 `wh_dict_item.item_code` 必须与现有数据完全一致
- **itemLabel / tagType**: 以字典表为准（管理界面可修改，修改后前端即时生效）
- **type_code**: 以 `DictTypes.java` 常量为权威定义。数据库 `wh_dict_type` 表作为持久化副本，二者通过启动校验（Phase 2 Step 2.6）保持同步

### 新增枚举值的流程
1. 管理界面在 `wh_dict_item` 表中新增条目（itemCode + itemLabel + tagType）
2. 前端 `el-option` 下拉列表自动显示新条目（无需改代码、打包、部署）
3. **如果新条目需要参与后端状态流转判断**：在对应 Bo 文件中添加该 item_code 的条件分支（这是业务逻辑变更，需要代码修改是合理的）
4. **如果仅用于 UI 展示**（如 `CLOSED` 状态）：无需修改后端代码

### CLOSED 状态处理
- `CHARTER_STATUS` 的 `CLOSED` 状态在前端已有展示逻辑（Dashboard、Charter 列表）
- 后端当前无设置 `CLOSED` 的业务代码（无"关闭立项"的审批流程）
- **决策**: 保留 `CLOSED` 在种子数据和字典条目中，作为合法的展示值。后续如有"关闭立项"业务需求，再补全后端流转逻辑。**不删除**种子数据中已有的 `CLOSED` 条目

### 不一致的检测与修复
- **启动校验**（Phase 2 Step 2.6）: 每次启动时自动校验 `DictTypes` 常量与 `wh_dict_type` 表的双向一致性，WARN 日志不阻塞启动
- **修复方向**: 
  - 若 `DictTypes` 有但表中无 → 补充种子数据 SQL
  - 若表中有但 `DictTypes` 无 → 在 `DictTypes.java` 中补全常量（或确认该类型已废弃，清理字典表）

## 风险与缓解

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| 种子数据 item_code 与现有数据不一致 | 中 | 高 | Phase 1 完成后用 SQL 对比现有数据中的 DISTINCT 值；特别检查 WBS 相关：核对种子数据中 `WBS_ELEMENT_TYPE` 是否包含 `TASK`，`WBS_ELEMENT_STATUS` 是否包含 `NOT_STARTED`（这些值硬编码在 `WhPmWbsElementBo.importWbs()` 方法中） |
| 前端迁移遗漏隐藏组件 | 中 | 中 | Phase 5 按 `grep -rn "statusLabel\|statusTagType"` 逐文件确认后划勾 |
| 管理界面误删正在使用的字典条目 | 低 | 高 | Phase 6 删除前显示警告对话框，提示需手动检查该 `item_code` 是否在前后端代码中引用；软删除而非物理删除 |
| 字典 API 性能（全量加载 70+ 条目） | 低 | 低 | 全量加载一次约 5KB，在 router 守卫中异步加载不阻塞渲染 |
| WBS 导入逻辑（`WhPmWbsElementBo`）中硬编码 TASK/NOT_STARTED | 低 | 中 | Phase 1 种子数据阶段（而非 Phase 3）已核对：确保 `wh_dict_item` 表中 `WBS_ELEMENT_TYPE` 包含 `TASK`，`WBS_ELEMENT_STATUS` 包含 `NOT_STARTED`，与 `WhPmWbsElementBo.importWbs()` 方法中的硬编码值一致。Phase 3 不做文件修改 |

## 开放问题
- [ ] WBS Element 的 `ELEMENT_TYPE` 当前只有 `TASK`；代码中是否有 `MILESTONE`、`PHASE`、`DELIVERABLE` 等其他值的隐含需求？
- [ ] `CHARTER_STATUS` 前端有 `CLOSED` 状态但后端 Bo 中未找到设置 `CLOSED` 的代码 -- 已决策：保留 `CLOSED` 在字典数据中作为合法展示值（见一致性维护策略），后续如有"关闭立项"需求再补全后端流转逻辑
- [ ] `BUDGET_STATUS` 使用 `PENDING` 而非 `PENDING_APPROVAL`，是否需要在字典中统一命名？
