-- Add project short name column to project charter table
ALTER TABLE wh_pm_project_charter ADD COLUMN PROJECT_SHORT_NAME TEXT NOT NULL DEFAULT '';
