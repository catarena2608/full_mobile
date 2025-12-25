package course.examples.nt118;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import course.examples.nt118.adapter.CommentAdapter;
import course.examples.nt118.databinding.ActivityRecipePostDetailBinding;
import course.examples.nt118.model.Comment;
import course.examples.nt118.model.CommentListResponse;
import course.examples.nt118.model.CommentResponse;
import course.examples.nt118.model.RecipeResponse;
import course.examples.nt118.model.UserResponse;
import course.examples.nt118.network.ApiService;
import course.examples.nt118.network.RetrofitClient;
import course.examples.nt118.utils.TokenManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostRecipeActivity extends AppCompatActivity {

    private static final String TAG = "PostRecipeActivity";
    private ActivityRecipePostDetailBinding binding;
    private ApiService apiService;

    // Core Data
    private String currentPostId;
    private String currentUserId;
    private String authorId; // Lưu lại để dùng cho chức năng xem profile

    // Comment System Variables
    private CommentAdapter commentAdapter;
    private String nextCursor = null;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    // Reply State
    private String replyToCommentId = null;
    @SuppressWarnings("unused")
    private String replyToUserName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecipePostDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d(TAG, "onCreate: Activity Created");

        currentUserId = TokenManager.getUserId(this);
        apiService = RetrofitClient.getInstance(this).getApiService();
        currentPostId = getIntent().getStringExtra("POST_ID");

        if (currentPostId == null || currentPostId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy bài viết", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initRecipeData();
        setupListeners();

        // [COMMENT] Setup RecyclerView & Load Comments
        setupCommentRecyclerView();
        fetchComments();
    }

    private void initRecipeData() {
        loadRecipeDetails(currentPostId);
    }

    private void setupListeners() {
        // 1. Nút Back
        binding.btnBack.setOnClickListener(v -> finish());

        // 2. Nút Nấu Ngay
        binding.btnCookNow.setOnClickListener(v -> {
            if (currentPostId != null) {
                Intent intent = new Intent(PostRecipeActivity.this, CookingModeActivity.class);
                intent.putExtra("postID", currentPostId);
                startActivity(intent);
            }
        });

        // 3. Menu More (Giữ nguyên logic cũ hoặc mở rộng sau)
        binding.btnMenuMore.setOnClickListener(v ->
                Toast.makeText(this, "Tùy chọn khác", Toast.LENGTH_SHORT).show()
        );

        // [COMMENT] 4. Các sự kiện cho comment input
        if (binding.btnSendComment != null)
            binding.btnSendComment.setOnClickListener(v -> handleSendComment());

        if (binding.btnCloseReply != null)
            binding.btnCloseReply.setOnClickListener(v -> cancelReplyMode());

        // [COMMENT] 5. Sự kiện click vào Avatar/Tên người đăng bài (sau khi load xong authorId)
        View.OnClickListener authorProfileClick = v -> openUserProfile(authorId);
        binding.imgAvatarRecipe.setOnClickListener(authorProfileClick);
        binding.txtUsernameRecipeInfo.setOnClickListener(authorProfileClick);
    }

    // ==================================================================
    // 1. LOGIC COMMENT (GIỐNG HỆT POST DETAIL)
    // ==================================================================

    private void setupCommentRecyclerView() {
        commentAdapter = new CommentAdapter(this, currentUserId, new CommentAdapter.CommentInteractionListener() {
            @Override
            public void onReplyClick(Comment comment) {
                activateReplyMode(comment);
            }

            @Override
            public void onUserClick(String userId) {
                openUserProfile(userId);
            }

            @Override
            public void onReportClick(Comment comment) {
                showReportCommentDialog(comment);
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.rvComments.setLayoutManager(layoutManager);
        binding.rvComments.setAdapter(commentAdapter);

        // Load More Logic
        binding.rvComments.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && !isLoading && !isLastPage) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                    if ((visibleItemCount + firstVisibleItem) >= totalItemCount - 2) {
                        fetchComments();
                    }
                }
            }
        });
    }

    private void fetchComments() {
        if (isLoading) return;
        isLoading = true;

        apiService.getComments(currentPostId, nextCursor).enqueue(new Callback<CommentListResponse>() {
            @Override
            public void onResponse(@NonNull Call<CommentListResponse> call, @NonNull Response<CommentListResponse> response) {
                isLoading = false;
                if (response.isSuccessful() && response.body() != null) {
                    List<CommentResponse> rawComments = response.body().getComments();

                    if (rawComments != null && !rawComments.isEmpty()) {
                        List<Comment> mappedComments = mapToFlattenList(rawComments);

                        if (nextCursor == null) {
                            commentAdapter.submitList(mappedComments);
                        } else {
                            commentAdapter.addData(mappedComments);
                        }

                        nextCursor = response.body().getNextCursor();
                        if (nextCursor == null || nextCursor.isEmpty()) isLastPage = true;
                    } else {
                        isLastPage = true;
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<CommentListResponse> call, @NonNull Throwable t) {
                isLoading = false;
                Log.e(TAG, "Lỗi tải bình luận", t);
            }
        });
    }

    // --- SEND COMMENT ---
    private void handleSendComment() {
        String content = binding.etCommentInput.getText().toString().trim();
        if (TextUtils.isEmpty(content)) return;

        binding.etCommentInput.setText("");
        hideKeyboard();

        Map<String, Object> body = new HashMap<>();
        body.put("postID", currentPostId);
        body.put("content", content);
        if (replyToCommentId != null) {
            body.put("reply", replyToCommentId);
        }

        apiService.addComment(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PostRecipeActivity.this, "Đã gửi bình luận", Toast.LENGTH_SHORT).show();
                    cancelReplyMode();
                    nextCursor = null;
                    isLastPage = false;
                    fetchComments();
                } else {
                    Toast.makeText(PostRecipeActivity.this, "Gửi thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Toast.makeText(PostRecipeActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- REPORT COMMENT ---
    private void showReportCommentDialog(Comment comment) {
        String[] reasons = {
                "Nội dung spam",
                "Ngôn từ thù ghét/xúc phạm",
                "Thông tin sai lệch",
                "Quấy rối người khác",
                "Nội dung khiêu dâm"
        };

        new AlertDialog.Builder(this)
                .setTitle("Báo cáo bình luận")
                .setSingleChoiceItems(reasons, -1, (dialog, which) -> {
                    String selectedReason = reasons[which];
                    sendReportCommentToApi(comment.getId(), selectedReason);
                    dialog.dismiss();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void sendReportCommentToApi(String target, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("target", target);
        body.put("reason", reason);

        apiService.reportComment(body).enqueue(new Callback<CommentResponse>() {
            @Override
            public void onResponse(@NonNull Call<CommentResponse> call, @NonNull Response<CommentResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PostRecipeActivity.this, "Đã gửi báo cáo bình luận.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PostRecipeActivity.this, "Gửi báo cáo thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CommentResponse> call, @NonNull Throwable t) {
                Toast.makeText(PostRecipeActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- HELPER COMMENT FUNCTIONS ---
    private List<Comment> mapToFlattenList(List<CommentResponse> responses) {
        List<Comment> result = new ArrayList<>();
        for (CommentResponse parent : responses) {
            result.add(convertSingleResponse(parent));
            if (parent.getReplies() != null && !parent.getReplies().isEmpty()) {
                for (CommentResponse child : parent.getReplies()) {
                    result.add(convertSingleResponse(child));
                    if (child.getReplies() != null && !child.getReplies().isEmpty()) {
                        for (CommentResponse grandChild : child.getReplies()) {
                            result.add(convertSingleResponse(grandChild));
                        }
                    }
                }
            }
        }
        return result;
    }

    private Comment convertSingleResponse(CommentResponse res) {
        Comment c = new Comment();
        c.setId(res.getId());
        c.setPostId(res.getPostId());
        c.setUserId(res.getUserId());
        c.setContent(res.getContent());
        c.setCreatedAt(res.getCreatedAt());
        c.setDepth(res.getDepth());
        c.setUserName(res.getUserName());
        c.setUserAvatar(res.getUserAvatar());
        return c;
    }

    private void activateReplyMode(Comment comment) {
        replyToCommentId = comment.getId();
        replyToUserName = comment.getUserName() != null ? comment.getUserName() : "người dùng";
        if (binding.layoutReplying != null) {
            binding.layoutReplying.setVisibility(View.VISIBLE);
            binding.tvReplyingTo.setText("Đang trả lời: " + replyToUserName);
        }
        binding.etCommentInput.requestFocus();
        showKeyboard();
    }

    private void cancelReplyMode() {
        replyToCommentId = null;
        replyToUserName = null;
        if (binding.layoutReplying != null) {
            binding.layoutReplying.setVisibility(View.GONE);
        }
        hideKeyboard();
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(binding.etCommentInput, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void openUserProfile(String targetUserId) {
        if (TextUtils.isEmpty(targetUserId)) return;
        Intent intent;
        if (targetUserId.equals(currentUserId)) {
            intent = new Intent(this, ProfileActivity.class);
        } else {
            intent = new Intent(this, OtherUserProfileActivity.class);
            intent.putExtra("USER_ID", targetUserId);
        }
        startActivity(intent);
    }

    // ==================================================================
    // 2. RECIPE & AUTHOR LOADING LOGIC (GIỮ NGUYÊN)
    // ==================================================================

    private void loadRecipeDetails(String postId) {
        apiService.getRecipeByPostID(postId).enqueue(new Callback<RecipeResponse>() {
            @Override
            public void onResponse(@NonNull Call<RecipeResponse> call, @NonNull Response<RecipeResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    RecipeResponse.Recipe recipe = response.body().getRecipe();
                    if (recipe != null) {
                        displayRecipeInfo(recipe);
                        // [QUAN TRỌNG] Lưu lại authorId để dùng cho chức năng click avatar
                        authorId = recipe.getUserID();
                        loadAuthorInfo(authorId);
                    }
                } else {
                    Log.e(TAG, "Load Recipe Failed: " + response.code());
                    Toast.makeText(PostRecipeActivity.this, "Không tải được nội dung bài viết", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RecipeResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Network Error", t);
                Toast.makeText(PostRecipeActivity.this, "Lỗi kết nối mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAuthorInfo(String userId) {
        if (userId == null) return;

        apiService.getUserById(userId, currentUserId).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse user = response.body().getRealUser();
                    if (user != null) {
                        binding.txtUsernameRecipeInfo.setText(user.getName());
                        binding.txtUserId.setText("@" + user.getName());

                        Glide.with(PostRecipeActivity.this)
                                .load(user.getAvatar())
                                .placeholder(android.R.drawable.sym_def_app_icon)
                                .circleCrop()
                                .into(binding.imgAvatarRecipe);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Load Author Failed", t);
            }
        });
    }

    private void displayRecipeInfo(RecipeResponse.Recipe recipe) {
        // 1. Tiêu đề
        binding.txtRecipeTitle.setText(recipe.getName());

        // 2. Hình ảnh Thumbnail
        String imgUrl = recipe.getThumbnail();
        if ((imgUrl == null || imgUrl.isEmpty()) && recipe.getMedia() != null && !recipe.getMedia().isEmpty()) {
            imgUrl = recipe.getMedia().get(0);
        }

        Glide.with(this)
                .load(imgUrl)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.imgRecipeThumbnail);

        // 3. Thông tin chung
        String info = String.format("Khẩu phần: %s người | Thời gian: %s",
                recipe.getRation(),
                recipe.getTime());
        binding.txtRationTime.setText(info);

        // 4. Render danh sách
        renderIngredients(recipe.getIngre());
        renderGuideSteps(recipe.getGuide());
    }

    private void renderIngredients(RecipeResponse.Ingredients ingredients) {
        binding.layoutIngredientsList.removeAllViews();

        if (ingredients == null) return;

        List<RecipeResponse.Ingredient> allIngredients = new ArrayList<>();
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
            String text = "• " + item.getQuantity() + " " + item.getName();
            tv.setText(text);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tv.setTextColor(getColor(android.R.color.black));
            tv.setPadding(0, 8, 0, 8);
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
            stepLayout.setPadding(0, 0, 0, 32);

            TextView titleTv = new TextView(this);
            titleTv.setText("Bước " + step.getStep());
            titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            titleTv.setTypeface(null, Typeface.BOLD);
            titleTv.setTextColor(getColor(android.R.color.black));
            stepLayout.addView(titleTv);

            TextView contentTv = new TextView(this);
            contentTv.setText(step.getContent());
            contentTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            contentTv.setTextColor(getColor(android.R.color.darker_gray));
            contentTv.setPadding(0, 8, 0, 0);
            stepLayout.addView(contentTv);

            binding.layoutGuideSteps.addView(stepLayout);
        }
    }

    // Lifecycle methods
    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override protected void onResume() { super.onResume(); Log.d(TAG, "onResume"); }
    @Override protected void onPause() { super.onPause(); Log.d(TAG, "onPause"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "onStop"); }
    @Override protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (commentAdapter != null) CommentAdapter.clearCache();
    }
}