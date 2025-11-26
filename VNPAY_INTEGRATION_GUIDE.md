# VNPay Payment Integration - FashionFlex

## 📋 Tổng Quan

Tích hợp thanh toán VNPay đã được thêm vào hệ thống FashionFlex với flow **Order trước - Thanh toán sau**.

### Luồng Xử Lý

1. **User checkout** → Tạo Order với status `PENDING` và payment_status `PENDING`
2. **Chọn VNPay** → Redirect đến VNPay payment gateway
3. **Thanh toán trên VNPay** → User nhập thông tin thanh toán
4. **VNPay callback** → Cập nhật Order status dựa trên kết quả
   - Thành công: status = `CONFIRMED`, payment_status = `PAID`
   - Thất bại: payment_status = `FAILED`

---

## 🗂️ Cấu Trúc Code

### 1. **Entity Changes**
- **Order.java** (`src/main/java/g6/fashionFlex/entity/Order.java`)
  - Thêm `VNPAY` vào enum `PaymentMethod`

### 2. **Configuration**
- **VNPayConfig.java** (`src/main/java/g6/fashionFlex/config/VNPayConfig.java`)
  - Quản lý VNPay credentials và endpoints
  - Load từ `application.properties`

### 3. **Utility Class**
- **VNPayUtil.java** (`src/main/java/g6/fashionFlex/util/VNPayUtil.java`)
  - HMAC-SHA512 hashing
  - MD5, SHA-256 utilities
  - IP address extraction
  - Query string building

### 4. **Service Layer**
- **VNPayService.java** (`src/main/java/g6/fashionFlex/service/VNPayService.java`)
  - `createPaymentUrl()` - Tạo VNPay payment URL
  - `verifyCallback()` - Verify chữ ký từ VNPay
  - `getPaymentResult()` - Extract payment result data
  - `isPaymentSuccess()` - Kiểm tra transaction success
  - `getResponseDescription()` - Mô tả response code

### 5. **Controllers**

#### VNPayController.java
```
GET  /payment/vnpay/pay/{orderId}     - Initiate payment
GET  /payment/vnpay/callback          - VNPay callback (PUBLIC)
GET  /payment/vnpay/result/{orderId}  - View payment result
```

#### CheckoutController.java (Updated)
- Thêm VNPayService dependency
- Check payment method sau khi place order
- Nếu VNPAY → redirect to VNPay gateway
- Nếu khác (COD, etc.) → redirect to confirmation page

### 6. **Views**
- **checkout.html** - Thêm VNPAY option với description
- **payment-result.html** - Hiển thị kết quả thanh toán (success/failed)

### 7. **Security**
- **SecurityConfig.java**
  - Thêm `/payment/vnpay/callback` vào public endpoints
  - CSRF disabled (đã có từ trước)

---

## ⚙️ Cấu Hình

### application.properties

```properties
# VNPay Configuration
vnpay.tmn-code=4YUP19I4
vnpay.secret-key=MDUIFDCRAKLNBPOFIAFNEKFRNMFBYEPX
vnpay.pay-url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.return-url=http://localhost:8080/payment/vnpay/callback
vnpay.api-url=https://sandbox.vnpayment.vn/merchant_webapi/api/transaction
vnpay.version=2.1.0
vnpay.command=pay
vnpay.order-type=other
vnpay.currency=VND
vnpay.locale=vn
```

**⚠️ LƯU Ý:**
- Credentials trên là **SANDBOX** từ VNPay demo
- Khi deploy production, cần thay bằng credentials thực từ VNPay merchant portal
- `return-url` cần update theo domain production

---

## 🚀 Hướng Dẫn Sử Dụng

### 1. Testing Payment Flow

```bash
# Start application
mvn spring-boot:run

# Navigate to:
http://localhost:8080
```

**Test Flow:**
1. Login/Register account
2. Thêm sản phẩm vào cart
3. Calculate shipping tại shopping cart
4. Proceed to Checkout
5. Nhập shipping information
6. **Chọn payment method: VNPAY**
7. Click "Place Order"
8. Redirect đến VNPay sandbox
9. Sử dụng thông tin test card VNPay:
   - Card Number: Test cards provided by VNPay
   - Hoặc chọn QR code payment trong sandbox

### 2. VNPay Sandbox Test Cards

VNPay sandbox cung cấp nhiều test scenarios:
- **Success**: Sử dụng test cards được VNPay cung cấp
- **Failed**: Chọn simulate failed transaction
- **Cancelled**: User click "Cancel" trên VNPay gateway

### 3. Kiểm Tra Logs

```java
// Logs quan trọng:
- "Creating VNPay payment URL for order: {orderNumber}"
- "Received VNPay payment callback"
- "VNPay callback signature verification: SUCCESS/FAILED"
- "Payment successful for order: {orderNumber}"
- "Payment failed for order: {orderNumber}"
```

---

## 📊 Database Schema

### Orders Table
Các field liên quan đến VNPay payment:

```sql
payment_method VARCHAR(50)   -- VNPAY
payment_status VARCHAR(50)   -- PENDING → PAID/FAILED
status VARCHAR(50)           -- PENDING → CONFIRMED (if payment success)
tracking_number VARCHAR(255) -- Lưu VNPay transaction number
```

---

## 🔒 Security

### 1. Signature Verification
- Tất cả callbacks từ VNPay được verify bằng HMAC-SHA512
- Secret key được lưu an toàn trong application.properties
- Invalid signature → reject callback

### 2. Public Endpoint
- `/payment/vnpay/callback` phải public để VNPay gọi được
- Không cần authentication nhưng **MUST verify signature**

### 3. Order Ownership
- Không verify ownership trong callback vì user chưa login session
- VNPay đảm bảo orderId không bị tamper (qua signature)

---

## 🐛 Troubleshooting

### 1. Callback không được gọi
- Kiểm tra `vnpay.return-url` trong application.properties
- Nếu localhost, VNPay sandbox vẫn callback được
- Production: phải dùng public domain

### 2. Signature verification failed
- Kiểm tra `vnpay.secret-key` đúng với merchant account
- Kiểm tra encoding UTF-8 trong hash data
- Check logs: "Invalid VNPay callback signature"

### 3. Order không update status
- Kiểm tra logs exception trong `VNPayController.handleCallback()`
- Verify orderId tồn tại
- Check database transaction

### 4. User không redirect về
- Kiểm tra payment-result.html template tồn tại
- Check SecurityConfig cho phép `/payment/vnpay/callback`

---

## 📈 Nâng Cấp Tương Lai

### 1. Transaction Logging (Recommended)
Tạo `VNPayTransaction` entity để log tất cả transactions:

```java
@Entity
public class VNPayTransaction {
    private Long id;
    private Long orderId;
    private String transactionNo;
    private String responseCode;
    private String transactionStatus;
    private BigDecimal amount;
    private String bankCode;
    private LocalDateTime createdAt;
}
```

### 2. IPN (Instant Payment Notification)
Thêm endpoint riêng cho VNPay IPN:
- VNPay gọi IPN độc lập với return URL
- Đảm bảo order update ngay cả khi user đóng browser

### 3. Refund Support
Implement VNPay refund API:
- Admin có thể refund orders
- Call VNPay API để refund transaction
- Update order status = REFUNDED

### 4. Multiple Payment Gateways
Tạo abstraction layer:
```java
public interface PaymentGateway {
    String createPaymentUrl(Order order);
    boolean verifyCallback(HttpServletRequest request);
}

public class VNPayGateway implements PaymentGateway { ... }
public class MomoGateway implements PaymentGateway { ... }
```

---

## 📞 Support

### VNPay Resources
- **Sandbox**: https://sandbox.vnpayment.vn/
- **Documentation**: https://sandbox.vnpayment.vn/apis/docs/
- **Merchant Portal**: Contact VNPay sales

### Code References
- **VNPay Sample Project**: `C:\Users\PC\Downloads\code_vnpay\code_vnpay\vn_pay`
- **FashionFlex VNPay Code**: Package `g6.fashionFlex` (config, service, controller, util)

---

## ✅ Checklist Deploy Production

- [ ] Đăng ký VNPay merchant account
- [ ] Lấy production credentials (TMN Code, Secret Key)
- [ ] Update `application.properties` với production values
- [ ] Update `vnpay.return-url` với production domain
- [ ] Test end-to-end với VNPay production environment
- [ ] Setup monitoring cho payment callbacks
- [ ] Setup alert cho failed payments
- [ ] Backup database trước khi deploy
- [ ] Document cho team về VNPay flow

---

**Generated**: 2025-01-20
**Version**: 1.0
**Author**: Claude AI (Anthropic)
