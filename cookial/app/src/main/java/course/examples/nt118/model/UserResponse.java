package course.examples.nt118.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class UserResponse {

    // ==========================================
    // 1. MAPPING ID (SỬA QUAN TRỌNG)
    // ==========================================
    // Server lúc thì trả về "id" (Login), lúc thì "_id" (Profile)
    // Dùng 'alternate' để bắt cả 2 trường hợp -> Không bao giờ bị null
    @SerializedName(value = "_id", alternate = {"id"})
    private String id;

    // ==========================================
    // 2. CÁC TRƯỜNG DỮ LIỆU
    // ==========================================
    private String name;
    private String email;
    private String avatar;
    private String coverImage;
    private int numPosts;
    private int numFollowed;
    private int numFollowing;
    private List<String> tags;
    private List<String> link;

    // Preference là Object
    private Preference preference;

    @SerializedName("meFollow")
    private boolean meFollow;

    // ==========================================
    // 3. THỦ THUẬT "SELF-WRAPPER" (Xử lý lồng nhau)
    // ==========================================
    // Hứng field "user" nếu JSON trả về dạng { "success": true, "user": {...} }
    @SerializedName("user")
    private UserResponse nestedUser;

    // Hàm thông minh: Tự động trả về data thật dù server trả về kiểu lồng hay không lồng
    public UserResponse getRealUser() {
        return nestedUser != null ? nestedUser : this;
    }

    // ==========================================
    // 4. GETTERS
    // ==========================================
    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getAvatar() { return avatar; }
    public String getCoverImage() { return coverImage; }
    public int getNumPosts() { return numPosts; }
    public int getNumFollowed() { return numFollowed; }
    public int getNumFollowing() { return numFollowing; }
    public List<String> getTags() { return tags; }
    public List<String> getLink() { return link; }
    public Preference getPreference() { return preference; }

    public boolean isMeFollow() { return meFollow; }

    // ==========================================
    // 5. SETTERS
    // ==========================================
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public void setNumPosts(int numPosts) { this.numPosts = numPosts; }
    public void setNumFollowed(int numFollowed) { this.numFollowed = numFollowed; }
    public void setNumFollowing(int numFollowing) { this.numFollowing = numFollowing; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void setLink(List<String> link) { this.link = link; }
    public void setPreference(Preference preference) { this.preference = preference; }
    public void setMeFollow(boolean meFollow) { this.meFollow = meFollow; }

    // ==========================================
    // 6. INNER CLASS (Preference)
    // ==========================================
    public static class Preference {
        private List<String> allergy;
        private List<String> illness;
        private List<String> diet;

        public List<String> getAllergy() { return allergy; }
        public List<String> getIllness() { return illness; }
        public List<String> getDiet() { return diet; }

        public void setAllergy(List<String> allergy) { this.allergy = allergy; }
        public void setIllness(List<String> illness) { this.illness = illness; }
        public void setDiet(List<String> diet) { this.diet = diet; }
    }
}