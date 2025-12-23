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

    @SerializedName(value = "_id", alternate = {"id"})
    private String id;

    @SerializedName("userID")
    private String userId;

    @SerializedName("actorID")
    private String actorId;

    // 🔥 SỬA ĐOẠN NÀY: Thêm các trường dự phòng (alternate)
    // Backend có thể trả về: targetID, targetId, postID, postId, entityID...
    @SerializedName(value = "targetID", alternate = {"targetId", "postID", "postId", "entityID", "entityId"})
    private String targetId;

    @SerializedName("type")
    private String type;

    @SerializedName("isRead")
    private boolean isRead;

    @SerializedName("createdAt")
    private String createdAt;

    private UserResponse actor;

    // ... (Giữ nguyên các Constructor, Getter, Setter và hàm getDescription bên dưới)
    public Notify() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public UserResponse getActor() { return actor; }
    public void setActor(UserResponse actor) { this.actor = actor; }

    public Date getCreatedAt() {
        if (createdAt == null) return new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            String dateString = createdAt;
            if (createdAt.length() > 23) dateString = createdAt.substring(0, 23);
            return sdf.parse(dateString);
        } catch (ParseException e) {
            return new Date();
        }
    }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getDescription() {
        if (type == null) return "đã có tương tác mới.";
        switch (type) {
            case "like": return "đã thích bài viết của bạn.";
            case "comment": return "đã bình luận về bài viết của bạn.";
            case "new_post": return "đã đăng một bài viết mới.";
            case "follow": return "đã bắt đầu theo dõi bạn.";
            case "reply": return "đã trả lời bình luận của bạn.";
            default: return "đã gửi một thông báo.";
        }
    }

    // 🔥 Thêm hàm này để Debug dễ hơn
    @Override
    public String toString() {
        return "Notify{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", targetId='" + targetId + '\'' +
                ", actorId='" + actorId + '\'' +
                '}';
    }
}