USE grocery_store;

SELECT 'Region' AS table_name, COUNT(*) AS row_count FROM Region
UNION ALL SELECT 'Store', COUNT(*) FROM Store
UNION ALL SELECT 'Salesperson', COUNT(*) FROM Salesperson
UNION ALL SELECT 'Customer', COUNT(*) FROM Customer
UNION ALL SELECT 'HomeCustomer', COUNT(*) FROM HomeCustomer
UNION ALL SELECT 'BusinessCustomer', COUNT(*) FROM BusinessCustomer
UNION ALL SELECT 'Category', COUNT(*) FROM Category
UNION ALL SELECT 'Product', COUNT(*) FROM Product
UNION ALL SELECT 'Inventory', COUNT(*) FROM Inventory
UNION ALL SELECT 'Orders', COUNT(*) FROM Orders
UNION ALL SELECT 'OrderItem', COUNT(*) FROM OrderItem
UNION ALL SELECT 'UserAccount', COUNT(*) FROM UserAccount
UNION ALL SELECT 'Purchase', COUNT(*) FROM Purchase;

SELECT r.region_id AS `ID`,
       r.region_name AS `Region`,
       r.region_manager AS `Region manager`,
       COUNT(s.store_id) AS `Stores`
FROM Region r
LEFT JOIN Store s ON s.region_id = r.region_id
GROUP BY r.region_id, r.region_name, r.region_manager
ORDER BY r.region_id;

SELECT s.store_id AS `ID`,
       s.street AS `Street`,
       s.city AS `City`,
       s.state AS `State`,
       s.zip_code AS `ZIP`,
       s.manager AS `Manager`,
       r.region_name AS `Region`,
       (SELECT COUNT(*) FROM Salesperson e WHERE e.store_id = s.store_id) AS `Salespersons`,
       (SELECT COUNT(*) FROM Inventory i WHERE i.store_id = s.store_id) AS `Products carried`,
       (SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i WHERE i.store_id = s.store_id) AS `Units in stock`
FROM Store s
JOIN Region r ON r.region_id = s.region_id
ORDER BY s.store_id;

SELECT e.salesperson_id AS `ID`,
       e.name AS `Name`,
       e.job_title AS `Job title`,
       CONCAT('#', s.store_id, ' ', s.city) AS `Store`,
       e.email AS `Email`,
       e.salary AS `Salary`,
       CONCAT_WS(', ', e.street, e.city, e.state, e.zip_code) AS `Address`,
       a.email AS `Login`
FROM Salesperson e
JOIN Store s ON s.store_id = e.store_id
LEFT JOIN UserAccount a ON a.salesperson_id = e.salesperson_id
ORDER BY e.store_id, e.salesperson_id;

SELECT c.customer_id AS `ID`,
       c.name AS `Name`,
       c.customer_type AS `Type`,
       CONCAT_WS(', ', c.street, c.city, c.state, c.zip_code) AS `Address`,
       h.marital_status AS `Marital status`,
       h.gender AS `Gender`,
       h.age AS `Age`,
       h.income AS `Income`,
       b.business_category AS `Business category`,
       b.annual_income AS `Annual income`,
       COALESCE(o.orders, 0) AS `Orders`,
       COALESCE(o.spent, 0) AS `Total spent`,
       a.email AS `Login`
FROM Customer c
LEFT JOIN HomeCustomer h ON h.customer_id = c.customer_id
LEFT JOIN BusinessCustomer b ON b.customer_id = c.customer_id
LEFT JOIN (
    SELECT o.customer_id,
           COUNT(DISTINCT o.order_id) AS orders,
           SUM(oi.quantity * oi.sale_price) AS spent
    FROM Orders o
    JOIN OrderItem oi ON oi.order_id = o.order_id
    WHERE o.order_status <> 'CANCELLED'
    GROUP BY o.customer_id
) o ON o.customer_id = c.customer_id
LEFT JOIN UserAccount a ON a.customer_id = c.customer_id
ORDER BY c.customer_id;

SELECT c.category_id AS `ID`,
       c.category_name AS `Category`,
       c.description AS `Description`,
       COUNT(p.product_id) AS `Products`
FROM Category c
LEFT JOIN Product p ON p.category_id = c.category_id
GROUP BY c.category_id, c.category_name, c.description
ORDER BY c.category_id;

SELECT p.product_id AS `ID`,
       p.product_name AS `Product`,
       p.brand AS `Brand`,
       c.category_name AS `Category`,
       p.price AS `Price`,
       MAX(CASE WHEN i.store_id = 1 THEN i.quantity END) AS `Store 1`,
       MAX(CASE WHEN i.store_id = 2 THEN i.quantity END) AS `Store 2`,
       MAX(CASE WHEN i.store_id = 3 THEN i.quantity END) AS `Store 3`,
       MAX(CASE WHEN i.store_id = 4 THEN i.quantity END) AS `Store 4`,
       MAX(CASE WHEN i.store_id = 5 THEN i.quantity END) AS `Store 5`,
       SUM(CASE WHEN i.store_id NOT IN (1, 2, 3, 4, 5) THEN i.quantity END) AS `Other stores`,
       COALESCE(SUM(i.quantity), 0) AS `Total`
FROM Product p
JOIN Category c ON c.category_id = p.category_id
LEFT JOIN Inventory i ON i.product_id = p.product_id
GROUP BY p.product_id, p.product_name, p.brand, c.category_name, p.price
ORDER BY p.product_id;

SELECT o.order_id AS `Order #`,
       o.order_date AS `Date`,
       o.order_status AS `Status`,
       c.name AS `Customer`,
       c.customer_type AS `Customer type`,
       CONCAT('#', s.store_id, ' ', s.city) AS `Store`,
       e.name AS `Salesperson`,
       o.payment_method AS `Payment`,
       COUNT(oi.product_id) AS `Lines`,
       COALESCE(SUM(oi.quantity), 0) AS `Units`,
       COALESCE(SUM(oi.quantity * oi.sale_price), 0) AS `Total`
FROM Orders o
JOIN Customer c ON c.customer_id = o.customer_id
JOIN Store s ON s.store_id = o.store_id
LEFT JOIN Salesperson e ON e.salesperson_id = o.salesperson_id
LEFT JOIN OrderItem oi ON oi.order_id = o.order_id
GROUP BY o.order_id, o.order_date, o.order_status, c.name, c.customer_type,
         s.store_id, s.city, e.name, o.payment_method
ORDER BY o.order_date DESC, o.order_id DESC;

SELECT oi.order_id AS `Order #`,
       o.order_date AS `Date`,
       c.name AS `Customer`,
       p.product_name AS `Product`,
       oi.quantity AS `Qty`,
       oi.sale_price AS `Sale price`,
       oi.unit_cost AS `Unit cost`,
       p.price AS `Current price`,
       oi.quantity * oi.sale_price AS `Line total`
FROM OrderItem oi
JOIN Orders o ON o.order_id = oi.order_id
JOIN Customer c ON c.customer_id = o.customer_id
JOIN Product p ON p.product_id = oi.product_id
ORDER BY oi.order_id DESC, p.product_name;

SELECT a.account_id AS `ID`,
       a.email AS `Email`,
       a.role AS `Role`,
       COALESCE(c.name, e.name) AS `Linked to`,
       a.created_at AS `Created`
FROM UserAccount a
LEFT JOIN Customer c ON c.customer_id = a.customer_id
LEFT JOIN Salesperson e ON e.salesperson_id = a.salesperson_id
ORDER BY a.account_id;

SELECT p.product_id AS `ID`,
       p.product_name AS `Product`,
       c.category_name AS `Category`,
       p.cost_price AS `Cost price`,
       COALESCE(SUM(s.quantity), 0) AS `Units sold`,
       COALESCE(SUM(s.quantity * s.sale_price), 0) AS `Sales`,
       COALESCE(SUM(s.quantity * (s.sale_price - s.unit_cost)), 0) AS `Profit`
FROM Product p
JOIN Category c ON c.category_id = p.category_id
LEFT JOIN (
    SELECT oi.product_id, oi.quantity, oi.sale_price, oi.unit_cost
    FROM OrderItem oi
    JOIN Orders o ON o.order_id = oi.order_id
    WHERE o.order_status <> 'CANCELLED'
) s ON s.product_id = p.product_id
GROUP BY p.product_id, p.product_name, c.category_name, p.cost_price
ORDER BY `Sales` DESC, p.product_name;

SELECT c.category_name AS `Category`,
       COALESCE(SUM(oi.quantity), 0) AS `Units sold`,
       COALESCE(SUM(oi.quantity * oi.sale_price), 0) AS `Sales`,
       ROUND(100 * COALESCE(SUM(oi.quantity * oi.sale_price), 0) / (
           SELECT SUM(x.quantity * x.sale_price)
           FROM OrderItem x JOIN Orders y ON y.order_id = x.order_id
           WHERE y.order_status <> 'CANCELLED'), 1) AS `Share %`
FROM Category c
LEFT JOIN Product p ON p.category_id = c.category_id
LEFT JOIN OrderItem oi ON oi.product_id = p.product_id
     AND oi.order_id IN (SELECT order_id FROM Orders WHERE order_status <> 'CANCELLED')
GROUP BY c.category_id, c.category_name
ORDER BY `Sales` DESC;

SELECT r.region_name AS `Region`,
       COUNT(DISTINCT s.store_id) AS `Stores`,
       COUNT(DISTINCT o.order_id) AS `Orders`,
       COALESCE(SUM(oi.quantity), 0) AS `Units`,
       COALESCE(SUM(oi.quantity * oi.sale_price), 0) AS `Sales`
FROM Region r
LEFT JOIN Store s ON s.region_id = r.region_id
LEFT JOIN Orders o ON o.store_id = s.store_id AND o.order_status <> 'CANCELLED'
LEFT JOIN OrderItem oi ON oi.order_id = o.order_id
GROUP BY r.region_id, r.region_name
ORDER BY `Sales` DESC;

SELECT p.product_name AS `Product`,
       c.name AS `Business`,
       b.business_category AS `Business type`,
       SUM(oi.quantity) AS `Units bought`,
       SUM(oi.quantity * oi.sale_price) AS `Spent`
FROM OrderItem oi
JOIN Orders o ON o.order_id = oi.order_id
JOIN Customer c ON c.customer_id = o.customer_id
JOIN BusinessCustomer b ON b.customer_id = c.customer_id
JOIN Product p ON p.product_id = oi.product_id
WHERE o.order_status <> 'CANCELLED'
GROUP BY p.product_id, p.product_name, c.customer_id, c.name, b.business_category
ORDER BY p.product_name, `Units bought` DESC;

SELECT DATE_FORMAT(o.order_date, '%Y-%m') AS `Month`,
       COUNT(DISTINCT o.order_id) AS `Orders`,
       SUM(oi.quantity) AS `Units`,
       SUM(oi.quantity * oi.sale_price) AS `Sales`,
       SUM(CASE WHEN o.order_status IN ('PAID', 'PROCESSING', 'COMPLETED')
                THEN oi.quantity * oi.sale_price ELSE 0 END) AS `Collected`
FROM Orders o
JOIN OrderItem oi ON oi.order_id = o.order_id
WHERE o.order_status <> 'CANCELLED'
GROUP BY `Month`
ORDER BY `Month`;

SELECT pu.purchase_id AS `ID`,
       pu.purchase_date AS `Date`,
       CONCAT('#', s.store_id, ' ', s.city) AS `Store`,
       p.product_name AS `Product`,
       pu.quantity AS `Qty`,
       pu.unit_cost AS `Unit cost`,
       pu.quantity * pu.unit_cost AS `Total cost`,
       pu.supplier AS `Supplier`
FROM Purchase pu
JOIN Store s ON s.store_id = pu.store_id
JOIN Product p ON p.product_id = pu.product_id
ORDER BY pu.purchase_date DESC, pu.purchase_id DESC;

SELECT k.store_id AS `Store`,
       p.product_name AS `Product`,
       COALESCE(d.delivered, 0) AS `Delivered`,
       COALESCE(x.sold, 0) AS `Sold`,
       i.quantity AS `In stock`,
       COALESCE(d.delivered, 0) - COALESCE(x.sold, 0) - COALESCE(i.quantity, 0) AS `Difference`
FROM (
    SELECT store_id, product_id FROM Inventory
    UNION
    SELECT store_id, product_id FROM Purchase
    UNION
    SELECT o.store_id, oi.product_id
    FROM OrderItem oi
    JOIN Orders o ON o.order_id = oi.order_id
) k
JOIN Product p ON p.product_id = k.product_id
LEFT JOIN Inventory i ON i.store_id = k.store_id AND i.product_id = k.product_id
LEFT JOIN (
    SELECT store_id, product_id, SUM(quantity) AS delivered
    FROM Purchase
    GROUP BY store_id, product_id
) d ON d.store_id = k.store_id AND d.product_id = k.product_id
LEFT JOIN (
    SELECT o.store_id, oi.product_id, SUM(oi.quantity) AS sold
    FROM OrderItem oi
    JOIN Orders o ON o.order_id = oi.order_id
    WHERE o.order_status <> 'CANCELLED'
    GROUP BY o.store_id, oi.product_id
) x ON x.store_id = k.store_id AND x.product_id = k.product_id
ORDER BY ABS(`Difference`) DESC, i.quantity IS NULL DESC, k.store_id, p.product_name;

SELECT o.payment_method AS `Payment method`,
       COUNT(DISTINCT o.order_id) AS `Orders`,
       SUM(oi.quantity * oi.sale_price) AS `Sales`,
       SUM(CASE WHEN o.order_status IN ('PAID', 'PROCESSING', 'COMPLETED')
                THEN oi.quantity * oi.sale_price ELSE 0 END) AS `Collected`,
       SUM(CASE WHEN o.order_status = 'PENDING'
                THEN oi.quantity * oi.sale_price ELSE 0 END) AS `Waiting for payment`
FROM Orders o
JOIN OrderItem oi ON oi.order_id = o.order_id
WHERE o.order_status <> 'CANCELLED'
GROUP BY o.payment_method
ORDER BY `Sales` DESC;
