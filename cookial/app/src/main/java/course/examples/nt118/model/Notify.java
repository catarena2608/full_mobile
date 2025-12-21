package course.examples.nt118.model;

import android.util.Log;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Notify implements Serializable {

    @SerializedName("_id")
    private String id;

    @SerializedName("userID")
    private String userId;

    @SerializedName("actorID")
    private String actorId;

    @SerializedName("targetID")
    private String targetId;

    @SerializedName("type")
    private String type;

    @SerializedName("isRead")
    private boolean isRead;

    // Giữ nguyên là String để Gson hứng dữ liệu thô từ Server không bị lỗi
    @SerializedName("createdAt")
    private String createdAt;

    // ================== CONSTRUCTORS ==================

    public Notify() {
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

    /**
     * 🔥 QUAN TRỌNG: Hàm này đã được sửa.
     * Nó sẽ parse chuỗi ISO 8601 từ Server thành đối tượng Date của Java.
     * Giúp Activity so sánh được ngày tháng.
     */
    public Date getCreatedAt() {
        if (createdAt == null) return new Date(); // Trả về thời gian hiện tại nếu null

        // Định dạng ngày tháng chuẩn ISO 8601 của MongoDB/NodeJS
        // Ví dụ: 2023-12-16T10:00:00.000Z
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Server lưu giờ UTC

        try {
            return sdf.parse(createdAt);
        } catch (ParseException e) {
            Log.e("NotifyModel", "Lỗi parse ngày tháng: " + createdAt);
            return new Date(); // Fallback về hiện tại nếu lỗi
        }
    }

    // Hàm setter vẫn nhận String (để Gson dùng hoặc khi set thủ công)
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    // ================== HELPER METHODS ==================

    public String getDescription() {
        if (type == null) return "Có thông báo mới";

        switch (type) {
            case "new_post":
                return "đã đăng bài viết mới.";
            case "like":
                return "đã thích bài viết của bạn.";
            case "comment":
                return "đã bình luận bài viết.";
            case "reply":
                return "đã trả lời bình luận.";
            case "follow":
                return "đã theo dõi bạn.";
            default:
                return "đã tương tác với bạn.";
        }
    }
}