# Database migrations (Liquibase)

Liquibase owns the schema. Hibernate runs with `ddl-auto: validate` in every profile and
only cross-checks that the JPA mappings match what Liquibase built.

## Layout

- `db.changelog-master.yaml` — the root changelog; `include`s each change file in order.
- `changes/001-baseline.sql` — full schema for the current JPA model, generated from Hibernate
  metadata. It has a precondition (`onFail: MARK_RAN`) that skips it when the `orders` table
  already exists, so it is a no-op on any database first built by the old `ddl-auto: update`.
- `changes/002-drop-legacy-tables.sql` — drops the pre-generalization tables
  (`customers`, `suppliers`, `purchase_orders*`, `sales_orders*`) that `ddl-auto: update`
  left behind. No-op on a fresh database.
- `changes/003-version-not-null.sql` — backfills `version = 0` and sets `NOT NULL DEFAULT 0`
  on the optimistic-lock columns for databases where they were added nullable. No-op on a
  fresh baseline (which already declares them that way).

## Adding a change

Create `changes/NNN-short-name.sql` as a Liquibase *formatted SQL* file:

```sql
--liquibase formatted sql

--changeset erp:NNN-short-name
ALTER TABLE parties ADD COLUMN whatever varchar(40);
--rollback ALTER TABLE parties DROP COLUMN whatever;
```

then add an `include` line to `db.changelog-master.yaml`. **Never edit a changeset that has
already run in any environment** — add a new one.

## Regenerating the baseline (only before the first real deployment)

```
mvn -q spring-boot:run -Dspring-boot.run.arguments="\
  --spring.jpa.hibernate.ddl-auto=none \
  --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create \
  --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=/tmp/schema-gen.sql \
  --spring.jpa.properties.hibernate.hbm2ddl.delimiter=;"
```

then splice `/tmp/schema-gen.sql` into `001-baseline.sql` below the changeset header.
