-- Grocery Store E-Commerce sample data
-- Run database/schema.sql before this file.
-- Run once against EMPTY tables created by the updated schema.sql.
-- Cost values are HYPOTHETICAL SAMPLE WHOLESALE COSTS, not verified historical costs.
-- This file inserts rows; it does not create tables.
USE grocery_store;

-- 1. Regions
INSERT INTO Region (region_id, region_name, region_manager) VALUES
(1, 'Western Pennsylvania', 'Morgan Reed'),
(2, 'Central Pennsylvania', 'Taylor Brooks'),
(3, 'Eastern Pennsylvania', 'Casey Parker');

-- 2. Stores (each belongs to an existing region)
INSERT INTO Store (store_id, street, city, state, zip_code, manager, region_id) VALUES
(1, '101 Market Street', 'Pittsburgh', 'PA', '15222', 'Jordan Ellis', 1),
(2, '250 Liberty Avenue', 'Pittsburgh', 'PA', '15222', 'Avery Bennett', 1),
(3, '15 Walnut Street', 'Harrisburg', 'PA', '17101', 'Quinn Cooper', 2),
(4, '600 Penn Street', 'Reading', 'PA', '19601', 'Riley Morgan', 2),
(5, '80 Chestnut Street', 'Philadelphia', 'PA', '19106', 'Peyton Ross', 3);

-- 3. Salespersons (two assigned to each store)
INSERT INTO Salesperson
(salesperson_id, name, street, city, state, zip_code, email, job_title, salary, store_id) VALUES
(1, 'Alex Rivera', '21 Oak Street', 'Pittsburgh', 'PA', '15213', 'alex.rivera@example.com', 'Cashier', 32000.00, 1),
(2, 'Jamie Clark', '32 Pine Street', 'Pittsburgh', 'PA', '15217', 'jamie.clark@example.com', 'Sales Associate', 34000.00, 1),
(3, 'Sam Wilson', '43 Maple Avenue', 'Pittsburgh', 'PA', '15206', 'sam.wilson@example.com', 'Cashier', 32500.00, 2),
(4, 'Drew Thomas', '54 Cedar Road', 'Pittsburgh', 'PA', '15203', 'drew.thomas@example.com', 'Sales Associate', 35000.00, 2),
(5, 'Robin Hayes', '65 Walnut Lane', 'Harrisburg', 'PA', '17102', 'robin.hayes@example.com', 'Cashier', 31000.00, 3),
(6, 'Chris Lane', '76 Elm Street', 'Harrisburg', 'PA', '17110', 'chris.lane@example.com', 'Sales Associate', 33500.00, 3),
(7, 'Dana Price', '87 Birch Street', 'Reading', 'PA', '19604', 'dana.price@example.com', 'Cashier', 31500.00, 4),
(8, 'Lee Carter', '98 Spruce Road', 'Reading', 'PA', '19602', 'lee.carter@example.com', 'Sales Associate', 34500.00, 4),
(9, 'Cameron Lee', '109 Broad Street', 'Philadelphia', 'PA', '19107', 'cameron.lee@example.com', 'Cashier', 33000.00, 5),
(10, 'Skyler Adams', '120 Pine Street', 'Philadelphia', 'PA', '19103', 'skyler.adams@example.com', 'Sales Associate', 35500.00, 5);

-- 4. Customers: 6 home customers and 4 business customers
INSERT INTO Customer (customer_id, name, street, city, state, zip_code, customer_type) VALUES
(1, 'Emma Davis', '14 Hill Street', 'Pittsburgh', 'PA', '15213', 'Home'),
(2, 'Noah Martin', '25 Oak Avenue', 'Pittsburgh', 'PA', '15217', 'Home'),
(3, 'Olivia Brown', '36 River Road', 'Harrisburg', 'PA', '17101', 'Home'),
(4, 'Liam Johnson', '47 Pine Street', 'Reading', 'PA', '19601', 'Home'),
(5, 'Sophia Miller', '58 Walnut Lane', 'Philadelphia', 'PA', '19106', 'Home'),
(6, 'Ethan Moore', '69 Cedar Street', 'Pittsburgh', 'PA', '15222', 'Home'),
(7, 'Corner Cafe LLC', '100 Main Street', 'Pittsburgh', 'PA', '15222', 'Business'),
(8, 'Fresh Start Catering', '200 State Street', 'Harrisburg', 'PA', '17101', 'Business'),
(9, 'Downtown Deli Co', '300 Penn Street', 'Reading', 'PA', '19601', 'Business'),
(10, 'City Lunch Kitchen', '400 Market Street', 'Philadelphia', 'PA', '19106', 'Business');

INSERT INTO HomeCustomer (customer_id, marital_status, gender, age, income) VALUES
(1, 'Single', 'Female', 24, 42000.00),
(2, 'Married', 'Male', 37, 68000.00),
(3, 'Single', 'Female', 29, 51000.00),
(4, 'Married', 'Male', 44, 75000.00),
(5, 'Single', 'Female', 32, 62000.00),
(6, 'Married', 'Male', 40, 81000.00);

INSERT INTO BusinessCustomer (customer_id, business_category, annual_income) VALUES
(7, 'Cafe', 275000.00),
(8, 'Catering', 540000.00),
(9, 'Deli', 360000.00),
(10, 'Restaurant', 720000.00);

-- 5. Product categories
INSERT INTO Category (category_id, category_name, description) VALUES
(1, 'Dairy', 'Milk, cheese, yogurt and related foods'),
(2, 'Produce', 'Fresh fruits and vegetables'),
(3, 'Bakery', 'Bread, baked goods and pastries'),
(4, 'Meat', 'Fresh meat and poultry'),
(5, 'Pantry', 'Shelf-stable foods and beverages');

-- 6. Products (price = selling price; cost_price = hypothetical wholesale cost)
INSERT INTO Product (product_id, product_name, brand, price, cost_price, category_id) VALUES
(101, 'Whole Milk 1 Gallon', 'Valley Farms', 4.29, 2.75, 1),
(102, 'Cheddar Cheese 8 oz', 'Valley Farms', 3.49, 2.10, 1),
(103, 'Greek Yogurt 5 oz', 'Morning Dairy', 1.29, 0.74, 1),
(104, 'Salted Butter 1 lb', 'Valley Farms', 4.79, 3.15, 1),
(105, 'Bananas 1 lb', 'Fresh Fields', 0.69, 0.38, 2),
(106, 'Apples 1 lb', 'Fresh Fields', 1.99, 1.10, 2),
(107, 'Romaine Lettuce', 'Green Basket', 2.49, 1.35, 2),
(108, 'Carrots 2 lb Bag', 'Green Basket', 2.29, 1.22, 2),
(109, 'Sandwich Bread', 'Daily Bake', 2.99, 1.65, 3),
(110, 'Bagels 6 Count', 'Daily Bake', 3.79, 2.14, 3),
(111, 'Croissants 4 Count', 'Daily Bake', 4.49, 2.55, 3),
(112, 'Dinner Rolls 12 Count', 'Daily Bake', 3.29, 1.85, 3),
(113, 'Chicken Breast 1 lb', 'Farm Select', 5.99, 4.05, 4),
(114, 'Ground Beef 1 lb', 'Farm Select', 6.49, 4.50, 4),
(115, 'Turkey Breast 1 lb', 'Farm Select', 5.49, 3.85, 4),
(116, 'Pork Chops 1 lb', 'Farm Select', 5.79, 4.05, 4),
(117, 'White Rice 2 lb', 'Pantry Choice', 3.99, 2.28, 5),
(118, 'Pasta 16 oz', 'Pantry Choice', 1.79, 0.96, 5),
(119, 'Tomato Sauce 24 oz', 'Pantry Choice', 2.19, 1.18, 5),
(120, 'Orange Juice 52 oz', 'Sun Valley', 4.59, 2.85, 5);

-- 7. Current stock for each store/product pair (not stored in Product)
-- These quantities are illustrative current balances, independent of the order history below.
INSERT INTO Inventory (store_id, product_id, quantity) VALUES
(1,101,40),(1,102,28),(1,103,45),(1,104,22),(1,105,80),(1,106,65),(1,107,35),(1,108,42),(1,109,50),(1,110,26),
(1,111,19),(1,112,24),(1,113,30),(1,114,25),(1,115,20),(1,116,18),(1,117,55),(1,118,65),(1,119,60),(1,120,38),
(2,101,18),(2,102,24),(2,103,35),(2,104,20),(2,105,55),(2,106,50),(2,107,28),(2,108,32),(2,109,40),(2,110,22),
(2,111,16),(2,112,20),(2,113,25),(2,114,21),(2,115,19),(2,116,16),(2,117,44),(2,118,52),(2,119,48),(2,120,30),
(3,101,32),(3,102,19),(3,103,30),(3,104,15),(3,105,60),(3,106,45),(3,107,22),(3,108,28),(3,109,34),(3,110,18),
(3,111,12),(3,112,16),(3,113,23),(3,114,17),(3,115,16),(3,116,14),(3,117,38),(3,118,47),(3,119,43),(3,120,25),
(4,101,27),(4,102,21),(4,103,32),(4,104,17),(4,105,58),(4,106,47),(4,107,25),(4,108,30),(4,109,37),(4,110,20),
(4,111,14),(4,112,18),(4,113,24),(4,114,19),(4,115,17),(4,116,15),(4,117,41),(4,118,49),(4,119,46),(4,120,27),
(5,101,63),(5,102,35),(5,103,52),(5,104,29),(5,105,90),(5,106,78),(5,107,41),(5,108,49),(5,109,58),(5,110,31),
(5,111,23),(5,112,28),(5,113,36),(5,114,31),(5,115,27),(5,116,24),(5,117,62),(5,118,72),(5,119,69),(5,120,46);

-- 8. Orders: salesperson_id may be NULL for online purchases
-- Non-NULL salespersons are assigned to the same store as the order.
INSERT INTO Orders (order_id, order_date, payment_method, customer_id, salesperson_id, store_id) VALUES
(1001, '2026-10-01', 'Credit Card', 1, 1, 1),
(1002, '2026-10-02', 'Debit Card', 2, NULL, 2),
(1003, '2026-10-03', 'Credit Card', 3, 5, 3),
(1004, '2026-10-04', 'Cash', 4, 7, 4),
(1005, '2026-10-05', 'Credit Card', 5, 9, 5),
(1006, '2026-10-06', 'Debit Card', 6, 2, 1),
(1007, '2026-10-07', 'Credit Card', 7, NULL, 1),
(1008, '2026-10-08', 'Credit Card', 8, 6, 3),
(1009, '2026-10-09', 'Debit Card', 9, 8, 4),
(1010, '2026-10-10', 'Credit Card', 10, NULL, 5);

-- 9. Order lines (sale_price = historical selling price; unit_cost = hypothetical historical cost)
-- For demo data only, historical costs are assumed to match the initial wholesale costs.
INSERT INTO OrderItem (order_id, product_id, quantity, sale_price, unit_cost) VALUES
(1001, 101, 2, 4.29, 2.75), (1001, 105, 3, 0.69, 0.38), (1001, 109, 1, 2.99, 1.65),
(1002, 102, 1, 3.49, 2.10), (1002, 106, 2, 1.99, 1.10),
(1003, 113, 2, 5.99, 4.05), (1003, 108, 1, 2.29, 1.22), (1003, 117, 1, 3.99, 2.28),
(1004, 114, 1, 6.49, 4.50), (1004, 118, 2, 1.79, 0.96),
(1005, 120, 2, 4.59, 2.85), (1005, 110, 1, 3.79, 2.14), (1005, 103, 4, 1.29, 0.74),
(1006, 104, 1, 4.79, 3.15), (1006, 111, 2, 4.49, 2.55),
(1007, 101, 5, 4.19, 2.75), (1007, 109, 4, 2.99, 1.65), (1007, 119, 6, 2.19, 1.18),
(1008, 113, 8, 5.79, 4.05), (1008, 107, 5, 2.49, 1.35),
(1009, 115, 6, 5.49, 3.85), (1009, 112, 4, 3.29, 1.85),
(1010, 116, 5, 5.79, 4.05), (1010, 117, 4, 3.99, 2.28), (1010, 120, 4, 4.59, 2.85);

-- Optional sanity checks
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
UNION ALL SELECT 'OrderItem', COUNT(*) FROM OrderItem;
