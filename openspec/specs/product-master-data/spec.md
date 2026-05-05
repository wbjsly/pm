## ADDED Requirements

### Requirement: Product Master Data Management
The system SHALL provide an `erp_product` table with fields: id, product_code (unique), product_name, status (ACTIVE/INACTIVE/PLACEHOLDER), description, audit fields. Product codes SHALL be unique.

#### Scenario: Create product
- **WHEN** a user creates a new product with a unique product_code
- **THEN** the system creates the product with status ACTIVE

#### Scenario: Duplicate product code
- **WHEN** a user attempts to create a product with an existing product_code
- **THEN** the system returns 409 Conflict

### Requirement: Module Master Data Management
The system SHALL provide an `erp_module` table with fields: id, product_id (FK to erp_product), module_code, module_name, description, audit fields. Each module SHALL belong to exactly one product.

#### Scenario: Create module under product
- **WHEN** a user creates a module with a valid product_id
- **THEN** the system creates the module linked to the product

### Requirement: Placeholder Product Creation
When a WBS import references a product_code that does not exist, the system SHALL automatically create a placeholder product record with product_name = product_code and status = PLACEHOLDER. The same SHALL apply for module references that do not exist under the resolved product.

#### Scenario: Import with unknown product code
- **WHEN** a WBS import contains product_code "PROD-UNKNOWN" that does not exist
- **THEN** the system creates a placeholder product with product_name="PROD-UNKNOWN" and status=PLACEHOLDER

#### Scenario: Placeholder product visibility
- **WHEN** a placeholder product is displayed in the WBS list
- **THEN** it is visually distinguished (e.g., gray label "占位产品")
