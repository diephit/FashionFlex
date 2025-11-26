-- Fix payment_method column to support VNPAY
-- Run this SQL script in your MySQL database

USE ecommerce_db;

-- Check current column definition
SHOW COLUMNS FROM orders LIKE 'payment_method';

-- Modify the column to use VARCHAR(50) to support longer enum names
ALTER TABLE orders
MODIFY COLUMN payment_method VARCHAR(50) NOT NULL;

-- Verify the change
SHOW COLUMNS FROM orders LIKE 'payment_method';

-- Check if there are any existing orders with payment methods
SELECT DISTINCT payment_method FROM orders;
