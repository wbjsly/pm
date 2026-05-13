# Deep Interview Spec: 菜单管理重构 — parent_id 层级替代 group_name

## Metadata
- Interview ID: di-menu-refactor-20260513
- Rounds: 7
- Final Ambiguity Score: 5%
- Type: brownfield
- Generated: 2026-05-13
- Threshold: 0.2
- Initial Context Summarized: no
- Status: PASSED

## Clarity Breakdown
| Dimension | Score | Weight | Weighted |
|-----------|-------|--------|----------|
| Goal Clarity | 0.98 | 35% | 0.34 |
| Constraint Clarity | 0.95 | 25% | 0.24 |
| Success Criteria | 0.90 | 25% | 0.23 |
| Context Clarity | 0.93 | 15% | 0.14 |
| **Total Clarity** | | | **0.95** |
| **Ambiguity** | | | **5%** |

## Goal
将菜单层级管理从 `group_name` 文本分组改为 `parent_id` 父子关系。一级菜单（主页、系统管理、项目管理）作为 DB 记录存在（parent_id 为空）。子菜单通过 parent_id 关联到一级菜单。**完全删除 `group_name` 字段**（DB 列、实体字段、所有代码引用）。仅使用 `sort_order` 控制排序：一级菜单按 sort_order 排列（1=主页, 2=项目管理, 3=系统管理），子菜单在各父级下独立按 sort_order 排列。Sidebar 完全由 DB 驱动，不再硬编码菜单项。

## Constraints
- **完全删除** `GROUP_NAME` 列（DDL 中移除，种子数据中移除）
- **完全删除** `SysMenu.java` 中的 `groupName` 字段
- **完全删除** 前端所有 `groupName` 引用（SidebarMenu.vue, menu/index.vue）
- 仅使用 `sort_order` 控制所有排序：一级菜单之间、子菜单在同一父级下均按此排序
- 数据迁移方式：更新种子 SQL `048-sys-menu.sql`，删除 `wh.sqlite` 重建
- `SidebarMenu.vue` 中硬编码的"主页"必须删除

## Non-Gos
- 不保留 `group_name` 字段（不存文本也不存数值）
- 不新建增量迁移 SQL 文件
- 不修改实体字段名之外的 MyBatis-Plus 配置

## Acceptance Criteria
- [ ] Sidebar 显示正确：主页在最上方，系统管理和项目管理分组正确展开/收起，子菜单都在正确的一级菜单下，点击后路由跳转正常
- [ ] 管理后台验证：树形表格层级正确，新增/编辑子菜单 parent_id 正确关联，CRUD 操作正常
- [ ] 前后端联合验证：角色权限过滤正常，接口返回数据层级正确，所有涉及 groupName 的逻辑不再生效
- [ ] `group_name` 在代码库中无残留（grep 验证）

## Assumptions Exposed & Resolved
| Assumption | Challenge | Resolution |
|------------|-----------|------------|
| group_name 文本分组是必要的 | 为什么不用 parent_id？ | parent_id 层级，group_name 完全删除 |
| 需要 group_name 作为一级菜单排序 | sort_order 能否同时处理？ | sort_order 共用：一级菜单之间排序 + 子菜单各自排序 |
| groupName 实体字段名保留 | 能否直接删除？ | 完全删除，包括 @TableField 注解 |
| 主页硬编码就够了 | 为什么主页不进 DB？ | 主页改为 DB 记录，Sidebar 完全由 DB 驱动 |
| 需要增量迁移 SQL | 开发环境能否直接重建？ | 更新种子 SQL + 删除 wh.sqlite 重建 |

## Technical Context

### 涉及文件

**后端：**
- `048-sys-menu.sql` — DDL 移除 GROUP_NAME 列和索引，种子数据重写：新增3条一级菜单 + 修改10条现有记录
- `SysMenu.java` — 删除 `groupName` 字段及 `@TableField("GROUP_NAME")` 注解
- `WhPmMenuBo.java` — 排序从 `GROUP_NAME, SORT_ORDER` 改为 `SORT_ORDER`

**前端：**
- `SidebarMenu.vue` — 核心改动：删除硬编码"主页"，改为 parent_id 树形渲染
- `menu/index.vue` — 删除所有 groupName 引用：列、表单字段、校验规则、下拉选项

### 种子数据结构（新）
```sql
-- DDL：无 GROUP_NAME 列，无 IDX_SYS_MENU_GROUP 索引

-- 一级菜单（parent_id=''）
INSERT INTO sys_menu (ID, PARENT_ID, TITLE, PATH, ICON, SORT_ORDER, PERM, STATUS)
VALUES ('MENU_HOME', '', '主页', '/dashboard', 'HomeFilled', 1, '', '1');
VALUES ('MENU_PM', '', '项目管理', '', 'Folder', 2, 'ROLE_PM', '1');
VALUES ('MENU_SYS', '', '系统管理', '', 'Setting', 3, 'ROLE_ADMIN', '1');

-- 子菜单 parent_id 指向一级菜单
VALUES ('MENU001', 'MENU_PM', '项目立项', '/pm/charter', 'Document', 1, 'ROLE_PM', '1');
...
```

### SidebarMenu 渲染逻辑（新）
```
1. 从 menuStore 读取所有菜单
2. 一级菜单 = parent_id 为空的记录，按 sort_order 升序
3. 每个一级菜单：
   - 无子菜单 → <el-menu-item>（如主页直接渲染为菜单项）
   - 有子菜单 → <el-sub-menu>，children 按 sort_order 升序
4. 删除硬编码"主页"和所有 group_name 分组逻辑
```

## Ontology (Key Entities)
| Entity | Type | Fields | Relationships |
|--------|------|--------|---------------|
| SysMenu | core domain | id, parentId, title, path, icon, sortOrder, perm, status | parent has many children |
| 一级菜单 | supporting | (SysMenu where parentId='') | has many 子菜单 |
| 子菜单 | supporting | (SysMenu where parentId is set) | belongs to 一级菜单 |
| SidebarMenu | UI component | — | renders SysMenu tree |
| 角色权限 | supporting | roleCode | controls menu visibility via perm |

## Ontology Convergence
| Round | Entity Count | New | Changed | Stable | Stability Ratio |
|-------|-------------|-----|---------|--------|----------------|
| 2 | 5 | 5 | 0 | — | N/A |
| 3 | 5 | 0 | 0 | 5 | 100% |
| 4 | 5 | 0 | 0 | 5 | 100% |
| 5 | 5 | 0 | 0 | 5 | 100% |
| 6 | 5 | 0 | 0 | 5 | 100% |
| 7 | 5 | 0 | 0 | 5 | 100% |

## Implementation Steps
1. 更新 `048-sys-menu.sql`：移除 GROUP_NAME 列和索引，新增3条一级菜单，修改10条现有记录的 parent_id
2. 删除 `SysMenu.java` 中的 `groupName` 字段
3. 修改 `WhPmMenuBo.java`：排序改为仅 `SORT_ORDER`
4. 重写 `SidebarMenu.vue`：parent_id 树形渲染替代 group_name 分组，删除硬编码主页
5. 修改 `menu/index.vue`：删除所有 groupName 引用
6. 删除 `wh.sqlite`，重启后端验证

## Interview Transcript
<details>
<summary>Full Q&A (7 rounds)</summary>

### Round 1 (Ambiguity: 100% → 44%)
**Q:** group_name 字段改为排序逻辑后，具体如何使用？
**A:** group_name 值改为纯数字，一级菜单按此升序排列

### Round 2 (Ambiguity: 44% → 31%)
**Q:** 表中已有 sort_order，两个字段如何分工？
**A:** 两个字段都保留。group_name 存数值控制一级菜单排序，sort_order 控制子菜单排序

### Round 3 (Ambiguity: 31% → 22%)
**Q:** 重构后"主页"在 Sidebar 中如何处理？
**A:** 主页变为 DB 记录，删除硬编码，Sidebar 完全由 DB 驱动

### Round 4 (Ambiguity: 22% → 11%)
**Q:** 重构完成后怎么验证？
**A:** Sidebar 显示 + 管理后台 + 前后端联合验证

### Round 5 (Ambiguity: 11%)
**Q:** 数据库迁移怎么做？
**A:** 更新种子 SQL + 重建库

### Round 6 (Ambiguity: 再确认)
**Q:** 还是要删除 group_name 字段，一级菜单也使用 sort_order？
**A:** 共用 sort_order，不同上下文中独立排序

### Round 7 (Ambiguity: ~5%)
**Q:** groupName 字段在实体和 DB 中如何处理？
**A:** 完全删除 groupName（实体字段 + DB 列 + 所有代码）
</details>
