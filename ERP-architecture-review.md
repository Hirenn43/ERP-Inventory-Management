# ERP Inventory Management — Architecture & Code Review (second pass, Opus 5)

`main` @ `2e34d6c` · Spring Boot 3.3.2 / Java 17 · Postgres (dev) / MySQL (prod) · Redis · JWT
~8,970 lines `src/main` Java, 195 files · 35 tests

Same six dimensions. This pass read the files the first pass only sampled — all 21 service impls, every entity, every request DTO, the WhatsApp conversation machinery, and the test suite. **Findings marked 🆕 are new; ⚠️ corrects the earlier pass.**

---

## Status & triage

The system is currently in a **test / pre-production environment**: no real users, no production data, no public exposure. **None of the findings below is a live incident.** They are triaged by *when* to act, not just severity:

- **🔴 NOW** — a real bug corrupting test data, or a fix that gets materially harder/more expensive once production data and a coupled frontend exist (schema, DB constraints, `@Version` columns, API contract shape). Cheap to do while the system is empty.
- **🟠 BEFORE PROD** — real, but only bites with real users, real traffic, real data volume, or public exposure (rate limiting, caching, observability, secret rotation, N+1 at scale). Safe to defer while in test.
- **🟢 BACKLOG** — cleanup with no functional or security impact; do opportunistically.

Every row in the six sections below carries a 🔴 / 🟠 / 🟢 tag. The consolidated action list is under **Prioritised remediation** at the end.

---

## 1. Security

### Pros
- jjwt 0.12.x used correctly: `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)` — the signature is genuinely verified before any claim is read, on every path.
- BCrypt (`BCryptPasswordEncoder`, cost 10) for passwords; no home-rolled hashing.
- Stateless session policy; CSRF correctly disabled for a bearer-token API.
- Webhook HMAC-SHA256 with `MessageDigest.isEqual` — a real constant-time comparison, not `String.equals`.
- `WhatsAppSimulatorController` is `@Profile("dev")` — the dev backdoor genuinely cannot register in prod.
- Generic 500 body; no stack traces returned to clients.
- Secrets are env-var driven; `.env*` git-ignored; nothing sensitive is tracked in git.
- `WaWebhookPayload` is `@JsonIgnoreProperties(ignoreUnknown = true)` throughout — no deserialization surprises from Meta payload changes.

### Cons

| # | Sev | When | Finding |
|---|-----|------|---------|
| 1 | **Critical** | ✅ FIXED (`main`) | ~~**Anonymous privilege escalation.** `/api/auth/**` is `permitAll`; `RegisterRequest.roles` flows into `resolveRoles()` → `getOrCreateRole()` unchecked. `POST /api/auth/register {"roles":["ROLE_ADMIN"]}` mints an admin token for an anonymous caller.~~ Fixed: `roles` field removed from `RegisterRequest`; `AuthServiceImpl.register()` now hardcodes `ROLE_STAFF`, `resolveRoles()` deleted. `service/impl/AuthServiceImpl.java:51`. |
| 2 | **Critical** | ✅ FIXED (`main`) / 🟠 (rotation) | ~~**Committed JWT signing key as a silent fallback.** `${JWT_SECRET:404E63…6B59}` — a missing env var boots with a repo-public key.~~ Fixed: committed fallback removed from `application.yml`; `JwtService.validateSecret()` `@PostConstruct` fails boot if `JWT_SECRET` is blank or <32 bytes; dev profile supplies an explicit throwaway key. Secret **rotation** (jti denylist etc.) still 🟠. |
| 3 | **High** | 🟠 BEFORE PROD | **Roles are decorative — there is no authorization layer.** No `@EnableMethodSecurity`, no `@PreAuthorize`, no `@Secured`, no `hasRole()` matcher anywhere in 195 files. The chain is `.anyRequest().authenticated()`. `ROLE_STAFF` can delete parties, adjust stock, wipe masters. *Scaffold `@EnableMethodSecurity` now; full role matrix before prod.* |
| 4 | **High** | ✅ FIXED (type check, `main`) / 🟠 (rotation) | ~~**Access and refresh tokens are interchangeable.**~~ Fixed: `JwtAuthenticationFilter` now requires `type=access`; `AuthServiceImpl.refresh()` rejects anything but `type=refresh`. Filter parses the token once (`JwtService.parseClaims`) instead of 4×. Error mapping: `JwtException` → 401 "Token is invalid or expired"; refresh token-type errors throw a dedicated `TokenRefreshException` → 401 with its own message; `BadCredentialsException`'s 401 body stays deliberately generic (no message echo → no enumeration). Rotation / jti denylist / logout still 🟠. |
| 5 | **High** | 🟠 BEFORE PROD | **No brute-force protection anywhere.** `UserPrincipal.isAccountNonLocked()` hardcoded `true`; no attempt counter, no lockout, no rate-limit filter on login, the webhook, or anything else. |
| 6 | **High** | 🟠 BEFORE PROD | **Webhook signature check fails open.** `signatureValid()` returns `true` when `app-secret` is blank — the default, with no profile guard. Deploy without `WHATSAPP_APP_SECRET` and any caller can POST a forged Meta payload; `QuickEntryExecutor` then creates orders/parties **as the mapped ERP user**. `controller/WhatsAppWebhookController.java:66-70`. *Signature verification already works when configured; add the non-dev "must be set" guard before prod.* |
| 7 | **Medium** | ✅ FIXED (`main`) | ~~**`DataSeeder` plants `admin`/`Admin@123` in every profile** and logs the password at INFO.~~ Fixed: `@Profile("!prod")` on `DataSeeder`; log line no longer prints the password. |
| 8 | **Medium** | 🟠 BEFORE PROD | **CORS `allowedOriginPatterns("*")` + `allowCredentials(true)`.** Spring permits this pairing via patterns, so origin isolation is effectively off. `config/SecurityConfig.java:83-92`. |
| 9 | **Medium** | 🟠 BEFORE PROD | Swagger (`/v3/api-docs/**`, `/swagger-ui/**`) and `/h2-console/**` are in `PUBLIC_ENDPOINTS`, and the H2 driver ships in every profile. |
| 10 🆕 | **Medium** | 🟠 BEFORE PROD | **`QuickEntryExecutor` never populates `SecurityContextHolder`.** It constructs `new UserPrincipal(link.getUser())` and calls services directly. Any `@PreAuthorize` added later (fix #3) will silently **not apply** to the entire WhatsApp ingress path. *Fix together with #3.* |
| 11 🆕 | **Low/Med** | ✅ FIXED (`main`) | ~~**Unbounded string fields reach the DB unvalidated.** No `@Size` on any of the new master DTOs.~~ Fixed: `@Size` (matching entity column lengths), `@Email`, `@DecimalMin/@DecimalMax` (brokeragePct/gstRate/tdsPct 0–100), `@PositiveOrZero` (creditLimit*, rates, loadingCharge) added to Broker/Area/Design/Job/Schedule/Transporter/Vehicle/Material/Party requests. Broader coverage still tracked under API #3. |
| 12 | **Low** | ✅ FIXED (`main`) | ~~User enumeration: `register` distinguishes "Username is already taken" from "Email is already registered"; `CustomUserDetailsService` echoes the username.~~ Fixed: `register` throws one generic "Username or email is already in use"; `CustomUserDetailsService` throws "Invalid username or password" without echoing the input. |

---

## 2. Scalability

### Pros
- Stateless auth ⇒ trivially horizontally scalable app tier, no sticky sessions.
- WA conversation state in Redis with a TTL (`conversation-ttl-seconds`, default 900) — no unbounded in-JVM state, survives restarts and multi-instance.
- The **batch** on-hand query `sumSignedQuantityGrouped` is a proper single grouped SQL aggregate — the right shape.
- Hand-written DTO projections everywhere; no entity graphs accidentally serialized.
- `jackson.default-property-inclusion: non_null` trims payloads.

### Cons

| # | Sev | When | Finding |
|---|-----|------|---------|
| 1 🆕 | **High** | 🟠 BEFORE PROD | **Every authenticated request costs a user + roles DB round-trip.** `JwtAuthenticationFilter` calls `userDetailsService.loadUserByUsername()` on every request → `userRepository.findByUsername()` → `User.roles` is `@ManyToMany(fetch = EAGER)` → a join/second query. The JWT **already carries** `userId` and `roles`, no cache. At 1,000 req/s that's 1,000+ needless auth queries/s. `security/JwtAuthenticationFilter.java:47`. *Fine at test scale.* |
| 2 🆕 | **High** | 🟠 BEFORE PROD | **`GET /api/materials` is N+1 on an aggregate.** `MaterialServiceImpl.toResponse()` calls `onHand(id)` → `sumSignedQuantityByMaterialId` — **one SUM query per material row**. 500 materials = 501 queries. `service/impl/MaterialServiceImpl.java:110-122`. *Do sooner if it's already slow with seed data.* |
| 3 | **High** | 🟠 BEFORE PROD | **`GET /api/orders` N+1 storm.** Per order, `toResponse()` → `buildRequirement()` → `findByProductId()` + `availableQuantities()` (its own aggregate), plus lazy `party`/`product`/`fulfillmentEvents`. ≈5N+1 queries, full BOM shortfall recomputed per row. `getById` recomputes it too. |
| 4 🆕 | **High** | 🟠 BEFORE PROD *(cheap)* | **`Party.roles` is `@ElementCollection(fetch = EAGER)`** with no `default_batch_fetch_size`. Every `partyRepository.findAll()` triggers 1 + N `party_roles` selects — and `EntityResolver.party()` calls `findAll()` **once per candidate token**. `entity/Party.java:88-94`. *One-line config (`default_batch_fetch_size`) is worth doing now.* |
| 5 | **High** | ⏸️ DEFERRED (owner decision 2026-09-10) / 🟠 (scale) / 🔴 (contract) | **No pagination on any of ~24 list endpoints.** All `findAll()` → map → bare array. Grows without bound. *Owner chose to keep bare arrays for now (frontend contract not yet firm); revisit before the Vue app couples hard. Add `Page<T>` envelopes + `?page/size/sort` when picked up.* |
| 6 | **High** | 🟠 BEFORE PROD | **`EntityResolver` loads whole tables and scores in Java.** `findAll()` + Levenshtein per row per call; `RuleBasedParser.bestTokenMatch()` invokes it per word and per adjacent word-pair. `resolver.order()` does `findAll()` on `orders`. The "datasets are small" comment is load-bearing and unenforced. |
| 7 | **High** | ✅ FIXED (indexes, `main`) / 🟠 (running balance) | ~~no index on `material_id`~~ Fixed: `@Index` `idx_msm_material_date` (`material_id, date`) + `idx_msm_party` added to `MaterialStockMovement` — covers `sumSignedQuantityByMaterialId`, `sumSignedQuantityGrouped`, `findByMaterialIdOrderByDateDesc`. Still recomputes Σ from full ledger on every read (no running balance / snapshot) — that's 🟠. Broader FK-index sweep is DB #7. |
| 8 | **Medium** | 🟠 BEFORE PROD | **`spring.jpa.open-in-view` left at default `true`** — a DB connection is pinned for the whole request including JSON serialization ⇒ pool starvation. Spring logs the warning at startup. |
| 9 | **Medium** | 🟠 BEFORE PROD | **No read-only transactions.** `readOnly=0` in every one of the 21 impls. Loses flush-mode and replica-routing optimizations. |
| 10 | **Medium** | 🟠 BEFORE PROD | **No connection-pool or timeout tuning.** Default Hikari (10), no statement timeout, no `default-timeout`. One unpaginated `findAll()` pins a connection. *A statement timeout is cheap insurance now.* |
| 11 | **Medium** | 🟠 BEFORE PROD | **No caching layer.** Redis present but used only for WA conversations; master data, BOM, and resolver lookups are all uncached and read-dominated. |
| 12 | **Low** | ✅ FIXED (`main`) | ~~`modelmapper` bean registered but mapping is hand-written throughout — dead weight.~~ Fixed: `ModelMapper` bean + `config/AppConfig.java` removed; `org.modelmapper` dependency dropped from `pom.xml`. |

---

## 3. System Design

### Pros
- Genuinely consistent layering across 195 files: Controller → Service interface → `*Impl` → Repository, DTOs at the boundary. No entity ever leaves a service. That consistency is real and worth keeping.
- Fulfillment modelled as an **append-only event ledger** (`OrderFulfillmentEvent`) with stage-ceiling validation, not a status column — the right model for partial completion/dispatch/settlement/returns.
- `Party` with a `Set<PartyRole>` instead of Customer/Supplier/Manufacturer tables — a correct generalization, and the migration away from the old `Customer`/`Supplier`/`PurchaseOrder`/`SalesOrder` entities was carried through cleanly (no orphans left).
- **WhatsApp ingress reuses the REST service layer** (`QuickEntryExecutor` → `OrderService`/`PartyService`/…) rather than duplicating business rules — the single most important structural decision in the codebase, and it's the right one.
- Quick-entry pipeline is well factored into single-responsibility pieces: `RuleBasedParser` → `EntityResolver` → `RequiredFields` → `FieldPrompts` → `FieldInterpreter` → `QuickEntryExecutor`, with `WaConversationStore` behind an interface (there's an `InMemoryWaConversationStore` test double).
- `RequiredFields` is explicitly documented as the single source of truth for both the REST parse and the WA loop — and it actually is.
- Typed config via `@ConfigurationProperties` + `@ConfigurationPropertiesScan`.

### Cons

| # | Sev | When | Finding |
|---|-----|------|---------|
| 1 | **High** | ✅ DECISION MADE: **MULTI-TENANT** (owner, 2026-09-10) / 🟠 (build) | **Multi-tenancy is modelled but not enforced.** `company_id` nullable on Party/Material/Broker/Resource/Product, `companyId` arrives in the request body, no query filters by company; `OrderServiceImpl.create` binds any `partyId` to any `productId` across tenants. **Direction chosen: enforce real multi-tenancy.** Build (🟠 BEFORE PROD, sequenced with Flyway DB #1): (1) carry `companyId` on `UserPrincipal` + a `type=access` JWT claim; (2) a `TenantContext` (request-scoped / `ThreadLocal`) populated by `JwtAuthenticationFilter`; (3) drop `companyId` from every request DTO — derive it server-side; (4) Hibernate `@Filter` / `@TenantId` (Hibernate 6) or a mandatory `where company_id = ?` on every repository; (5) `company_id NOT NULL` + FK on all tenant-scoped tables (Flyway); (6) cross-tenant reference guard in `OrderServiceImpl.create` and the WhatsApp `QuickEntryExecutor`. |
| 2 | **High** | 🟠 BEFORE PROD *(design now)* | **Three parallel stock models with three consistency semantics.** (a) `Inventory` — mutable balance, `@Version`. (b) `MaterialStockMovement` — event-sourced, SUM-on-read, no balance row, no `@Version`. (c) `Order` fulfillment events — touch **neither**; dispatching never decrements inventory or consumes material, only emits advisory strings. "How much do we have?" has three answers. |
| 3 🆕 | **Medium/High** | ✅ FIXED (`main`) | ~~Soft delete exists for one entity and is never honoured; `getAll()` / `EntityResolver.product()` do `findAll()`.~~ Fixed: `ProductRepository.findByActiveTrue()`; `ProductServiceImpl.getAll()` and `EntityResolver` (`product()` + `candidates`) use it — soft-deleted products vanish from lists and WhatsApp resolution. `getById` still returns them. |
| 4 🆕 | **Medium** | ✅ FIXED (app-level, `main`) | ~~`Party`/`Broker`/`Transporter` etc. have no duplicate detection.~~ Fixed: `existsByNameIgnoreCase` / `…AndIdNot` guards in `create()`/`update()` for Party + Broker/Area/Design/Job/Schedule/Transporter/Vehicle → `DuplicateResourceException`. DB unique constraint still pending (DB #8); TOCTOU race until then, same as existing `Material.existsByCode`. |
| 5 | **Medium** | 🟠 BEFORE PROD | **`open-in-view=true` is load-bearing.** With no read-only transactions and lazy access inside `toResponse`, turning OIV off would throw `LazyInitializationException` across the app. |
| 6 | **Medium** | 🟠 BEFORE PROD | **No domain events / outbox / async seam.** Nothing to hang cross-cutting reactions on; every new concern becomes more inline service code. |
| 7 🆕 | **Low/Med** | ✅ FIXED (`main`) | ~~Three separately-configured `ObjectMapper`s.~~ Fixed: `RedisConfig` takes the shared `ObjectMapper` bean; `QuickEntryExecutor` derives its mapper via `objectMapper.copy()` (inherits modules/dates/naming) and overrides only `FAIL_ON_UNKNOWN_PROPERTIES=false` (the `fields` map legitimately carries non-DTO keys). No hand-rolled `new ObjectMapper()` left. |
| 8 | **Low/Med** | ⏸️ DEFERRED (owner, 2026-09-10) | **No API versioning** — everything under `/api/**`, no `/v1`. *Deferred with pagination (Scale #5): re-prefixing every route breaks the current Vue contract; revisit while the frontend is still fluid.* |
| 9 | **Low** | 🟠 BEFORE PROD | **No JPA auditing.** Only `Product` has `updatedAt`/`@PreUpdate`. `@EnableJpaAuditing` + a `BaseEntity` would add `updatedAt`/`createdBy` uniformly. *Adding the columns before there's data avoids a backfill.* |
| 10 | **Low** | ✅ FIXED (`main`) | ~~`Material.categoryId`, `Party.scheduleId/brokerId/areaId` are raw `Long`s — no FK.~~ Fixed: converted to `@ManyToOne` (`Material.category`, `Party.schedule/broker/area`) with `@JoinColumn` FKs + indexes; services resolve the id → entity (404 if missing). Request/response DTOs keep the `*Id` fields (no contract change). FKs are in the Liquibase baseline (DB #1). |

---

## 4. API Architecture

### Pros
- Uniform, predictable REST shape across 23 controllers: plural nouns, `POST`→201, `PUT`→200, `DELETE`→204, `GET`→200. A frontend dev can guess any endpoint.
- Sub-resource modelled properly: `POST /api/orders/{id}/fulfillment-events`.
- Separate request/response DTOs per entity — no entity leakage, no over-posting of `id`/`createdAt`/`active`.
- `@Valid` on every write endpoint; `MethodArgumentNotValidException` aggregated into `ApiError.details` — good error ergonomics where constraints exist.
- `@AuthenticationPrincipal UserPrincipal` rather than re-parsing the token in controllers.
- Purpose-built read endpoints (`/api/orders/material-requirement`, `/api/inventory/product/{id}`, `/api/inventory/movements/product/{id}`).
- `@SecurityRequirements` on `AuthController` correctly marks it unauthenticated in the OpenAPI doc.

### Cons

| # | Sev | When | Finding |
|---|-----|------|---------|
| 1 | **High** | ⏸️ DEFERRED (owner, 2026-09-10) | **`PUT` full-replaces with no null-guarding ⇒ silent field wipe.** `PUT /api/brokers/1 {"name":"X"}` nulls every other column. No `PATCH`. *Deferred with the other contract decisions (pagination, /v1): PATCH-vs-strict-PUT changes the write contract the Vue app is being built against; decide as a set.* |
| 2 | **High** | 🟠 (scale) / 🔴 (contract) | **No pagination / filtering / sorting contract** on any collection endpoint. *See Scale #5.* |
| 3 🆕 | **High** | ✅ MOSTLY FIXED (`main`) | ~~Validation coverage collapsed in the master-data code.~~ Fixed: `@Size` (all string fields, matching column lengths), `@Email`, `@DecimalMin/@DecimalMax` (brokeragePct/gstRate/tdsPct 0–100), `@PositiveOrZero` (creditLimit*, rates, charges) across Broker/Area/Design/Job/Schedule/Transporter/Vehicle/Material/Party. **Still open:** format `@Pattern` for `gstin`/`pan`/`pinCode`/phone, and a pass over the older non-master DTOs (Company/Warehouse/Resource/…). |
| 4 | **Medium** | ⏸️ DEFERRED (owner, 2026-09-10) | **List endpoints return a bare JSON array** — no envelope. *Deferred with pagination (Scale #5).* |
| 5 🆕 | **Medium** | ✅ FIXED (`main`) | ~~`DataIntegrityViolationException` mapped to one hardcoded message.~~ Fixed: `GlobalExceptionHandler.handleDataIntegrity` now inspects the root-cause message — unique/duplicate → 409 "already exists", NOT NULL → 400 "required field missing", FK → 409 "still referenced", length overflow → 400 "value too long"; unclassified is logged + generic 409. |
| 6 | **Medium** | 🟠 BEFORE PROD | **No idempotency keys.** Neither `POST /api/orders` nor the webhook supports `Idempotency-Key`; a double-tap or a Meta redelivery creates a duplicate order. *Webhook message-id dedup itself is 🔴 — see BG #5.* |
| 7 🆕 | **Medium** | ✅ FIXED (`main`) | ~~`ProductComponentService` has no `update`.~~ Fixed: `PUT /api/product-components/{id}` → `update(id, request)` (re-resolves product/material, keeps row id, atomic). Also enforces the new `(product_id, slot_no)` uniqueness (DB #6). |
| 8 | **Medium** | ✅ FIXED (`main`) | ~~Unhandled exception types fall through to 500.~~ Fixed: `@ExceptionHandler`s added for `HttpMessageNotReadableException`, `ConstraintViolationException`, `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException` → all 400 with a useful message. |
| 9 | **Low** | ✅ FIXED (`main`) | ~~No `Location` header on 201; no ETags; OpenAPI has no error-response docs.~~ Fixed centrally, zero controller churn: `CreatedLocationAdvice` (`ResponseBodyAdvice`) sets `Location: <path>/<id>` on every 201 with an `id`; `ShallowEtagHeaderFilter` on `/api/*` → `ETag` + `304` on unchanged GETs (verified); `OpenApiConfig` customizer documents the shared `ApiError` shape as 400/401/403/404/409 on every operation. Per-method `@Operation` summaries still optional. |

---

## 5. Database Design

### Pros
- `BigDecimal` with deliberate `precision`/`scale` throughout — quantities 14,4; money 12,2; `gstRate` 6,2. No floats anywhere near money. This is done properly.
- `@Enumerated(EnumType.STRING)` on every enum — safe against reordering.
- Real DB constraints where they were thought about: `orders.orderNumber` unique, `products.sku` unique, `users` unique username+email, `warehouses.name` unique, `categories.name` unique, `inventory` unique `(product_id, warehouse_id)`, `whatsapp_user_links.phone` unique, `roles.name` unique.
- `Inventory` carries `@Version` — real optimistic locking on the one mutable-balance table.
- `@ManyToOne(fetch = LAZY)` is the default everywhere; the two EAGER associations (`User.roles`, `WhatsappUserLink.user`) are deliberate.
- Postgres `jsonb` (not text/json) via `@JdbcTypeCode(SqlTypes.JSON)` for the flexible attribute bags.
- Mandatory relations are `nullable = false` at both the JPA and column level.

### Cons

| # | Sev | When | Finding |
|---|-----|------|---------|
| 1 | **High** | ✅ FIXED (`main`) — **Liquibase** | ~~No migration tool.~~ Adopted **Liquibase**. `db/changelog/db.changelog-master.yaml` → `001-baseline.sql` (full current schema, generated from Hibernate metadata — 26 tables, all FKs/indexes/`version … DEFAULT 0 NOT NULL`; `MARK_RAN` when `orders` already exists), `002-drop-legacy-tables.sql` (drops the old Customer/Supplier/PO/SO tables `ddl-auto:update` left behind), `003-version-not-null.sql` (reconciles `version` columns on drifted DBs). `ddl-auto: validate` in every profile — Liquibase owns the schema, Hibernate only cross-checks. Verified: runs clean on the existing dev DB (baseline MARK_RAN, 002+003 EXECUTED, `validate` passes) **and** the baseline DDL executes fresh (26 tables in a temp schema). Further schema changes (DB #4/#8 composite constraints, multi-tenancy) now land as ordered changesets. |
| 2 | **High** | ✅ FIXED (`main`) | ~~`orderNumber = "ORD-" + Instant.now().toEpochMilli()` — same-millisecond collisions, `existsByOrderNumber` never called.~~ Fixed: `generateOrderNumber()` now appends a random 4-digit suffix and loops on `existsByOrderNumber` (10 attempts); the unique index is still the final backstop. A DB sequence remains the ideal — do it with Flyway (#1). |
| 3 | **High** | ✅ FIXED (columns, `main`) / 🟠 (retry logic) | ~~`@Version` only on `Inventory`.~~ Fixed: `@Version Long version` (`@ColumnDefault("0")`, `nullable = false`) on Order, MaterialStockMovement, Party, Material, Product + 7 masters. Liquibase baseline declares `version bigint DEFAULT 0 NOT NULL`; changeset `003` backfilled the pre-existing NULLs (were 43) and set the constraint on drifted DBs (the `LegacyVersionBackfill` runner it replaced is deleted). `OptimisticLockingFailureException` → 409. Retry logic + row lock (DB #4) still 🟠. |
| 4 | **High** | 🟠 BEFORE PROD | **The fulfillment ceiling invariant is unenforceable as written.** `validateFulfillmentEvent` sums `order.getFulfillmentEvents()` in memory, no row lock, no `@Version` on `Order`. Two concurrent `DISPATCHED` events both pass. `service/impl/OrderServiceImpl.java:129-149`. *A single tester won't hit it; fix with #3's `@Version`.* |
| 5 🆕 | **High** | ✅ FIXED (`main`) | ~~`materials.code` has no unique constraint, and `update()` doesn't re-check it.~~ Fixed: `Material.code` is now `@Column(unique = true)`; `update()` calls `existsByCodeAndIdNot`. (If multi-tenancy makes this `(company_id, code)`, that's a Flyway migration.) |
| 6 🆕 | **High** | ✅ FIXED (`main`) | ~~`product_components` has no unique on `(product_id, slot_no)`.~~ Fixed: `@UniqueConstraint uk_product_component_slot (product_id, slot_no)` + `existsByProductIdAndSlotNoIgnoreCase(…AndIdNot)` guards in create/update. Stops `buildRequirement()` double-counting. |
| 7 | **Medium** | ✅ FIXED (`main`) | ~~No indexes declared anywhere.~~ Fixed: `@Index` on `material_stock_movements (material_id, date)` + `party_id`, `orders (party_id, product_id)`, `order_fulfillment_events (order_id)`, `products (category_id, company_id)`, `materials (company_id, category_id)`, `parties (company_id, schedule_id, broker_id, area_id)`. `inventory (product_id, warehouse_id)` and `whatsapp_user_links.phone` were already unique-indexed. **All captured in the Liquibase baseline (DB #1)** so they land on every environment. |
| 8 | **Medium** | ⏸️ PARTLY / DEFERRED | **Under-constrained columns.** Done: `Broker.name` + `Area.name` now `NOT NULL` (the other 6 masters already were); app-level dup-name guards on all 8 (Design #4). **Deferred:** the uniqueness constraints — with multi-tenancy chosen (Design #1) these must be `(company_id, name)` / `(company_id, code)`, so they land with the multi-tenant Flyway migration, not as throwaway global constraints now. |
| 9 | **Medium** | 🟠 BEFORE PROD | **Hard delete on 12 of 13 entities** (`Product` excepted), including `Party`, `Material`, `Broker` — all referenced by historical orders/movements. FK error surfaced as a misleading 409 (API #5), or destroyed history. `Party.closed` / `Broker.blacklisted` exist and are ignored by `delete()`. |
| 10 | **Medium** | ✅ FIXED (`main`) | ~~Profile/engine mismatch — `jsonb` DDL but `application-prod.yml` targets MySQL.~~ Fixed: **engine = PostgreSQL** for all profiles. `application-prod.yml` now uses the Postgres URL/driver/`PostgreSQLDialect`; `mysql-connector-j` removed from `pom.xml`. (H2 driver still present — that's Sec #9, 🟠.) |
| 11 | **Low/Med** | 🟠 BEFORE PROD | `created_by` / `performed_by` FKs nullable with no `ON DELETE` policy — deleting a referenced `User` breaks or orphans audit history. |
| 12 | **Low** | 🟠 BEFORE PROD | `LocalDateTime.now()` (server-local) for all audit timestamps rather than `Instant`/`timestamptz`. *Change before there's data to migrate.* |
| 13 | **Low** | 🟢 BACKLOG | `jsonb` bags have no schema, no GIN index, no size cap. |

---

## 6. Background Processing — "nothing slow should block the HTTP request"

**Confirmed by exhaustive grep: zero `@Async`, `@EnableAsync`, `@Scheduled`, `@EnableScheduling`, `TaskExecutor`, `CompletableFuture`, `ExecutorService`, `ApplicationEventPublisher`, `@EventListener`, `@Retryable`, message broker, or outbox in the entire codebase.** Everything runs on the Tomcat request thread inside the request transaction.

### Pros
- `POST /api/quick-entry/parse` is parse-only and returns a draft; the frontend then submits through normal create endpoints — a clean split that keeps the interactive path cheap.
- WA conversation state in Redis with a TTL means the common "ask the next question" turn is genuinely short.
- The webhook always answers 200 and `handleInbound` never throws, so a Meta or app error doesn't trigger a retry storm.
- `WhatsAppClient` degrades to a log line when disabled or uncredentialed — the whole conversation flow runs locally with no Meta setup. Good for dev velocity.
- `WaConversationStore` is an interface with an in-memory test double — the seam for moving this off the request thread already exists.

### Cons

| # | Sev | When | Finding |
|---|-----|------|---------|
| 1 | **High** | 🟠 BEFORE PROD | **The webhook performs the entire workload synchronously before returning 200.** `receive()` → `handleInbound()` runs Redis I/O → `EntityResolver` full-table scans × every token → shortfall aggregates → a **DB write transaction** → **a blocking outbound HTTPS POST to Meta**. Meta expects a prompt 200 and retries on timeout. `controller/WhatsAppWebhookController.java:47-64`. *Latency only bites with real Meta + traffic; design the enqueue seam now.* |
| 2 | **High** | ✅ FIXED (`main`) | ~~`WhatsAppClient` has no connect/read timeout and builds a fresh `RestClient` per call.~~ Fixed: one `RestClient` built once in the constructor with `ClientHttpRequestFactorySettings` — 3s connect, 8s read. |
| 3 | **High** | 🟠 BEFORE PROD | **No async infrastructure exists** — nowhere to move slow work. The LLM fallback is scaffolded (`app.anthropic.*`, `QuickEntryServiceImpl:32` `TODO (step 6)`); wiring it in puts a multi-second Claude call on this same request thread. *Build the async seam before enabling the LLM path.* |
| 4 🆕 | **High** | ✅ FIXED (`main`) | ~~All but the first inbound message discarded (`.findFirst()`).~~ Fixed: `firstTextMessage()` → `textMessages()` (all, in order, each with Meta's message id); `receive()` loops over them, **each message in its own try/catch** so one bad message can't starve the rest of the batch (code-review follow-up). |
| 5 | **Medium/High** | ✅ FIXED (`main`) | ~~No webhook idempotency.~~ Fixed: `processed_webhook_messages` table + `WebhookIdempotencyService.tryClaim()` — **claims the id first in its own `REQUIRES_NEW` transaction** (concurrent redelivery loses the race and is skipped), then processes; on failure the claim is **released** so Meta can retry just that message. Crash-after-claim still loses one message — full durability is BG #7 (🟠). |
| 6 | **Medium** | 🟠 BEFORE PROD | **Concurrent messages from one phone race on the shared conversation.** Two threads read/mutate/save the same `WaConversation` — last write wins, steps skipped or entry executed twice. No per-phone lock. *A single tester is unlikely to trigger it.* |
| 7 | **Medium** | 🟠 BEFORE PROD | **No retry, no dead letter, no durable inbound record.** `handleInbound` catches `RuntimeException`, logs, replies "Something went wrong", message is gone — raw payload never persisted. |
| 8 | **Medium** | 🟠 BEFORE PROD | **Redis is a hard dependency with no degraded mode.** Redis down ⇒ every WhatsApp message fails. `spring.data.redis` also configures no password, SSL, or timeout for prod. |
| 9 | **Medium** | 🟠 BEFORE PROD | **No observability.** Actuator isn't a dependency — no health, readiness, Micrometer timers, tracing. *Adding Actuator now would help you debug, but it's not blocking test work.* |
| 10 🆕 | **Low** | ✅ FIXED (`main`) | ~~`interpretNumber` strips `-` before parsing; `-100` accepted as `100`.~~ Fixed: leading `-` is detected on the trimmed input and the parsed value negated, so the existing `signum()` guards now fire. |

---

## Testing (cross-cutting) 🆕

35 tests, and their distribution is inverted relative to risk:

| Area | Tests |
|---|---|
| `RuleBasedParser` | 12 |
| `WhatsAppQuickEntryService` | 7 |
| `WhatsAppWebhookController` | 6 |
| `QuickEntryExecutor` | 5 |
| `OrderServiceImpl` | 3 |
| `QuickEntryServiceImpl` | 2 |
| **Security / JWT / auth filter** | **0** |
| **`InventoryServiceImpl`** (the only optimistic locking in the system) | **0** |
| **`MaterialStockMovementServiceImpl`** (all stock math) | **0** |
| **All 20 CRUD services** | **0** |
| **`GlobalExceptionHandler`** | **0** |

The text-parsing heuristics — the most forgiving, least damaging code in the repo — carry the coverage. Auth and stock arithmetic, where a bug is unrecoverable, carry none.

---

## Prioritised remediation

Organised by **when**, not severity. Nothing here is a production incident — the system is in test.

### 🔴 NOW — while the system is in test

*Actual bugs corrupting test data, or fixes that get materially harder once prod data / a coupled frontend exist. Mostly small.*

**Foundational — ✅ both DONE (this unblocked the rest):**
1. ✅ **DONE** — DB engine = **Postgres**; `application-prod.yml` fixed, `mysql-connector-j` removed (DB #10).
2. ✅ **DONE** — Adopted **Liquibase**: `db/changelog/` baseline (`MARK_RAN` on existing DBs) + drift-cleanup changesets; `ddl-auto: validate` everywhere (DB #1). Verified on the dev DB and against a fresh schema.

**Schema / DB:**
3. ✅ **DONE (interim)** — `generateOrderNumber()` = epoch-millis + random suffix + `existsByOrderNumber` retry loop (DB #2). DB sequence is the Flyway-time ideal.
4. ✅ **DONE** — `@Version` columns on Order + MaterialStockMovement + Party/Material/Product + 7 masters (DB #3). Retry logic still 🟠.
5. ✅ **DONE** — Unique `materials.code` + `existsByCodeAndIdNot` re-check (DB #5); `@UniqueConstraint (product_id, slot_no)` on `product_components` (DB #6).
6. ✅ **DONE (indexes)** — FK indexes on `material_stock_movements`, `orders`, `order_fulfillment_events`, `products`, `materials`, `parties` — all captured in the Liquibase baseline (DB #7). `NOT NULL` on `Broker.name`/`Area.name` (DB #8). **Deferred:** composite `(company_id, …)` uniqueness → multi-tenant changeset.

**Correctness bugs:**
7. ✅ **DONE** — `WaWebhookPayload.textMessages()` iterates all messages, each in its own try/catch (BG #4).
8. ✅ **DONE** — Webhook idempotency: `WebhookIdempotencyService.tryClaim()` claims-first in a `REQUIRES_NEW` tx, releases on failure (BG #5).
9. ✅ **DONE** — Honour `active` in list + resolver queries (Design #3). `closed` on other masters is DB #9 (🟠).
10. ✅ **DONE (app-level)** — Duplicate-name detection on Party + 7 new masters (Design #4); composite DB constraint → multi-tenant migration.
11. ✅ **DONE** — `FieldInterpreter.interpretNumber` parses the sign before stripping (BG #10).
12. ✅ **DONE** — `ProductComponentService.update` added; `PUT /api/product-components/{id}` (API #7).

**Cheap hardening / correctness:**
13. ✅ **DONE** — Removed `roles` from `RegisterRequest`; hardcoded `ROLE_STAFF` (Sec #1).
14. ✅ **DONE** — Startup assertion that `JWT_SECRET` is set and ≥32 bytes; removed the committed fallback (Sec #2).
15. ✅ **DONE** — `@Profile("!prod")` on `DataSeeder`, password no longer logged (Sec #7).
16. ✅ **DONE** — Enforce the JWT `type` claim on filter + refresh (Sec #4, type-check only).
17. ✅ **DONE** — Connect (3s) + read (8s) timeouts on `WhatsAppClient`; one reused `RestClient` (BG #2).
18. ✅ **DONE (master DTOs)** — `@Size`/`@Email`/`@DecimalMin/Max`/`@PositiveOrZero` on the 7 new masters + Party/Material (Sec #11). Format `@Pattern` + older-DTO pass still open under API #3.
19. ✅ **DONE** — `DataIntegrityViolationException` split by root cause; added `HttpMessageNotReadable` / `ConstraintViolation` / `MissingServletRequestParameter` / `MethodArgumentTypeMismatch` → 400 handlers (API #5, #8).
20. ✅ **DONE** — Shared `ObjectMapper` used by `RedisConfig` + `QuickEntryExecutor`; ad-hoc instances dropped (Design #7).

**Contract decisions — cheap now, breaking later (do while the Vue app is still fluid):**
21. ⏸️ **DEFERRED (owner, 2026-09-10)** — Pagination: `Page<T>` envelope + `?page/size/sort` on every list endpoint (Scale #5, API #2, #4). Kept bare arrays for now; revisit before the frontend couples hard.
22. ⏸️ **DEFERRED (owner, 2026-09-10)** — `PATCH` with null-means-absent, or explicitly reject partial `PUT` (API #1). Bundled with the contract deferrals.
23. ⏸️ **DEFERRED (owner, 2026-09-10)** — `/api/v1` prefix (Design #8). Bundled with the pagination deferral.
24. ✅ **DECIDED: MULTI-TENANT** (owner, 2026-09-10) — Design #1. Implementation is 🟠 (sequenced with Flyway DB #1); see the Design #1 row for the build steps. New code from here on must derive `companyId` server-side, never from the request body.

### 🟠 BEFORE PROD — not before

*Only bites with real users, real traffic, real data volume, or public exposure.*

- `@EnableMethodSecurity` + `@PreAuthorize` role matrix, **and** populate `SecurityContextHolder` in `QuickEntryExecutor` (Sec #3, #10)
- Refresh-token rotation, Redis jti denylist, logout (Sec #4)
- Login lockout + rate-limit filter (Sec #5)
- Require `WHATSAPP_APP_SECRET` outside dev; never skip signature verification in prod (Sec #6)
- Lock CORS to real origins; Swagger + h2-console behind auth / dev profile (Sec #8, #9)
- Build the `Authentication` from JWT claims or cache `UserDetails` — stop the per-request user reload (Scale #1)
- Drop `onHand()` from `MaterialServiceImpl.toResponse`; drop `buildRequirement` from order **list** responses (Scale #2, #3)
- `default_batch_fetch_size` / make `Party.roles` LAZY (Scale #4)
- DB-side search for `EntityResolver` (`pg_trgm` GIN); cache master lists; never `findAll()` on `orders` (Scale #6)
- Running-balance table or period snapshots for material stock (Scale #7)
- Disable `open-in-view`; `@Transactional(readOnly = true)` on read paths (Scale #8, #9)
- Hikari tuning + statement timeout + `default-timeout` (Scale #10); caching layer (Scale #11)
- **Build multi-tenancy** (Design #1, decided 2026-09-10): tenant on `UserPrincipal`+JWT, `TenantContext`, drop `companyId` from DTOs, mandatory `company_id` filter on every query, `NOT NULL`+FK via Flyway, cross-tenant guards in `OrderServiceImpl`/`QuickEntryExecutor`
- Unify the stock model; order dispatch posts movements transactionally (Design #2)
- Domain events / outbox seam (Design #6)
- Webhook async offload + bounded worker (BG #1, #3); per-phone conversation lock (BG #6); retry/DLQ + durable inbound record (BG #7); Redis degraded mode + auth/SSL (BG #8)
- `@Version` optimistic-retry handling + row lock around `validateFulfillmentEvent` (DB #3, #4)
- Soft-delete the referenced masters; honour `blacklisted` (DB #9)
- `ON DELETE` policy for `created_by`/`performed_by` (DB #11); `Instant`/`timestamptz` audit columns (DB #12)
- `@EnableJpaAuditing` + `BaseEntity` (Design #9)
- Actuator + Micrometer; health/readiness/metrics (BG #9)
- Tests for auth, `InventoryServiceImpl` concurrency, and stock arithmetic

### 🟢 BACKLOG

- ✅ **DONE** — Dropped `modelmapper` (Scale #12)
- ✅ **DONE** — Raw-`Long` id fields → real `@ManyToOne` FK associations (Design #10)
- ✅ **DONE** — `Location` header on 201 (advice); ETags/304 on GETs (filter); `ApiError` error responses on every OpenAPI operation (API #9)
- ✅ **DONE** — Fixed the user-enumeration message differences (Sec #12)
- `jsonb` schema/GIN/size caps (DB #13) — still open
