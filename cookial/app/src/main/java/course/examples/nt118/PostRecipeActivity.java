package course.examples.nt118;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import course.examples.nt118.databinding.ActivityRecipePostDetailBinding; // Đảm bảo tên file XML là activity_recipe_post_detail.xml
import course.examples.nt118.model.RecipeResponse;
import course.examples.nt118.model.UserResponse;
import course.examples.nt118.network.ApiService;
import course.examples.nt118.network.RetrofitClient;
import course.examples.nt118.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostRecipeActivity extends AppCompatActivity {

    private static final String TAG = "PostRecipeActivity";
    private ActivityRecipePostDetailBinding binding;
    private ApiService apiService;
    private String currentPostId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecipePostDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d(TAG, "onCreate: Activity Created");

        initData();
        setupListeners();
    }

    private void initData() {
        apiService = RetrofitClient.getInstance(this).getApiService();

        // Lấy PostID từ Intent truyền sang
        currentPostId = getIntent().getStringExtra("POST_ID");

        if (currentPostId != null && !currentPostId.isEmpty()) {
            loadRecipeDetails(currentPostId);
        } else {
            Toast.makeText(this, "Không tìm thấy bài viết", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupListeners() {
        // 1. Nút Back
        binding.btnBack.setOnClickListener(v -> finish());

        // 2. Nút Nấu Ngay -> Chuyển sang chế độ nấu ăn
        binding.btnCookNow.setOnClickListener(v -> {
            if (currentPostId != null) {
                Intent intent = new Intent(PostRecipeActivity.this, CookingModeActivity.class);
                intent.putExtra("postID", currentPostId);
                startActivity(intent);
            }
        });



        // 4. Menu More
        binding.btnMenuMore.setOnClickListener(v ->
                Toast.makeText(this, "Tùy chọn khác", Toast.LENGTH_SHORT).show()
        );
    }

    // ---------------------------------------------------------
    // API CALLS
    // ---------------------------------------------------------

    private void loadRecipeDetails(String postId) {
        // Gọi API: GET recipe/{postID}
        apiService.getRecipeByPostID(postId).enqueue(new Callback<RecipeResponse>() {
            @Override
            public void onResponse(Call<RecipeResponse> call, Response<RecipeResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    RecipeResponse.Recipe recipe = response.body().getRecipe();
                    if (recipe != null) {
                        displayRecipeInfo(recipe);
                        // Sau khi có recipe thì lấy thông tin người đăng
                        loadAuthorInfo(recipe.getUserID());
                    }
                } else {
                    Log.e(TAG, "Load Recipe Failed: " + response.code());
                    Toast.makeText(PostRecipeActivity.this, "Không tải được nội dung bài viết", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RecipeResponse> call, Throwable t) {
                Log.e(TAG, "Network Error", t);
                Toast.makeText(PostRecipeActivity.this, "Lỗi kết nối mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAuthorInfo(String userId) {
        if (userId == null) return;
        String myId = TokenManager.getUserId(this); // Lấy ID người dùng hiện tại để check follow (nếu cần)

        apiService.getUserById(userId, myId).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse user = response.body().getRealUser();
                    if (user != null) {
                        binding.txtUsernameRecipeInfo.setText(user.getName());
                        binding.txtUserId.setText("@" + user.getName()); // Giả sử model User có field username

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
                Log.e(TAG, "Load Author Failed", t);
            }
        });
    }

    // ---------------------------------------------------------
    // UI DISPLAY LOGIC
    // ---------------------------------------------------------

    private void displayRecipeInfo(RecipeResponse.Recipe recipe) {
        // 1. Tiêu đề
        binding.txtRecipeTitle.setText(recipe.getName());

        // 2. Hình ảnh Thumbnail
        String imgUrl = recipe.getThumbnail();
        // Fallback: Nếu thumbnail rỗng thì lấy ảnh đầu tiên trong media (nếu có)
        if ((imgUrl == null || imgUrl.isEmpty()) && recipe.getMedia() != null && !recipe.getMedia().isEmpty()) {
            imgUrl = recipe.getMedia().get(0);
        }

        Glide.with(this)
                .load(imgUrl)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.imgRecipeThumbnail);

        // 3. Thông tin chung (Khẩu phần & Thời gian)
        String info = String.format("Khẩu phần: %s người | Thời gian: %s",
                recipe.getRation(),
                recipe.getTime());
        binding.txtRationTime.setText(info);

        // 4. Render danh sách nguyên liệu
        renderIngredients(recipe.getIngre());

        // 5. Render các bước hướng dẫn
        renderGuideSteps(recipe.getGuide());
    }

    private void renderIngredients(RecipeResponse.Ingredients ingredients) {
        binding.layoutIngredientsList.removeAllViews();

        if (ingredients == null) return;

        List<RecipeResponse.Ingredient> allIngredients = new ArrayList<>();
        // Gom nhóm nguyên liệu lại (Tùy logic backend trả về null hay empty list)
        if (ingredients.getBase() != null) allIngredients.addAll(ingredients.getBase());
        if (ingredients.getComple() != null) allIngredients.addAll(ingredients.getComple());
        if (ingredients.getSpice() != null) allIngredients.addAll(ingredients.getSpice());
        if (ingredients.getOther() != null) allIngredients.addAll(ingredients.getOther());

        if (allIngredients.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("Không có thông tin nguyên liệu.");
            binding.layoutIngredientsList.addView(emptyText);
            return;
        }

        for (RecipeResponse.Ingredient item : allIngredients) {
            TextView tv = new TextView(this);
            // Format: • 500g Thịt ba chỉ
            String text = "• " + item.getQuantity() + " " + item.getName();
            tv.setText(text);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tv.setTextColor(getColor(android.R.color.black));
            tv.setPadding(0, 8, 0, 8); // Margin top/bottom nhẹ
            binding.layoutIngredientsList.addView(tv);
        }
    }

    private void renderGuideSteps(List<RecipeResponse.Step> steps) {
        binding.layoutGuideSteps.removeAllViews();

        if (steps == null || steps.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("Chưa có hướng dẫn chi tiết.");
            binding.layoutGuideSteps.addView(emptyText);
            return;
        }

        for (RecipeResponse.Step step : steps) {
            LinearLayout stepLayout = new LinearLayout(this);
            stepLayout.setOrientation(LinearLayout.VERTICAL);
            stepLayout.setPadding(0, 0, 0, 32); // Khoảng cách giữa các bước

            // Tiêu đề bước: "Bước 1"
            TextView titleTv = new TextView(this);
            titleTv.setText("Bước " + step.getStep());
            titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            titleTv.setTypeface(null, Typeface.BOLD);
            titleTv.setTextColor(getColor(android.R.color.black));
            stepLayout.addView(titleTv);

            // Nội dung bước
            TextView contentTv = new TextView(this);
            contentTv.setText(step.getContent());
            contentTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            contentTv.setTextColor(getColor(android.R.color.darker_gray));
            contentTv.setPadding(0, 8, 0, 0);
            stepLayout.addView(contentTv);

            binding.layoutGuideSteps.addView(stepLayout);
        }
    }

    // ---------------------------------------------------------
    // LIFECYCLE LOGGING
    // ---------------------------------------------------------
    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override protected void onResume() { super.onResume(); Log.d(TAG, "onResume"); }
    @Override protected void onPause() { super.onPause(); Log.d(TAG, "onPause"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "onStop"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy"); }
}