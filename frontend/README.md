# Grocery Store — Angular frontend

A deliberately minimal user interface for the Grocery Store database assignment.

## Requirements

- Node.js 20.19+ or Node.js 22.12+ (Node 22 recommended)
- Angular packages are installed locally by npm; no global Angular CLI is needed
- Running MySQL and Spring Boot backend on `http://localhost:8080`

## Run on Windows (two PowerShell windows)

First window — backend:

```powershell
cd C:\Users\Raymond\Desktop\database-management-project\backend
$env:DB_PASSWORD = "YOUR_MYSQL_PASSWORD"
.\mvnw.cmd spring-boot:run
```

Second window — frontend:

```powershell
cd C:\Users\Raymond\Desktop\database-management-project\frontend
npm install
npm start
```

Open **http://localhost:4200**.

Angular proxies requests to `/api` to `localhost:8080` through `proxy.conf.json`, so CORS settings are not required for local development.

## Pages

- **Browse products**: search names/brands/IDs, category & price filters, stock per store, add to cart.
- **Cart / checkout**: choose customer, store, and payment method, then place order (simulated payment only). New order IDs are suggested but not reserved: a conflict requires a different ID.
- **Customers**: list, create, edit (including home/business subtype), delete. Customer deletion is blocked when orders exist.
- **Inventory**: check and adjust stock per store; enter the new total quantity.
- **Orders**: view orders and their line items.
- **Reports**: aggregate product revenue/gross profit, category rankings, region sales volumes, and business buyers of a selected product.

## Small backend additions included in the full project ZIP

The updated backend includes two read-only endpoints needed by the UI:

- `GET /api/customers/{customerId}/details` — includes HomeCustomer/BusinessCustomer fields for safe editing.
- `GET /api/order-items/order/{orderId}` — displays purchased products on the order history screen.

It also fixes the previously misplaced `orderItem.setUnitCost(product.getCostPrice())` line in `OrderService.java`, and supports legacy lowercase customer types in update/delete paths.

## Data setup

Use the project's database schema and sample-data SQL for a fresh database. If your database is already populated, **do not rerun sample-data.sql** because fixed primary keys will conflict and you may overwrite earlier work. The report's gross profit is unavailable when historical `unit_cost` is null.

## Limitations

This is a coursework/demo UI, not a production retail system: there is no authentication/roles or real card charging. The backend performs inventory checks and transaction handling. The current API supports changing stock quantities for existing store-product pairs, not creating new product catalog entries or new store-product relationships.
