package course.examples.nt118.model;

import java.util.List;
import java.util.Map;

public class RecipeResponse {
    public boolean success;
    public Recipe recipe;

    public static class Recipe {
        public String _id;
        public String userID;
        public String postID;

        public String name;
        public String description;
        public int ration;
        public String time;
        public String thumbnail;
        public String caption;

        // ✅ ingredients (4 nhóm)
        public IngredientGroup ingredients;

        // ✅ guide (list step)
        public List<Step> guide;

        // ✅ tags
        public List<String> tags;
    }

    // ✅ ingredient group: base / comple / spice / other
    public static class IngredientGroup {
        public List<Ingredient> base;
        public List<Ingredient> comple;
        public List<Ingredient> spice;
        public List<Ingredient> other;
    }

    public static class Ingredient {
        public String quantity;
        public String name;
    }

    // ✅ step (bước nấu ăn)
    public static class Step {
        public int step;
        public String content;
        public List<String> media;
    }
}
