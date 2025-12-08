package course.examples.nt118.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Comment implements Serializable {

    private String id;
    private String postId;
    private String userId;

    // Thông tin user hiển thị trên UI
    private String userName;
    private String userAvatar;

    private String content;
    private Date createdAt;

    // Dùng để xử lý logic hiển thị
    private int depth;
    private List<Comment> replies = new ArrayList<>();

    public Comment() {}

    // --- Constructor tiện ích (Optional) ---
    public Comment(String id, String content, String userId, int depth) {
        this.id = id;
        this.content = content;
        this.userId = userId;
        this.depth = depth;
    }

    // --- GETTERS & SETTERS ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }

    public List<Comment> getReplies() { return replies; }
    public void setReplies(List<Comment> replies) { this.replies = replies; }
}