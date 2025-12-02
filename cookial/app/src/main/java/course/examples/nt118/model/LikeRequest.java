package course.examples.nt118.model;

// Model này đại diện cho body của request Like/Unlike
public class LikeRequest {
    private String userID;
    private String postID;

    // Constructor mới (cần 2 đối số)
    public LikeRequest(String userID, String postID) {
        this.userID = userID;
        this.postID = postID;
    }

    // Getters (Để Retrofit/Gson hoạt động)
    public String getUserID() {
        return userID;
    }

    public String getPostID() {
        return postID;
    }
}