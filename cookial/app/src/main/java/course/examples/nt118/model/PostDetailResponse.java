package course.examples.nt118.model;

/**
 * Response wrapper cho API lấy chi tiết một bài viết.
 */
public class PostDetailResponse {

    private boolean success;
    private Post post;
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

