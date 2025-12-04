-- ==================================
-- Database: ff (E-commerce + Membership)
-- ==================================
CREATE DATABASE ff;
USE ff;

-- ==========================
-- E-commerce DB Schema
-- ==========================

-- ROLES
CREATE TABLE roles (
    roleID INT AUTO_INCREMENT PRIMARY KEY,
    roleName VARCHAR(50) NOT NULL UNIQUE 
);

-- USERS
CREATE TABLE users (
    userID INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    roleID INT NOT NULL,
    status ENUM('active','inactive') DEFAULT 'active',
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (roleID) REFERENCES roles(roleID)
);

-- MEMBERSHIP LEVELS
CREATE TABLE membership_levels (
    levelID INT AUTO_INCREMENT PRIMARY KEY,
    levelName VARCHAR(50) NOT NULL, -- Bronze, Silver, Gold, Platinum
    minSpent FLOAT NOT NULL,
    discountRate FLOAT DEFAULT 0,
    bonusRate FLOAT DEFAULT 0,
    description TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- CUSTOMERS
CREATE TABLE customers (
    customerID INT AUTO_INCREMENT PRIMARY KEY,
    userID INT NOT NULL,
    phone VARCHAR(20),
    dateOfBirth DATE,
    loyaltyPoints INT DEFAULT 0,
    totalSpent FLOAT DEFAULT 0,
    levelID INT DEFAULT 1, 
    address TEXT,
    customerImg VARCHAR(255),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (userID) REFERENCES users(userID),
    FOREIGN KEY (levelID) REFERENCES membership_levels(levelID)
);

-- ADDRESSES
CREATE TABLE addresses (
    addressID INT AUTO_INCREMENT PRIMARY KEY,
    customerID INT NOT NULL,
    fullName VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    addressLine1 VARCHAR(255) NOT NULL,
    addressLine2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    postalCode VARCHAR(20),
    country VARCHAR(100) DEFAULT 'Vietnam',
    isDefault BOOLEAN DEFAULT FALSE,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customerID) REFERENCES customers(customerID) ON DELETE CASCADE
);

-- ADMINS
CREATE TABLE admins (
    adminID INT AUTO_INCREMENT PRIMARY KEY,
    userID INT NOT NULL,
    department VARCHAR(100),
    position VARCHAR(100),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (userID) REFERENCES users(userID)
);

-- CATEGORIES
CREATE TABLE categories (
    categoryID INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parentCategoryID INT DEFAULT NULL,
    FOREIGN KEY (parentCategoryID) REFERENCES categories(categoryID),
    status ENUM('active','inactive') DEFAULT 'active'
);

-- PRODUCTS
CREATE TABLE products (
    productID INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    categoryID INT NOT NULL,
    mainImage VARCHAR(255),
    stockQuantity INT DEFAULT 0,
    createdByAdminID INT,
    updatedByAdminID INT,
    status ENUM('active', 'inactive') DEFAULT 'active',
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (categoryID) REFERENCES categories(categoryID),
    FOREIGN KEY (createdByAdminID) REFERENCES admins(adminID),
    FOREIGN KEY (updatedByAdminID) REFERENCES admins(adminID)
);

CREATE TABLE product_variants (
    variantID INT AUTO_INCREMENT PRIMARY KEY,
    productID INT NOT NULL,
    sku VARCHAR(100) NOT NULL UNIQUE,
    price DECIMAL(10,2) NOT NULL,
    variantImage VARCHAR(255) DEFAULT NULL, -- optional variant image
    status ENUM('active','inactive') DEFAULT 'active',
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (productID) REFERENCES products(productID)
);

-- LOGICAL STOCKS
CREATE TABLE stocks (
    stockID INT AUTO_INCREMENT PRIMARY KEY,
    variantID INT NOT NULL,
    currentQuantity INT DEFAULT 0,
    changeAmount INT,
    changeType ENUM('import','export','adjust') NOT NULL,
    note TEXT,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    adminID INT,
    FOREIGN KEY (variantID) REFERENCES product_variants(variantID),
    FOREIGN KEY (adminID) REFERENCES admins(adminID)
);

-- CARTS
CREATE TABLE carts (
    cartID INT AUTO_INCREMENT PRIMARY KEY,
    customerID INT DEFAULT NULL,
    sessionID VARCHAR(255) DEFAULT NULL, -- for guest carts
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    status ENUM('active','ordered','abandoned') DEFAULT 'active',
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
    userID INT NULL,
    customerID INT NULL,
    orderDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('pending','paid','shipped','completed','canceled') DEFAULT 'pending',
    totalAmount FLOAT NOT NULL,
    contactEmail VARCHAR(150),
    shippingAddress TEXT,
    customerName VARCHAR(150),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (userID) REFERENCES users(userID),
    FOREIGN KEY (customerID) REFERENCES customers(customerID)
);


-- ORDER ITEMS
CREATE TABLE order_items (
    orderItemID INT AUTO_INCREMENT PRIMARY KEY,
    orderID INT NOT NULL,
    variantID INT NOT NULL,
    quantity INT NOT NULL,
    price FLOAT NOT NULL,
    totalPrice FLOAT NOT NULL,
    selectedSize VARCHAR(10) DEFAULT NULL, 
    FOREIGN KEY (orderID) REFERENCES orders(orderID),
    FOREIGN KEY (variantID) REFERENCES product_variants(variantID)
);
-- PAYMENTS
CREATE TABLE payments (
    paymentID INT AUTO_INCREMENT PRIMARY KEY,
    orderID INT NOT NULL,
    method VARCHAR(50) NOT NULL, -- VNPay, CreditCard, Momo
    amount FLOAT NOT NULL,
    status ENUM('pending','success','failed','refunded') DEFAULT 'pending',
    transactionCode VARCHAR(100),
    paymentDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    paymentDetails TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (orderID) REFERENCES orders(orderID)
);

-- Đồng bộ trạng thái đơn hàng theo kết quả thanh toán:
-- - Nếu payment.status = 'success'  -> orders.status = 'completed'
-- - Nếu payment.status = 'failed' hoặc 'refunded' -> orders.status = 'pending'
DELIMITER //
CREATE TRIGGER trg_payments_after_insert
AFTER INSERT ON payments
FOR EACH ROW
BEGIN
    IF NEW.status = 'success' THEN
        UPDATE orders SET status = 'completed' WHERE orderID = NEW.orderID;
    ELSEIF NEW.status IN ('failed','refunded') THEN
        UPDATE orders SET status = 'pending' WHERE orderID = NEW.orderID;
    END IF;
END//

CREATE TRIGGER trg_payments_after_update
AFTER UPDATE ON payments
FOR EACH ROW
BEGIN
    IF NEW.status = 'success' THEN
        UPDATE orders SET status = 'completed' WHERE orderID = NEW.orderID;
    ELSEIF NEW.status IN ('failed','refunded') THEN
        UPDATE orders SET status = 'pending' WHERE orderID = NEW.orderID;
    END IF;
END//
DELIMITER ;

-- OPTIONAL: Membership history
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
CREATE TABLE wishlist (
    wishlistID INT AUTO_INCREMENT PRIMARY KEY,
    customerID INT NOT NULL,                             -- người dùng sở hữu wishlist
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customerID) REFERENCES customers(customerID)
);
CREATE TABLE wishlist_items (
    wishlistItemID INT AUTO_INCREMENT PRIMARY KEY,
    wishlistID INT NOT NULL,
    productID INT NOT NULL,
    variantID INT DEFAULT NULL,                          
    addedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (wishlistID) REFERENCES wishlist(wishlistID),
    FOREIGN KEY (productID) REFERENCES products(productID),
    FOREIGN KEY (variantID) REFERENCES product_variants(variantID)
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
INSERT INTO users (name, email, password, roleID) VALUES
('Alice', 'alice@gmail.com', 'hash1', 1),
('Bob', 'bob@gmail.com', 'hash2', 2);

-- ADMINS
INSERT INTO admins (userID, department, position) VALUES (2, 'Operations', 'System Admin');

-- CUSTOMERS
INSERT INTO customers (userID, phone, totalSpent, levelID, address,customerImg)
VALUES (1, '0123456789', 300.00, 1, '123 Main St','static/images/customer1.jpg');


INSERT INTO categories (categoryID, name, parentCategoryID) VALUES
(1, 'Fashion', NULL),
(2, 'Men', 1),
(3, 'Women', 1),
(4, 'Kids', 1),
(5, 'Tops', 2),
(6, 'Bottoms', 2),
(7, 'Dresses', 3),
(8, 'Shoes', 2),
(9, 'Shoes', 3),
(10, 'T-Shirts', 5),
(11, 'Shirts', 5),
(12, 'Jeans', 6),
(13, 'Shorts', 6),
(14, 'Gowns', 7),
(15, 'Skirts', 7),
(16, 'Accessories', NULL),
(17, 'Bags', 16),
(18, 'Belts', 16),
(19, 'Watches', 16),
(20, 'Jewelry', 16),
(21, 'Tops', 4),
(22, 'Bottoms', 4),
(23, 'Shoes', 4);
-- Men -> T-Shirts
INSERT INTO products (name, description, categoryID, mainImage, stockQuantity, createdByAdminID, updatedByAdminID, status)
VALUES
('Classic Cotton T-Shirt', 'Soft cotton T-shirt with relaxed fit', 10, 'static/images/tshirt1.jpg', 100, 1, 1, 'active'),
('Graphic Tee', 'Printed graphic T-shirt for casual wear', 10, 'static/images/tshirt2.jpg', 100, 1, 1, 'active');

-- Men -> Polo
INSERT INTO products (name, description, categoryID, mainImage, stockQuantity, createdByAdminID, updatedByAdminID, status)
VALUES
('Formal Polo', 'Slim-fit formal polo for office', 11, 'static/images/polo1.jpg', 100, 1, 1, 'active'),
('Casual Plaid Polo', 'Comfortable plaid polo for daily wear', 11, 'static/images/polo2.jpg', 100, 1, 1, 'active');

-- Men -> Jeans
INSERT INTO products (name, description, categoryID, mainImage, stockQuantity, createdByAdminID, updatedByAdminID, status)
VALUES
('Slim Fit Jeans', 'Blue denim with stretch material', 12, 'static/images/jeans1.jpg', 100, 1, 1, 'active'),
('Relaxed Fit Jeans', 'Casual denim for everyday comfort', 12, 'static/images/jeans2.jpg', 100, 1, 1, 'active');


-- Men -> Shoes
INSERT INTO products (name, description, categoryID, mainImage, stockQuantity, createdByAdminID, updatedByAdminID, status)
VALUES
('Running Sneakers', 'Breathable sneakers for running', 8, 'static/images/sneakers1.jpg', 100, 1, 1, 'active'),
('Leather Boots', 'Durable leather boots', 8, 'static/images/boots1.jpg', 100, 1, 1, 'active');

-- Women -> Gowns
INSERT INTO products (name, description, categoryID, mainImage, stockQuantity, createdByAdminID, updatedByAdminID, status)
VALUES
('Evening Gown', 'Elegant evening gown for formal events', 14, 'static/images/gown1.jpg', 100, 1, 1, 'active'),
('Cocktail Dress', 'Stylish cocktail dress', 14, 'static/images/gown2.jpg', 100, 1, 1, 'active');

-- Women -> Skirts
INSERT INTO products (name, description, categoryID, mainImage, stockQuantity, createdByAdminID, updatedByAdminID, status)
VALUES
('Pleated Skirt', 'Comfortable pleated skirt', 15, 'static/images/skirt1.jpg', 100, 1, 1, 'active');

-- Women -> Shoes
INSERT INTO products (name, description, categoryID, mainImage, stockQuantity, createdByAdminID, updatedByAdminID, status)
VALUES
('High Heels', 'Elegant high heels for formal occasions', 9, 'static/images/heels1.jpg', 100, 1, 1, 'active');

-- Accessories -> Bags
INSERT INTO products (name, description, categoryID, mainImage, stockQuantity, createdByAdminID, updatedByAdminID, status)
VALUES
('Leather Backpack', 'Spacious backpack for everyday use', 17, 'static/images/backpack1.jpg', 100, 1, 1, 'active'),
('Canvas Tote Bag', 'Casual tote bag', 17, 'static/images/totebag1.jpg', 100, 1, 1, 'active');

-- Accessories -> Belts
INSERT INTO products (name, description, categoryID, mainImage, stockQuantity, createdByAdminID, updatedByAdminID, status)
VALUES
('Men Leather Belt', 'Genuine leather belt for men', 18, 'static/images/belt1.jpg', 100, 1, 1, 'active');

-- Accessories -> Watches
INSERT INTO products (name, description, categoryID, mainImage, stockQuantity, createdByAdminID, updatedByAdminID, status)
VALUES
('Smart Watch', 'Track activity and heart rate', 19, 'static/images/smartwatch1.jpg', 100, 1, 1, 'active'),
('Regular Watch','To see what time is it now duh',19,'static/images/regularwatch1.jpg',100,1,1,'active');

-- Accessories -> Jewelry
INSERT INTO products (name, description, categoryID, mainImage, stockQuantity, createdByAdminID, updatedByAdminID, status)
VALUES
('Silver Necklace', 'Elegant silver necklace for women', 20, 'static/images/necklace1.jpg', 100, 1, 1, 'active');

-- Kids -> Tops
INSERT INTO products (name, description, categoryID, mainImage, stockQuantity, createdByAdminID, updatedByAdminID, status)
VALUES
('Kids Cotton T-Shirt', 'Soft cotton T-shirt for kids, comfortable fit', 21, 'static/images/kids_tshirt1.jpg', 100, 1, 1, 'active'),
('Cartoon Tee', 'Printed cartoon T-shirt for playful look', 21, 'static/images/kids_tshirt2.jpg', 100, 1, 1, 'active');

-- Kids -> Bottoms
INSERT INTO products (name, description, categoryID, mainImage, stockQuantity, createdByAdminID, updatedByAdminID, status)
VALUES
('Kids Casual Shorts', 'Comfortable shorts for daily wear', 22, 'static/images/kids_shorts1.jpg', 100, 1, 1, 'active');

-- Kids -> Shoes
INSERT INTO products (name, description, categoryID, mainImage, stockQuantity, createdByAdminID, updatedByAdminID, status)
VALUES
('Kids Sneakers', 'Lightweight sneakers for children', 23, 'static/images/kids_sneakers1.jpg', 100, 1, 1, 'active'),
('Kids Sandals', 'Breathable sandals for summer', 23, 'static/images/kids_sandals1.jpg', 100, 1, 1, 'active');

-- Men -> T-Shirts
INSERT INTO product_variants (productID, sku, price, variantImage, status)
VALUES
(1, 'TSHIRT1-Blue', 15.99, 'static/images/tshirt1_blue.jpg', 'active'),
(1, 'TSHIRT1-White', 15.99, 'static/images/tshirt1_white.jpg', 'active'),
(1, 'TSHIRT1-Black', 17.99, 'static/images/tshirt1_black.jpg', 'active'),
(1, 'TSHIRT1-Gray', 17.99, 'static/images/tshirt1_gray.jpg', 'active'),
(2, 'GT-Fallen', 20.99, 'static/images/tshirt2_fallen.jpg', 'active'),
(2, 'GT-Meteora', 20.99, 'static/images/tshirt2_meteora.jpg', 'active');

-- Men -> Polo
INSERT INTO product_variants (productID, sku, price, variantImage, status)
VALUES
(3, 'POLO1-Navy', 19.99, 'static/images/polo1_navy.jpg', 'active'),
(3, 'POLO1-White', 19.99, 'static/images/polo1_white.jpg', 'active'),
(3, 'POLO1-Black', 18.99, 'static/images/polo1_black.jpg', 'active'),
(4, 'POLO2-Green', 18.99, 'static/images/polo2_green.jpg', 'active'),
(4, 'POLO2-Blue', 18.99, 'static/images/polo2_blue.jpg', 'active');

-- Men -> Jeans
INSERT INTO product_variants (productID, sku, price, variantImage, status)
VALUES
(5, 'JEANS1-Blue', 29.99, 'static/images/jeans1_blue.jpg', 'active'),
(5, 'JEANS1-DarkBlue', 29.99, 'static/images/jeans1_darkblue.jpg', 'active'),
(6, 'JEANS2-LightBlue', 27.99, 'static/images/jeans2_lightblue.jpg', 'active'),
(6, 'JEANS2-Black', 27.99, 'static/images/jeans2_black.jpg', 'active');

-- Men -> Shorts
INSERT INTO product_variants (productID, sku, price, variantImage, status)
VALUES
(7, 'SHORTS1-Khaki', 22.99, 'static/images/shorts1_khaki.jpg', 'active'),
(7, 'SHORTS1-Navy', 22.99, 'static/images/shorts1_navy.jpg', 'active');

-- Men -> Shoes
INSERT INTO product_variants (productID, sku, price, variantImage, status)
VALUES
(8, 'SNEAKERS1-White', 49.99, 'static/images/sneakers1_white.jpg', 'active'),
(8, 'SNEAKERS1-Black', 49.99, 'static/images/sneakers1_black.jpg', 'active'),
(9, 'BOOTS1-Brown', 79.99, 'static/images/boots1_brown.jpg', 'active');

-- Women -> Gowns
INSERT INTO product_variants (productID, sku, price, variantImage, status)
VALUES
(10, 'GOWN1-Red', 99.99, 'static/images/gown1_red.jpg', 'active'),
(10, 'GOWN1-Blue', 99.99, 'static/images/gown1_blue.jpg', 'active'),
(11, 'DRESS2-Black', 89.99, 'static/images/dress2_black.jpg', 'active');

-- Women -> Skirts
INSERT INTO product_variants (productID, sku, price, variantImage, status)
VALUES
(12, 'SKIRT1-Black', 39.99, 'static/images/skirt1_black.jpg', 'active'),
(12, 'SKIRT1-Blue', 39.99, 'static/images/skirt1_blue.jpg', 'active');

-- Women -> Shoes
INSERT INTO product_variants (productID, sku, price, variantImage, status)
VALUES
(13, 'HEELS1-Red', 59.99, 'static/images/heels1_red.jpg', 'active'),
(13, 'HEELS1-White', 59.99, 'static/images/heels1_white.jpg', 'active'),
(13, 'HEELS1-Black', 59.99, 'static/images/heels1_black.jpg', 'active');

-- Accessories -> Bags
INSERT INTO product_variants (productID, sku, price, variantImage, status)
VALUES
(14, 'BACKPACK1-Black', 49.99, 'static/images/backpack1_black.jpg', 'active'),
(14, 'BACKPACK1-Brown', 49.99, 'static/images/backpack1_brown.jpg', 'active'),
(15, 'TOTEBAG1-Beige', 29.99, 'static/images/totebag1_beige.jpg', 'active');

-- Accessories -> Belts
INSERT INTO product_variants (productID, sku, price, variantImage, status)
VALUES
(16, 'BELT1-Brown', 19.99, 'static/images/belt1_brown.jpg', 'active'),
(16, 'BELT1-Black', 19.99, 'static/images/belt1_black.jpg', 'active');

-- Accessories -> Watches
INSERT INTO product_variants (productID, sku, price, variantImage, status)
VALUES
(17, 'Apple Watch-Series 9', 129.99, 'static/images/smartwatch1_apple.jpg', 'active'),
(17, 'Garmin Venu 3S', 129.99, 'static/images/smartwatch1_garmin.jpg', 'active'),
(18, 'WATCH2-Brown', 99.99, 'static/images/regularwatch1_brown.jpg', 'active'),
(18, 'WATCH2-Silver', 79.99, 'static/images/regularwatch1_silver.jpg', 'active'),
(18, 'WATCH2-Black', 99.99, 'static/images/regularwatch1_black.jpg', 'active');

-- Accessories -> Jewelry
INSERT INTO product_variants (productID, sku, price, variantImage, status)
VALUES
(19, 'Dancing Swan', 49.99, 'static/images/necklace1_swan.jpg', 'active'),
(19, 'Genma Pendant', 19.99, 'static/images/necklace1_genma.jpg', 'active'),
(19, 'Chroma Pendant', 19.99, 'static/images/necklace1_chroma.jpg', 'active');

-- Kids -> Tops
INSERT INTO product_variants (productID, sku, price, variantImage, status)
VALUES
(20, 'KIDTSHIRT1-Blue', 12.99, 'static/images/kids_tshirt1_blue.jpg', 'active'),
(20, 'KIDTSHIRT1-Red', 12.99, 'static/images/kids_tshirt1_red.jpg', 'active'),
(21, 'Cartoon Tee Mickey', 13.99, 'static/images/kids_tshirt2_mickey.jpg', 'active'),
(21, 'Cartoon Tee Donald', 13.99, 'static/images/kids_tshirt2_donald.jpg', 'active');

-- Kids -> Bottoms
INSERT INTO product_variants (productID, sku, price, variantImage, status)
VALUES
(22, 'KIDSHORTS1-Blue', 14.99, 'static/images/kids_shorts1_blue.jpg', 'active'),
(22, 'KIDSHORTS1-Khaki', 14.99, 'static/images/kids_shorts1_khaki.jpg', 'active');

-- Kids -> Shoes
INSERT INTO product_variants (productID, sku, price, variantImage, status)
VALUES
(23, 'KIDS_SNEAKERS1-White', 24.99, 'static/images/kids_sneakers1_white.jpg', 'active'),
(23, 'KIDS_SNEAKERS1-Blue', 24.99, 'static/images/kids_sneakers1_blue.jpg', 'active'),
(24, 'KIDS_SANDALS1-Pink', 19.99, 'static/images/kids_sandals1_pink.jpg', 'active'),
(24, 'KIDS_SANDALS1-Blue', 19.99, 'static/images/kids_sandals1_blue.jpg', 'active');


