package course.examples.nt118;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import course.examples.nt118.adapter.SavedPostAdapter;
import course.examples.nt118.databinding.ActivitySavedPostBinding;
import course.examples.nt118.model.Post;
import course.examples.nt118.model.PostsResponse;
import course.examples.nt118.model.UserResponse; // Model từ API
import course.examples.nt118.network.ApiService;
import course.examples.nt118.network.RetrofitClient;
import course.examples.nt118.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SavedPostsActivity extends AppCompatActivity {

    private static final String TAG = SavedPostsActivity.class.getSimpleName();
    private ActivitySavedPostBinding binding;
    private SavedPostAdapter adapter;

    // Hai danh sách để lưu trữ sau khi lọc
    private final List<Post> listRegular = new ArrayList<>();
    private final List<Post> listRecipes = new ArrayList<>();

    // Biến quản lý tải bất đồng bộ (Dùng UserResponse để cache)
    private final Map<String, UserResponse> userCache = new HashMap<>();
    private int postsToProcess = 0;
    private int postsProcessed = 0;

    private String userId;
    private boolean isShowingRecipe = false; // Mặc định hiện Regular posts

    // ==================================================================
    // 1. LIFECYCLE & SETUP
    // ==================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "1. onCreate");
        binding = ActivitySavedPostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userId = TokenManager.getUserId(this);
        if (userId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setupUI();
        setupBottomNavigation();
        fetchSavedPosts();
    }

    @Override
    protected void onStart() { super.onStart(); Log.d(TAG, "2. onStart"); }

    @Override
    protected void onResume() { super.onResume(); Log.d(TAG, "3. onResume"); }

    @Override
    protected void onPause() { super.onPause(); Log.d(TAG, "4. onPause"); }

    @Override
    protected void onStop() { super.onStop(); Log.d(TAG, "5. onStop"); }

    @Override
    protected void onRestart() { super.onRestart(); Log.d(TAG, "6. onRestart"); }

    @Override
    protected void onDestroy() { super.onDestroy(); Log.d(TAG, "7. onDestroy"); }

    private void setupUI() {
        adapter = new SavedPostAdapter(this, this::openPostDetail);
        binding.rvSavedPosts.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvSavedPosts.setAdapter(adapter);

        // Tab Clicks
        binding.tabPosts.setOnClickListener(v -> switchTab(false));
        binding.tabRecipes.setOnClickListener(v -> switchTab(true));
    }

    // ==================================================================
    // 2. DATA LOADING & ASYNC USER FETCH
    // ==================================================================

    private void fetchSavedPosts() {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        binding.progressBar.setVisibility(View.VISIBLE);

        api.getSavedPosts().enqueue(new Callback<PostsResponse>() {
            @Override
            public void onResponse(Call<PostsResponse> call, Response<PostsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Post> rawList = response.body().getPosts();

                    if (rawList != null && !rawList.isEmpty()) {
                        postsToProcess = rawList.size();
                        postsProcessed = 0;
                        listRegular.clear();
                        listRecipes.clear();

                        for (Post r : rawList) {
                            Post p = mapToPostModel(r);

                            if ("Recipe".equalsIgnoreCase(p.getType())) {
                                listRecipes.add(p);
                            } else {
                                listRegular.add(p);
                            }

                            // KÍCH HOẠT TẢI USER INFO (Bất đồng bộ)
                            fetchUserInfoAndRefresh(p);
                        }
                    } else {
                        onLoadingFinished(false);
                    }
                } else {
                    onLoadingFinished(false);
                    Toast.makeText(SavedPostsActivity.this, "Lỗi tải trang: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PostsResponse> call, Throwable t) {
                onLoadingFinished(false);
                Log.e(TAG, "Error", t);
                Toast.makeText(SavedPostsActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUserInfoAndRefresh(Post post) {
        String authorId = post.getUserID();

        // 1. Kiểm tra Cache
        if (userCache.containsKey(authorId)) {
            UserResponse cachedUser = userCache.get(authorId);
            updatePostUserInfo(post, cachedUser);
            onPostProcessed();
            return;
        }

        // 2. Gọi API sử dụng getUserById(authorId, userId)
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.getUserById(authorId, userId).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Dùng getRealUser() để xử lý cấu trúc lồng
                    UserResponse user = response.body().getRealUser();

                    if (user != null && user.getName() != null) {
                        userCache.put(authorId, user);
                        updatePostUserInfo(post, user);
                    } else {
                        post.setUserName("User " + authorId);
                        post.setUserAvatar(null);
                    }
                } else {
                    post.setUserName("User " + authorId);
                    post.setUserAvatar(null);
                }
                onPostProcessed();
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                post.setUserName("User " + authorId);
                onPostProcessed();
            }
        });
    }

    private void onPostProcessed() {
        postsProcessed++;
        if (postsProcessed >= postsToProcess) {
            // Khi tất cả bài viết đã được xử lý xong User Info
            filterAndDisplayPosts();
        }
    }

    // ==================================================================
    // 3. UI MAPPING & LOGIC
    // ==================================================================

    private void updatePostUserInfo(Post post, UserResponse user) {
        post.setUserName(user.getName());
        post.setUserAvatar(user.getAvatar());
        // Cập nhật trạng thái Follow (quan trọng cho nút Follow trong Post Detail)
        post.setFollowed(user.isMeFollow());
    }

    private void filterAndDisplayPosts() {
        // Cập nhật giao diện thông báo nếu rỗng
        boolean hasContent = !listRegular.isEmpty() || !listRecipes.isEmpty();
        onLoadingFinished(hasContent);

        // Load tab mặc định
        switchTab(isShowingRecipe);
    }

    private void onLoadingFinished(boolean hasContent) {
        binding.progressBar.setVisibility(View.GONE);
        binding.tvNoContent.setVisibility(hasContent ? View.GONE : View.VISIBLE);
    }

    private void switchTab(boolean showRecipe) {
        isShowingRecipe = showRecipe;
        int white = ContextCompat.getColor(this, android.R.color.white);
        int grayText = 0xFF757575;

        // Logic thay đổi màu tab
        if (showRecipe) {
            binding.tabRecipes.setBackgroundResource(R.drawable.bg_button_orange);
            binding.tabRecipes.setTextColor(white);
            binding.tabRecipes.setTypeface(null, android.graphics.Typeface.BOLD);

            binding.tabPosts.setBackgroundResource(R.drawable.bg_input_border);
            binding.tabPosts.setTextColor(grayText);
            binding.tabPosts.setTypeface(null, android.graphics.Typeface.NORMAL);

            adapter.setData(listRecipes);
        } else {
            binding.tabPosts.setBackgroundResource(R.drawable.bg_button_orange);
            binding.tabPosts.setTextColor(white);
            binding.tabPosts.setTypeface(null, android.graphics.Typeface.BOLD);

            binding.tabRecipes.setBackgroundResource(R.drawable.bg_input_border);
            binding.tabRecipes.setTextColor(grayText);
            binding.tabRecipes.setTypeface(null, android.graphics.Typeface.NORMAL);

            adapter.setData(listRegular);
        }
    }

    private Post mapToPostModel(Post r) {
        Post p = new Post();
        p.set_id(r.get_id());
        p.setType(r.getType());
        p.setCaption(r.getCaption());
        p.setMedia(r.getMedia());
        p.setLike(r.getLike());
        p.setComment(r.getComment());
        p.setUserID(r.getUserID());

        // GÁN TRẠNG THÁI TƯƠNG TÁC
        p.setBookmarked(true); // Mặc định TRUE vì nó ở trang Saved
        p.setMeLike(r.isMeLike());
        p.setFollowed(r.isFollowed());

        // Để NULL, sẽ được fetchUserInfoAndRefresh cập nhật
        p.setUserName(null);
        p.setUserAvatar(null);

        return p;
    }

    private void openPostDetail(Post post) {
        Intent intent;
        if ("Recipe".equalsIgnoreCase(post.getType())) {
            intent = new Intent(this, PostRecipeActivity.class);
        } else {
            intent = new Intent(this, PostDetailActivity.class);
        }

        // TRUYỀN DỮ LIỆU CẦN THIẾT
        intent.putExtra("POST_ID", post.get_id());
        intent.putExtra("USER_NAME", post.getUserName());
        intent.putExtra("USER_AVATAR", post.getUserAvatar());
        intent.putExtra("CAPTION", post.getCaption());
        intent.putExtra("LIKES", post.getLike());
        intent.putExtra("COMMENTS", post.getComment());

        intent.putExtra("AUTHOR_ID", post.getUserID());

        intent.putExtra("IS_LIKED", post.isMeLike());
        intent.putExtra("IS_BOOKMARKED", post.isBookmarked());
        intent.putExtra("IS_FOLLOWED", post.isFollowed());

        if (post.getMedia() != null && !post.getMedia().isEmpty()) {
            intent.putExtra("MEDIA_URL", post.getMedia().get(0));
        }
        startActivity(intent);
    }

    private void setupBottomNavigation() {
        View navView = binding.bottomNavigationBar.getRoot();
        View navHome = navView.findViewById(R.id.nav_home);
        View navSearch = navView.findViewById(R.id.nav_search);
        View navAdd = navView.findViewById(R.id.nav_add);
        View navSaved = navView.findViewById(R.id.nav_saved);
        View navProfile = navView.findViewById(R.id.nav_profile);

        if (navHome != null) navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
        if (navSearch != null) navSearch.setOnClickListener(v -> {
            startActivity(new Intent(this, SearchActivity.class));
            finish();
        });
        if (navAdd != null) navAdd.setOnClickListener(v -> {
            startActivity(new Intent(this, NewPostActivity.class));
        });
        if (navProfile != null) navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
            finish();
        });
    }
}