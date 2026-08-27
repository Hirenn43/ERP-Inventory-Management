# Postman collection — ERP Inventory Management

Files:

| File | Purpose |
|------|---------|
| `ERP-Inventory-Management.postman_collection.json` | All API requests, grouped by flow, with test scripts that capture tokens & ids |
| `ERP-Inventory-Management.local.postman_environment.json` | Local config only (`baseUrl`, admin credentials) |

## Import

Postman → **Import** → select both files → pick the **ERP Inventory - Local** environment (top-right).

## Run the whole flow

Run requests **top to bottom**, or use **Collection Runner** / Newman. Each request stores what the next one needs:

1. **Auth** – `Login` captures `accessToken` + `refreshToken` into collection variables. A collection pre-request script attaches `Authorization: Bearer {{accessToken}}` to every non-auth request automatically.
2. **Master Data** – create Category → Supplier → Warehouse → Customer → Product (ids saved as `categoryId`, `supplierId`, …).
3. **Purchase Orders (stock IN)** – create a PO (status `PENDING`), then `Complete` it → stock is received into the warehouse.
4. **Inventory** – inspect stock, view movement history, post manual `STOCK_IN` / `STOCK_OUT` adjustments.
5. **Sales Orders (stock OUT)** – create a SO → stock is deducted and the order auto-completes; `Cancel` restores the stock. One request deliberately over-orders to show the `400 Insufficient stock` path.
6. **Cleanup (optional)** – deletes the master data. Rows still referenced by orders/movements return `409 Conflict` (expected).

## Collection variables

| Variable | Set by | Notes |
|----------|--------|-------|
| `baseUrl` | environment | `http://localhost:8080` |
| `username` / `password` | environment | seeded admin — `admin` / `Admin@123` |
| `accessToken` / `refreshToken` | `Login`, `Refresh` scripts | JWT, ~1h / 7d |
| `categoryId`, `supplierId`, `warehouseId`, `customerId`, `productId` | Master Data create scripts | |
| `purchaseOrderId`, `salesOrderId` | order create scripts | |

## Newman (CLI)

```bash
npx newman run postman/ERP-Inventory-Management.postman_collection.json \
  -e postman/ERP-Inventory-Management.local.postman_environment.json
```

Start the app first (`mvn spring-boot:run`) against a fresh DB for a clean run.
