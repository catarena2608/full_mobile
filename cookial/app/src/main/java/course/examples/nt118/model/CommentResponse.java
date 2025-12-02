package course.examples.nt118.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommentResponse {

    private String _id;           // ID comment
    private String postId;        // ID bài viết
    private String userId;        // ID người comment
    private String content;       // Nội dung comment
    private Date createdAt;       // Ngày tạo
    private String userName;      // Tên người comment
    private String userAvatar;    // Ảnh đại diện người comment
    private int depth;            // depth comment (0,1,2)
    private List<CommentResponse> replies = new ArrayList<>(); // reply lồng nhau

    public CommentResponse() {
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public List<CommentResponse> getReplies() {
        return replies;
    }

    public void setReplies(List<CommentResponse> replies) {
        this.replies = replies;
    }
}
