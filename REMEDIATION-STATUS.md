# ERP Inventory Management — Remediation Status (Frontend + Backend)

Consolidated tracking of every finding from the two architecture reviews plus the
coordinated FE⇄BE work.

- **FE repo**: `ERP-Inventory-Management-Frontend` — source review: `frontend-review.md`
- **BE repo**: `ERP-Inventory-Management` — source review: `ERP-architecture-review.md`

Status key: **✅ done** · **🟡 partial** · **⏳ pending** · **➖ n/a / accepted**
Pending priority (from the BE review's own triage): **🟠 before prod** · **🟢 backlog**

Last updated after the multi-tenancy / pagination / PATCH session.

---

## 0. Coordinated FE⇄BE items — ✅ ALL DONE

| Item | Backend | Frontend | Verified |
|---|---|---|---|
| API versioning `/api/v1` | `defd30e` | `456641e` | login + every list call hits `/api/v1/*` → 200 |
| Multi-tenancy enforcement | `b0f9d26` | `c7ecc2c` | 2nd tenant sees 0 rows; cross-tenant id → 404; cross-ref → 404; body `companyId` ignored |
| Server-side pagination + search | `52d54d7` | `4e1438a` | `?page&size&sort&q` envelope; `q=yarn` → 2 rows server-side; inline edit intact |
| PATCH partial-update | `78b8e55` | `1493ebd` | `PATCH /brokers/5 {phone1}` merges, other fields preserved; invalid value → 400 |

---

# BACKEND — `ERP-Inventory-Management`

Totals: **33 done · 32 pending** (several partial). Every pending item is 🟠 or 🟢 — **no 🔴 remain.**

## 1. Security — 6 done / 6 pending

| # | Finding | Status | Notes |
|---|---|---|---|
| 1 | Anonymous privilege escalation (`RegisterRequest.roles` → admin) | ✅ done | `roles` removed, hardcoded `ROLE_STAFF` |
| 2 | Committed JWT signing key as silent fallback | ✅ done | fallback removed; `@PostConstruct` fails boot if `JWT_SECRET` <32 bytes |
| 3 | Roles decorative — no `@EnableMethodSecurity` / `@PreAuthorize` anywhere | ⏳ pending 🟠 | scaffold method security + full role matrix |
| 4 | Access & refresh tokens interchangeable | 🟡 partial | `type` claim enforced on filter + refresh ✅; **rotation / jti denylist / logout ⏳ 🟠** |
| 5 | No brute-force protection (no lockout, no login rate-limit) | ⏳ pending 🟠 | |
| 6 | Webhook signature check fails open when `app-secret` blank | ⏳ pending 🟠 | add "must be set outside dev" guard |
| 7 | `DataSeeder` plants `admin/Admin@123` in every profile, logs password | ✅ done | `@Profile("!prod")`, password not logged |
| 8 | CORS `allowedOriginPatterns("*")` + `allowCredentials(true)` | ⏳ pending 🟠 | **must fix before any cookie-based auth** |
| 9 | Swagger + `/h2-console` public; H2 driver in every profile | ⏳ pending 🟠 | H2 dep removed? no — still present |
| 10 | `QuickEntryExecutor` never populates `SecurityContextHolder` | ⏳ pending 🟠 | fix together with #3 |
| 11 | Unbounded string fields reach DB unvalidated | 🟡 partial | `@Size`/`@Email`/`@DecimalMin-Max` on master DTOs ✅; **`@Pattern` for gstin/pan/pinCode/phone ⏳** |
| 12 | User enumeration (distinct register / login messages) | ✅ done | single generic message |

## 2. Scalability — 3 done / 9 pending

| # | Finding | Status | Notes |
|---|---|---|---|
| 1 | Every authenticated request = user + roles DB round-trip | ⏳ pending 🟠 | build `Authentication` from JWT claims, or cache `UserDetails` |
| 2 | `GET /materials` N+1 (`onHand` per row) | 🟡 partial | pagination bounds it to page size; **drop `onHand` from list DTO ⏳ 🟠** |
| 3 | `GET /orders` N+1 storm (`buildRequirement` per row) | 🟡 partial | pagination bounds it; **drop `buildRequirement` from list DTO ⏳ 🟠** |
| 4 | `Party.roles` `@ElementCollection(EAGER)`, no `default_batch_fetch_size` | ⏳ pending 🟠 | one-line config |
| 5 | No pagination on ~24 list endpoints | ✅ done | `PageResponse<T>` + `?page&size&sort&q`; bare GET still returns array (back-compat) |
| 6 | `EntityResolver` loads whole tables, Levenshtein in Java | ⏳ pending 🟠 | DB-side search (`pg_trgm` GIN); never `findAll()` on `orders` |
| 7 | On-hand recomputed from full ledger; no FK indexes | 🟡 partial | indexes ✅; **running-balance table / snapshots ⏳ 🟠** |
| 8 | `spring.jpa.open-in-view` left at default `true` | ⏳ pending 🟠 | disable + add read-only tx (with #9) |
| 9 | No `@Transactional(readOnly = true)` on read paths | ⏳ pending 🟠 | |
| 10 | No Hikari / statement-timeout tuning | ⏳ pending 🟠 | statement timeout is cheap insurance |
| 11 | No caching layer (Redis used only for WA conversations) | ⏳ pending 🟠 | cache master lists / BOM / resolver |
| 12 | `modelmapper` bean registered, mapping hand-written | ✅ done | dependency + config removed |

## 3. System Design — 6 done / 4 pending

| # | Finding | Status | Notes |
|---|---|---|---|
| 1 | Multi-tenancy modelled but not enforced | ✅ done | JWT `companyId` claim + `TenantContext` + Hibernate `@Filter` + `findOneById` id lookups; services derive company server-side; cross-tenant refs → 404 |
| 2 | Three parallel stock models, three consistency semantics | ⏳ pending 🟠 | unify; order dispatch should post movements transactionally |
| 3 | Soft delete exists for one entity, never honoured | ✅ done | `active` honoured in list + resolver |
| 4 | No duplicate detection on Party/Broker/Transporter/… | 🟡 partial | app-level `existsByNameIgnoreCase` guards ✅; **DB composite `(company_id, name)` unique ⏳ — see DB #8** |
| 5 | `open-in-view=true` is load-bearing | ⏳ pending 🟠 | same work item as Scale #8/#9 |
| 6 | No domain events / outbox / async seam | ⏳ pending 🟠 | |
| 7 | Three separately-configured `ObjectMapper`s | ✅ done | shared bean injected everywhere |
| 8 | No API versioning (`/api` not `/api/v1`) | ✅ done | `WebConfig` path prefix |
| 9 | No JPA auditing (only `Product` has `updatedAt`) | ⏳ pending 🟠 | `@EnableJpaAuditing` + `BaseEntity` before there's data |
| 10 | Raw `Long` id fields ("resolved client-side") not FKs | ✅ done | converted to `@ManyToOne` + FK |

## 4. API Architecture — 7 done / 2 pending

| # | Finding | Status | Notes |
|---|---|---|---|
| 1 | `PUT` full-replaces → silent field wipe; no `PATCH` | ✅ done | `PATCH /{id}` on 14 masters via `PatchMerger` (merge onto current state + validate) |
| 2 | No pagination / filter / sort contract | ✅ done | see Scale #5 |
| 3 | Validation coverage collapsed in master-data DTOs | 🟡 partial | `@Size`/`@Email`/`@DecimalMin-Max`/`@PositiveOrZero` ✅; **format `@Pattern` + older DTOs (Company/Warehouse/Resource) ⏳** |
| 4 | List endpoints return bare array, no envelope | ✅ done | `PageResponse<T>` when paged |
| 5 | `DataIntegrityViolationException` → one hardcoded message | ✅ done | split by root cause (unique/NOT NULL/FK/length) |
| 6 | No idempotency keys on `POST /orders` or webhook | ⏳ pending 🟠 | webhook message-id dedup done (BG #5); `Idempotency-Key` header ⏳ |
| 7 | `ProductComponentService` has no `update` | ✅ done | `PUT /api/v1/product-components/{id}` |
| 8 | Unhandled exception types fall through to 500 | ✅ done | 400 handlers for malformed JSON / type mismatch / missing param / constraint violation |
| 9 | No `Location` header on 201; no ETags; no OpenAPI error docs | ✅ done | `CreatedLocationAdvice` + `ShallowEtagHeaderFilter` + `OpenApiConfig` |

## 5. Database Design — 7 done / 5 pending

| # | Finding | Status | Notes |
|---|---|---|---|
| 1 | No migration tool | ✅ done | Liquibase baseline + drift changesets + `004-users-company`; `ddl-auto: validate` |
| 2 | `orderNumber = "ORD-" + epochMilli` collides | 🟡 partial | random suffix + `existsByOrderNumber` retry loop ✅; **DB sequence 🟢** |
| 3 | `@Version` only on `Inventory` | 🟡 partial | `@Version` columns on all client-updatable entities ✅; **optimistic-retry handling + row lock ⏳ 🟠** |
| 4 | Fulfillment-ceiling invariant unenforceable (no row lock, no `@Version` on `Order`) | ⏳ pending 🟠 | fix with #3's `@Version` |
| 5 | `materials.code` not unique; `update()` skips the check | ✅ done | `@Column(unique=true)` + `existsByCodeAndIdNot` |
| 6 | `product_components` no unique `(product_id, slot_no)` | ✅ done | `@UniqueConstraint` + guards — stops shortfall double-count |
| 7 | No indexes declared anywhere | ✅ done | FK + `(material_id, date)` indexes in the Liquibase baseline |
| 8 | Under-constrained master columns | 🟡 partial | `Broker.name`/`Area.name` NOT NULL ✅; `company_id` NOT NULL on 11 tenant tables (migration `004`) ✅; **composite `(company_id, name)` / `(company_id, code)` uniqueness ⏳** |
| 9 | Hard delete on 12 of 13 entities (Party/Material/Broker referenced by history) | ⏳ pending 🟠 | soft-delete + honour `closed`/`blacklisted` |
| 10 | Profile/engine mismatch (`jsonb` DDL, prod targets MySQL) | ✅ done | PostgreSQL for all profiles; `mysql-connector-j` removed |
| 11 | `created_by`/`performed_by` FKs nullable, no `ON DELETE` policy | ⏳ pending 🟠 | |
| 12 | `LocalDateTime.now()` (server-local) for audit timestamps | ⏳ pending 🟠 | move to `Instant`/`timestamptz` before there's data |
| 13 | `jsonb` bags: no schema, no GIN index, no size cap | ⏳ pending 🟢 | |

## 6. Background Processing — 4 done / 6 pending

| # | Finding | Status | Notes |
|---|---|---|---|
| 1 | Webhook does the whole workload synchronously before 200 | ⏳ pending 🟠 | design the enqueue seam |
| 2 | `WhatsAppClient` no connect/read timeout; fresh `RestClient` per call | ✅ done | 3s connect / 8s read, one reused client |
| 3 | No async infrastructure — nowhere to move slow work (LLM fallback) | ⏳ pending 🟠 | build before enabling the LLM path |
| 4 | Only the first inbound message per webhook payload is processed | ✅ done | iterate all messages, each in its own try/catch |
| 5 | No webhook idempotency | ✅ done | `processed_webhook_messages` + claim-first `REQUIRES_NEW` tx |
| 6 | Concurrent messages from one phone race on the shared conversation | ⏳ pending 🟠 | per-phone lock |
| 7 | No retry / dead-letter / durable inbound record | ⏳ pending 🟠 | |
| 8 | Redis is a hard dependency with no degraded mode; no prod auth/SSL/timeout | ⏳ pending 🟠 | |
| 9 | No observability — Actuator not a dependency, no Micrometer/tracing | ⏳ pending 🟠 | |
| 10 | `interpretNumber` strips `-` before parsing (`-100` accepted as `100`) | ✅ done | sign parsed before stripping |

## 7. Testing (cross-cutting) — ⏳ pending

35 tests, distribution inverted vs risk. **Zero tests** for: Security/JWT/auth filter, `InventoryServiceImpl` (the only optimistic locking), `MaterialStockMovementServiceImpl` (stock math), all 20 CRUD services, `GlobalExceptionHandler`, and the new multi-tenancy / pagination / PATCH paths. Add before prod.

---

# FRONTEND — `ERP-Inventory-Management-Frontend`

Totals: **~14 done · ~21 pending** (+ 5 bonus fixes done).

## 1. Security — 4 done / 3 pending

| Finding | Status | Notes |
|---|---|---|
| Access + refresh tokens in `localStorage` | ⏳ pending | needs BE httpOnly `Secure` `SameSite` refresh cookie; access token in memory only |
| Sentry Session Replay records PII (bank/PAN/GSTIN fields) | ✅ done | `maskAllText` + `maskAllInputs` + `blockAllMedia`, `sendDefaultPii:false`, breadcrumb bodies stripped, prod-only sampling |
| PostHog autocapture / session recording on | ✅ done | `autocapture:false`, `disable_session_recording:true`, `mask_all_text` |
| Refresh-token rotation desync (module state vs store) | ✅ done | `registerTokenRefreshHandler` persists the rotated pair |
| Client-only role gating; `to.meta.roles` set on zero routes (dead branch) | ⏳ pending | wire real `meta.roles` or delete the branch; **API must enforce authz** |
| No CSP / SRI / security headers | ⏳ pending | add at web server / CDN |
| Raw backend `details[]` surfaced in toasts | ⏳ pending | low — mitigated by BE splitting `DataIntegrityViolation` |
| Public `register()` with `roles` field | ✅ done | `roles` removed from `RegisterRequest` (FE + BE) |

## 2. Scalability — 2 done / 3 pending

| Finding | Status | Notes |
|---|---|---|
| No server-side pagination — every `list()` pulls the whole table | ✅ **done for the 13 `CrudPage` screens**; ⏳ **Orders / Inventory / MaterialMovements / Dashboard still fetch full lists** (endpoints support paging, screens not switched) |
| Client-side company scoping (downloads all tenants' data) | ✅ done | server scopes every response; client filter removed |
| Dashboard aggregates 3 full tables client-side | ⏳ pending | needs BE `/inventory/summary` + `/dashboard/stats` |
| Global search is client-side `contains` over full dataset | ✅ done for `CrudPage` (server `q`); ⏳ custom views |
| No table virtualization | ⏳ pending | mostly moot now with server paging |
| `quick-entry/parse` on every keystroke (450ms debounce) | ➖ accepted | monitor aggregate backend cost |

## 3. System Design — 1 done / 4 pending

| Finding | Status | Notes |
|---|---|---|
| Refresh token not persisted after rotation | ✅ done | see Security |
| Type safety bypassed in the generic grid (`as never`, `as unknown as Row`) | ⏳ pending | typed row/payload contract or runtime guards |
| `customFields.ts` is a frontend-owned JSONB whitelist | ⏳ pending | contract test against BE, or BE-served schema |
| No app-level error boundary | ⏳ pending | `onErrorCaptured` fallback UI at the layout |
| Residual hardcoded strings despite i18n (`GSTIN`, `Adjust stock`, most of `InventoryView`) | ⏳ pending | move to `en.json` |
| `en.json` only locale | ➖ accepted | wrapping already paid for |

## 4. API Architecture — 3 done / 2 pending

| Finding | Status | Notes |
|---|---|---|
| No pagination envelope handling | ✅ done | `PageResponse<T>` / `PageParams`, lazy DataTable |
| No API versioning in the client | ✅ done | `baseURL + /api/v1` |
| Inconsistent filter conventions | 🟡 partial | list filters standardised (`?page/size/sort/q`); sub-resource paths unchanged |
| 30s global axios timeout | ⏳ pending | per-call overrides for interactive GETs |
| No request cancellation for in-flight mutations / parse on route change | ⏳ pending | `AbortController` on the parse call + long mutations |
| `register()` returning a full session | ✅ done | BE hardened (new tenant, `ROLE_STAFF`) |

## 5. Database (inferred — same items as Backend §5) — 0 done / 5 pending

| Finding | Status |
|---|---|
| Bank / tax identifiers still in the JSONB `attributes` bag | ⏳ pending (BE — promote to typed columns) |
| Derived stock recomputed per read | ⏳ pending (BE — materialised balance) |
| Hard delete of records with financial history | ⏳ pending (BE — soft-delete) |
| `openingBalance` / `openingStockQty` treated as mutable? | ⏳ pending (BE — confirm immutable opening entries) |
| `attributes` / `variant` bags unbounded | ⏳ pending (BE) |

## 6. Background Processing — 2 done / 3 pending

| Finding | Status | Notes |
|---|---|---|
| Sentry + PostHog init on the boot critical path | ✅ done | PostHog deferred to `requestIdleCallback` |
| `saveAllDrafts` = N sequential POSTs | ✅ done | bounded-concurrency batches of 5 |
| Client-side aggregation on the main thread (dashboard, filters) | ⏳ pending | real fix = BE summary endpoints |
| `QRCode.toDataURL` on the main thread | ⏳ pending | low — fine for one modal |
| `pdfjs-dist` worker not configured | ⏳ pending | wire `GlobalWorkerOptions.workerSrc` when the PDF screen lands (unused today) |

## 7. Bonus frontend fixes (not in the original review) — ✅ done

| Fix | Commit |
|---|---|
| `CrudPage` no longer wipes un-listed columns on update (superseded by PATCH) | `84d8625` → `1493ebd` |
| `LoginView` handles 429 / `Retry-After` | `f2f5b36` |
| Client-side GSTIN / PAN format validation mirroring BE `@Pattern` | `f2f5b36` |
| `productComponentsService.update` added | `8dd81e2` |
| Refetch the list on 409 (stale copy after optimistic-lock / uniqueness clash) | `8dd81e2` |
| Sentry `tracePropagationTargets` links FE spans to BE traces | `8dd81e2` |
| `ProductsView` BOM inline-edit wired to `productComponentsService.update` | ⏳ **pending** (service ready, UI not wired) |

---

# Commit reference

### Backend (`main`, from `2e34d6c`)
| Commit | Scope |
|---|---|
| `08f83e5` | Review-remediation batch (Liquibase, `@Version`, JWT `type`, webhook idempotency, validation, exception mapping, timeouts, indexes, Postgres engine, …) |
| `defd30e` | `/api/v1` versioning |
| `b0f9d26` | Multi-tenancy enforcement |
| `52d54d7` | Server-side pagination + search |
| `78b8e55` | `PATCH /{id}` partial update |

### Frontend (`main`)
| Commit | Scope |
|---|---|
| `ad80d31` | Sentry/PostHog hardening, token-rotation fix, deferred analytics, batched saves |
| `8dd81e2` | Drop register `roles`, `productComponentsService.update`, refetch-on-409, trace propagation |
| `84d8625` | CrudPage partial-PUT wipe fix (interim) |
| `f2f5b36` | LoginView 429 handling, GSTIN/PAN patterns |
| `456641e` | API client → `/api/v1` |
| `c7ecc2c` | Multi-tenancy: drop `companyId` from payloads, remove company switcher |
| `4e1438a` | Server-driven pagination + search on master grids |
| `1493ebd` | Route grid updates through PATCH |

---

# Suggested next order of work

**Frontend (independent):**
1. Wire `ProductsView` BOM inline-edit to `productComponentsService.update`.
2. Switch Orders / Inventory / MaterialMovements grids to server pagination (endpoints ready).
3. App-level error boundary; move hardcoded strings to `en.json`.
4. Drop `as never` casts in `CrudPage` / `OrdersView` (typed row contract).

**Backend before prod (highest impact first):**
1. `@EnableMethodSecurity` + `@PreAuthorize` role matrix, and populate `SecurityContextHolder` in `QuickEntryExecutor`.
2. Stop the per-request user reload — build `Authentication` from JWT claims or cache it.
3. Drop `onHand()` / `buildRequirement()` from list DTOs; add `/inventory/summary` + `/dashboard/stats`.
4. Login lockout + rate-limit filter; require `WHATSAPP_APP_SECRET` outside dev; lock CORS to the real FE origin.
5. Refresh-token rotation + Redis jti denylist + `/logout`; then move the refresh token to an httpOnly cookie and switch the FE.
6. Disable `open-in-view`; add read-only transactions + Hikari/statement-timeout tuning.
7. Webhook async offload + per-phone lock + durable inbound record; Actuator + Micrometer.
8. Soft-delete the referenced masters; `@Version` optimistic-retry handling + row lock on fulfillment.
9. Tests for auth, stock arithmetic, and `InventoryServiceImpl` concurrency.
