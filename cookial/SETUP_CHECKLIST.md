# ✅ Checklist Setup Dự án Android

## 📋 Trước khi bắt đầu

- [ ] Android Studio đã được cài đặt (Hedgehog | 2023.1.1+)
- [ ] JDK 11+ đã được cài đặt
- [ ] Android SDK đã được cài đặt (API 24-36)
- [ ] Backend services đã sẵn sàng chạy

## 🔧 Cấu hình Project

- [x] Project đã được mở trong Android Studio
- [x] Gradle sync thành công
- [ ] Cấu hình API Base URL trong `ApiConfig.java`
  - [ ] Cho Emulator: `USE_EMULATOR = true`
  - [ ] Cho Device: `USE_EMULATOR = false`, cập nhật IP
- [ ] Kiểm tra dependencies trong `build.gradle.kts`

## 🚀 Chạy Backend Services

- [ ] Gateway đang chạy (Port 3000)
- [ ] Auth Service đang chạy (Port 3001)
- [ ] User Service đang chạy (Port 3002)
- [ ] Post Service đang chạy (Port 4001)

## 📱 Test trên Emulator/Device

- [ ] App khởi động thành công
- [ ] Màn hình Login hiển thị
- [ ] Đăng ký tài khoản mới hoạt động
- [ ] Đăng nhập thành công
- [ ] Xem profile hoạt động
- [ ] Chỉnh sửa profile hoạt động
- [ ] API calls hoạt động (kiểm tra Logcat)

## 🐛 Troubleshooting

Nếu gặp lỗi:

1. **Build errors:**
   - [ ] Clean Project
   - [ ] Rebuild Project
   - [ ] Invalidate Caches

2. **API connection errors:**
   - [ ] Kiểm tra backend services đang chạy
   - [ ] Kiểm tra IP/Port trong ApiConfig
   - [ ] Kiểm tra Firewall
   - [ ] Kiểm tra network_security_config.xml

3. **Runtime errors:**
   - [ ] Kiểm tra Logcat để xem error messages
   - [ ] Kiểm tra RetrofitClient đã được init
   - [ ] Kiểm tra API responses

## 📝 Files quan trọng

- [x] `ApiConfig.java` - Cấu hình API base URL
- [x] `RetrofitClient.java` - HTTP client setup
- [x] `CookialApplication.java` - Application class
- [x] `AndroidManifest.xml` - App configuration
- [x] `build.gradle.kts` - Dependencies
- [x] `network_security_config.xml` - Network security

## ✨ Tính năng đã test

- [ ] Đăng ký
- [ ] Đăng nhập
- [ ] Xem profile
- [ ] Chỉnh sửa profile
- [ ] Quên mật khẩu (nếu có)

## 📚 Tài liệu

- [x] README.md - Hướng dẫn chi tiết
- [x] QUICKSTART.md - Hướng dẫn nhanh
- [x] SETUP_CHECKLIST.md - Checklist này

---

**Lưu ý:** Đánh dấu các mục đã hoàn thành để theo dõi tiến độ!

