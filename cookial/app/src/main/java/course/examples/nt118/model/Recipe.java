package course.examples.nt118.model;

import java.io.Serializable;
import java.util.List;

public class Recipe implements Serializable {
    private String _id;
    private String userID;
    private String postID;
    private String thumbnail;
    private String caption;
    private String name;
    private String description;
    private int ration;
    private String time;

    // ĐỔI TÊN THUỘC TÍNH ĐỂ KHỚP VỚI JSON BACKEND
    private Ingredients ingre;

    private List<Step> guide;
    private List<String> tags;

    // Constructors (Nếu cần)
    public Recipe() {}

    // --- INNER CLASSES ---

    public static class Ingredients implements Serializable {
        private List<Ingredient> base;
        private List<Ingredient> comple;
        private List<Ingredient> spice;
        private List<Ingredient> other;

        public List<Ingredient> getBase() { return base; }
        public List<Ingredient> getComple() { return comple; }
        public List<Ingredient> getSpice() { return spice; }
        public List<Ingredient> getOther() { return other; }

        public void setBase(List<Ingredient> base) { this.base = base; }
        public void setComple(List<Ingredient> comple) { this.comple = comple; }
        public void setSpice(List<Ingredient> spice) { this.spice = spice; }
        public void setOther(List<Ingredient> other) { this.other = other; }
    }

    public static class Ingredient implements Serializable {
        private String quantity;
        private String name;

        public String getQuantity() { return quantity; }
        public String getName() { return name; }

        public void setQuantity(String quantity) { this.quantity = quantity; }
        public void setName(String name) { this.name = name; }
    }

    public static class Step implements Serializable {
        private int num;
        private String content;
        private List<String> media;

        public int getNum() { return num; }
        public String getContent() { return content; }
        public List<String> getMedia() { return media; }

        public void setNum(int num) { this.num = num; }
        public void setContent(String content) { this.content = content; }
        public void setMedia(List<String> media) { this.media = media; }
    }

    // --- GETTERS & SETTERS CHO CÁC TRƯỜNG CHÍNH ---

    // GETTERS
    public String get_id() { return _id; }
    public String getUserID() { return userID; }
    public String getPostID() { return postID; }
    public String getThumbnail() { return thumbnail; }
    public String getCaption() { return caption; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getRation() { return ration; }
    public String getTime() { return time; }
    public List<Step> getGuide() { return guide; }
    public List<String> getTags() { return tags; }

    // GETTER/SETTER CHO TÊN THUỘC TÍNH MỚI
    public Ingredients getIngre() { return ingre; }
    public void setIngre(Ingredients ingre) { this.ingre = ingre; }


    // SETTERS CHO CÁC TRƯỜNG KHÁC
    public void set_id(String _id) { this._id = _id; }
    public void setUserID(String userID) { this.userID = userID; }
    public void setPostID(String postID) { this.postID = postID; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
    public void setCaption(String caption) { this.caption = caption; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setRation(int ration) { this.ration = ration; }
    public void setTime(String time) { this.time = time; }
    public void setGuide(List<Step> guide) { this.guide = guide; }
    public void setTags(List<String> tags) { this.tags = tags; }

    // ... (Phương thức equals và hashCode giữ nguyên) ...
}