package course.examples.nt118.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PostsResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName(value = "posts", alternate = {"data", "results"})
    private List<Post> posts; // [QUAN TRỌNG] Dùng ngay class Post

    @SerializedName(value = "nextCursor", alternate = {"next_cursor", "cursor"})
    private String nextCursor;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }

    // Getter trả về đúng List<Post> để ném thẳng vào Adapter
    public List<Post> getPosts() { return posts; }

    public String getNextCursor() { return nextCursor; }
}