Requirements

- Java JDK 25
- Node.js 22.12 or newer
- MySQL Server 8.0.16 or newer on port 3306, and MySQL Workbench

1. Set up the database

In MySQL Workbench, connected as `root`:

1. Run `database/schema.sql`, then `database/sample-data.sql` (Execute All, `Ctrl+Shift+Enter`). Run them only once.
2. Create the account the backend uses:

```sql
CREATE USER 'grocery_app'@'localhost' IDENTIFIED BY 'CHOOSE_A_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE ON grocery_store.* TO 'grocery_app'@'localhost';
```

The connection settings are in `backend/src/main/resources/application.properties` (`localhost:3306/grocery_store`, user `grocery_app`). `database/view-data.sql` has read-only queries for checking the data and the reports.

2. Start the backend

```powershell
cd backend
$env:DB_PASSWORD = "CHOOSE_A_PASSWORD"
.\mvnw.cmd spring-boot:run
```
Wait for `Started BackendApplication`; http://localhost:8080/api/products should then show product data.

3. Start the frontend

In a second PowerShell window:

```powershell
cd frontend
npm.cmd install
npm.cmd start
```

Open http://localhost:4200. `npm.cmd install` is only needed the first time.


The backend creates these on its first start. Customers can also register on the site.

| Role | Email | Password |
| --- | --- | --- |
| Administrator | admin@grocerystore.local | Admin123! |
| Salesperson | salesperson@grocerystore.local | Salesperson123! |
| Customer | customer@grocerystore.local | Customer123! |
| Business customer | business@grocerystore.local | Business123! |
