package course.examples.nt118;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import course.examples.nt118.databinding.ActivityRecipePostDetailBinding;
import course.examples.nt118.model.Recipe;

import java.util.ArrayList;
import java.util.List;

public class PostRecipeActivity extends AppCompatActivity {

    private ActivityRecipePostDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecipePostDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final String postId = getIntent().getStringExtra("POST_ID");

        if (postId != null) {
            Recipe mockRecipe = createMockRecipe();
            displayRecipeDetails(mockRecipe);
        } else {
            Recipe mockRecipe = createMockRecipe();
            displayRecipeDetails(mockRecipe);
        }

        setupListeners();
    }

    private void setupListeners() {
        // Truy cập nút Back qua findViewById trên Header Bar
        binding.headerBar.findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    /**
     * Tạo dữ liệu mẫu TẠM THỜI để kiểm tra UI.
     * ĐÃ FIX: Hoàn thiện dữ liệu Ingredients để hiển thị đúng.
     */
    private Recipe createMockRecipe() {
        // --- TẠO CÁC NGUYÊN LIỆU MẪU ---
        Recipe.Ingredient ingre_base = new Recipe.Ingredient();
        ingre_base.setQuantity("4 chén");
        ingre_base.setName("White Rice");

        Recipe.Ingredient ingre_comple = new Recipe.Ingredient();
        ingre_comple.setQuantity("8 quả");
        ingre_comple.setName("Chicken Egg");

        Recipe.Ingredient ingre_spice = new Recipe.Ingredient();
        ingre_spice.setQuantity("2 muỗng");
        ingre_spice.setName("Fish Sauce");

        // --- TẠO RECIPE.INGREDIENTS (Container) ---
        Recipe.Ingredients ingredients = new Recipe.Ingredients();

        // GÁN DỮ LIỆU VÀO CÁC NHÓM (CẦN THIẾT ĐỂ LOGIC HIỂN THỊ ĐÚNG)
        List<Recipe.Ingredient> baseList = new ArrayList<>();
        baseList.add(ingre_base);
        ingredients.setBase(baseList);

        List<Recipe.Ingredient> compleList = new ArrayList<>();
        compleList.add(ingre_comple);
        ingredients.setComple(compleList);

        List<Recipe.Ingredient> spiceList = new ArrayList<>();
        spiceList.add(ingre_spice);
        ingredients.setSpice(spiceList);

        ingredients.setOther(new ArrayList<>());

        // --- TẠO RECIPE CHÍNH ---
        Recipe recipe = new Recipe();
        recipe.setName("Cách làm món thịt kho tàu cực đơn giản");
        recipe.setUserID("Vinh Nguyen");
        recipe.setRation(3);
        recipe.setTime("15 phút");
        recipe.setIngre(ingredients); // SỬ DỤNG SETTER MỚI: setIngre()

        return recipe;
    }

    /**
     * Hiển thị chi tiết công thức lên giao diện
     */
    private void displayRecipeDetails(Recipe recipe) {
        // 1. Thumbnail, Title, User Info
        binding.txtRecipeTitle.setText(recipe.getName());
        binding.txtUsernameRecipeInfo.setText(recipe.getUserID());

        // 2. Nguyên liệu và Hướng dẫn
        // Sử dụng Resource String cho Khẩu phần/Thời gian
        binding.txtRationTime.setText(getString(R.string.recipe_ration_time_format,
                recipe.getRation(),
                recipe.getTime()));

        displayIngredients(recipe);
    }

    /**
     * Hàm xử lý Nguyên liệu (Tạo TextView động)
     */
    private void displayIngredients(Recipe recipe) {
        Context context = this;
        LinearLayout ingredientsLayout = binding.layoutIngredientsList;
        ingredientsLayout.removeAllViews();

        // SỬ DỤNG GETTER MỚI: getIngre()
        Recipe.Ingredients ingredients = recipe.getIngre();

        if (ingredients == null) return;

        // Sắp xếp các nhóm nguyên liệu theo thứ tự hiển thị
        List<List<Recipe.Ingredient>> groups = new ArrayList<>();
        groups.add(ingredients.getBase());
        groups.add(ingredients.getComple());
        groups.add(ingredients.getSpice());
        groups.add(ingredients.getOther());

        for (List<Recipe.Ingredient> groupList : groups) {
            if (groupList != null) {
                for (Recipe.Ingredient ingredient : groupList) {
                    TextView ingredientView = new TextView(context);

                    String ingredientText = ingredient.getQuantity() + " " + ingredient.getName();

                    ingredientView.setText(ingredientText);
                    ingredientView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 0, 0, (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 4, context.getResources().getDisplayMetrics()));
                    ingredientView.setLayoutParams(params);

                    ingredientsLayout.addView(ingredientView);
                }
            }
        }
    }
}