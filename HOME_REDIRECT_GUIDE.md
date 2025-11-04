# Hướng dẫn: Chuyển hướng về Home sau khi Login

## ✅ Đã hoàn thành

Đã cấu hình để sau khi đăng nhập thành công, user sẽ được chuyển hướng về trang Home (index.html) thay vì user_home.html, đồng thời hiển thị thông tin user đã đăng nhập trên header.

## 📝 Các thay đổi đã thực hiện

### 1. Cập nhật SecurityConfig - Đổi redirect URL

**File:** [SecurityConfig.java:73](src/main/java/g6/fashionFlex/config/SecurityConfig.java#L73)

```java
// TRƯỚC:
.defaultSuccessUrl("/user/home", true)

// SAU:
.defaultSuccessUrl("/", true)  // Redirect về trang home
```

### 2. Tạo HomeController - Xử lý các endpoint home

**File:** [HomeController.java](src/main/java/g6/fashionFlex/controller/HomeController.java)

**Chức năng:**
- Xử lý các endpoint: `/`, `/index`, `/home`, `/home-02`, `/home-03`
- Tự động detect user đã đăng nhập
- Truyền thông tin user vào Model để hiển thị trên view
- Set biến `isAuthenticated` để template biết user đã login hay chưa

**Code:**
```java
@GetMapping("/")
public String home(Model model) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null && authentication.isAuthenticated()
            && !authentication.getName().equals("anonymousUser")) {
        try {
            String email = authentication.getName();
            UserDTO user = userService.getUserByEmail(email);
            model.addAttribute("user", user);
            model.addAttribute("isAuthenticated", true);
        } catch (Exception e) {
            model.addAttribute("isAuthenticated", false);
        }
    } else {
        model.addAttribute("isAuthenticated", false);
    }

    return "index";
}
```

### 3. Cập nhật index.html - Hiển thị thông tin user

**File:** [index.html:2](src/main/resources/templates/index.html#L2)

**Thêm Thymeleaf namespace:**
```html
<html lang="en" xmlns:th="http://www.thymeleaf.org">
```

**Cập nhật header - Hiển thị user info:**

```html
<!-- TRƯỚC: -->
<a href="login.html" class="flex-c-m trans-04 p-lr-25">
    My Account
</a>

<!-- SAU: -->
<!-- Nếu đã login: hiển thị tên user -->
<a th:if="${isAuthenticated}" th:href="@{/user/home}" class="flex-c-m trans-04 p-lr-25">
    <i class="fa fa-user"></i> <span th:text="${user.fullName}"></span>
</a>

<!-- Nếu chưa login: hiển thị "My Account" -->
<a th:unless="${isAuthenticated}" th:href="@{/login}" class="flex-c-m trans-04 p-lr-25">
    My Account
</a>

<!-- Nút Logout (chỉ hiển thị khi đã login) -->
<form th:if="${isAuthenticated}" th:action="@{/logout}" method="post" style="display: inline; margin: 0;">
    <button type="submit" class="flex-c-m trans-04 p-lr-25"
            style="background: none; border: none; cursor: pointer; font-family: inherit; font-size: inherit; color: inherit;">
        Logout
    </button>
</form>
```

## 🎯 Flow hoạt động

### Khi chưa đăng nhập:

1. User truy cập `/` → HomeController xử lý
2. `isAuthenticated = false`
3. Header hiển thị: "My Account" link đến `/login`
4. Không có nút Logout

### Khi đã đăng nhập:

1. User login thành công → Redirect về `/`
2. HomeController detect user đã login
3. Lấy thông tin user từ database
4. Truyền `user` và `isAuthenticated = true` vào Model
5. Header hiển thị:
   - 👤 User name (click vào → `/user/home`)
   - Nút "Logout"
6. User có thể tiếp tục shopping hoặc logout

### Flow đầy đủ:

```
1. User vào /login
2. Nhập email/password → Submit
3. Spring Security xác thực
4. ✅ Success → Redirect to "/"
5. HomeController.home() được gọi
6. Kiểm tra authentication
7. Load user info từ DB
8. Render index.html với user data
9. Header hiển thị tên user + nút logout
```

## 🧪 Cách test

### Test 1: Chưa đăng nhập

1. Mở browser ở chế độ incognito/private
2. Truy cập: http://localhost:8080
3. **Expected:**
   - Trang home hiển thị bình thường
   - Header có "My Account" link
   - Không có tên user
   - Không có nút logout

### Test 2: Đăng nhập và redirect

1. Click "My Account" → Chuyển đến trang login
2. Đăng ký hoặc đăng nhập với tài khoản có sẵn
3. **Expected:**
   - ✅ Redirect về trang home (`/`)
   - ✅ Header hiển thị tên user (vd: "John Doe")
   - ✅ Có nút "Logout"
   - ✅ Click vào tên user → chuyển đến `/user/home`

### Test 3: Logout

1. Khi đang ở trang home đã login
2. Click nút "Logout"
3. **Expected:**
   - ✅ Redirect về `/login?logout=true`
   - ✅ Hiển thị thông báo "You have been logged out successfully"
   - ✅ Session bị xóa
   - ✅ Cookie JSESSIONID bị xóa

### Test 4: Quay lại home sau logout

1. Sau khi logout, click logo hoặc truy cập `/`
2. **Expected:**
   - ✅ Quay về home như guest (chưa login)
   - ✅ Header hiển thị "My Account"
   - ✅ Không còn tên user

## 📊 Các biến có sẵn trong template

Sau khi implement, các template có thể sử dụng:

### Biến `isAuthenticated` (Boolean)
- `true`: User đã đăng nhập
- `false`: User chưa đăng nhập (guest)

**Usage:**
```html
<div th:if="${isAuthenticated}">
    Welcome back!
</div>

<div th:unless="${isAuthenticated}">
    Please login to continue
</div>
```

### Biến `user` (UserDTO object)

**Available properties:**
- `user.id` - User ID
- `user.fullName` - Tên đầy đủ
- `user.email` - Email
- `user.phoneNumber` - Số điện thoại
- `user.address` - Địa chỉ
- `user.provider` - Loại tài khoản (local/google/facebook)
- `user.roles` - Set các roles (ROLE_USER, ROLE_ADMIN)
- `user.createdAt` - Ngày tạo tài khoản

**Usage:**
```html
<span th:text="${user.fullName}">User Name</span>
<span th:text="${user.email}">user@example.com</span>

<!-- Check if admin -->
<div th:if="${user.roles.contains('ROLE_ADMIN')}">
    Admin Panel
</div>
```

## 🎨 Tùy chỉnh thêm (Optional)

### 1. Thêm welcome message

```html
<!-- Thêm vào đầu trang home -->
<div th:if="${isAuthenticated}" class="welcome-banner">
    <p>Welcome back, <strong th:text="${user.fullName}"></strong>!</p>
</div>
```

### 2. Hiển thị avatar

```html
<a th:if="${isAuthenticated}" th:href="@{/user/home}">
    <img th:src="${user.avatar}" alt="Avatar" class="user-avatar">
    <span th:text="${user.fullName}"></span>
</a>
```

### 3. Dropdown menu cho user

```html
<div th:if="${isAuthenticated}" class="user-menu-dropdown">
    <button class="dropdown-toggle">
        <i class="fa fa-user"></i>
        <span th:text="${user.fullName}"></span>
    </button>
    <div class="dropdown-menu">
        <a href="/user/home">My Profile</a>
        <a href="/user/orders">My Orders</a>
        <a href="/user/wishlist">Wishlist</a>
        <hr>
        <form th:action="@{/logout}" method="post">
            <button type="submit">Logout</button>
        </form>
    </div>
</div>
```

## 🔄 Cập nhật các trang khác

Nếu muốn các trang khác (home-02, home-03, product, etc.) cũng hiển thị thông tin user, làm tương tự:

### 1. Thêm Thymeleaf namespace

```html
<html lang="en" xmlns:th="http://www.thymeleaf.org">
```

### 2. Cập nhật header/navigation

Copy đoạn code user info từ index.html

### 3. Tạo Controller hoặc cập nhật existing

Đảm bảo controller truyền `user` và `isAuthenticated` vào Model.

## 📚 Files liên quan

### Backend:
- [SecurityConfig.java](src/main/java/g6/fashionFlex/config/SecurityConfig.java) - Security configuration
- [HomeController.java](src/main/java/g6/fashionFlex/controller/HomeController.java) - Home endpoints
- [AuthController.java](src/main/java/g6/fashionFlex/controller/AuthController.java) - Login/Register
- [UserService.java](src/main/java/g6/fashionFlex/service/UserService.java) - User business logic

### Frontend:
- [index.html](src/main/resources/templates/index.html) - Home page
- [login.html](src/main/resources/templates/login.html) - Login/Register page
- [user_home.html](src/main/resources/templates/user_home.html) - User profile page

## ✨ Kết quả

Sau khi implement:
- ✅ Login thành công → Redirect về home page
- ✅ Header tự động hiển thị tên user khi đã login
- ✅ Có nút logout trên header
- ✅ Guest và logged-in user có UI khác nhau
- ✅ User experience mượt mà, không cần redirect qua trang trung gian

## 🚀 Chạy và test

```bash
# Stop ứng dụng cũ (nếu đang chạy)
Ctrl + C

# Rebuild
mvn clean compile

# Chạy lại
mvn spring-boot:run

# Test
# 1. Mở: http://localhost:8080
# 2. Click "My Account" → Login
# 3. Sau khi login → Tự động về home với tên user hiển thị
```

---

**Tạo bởi:** Claude Code Assistant
**Ngày:** 2025-11-04
**Version:** FashionFlex v0.0.1-SNAPSHOT
