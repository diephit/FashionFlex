# FashionFlex - Quick Start Guide

## 🚀 Hướng dẫn chạy ứng dụng

### 1. Chuẩn bị Database

**Tạo MySQL Database:**
```sql
CREATE DATABASE ecommerce_db;
```

**Hoặc sử dụng MySQL Workbench hoặc phpMyAdmin để tạo database.**

### 2. Kiểm tra cấu hình Database

Mở file `src/main/resources/application.properties` và đảm bảo:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce_db
spring.datasource.username=root
spring.datasource.password=root
```

Nếu username/password MySQL của bạn khác, hãy thay đổi cho phù hợp.

### 3. Chạy ứng dụng

**Cách 1: Sử dụng Maven**
```bash
mvn spring-boot:run
```

**Cách 2: Sử dụng IDE**
- Mở file `FashionFlexApplication.java`
- Click nút Run (▶️) hoặc nhấn Shift+F10

### 4. Truy cập ứng dụng

Sau khi ứng dụng khởi động thành công (xem log "Started FashionFlexApplication"), truy cập:

**URL:** http://localhost:8080/login

## 📝 Test chức năng

### ✅ Test Đăng ký (Register)

1. Truy cập: http://localhost:8080/login
2. Click tab **"REGISTER"**
3. Điền form:
   - **Full Name:** Nguyen Van A
   - **Email:** test@example.com
   - **Password:** 123456 (tối thiểu 6 ký tự)
   - ✓ Check "I agree to the Terms & Conditions"
4. Click **"Create Account"**
5. **Kết quả:** Redirect về tab LOGIN với thông báo màu xanh "Registration successful! Please log in."

### ✅ Test Đăng nhập (Login)

1. Ở tab **"LOGIN"**
2. Nhập:
   - **Email:** test@example.com
   - **Password:** 123456
3. (Optional) Check "Remember Me"
4. Click **"Sign In"**
5. **Kết quả:** Redirect đến trang `/user/home` với:
   - Thông tin user (Name, Email, Account Type, Member Since)
   - Nút "Start Shopping"
   - Nút "Logout"

### ✅ Test Logout

1. Ở trang `/user/home`, click nút **"Logout"**
2. **Kết quả:** Redirect về `/login` với thông báo "You have been logged out successfully"

## 🔍 Troubleshooting

### Lỗi "Cannot connect to database"
```
Solution:
1. Kiểm tra MySQL đang chạy: services.msc (Windows) hoặc systemctl status mysql (Linux)
2. Kiểm tra database đã tạo: SHOW DATABASES; trong MySQL
3. Kiểm tra username/password trong application.properties
```

### Lỗi "Port 8080 already in use"
```
Solution 1: Stop ứng dụng đang chạy trên port 8080
Solution 2: Thay đổi port trong application.properties:
   server.port=8081
```

### Lỗi "HTTP ERROR 401" sau khi login
```
Đã fix! Vấn đề là session management trong SecurityConfig.
Current fix: Đã đổi từ STATELESS sang IF_REQUIRED và thêm formLogin() configuration.
```

### Lỗi "Whitelabel Error Page"
```
Kiểm tra:
1. Template tồn tại trong src/main/resources/templates/
2. Tên file khớp với return value trong Controller
3. Spring Boot đã load Thymeleaf dependency
```

### Database tables không được tạo
```
Solution:
spring.jpa.hibernate.ddl-auto=update trong application.properties sẽ tự động tạo tables.
Nếu không tạo, thử đổi thành: spring.jpa.hibernate.ddl-auto=create (chỉ dùng lần đầu)
```

## 📊 Kiểm tra Database

Sau khi đăng ký user, kiểm tra trong MySQL:

```sql
USE ecommerce_db;

-- Xem tất cả tables
SHOW TABLES;

-- Xem users
SELECT * FROM users;

-- Xem roles
SELECT * FROM roles;

-- Xem user_roles mapping
SELECT u.email, r.name
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON r.id = ur.role_id;
```

**Expected output:**
- Table `users` có record với email và password đã mã hóa
- Table `roles` có ROLE_USER và ROLE_ADMIN
- Table `user_roles` có mapping user với ROLE_USER

## 🎯 Các endpoint có sẵn

### Public endpoints (không cần đăng nhập)
- `GET /` - Trang chủ
- `GET /login` - Trang đăng nhập/đăng ký
- `POST /api/auth/register` - API đăng ký
- `POST /api/auth/login` - API đăng nhập (trả về JWT)
- `GET /product` - Trang sản phẩm
- `GET /about` - Trang giới thiệu
- `GET /contact` - Trang liên hệ

### Protected endpoints (cần đăng nhập)
- `GET /user/home` - Trang home của user đã login
- `POST /logout` - Đăng xuất

### Admin endpoints (cần role ADMIN)
- `/api/admin/**` - Các API dành cho admin

## 🔐 Security Features

### Password Encryption
- ✅ BCrypt encryption
- ✅ Độ mạnh mặc định (strength 10)
- ✅ Password không bao giờ lưu dạng plaintext

### Session Management
- ✅ Session-based cho form login
- ✅ JWT-based cho API requests
- ✅ Remember me functionality

### Authentication
- ✅ Form-based login
- ✅ Spring Security authentication
- ✅ Custom UserDetailsService
- ✅ Role-based authorization

## 📱 Test với API Client (Postman/curl)

### Đăng ký qua API
```bash
curl -X POST http://localhost:8080/api/auth/register-json \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "API User",
    "email": "api@example.com",
    "password": "password123"
  }'
```

### Đăng nhập qua API (nhận JWT token)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "api@example.com",
    "password": "password123"
  }'
```

Response sẽ trả về JWT token:
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "userId": 1,
  "email": "api@example.com",
  "fullName": "API User"
}
```

### Sử dụng JWT token
```bash
curl http://localhost:8080/api/protected-endpoint \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"
```

## 📝 Default Data

Khi ứng dụng khởi động lần đầu, DataInitializer sẽ tự động tạo:
- ✅ ROLE_USER
- ✅ ROLE_ADMIN

User đầu tiên đăng ký sẽ được gán ROLE_USER tự động.

## 🎨 Frontend Features

### Login Page (`/login`)
- ✅ Tabbed interface (Login/Register)
- ✅ Form validation
- ✅ Error/Success messages
- ✅ Responsive design
- ⏳ Social login buttons (chưa hoạt động)

### User Home Page (`/user/home`)
- ✅ Display user info
- ✅ Account type (local/google/facebook)
- ✅ Member since date
- ✅ Logout button
- ✅ Shopping button

## 🚧 Tính năng chưa implement

### OAuth2 Social Login (đã config sẵn)
- ⏳ Google Login
- ⏳ Facebook Login
- Cần implement: OAuth2UserService, Success/Failure handlers

### Forgot Password
- ⏳ Password reset flow
- ⏳ Email service
- ⏳ Reset token generation

### Email Verification
- ⏳ Email verification sau đăng ký
- ⏳ Verification token

## 📚 Documentation

Xem chi tiết hơn tại:
- [AUTHENTICATION_README.md](AUTHENTICATION_README.md) - Hướng dẫn chi tiết về authentication
- [important_infomation.txt](important_infomation.txt) - Thông tin quan trọng về dự án

## ⚡ Performance Tips

### Development Mode
- Database: MySQL (như hiện tại)
- Session: IF_REQUIRED
- Show SQL: true (để debug)

### Production Mode (khi deploy)
Thay đổi trong `application.properties`:
```properties
# Tắt SQL logging
spring.jpa.show-sql=false

# Sử dụng connection pool
spring.datasource.hikari.maximum-pool-size=10

# Enable security headers
# (thêm trong SecurityConfig)
```

## 🎉 Ready to go!

Sau khi hoàn tất các bước trên, ứng dụng đã sẵn sàng với:
- ✅ User registration
- ✅ User login/logout
- ✅ Session management
- ✅ Password encryption
- ✅ Role-based authorization
- ✅ JWT API authentication
- ✅ Responsive UI

Enjoy coding! 🚀
