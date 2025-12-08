package course.examples.nt118.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommentResponse {

    // --- MAPPING ID (Quan trọng) ---
    // JSON trả về "_id" nhưng Java dùng "id" cho thuận tiện
    @SerializedName(value = "_id", alternate = {"id"})
    private String id;

    @SerializedName("postID") // Hoặc "postId" tùy server
    private String postId;

    @SerializedName("userID") // Hoặc "userId"
    private String userId;

    @SerializedName("content")
    private String content;

    @SerializedName(value = "createdAt", alternate = {"created_at"})
    private Date createdAt;

    // --- Các trường thông tin User (Nếu API trả về kèm luôn) ---
    @SerializedName("userName")
    private String userName;

    @SerializedName("userAvatar")
    private String userAvatar;

    // --- Cấu trúc phân cấp ---
    @SerializedName("depth")
    private int depth;

    @SerializedName("replies")
    private List<CommentResponse> replies = new ArrayList<>();

    public CommentResponse() {}

    // --- GETTERS & SETTERS ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }

    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }

    public List<CommentResponse> getReplies() { return replies; }
    public void setReplies(List<CommentResponse> replies) { this.replies = replies; }
}