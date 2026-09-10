--liquibase formatted sql

--changeset erp:004-users-company splitStatements:true endDelimiter:;
--comment: Multi-tenancy: every user belongs to a company. Add users.company_id, ensure a company exists, backfill users and any company-less tenant rows to it, then make the tenant columns NOT NULL.
ALTER TABLE users ADD COLUMN IF NOT EXISTS company_id BIGINT;

INSERT INTO companies (name)
SELECT 'Default Company'
WHERE NOT EXISTS (SELECT 1 FROM companies);

UPDATE users
   SET company_id = (SELECT id FROM companies ORDER BY id LIMIT 1)
 WHERE company_id IS NULL;

ALTER TABLE users
    ALTER COLUMN company_id SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT fk_users_company FOREIGN KEY (company_id) REFERENCES companies (id);

CREATE INDEX IF NOT EXISTS idx_users_company ON users (company_id);

-- Any tenant-scoped row created before scoping was enforced: attach to the first company.
UPDATE parties      SET company_id = (SELECT id FROM companies ORDER BY id LIMIT 1) WHERE company_id IS NULL;
UPDATE materials    SET company_id = (SELECT id FROM companies ORDER BY id LIMIT 1) WHERE company_id IS NULL;
UPDATE products     SET company_id = (SELECT id FROM companies ORDER BY id LIMIT 1) WHERE company_id IS NULL;
UPDATE brokers      SET company_id = (SELECT id FROM companies ORDER BY id LIMIT 1) WHERE company_id IS NULL;
UPDATE areas        SET company_id = (SELECT id FROM companies ORDER BY id LIMIT 1) WHERE company_id IS NULL;
UPDATE schedules    SET company_id = (SELECT id FROM companies ORDER BY id LIMIT 1) WHERE company_id IS NULL;
UPDATE jobs         SET company_id = (SELECT id FROM companies ORDER BY id LIMIT 1) WHERE company_id IS NULL;
UPDATE designs      SET company_id = (SELECT id FROM companies ORDER BY id LIMIT 1) WHERE company_id IS NULL;
UPDATE transporters SET company_id = (SELECT id FROM companies ORDER BY id LIMIT 1) WHERE company_id IS NULL;
UPDATE vehicles     SET company_id = (SELECT id FROM companies ORDER BY id LIMIT 1) WHERE company_id IS NULL;
UPDATE resources    SET company_id = (SELECT id FROM companies ORDER BY id LIMIT 1) WHERE company_id IS NULL;

ALTER TABLE parties      ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE materials    ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE products     ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE brokers      ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE areas        ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE schedules    ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE jobs         ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE designs      ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE transporters ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE vehicles     ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE resources    ALTER COLUMN company_id SET NOT NULL;

--rollback ALTER TABLE users DROP CONSTRAINT fk_users_company; ALTER TABLE users DROP COLUMN company_id;
