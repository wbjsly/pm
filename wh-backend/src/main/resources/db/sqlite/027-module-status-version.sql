-- Add status and version columns to erp_module
ALTER TABLE erp_module ADD COLUMN STATUS TEXT NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE erp_module ADD COLUMN MODULE_VERSION TEXT;
