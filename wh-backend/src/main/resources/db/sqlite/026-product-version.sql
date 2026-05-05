-- Add product version and release date columns to erp_product
ALTER TABLE erp_product ADD COLUMN PRODUCT_VERSION TEXT;
ALTER TABLE erp_product ADD COLUMN VERSION_RELEASE_DATE TEXT;

-- Populate existing products with default version
UPDATE erp_product SET PRODUCT_VERSION = '1.0.0', VERSION_RELEASE_DATE = datetime('now', 'localtime') WHERE PRODUCT_VERSION IS NULL;
