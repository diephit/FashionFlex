-- Script to create an admin user for FashionFlex
-- Run this after starting the application (roles will be auto-created by DataInitializer)

-- Password is: admin123
-- This is BCrypt hash of "admin123" with strength 10
INSERT INTO users (full_name, email, password, enabled, provider, created_at)
VALUES ('Admin User', 'admin@fashionflex.com',
        '$2a$10$EIXmJFH5JHwZ2Y9pGqZ5UOwpEGqJqWZqJH.NdVq6vO0VQ7KIr7Z3G',
        true, 'local', CURRENT_TIMESTAMP);

-- Get the user ID (MySQL)
SET @admin_user_id = LAST_INSERT_ID();

-- Assign ROLE_ADMIN to the user
INSERT INTO user_roles (user_id, role_id)
SELECT @admin_user_id, id FROM roles WHERE name = 'ROLE_ADMIN';

-- Also assign ROLE_USER (optional, for accessing both admin and customer features)
INSERT INTO user_roles (user_id, role_id)
SELECT @admin_user_id, id FROM roles WHERE name = 'ROLE_USER';

-- Verify the admin user was created
SELECT u.id, u.full_name, u.email, u.enabled, r.name as role
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.email = 'admin@fashionflex.com';

-- ==========================================
-- Login Credentials:
-- Email: admin@fashionflex.com
-- Password: admin123
-- ==========================================
