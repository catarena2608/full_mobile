# 🔍 Debug - Không vào được HomeActivity sau khi đăng nhập

## 📋 Các bước kiểm tra

### 1. Kiểm tra Logcat
1. Mở tab **Logcat** ở dưới Android Studio
2. Filter theo tag: `LoginActivity` hoặc `HomeActivity`
3. Chạy app và thử đăng nhập
4. Xem các log messages:

**Nếu thấy:**
- `"Đăng nhập thành công! User ID: ..."` → Login thành công
- `"Đang chuyển đến HomeActivity với USER_ID: ..."` → Đang chuyển màn hình
- `"Layout đã được load thành công"` → HomeActivity đã load layout
- `"HomeActivity đã được khởi tạo thành công"` → HomeActivity hoạt động

**Nếu thấy lỗi (màu đỏ):**
- Copy toàn bộ log lỗi và gửi lại

### 2. Kiểm tra Build Output
1. Xem tab **Build** ở dưới Android Studio
2. Kiểm tra xem có lỗi build không
3. Nếu có lỗi, copy và gửi lại

### 3. Clean và Rebuild
1. **Build** → **Clean Project**
2. **Build** → **Rebuild Project**
3. Đợi build xong
4. Chạy lại app

### 4. Kiểm tra Dependencies
Đảm bảo các dependencies sau đã được thêm vào `build.gradle.kts`:
```kotlin
implementation("androidx.recyclerview:recyclerview:1.3.2")
implementation("androidx.cardview:cardview:1.0.0")
```

### 5. Kiểm tra Layout Files
Đảm bảo các file sau tồn tại:
- `app/src/main/res/layout/activity_home.xml`
- `app/src/main/res/layout/item_post.xml`

### 6. Kiểm tra AndroidManifest
Đảm bảo `HomeActivity` đã được khai báo trong `AndroidManifest.xml`:
```xml
<activity
    android:name=".HomeActivity"
    android:exported="false"
    android:windowSoftInputMode="adjustResize|stateHidden" />
```

## 🐛 Các lỗi thường gặp

### Lỗi: "ActivityNotFoundException"
- **Nguyên nhân**: HomeActivity chưa được khai báo trong Manifest
- **Giải pháp**: Kiểm tra AndroidManifest.xml

### Lỗi: "Resource not found"
- **Nguyên nhân**: Layout file không tồn tại hoặc tên sai
- **Giải pháp**: 
  1. Kiểm tra file `activity_home.xml` có tồn tại không
  2. Clean và Rebuild Project

### Lỗi: "NullPointerException"
- **Nguyên nhân**: View không được tìm thấy hoặc null
- **Giải pháp**: 
  1. Kiểm tra layout có đúng ID không
  2. Xem Logcat để biết view nào bị null

### Lỗi: "ClassNotFoundException"
- **Nguyên nhân**: Thiếu dependencies hoặc class không được compile
- **Giải pháp**: 
  1. Clean và Rebuild
  2. Sync Gradle
  3. Invalidate Caches

## 📝 Log Messages cần tìm

### Thành công:
```
D/LoginActivity: Đăng nhập thành công! User ID: xxx
D/LoginActivity: Đang chuyển đến HomeActivity với USER_ID: xxx
D/HomeActivity: Layout đã được load thành công
D/HomeActivity: User ID từ intent: xxx
D/HomeActivity: Views đã được khởi tạo thành công
D/HomeActivity: RecyclerView đã được setup thành công
D/HomeActivity: HomeActivity đã được khởi tạo thành công
```

### Lỗi:
```
E/LoginActivity: User ID là null hoặc rỗng!
E/LoginActivity: Lỗi khi chuyển đến HomeActivity: ...
E/HomeActivity: Lỗi khi load layout: ...
E/HomeActivity: Không tìm thấy một hoặc nhiều view trong layout!
E/HomeActivity: Lỗi khi khởi tạo HomeActivity: ...
```

## 🚀 Nếu vẫn không được

1. **Gửi Logcat**: Copy toàn bộ log từ Logcat khi chạy app và thử đăng nhập
2. **Gửi Build Output**: Copy toàn bộ output từ tab Build
3. **Gửi Screenshot**: Chụp màn hình Android Studio và thiết bị/emulator
4. **Mô tả chi tiết**: 
   - App có crash không?
   - Có thông báo lỗi gì không?
   - Màn hình có hiển thị gì không?

## ✅ Checklist nhanh

- [ ] Clean và Rebuild Project
- [ ] Sync Gradle
- [ ] Kiểm tra Logcat
- [ ] Kiểm tra Build Output
- [ ] Kiểm tra AndroidManifest.xml
- [ ] Kiểm tra Layout files tồn tại
- [ ] Kiểm tra Dependencies đã sync
- [ ] Invalidate Caches


