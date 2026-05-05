-- Add module version release date column to erp_module
ALTER TABLE erp_module ADD COLUMN VERSION_RELEASE_DATE TEXT;

-- Populate existing modules with default release date
UPDATE erp_module SET VERSION_RELEASE_DATE = datetime('now', 'localtime') WHERE VERSION_RELEASE_DATE IS NULL;
