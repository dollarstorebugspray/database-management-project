# Grocery Store E-Commerce Database Project

A local web application for browsing grocery products, placing sample orders, managing customers and inventory, reviewing order history, and viewing sales reports. This project uses **MySQL** for storage, **Java 25 / Spring Boot 4** for the backend API, and **Angular 20** for the frontend.

This guide starts from the files included in the downloaded project. It assumes a **Windows computer with PowerShell** and **no development tools installed yet**. You do not need to install Spring Boot, Maven, or Angular CLI separately.

> **Demo/class project:** Payments are simulated; the app records a payment method but does not charge cards. There is no login/role system. Use sample or non-sensitive data.

## 1. Files in the downloaded project

Extract the ZIP first. The resulting folder should look like this (other configuration/source files are omitted):

```text
database-management-project/
├── README.md
├── database/
│   ├── schema.sql           # Creates grocery_store and all 11 tables
│   └── sample-data.sql      # Inserts the demonstration records
├── backend/
│   ├── pom.xml              # Spring Boot/Maven configuration
│   ├── mvnw.cmd             # Maven Wrapper for Windows
│   └── src/main/
│       ├── java/com/grocerystore/backend/
│       └── resources/application.properties
└── frontend/
    ├── package.json         # Angular dependencies and commands
    ├── package-lock.json
    ├── proxy.conf.json      # Sends /api requests to the backend
    └── src/
```

**Always open terminals in the correct `backend` or `frontend` directory.** The examples below use a folder on the Desktop; substitute your actual extracted location.

## 2. Install the required software (one-time setup)

Install these on each computer that will run the project:

| Software | What it does | Where to get it |
| --- | --- | --- |
| **Java JDK 25** | Runs/compiles the backend | [Eclipse Temurin JDK 25](https://adoptium.net/temurin/releases/?version=25) or another JDK 25 distribution |
| **Node.js** | Includes npm; installs and runs the Angular frontend | [Node.js downloads](https://nodejs.org/en/download) |
| **MySQL Community Server 8.x** | Stores the database | [MySQL Community Server](https://dev.mysql.com/downloads/mysql/) |
| **MySQL Workbench** | GUI to execute the included SQL scripts | [MySQL Workbench](https://dev.mysql.com/downloads/workbench/) |

**Java:** Install the full **JDK**, not only a JRE. If prompted, enable **Add to PATH** and **Set JAVA_HOME**. After installing, open a *new* PowerShell window and verify:

```powershell
java --version
javac --version
```

Both should show Java 25. If `java` is not found, restart PowerShell and check that the JDK `bin` directory is on PATH and `JAVA_HOME` points to the JDK installation folder.

**Node.js:** Install an actively supported Node.js version compatible with Angular 20 (for example **Node.js 22.12+**). npm is included. Restart PowerShell and verify:

```powershell
node --version
npm.cmd --version
```

The instructions use `npm.cmd` because some PowerShell configurations prevent running `npm.ps1`. No separate npm or global Angular CLI installation is needed.

**MySQL:** During the MySQL Server installation, configure a local server on port **3306** and remember the administrator (`root`) password. MySQL Workbench is a separate GUI program; installing Workbench alone **does not** install MySQL Server. Make sure the MySQL service is running.

You do **not** need to install Maven or Spring Boot separately: `backend/mvnw.cmd` downloads the needed Maven version, and `pom.xml` defines Spring Boot dependencies. You also do **not** need a global `ng` command: `frontend/package.json` installs the Angular CLI locally.

## 3. Create and populate the database (one-time setup)

1. Open **MySQL Workbench** and connect to your local MySQL Server as an administrator (usually `root`, on `localhost:3306`).
2. Use **File > Open SQL Script** to open `database/schema.sql` from the extracted project folder.
3. Execute the **entire script** using Workbench's **Execute All** button (lightning icon) or `Ctrl+Shift+Enter`.
4. Open `database/sample-data.sql` and execute the **entire script** the same way.
5. Optionally verify that data is present:

```sql
USE grocery_store;
SHOW TABLES;
SELECT COUNT(*) AS product_count FROM Product;
SELECT COUNT(*) AS order_count FROM Orders;
```

On an empty installation, the supplied sample data includes **20 products** and **10 orders**. The schema includes `cost_price` in `Product` and `unit_cost` in `OrderItem` for gross-profit reporting. Sample costs are **hypothetical**, for coursework purposes.

**Important:** `schema.sql` creates tables and `sample-data.sql` inserts rows with fixed IDs. **Run them only once for a fresh database.** Re-running them against populated tables can produce table-already-exists or duplicate-primary-key errors. Do not drop an existing database just to troubleshoot: that would delete orders, customers, and inventory changes.

## 4. Give the backend permission to use MySQL (one-time setup)

The backend is configured to connect as **`grocery_app`**, not as `root`.

While still connected in Workbench as a MySQL administrator, run the following. Replace `CHOOSE_A_PASSWORD` with a new local password you will remember:

```sql
CREATE USER 'grocery_app'@'localhost' IDENTIFIED BY 'CHOOSE_A_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE ON grocery_store.* TO 'grocery_app'@'localhost';
```

If that user already exists, **do not run `CREATE USER` again**. Instead, use its existing password or, if you administer this local installation, reset it with:

```sql
ALTER USER 'grocery_app'@'localhost' IDENTIFIED BY 'YOUR_NEW_PASSWORD';
```

This MySQL account has the data-access permissions the application needs. Create/import the schema with your administrator account, not with `grocery_app`.

## 5. Configure the backend

Check `backend/src/main/resources/application.properties`. The downloaded project already contains:

```properties
spring.application.name=backend
spring.datasource.url=jdbc:mysql://localhost:3306/grocery_store
spring.datasource.username=grocery_app
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
server.port=8080
```

You generally **do not need to change this file**. It assumes the database is called `grocery_store` and MySQL is on port `3306`.

`DB_PASSWORD` is an environment variable. **Do not replace it with a real password in a file committed to GitHub.** The `validate` setting verifies the database schema at startup; it does not create the tables or load the sample data.

## 6. Start the application

You will need **two PowerShell windows**, one for the backend and one for the frontend. Keep both open while using the site.

### Terminal 1 — Spring Boot backend

Open PowerShell and navigate into the extracted project's `backend` folder. Example:

```powershell
cd "$HOME\Desktop\database-management-project\backend"
```

Set the password you created for **`grocery_app`** in **that same PowerShell window**:

```powershell
$env:DB_PASSWORD = "YOUR_GROCERY_APP_PASSWORD"
```

Start Spring Boot:

```powershell
.\mvnw.cmd spring-boot:run
```

On the first launch, Maven downloads dependencies, so an internet connection is needed. A successful startup includes a message similar to `Started BackendApplication` and indicates that Tomcat is listening on **port 8080**.

You can check the API in a browser at:

**http://localhost:8080/api/products**

It should show JSON product data. Leave this PowerShell window running.

> The `$env:DB_PASSWORD` assignment lasts for the current PowerShell session. Set it again whenever you start the backend from a new window. The password is for `grocery_app` and may differ from the MySQL `root` password.

### Terminal 2 — Angular frontend

Open a **second PowerShell window** and navigate to the extracted project's `frontend` folder:

```powershell
cd "$HOME\Desktop\database-management-project\frontend"
```

Install the packages listed in the included `package.json` and lock file:

```powershell
npm.cmd install
```

You normally need to install dependencies only the first time, or after `package.json`/`package-lock.json` changes. Then start the frontend:

```powershell
npm.cmd start
```

When Angular reports that its development server is ready, open:

**http://localhost:4200**

The included `frontend/proxy.conf.json` sends frontend `/api` calls to the backend at `http://localhost:8080`. **Both servers must be running** for the interface to display or change database data. MySQL Server must also stay running.

### The normal startup routine on later days

You do not need to re-import SQL data or reinstall packages every time:

1. Ensure the MySQL Server service is running.
2. Open PowerShell in `backend`, set `$env:DB_PASSWORD`, run `.\mvnw.cmd spring-boot:run`.
3. Open another PowerShell in `frontend`, run `npm.cmd start`.
4. Visit **http://localhost:4200**.

To stop a running development server, press **Ctrl+C** in its PowerShell window.

## 7. How to use the website

Use the navigation buttons across the top of the Angular page. All records are stored in your **local MySQL database**, not in the ZIP file or on GitHub.

### Browse products

1. Open **Browse products**.
2. Select a **store**; stock quantities are specific to each store.
3. Browse the product list, search by product name/brand/ID, or use the category and maximum-price filters.
4. Enter a quantity and choose **Add to cart** for a product in stock.

The cart is for the selected store. Switching stores may change availability.

### Cart and checkout

1. Open **Cart and checkout** after adding products.
2. Review items, change quantities, or remove items.
3. Select an existing **customer** (create one on the Customers page first if necessary).
4. Confirm the **order ID** is unique and choose a payment method. You may optionally supply a salesperson ID.
5. Select **Submit purchase**.

If the order is valid and products are in stock, the backend saves it in `Orders` and `OrderItem` and decreases inventory. It rejects invalid requests and insufficient stock. If an order ID is already used, choose a different one. **Payments are simulated**, not processed through a bank.

### Customers

1. Open **Customers** to see existing customers.
2. To add a customer, enter an unused **Customer ID**, name/address information, and choose **Home** or **Business**.
3. Fill in the additional fields shown for that customer type and select **Create customer**.
4. To change an existing customer, select **Edit**, change the information, then **Save changes**.
5. To delete a customer, select **Delete** and confirm.

Customer types cannot be switched during editing. A customer who has orders **cannot be deleted**, because the system preserves purchase history.

### Inventory

1. Open **Inventory management**.
2. Choose a **store**.
3. Find the product row, enter the **new total quantity** in the New quantity field, and select **Save stock**.

For example, if the current quantity is 8 and you want 15 in stock, enter **15**, not **7**. Negative stock quantities are rejected. This screen updates existing store/product inventory records; it does not add brand-new products or stores.

### Orders

1. Open **Order history**.
2. Browse the list of orders with their dates, customers, stores, and payment methods.
3. Select **View items** to see the purchased products, quantities, prices, and line totals for an order.

### Reports

Open **Reports** to access the required database aggregation features:

- **Product sales and gross profit:** Units sold, revenue, and profit by product. Gross profit may show **N/A** if historical cost data is missing.
- **Top product categories:** Categories ranked by product sales.
- **Regional sales comparison:** Compare regions using sales volume, order counts, and revenue.
- **Business purchases:** Select a specific product to see which business customers bought the most units of it.

Use **Refresh reports** after entering new purchases to get updated totals. Use the product selector and its report button to update the business-purchasing results.

## 8. Common problems and solutions

| Problem | What to check |
| --- | --- |
| `java` or `javac` not recognized | Install JDK 25, restart PowerShell, check PATH/JAVA_HOME. |
| `node` or `npm` not recognized | Install Node.js, restart PowerShell, use `npm.cmd`. |
| `npm.ps1 cannot be loaded` | Run `npm.cmd install` and `npm.cmd start` instead of `npm`. |
| `mvnw.cmd` not found | Confirm you are inside the downloaded project's `backend` directory. |
| `Access denied for user 'grocery_app'@'localhost'` | Confirm the MySQL user exists, the password matches, and `$env:DB_PASSWORD` is set in the terminal running Spring Boot. |
| `Communications link failure` or cannot connect to MySQL | Start MySQL Server; verify `localhost:3306` and the database name. |
| `Schema validation: missing table/column` | Execute the included `database/schema.sql` on a *fresh* database; verify the installed schema matches the Java models, especially `cost_price` and `unit_cost`. |
| `Table already exists` or duplicate-key errors while importing | The database already contains schema/data. Do not rerun the setup scripts on a populated database. |
| Port `8080` already in use | Stop the previous backend process before starting another one. |
| Port `4200` already in use | Stop the previous Angular process before starting another one. |
| Website opens but products/customers do not load | Verify Spring Boot is running on port 8080 and shows no database errors; start Angular with `npm.cmd start` so the proxy is active. |
| Checkout returns `409 Conflict` | Confirm stock is sufficient and the order ID hasn't already been used. |
| Report profit shows `N/A` | One or more historical order-item costs (`unit_cost`) are missing. |
| npm reports package vulnerabilities | Installation may still succeed. Review with `npm audit`; avoid `npm audit fix --force` without testing for breaking changes. |

If the backend prints `BUILD SUCCESS` but also says `Application run failed`, **the app did not start successfully**; inspect the earlier error lines.

## 9. Useful reference: local services and URLs

| Component | Local address |
| --- | --- |
| Angular website | `http://localhost:4200` |
| Spring Boot API | `http://localhost:8080` |
| Example product API | `http://localhost:8080/api/products` |
| MySQL Server | `localhost:3306` |

The backend exposes APIs for products, customers, inventory, orders, order items, and reports. Angular calls these automatically; users normally work through the website at port 4200.

## 10. Sharing the project

The ZIP/repository contains the **source code and SQL setup scripts**, not a live MySQL database. Each teammate sets up their own local database using Section 3. Their changes to customers, orders, and inventory stay on their own computer unless data is explicitly exported and shared.

Do **not** upload real database passwords or private configuration files. The downloaded ZIP may contain generated folders (for example `frontend/node_modules`, `backend/target`, or `.git`), but fresh users should rely on `npm.cmd install` and the Maven Wrapper instead of manually copying installed packages. Git itself is **not required** just to download a ZIP and run the application.
