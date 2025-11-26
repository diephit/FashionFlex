# Thymeleaf Fragments Usage Guide

## Tổng quan
Thư mục này chứa các fragments (phần tử có thể tái sử dụng) cho toàn bộ dự án FashionFlex.

## Các file fragments

### 1. header.html
Chứa 3 fragments chính:

#### a) head(pageTitle)
Fragment cho phần `<head>` của trang, bao gồm tất cả CSS cần thiết.

**Cách sử dụng:**
```html
<head th:replace="~{fragments/header :: head('Home')}">
</head>
```

#### b) header(activeMenu)
Fragment cho header/navigation bar.

**Cách sử dụng:**
```html
<div th:replace="~{fragments/header :: header('home')}"></div>
```

**Tham số activeMenu có thể là:**
- `'home'` - Trang chủ
- `'shop'` - Trang sản phẩm
- `'cart'` - Giỏ hàng
- `'blog'` - Blog
- `'about'` - Giới thiệu
- `'contact'` - Liên hệ

#### c) cart
Fragment cho cart sidebar (giỏ hàng trượt).

**Cách sử dụng:**
```html
<div th:replace="~{fragments/header :: cart}"></div>
```

#### d) scripts
Fragment chứa tất cả JavaScript cần thiết.

**Cách sử dụng:**
```html
<div th:replace="~{fragments/header :: scripts}"></div>
```

### 2. footer.html
Chứa 2 fragments:

#### a) footer
Fragment cho footer của trang.

**Cách sử dụng:**
```html
<div th:replace="~{fragments/footer :: footer}"></div>
```

#### b) back-to-top
Fragment cho nút quay lên đầu trang.

**Cách sử dụng:**
```html
<div th:replace="~{fragments/footer :: back-to-top}"></div>
```

### 3. base-layout.html
Template cơ bản cho các trang, đã include sẵn tất cả fragments.

## Ví dụ sử dụng

### Cách 1: Sử dụng từng fragment riêng lẻ

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/header :: head('Product List')}">
</head>
<body class="animsition">

<!-- Header -->
<div th:replace="~{fragments/header :: header('shop')}"></div>

<!-- Cart Sidebar -->
<div th:replace="~{fragments/header :: cart}"></div>

<!-- Page Content -->
<section class="bg0 p-t-104 p-b-116">
    <div class="container">
        <!-- Your page content here -->
    </div>
</section>

<!-- Footer -->
<div th:replace="~{fragments/footer :: footer}"></div>

<!-- Back to top -->
<div th:replace="~{fragments/footer :: back-to-top}"></div>

<!-- Scripts -->
<div th:replace="~{fragments/header :: scripts}"></div>

</body>
</html>
```

### Cách 2: Extend từ base-layout (Khuyến nghị)

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/base-layout :: html(pageTitle='Product List', activeMenu='shop')}">
<body>

<!-- Your page-specific content -->
<section class="bg0 p-t-104 p-b-116">
    <div class="container">
        <!-- Your page content here -->
    </div>
</section>

</body>
</html>
```

## Luồng liên kết giữa các trang

### Navigation chính
- **Home**: `/` hoặc `@{/}`
- **Shop**: `@{/product}`
- **Cart**: `@{/shoping-cart}`
- **Blog**: `@{/blog}`
- **About**: `@{/about}`
- **Contact**: `@{/contact}`

### User pages
- **My Account**: `@{/user/my-account}`
- **Wishlist**: `@{/user/wishlist}`
- **Order History**: `@{/user/order-history}`
- **Order Tracking**: `@{/user/order-tracking}`
- **Address Book**: `@{/user/address-book}`

### Checkout pages
- **Checkout**: `@{/checkout/checkout}`
- **Payment**: `@{/checkout/payment}`
- **Order Confirmation**: `@{/checkout/order-confirmation}`

### Info pages
- **FAQ**: `@{/info/faq}`
- **Privacy Policy**: `@{/info/privacy-policy}`
- **Terms & Conditions**: `@{/info/terms-conditions}`
- **Return Policy**: `@{/info/return-policy}`
- **Shipping Policy**: `@{/info/shipping-policy}`
- **Size Guide**: `@{/info/size-guide}`

### Authentication
- **Login**: `@{/login}`
- **Forgot Password**: `@{/auth/forgot-password}`
- **Reset Password**: `@{/auth/reset-password}`

## Authentication trong Header

Header tự động hiển thị:
- Tên người dùng và nút Logout nếu `${isAuthenticated}` = true
- Link "My Account" để đăng nhập nếu `${isAuthenticated}` = false

**Lưu ý**: Controller cần truyền 2 biến:
- `isAuthenticated`: boolean
- `user`: object chứa thông tin user (nếu authenticated)

## Best Practices

1. **Sử dụng Thymeleaf URL syntax**: Luôn dùng `@{/path}` thay vì hardcode URL
2. **Set activeMenu**: Luôn set đúng activeMenu cho từng trang để highlight menu item
3. **Set pageTitle**: Mỗi trang nên có title riêng
4. **DRY principle**: Tái sử dụng fragments thay vì copy-paste code

## Files cần xóa

Các file HTML tĩnh ở thư mục root có thể xóa sau khi đã migrate sang Thymeleaf:
- `about.html`
- `blog.html`
- `blog-detail.html`
- `contact.html`
- `home-02.html`
- `home-03.html`
- `index.html`
- `login.html`
- `product.html`
- `product-detail.html`
- `shoping-cart.html`

Chỉ giữ lại các file trong `src/main/resources/templates/`.
