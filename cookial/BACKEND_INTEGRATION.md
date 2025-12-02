# 🔄 Tích hợp Backend Post Service

## 📋 Tóm tắt thay đổi

Đã cập nhật code Android để phù hợp với backend Post service.

## 🔧 Các thay đổi chính

### 1. Model Classes

#### PostResponse.java
- ✅ Cập nhật để phù hợp với schema MongoDB:
  - `_id`: ID của post
  - `userID`: ID của người đăng
  - `type`: Loại post (Moment / Rate / Tip / Recipe)
  - `caption`: Nội dung bài viết
  - `tag`: Mảng các tag
  - `media`: Mảng URL ảnh/video
  - `like`: Số lượng like
  - `location`: Thông tin địa điểm
  - `comment`: Mảng comment (đệ quy)
  - `createdAt`: Thời gian tạo
  - `meLike`: Boolean - user hiện tại đã like chưa

#### PostsResponse.java (MỚI)
- ✅ Wrapper class cho response từ GET /posts:
  - `success`: Boolean
  - `posts`: List<PostResponse>
  - `nextCursor`: String (cho pagination)

### 2. API Service

#### ApiService.java
- ✅ Cập nhật endpoints:
  - `GET /posts?userID=xxx&after=xxx` - Lấy posts với cursor pagination
  - `POST /posts/upload` - Upload bài viết (với media)
  - `POST /posts/like` - Like bài viết (body: userID, postID)
  - `DELETE /posts/like` - Unlike bài viết (body: userID, postID)

### 3. HomeActivity.java

- ✅ Cập nhật `loadPosts()`:
  - Gửi `userID` trong query params (bắt buộc)
  - Xử lý `PostsResponse` thay vì `List<PostResponse>`
  - Load thông tin user (name, avatar) cho mỗi post
  - Tự động tạo posts mẫu nếu API fail

### 4. PostAdapter.java

- ✅ Cập nhật để phù hợp với backend:
  - Nhận `currentUserID` trong constructor
  - Sử dụng `caption` thay vì `content`
  - Sử dụng `media` thay vì `images`
  - Sử dụng `meLike` từ backend
  - Format thời gian từ ISO 8601
  - Like/Unlike gửi đúng body: `{userID, postID}`
  - Ẩn share button (backend không có)

### 5. Layout

#### item_post.xml
- ✅ Ẩn share button và share count (backend không có tính năng này)

## 📡 API Endpoints

### GET /posts
**Query Params:**
- `userID` (bắt buộc): ID của user hiện tại
- `after` (optional): Cursor cho pagination

**Response:**
```json
{
  "success": true,
  "posts": [...],
  "nextCursor": "2025-11-08T13:00:00.000Z"
}
```

### POST /posts/like
**Body:**
```json
{
  "userID": "user123",
  "postID": "post456"
}
```

### DELETE /posts/like
**Body:**
```json
{
  "userID": "user123",
  "postID": "post456"
}
```

## ⚠️ Lưu ý

1. **userID bắt buộc**: Tất cả API calls đều cần `userID` của user hiện tại
2. **Không có Share**: Backend không có tính năng share, đã ẩn UI
3. **Pagination**: Sử dụng cursor-based pagination với `after` parameter
4. **meLike**: Backend tự động thêm field `meLike` vào mỗi post dựa trên `userID`

## 🧪 Test

1. **Demo Mode**: Long press nút đăng nhập để test HomeActivity không cần server
2. **Với Server**: Đảm bảo backend Post service đang chạy trên port 3000 (hoặc port được config trong gateway)

## 🔄 Cần cập nhật sau

1. **User Info Caching**: Tối ưu việc load user info (name, avatar) để tránh gọi API nhiều lần
2. **Pagination**: Implement load more posts khi scroll xuống cuối
3. **Upload Post**: Implement tính năng upload post với media
4. **Comments**: Implement tính năng comment (backend đã có schema)


