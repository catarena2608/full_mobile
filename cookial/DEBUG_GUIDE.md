# 🔍 Hướng dẫn Debug - App không hiển thị màn hình đăng nhập

## 📋 Các bước kiểm tra và sửa lỗi

### 1. Clean và Rebuild Project
1. Trong Android Studio, vào **Build** → **Clean Project**
2. Sau đó **Build** → **Rebuild Project**
3. Đợi cho đến khi build xong (xem thanh status bar ở dưới)

### 2. Kiểm tra Logcat
1. Mở tab **Logcat** ở dưới Android Studio
2. Filter theo tag: `LoginActivity` hoặc `CookialApplication`
3. Chạy app và xem có lỗi gì không
4. Tìm các dòng có màu đỏ (ERROR) hoặc vàng (WARN)

### 3. Kiểm tra Build Output
1. Xem tab **Build** ở dưới Android Studio
2. Kiểm tra xem có lỗi build không
3. Nếu có lỗi, copy và gửi lại để được hỗ trợ

### 4. Kiểm tra APK có được cài đặt không
1. Chạy app
2. Kiểm tra xem app có xuất hiện trong danh sách apps trên thiết bị/emulator không
3. Nếu có, thử mở app từ danh sách apps

### 5. Kiểm tra Manifest
- Đảm bảo `LoginActivity` có `intent-filter` với `MAIN` và `LAUNCHER`
- Đảm bảo `CookialApplication` được khai báo đúng

### 6. Kiểm tra Resources
- Đảm bảo file `activity_login.xml` tồn tại trong `app/src/main/res/layout/`
- Đảm bảo các drawable được tham chiếu tồn tại:
  - `facecook_logo.png`
  - `edittext_rounded_bg.xml`
  - `button_orange_rounded.xml`
- Đảm bảo color `colorOrange` được định nghĩa trong `colors.xml`

### 7. Kiểm tra Dependencies
- Đảm bảo tất cả dependencies đã được sync thành công
- Vào **File** → **Sync Project with Gradle Files**

### 8. Invalidate Caches
1. Vào **File** → **Invalidate Caches...**
2. Chọn **Invalidate and Restart**
3. Đợi Android Studio khởi động lại

## 🐛 Các lỗi thường gặp

### Lỗi: "App keeps stopping"
- **Nguyên nhân**: Crash khi khởi động
- **Giải pháp**: 
  1. Xem Logcat để tìm lỗi cụ thể
  2. Kiểm tra xem có exception nào không
  3. Đảm bảo tất cả resources đều tồn tại

### Lỗi: "Cannot resolve symbol R"
- **Nguyên nhân**: Build chưa hoàn tất hoặc có lỗi trong resources
- **Giải pháp**:
  1. Clean và Rebuild Project
  2. Kiểm tra các file XML trong res/ có đúng syntax không
  3. Sync Gradle

### Lỗi: "Resource not found"
- **Nguyên nhân**: Thiếu resource hoặc tên sai
- **Giải pháp**:
  1. Kiểm tra tên file trong layout
  2. Đảm bảo file tồn tại trong đúng thư mục
  3. Clean và Rebuild

### Lỗi: "Activity not found"
- **Nguyên nhân**: Activity chưa được khai báo trong Manifest hoặc tên sai
- **Giải pháp**:
  1. Kiểm tra AndroidManifest.xml
  2. Đảm bảo package name đúng
  3. Đảm bảo activity có intent-filter với MAIN và LAUNCHER

## 📝 Log Messages để tìm

Sau khi chạy app, tìm các log messages sau trong Logcat:

### Thành công:
```
D/LoginActivity: Layout đã được load thành công
D/LoginActivity: LoginActivity đã được khởi tạo thành công
D/CookialApplication: RetrofitClient đã được khởi tạo thành công
```

### Lỗi:
```
E/LoginActivity: Lỗi khi load layout: ...
E/LoginActivity: Không tìm thấy một hoặc nhiều view trong layout!
E/CookialApplication: Lỗi khi khởi tạo RetrofitClient: ...
```

## 🚀 Nếu vẫn không được

1. **Gửi Logcat**: Copy toàn bộ log từ Logcat khi chạy app
2. **Gửi Build Output**: Copy toàn bộ output từ tab Build
3. **Gửi Screenshot**: Chụp màn hình Android Studio và thiết bị/emulator

## ✅ Checklist nhanh

- [ ] Clean và Rebuild Project
- [ ] Sync Gradle
- [ ] Invalidate Caches
- [ ] Kiểm tra Logcat
- [ ] Kiểm tra Build Output
- [ ] Kiểm tra AndroidManifest.xml
- [ ] Kiểm tra Resources tồn tại
- [ ] Kiểm tra Dependencies đã sync


