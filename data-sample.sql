-- ============================================
-- FashionFlex Sample Data Script
-- ============================================

-- Insert Categories
INSERT INTO categories (id, name, description, active, created_at, updated_at) VALUES
(1, 'Women', 'Women fashion collection', true, NOW(), NOW()),
(2, 'Men', 'Men fashion collection', true, NOW(), NOW()),
(3, 'Bags', 'Bags and accessories', true, NOW(), NOW()),
(4, 'Shoes', 'Footwear collection', true, NOW(), NOW()),
(5, 'Watches', 'Watches and timepieces', true, NOW(), NOW()),
(6, 'Accessories', 'Fashion accessories', true, NOW(), NOW());

-- Insert Brands
INSERT INTO brands (id, name, description, logo_url, active, created_at, updated_at) VALUES
(1, 'Nike', 'Just Do It - Athletic wear and sportswear', '/images/brands/550213b1-68e2-4caf-9cbc-336b63babf81.png', true, NOW(), NOW()),
(2, 'Adidas', 'Impossible is Nothing - Sports fashion', '/images/brands/550213b1-68e2-4caf-9cbc-336b63babf81.png', true, NOW(), NOW()),
(3, 'Zara', 'Fast fashion retailer', '/images/brands/550213b1-68e2-4caf-9cbc-336b63babf81.png', true, NOW(), NOW()),
(4, 'H&M', 'Affordable fashion for everyone', '/images/brands/550213b1-68e2-4caf-9cbc-336b63babf81.png', true, NOW(), NOW()),
(5, 'Gucci', 'Luxury Italian fashion house', '/images/brands/550213b1-68e2-4caf-9cbc-336b63babf81.png', true, NOW(), NOW()),
(6, 'Puma', 'Forever Faster - Athletic brand', '/images/brands/550213b1-68e2-4caf-9cbc-336b63babf81.png', true, NOW(), NOW());

-- Insert Products
INSERT INTO products (id, name, description, price, discount_price, stock, sku, image_url, category_id, brand_id, active, featured, view_count, sold_count, created_at, updated_at) VALUES
(1, 'Vintage Floral Print Dress', 'Beautiful vintage-inspired floral print dress perfect for spring and summer occasions', 89.99, 69.99, 50, 'DRESS-001', '/images/product-01.jpg', 1, 3, true, true, 120, 25, NOW(), NOW()),
(2, 'Classic White T-Shirt', 'Premium cotton white t-shirt with comfortable fit', 29.99, NULL, 100, 'TSHIRT-001', '/images/product-02.jpg', 2, 1, true, true, 350, 89, NOW(), NOW()),
(3, 'Denim Skinny Jeans', 'Stylish skinny jeans with stretch fabric for ultimate comfort', 79.99, 59.99, 75, 'JEANS-001', '/images/product-03.jpg', 1, 4, true, false, 200, 45, NOW(), NOW()),
(4, 'Leather Crossbody Bag', 'Elegant leather crossbody bag with adjustable strap', 129.99, NULL, 30, 'BAG-001', '/images/product-04.jpg', 3, 5, true, true, 180, 32, NOW(), NOW()),
(5, 'Running Sneakers', 'Lightweight running sneakers with cushioned sole', 119.99, 99.99, 60, 'SHOES-001', '/images/product-05.jpg', 4, 1, true, true, 420, 78, NOW(), NOW()),
(6, 'Wool Blend Coat', 'Elegant wool blend coat for cold weather', 199.99, 149.99, 25, 'COAT-001', '/images/product-06.jpg', 1, 3, true, true, 95, 18, NOW(), NOW()),
(7, 'Casual Cotton Shirt', 'Casual cotton shirt perfect for everyday wear', 49.99, NULL, 80, 'SHIRT-001', '/images/product-07.jpg', 2, 4, true, false, 145, 38, NOW(), NOW()),
(8, 'Summer Maxi Dress', 'Flowing maxi dress ideal for beach and summer parties', 99.99, 79.99, 45, 'DRESS-002', '/images/product-08.jpg', 1, 3, true, true, 210, 52, NOW(), NOW()),
(9, 'Leather Wallet', 'Genuine leather wallet with multiple card slots', 49.99, 39.99, 120, 'WALLET-001', '/images/product-09.jpg', 6, 5, true, false, 280, 95, NOW(), NOW()),
(10, 'Sports Watch', 'Waterproof sports watch with multiple features', 159.99, NULL, 40, 'WATCH-001', '/images/product-10.jpg', 5, 2, true, true, 165, 28, NOW(), NOW()),
(11, 'Knit Sweater', 'Cozy knit sweater perfect for fall and winter', 69.99, 54.99, 65, 'SWEATER-001', '/images/product-11.jpg', 2, 4, true, false, 175, 42, NOW(), NOW()),
(12, 'Canvas Tote Bag', 'Spacious canvas tote bag for daily use', 39.99, NULL, 90, 'BAG-002', '/images/product-12.jpg', 3, 4, true, false, 230, 68, NOW(), NOW()),
(13, 'High Heel Pumps', 'Classic high heel pumps for formal occasions', 89.99, 69.99, 35, 'SHOES-002', '/images/product-13.jpg', 4, 5, true, true, 190, 35, NOW(), NOW()),
(14, 'Striped Polo Shirt', 'Classic striped polo shirt in premium cotton', 54.99, NULL, 70, 'POLO-001', '/images/product-14.jpg', 2, 6, true, false, 155, 41, NOW(), NOW()),
(15, 'Pleated Mini Skirt', 'Trendy pleated mini skirt in various colors', 44.99, 34.99, 85, 'SKIRT-001', '/images/product-15.jpg', 1, 3, true, false, 265, 72, NOW(), NOW()),
(16, 'Athletic Shorts', 'Breathable athletic shorts for workout and sports', 34.99, 24.99, 110, 'SHORTS-001', '/images/product-16.jpg', 2, 6, true, true, 340, 98, NOW(), NOW());

-- Insert Product Sizes
INSERT INTO product_sizes (product_id, size) VALUES
-- Product 1: Dress
(1, 'XS'), (1, 'S'), (1, 'M'), (1, 'L'), (1, 'XL'),
-- Product 2: T-Shirt
(2, 'S'), (2, 'M'), (2, 'L'), (2, 'XL'), (2, 'XXL'),
-- Product 3: Jeans
(3, '26'), (3, '28'), (3, '30'), (3, '32'), (3, '34'),
-- Product 4: Bag (One Size)
(4, 'One Size'),
-- Product 5: Sneakers
(5, '6'), (5, '7'), (5, '8'), (5, '9'), (5, '10'), (5, '11'),
-- Product 6: Coat
(6, 'S'), (6, 'M'), (6, 'L'), (6, 'XL'),
-- Product 7: Shirt
(7, 'S'), (7, 'M'), (7, 'L'), (7, 'XL'),
-- Product 8: Maxi Dress
(8, 'XS'), (8, 'S'), (8, 'M'), (8, 'L'),
-- Product 9: Wallet (One Size)
(9, 'One Size'),
-- Product 10: Watch (One Size)
(10, 'One Size'),
-- Product 11: Sweater
(11, 'S'), (11, 'M'), (11, 'L'), (11, 'XL'),
-- Product 12: Tote Bag (One Size)
(12, 'One Size'),
-- Product 13: Pumps
(13, '5'), (13, '6'), (13, '7'), (13, '8'), (13, '9'),
-- Product 14: Polo
(14, 'S'), (14, 'M'), (14, 'L'), (14, 'XL'),
-- Product 15: Skirt
(15, 'XS'), (15, 'S'), (15, 'M'), (15, 'L'),
-- Product 16: Shorts
(16, 'S'), (16, 'M'), (16, 'L'), (16, 'XL');

-- Insert Product Colors
INSERT INTO product_colors (product_id, color) VALUES
-- Product 1
(1, 'Floral Pink'), (1, 'Floral Blue'),
-- Product 2
(2, 'White'), (2, 'Black'), (2, 'Gray'),
-- Product 3
(3, 'Dark Blue'), (3, 'Light Blue'), (3, 'Black'),
-- Product 4
(4, 'Brown'), (4, 'Black'),
-- Product 5
(5, 'Black/White'), (5, 'Navy/Orange'), (5, 'Gray/Red'),
-- Product 6
(6, 'Camel'), (6, 'Navy'), (6, 'Gray'),
-- Product 7
(7, 'White'), (7, 'Blue'), (7, 'Pink'),
-- Product 8
(8, 'White'), (8, 'Coral'), (8, 'Navy'),
-- Product 9
(9, 'Brown'), (9, 'Black'),
-- Product 10
(10, 'Black'), (10, 'Silver'),
-- Product 11
(11, 'Beige'), (11, 'Navy'), (11, 'Burgundy'),
-- Product 12
(12, 'Natural'), (12, 'Black'),
-- Product 13
(13, 'Black'), (13, 'Red'), (13, 'Nude'),
-- Product 14
(14, 'Navy/White'), (14, 'Black/Gray'),
-- Product 15
(15, 'Black'), (15, 'Navy'), (15, 'Burgundy'), (15, 'Beige'),
-- Product 16
(16, 'Black'), (16, 'Navy'), (16, 'Gray');

-- Insert Additional Product Images
INSERT INTO product_images (product_id, image_url) VALUES
-- Product 1 additional images
(1, '/images/product-detail-01.jpg'),
(1, '/images/product-detail-02.jpg'),
(1, '/images/product-detail-03.jpg'),
-- Product 2 additional images
(2, '/images/product-min-01.jpg'),
-- Product 3 additional images
(3, '/images/product-min-02.jpg'),
-- Product 4 additional images
(4, '/images/product-min-03.jpg'),
-- Product 5 additional images
(5, '/images/product-detail-01.jpg'),
(5, '/images/product-detail-02.jpg'),
-- Product 8 additional images
(8, '/images/product-detail-01.jpg'),
(8, '/images/product-detail-03.jpg'),
-- Product 13 additional images
(13, '/images/product-detail-02.jpg');

-- Reset sequences (if needed for PostgreSQL)
-- For MySQL, auto_increment will handle this automatically
-- SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));
-- SELECT setval('brands_id_seq', (SELECT MAX(id) FROM brands));
-- SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
