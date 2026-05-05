## Budget Database Schema

### pm_budget_item (预算科目骨架表)

```sql
CREATE TABLE IF NOT EXISTS pm_budget_item (
    ID              TEXT NOT NULL,
    BUDGET_ID       TEXT NOT NULL REFERENCES pm_budget(ID),
    CATEGORY        TEXT NOT NULL,           -- LABOR/PROCUREMENT/TRAVEL/BUSINESS/ENTERTAINMENT/ACTIVITY/OTHER
    PARENT_ID       TEXT REFERENCES pm_budget_item(ID),
    AMOUNT          TEXT NOT NULL DEFAULT '0',
    LEVEL           INTEGER NOT NULL DEFAULT 1,  -- 1=一级科目, 2=二级科目
    SORT_ORDER      INTEGER DEFAULT 0,
    CREATE_BY       TEXT,
    CREATE_DATE     TEXT DEFAULT (datetime('now', 'localtime')),
    UPDATE_BY       TEXT,
    UPDATE_DATE     TEXT DEFAULT (datetime('now', 'localtime')),
    REMARKS         TEXT,
    DEL_FLAG        TEXT DEFAULT '0',
    VER_NO          INTEGER DEFAULT 0 NOT NULL,
    SYS_CODE        TEXT,
    CONSTRAINT PK_PM_BUDGET_ITEM PRIMARY KEY (ID)
);

CREATE INDEX IF NOT EXISTS IDX_PM_BI_BUDGET ON pm_budget_item(BUDGET_ID);
CREATE INDEX IF NOT EXISTS IDX_PM_BI_PARENT ON pm_budget_item(PARENT_ID);
CREATE INDEX IF NOT EXISTS IDX_PM_BI_CATEGORY ON pm_budget_item(CATEGORY);
```

### pm_budget_item_labor (人工明细表)

```sql
CREATE TABLE IF NOT EXISTS pm_budget_item_labor (
    ID              TEXT NOT NULL,
    BUDGET_ITEM_ID  TEXT NOT NULL REFERENCES pm_budget_item(ID),
    ROLE_CODE       TEXT NOT NULL,           -- 岗位/角色编码
    HOURS           TEXT NOT NULL,           -- 工时
    COST_RATE       TEXT NOT NULL,           -- 成本定额（元/小时）
    AMOUNT          TEXT NOT NULL,           -- = HOURS * COST_RATE
    CREATE_BY       TEXT,
    CREATE_DATE     TEXT DEFAULT (datetime('now', 'localtime')),
    UPDATE_BY       TEXT,
    UPDATE_DATE     TEXT DEFAULT (datetime('now', 'localtime')),
    REMARKS         TEXT,
    DEL_FLAG        TEXT DEFAULT '0',
    VER_NO          INTEGER DEFAULT 0 NOT NULL,
    SYS_CODE        TEXT,
    CONSTRAINT PK_PM_BI_LABOR PRIMARY KEY (ID)
);

CREATE INDEX IF NOT EXISTS IDX_PM_BIL_ITEM ON pm_budget_item_labor(BUDGET_ITEM_ID);
```

### pm_budget_item_procurement (采购明细表)

```sql
CREATE TABLE IF NOT EXISTS pm_budget_item_procurement (
    ID              TEXT NOT NULL,
    BUDGET_ITEM_ID  TEXT NOT NULL REFERENCES pm_budget_item(ID),
    BOM_ITEM        TEXT NOT NULL,           -- BOM 项名称
    QTY             TEXT DEFAULT '0',        -- 数量
    UNIT_PRICE      TEXT DEFAULT '0',        -- 单价
    AMOUNT          TEXT NOT NULL,           -- = QTY * UNIT_PRICE
    CREATE_BY       TEXT,
    CREATE_DATE     TEXT DEFAULT (datetime('now', 'localtime')),
    UPDATE_BY       TEXT,
    UPDATE_DATE     TEXT DEFAULT (datetime('now', 'localtime')),
    REMARKS         TEXT,
    DEL_FLAG        TEXT DEFAULT '0',
    VER_NO          INTEGER DEFAULT 0 NOT NULL,
    SYS_CODE        TEXT,
    CONSTRAINT PK_PM_BI_PROCUREMENT PRIMARY KEY (ID)
);

CREATE INDEX IF NOT EXISTS IDX_PM_BIP_ITEM ON pm_budget_item_procurement(BUDGET_ITEM_ID);
```

### pm_budget_item_other (其他费用明细表)

差旅、商务费用、客户招待费、活动费、其他共用此表，通过 CATEGORY 字段区分。

```sql
CREATE TABLE IF NOT EXISTS pm_budget_item_other (
    ID              TEXT NOT NULL,
    BUDGET_ITEM_ID  TEXT NOT NULL REFERENCES pm_budget_item(ID),
    CATEGORY        TEXT NOT NULL,           -- TRAVEL/BUSINESS/ENTERTAINMENT/ACTIVITY/OTHER
    DESCRIPTION     TEXT,
    AMOUNT          TEXT NOT NULL,
    CREATE_BY       TEXT,
    CREATE_DATE     TEXT DEFAULT (datetime('now', 'localtime')),
    UPDATE_BY       TEXT,
    UPDATE_DATE     TEXT DEFAULT (datetime('now', 'localtime')),
    REMARKS         TEXT,
    DEL_FLAG        TEXT DEFAULT '0',
    VER_NO          INTEGER DEFAULT 0 NOT NULL,
    SYS_CODE        TEXT,
    CONSTRAINT PK_PM_BI_OTHER PRIMARY KEY (ID)
);

CREATE INDEX IF NOT EXISTS IDX_PM_BIO_ITEM ON pm_budget_item_other(BUDGET_ITEM_ID);
```

### pm_actual_cost 扩展

在现有 `pm_actual_cost` 表上新增字段：

```sql
ALTER TABLE pm_actual_cost ADD COLUMN BUDGET_ITEM_ID TEXT REFERENCES pm_budget_item(ID);
ALTER TABLE pm_actual_cost ADD COLUMN SOURCE_SYSTEM TEXT;   -- timesheet/procurement/reimbursement/manual
ALTER TABLE pm_actual_cost ADD COLUMN SOURCE_ID TEXT;       -- 来源记录ID（用于去重/撤回）
```

### pm_cost_warning (成本预警表)

```sql
CREATE TABLE IF NOT EXISTS pm_cost_warning (
    ID              TEXT NOT NULL,
    PROJECT_ID      TEXT NOT NULL REFERENCES wh_pm_project_charter(ID),
    BUDGET_ID       TEXT NOT NULL REFERENCES pm_budget(ID),
    LEVEL           TEXT NOT NULL,           -- INFO/WARN/CRITICAL
    RATIO           TEXT NOT NULL,           -- 实际/预算比率
    STATUS          TEXT NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE/CLOSED
    TRIGGERED_AT    TEXT DEFAULT (datetime('now', 'localtime')),
    CLOSED_BY       TEXT,
    CLOSED_AT       TEXT,
    CREATE_BY       TEXT,
    CREATE_DATE     TEXT DEFAULT (datetime('now', 'localtime')),
    UPDATE_BY       TEXT,
    UPDATE_DATE     TEXT DEFAULT (datetime('now', 'localtime')),
    REMARKS         TEXT,
    DEL_FLAG        TEXT DEFAULT '0',
    VER_NO          INTEGER DEFAULT 0 NOT NULL,
    SYS_CODE        TEXT,
    CONSTRAINT PK_PM_COST_WARNING PRIMARY KEY (ID)
);

CREATE INDEX IF NOT EXISTS IDX_PM_CW_PROJECT ON pm_cost_warning(PROJECT_ID);
CREATE INDEX IF NOT EXISTS IDX_PM_CW_BUDGET ON pm_cost_warning(BUDGET_ID);
CREATE INDEX IF NOT EXISTS IDX_PM_CW_STATUS ON pm_cost_warning(STATUS, LEVEL);
```
