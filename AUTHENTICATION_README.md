# FashionFlex - Authentication System Documentation

## Tổng quan
Hệ thống authentication đã được hoàn thiện với đầy đủ chức năng đăng ký (register) và đăng nhập (login), kết nối với frontend Thymeleaf.

## Công nghệ sử dụng
- **Spring Boot 3.5.7** với Java 21
- **Spring Security** - Bảo mật và authentication
- **JWT (JSON Web Token)** - Token-based authentication
- **MySQL** - Database chính
- **Thymeleaf** - Template engine
- **BCrypt** - Password encryption
- **Lombok** - Giảm boilerplate code

## Cấu trúc dự án

### 1. Entities
- **User** ([User.java](src/main/java/g6/fashionFlex/entity/User.java))
  - Thông tin người dùng: fullName, email, password, roles
  - Hỗ trợ OAuth2 (provider, providerId)
  - Timestamps: createdAt, updatedAt

- **Role** ([Role.java](src/main/java/g6/fashionFlex/entity/Role.java))
  - Quản lý vai trò: ROLE_USER, ROLE_ADMIN

### 2. Repositories
- **UserRepository** ([UserRepository.java](src/main/java/g6/fashionFlex/repository/UserRepository.java))
- **RoleRepository** ([RoleRepository.java](src/main/java/g6/fashionFlex/repository/RoleRepository.java))

### 3. DTOs
- **RegisterRequest** - Dữ liệu đăng ký
- **LoginRequest** - Dữ liệu đăng nhập
- **AuthResponse** - Response chứa JWT token
- **UserDTO** - Thông tin user trả về

### 4. Security Components
- **SecurityConfig** ([SecurityConfig.java](src/main/java/g6/fashionFlex/config/SecurityConfig.java))
  - Cấu hình Spring Security
  - Password encoder (BCrypt)
  - Các endpoint được phép truy cập công khai

- **JwtTokenProvider** ([JwtTokenProvider.java](src/main/java/g6/fashionFlex/security/JwtTokenProvider.java))
  - Generate và validate JWT token

- **JwtAuthenticationFilter** ([JwtAuthenticationFilter.java](src/main/java/g6/fashionFlex/security/JwtAuthenticationFilter.java))
  - Filter xác thực JWT cho mỗi request

- **CustomUserDetailsService** ([CustomUserDetailsService.java](src/main/java/g6/fashionFlex/security/CustomUserDetailsService.java))
  - Load user từ database cho Spring Security

### 5. Services
- **UserService** ([UserService.java](src/main/java/g6/fashionFlex/service/UserService.java))
  - Business logic cho user management
  - Đăng ký user mới
  - Mã hóa password
  - OAuth2 user management

### 6. Controllers
- **AuthController** ([AuthController.java](src/main/java/g6/fashionFlex/controller/AuthController.java))
  - `GET /login` - Hiển thị trang login
  - `POST /login` - Xử lý đăng nhập
  - `POST /api/auth/register` - Đăng ký từ form
  - `POST /api/auth/login` - API đăng nhập (trả về JWT)
  - `GET /user/home` - Trang home sau khi login
  - `GET /logout` - Đăng xuất

### 7. Exception Handling
- **GlobalExceptionHandler** ([GlobalExceptionHandler.java](src/main/java/g6/fashionFlex/exception/GlobalExceptionHandler.java))
  - Xử lý tất cả exceptions
  - Validation errors
  - Authentication errors

## Cách sử dụng

### Yêu cầu trước khi chạy
1. **MySQL Database**
   - Tạo database: `ecommerce_db`
   - Hoặc thay đổi cấu hình trong `application.properties`

2. **Cấu hình Database** (application.properties):
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce_db
spring.datasource.username=root
spring.datasource.password=root
```

### Chạy ứng dụng

1. **Build project:**
```bash
mvn clean install
```

2. **Chạy ứng dụng:**
```bash
mvn spring-boot:run
```

Hoặc chạy trực tiếp file `FashionFlexApplication.java`

3. **Truy cập ứng dụng:**
- URL: http://localhost:8080
- Trang login: http://localhost:8080/login

### Quy trình sử dụng

#### 1. Đăng ký tài khoản mới
1. Truy cập: http://localhost:8080/login
2. Click tab "REGISTER"
3. Điền thông tin:
   - Full Name (bắt buộc)
   - Email (bắt buộc, phải đúng format)
   - Password (bắt buộc, tối thiểu 6 ký tự)
   - Đồng ý Terms & Conditions
4. Click "Create Account"
5. Nếu thành công, sẽ redirect về tab Login với thông báo "Registration successful!"

#### 2. Đăng nhập
1. Ở tab "LOGIN"
2. Nhập:
   - Email
   - Password
   - (Optional) Check "Remember Me"
3. Click "Sign In"
4. Nếu thành công, redirect đến `/user/home`

#### 3. Trang User Home
- Hiển thị thông tin user:
  - Tên đầy đủ
  - Email
  - Loại tài khoản (local/google/facebook)
  - Ngày tạo tài khoản
- Có nút "Start Shopping" để vào trang sản phẩm
- Có nút "Logout" để đăng xuất

#### 4. Đăng xuất
- Click nút "Logout" hoặc truy cập: http://localhost:8080/logout
- Redirect về trang login với thông báo "You have been logged out successfully"

## API Endpoints

### REST API (JSON)

#### Đăng ký (JSON)
```http
POST /api/auth/register-json
Content-Type: application/json

{
  "fullName": "Nguyen Van A",
  "email": "nguyenvana@example.com",
  "password": "password123"
}
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Registration successful",
  "user": {
    "id": 1,
    "fullName": "Nguyen Van A",
    "email": "nguyenvana@example.com",
    "enabled": true,
    "provider": "local",
    "roles": ["ROLE_USER"],
    "createdAt": "2025-11-04T14:30:00"
  }
}
```

#### Đăng nhập (JSON)
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "nguyenvana@example.com",
  "password": "password123"
}
```

**Response (Success):**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "userId": 1,
  "email": "nguyenvana@example.com",
  "fullName": "Nguyen Van A"
}
```

### Sử dụng JWT Token
Sau khi đăng nhập bằng API, sử dụng token trong header:
```http
Authorization: Bearer <your-jwt-token>
```

## Security Configuration

### Public Endpoints (không cần authentication)
- `/` - Trang chủ
- `/login` - Trang đăng nhập
- `/api/auth/**` - API authentication
- `/product`, `/product-detail/**` - Trang sản phẩm
- `/about`, `/contact`, `/blog` - Các trang thông tin
- `/css/**`, `/js/**`, `/images/**` - Static resources

### Protected Endpoints (cần authentication)
- `/user/**` - Các trang dành cho user đã đăng nhập
- `/api/admin/**` - API dành cho admin (cần role ADMIN)

## Database Schema

### Table: users
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `full_name` (VARCHAR)
- `email` (VARCHAR, UNIQUE)
- `password` (VARCHAR, encrypted)
- `phone_number` (VARCHAR)
- `address` (VARCHAR)
- `enabled` (BOOLEAN)
- `provider` (VARCHAR) - local, google, facebook
- `provider_id` (VARCHAR)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

### Table: roles
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `name` (VARCHAR, UNIQUE) - ROLE_USER, ROLE_ADMIN

### Table: user_roles (Many-to-Many)
- `user_id` (BIGINT, FK)
- `role_id` (BIGINT, FK)

## Validation Rules

### Đăng ký
- **Full Name**: Bắt buộc, không được để trống
- **Email**: Bắt buộc, phải đúng format email, không được trùng
- **Password**: Bắt buộc, tối thiểu 6 ký tự

### Đăng nhập
- **Email**: Bắt buộc, phải đúng format email
- **Password**: Bắt buộc

## Error Handling

### Các lỗi phổ biến

1. **Email already registered**
   - Status: 409 Conflict
   - Message: "Email already registered: xxx@example.com"

2. **Invalid credentials**
   - Status: 401 Unauthorized
   - Message: "Invalid email or password"

3. **Validation errors**
   - Status: 400 Bad Request
   - Hiển thị chi tiết lỗi từng field

## Bảo mật

### Password Encryption
- Sử dụng BCrypt với strength mặc định
- Password không bao giờ được lưu dưới dạng plaintext

### JWT Token
- Secret key được cấu hình trong `application.properties`
- Token expiration: 15 phút (900000ms)
- Token được gửi trong Authorization header

### CSRF Protection
- Đã disable cho REST API
- Có thể enable lại nếu cần cho form-based authentication

### Session Management
- Sử dụng stateless session (JWT-based)
- Không lưu session trên server

## Tính năng mở rộng (chưa implement)

### 1. OAuth2 Social Login
- Google Login - đã cấu hình sẵn
- Facebook Login - đã cấu hình sẵn
- Cần implement OAuth2UserService và Success/Failure handlers

### 2. Forgot Password
- Endpoint đã có trong frontend
- Cần implement:
  - Password reset token
  - Email service
  - Reset password form

### 3. Email Verification
- Xác thực email sau khi đăng ký
- Send verification email
- Verify endpoint

### 4. Refresh Token
- Đã cấu hình refresh token expiration
- Cần implement refresh token endpoint

## Testing

### Test Registration
1. Mở http://localhost:8080/login
2. Tab "REGISTER"
3. Điền form và submit
4. Kiểm tra:
   - Có thông báo success
   - User được tạo trong database
   - Password được mã hóa
   - Default role ROLE_USER được gán

### Test Login
1. Tab "LOGIN"
2. Nhập credentials
3. Kiểm tra:
   - Redirect đến /user/home
   - Thông tin user hiển thị đúng
   - Có thể logout

### Test API với Postman/curl
```bash
# Register
curl -X POST http://localhost:8080/api/auth/register-json \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Test User","email":"test@example.com","password":"password123"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

## Troubleshooting

### Lỗi "Unable to connect to database"
- Kiểm tra MySQL đã chạy chưa
- Kiểm tra database `ecommerce_db` đã tạo chưa
- Kiểm tra username/password trong application.properties

### Lỗi "Table doesn't exist"
- JPA sẽ tự động tạo tables với `spring.jpa.hibernate.ddl-auto=update`
- Hoặc chạy lại ứng dụng

### Lỗi JWT "Invalid signature"
- Kiểm tra JWT secret trong application.properties
- Đảm bảo secret key đủ dài (ít nhất 256 bits)

### Không redirect được sau login
- Kiểm tra SecurityConfig
- Kiểm tra endpoint `/user/home` có tồn tại không

## Contributors
- G6 Team - FashionFlex Project

## License
Private project for educational purposes.
