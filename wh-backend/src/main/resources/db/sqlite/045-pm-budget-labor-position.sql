-- Add POSITION_ID column to pm_budget_item_labor for dynamic position-quota linkage
ALTER TABLE pm_budget_item_labor ADD COLUMN POSITION_ID TEXT;

CREATE INDEX IF NOT EXISTS IDX_PM_BIL_POSITION ON pm_budget_item_labor(POSITION_ID);
