-- Budget Management Tables (FR-PM-021)

-- pm_budget_item 预算科目骨架表
CREATE TABLE IF NOT EXISTS pm_budget_item (
    ID              TEXT NOT NULL,
    BUDGET_ID       TEXT NOT NULL REFERENCES pm_budget(ID),
    CATEGORY        TEXT NOT NULL,
    PARENT_ID       TEXT REFERENCES pm_budget_item(ID),
    AMOUNT          TEXT NOT NULL DEFAULT '0',
    LEVEL           INTEGER NOT NULL DEFAULT 1,
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

-- pm_budget_item_labor 人工明细表
CREATE TABLE IF NOT EXISTS pm_budget_item_labor (
    ID              TEXT NOT NULL,
    BUDGET_ITEM_ID  TEXT NOT NULL REFERENCES pm_budget_item(ID),
    ROLE_CODE       TEXT NOT NULL,
    HOURS           TEXT NOT NULL,
    COST_RATE       TEXT NOT NULL,
    AMOUNT          TEXT NOT NULL,
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

-- pm_budget_item_procurement 采购明细表
CREATE TABLE IF NOT EXISTS pm_budget_item_procurement (
    ID              TEXT NOT NULL,
    BUDGET_ITEM_ID  TEXT NOT NULL REFERENCES pm_budget_item(ID),
    BOM_ITEM        TEXT NOT NULL,
    QTY             TEXT DEFAULT '0',
    UNIT_PRICE      TEXT DEFAULT '0',
    AMOUNT          TEXT NOT NULL,
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

-- pm_budget_item_other 其他费用明细表（差旅/商务/招待/活动/其他共用）
CREATE TABLE IF NOT EXISTS pm_budget_item_other (
    ID              TEXT NOT NULL,
    BUDGET_ITEM_ID  TEXT NOT NULL REFERENCES pm_budget_item(ID),
    CATEGORY        TEXT NOT NULL,
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

-- pm_cost_warning 成本预警表
CREATE TABLE IF NOT EXISTS pm_cost_warning (
    ID              TEXT NOT NULL,
    PROJECT_ID      TEXT NOT NULL REFERENCES wh_pm_project_charter(ID),
    BUDGET_ID       TEXT NOT NULL REFERENCES pm_budget(ID),
    LEVEL           TEXT NOT NULL,
    RATIO           TEXT NOT NULL,
    STATUS          TEXT NOT NULL DEFAULT 'ACTIVE',
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

-- pm_actual_cost 扩展字段
ALTER TABLE pm_actual_cost ADD COLUMN BUDGET_ITEM_ID TEXT REFERENCES pm_budget_item(ID);
ALTER TABLE pm_actual_cost ADD COLUMN SOURCE_SYSTEM TEXT;
ALTER TABLE pm_actual_cost ADD COLUMN SOURCE_ID TEXT;
