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
| 1 | **Critical** | 🔴 NOW | **Anonymous privilege escalation.** `/api/auth/**` is `permitAll`; `RegisterRequest.roles` flows into `resolveRoles()` → `getOrCreateRole()` unchecked. `POST /api/auth/register {"roles":["ROLE_ADMIN"]}` mints an admin token for an anonymous caller. `service/impl/AuthServiceImpl.java:94-110`. *5-line change; the frontend is being built against the wrong assumption.* |
| 2 | **Critical** | 🔴 NOW | **Committed JWT signing key as a silent fallback.** `${JWT_SECRET:404E63…6B59}` in `application.yml:27`. `application-prod.yml` never sets `JWT_SECRET` and nothing validates it at boot — a missing env var boots with a repo-public key. *Add a startup assertion now; secret rotation itself is 🟠.* |
| 3 | **High** | 🟠 BEFORE PROD | **Roles are decorative — there is no authorization layer.** No `@EnableMethodSecurity`, no `@PreAuthorize`, no `@Secured`, no `hasRole()` matcher anywhere in 195 files. The chain is `.anyRequest().authenticated()`. `ROLE_STAFF` can delete parties, adjust stock, wipe masters. *Scaffold `@EnableMethodSecurity` now; full role matrix before prod.* |
| 4 | **High** | 🔴 NOW (type check) / 🟠 (rotation) | **Access and refresh tokens are interchangeable.** `JwtAuthenticationFilter` never inspects the `type` claim, and `AuthServiceImpl.refresh()` doesn't require `type=refresh`. A 7-day refresh token authenticates every endpoint. *`type` enforcement is a cheap correctness fix now; rotation / jti denylist / logout is 🟠.* |
| 5 | **High** | 🟠 BEFORE PROD | **No brute-force protection anywhere.** `UserPrincipal.isAccountNonLocked()` hardcoded `true`; no attempt counter, no lockout, no rate-limit filter on login, the webhook, or anything else. |
| 6 | **High** | 🟠 BEFORE PROD | **Webhook signature check fails open.** `signatureValid()` returns `true` when `app-secret` is blank — the default, with no profile guard. Deploy without `WHATSAPP_APP_SECRET` and any caller can POST a forged Meta payload; `QuickEntryExecutor` then creates orders/parties **as the mapped ERP user**. `controller/WhatsAppWebhookController.java:66-70`. *Signature verification already works when configured; add the non-dev "must be set" guard before prod.* |
| 7 | **Medium** | 🔴 NOW | **`DataSeeder` plants `admin`/`Admin@123` in every profile** and logs the password at INFO. No `@Profile` guard. `config/DataSeeder.java:29-46`. *One annotation.* |
| 8 | **Medium** | 🟠 BEFORE PROD | **CORS `allowedOriginPatterns("*")` + `allowCredentials(true)`.** Spring permits this pairing via patterns, so origin isolation is effectively off. `config/SecurityConfig.java:83-92`. |
| 9 | **Medium** | 🟠 BEFORE PROD | Swagger (`/v3/api-docs/**`, `/swagger-ui/**`) and `/h2-console/**` are in `PUBLIC_ENDPOINTS`, and the H2 driver ships in every profile. |
| 10 🆕 | **Medium** | 🟠 BEFORE PROD | **`QuickEntryExecutor` never populates `SecurityContextHolder`.** It constructs `new UserPrincipal(link.getUser())` and calls services directly. Any `@PreAuthorize` added later (fix #3) will silently **not apply** to the entire WhatsApp ingress path. *Fix together with #3.* |
| 11 🆕 | **Low/Med** | 🔴 NOW | **Unbounded string fields reach the DB unvalidated.** No `@Size` on any of the new master DTOs, but entity columns are bounded (`Material.code` 80, `Broker.address1` 255, `Party.gstin` 20). *Part of the validation-restore in API #3; cheap, and keeps test data clean.* |
| 12 | **Low** | 🟢 BACKLOG | User enumeration: `register` distinguishes "Username is already taken" from "Email is already registered"; `CustomUserDetailsService` echoes the username in `UsernameNotFoundException`. |

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
| 5 | **High** | 🟠 (scale) / 🔴 (contract) | **No pagination on any of ~24 list endpoints.** All `findAll()` → map → bare array. Grows without bound. *If the Vue contract is still fluid, add `Page<T>` envelopes NOW — it's a breaking response-shape change once the frontend couples to bare arrays.* |
| 6 | **High** | 🟠 BEFORE PROD | **`EntityResolver` loads whole tables and scores in Java.** `findAll()` + Levenshtein per row per call; `RuleBasedParser.bestTokenMatch()` invokes it per word and per adjacent word-pair. `resolver.order()` does `findAll()` on `orders`. The "datasets are small" comment is load-bearing and unenforced. |
| 7 | **High** | 🟠 (running balance) / 🔴 (indexes) | **On-hand stock is recomputed from full ledger history on every read** — no running balance, no snapshot, no index on `material_id`. *Indexes are cheap and matter once you seed realistic volume; the balance table can wait.* |
| 8 | **Medium** | 🟠 BEFORE PROD | **`spring.jpa.open-in-view` left at default `true`** — a DB connection is pinned for the whole request including JSON serialization ⇒ pool starvation. Spring logs the warning at startup. |
| 9 | **Medium** | 🟠 BEFORE PROD | **No read-only transactions.** `readOnly=0` in every one of the 21 impls. Loses flush-mode and replica-routing optimizations. |
| 10 | **Medium** | 🟠 BEFORE PROD | **No connection-pool or timeout tuning.** Default Hikari (10), no statement timeout, no `default-timeout`. One unpaginated `findAll()` pins a connection. *A statement timeout is cheap insurance now.* |
| 11 | **Medium** | 🟠 BEFORE PROD | **No caching layer.** Redis present but used only for WA conversations; master data, BOM, and resolver lookups are all uncached and read-dominated. |
| 12 | **Low** | 🟢 BACKLOG | `modelmapper` bean registered but mapping is hand-written throughout — dead weight. |

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
| 1 | **High** | 🔴 NOW (decision) / 🟠 (build) | **Multi-tenancy is modelled but not enforced.** `Company` is documented "Tenant root", yet `company_id` is nullable on Party/Material/Broker/Resource/Product, `companyId` arrives **in the request body**, and no query filters by company. `OrderServiceImpl.create` binds any `partyId` to any `productId` across tenants. *The single most expensive thing to retrofit — decide single- vs multi-tenant now, before more code assumes the current shape.* |
| 2 | **High** | 🟠 BEFORE PROD *(design now)* | **Three parallel stock models with three consistency semantics.** (a) `Inventory` — mutable balance, `@Version`. (b) `MaterialStockMovement` — event-sourced, SUM-on-read, no balance row, no `@Version`. (c) `Order` fulfillment events — touch **neither**; dispatching never decrements inventory or consumes material, only emits advisory strings. "How much do we have?" has three answers. |
| 3 🆕 | **Medium/High** | 🔴 NOW | **Soft delete exists for exactly one entity and is never honoured.** `ProductServiceImpl.delete()` sets `active=false` — but `getAll()` has no `active` filter and `EntityResolver.product()` does `findAll()`, so "deleted" products still appear in lists and still resolve in WhatsApp quick entry. `service/impl/ProductServiceImpl.java:82-87`. *Functional bug today.* |
| 4 🆕 | **Medium** | 🔴 NOW | **`Party`/`Broker`/`Transporter` etc. have no duplicate detection** — unlike `Material`/`Product`/`Category`/`WhatsappUserLink`. Duplicate parties make `EntityResolver.best()` return `Optional.empty()`, so WhatsApp quick entry silently stops resolving that customer. *Adding the checks + constraints after dupes accumulate in prod is a cleanup migration.* |
| 5 | **Medium** | 🟠 BEFORE PROD | **`open-in-view=true` is load-bearing.** With no read-only transactions and lazy access inside `toResponse`, turning OIV off would throw `LazyInitializationException` across the app. |
| 6 | **Medium** | 🟠 BEFORE PROD | **No domain events / outbox / async seam.** Nothing to hang cross-cutting reactions on; every new concern becomes more inline service code. |
| 7 🆕 | **Low/Med** | 🔴 NOW *(cheap)* | **Three separately-configured `ObjectMapper`s.** `QuickEntryExecutor` and `RedisConfig` each `new ObjectMapper()`; MVC uses Spring's. The executor's `FAIL_ON_UNKNOWN_PROPERTIES=false` means a typo'd field in a parsed quick-entry map is silently dropped. *Inject the shared bean.* |
| 8 | **Low/Med** | 🔴 NOW *(if feasible)* | **No API versioning** — everything under `/api/**`, no `/v1`. *Free to add now; a breaking change once external consumers lock in.* |
| 9 | **Low** | 🟠 BEFORE PROD | **No JPA auditing.** Only `Product` has `updatedAt`/`@PreUpdate`. `@EnableJpaAuditing` + a `BaseEntity` would add `updatedAt`/`createdBy` uniformly. *Adding the columns before there's data avoids a backfill.* |
| 10 | **Low** | 🟢 BACKLOG | `Material.categoryId`, `Party.scheduleId/brokerId/areaId` are raw `Long`s "resolved client-side" — no FK, orphan-prone, no join possible. |

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
| 1 | **High** | 🔴 NOW | **`PUT` full-replaces with no null-guarding ⇒ silent field wipe.** Every `update()` sets every column from the request, and the new master DTOs are almost entirely optional. `PUT /api/brokers/1 {"name":"X"}` nulls `phone1`, `email`, `brokeragePct`, all four address lines. No `PATCH` anywhere. *Contract decision — cheap to change while the frontend is fluid, real data loss otherwise.* |
| 2 | **High** | 🟠 (scale) / 🔴 (contract) | **No pagination / filtering / sorting contract** on any collection endpoint. *See Scale #5.* |
| 3 🆕 | **High** | 🔴 NOW | **Validation coverage collapsed in the newly-merged master-data code.** Constraints per field: `BrokerRequest` 1/14, `MaterialRequest` 1/12, `PartyRequest` 2/23 — vs the older `ProductRequest` 7/9, `OrderRequest` 5/7. `brokeragePct` accepts 9999, `creditLimitAmount` accepts negatives, `gstin`/`pan`/`pinCode`/phones/emails unvalidated, no `@Size` anywhere. *~½ day of annotations; keeps test data clean.* |
| 4 | **Medium** | 🔴 NOW *(with #2)* | **List endpoints return a bare JSON array** — no envelope, no total, no page metadata. Adding pagination later is a breaking response-shape change. |
| 5 🆕 | **Medium** | 🔴 NOW | **`DataIntegrityViolationException` is mapped to one hardcoded, usually-wrong message.** `GlobalExceptionHandler` returns *"the record is still referenced by other records"* (409) for **every** integrity violation — length overflow, NOT NULL, unique collisions alike. `exception/GlobalExceptionHandler.java:49-54`. *Wastes your debugging time now.* |
| 6 | **Medium** | 🟠 BEFORE PROD | **No idempotency keys.** Neither `POST /api/orders` nor the webhook supports `Idempotency-Key`; a double-tap or a Meta redelivery creates a duplicate order. *Webhook message-id dedup itself is 🔴 — see BG #5.* |
| 7 🆕 | **Medium** | 🔴 NOW | **`ProductComponentService` has no `update`** — only `create`, `getByProduct`, `delete`. Correcting a BOM line's `quantityPerUnit` (the input to all shortfall math) requires delete-then-recreate, not atomic, loses the row id. *Missing CRUD op, cheap.* |
| 8 | **Medium** | 🔴 NOW | **Unhandled exception types fall through to 500**: `HttpMessageNotReadableException` (malformed JSON), `ConstraintViolationException`, `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException` (`/api/orders/abc`). All should be 400. *Cheap; 500s masking 400s slow debugging now.* |
| 9 | **Low** | 🟢 BACKLOG | No `Location` header on 201; no ETags/caching on master-data GETs; OpenAPI has no `@Operation`/`@ApiResponse` annotations. |

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
| 1 | **High** | 🔴 NOW | **No migration tool.** No Flyway/Liquibase, no SQL in the repo. Dev runs `ddl-auto: update` (drift-prone), prod runs `validate` against a schema in no version-controlled artifact. *Gets harder every day; baseline while the DB is empty. Several other 🔴 items below become Flyway migrations once this is in place.* |
| 2 | **High** | 🔴 NOW | **`orderNumber = "ORD-" + Instant.now().toEpochMilli()`.** Same-millisecond creates collide against the unique index → 409 with no retry. `existsByOrderNumber` exists and is never called. `service/impl/OrderServiceImpl.java:215-217`. *Bites the moment you script or bulk-create in test.* |
| 3 | **High** | 🔴 NOW (add column) / 🟠 (retry logic) | **`@Version` only on `Inventory`.** `Order`, `MaterialStockMovement`, `Party`, `Material`, `Product` and all 7 new masters have none — concurrent `PUT`s silently lost-update. *Add the column now (free while empty; migration + backfill later); the optimistic-retry handling is 🟠.* |
| 4 | **High** | 🟠 BEFORE PROD | **The fulfillment ceiling invariant is unenforceable as written.** `validateFulfillmentEvent` sums `order.getFulfillmentEvents()` in memory, no row lock, no `@Version` on `Order`. Two concurrent `DISPATCHED` events both pass. `service/impl/OrderServiceImpl.java:129-149`. *A single tester won't hit it; fix with #3's `@Version`.* |
| 5 🆕 | **High** | 🔴 NOW | **`materials.code` has no unique constraint, and `update()` doesn't re-check it.** `create()` calls `existsByCode` (TOCTOU, no DB backstop); `update()` skips it. `PUT /api/materials/2 {"code":"<material 1's code>"}` succeeds → `EntityResolver.material()` goes ambiguous → WhatsApp yarn-purchase silently stops resolving. `service/impl/MaterialServiceImpl.java:32-34, 51-70`. |
| 6 🆕 | **High** | 🔴 NOW | **`product_components` has no unique on `(product_id, slot_no)`.** Duplicate slot ⇒ `buildRequirement()` **double-counts** that slot's requirement and shortfall. Directly corrupts the numbers the whole feature exists to produce. `entity/ProductComponent.java`. |
| 7 | **Medium** | 🔴 NOW | **No indexes declared anywhere.** Postgres does not auto-index FK columns, so `findByMaterialIdOrderByDateDesc`, `findByProductIdAndWarehouseId`, `findByPhoneAndEnabledTrue`, `findByProductId`, and `SUM … WHERE material_id = ?` are all seq scans. *Cheap; query plans matter once you seed realistic volume. Becomes a Flyway migration (#1).* |
| 8 | **Medium** | 🔴 NOW | **Under-constrained columns.** `Broker.name`, `Design.*`, `Transporter.*`, `Vehicle.*`, `Area.*`, `Schedule.*` nullable with no uniqueness; `parties.name` no unique; no `(company_id, code)` / `(company_id, name)` composite uniqueness. *Constraints after dupes accumulate = cleanup migration.* |
| 9 | **Medium** | 🟠 BEFORE PROD | **Hard delete on 12 of 13 entities** (`Product` excepted), including `Party`, `Material`, `Broker` — all referenced by historical orders/movements. FK error surfaced as a misleading 409 (API #5), or destroyed history. `Party.closed` / `Broker.blacklisted` exist and are ignored by `delete()`. |
| 10 | **Medium** | 🔴 NOW | **Profile/engine mismatch.** `@Column(columnDefinition = "jsonb")` is Postgres DDL on 8 entities, but `application-prod.yml` targets **MySQL** with `MySQLDialect`. *Prod won't cleanly start — decide the engine now, before writing migrations (#1).* |
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
| 2 | **High** | 🔴 NOW | **`WhatsAppClient` has no connect or read timeout.** `restClientBuilder.baseUrl(...).build()` with framework defaults. A hung TCP connection to `graph.facebook.com` blocks a Tomcat worker **indefinitely**. Also builds a fresh `RestClient` per call. `client/WhatsAppClient.java:31-45`. *One config block; prevents your test instance hanging on a flaky network.* |
| 3 | **High** | 🟠 BEFORE PROD | **No async infrastructure exists** — nowhere to move slow work. The LLM fallback is scaffolded (`app.anthropic.*`, `QuickEntryServiceImpl:32` `TODO (step 6)`); wiring it in puts a multi-second Claude call on this same request thread. *Build the async seam before enabling the LLM path.* |
| 4 🆕 | **High** | 🔴 NOW | **All but the first inbound message in a webhook payload are silently discarded.** `WaWebhookPayload.firstTextMessage()` ends in `.findFirst()`, but Meta batches multiple messages into one POST. Messages 2..n are dropped and, because the handler returns 200, Meta never redelivers. `dto/whatsapp/WaWebhookPayload.java:22-36`. *`.findFirst()` → iterate; silent data loss today.* |
| 5 | **Medium/High** | 🔴 NOW | **No webhook idempotency.** Meta's message id is never read or deduplicated. A retry after a slow 200 re-runs `QuickEntryExecutor` and creates a second order with a fresh `orderNumber`. *A `processed_message(id)` check is cheap; duplicate orders happen in test too.* |
| 6 | **Medium** | 🟠 BEFORE PROD | **Concurrent messages from one phone race on the shared conversation.** Two threads read/mutate/save the same `WaConversation` — last write wins, steps skipped or entry executed twice. No per-phone lock. *A single tester is unlikely to trigger it.* |
| 7 | **Medium** | 🟠 BEFORE PROD | **No retry, no dead letter, no durable inbound record.** `handleInbound` catches `RuntimeException`, logs, replies "Something went wrong", message is gone — raw payload never persisted. |
| 8 | **Medium** | 🟠 BEFORE PROD | **Redis is a hard dependency with no degraded mode.** Redis down ⇒ every WhatsApp message fails. `spring.data.redis` also configures no password, SSL, or timeout for prod. |
| 9 | **Medium** | 🟠 BEFORE PROD | **No observability.** Actuator isn't a dependency — no health, readiness, Micrometer timers, tracing. *Adding Actuator now would help you debug, but it's not blocking test work.* |
| 10 🆕 | **Low** | 🔴 NOW *(2-line)* | `FieldInterpreter.interpretNumber` strips `-` before parsing, so the `value.signum() < 0` guard is dead code and `-100` is silently accepted as `100`. `service/whatsapp/FieldInterpreter.java:88-105`. |

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

**Do this batch first — it unblocks the rest:**
1. **Decide the DB engine** (Postgres, given `jsonb`) and fix `application-prod.yml` (DB #10).
2. **Adopt Flyway**, baseline V1 from the current model, `ddl-auto: none` everywhere (DB #1). Items 3–6 below then land as migrations on top.

**Schema / DB (become Flyway migrations):**
3. `orderNumber` from a DB sequence/allocator inside the create transaction (DB #2).
4. Add `@Version` columns to `Order` + all client-updatable entities (DB #3 — free now, backfill later).
5. Unique `materials.code` + re-check on update (DB #5); unique `(product_id, slot_no)` on `product_components` (DB #6 — silently corrupts shortfall math today).
6. Uniqueness + NOT NULL on the under-constrained master columns (DB #8); indexes on every FK and `(material_id, date)` (DB #7).

**Correctness bugs:**
7. `WaWebhookPayload` — iterate all messages, not `.findFirst()` (BG #4).
8. Webhook idempotency: `processed_message(id)` check before executing (BG #5).
9. Honour `active`/`closed` in list + resolver queries — deleted products currently reappear (Design #3).
10. Add duplicate detection on `Party`/`Broker`/`Transporter`/… (Design #4).
11. `FieldInterpreter.interpretNumber` — parse the sign before stripping (BG #10).
12. `ProductComponentService.update` — add the missing op (API #7).

**Cheap hardening / correctness:**
13. Remove `roles` from `RegisterRequest`; hardcode `ROLE_STAFF` (Sec #1).
14. Startup assertion that `JWT_SECRET` is set and ≥32 bytes; remove the committed fallback (Sec #2).
15. `@Profile("!prod")` on `DataSeeder` (Sec #7).
16. Enforce the JWT `type` claim on filter + refresh (Sec #4, type-check only).
17. Connect + read timeouts on `WhatsAppClient`; reuse one `RestClient` (BG #2).
18. Restore validation on the new master DTOs — `@Size`, `@Email`, `@DecimalMin/Max`, `@PositiveOrZero` (API #3, Sec #11).
19. Split `DataIntegrityViolationException` by cause; add the 4 missing 400-class exception handlers (API #5, #8).
20. Inject the shared `ObjectMapper`; drop the ad-hoc ones (Design #7).

**Contract decisions — cheap now, breaking later (do while the Vue app is still fluid):**
21. Pagination: `Page<T>` envelope + `?page/size/sort` on every list endpoint (Scale #5, API #2, #4).
22. `PATCH` with null-means-absent, or explicitly reject partial `PUT` (API #1).
23. `/api/v1` prefix (Design #8).
24. **Decide multi-tenant vs single-tenant** (Design #1) — implementation is 🟠, but the decision blocks new code.

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

- Drop `modelmapper` (Scale #12)
- Raw-`Long` id fields → real FK associations (Design #10)
- `Location` header on 201; ETags on master GETs; `@Operation`/`@ApiResponse` on the OpenAPI (API #9)
- Fix the user-enumeration message differences (Sec #12)
- `jsonb` schema/GIN/size caps (DB #13)
