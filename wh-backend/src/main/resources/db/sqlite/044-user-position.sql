-- 用户表增加成本岗位字段

-- 添加 position_id 列（先允许 NULL，迁移完数据后再 NOT NULL 不能直接在 SQLite 改）
ALTER TABLE sys_user ADD COLUMN POSITION_ID TEXT;

-- 已有用户默认设为"开发"岗位
UPDATE sys_user SET POSITION_ID = 'pos-006' WHERE POSITION_ID IS NULL AND DEL_FLAG = '0';

CREATE INDEX IF NOT EXISTS IDX_SYS_USER_POSITION ON sys_user(POSITION_ID);
