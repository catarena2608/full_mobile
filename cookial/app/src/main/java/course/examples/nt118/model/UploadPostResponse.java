package course.examples.nt118.model;

import com.google.gson.annotations.SerializedName;

public class UploadPostResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    // Đây là object bài viết vừa tạo xong (trùng khớp với class PostResponse cũ của bạn)
    @SerializedName("post")
    private Post post;

    // --- GETTERS ---
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Post getPost() {
        return post;
    }

    // --- SETTERS ---
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setPost(Post post) {
        this.post = post;
    }
}