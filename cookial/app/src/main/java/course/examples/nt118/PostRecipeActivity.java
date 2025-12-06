package course.examples.nt118;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import course.examples.nt118.databinding.ActivityRecipePostDetailBinding;
import course.examples.nt118.model.RecipeResponse; // Import đúng model này
import course.examples.nt118.model.UserResponse;
import course.examples.nt118.network.ApiService;
import course.examples.nt118.network.RetrofitClient;
import course.examples.nt118.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostRecipeActivity extends AppCompatActivity {

    private ActivityRecipePostDetailBinding binding;
    private static final String TAG = PostRecipeActivity.class.getSimpleName();
    private String currentPostId;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecipePostDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo API Service
        apiService = RetrofitClient.getInstance(this).getApiService();
        currentPostId = getIntent().getStringExtra("POST_ID");

        if (currentPostId != null) {
            fetchRecipeDetails(currentPostId);
        } else {
            Toast.makeText(this, "Error: Post ID not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        setupListeners();
    }

    private void setupListeners() {
        // 1. Back Button
        binding.btnBack.setOnClickListener(v -> finish());

        // 2. Cook Now Button -> Go to Cooking Mode
        binding.btnCookNow.setOnClickListener(v -> {
            if (currentPostId != null) {
                Intent intent = new Intent(PostRecipeActivity.this, CookingModeActivity.class);
                intent.putExtra("postID", currentPostId);
                startActivity(intent);
            }
        });

        // 3. Customize Button (Placeholder)
        binding.btnCustomize.setOnClickListener(v ->
                Toast.makeText(this, "Customize feature coming soon!", Toast.LENGTH_SHORT).show()
        );

        // 4. Menu More (Placeholder)
        binding.btnMenuMore.setOnClickListener(v ->
                Toast.makeText(this, "More options...", Toast.LENGTH_SHORT).show()
        );
    }

    private void fetchRecipeDetails(String postId) {
        apiService.getRecipeByPostID(postId).enqueue(new Callback<RecipeResponse>() {
            @Override
            public void onResponse(Call<RecipeResponse> call, Response<RecipeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {

                    // [ĐÃ SỬA] Dùng RecipeResponse.Recipe
                    RecipeResponse.Recipe recipe = response.body().getRecipe();

                    if (recipe != null) {
                        displayRecipeDetails(recipe);
                        fetchAuthorInfo(recipe.getUserID());
                    }
                } else {
                    Toast.makeText(PostRecipeActivity.this, "Failed to load recipe details", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Response Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<RecipeResponse> call, Throwable t) {
                Toast.makeText(PostRecipeActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Network Error", t);
            }
        });
    }

    private void fetchAuthorInfo(String userId) {
        if (userId == null) return;
        String myId = TokenManager.getUserId(this);

        apiService.getUserById(userId, myId).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse user = response.body().getRealUser();
                    if (user != null) {
                        binding.txtUsernameRecipeInfo.setText(user.getName());
                        binding.txtUserId.setText("@" + user.getName());

                        // Load Avatar
                        Glide.with(PostRecipeActivity.this)
                                .load(user.getAvatar())
                                .placeholder(android.R.drawable.sym_def_app_icon)
                                .circleCrop()
                                .into(binding.imgAvatarRecipe);
                    }
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                // Ignore or log
            }
        });
    }

    // [ĐÃ SỬA] Tham số là RecipeResponse.Recipe
    private void displayRecipeDetails(RecipeResponse.Recipe recipe) {
        // 1. Basic Info
        binding.txtRecipeTitle.setText(recipe.getName());

        // Load Thumbnail Image
        if (recipe.getMedia() != null && !recipe.getMedia().isEmpty()) {
            Glide.with(this)
                    .load(recipe.getMedia().get(0)) // Load first image
                    .centerCrop()
                    .into(binding.imgRecipeThumbnail);
        }

        // Ration & Time
        String rationTime = "Khẩu phần: " + recipe.getRation() + " người | Thời gian: " + recipe.getTime();
        binding.txtRationTime.setText(rationTime);

        // 2. Ingredients (Dynamic List)
        displayIngredients(recipe);

        // 3. Guide Steps (Dynamic List)
        displayGuideSteps(recipe);
    }

    // [ĐÃ SỬA] Tham số là RecipeResponse.Recipe
    private void displayIngredients(RecipeResponse.Recipe recipe) {
        LinearLayout ingredientsLayout = binding.layoutIngredientsList;
        ingredientsLayout.removeAllViews();

        // [ĐÃ SỬA] Dùng RecipeResponse.Ingredients
        RecipeResponse.Ingredients ingredients = recipe.getIngre();
        if (ingredients == null) return;

        // [ĐÃ SỬA] List<List<RecipeResponse.Ingredient>>
        List<List<RecipeResponse.Ingredient>> groups = new ArrayList<>();

        if (ingredients.getBase() != null) groups.add(ingredients.getBase());
        if (ingredients.getComple() != null) groups.add(ingredients.getComple());
        if (ingredients.getSpice() != null) groups.add(ingredients.getSpice());
        if (ingredients.getOther() != null) groups.add(ingredients.getOther());

        Context context = this;
        // [ĐÃ SỬA] Vòng lặp
        for (List<RecipeResponse.Ingredient> groupList : groups) {
            for (RecipeResponse.Ingredient ingredient : groupList) {
                TextView ingredientView = new TextView(context);

                String text = "• " + ingredient.getQuantity() + " " + ingredient.getName();
                ingredientView.setText(text);
                ingredientView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                ingredientView.setTextColor(getResources().getColor(android.R.color.black));
                ingredientView.setPadding(0, 8, 0, 8);

                ingredientsLayout.addView(ingredientView);
            }
        }
    }

    // [ĐÃ SỬA] Tham số là RecipeResponse.Recipe
    private void displayGuideSteps(RecipeResponse.Recipe recipe) {
        LinearLayout guideLayout = binding.layoutGuideSteps;
        guideLayout.removeAllViews();

        // [ĐÃ SỬA] List<RecipeResponse.Step>
        List<RecipeResponse.Step> steps = recipe.getGuide();
        if (steps == null || steps.isEmpty()) return;

        Context context = this;
        // [ĐÃ SỬA] Vòng lặp
        for (RecipeResponse.Step step : steps) {
            LinearLayout stepContainer = new LinearLayout(context);
            stepContainer.setOrientation(LinearLayout.VERTICAL);
            stepContainer.setPadding(0, 0, 0, 24);

            // Step Title
            TextView stepTitle = new TextView(context);
            stepTitle.setText("Bước " + step.getStep());
            stepTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            stepTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            stepContainer.addView(stepTitle);

            // Step Content
            TextView stepContent = new TextView(context);
            stepContent.setText(step.getContent());
            stepContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            stepContent.setTextColor(getResources().getColor(android.R.color.darker_gray));
            stepContainer.addView(stepContent);

            guideLayout.addView(stepContainer);
        }
    }

    @Override protected void onStart() { super.onStart(); Log.d(TAG, "2. onStart"); }
    @Override protected void onResume() { super.onResume(); Log.d(TAG, "3. onResume"); }
    @Override protected void onPause() { super.onPause(); Log.d(TAG, "4. onPause"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "5. onStop"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "7. onDestroy"); }
}