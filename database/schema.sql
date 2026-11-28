-- Grocery Store E-Commerce schema (includes cost fields for profit reporting)
-- Run BEFORE sample-data.sql. Run against a NEW/EMPTY database.
CREATE DATABASE IF NOT EXISTS grocery_store;
USE grocery_store;

CREATE TABLE Region (
    region_id INT PRIMARY KEY,
    region_name VARCHAR(50) NOT NULL,
    region_manager VARCHAR(50)
);

CREATE TABLE Store (
    store_id INT PRIMARY KEY,
    street VARCHAR(50) NOT NULL,
    city VARCHAR(25) NOT NULL,
    state VARCHAR(15) NOT NULL,
    zip_code VARCHAR(10) NOT NULL,
    manager VARCHAR(50),
    region_id INT NOT NULL,
    FOREIGN KEY (region_id) REFERENCES Region(region_id)
);

CREATE TABLE Salesperson (
    salesperson_id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    street VARCHAR(50),
    city VARCHAR(25),
    state VARCHAR(15),
    zip_code VARCHAR(10),
    email VARCHAR(50),
    job_title VARCHAR(50),
    salary DECIMAL(10,2),
    store_id INT NOT NULL,
    FOREIGN KEY (store_id) REFERENCES Store(store_id)
);

CREATE TABLE Customer (
    customer_id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    street VARCHAR(50),
    city VARCHAR(25),
    state VARCHAR(15),
    zip_code VARCHAR(10),
    customer_type VARCHAR(20) NOT NULL,
    CONSTRAINT chk_customer_type CHECK (customer_type IN ('HOME', 'BUSINESS'))
);

CREATE TABLE HomeCustomer (
    customer_id INT PRIMARY KEY,
    marital_status VARCHAR(10),
    gender VARCHAR(20),
    age INT,
    income DECIMAL(12,2),
    FOREIGN KEY (customer_id) REFERENCES Customer(customer_id)
);

CREATE TABLE BusinessCustomer (
    customer_id INT PRIMARY KEY,
    business_category VARCHAR(50),
    annual_income DECIMAL(15,2),
    FOREIGN KEY (customer_id) REFERENCES Customer(customer_id)
);

CREATE TABLE Category (
    category_id INT PRIMARY KEY,
    category_name VARCHAR(50) NOT NULL,
    description VARCHAR(100)
);

CREATE TABLE Product (
    product_id INT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    brand VARCHAR(50),
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    cost_price DECIMAL(10,2) CHECK (cost_price >= 0),
    category_id INT NOT NULL,
    FOREIGN KEY (category_id) REFERENCES Category(category_id)
);

CREATE TABLE Inventory (
    store_id INT,
    product_id INT,
    quantity INT NOT NULL CHECK (quantity >= 0),
    PRIMARY KEY (store_id, product_id),
    FOREIGN KEY (store_id) REFERENCES Store(store_id),
    FOREIGN KEY (product_id) REFERENCES Product(product_id)
);

CREATE TABLE Orders (
    order_id INT PRIMARY KEY,
    order_date DATE NOT NULL,
    payment_method VARCHAR(30),
    order_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    customer_id INT NOT NULL,
    salesperson_id INT,
    store_id INT NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES Customer(customer_id),
    FOREIGN KEY (salesperson_id) REFERENCES Salesperson(salesperson_id),
    FOREIGN KEY (store_id) REFERENCES Store(store_id),
    CONSTRAINT chk_order_status
        CHECK (order_status IN ('PENDING', 'PAID', 'PROCESSING', 'COMPLETED', 'CANCELLED'))
);

CREATE TABLE OrderItem (
    order_id INT,
    product_id INT,
    quantity INT NOT NULL,
    sale_price DECIMAL(10,2) NOT NULL,
    unit_cost DECIMAL(10,2),
    PRIMARY KEY (order_id, product_id),
    FOREIGN KEY (order_id) REFERENCES Orders(order_id),
    FOREIGN KEY (product_id) REFERENCES Product(product_id)
);

CREATE TABLE UserAccount (
    account_id INT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    customer_id INT UNIQUE,
    salesperson_id INT UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES Customer(customer_id),
    FOREIGN KEY (salesperson_id) REFERENCES Salesperson(salesperson_id),
    CONSTRAINT chk_account_role CHECK (role IN ('CUSTOMER', 'EMPLOYEE', 'ADMIN'))
);

CREATE TABLE Purchase (
    purchase_id INT PRIMARY KEY,
    purchase_date DATE NOT NULL,
    store_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_cost DECIMAL(10,2) NOT NULL CHECK (unit_cost >= 0),
    supplier VARCHAR(50),
    FOREIGN KEY (store_id) REFERENCES Store(store_id),
    FOREIGN KEY (product_id) REFERENCES Product(product_id)
);

CREATE INDEX idx_product_name ON Product(product_name);
CREATE INDEX idx_salesperson_store ON Salesperson(store_id);
CREATE INDEX idx_orders_customer_date ON Orders(customer_id, order_date);
CREATE INDEX idx_orders_status_date ON Orders(order_status, order_date);
CREATE INDEX idx_purchase_cost ON Purchase(product_id, store_id, purchase_date);
