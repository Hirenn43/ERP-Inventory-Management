--liquibase formatted sql

--changeset erp:003-version-not-null splitStatements:true endDelimiter:;
--comment: Bring the optimistic-lock version columns to NOT NULL DEFAULT 0 on DBs first built by ddl-auto:update (where the column was added nullable, no default). No-op on a fresh Liquibase baseline, which already declares them that way.
UPDATE orders                     SET version = 0 WHERE version IS NULL;
UPDATE material_stock_movements   SET version = 0 WHERE version IS NULL;
UPDATE parties                    SET version = 0 WHERE version IS NULL;
UPDATE materials                  SET version = 0 WHERE version IS NULL;
UPDATE products                   SET version = 0 WHERE version IS NULL;
UPDATE brokers                    SET version = 0 WHERE version IS NULL;
UPDATE areas                      SET version = 0 WHERE version IS NULL;
UPDATE designs                    SET version = 0 WHERE version IS NULL;
UPDATE jobs                       SET version = 0 WHERE version IS NULL;
UPDATE schedules                  SET version = 0 WHERE version IS NULL;
UPDATE transporters               SET version = 0 WHERE version IS NULL;
UPDATE vehicles                   SET version = 0 WHERE version IS NULL;
ALTER TABLE orders                     ALTER COLUMN version SET DEFAULT 0, ALTER COLUMN version SET NOT NULL;
ALTER TABLE material_stock_movements   ALTER COLUMN version SET DEFAULT 0, ALTER COLUMN version SET NOT NULL;
ALTER TABLE parties                    ALTER COLUMN version SET DEFAULT 0, ALTER COLUMN version SET NOT NULL;
ALTER TABLE materials                  ALTER COLUMN version SET DEFAULT 0, ALTER COLUMN version SET NOT NULL;
ALTER TABLE products                   ALTER COLUMN version SET DEFAULT 0, ALTER COLUMN version SET NOT NULL;
ALTER TABLE brokers                    ALTER COLUMN version SET DEFAULT 0, ALTER COLUMN version SET NOT NULL;
ALTER TABLE areas                      ALTER COLUMN version SET DEFAULT 0, ALTER COLUMN version SET NOT NULL;
ALTER TABLE designs                    ALTER COLUMN version SET DEFAULT 0, ALTER COLUMN version SET NOT NULL;
ALTER TABLE jobs                       ALTER COLUMN version SET DEFAULT 0, ALTER COLUMN version SET NOT NULL;
ALTER TABLE schedules                  ALTER COLUMN version SET DEFAULT 0, ALTER COLUMN version SET NOT NULL;
ALTER TABLE transporters               ALTER COLUMN version SET DEFAULT 0, ALTER COLUMN version SET NOT NULL;
ALTER TABLE vehicles                   ALTER COLUMN version SET DEFAULT 0, ALTER COLUMN version SET NOT NULL;
--rollback -- not reversible
