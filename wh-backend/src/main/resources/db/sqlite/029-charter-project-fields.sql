-- Add project classification and financial fields to project charter table
ALTER TABLE wh_pm_project_charter ADD COLUMN CONTRACT_NO TEXT;
ALTER TABLE wh_pm_project_charter ADD COLUMN PROJECT_CATEGORY TEXT;
ALTER TABLE wh_pm_project_charter ADD COLUMN OUTPUT_VALUE_TAXABLE TEXT;
ALTER TABLE wh_pm_project_charter ADD COLUMN OUTPUT_VALUE_EXCLUDING_TAX TEXT;
ALTER TABLE wh_pm_project_charter ADD COLUMN TAX_RATE TEXT;
ALTER TABLE wh_pm_project_charter ADD COLUMN TAX_AMOUNT TEXT;
