-- Sample Brands
INSERT INTO brands (name, description, active, created_at, updated_at) VALUES
('Nike', 'Leading sportswear and athletic brand', true, NOW(), NOW()),
('Adidas', 'German multinational sportswear brand', true, NOW(), NOW()),
('Zara', 'Spanish fast fashion brand', true, NOW(), NOW()),
('H&M', 'Swedish multinational clothing retailer', true, NOW(), NOW()),
('Gucci', 'Italian luxury fashion brand', true, NOW(), NOW()),
('Puma', 'German athletic and casual footwear brand', true, NOW(), NOW()),
('Levi''s', 'American denim and clothing brand', true, NOW(), NOW()),
('Uniqlo', 'Japanese casual wear designer and retailer', true, NOW(), NOW());

-- Sample Products (assuming categories with IDs 1-6 already exist: Women, Men, Bag, Shoes, Watches, Accessories)
-- Women's Products
INSERT INTO products (name, description, price, discount_price, stock, sku, image_url, category_id, brand_id, active, featured, created_at, updated_at) VALUES
('Women Summer Dress', 'Beautiful floral summer dress perfect for warm weather', 49.99, 39.99, 50, 'WSD-001', '/images/products/dress-001.jpg', 1, 3, true, true, NOW(), NOW()),
('Ladies Casual T-Shirt', 'Comfortable cotton t-shirt for everyday wear', 19.99, NULL, 100, 'LCT-002', '/images/products/tshirt-women-001.jpg', 1, 4, true, false, NOW(), NOW()),
('Women Yoga Pants', 'High-quality stretchy yoga pants', 34.99, 29.99, 75, 'WYP-003', '/images/products/yoga-pants-001.jpg', 1, 1, true, true, NOW(), NOW());

-- Men's Products
INSERT INTO products (name, description, price, discount_price, stock, sku, image_url, category_id, brand_id, active, featured, created_at, updated_at) VALUES
('Men Casual Shirt', 'Classic casual shirt for men', 39.99, NULL, 60, 'MCS-004', '/images/products/shirt-men-001.jpg', 2, 4, true, false, NOW(), NOW()),
('Men Sports T-Shirt', 'Breathable sports t-shirt', 24.99, 19.99, 80, 'MST-005', '/images/products/sports-tshirt-001.jpg', 2, 2, true, true, NOW(), NOW()),
('Men Denim Jeans', 'Classic blue denim jeans', 59.99, 49.99, 40, 'MDJ-006', '/images/products/jeans-001.jpg', 2, 7, true, false, NOW(), NOW());

-- Bags
INSERT INTO products (name, description, price, discount_price, stock, sku, image_url, category_id, brand_id, active, featured, created_at, updated_at) VALUES
('Leather Backpack', 'Premium leather backpack for daily use', 89.99, NULL, 30, 'LBP-007', '/images/products/backpack-001.jpg', 3, 5, true, true, NOW(), NOW()),
('Women Handbag', 'Elegant handbag for formal occasions', 129.99, 99.99, 25, 'WHB-008', '/images/products/handbag-001.jpg', 3, 5, true, true, NOW(), NOW()),
('Travel Duffle Bag', 'Large capacity travel bag', 69.99, NULL, 35, 'TDB-009', '/images/products/duffle-001.jpg', 3, 1, true, false, NOW(), NOW());

-- Shoes
INSERT INTO products (name, description, price, discount_price, stock, sku, image_url, category_id, brand_id, active, featured, created_at, updated_at) VALUES
('Running Shoes', 'Professional running shoes with comfort fit', 79.99, 69.99, 50, 'RS-010', '/images/products/running-shoes-001.jpg', 4, 1, true, true, NOW(), NOW()),
('Casual Sneakers', 'Stylish casual sneakers for everyday wear', 59.99, NULL, 65, 'CS-011', '/images/products/sneakers-001.jpg', 4, 2, true, false, NOW(), NOW()),
('Formal Shoes', 'Classic formal shoes for business', 99.99, 89.99, 30, 'FS-012', '/images/products/formal-shoes-001.jpg', 4, 5, true, false, NOW(), NOW());

-- Watches
INSERT INTO products (name, description, price, discount_price, stock, sku, image_url, category_id, brand_id, active, featured, created_at, updated_at) VALUES
('Smart Watch Pro', 'Advanced smartwatch with fitness tracking', 199.99, 179.99, 40, 'SWP-013', '/images/products/smartwatch-001.jpg', 5, 1, true, true, NOW(), NOW()),
('Classic Analog Watch', 'Elegant analog watch with leather strap', 149.99, NULL, 20, 'CAW-014', '/images/products/analog-watch-001.jpg', 5, 5, true, false, NOW(), NOW()),
('Sports Digital Watch', 'Durable digital watch for sports', 79.99, 69.99, 45, 'SDW-015', '/images/products/digital-watch-001.jpg', 5, 2, true, false, NOW(), NOW());

-- Sample Product Sizes
INSERT INTO product_sizes (product_id, size) VALUES
(1, 'S'), (1, 'M'), (1, 'L'), (1, 'XL'),
(2, 'S'), (2, 'M'), (2, 'L'), (2, 'XL'), (2, 'XXL'),
(3, 'XS'), (3, 'S'), (3, 'M'), (3, 'L'),
(4, 'M'), (4, 'L'), (4, 'XL'), (4, 'XXL'),
(5, 'S'), (5, 'M'), (5, 'L'), (5, 'XL'),
(6, '28'), (6, '30'), (6, '32'), (6, '34'), (6, '36'),
(10, '7'), (10, '8'), (10, '9'), (10, '10'), (10, '11'),
(11, '7'), (11, '8'), (11, '9'), (11, '10'),
(12, '8'), (12, '9'), (12, '10'), (12, '11');

-- Sample Product Colors
INSERT INTO product_colors (product_id, color) VALUES
(1, 'Red'), (1, 'Blue'), (1, 'White'),
(2, 'Black'), (2, 'White'), (2, 'Gray'), (2, 'Navy'),
(3, 'Black'), (3, 'Gray'), (3, 'Purple'),
(4, 'White'), (4, 'Blue'), (4, 'Black'),
(5, 'Red'), (5, 'Blue'), (5, 'Green'),
(6, 'Blue'), (6, 'Black'),
(7, 'Brown'), (7, 'Black'),
(8, 'Black'), (8, 'Red'), (8, 'Brown'),
(9, 'Black'), (9, 'Navy'),
(10, 'Black'), (10, 'White'), (10, 'Blue'),
(11, 'White'), (11, 'Black'), (11, 'Gray'),
(12, 'Black'), (12, 'Brown'),
(13, 'Black'), (13, 'Silver'),
(14, 'Gold'), (14, 'Silver'),
(15, 'Black'), (15, 'Blue');