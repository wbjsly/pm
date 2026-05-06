-- Add actual start date column for tracking when tasks actually started
ALTER TABLE pm_wbs_element ADD COLUMN ACTUAL_START_DATE TEXT;
