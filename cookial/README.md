# Cookial Mobile App - Android

Ứng dụng Android cho Cookial - Mạng xã hội ẩm thực. Ứng dụng này kết nối đến Backend API thông qua API Gateway.

## 📋 Yêu cầu

- **Android Studio**: Hedgehog | 2023.1.1 trở lên
- **JDK**: 11 trở lên
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36 (Android 15)
- **Gradle**: 8.13.0
- **Kotlin**: Không (Dự án sử dụng Java)

## 🚀 Cài đặt

### 1. Clone/Mở dự án

```bash
# Mở Android Studio
# File > Open > Chọn thư mục MobileApp
```

### 2. Đồng bộ Gradle

Android Studio sẽ tự động đồng bộ Gradle khi mở project. Nếu không, click **Sync Now** khi có thông báo.

### 3. Cấu hình API Base URL

Mở file `app/src/main/java/course/examples/nt118/config/ApiConfig.java`:

```java
// Cho Android Emulator (mặc định)
private static final String EMULATOR_BASE_URL = "http://10.0.2.2:3000/api/";

// Cho thiết bị thật - Thay IP này thành IP máy tính của bạn
private static final String DEVICE_BASE_URL = "http://192.168.1.100:3000/api/";

// Chọn môi trường
private static final boolean USE_EMULATOR = true; // true = Emulator, false = Device
```

**Lưu ý khi test trên thiết bị thật:**
1. Đảm bảo máy tính và điện thoại cùng mạng WiFi
2. Tìm IP máy tính:
   - **Windows**: Mở CMD, gõ `ipconfig`, tìm IPv4 Address
   - **Mac/Linux**: Mở Terminal, gõ `ifconfig`, tìm inet
3. Thay IP trong `DEVICE_BASE_URL`
4. Đặt `USE_EMULATOR = false`
5. Đảm bảo Backend Gateway đang chạy trên máy tính

### 4. Chạy Backend Services

Trước khi chạy app, đảm bảo các Backend services đang chạy:

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

## 🏃 Chạy ứng dụng

### Trên Android Emulator:

1. Mở Android Studio
2. Tạo/Chọn Android Emulator (API 24+)
3. Click **Run** (▶️) hoặc `Shift + F10`
4. App sẽ tự động build và cài đặt

### Trên thiết bị thật:

1. Bật **Developer Options** trên điện thoại:
   - Vào Settings > About Phone
   - Tap 7 lần vào "Build Number"
2. Bật **USB Debugging**:
   - Settings > Developer Options > USB Debugging
3. Kết nối điện thoại qua USB
4. Cho phép USB Debugging khi có popup
5. Chọn device trong Android Studio
6. Click **Run**

## 📱 Tính năng

### Đã triển khai:
- ✅ Đăng ký tài khoản
- ✅ Đăng nhập/Đăng xuất
- ✅ Xem profile người dùng
- ✅ Chỉnh sửa profile
- ✅ Quên mật khẩu (OTP qua email)
- ✅ Cookie-based authentication
- ✅ JWT token management
- ✅ Auto-retry với token

### Đang phát triển:
- ⏳ Upload bài viết
- ⏳ Xem newsfeed
- ⏳ Like/Unlike bài viết
- ⏳ Comment bài viết
- ⏳ Follow/Unfollow users

## 🏗️ Cấu trúc dự án

```
MobileApp/
├── app/
│   └── src/
│       └── main/
│           ├── java/
│           │   └── course/examples/nt118/
│           │       ├── config/
│           │       │   └── ApiConfig.java          # Cấu hình API
│           │       ├── model/                      # Data models
│           │       │   ├── LoginRequest.java
│           │       │   ├── LoginResponse.java
│           │       │   ├── RegisterRequest.java
│           │       │   └── UserResponse.java
│           │       ├── network/
│           │       │   ├── ApiService.java         # Retrofit API interface
│           │       │   └── RetrofitClient.java     # Retrofit client setup
│           │       ├── utils/
│           │       │   └── TokenManager.java       # JWT token management
│           │       ├── LoginActivity.java
│           │       ├── RegisterActivity.java
│           │       ├── ProfileActivity.java
│           │       └── EditProfileActivity.java
│           ├── res/                                # Resources
│           │   ├── layout/                         # XML layouts
│           │   ├── drawable/                       # Images, icons
│           │   └── values/                         # Strings, colors, styles
│           └── AndroidManifest.xml
├── build.gradle.kts                                # Project-level build config
├── settings.gradle.kts
└── gradle.properties
```

## 🔧 Cấu hình

### Dependencies chính:

- **Retrofit 2.9.0**: HTTP client cho API calls
- **OkHttp 4.11.0**: HTTP client với cookie support
- **Gson**: JSON parsing
- **Glide 4.15.1**: Image loading
- **Material Design**: UI components
- **CircleImageView**: Circular image views

### Network Security:

File `res/xml/network_security_config.xml` cho phép cleartext traffic cho localhost và emulator.

## 🐛 Troubleshooting

### Lỗi kết nối API:

1. **Kiểm tra Backend đang chạy:**
   ```bash
   # Test Gateway
   curl http://localhost:3000
   ```

2. **Kiểm tra IP và Port:**
   - Emulator: `10.0.2.2:3000`
   - Device: `YOUR_COMPUTER_IP:3000`

3. **Kiểm tra Firewall:**
   - Tắt Windows Firewall tạm thời hoặc cho phép port 3000

4. **Kiểm tra Network Security Config:**
   - Đảm bảo `network_security_config.xml` có domain đúng

### Lỗi build:

1. **Clean và Rebuild:**
   ```
   Build > Clean Project
   Build > Rebuild Project
   ```

2. **Invalidate Caches:**
   ```
   File > Invalidate Caches > Invalidate and Restart
   ```

3. **Kiểm tra JDK:**
   - File > Project Structure > SDK Location
   - Đảm bảo JDK 11+ được chọn

### Lỗi token authentication:

1. Clear app data:
   - Settings > Apps > Cookial > Clear Data
2. Đăng nhập lại
3. Kiểm tra TokenManager có lưu token đúng không

## 📝 API Endpoints

App sử dụng các endpoints sau:

### Authentication:
- `POST /api/auth/register` - Đăng ký
- `POST /api/auth/login` - Đăng nhập
- `GET /api/auth/me` - Kiểm tra token
- `POST /api/auth/logout` - Đăng xuất
- `POST /api/auth/forgot-password` - Quên mật khẩu
- `POST /api/auth/verify-otp` - Xác minh OTP
- `POST /api/auth/reset-password` - Đặt lại mật khẩu

### Users:
- `GET /api/users` - Lấy tất cả users
- `GET /api/users/:id` - Lấy user theo ID
- `POST /api/users/editProfile/:id` - Chỉnh sửa profile

## 👨‍💻 Phát triển

### Thêm tính năng mới:

1. **Thêm API endpoint:**
   - Mở `ApiService.java`
   - Thêm method mới với annotation Retrofit

2. **Thêm Activity:**
   - Tạo class mới extends `AppCompatActivity`
   - Thêm vào `AndroidManifest.xml`
   - Tạo layout XML trong `res/layout/`

3. **Cập nhật Config:**
   - Nếu cần thay đổi API base URL, sửa `ApiConfig.java`

### Best Practices:

- ✅ Sử dụng Retrofit cho tất cả API calls
- ✅ Sử dụng Glide cho image loading
- ✅ Lưu token bằng TokenManager
- ✅ Xử lý errors đúng cách
- ✅ Sử dụng Material Design components
- ✅ Follow Android coding conventions

## 📄 License

ISC

## 👥 Tác giả

Cookial Mobile App Team

