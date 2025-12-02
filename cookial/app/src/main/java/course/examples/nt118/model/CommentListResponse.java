package course.examples.nt118.model;

import java.util.List;

public class CommentListResponse {

    private boolean success;
    private String nextCursor;
    private List<CommentResponse> comments;

    public CommentListResponse() {}

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getNextCursor() { return nextCursor; }
    public void setNextCursor(String nextCursor) { this.nextCursor = nextCursor; }

    public List<CommentResponse> getComments() { return comments; }
    public void setComments(List<CommentResponse> comments) { this.comments = comments; }
}
