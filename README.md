# Inventory Management System

ERP Inventory Management backend built with Spring Boot 3, Spring Security 6, and JWT.
Every API endpoint (except `/api/auth/**` and Swagger docs) requires a valid `Bearer` access token.

The schema is generalized for reuse across make-to-order businesses (not yarn/textile-specific) - see
[Multi-Industry Inventory Schema](#schema) below.

## Tech stack

- Java 17, Spring Boot 3.3
- Spring Web, Spring Data JPA, Spring Security
- JWT (jjwt)
- PostgreSQL (dev and prod profiles both point at Postgres; MySQL/H2 drivers are also on the classpath if you'd rather use those)
- springdoc-openapi (Swagger UI)
- Lombok, ModelMapper

## Running locally

Needs a running Postgres instance (`dev` profile defaults to `jdbc:postgresql://localhost:5432/inventory_db`,
user/password `inventory`/`inventory` — override with `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`).

```bash
mvn spring-boot:run
```

Swagger UI: `http://localhost:8080/swagger-ui.html`.

A default admin user is seeded on startup: `admin` / `Admin@123`.

To run against MySQL instead, activate the `prod` profile and set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Authentication flow

1. `POST /api/auth/register` — create a user (optionally with roles: `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_STAFF`).
2. `POST /api/auth/login` — returns `accessToken` and `refreshToken`.
3. Send `Authorization: Bearer <accessToken>` on every subsequent request.
4. `POST /api/auth/refresh` — exchange a valid refresh token for a new token pair.

## Modules

- **Companies** `/api/companies` — tenant root; scopes Party, Material, Resource, Product
- **Parties** `/api/parties` — customers, suppliers and manufacturers are all one entity with `roles`
- **Categories** `/api/categories`
- **Materials** `/api/materials` — raw materials/inputs (purchased and consumed)
- **Products** `/api/products` — finished goods (designed and sold)
- **Product Components** `/api/product-components` — bill-of-materials: which Material fills which slot of a Product
- **Resources** `/api/resources` — production equipment (machines), optionally referenced by fulfillment events
- **Warehouses** `/api/warehouses`
- **Inventory** `/api/inventory` — finished-product stock levels + adjustments, by warehouse
- **Material Stock Movements** `/api/material-stock-movements` — raw-material purchase/sale ledger (`direction: IN|OUT`)
- **Orders** `/api/orders` — finished-goods orders; fulfillment is a ledger of events (`/api/orders/{id}/fulfillment-events`), not a single status column — an order accumulates independent `COMPLETED` / `DISPATCHED` / `SETTLED` / `RETURNED` quantities over time

## Extensible fields (no migration needed)

`Company`, `Party`, `Material`, `Resource`, `Product`, `Order` (as `variant`), and `MaterialStockMovement`
each carry an `attributes`/`variant` column (`jsonb` on Postgres) alongside their normal typed columns.
Add or remove ad-hoc fields there any time — e.g. a yarn business puts `{"denier": 150}` on Material and
`{"pick": 40, "reed": 114}` on Product, a furniture business would put `{"boardFeet": 12}` and
`{"finishType": "lacquer"}` instead — without a schema migration. Reach for a real typed column instead
of `attributes` for anything you need to validate, index, or query/filter on directly.

<a name="schema"></a>
## Schema shape

- **Company** is the tenant root; Party/Material/Resource/Product all optionally scope to one.
- **Party** replaces separate customer/supplier tables — one party can hold any combination of
  `CUSTOMER` / `SUPPLIER` / `MANUFACTURER` roles.
- **Material** (raw input) and **Product** (finished good) are different entities with different
  lifecycles, linked by **ProductComponent** (the bill of materials — which material fills which slot).
- **MaterialStockMovement** is the raw-material purchase/sale ledger (`direction: IN|OUT`).
- **Order** doesn't carry a single status: it accumulates **OrderFulfillmentEvent** rows, one per stage
  (`COMPLETED`/`DISPATCHED`/`SETTLED`/`RETURNED`) and quantity — `OrderResponse` computes the roll-up
  totals per stage from that ledger.
- **User** (app login/auth) is intentionally unrelated to the business entities above.

## Build

```bash
mvn clean package
java -jar target/inventory-management.jar
```
