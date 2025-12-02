package course.examples.nt118.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable; // Thêm Serializable để truyền qua Intent dễ dàng
import java.util.List;
import java.util.Objects;

public class Post implements Serializable { // Implement Serializable

    // =========================================================
    // 1. MAPPING VỚI JSON TỪ SERVER
    // =========================================================

    @SerializedName("_id")
    private String _id;

    @SerializedName(value = "userID", alternate = {"user", "author"})
    private String userID;

    @SerializedName(value = "caption", alternate = {"content", "description"})
    private String caption;

    @SerializedName("media")
    private List<String> media;

    @SerializedName("type")
    private String type;

    @SerializedName(value = "likes", alternate = {"like", "likeCount"})
    private int like;

    @SerializedName(value = "comments", alternate = {"comment", "commentCount"})
    private int comment;

    // Map các trạng thái từ Server (meLike, meSave)
    @SerializedName(value = "meLike", alternate = {"isLiked"})
    private boolean meLike;

    // Map meSave vào biến bookmarked cho UI
    @SerializedName(value = "isBookmarked", alternate = {"saved", "bookmarked", "meSave", "meSaved"})
    private boolean isBookmarked;

    // Map meFollow (nếu có) hoặc để mặc định false
    @SerializedName(value = "isFollowed", alternate = {"followed", "meFollow"})
    private boolean isFollowed;

    @SerializedName("createdAt")
    private String createdAt;

    // =========================================================
    // 2. CÁC TRƯỜNG UI (Set thủ công sau khi lấy User Info)
    // =========================================================
    private String userName;
    private String userAvatar;

    // =========================================================
    // 3. GETTERS & SETTERS
    // =========================================================
    public String get_id() { return _id; }
    public void set_id(String _id) { this._id = _id; }

    public String getUserID() { return userID; }
    public void setUserID(String userID) { this.userID = userID; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public List<String> getMedia() { return media; }
    public void setMedia(List<String> media) { this.media = media; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getLike() { return like; }
    public void setLike(int like) { this.like = like; }

    public int getComment() { return comment; }
    public void setComment(int comment) { this.comment = comment; }

    public boolean isMeLike() { return meLike; }
    public void setMeLike(boolean meLike) { this.meLike = meLike; }

    public boolean isBookmarked() { return isBookmarked; }
    public void setBookmarked(boolean bookmarked) { isBookmarked = bookmarked; }

    public boolean isFollowed() { return isFollowed; }
    public void setFollowed(boolean followed) { isFollowed = followed; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }

    public String getCreatedAt() { return createdAt; }

    // =========================================================
    // 4. EQUALS & HASHCODE (Quan trọng cho RecyclerView tối ưu)
    // =========================================================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Post post = (Post) o;
        return like == post.like &&
                comment == post.comment &&
                meLike == post.meLike &&
                isFollowed == post.isFollowed &&
                isBookmarked == post.isBookmarked &&
                Objects.equals(_id, post._id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_id, like, comment, meLike, isBookmarked);
    }
}