# Inventory Management System

ERP Inventory Management backend built with Spring Boot 3, Spring Security 6, and JWT.
Every API endpoint (except `/api/auth/**` and Swagger docs) requires a valid `Bearer` access token.

## Tech stack

- Java 17, Spring Boot 3.3
- Spring Web, Spring Data JPA, Spring Security
- JWT (jjwt)
- H2 (dev profile) / MySQL (prod profile)
- springdoc-openapi (Swagger UI)
- Lombok, ModelMapper

## Running locally

```bash
mvn spring-boot:run
```

Runs with the `dev` profile by default (in-memory H2 database). Swagger UI: `http://localhost:8080/swagger-ui.html`.
H2 console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:inventorydb`).

A default admin user is seeded on startup: `admin` / `Admin@123`.

To run against MySQL, activate the `prod` profile and set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Authentication flow

1. `POST /api/auth/register` — create a user (optionally with roles: `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_STAFF`).
2. `POST /api/auth/login` — returns `accessToken` and `refreshToken`.
3. Send `Authorization: Bearer <accessToken>` on every subsequent request.
4. `POST /api/auth/refresh` — exchange a valid refresh token for a new token pair.

## Modules

- **Categories** `/api/categories`
- **Suppliers** `/api/suppliers`
- **Warehouses** `/api/warehouses`
- **Customers** `/api/customers`
- **Products** `/api/products`
- **Inventory** `/api/inventory` (stock levels + adjustments)
- **Purchase Orders** `/api/purchase-orders`
- **Sales Orders** `/api/sales-orders`

## Build

```bash
mvn clean package
java -jar target/inventory-management.jar
```
