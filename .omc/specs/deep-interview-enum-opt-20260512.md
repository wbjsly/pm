# Deep Interview Spec: 枚举值数据驱动改造

## Metadata
- Interview ID: enum-opt-20260512
- Rounds: 7
- Final Ambiguity Score: 13.3%
- Type: brownfield
- Generated: 2026-05-12T11:52+08:00
- Threshold: 0.2
- Initial Context Summarized: no
- Status: PASSED

## Clarity Breakdown
| Dimension | Score | Weight | Weighted |
|-----------|-------|--------|----------|
| Goal Clarity | 0.90 | 35% | 0.315 |
| Constraint Clarity | 0.90 | 25% | 0.225 |
| Success Criteria | 0.80 | 25% | 0.200 |
| Context Clarity | 0.85 | 15% | 0.128 |
| **Total Clarity** | | | **0.868** |
| **Ambiguity** | | | **13.3%** |

## Goal
将所有枚举值（状态、类型、分类等）从硬编码字符串改为数据库字典表驱动，前端通过 API 获取，后端 Bo 类引用字典常量；提供可视化管理界面支持运营配置，新增枚举值无需改代码发版。**状态流转规则仍保留在代码中**（使用 Java enum 或常量类），不做数据库化。

## Constraints
- 2 周内一次性完成
- 历史数据（已有 Charter、Budget、Deliverable 等记录）必须兼容，不改动存量数据
- 状态流转逻辑（如 DRAFT→PENDING→APPROVED）保留在 Java 代码中，不走数据库配置

## Non-Goals
- 状态机/工作流规则的数据驱动化（明确排除）
- 国际化支持（枚举值目前只需中文 label）
- 枚举值的版本管理或审计日志

## Acceptance Criteria
- [ ] 提供数据库字典表存储所有枚举类型和枚举值，支持 type + code + label 结构
- [ ] 提供管理页面（/system/dict），可对枚举值进行增删改、排序、启用/停用
- [ ] 后端所有 Bo 类中的字符串字面量状态判断替换为引用字典 code（或 Java enum 常量）
- [ ] 前端提供共享常量/API 层，从后端获取枚举列表，各 Vue 组件不再各自定义映射
- [ ] 已有历史数据正常工作，状态展示、筛选、流转均不受影响
- [ ] 新增一个枚举值只需在管理界面操作，无需改代码和重新部署

## Assumptions Exposed & Resolved
| Assumption | Challenge | Resolution |
|------------|-----------|------------|
| 状态流转也需要数据驱动 | Contrarian Mode: 流转规则极少变动，是否过度设计？ | 确认过度设计，状态流转保留在代码中 |
| 可以分模块逐步推进 | 是否需要分步？ | 一次完成，2周内可用 |
| 只需解决维护成本 | 驱动力是什么？ | 需要同时解决灵活性、一致性、维护成本三个问题 |

## Technical Context
- 后端: Spring Boot 2.7 + MyBatis-Plus + SQLite
- 前端: Vue 3 + Vite + Element Plus
- 当前状态: 仅 1 个 Java enum（CostEvent.SourceSystem），其余全部 String 硬编码
- 数据库: 70+ 张业务表，无字典表
- 无字典/枚举查询 API
- 前端枚举映射散落在 20+ 个 Vue 组件中重复定义
- 涉及模块: Charter, Budget, Deliverable, WBS, WorkLog, CostWarning, WorkCalendar, Product, User

## Ontology (Key Entities)
| Entity | Type | Fields | Relationships |
|--------|------|--------|---------------|
| 枚举值 | core domain | code, label, type, sort, enabled | 被 Bo类 引用校验，被 前端组件 消费展示 |
| 字典类型 | core domain | type_code, type_name, remark | 包含 多个枚举值 |
| 状态流转 | core domain | from_status, to_status, domain | 依赖 枚举值，保留在代码中 |
| 字典管理界面 | supporting | type管理, code管理, 排序, 启停 | 管理 枚举值 和 字典类型 |
| 前端组件 | supporting | statusLabel(), statusTagType(), selectOptions | 展示 枚举值，通过 API 获取 |
| Bo类 | supporting | statusCheck(), statusTransition() | 校验 枚举值，引用字典常量 |
| 历史数据 | external system | 已有Charter/Budget/Deliverable记录 | 依赖于现有枚举值 code |

## Ontology Convergence
| Round | Entity Count | New | Changed | Stable | Stability Ratio |
|-------|-------------|-----|---------|--------|----------------|
| 1 | 5 | 5 | - | - | N/A |
| 2 | 7 | 2 | 0 | 5 | 71% |
| 3 | 8 | 1 | 0 | 7 | 88% |
| 4 | 7 | 0 | 0 | 7 | 100% |
| 5 | 7 | 0 | 0 | 7 | 100% |
| 6 | 7 | 0 | 0 | 7 | 100% |
| 7 | 7 | 0 | 0 | 7 | 100% |

## 推荐方案

### 整体架构

```
┌──────────────────────────────────────────────────────┐
│                  管理界面 /system/dict                  │
│               (字典类型管理 + 枚举值管理)                 │
└──────────────────────┬───────────────────────────────┘
                       │ CRUD
┌──────────────────────▼───────────────────────────────┐
│              wh_dict_type / wh_dict_item              │
│         (字典类型表)      (枚举值表: type, code, label)  │
└──────────────────────┬───────────────────────────────┘
                       │
         ┌─────────────┴─────────────┐
         │                           │
┌────────▼────────┐     ┌────────────▼──────────┐
│   后端 Bo 类      │     │   前端共享层            │
│ 引用常量/DictCode │     │ src/utils/dict.js     │
│ (编译期安全)      │     │ (API 获取 + 缓存)      │
└─────────────────┘     └───────────────────────┘
```

### 后端改造

**1. 新增字典表**（数据库迁移 SQL）

```sql
-- wh_dict_type: 字典类型
CREATE TABLE wh_dict_type (
    id CHAR(32) PRIMARY KEY,
    type_code VARCHAR(64) NOT NULL UNIQUE,  -- 如 'CHARTER_STATUS'
    type_name VARCHAR(128) NOT NULL,         -- 如 '项目章程状态'
    remark VARCHAR(255),
    is_enabled CHAR(1) DEFAULT '1',
    sort_order INT DEFAULT 0,
    created_at, updated_at, ...
);

-- wh_dict_item: 枚举值
CREATE TABLE wh_dict_item (
    id CHAR(32) PRIMARY KEY,
    type_code VARCHAR(64) NOT NULL,          -- 关联 wh_dict_type.type_code
    item_code VARCHAR(64) NOT NULL,          -- 如 'DRAFT', 'APPROVED'
    item_label VARCHAR(128) NOT NULL,         -- 如 '草稿', '已通过'
    ext_value VARCHAR(255),                   -- 扩展字段（如 tagType: 'info'/'success'）
    is_enabled CHAR(1) DEFAULT '1',
    sort_order INT DEFAULT 0,
    UNIQUE(type_code, item_code),
    created_at, updated_at, ...
);
```

**2. 新增后端常量类**（`com.wh.constant.DictTypes.java`）

```java
// 只定义 type_code 常量（极少变动），不定义 item_code（管理界面可自由增删）
public interface DictTypes {
    String CHARTER_STATUS = "CHARTER_STATUS";
    String CHARTER_CATEGORY = "CHARTER_CATEGORY";
    String CHARTER_PROGRESS = "CHARTER_PROGRESS";
    String BUDGET_STATUS = "BUDGET_STATUS";
    String BUDGET_CATEGORY = "BUDGET_CATEGORY";
    String DELIVERABLE_STATUS = "DELIVERABLE_STATUS";
    String WBS_STATUS = "WBS_STATUS";
    String WBS_PRIORITY = "WBS_PRIORITY";
    String WBS_DIFFICULTY = "WBS_DIFFICULTY";
    String WORKLOG_STATUS = "WORKLOG_STATUS";
    String WARNING_STATUS = "WARNING_STATUS";
    String WARNING_LEVEL = "WARNING_LEVEL";
    String CALENDAR_DAY_TYPE = "CALENDAR_DAY_TYPE";
    String PRODUCT_STATUS = "PRODUCT_STATUS";
    String USER_STATUS = "USER_STATUS";
}
```

**3. Bo 类改造**

- `"DRAFT".equals(status)` → 对于状态流转判断，引入 Java enum 类（如 `CharterStatus.DRAFT`）替代字符串字面量
- 对于通用的 code→label 查询，通过 `WhDictBo.getLabel(DictTypes.CHARTER_STATUS, status)` 获取中文名
- 状态流转的 if-else 逻辑保持不变（非本次改造范围）
- 新增 `WhDictBo`：提供字典查询服务，使用 Caffeine 本地缓存

**4. 新增 API 端点**

```
GET /api/system/dict/types              → 所有字典类型列表
GET /api/system/dict/items/{typeCode}   → 某类型下的枚举值列表（含 label, ext_value）
GET /api/system/dict/all                → 全量字典（前端一次性缓存）
POST/PUT/DELETE /api/system/dict/type   → 管理端 CRUD
POST/PUT/DELETE /api/system/dict/item   → 管理端 CRUD
```

**5. 管理端 Controller**

- `DictController`（管理接口）
- `DictPublicController`（前端查询接口，可匿名/登录访问）

### 前端改造

**1. 新增共享层** `wh-frontend/src/utils/dict.js`

```js
// 基于 Pinia 的字典 store，应用启动时全量加载
// 所有组件通过 useDictStore() 获取字典数据

export const useDictStore = defineStore('dict', {
  state: () => ({ dictMap: {} }),
  actions: {
    async loadAll() { /* GET /api/system/dict/all → dictMap */ },
    getLabel(typeCode, itemCode) { /* dictMap[typeCode][itemCode]?.label */ },
    getItems(typeCode) { /* Object.values(dictMap[typeCode] || {}) */ },
    getTagType(typeCode, itemCode) { /* dictMap[typeCode][itemCode]?.ext?.tagType */ },
  }
})
```
加载时机：在 `router.beforeEach` 中首次进入时调用 `loadAll()`，存入 Pinia store。后续所有页面均从 store 读取，零额外请求。

**2. Vue 组件改造**

改造前（重复定义）:
```js
const statusMap = { DRAFT: '草稿', APPROVED: '已通过', ... }
const statusTagMap = { DRAFT: 'info', APPROVED: 'success', ... }
```

改造后（统一调用）:
```js
const { getDictLabel, getDictTagType, getDictItems } = useDict()
// 模板中: {{ getDictLabel('CHARTER_STATUS', row.status) }}
```

**3. 下拉选项改造**

改造前（硬编码 `<el-option>`）→ 改造后（v-for 遍历 `getDictItems('BUDGET_CATEGORY')`）

**4. 新增管理页面** `views/system/dict/index.vue`

- 左栏: 字典类型列表（增删改）
- 右栏: 选中类型的枚举值列表（增删改、排序、启用/停用）

### 执行步骤

| 阶段 | 内容 | 工期 |
|------|------|------|
| **Phase 1** | 数据库迁移 + 实体/Mapper + 初始数据填充 | 2天 |
| **Phase 2** | WhDictBo + 公开 API + 缓存 | 2天 |
| **Phase 3** | 前端 dict.js + 管理页面 | 3天 |
| **Phase 4** | 逐模块替换（后端常量 + 前端组件） | 4天 |
| **Phase 5** | 回归测试 + 历史数据兼容验证 | 2天 |
| **Phase 6** | 清理 + Code Review + 文档 | 1天 |

### 风险与应对

| 风险 | 等级 | 应对 |
|------|------|------|
| 遗漏硬编码 | 中 | Phase 4 逐模块 checklist 式扫荡，每改完一个模块跑该模块的页面确认 |
| 历史数据不兼容 | 中 | 初始数据填充时确保 item_code 与现有数据中的字符串值完全一致；Phase 5 专项验证 |
| SQLite 并发限制 | 低 | 字典数据量小（< 500 条），前端可全量缓存，加上后端本地缓存（Caffeine），数据库压力极低 |
| 前端缓存过期 | 低 | 管理端修改后提供刷新机制；也可配合版本号/时间戳校验 |
| 2周时间紧张 | 中 | 优先 Charter + Budget 两个最大模块，其余模块可降级为"常量引用 + 暂不接字典 API"，但管理页面和字典表结构必须一次到位 |

## Interview Transcript
<details>
<summary>Full Q&A (5 rounds)</summary>

### Round 1
**Q:** 你发起这个优化的主要驱动力是什么？
**A:** 以上都是，需要系统性解决（灵活性、一致性、维护成本三者兼顾）
**Ambiguity:** 65.5%

### Round 2
**Q:** 改造深度：轻量级 vs 中等 vs 完全数据驱动？
**A:** 深度：完全数据驱动（含状态机）
**Ambiguity:** 54.0%

### Round 3
**Q:** 哪些是"必须做到"的验收标准？
**A:** 枚举值运营可配、状态流转可配、管理界面、向后兼容 — 四个都必须
**Ambiguity:** 37.5%

### Round 4 (Contrarian)
**Q:** 状态流转规则做成数据驱动是否过度设计？
**A:** 确实过度设计了，流转规则保留在代码中
**Ambiguity:** 29.5%

### Round 5
**Q:** 时间窗口和资源约束？
**A:** 一次性完成，2周内可用
**Ambiguity:** 17.0%

### Round 6 (Simplifier)
**Q:** DictCodes 常量类是否过度设计？（新增枚举需要同步加常量 → 与"不改代码免发布"矛盾）
**A:** 只建 type_code 常量，不建 item_code 常量
**Ambiguity:** 14.5%

### Round 7
**Q:** 前端缓存策略怎么选？
**A:** 应用启动时全量加载到 Pinia store
**Ambiguity:** 13.3% ✅

</details>
