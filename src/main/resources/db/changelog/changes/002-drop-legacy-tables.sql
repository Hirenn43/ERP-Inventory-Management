--liquibase formatted sql

--changeset erp:002-drop-legacy-tables
--comment: Drop the pre-generalization tables (Customer/Supplier/PurchaseOrder/SalesOrder). ddl-auto:update left them behind in dev DBs; a fresh Liquibase build never creates them, so these DROPs are no-ops there.
DROP TABLE IF EXISTS purchase_order_items CASCADE;
DROP TABLE IF EXISTS purchase_orders CASCADE;
DROP TABLE IF EXISTS sales_order_items CASCADE;
DROP TABLE IF EXISTS sales_orders CASCADE;
DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS suppliers CASCADE;
--rollback -- legacy tables are gone for good; no rollback
