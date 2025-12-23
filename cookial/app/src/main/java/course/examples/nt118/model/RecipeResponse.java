package course.examples.nt118.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class RecipeResponse {
    @SerializedName("success")
    public boolean success;

    @SerializedName("message")
    public String message;

    @SerializedName("recipe")
    public Recipe recipe;

    public Recipe getRecipe() {
        return recipe;
    }

    public static class Recipe implements Serializable {
        @SerializedName("_id")
        private String _id;

        @SerializedName("userID")
        private String userID;

        @SerializedName("name")
        private String name;

        @SerializedName("description")
        private String description;

        @SerializedName("ration")
        private int ration;

        @SerializedName("time")
        private String time;

        @SerializedName("caption")
        private String caption;

        // --- THÊM TRƯỜNG THUMBNAIL ---
        @SerializedName("thumbnail")
        private String thumbnail;

        @SerializedName("media")
        private List<String> media;

        @SerializedName("ingredients")
        private Ingredients ingre;

        @SerializedName("guide")
        private List<Step> guide;

        @SerializedName("tags")
        private List<String> tags;

        // --- GETTERS ---
        public String get_id() { return _id; }
        public String getUserID() { return userID; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getRation() { return ration; }
        public String getTime() { return time; }
        public String getCaption() { return caption; }

        // Getter cho thumbnail
        public String getThumbnail() { return thumbnail; }

        public List<String> getMedia() { return media; }
        public Ingredients getIngre() { return ingre; }
        public List<Step> getGuide() { return guide; }
        public List<String> getTags() { return tags; }
    }

    // ... (Giữ nguyên các class Ingredients, Ingredient, Step bên dưới) ...
    public static class Ingredients implements Serializable {
        @SerializedName("base")
        private List<Ingredient> base;
        @SerializedName("comple")
        private List<Ingredient> comple;
        @SerializedName("spice")
        private List<Ingredient> spice;
        @SerializedName("other")
        private List<Ingredient> other;

        public List<Ingredient> getBase() { return base; }
        public List<Ingredient> getComple() { return comple; }
        public List<Ingredient> getSpice() { return spice; }
        public List<Ingredient> getOther() { return other; }
    }

    public static class Ingredient implements Serializable {
        @SerializedName("quantity")
        private String quantity;
        @SerializedName("name")
        private String name;

        public String getQuantity() { return quantity; }
        public String getName() { return name; }
    }

    public static class Step implements Serializable {
        @SerializedName("step")
        private int step;
        @SerializedName("content")
        private String content;
        @SerializedName("media")
        private List<String> media;

        public int getStep() { return step; }
        public String getContent() { return content; }
        public List<String> getMedia() { return media; }
    }
}