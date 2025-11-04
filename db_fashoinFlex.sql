-- ==================================
-- Database: ff (E-commerce + Membership)
-- ==================================
CREATE DATABASE ecommerce_db;
USE ecommerce_db;

-- ==================================
-- TABLES
-- ==================================

-- ==================================
-- E-commerce Data Model (Final + Membership + Payment History)
-- ==================================

-- USERS
CREATE TABLE users (
  userID INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(100) UNIQUE NOT NULL,
  passwordHash VARCHAR(255) NOT NULL,
  roleID INT NOT NULL,
  status ENUM('active', 'inactive') DEFAULT 'active',
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ROLES
CREATE TABLE roles (
  roleID INT AUTO_INCREMENT PRIMARY KEY,
  roleName ENUM('customer', 'admin') NOT NULL
);

-- MEMBERSHIP LEVELS
CREATE TABLE membership_levels (
  levelID INT AUTO_INCREMENT PRIMARY KEY,
  levelName VARCHAR(50) NOT NULL,
  minSpent DECIMAL(12,2) NOT NULL,
  discountRate DECIMAL(5,2) NOT NULL,
  bonusRate DECIMAL(5,2) NOT NULL,
  description TEXT,
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- MEMBERSHIP HISTORY (log khi lên/xuống cấp)
CREATE TABLE membership_history (
  historyID INT AUTO_INCREMENT PRIMARY KEY,
  customerID INT NOT NULL,
  oldLevelID INT,
  newLevelID INT,
  changedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  note TEXT,
  FOREIGN KEY (customerID) REFERENCES customers(customerID),
  FOREIGN KEY (oldLevelID) REFERENCES membership_levels(levelID),
  FOREIGN KEY (newLevelID) REFERENCES membership_levels(levelID)
);

-- CUSTOMERS
CREATE TABLE customers (
  customerID INT AUTO_INCREMENT PRIMARY KEY,
  userID INT UNIQUE,
  phone VARCHAR(20),
  dateOfBirth DATE,
  loyaltyPoints INT DEFAULT 0,
  totalSpent DECIMAL(12,2) DEFAULT 0,
  levelID INT,
  address TEXT,
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (userID) REFERENCES users(userID),
  FOREIGN KEY (levelID) REFERENCES membership_levels(levelID)
);

-- ADMINS
CREATE TABLE admins (
  adminID INT AUTO_INCREMENT PRIMARY KEY,
  userID INT UNIQUE,
  department VARCHAR(100),
  position VARCHAR(100),
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (userID) REFERENCES users(userID)
);

-- CATEGORIES
CREATE TABLE categories (
  categoryID INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  parentCategoryID INT,
  FOREIGN KEY (parentCategoryID) REFERENCES categories(categoryID)
);

-- PRODUCTS
CREATE TABLE products (
  productID INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(150) NOT NULL,
  description TEXT,
  categoryID INT,
  createdByAdminID INT,
  updatedByAdminID INT,
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (categoryID) REFERENCES categories(categoryID),
  FOREIGN KEY (createdByAdminID) REFERENCES admins(adminID),
  FOREIGN KEY (updatedByAdminID) REFERENCES admins(adminID)
);

-- PRODUCT VARIANTS
CREATE TABLE product_variants (
  variantID INT AUTO_INCREMENT PRIMARY KEY,
  productID INT NOT NULL,
  sku VARCHAR(100) UNIQUE NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  size VARCHAR(50),
  color VARCHAR(50),
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (productID) REFERENCES products(productID)
);

-- STOCKS (logical stock)
CREATE TABLE stocks (
  stockID INT AUTO_INCREMENT PRIMARY KEY,
  variantID INT NOT NULL,
  currentQuantity INT NOT NULL,
  changeAmount INT NOT NULL,
  changeType ENUM('import', 'export', 'adjust'),
  note TEXT,
  updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  adminID INT,
  FOREIGN KEY (variantID) REFERENCES product_variants(variantID),
  FOREIGN KEY (adminID) REFERENCES admins(adminID)
);

-- CARTS
CREATE TABLE carts (
  cartID INT AUTO_INCREMENT PRIMARY KEY,
  customerID INT,
  sessionID VARCHAR(255),
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  status ENUM('active', 'ordered', 'abandoned') DEFAULT 'active',
  FOREIGN KEY (customerID) REFERENCES customers(customerID)
);

-- CART ITEMS
CREATE TABLE cart_items (
  cartItemID INT AUTO_INCREMENT PRIMARY KEY,
  cartID INT NOT NULL,
  variantID INT NOT NULL,
  quantity INT NOT NULL,
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (cartID) REFERENCES carts(cartID),
  FOREIGN KEY (variantID) REFERENCES product_variants(variantID)
);

-- ORDERS
CREATE TABLE orders (
  orderID INT AUTO_INCREMENT PRIMARY KEY,
  customerID INT NOT NULL,
  orderDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  status ENUM('pending', 'paid', 'shipped', 'completed', 'canceled') DEFAULT 'pending',
  totalAmount DECIMAL(12,2),
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (customerID) REFERENCES customers(customerID)
);

-- ORDER ITEMS
CREATE TABLE order_items (
  orderItemID INT AUTO_INCREMENT PRIMARY KEY,
  orderID INT NOT NULL,
  variantID INT NOT NULL,
  quantity INT NOT NULL,
  price DECIMAL(12,2),
  totalPrice DECIMAL(12,2),
  FOREIGN KEY (orderID) REFERENCES orders(orderID),
  FOREIGN KEY (variantID) REFERENCES product_variants(variantID)
);

-- PAYMENTS
CREATE TABLE payments (
  paymentID INT AUTO_INCREMENT PRIMARY KEY,
  orderID INT NOT NULL,
  method ENUM('VNPay', 'CreditCard', 'Momo') NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  status ENUM('pending', 'success', 'failed', 'refunded') DEFAULT 'pending',
  transactionCode VARCHAR(100),
  paymentDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  paymentDetails JSON,
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (orderID) REFERENCES orders(orderID)
);

-- PAYMENT HISTORY
CREATE TABLE payment_history (
  historyID INT AUTO_INCREMENT PRIMARY KEY,
  paymentID INT NOT NULL,
  status ENUM('pending', 'success', 'failed', 'refunded'),
  changedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  note TEXT,
  gatewayResponse JSON,
  FOREIGN KEY (paymentID) REFERENCES payments(paymentID)
);

-- ==================================
-- SAMPLE DATA
-- ==================================

-- ROLES
INSERT INTO roles (roleName) VALUES ('customer'), ('admin');

-- MEMBERSHIP LEVELS
INSERT INTO membership_levels (levelName, minSpent, discountRate, bonusRate, description)
VALUES
('Bronze', 0, 0.00, 1.00, 'Cấp mặc định cho người mới'),
('Silver', 500, 2.00, 1.50, 'Tăng 2% giảm giá và 1.5x điểm thưởng'),
('Gold', 1500, 5.00, 2.00, 'Giảm 5%, thưởng 2x'),
('Platinum', 5000, 10.00, 3.00, 'Khách hàng thân thiết cao cấp');

-- USERS
INSERT INTO users (name, email, passwordHash, roleID) VALUES
('Alice', 'alice@mail.com', 'hash1', 1),
('Bob', 'bob@mail.com', 'hash2', 2);

-- ADMINS
INSERT INTO admins (userID, department, position) VALUES (2, 'Operations', 'System Admin');

-- CUSTOMERS
INSERT INTO customers (userID, phone, totalSpent, levelID, address)
VALUES (1, '0123456789', 300.00, 1, '123 Main St');

-- CATEGORIES
INSERT INTO categories (name) VALUES ('Electronics'), ('Clothing');

-- PRODUCTS
INSERT INTO products (name, description, categoryID, createdByAdminID)
VALUES ('Smartphone', 'Latest smartphone with 5G', 1, 1),
('T-Shirt', 'Cotton T-shirt', 2, 1);

-- PRODUCT VARIANTS
INSERT INTO product_variants (productID, sku, price, size, color)
VALUES (1, 'SPH-001', 500.00, NULL, 'Black'),
(2, 'TSH-001', 20.00, 'M', 'White');

-- STOCKS
INSERT INTO stocks (variantID, currentQuantity, changeAmount, changeType, note, adminID)
VALUES (1, 50, 50, 'import', 'Initial stock', 1),
(2, 100, 100, 'import', 'Initial stock', 1);

-- ORDERS
INSERT INTO orders (customerID, status, totalAmount)
VALUES (1, 'paid', 520.00);

-- ORDER ITEMS
INSERT INTO order_items (orderID, variantID, quantity, price, totalPrice)
VALUES (1, 1, 1, 500.00, 500.00),
(1, 2, 1, 20.00, 20.00);

-- PAYMENTS
INSERT INTO payments (orderID, method, amount, status, transactionCode)
VALUES (1, 'VNPay', 520.00, 'success', 'VN123456789');

-- PAYMENT HISTORY
INSERT INTO payment_history (paymentID, status, note)
VALUES (1, 'success', 'Payment confirmed via VNPay');

