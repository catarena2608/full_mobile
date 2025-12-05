package course.examples.nt118.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Notify implements Serializable {

    // Map trường "_id" từ MongoDB sang biến "id" trong Java
    @SerializedName("_id")
    private String id;

    // Backend gửi "userID" -> map sang "userId"
    @SerializedName("userID")
    private String userId;

    // ⚠️ QUAN TRỌNG: Backend gửi "actorID" (chữ D hoa), cần map chính xác
    @SerializedName("actorID")
    private String actorId;

    // ⚠️ QUAN TRỌNG: Backend gửi "targetID" (chữ D hoa)
    @SerializedName("targetID")
    private String targetId;

    @SerializedName("type")
    private String type; // Các loại: "new_post", "like", "comment", "reply", "follow"

    @SerializedName("isRead")
    private boolean isRead;

    @SerializedName("createdAt")
    private String createdAt;

    // ================== CONSTRUCTORS ==================

    public Notify() {
    }

    public Notify(String id, String userId, String actorId, String targetId, String type, boolean isRead, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.actorId = actorId;
        this.targetId = targetId;
        this.type = type;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    // ================== GETTERS & SETTERS ==================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    // ================== HELPER METHODS ==================

    // Hàm hỗ trợ tạo nội dung hiển thị nhanh
    public String getDescription() {
        if (type == null) return "Có thông báo mới";

        switch (type) {
            case "new_post":
                return "đã đăng một bài viết mới.";
            case "like":
                return "đã thích bài viết của bạn.";
            case "comment":
                return "đã bình luận về bài viết của bạn.";
            case "reply":
                return "đã trả lời bình luận của bạn.";
            case "follow":
                return "đã bắt đầu theo dõi bạn.";
            default:
                return "đã tương tác với bạn.";
        }
    }

    @Override
    public String toString() {
        return "Notify{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", actorId='" + actorId + '\'' +
                ", targetId='" + targetId + '\'' +
                '}';
    }
}