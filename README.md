# FashionFlex - E-commerce Platform

## 🚀 Quick Start (Không cần cấu hình)

**Bạn có thể chạy ứng dụng NGAY mà không cần tạo file .env!**

Ứng dụng đã được cấu hình với **giá trị mặc định**, bạn chỉ cần:

### Bước 1: Clone project
```bash
git clone <repository-url>
cd ff-2.0
```

### Bước 2: Tạo Database (nếu chưa có)
```sql
CREATE DATABASE ff;
```

### Bước 3: Chạy ứng dụng
```bash
mvn spring-boot:run
```

**Xong!** Ứng dụng sẽ chạy với cấu hình mặc định:
- Database: `localhost:3306/ff`
- Username: `root`
- Password: `123456`
- JWT Secret: `change_me_development_secret_key_please_override`

---

## ⚙️ Tùy chỉnh cấu hình (Optional)

Nếu bạn muốn thay đổi cấu hình (database khác, Gmail, etc.):

### Cách 1: Chạy script tự động (Khuyến nghị)

**Windows:**
```bash
setup.bat
```

**Linux/Mac:**
```bash
chmod +x setup.sh
./setup.sh
```

Script sẽ tự động tạo file `.env` từ `.env.example`.

### Cách 2: Tạo thủ công

1. Copy `.env.example` thành `.env`:
   ```bash
   # Windows
   copy .env.example .env
   
   # Linux/Mac
   cp .env.example .env
   ```

2. Mở file `.env` và chỉnh sửa theo nhu cầu.

---

## 📋 Giá trị mặc định

Nếu **KHÔNG** có file `.env`, ứng dụng sẽ dùng:

| Cấu hình | Giá trị mặc định | Ghi chú |
|----------|------------------|---------|
| Database URL | `jdbc:mysql://localhost:3306/ff` | Nếu khác → tạo `.env` |
| Database Username | `root` | Nếu khác → tạo `.env` |
| Database Password | `123456` | ⚠️ **Nếu password khác → BẮT BUỘC tạo `.env`** |
| JWT Secret | `change_me_development_secret_key_please_override` | OK cho dev |
| JWT Expiration | `86400000` ms (24 hours) | OK cho dev |
| Gmail Username | (trống) | Chỉ cần nếu dùng email |
| Gmail Password | (trống) | Chỉ cần nếu dùng email |

### ⚠️ Lưu ý quan trọng:

**Nếu password MySQL của bạn KHÁC `123456`:**
1. Chạy script: `setup.bat` (Windows) hoặc `./setup.sh` (Linux/Mac)
2. Mở file `.env` vừa tạo
3. Sửa dòng: `DB_PASSWORD=your_actual_password`
4. Lưu và chạy lại ứng dụng

**Nếu không tạo `.env` với password đúng → Ứng dụng sẽ BÁO LỖI kết nối database!**

---

## 🔧 Khi nào cần tạo .env?

### ⚠️ BẮT BUỘC tạo .env nếu:

- ❗ **Password MySQL của bạn KHÁC `123456`**
- ❗ **Username MySQL của bạn KHÁC `root`**
- ❗ **Database name khác `ff`**

### ✅ Nên tạo .env nếu:

- ✅ Muốn dùng Gmail để gửi email
- ✅ Muốn thay đổi JWT secret key
- ✅ Deploy lên production

### ✅ KHÔNG CẦN tạo .env nếu:

- ✅ Database của bạn đúng là: `root/123456` trên `localhost:3306/ff`
- ✅ Chỉ test/develop local với cấu hình mặc định
- ✅ Muốn chạy nhanh để xem demo

---

## 📚 Tài liệu chi tiết

- [QUICK_START.md](QUICK_START.md) - Hướng dẫn chi tiết
- [SETUP.md](SETUP.md) - Hướng dẫn setup
- [README_ENV.md](README_ENV.md) - Hướng dẫn về .env
- [GIT_WORKFLOW.md](GIT_WORKFLOW.md) - Git workflow

---

## 🎯 Tóm tắt

| Tình huống | Cần làm gì? |
|------------|-------------|
| **Password MySQL = `123456`** | ✅ Không cần gì, chạy `mvn spring-boot:run` luôn |
| **Password MySQL KHÁC `123456`** | ⚠️ **BẮT BUỘC:** Chạy `setup.bat` → Sửa `.env` → Điền password đúng |
| **Username MySQL khác `root`** | ⚠️ **BẮT BUỘC:** Tạo `.env` và sửa `DB_USERNAME` |
| **Database name khác `ff`** | ⚠️ **BẮT BUỘC:** Tạo `.env` và sửa `DB_URL` |
| **Dùng Gmail** | Tạo `.env` và điền `MAIL_USERNAME`, `MAIL_PASSWORD` |
| **Production** | Bắt buộc tạo `.env` với giá trị thực |

### 🔍 Kiểm tra nhanh:

**Câu hỏi:** Password MySQL của bạn là gì?
- ✅ Nếu là `123456` → Chạy ngay, không cần `.env`
- ❌ Nếu KHÁC `123456` → **PHẢI** tạo `.env` trước!

---

## ⚠️ Lưu ý bảo mật

- File `.env` đã được gitignore, **KHÔNG** bị commit lên GitHub
- Giá trị mặc định chỉ dùng cho **development**
- **KHÔNG** dùng giá trị mặc định trên **production**

---

Enjoy coding! 🚀

