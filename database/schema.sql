
CREATE DATABASE IF NOT EXISTS grocery_store;

USE grocery_store;

CREATE TABLE Customer (
    customer_id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    street VARCHAR(50),
    city VARCHAR(25),
    state VARCHAR(15),
    zip_code VARCHAR(10),
);

CREATE TABLE HomeCustomer (
    customer_id INT PRIMARY KEY,
    marital_status VARCHAR(10),
    gender VARCHAR(20),
    age INT,
    income DECIMAL(12,2),

    FOREIGN KEY (customer_id)
        REFERENCES Customer(customer_id)
);

CREATE TABLE BusinessCustomer (
    customer_id INT PRIMARY KEY,
    business_category VARCHAR(50),
    annual_income DECIMAL(15,2),

    FOREIGN KEY (customer_id)
        REFERENCES Customer(customer_id)
);

CREATE TABLE Region (
    region_id INT PRIMARY KEY,
    region_name VARCHAR(50) NOT NULL,
);

CREATE TABLE Store (
    store_id INT PRIMARY KEY,
    street VARCHAR(50) NOT NULL,
    city VARCHAR(25) NOT NULL,
    state VARCHAR(15) NOT NULL,
    zip_code VARCHAR(10) NOT NULL,
    region_id INT NOT NULL,

    FOREIGN KEY (region_id)
        REFERENCES Region(region_id)
);

CREATE TABLE Employee (
    employee_id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    street VARCHAR(50),
    city VARCHAR(25),
    state VARCHAR(15),
    zip_code VARCHAR(10),
    email VARCHAR(50),
    job_title VARCHAR(50),
    salary DECIMAL(10,2),
    store_id INT NOT NULL,

    FOREIGN KEY (store_id)
        REFERENCES Store(store_id)
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
    price DECIMAL(10,2) NOT NULL,
    category_id INT NOT NULL,

    FOREIGN KEY (category_id)
        REFERENCES Category(category_id)
);

CREATE TABLE Inventory (
    store_id INT,
    product_id INT,
    quantity INT NOT NULL,

    PRIMARY KEY (store_id, product_id),

    FOREIGN KEY (store_id)
        REFERENCES Store(store_id),

    FOREIGN KEY (product_id)
        REFERENCES Product(product_id)
);

CREATE TABLE Orders (
    order_id INT PRIMARY KEY,
    order_date DATE NOT NULL,
    payment_method VARCHAR(30),
    customer_id INT NOT NULL,
    employee_id INT,
    store_id INT NOT NULL,

    FOREIGN KEY (customer_id)
        REFERENCES Customer(customer_id),

    FOREIGN KEY (employee_id)
        REFERENCES Employee(employee_id),

    FOREIGN KEY (store_id)
        REFERENCES Store(store_id)
);

CREATE TABLE OrderItem (
    order_id INT,
    product_id INT,
    quantity INT NOT NULL,
    sale_price DECIMAL(10,2) NOT NULL,

    PRIMARY KEY (order_id, product_id),

    FOREIGN KEY (order_id)
        REFERENCES Orders(order_id),

    FOREIGN KEY (product_id)
        REFERENCES Product(product_id)
);
