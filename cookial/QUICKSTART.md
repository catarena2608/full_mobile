# 🚀 Hướng dẫn nhanh - Cookial Mobile App

## Bước 1: Mở project trong Android Studio

1. Mở **Android Studio**
2. Chọn **File > Open**
3. Chọn thư mục `MobileApp`
4. Đợi Gradle sync hoàn tất

## Bước 2: Cấu hình API Base URL

Mở file: `app/src/main/java/course/examples/nt118/config/ApiConfig.java`

### Cho Android Emulator (mặc định):
```java
private static final boolean USE_EMULATOR = true;
```

### Cho thiết bị thật:
1. Tìm IP máy tính của bạn:
   - **Windows**: Mở CMD, gõ `ipconfig`
   - **Mac/Linux**: Mở Terminal, gõ `ifconfig`
2. Thay IP trong `DEVICE_BASE_URL`:
```java
private static final String DEVICE_BASE_URL = "http://192.168.1.XXX:3000/api/";
private static final boolean USE_EMULATOR = false;
```

## Bước 3: Chạy Backend Services

Trước khi chạy app, đảm bảo các backend services đang chạy:

```bash
# Terminal 1: Gateway (Port 3000)
cd gateway/gateway
node server.js

# Terminal 2: Auth Service (Port 3001)
cd BackEnd_auth/BackEnd_auth
node server.js

# Terminal 3: User Service (Port 3002)
cd BackEnd_user/BackEnd_user
node server.js

# Terminal 4: Post Service (Port 4001)
cd BackEnd_post/BackEnd_post
node server.js
```

## Bước 4: Chạy ứng dụng

### Trên Emulator:
1. Tạo/chọn Android Emulator (API 24+)
2. Click **Run** (▶️) hoặc `Shift + F10`

### Trên thiết bị thật:
1. Bật **USB Debugging** trên điện thoại
2. Kết nối điện thoại qua USB
3. Chọn device trong Android Studio
4. Click **Run**

## ✅ Kiểm tra

- App khởi động thành công
- Màn hình Login hiển thị
- Có thể đăng ký/đăng nhập
- Kết nối API thành công

## ❌ Troubleshooting

### Lỗi kết nối API:
- Kiểm tra backend services đang chạy
- Kiểm tra IP/Port trong `ApiConfig.java`
- Kiểm tra Firewall

### Lỗi build:
- **Build > Clean Project**
- **Build > Rebuild Project**
- **File > Invalidate Caches**

## 📱 Tính năng hiện có

- ✅ Đăng ký/Đăng nhập
- ✅ Xem/Chỉnh sửa Profile
- ✅ Quên mật khẩu (OTP)

## 📚 Xem thêm

Chi tiết đầy đủ trong file `README.md`

