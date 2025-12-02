package course.examples.nt118;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import course.examples.nt118.adapter.ProfilePostAdapter;
import course.examples.nt118.db.UserDao;
import course.examples.nt118.model.Post;
import course.examples.nt118.model.PostsResponse;
import course.examples.nt118.model.UserResponse;
import course.examples.nt118.network.ApiService;
import course.examples.nt118.network.RetrofitClient;
import course.examples.nt118.utils.TokenManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = ProfileActivity.class.getSimpleName();

    // UI Components
    private ImageView coverImageView, profileImageView;
    private TextView nameTextView, handleTextView, linkTextView;
    private TextView postsCountTextView, followersCountTextView, followingCountTextView;
    private AppCompatButton editButton, shareButton, logoutButton;
    private LinearLayout layoutLink, layoutButtons;
    private RecyclerView rvProfileImages;

    // Tabs UI
    private LinearLayout tabPosts, tabSaved;
    private TextView tvTabPosts, tvTabSaved;
    private ImageView ivTabPosts, ivTabSaved;

    // Data Caching
    private String myUserId;
    private UserDao userDao;
    private ProfilePostAdapter postAdapter;
    private boolean isShowingSaved = false; // Trạng thái tab hiện tại (false = Posts, true = Saved)

    // *** CACHE DỮ LIỆU ĐỂ TRÁNH GỌI API LẶP LẠI ***
    private List<Post> myPostsCache = new ArrayList<>();
    private List<Post> savedPostsCache = new ArrayList<>();

    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    boolean isUpdated = result.getData().getBooleanExtra("UPDATED", false);
                    if (isUpdated) {
                        loadProfileFromCache();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "1. onCreate");
        setContentView(R.layout.activity_profile);

        userDao = new UserDao(this);
        myUserId = TokenManager.getUserId(this);

        if (TextUtils.isEmpty(myUserId)) {
            Toast.makeText(this, "Lỗi đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupListeners();
        setupBottomNavigation();

        // 1. Load Info User
        loadProfileFromCache();
        syncProfileWithServer();

        // 2. Load Dữ liệu cho cả hai tab (Posts và Saved) khi Activity khởi tạo
        loadAllPostsData();
    }

    @Override
    protected void onStart() { super.onStart(); Log.d(TAG, "2. onStart"); }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "3. onResume");
    }

    @Override
    protected void onPause() { super.onPause(); Log.d(TAG, "4. onPause"); }

    @Override
    protected void onStop() { super.onStop(); Log.d(TAG, "5. onStop"); }

    @Override
    protected void onRestart() { super.onRestart(); Log.d(TAG, "6. onRestart"); }

    @Override
    protected void onDestroy() { super.onDestroy(); Log.d(TAG, "7. onDestroy"); }

    private void initViews() {
        coverImageView = findViewById(R.id.tv_cover);
        profileImageView = findViewById(R.id.iv_avatar);
        nameTextView = findViewById(R.id.tv_name);
        handleTextView = findViewById(R.id.tv_handle);
        linkTextView = findViewById(R.id.tv_link);
        layoutLink = findViewById(R.id.layout_link);
        postsCountTextView = findViewById(R.id.tv_post_count);
        followersCountTextView = findViewById(R.id.tv_followers_count);
        followingCountTextView = findViewById(R.id.tv_following_count);
        editButton = findViewById(R.id.btn_edit_profile);
        shareButton = findViewById(R.id.btn_share_profile);
        logoutButton = findViewById(R.id.btn_logout);
        rvProfileImages = findViewById(R.id.rv_profile_images);

        // Init Tabs
        LinearLayout layoutTabsParent = findViewById(R.id.layout_tabs);
        tabPosts = (LinearLayout) layoutTabsParent.getChildAt(0);
        tabSaved = (LinearLayout) layoutTabsParent.getChildAt(1);

        ivTabPosts = (ImageView) tabPosts.getChildAt(0);
        tvTabPosts = (TextView) tabPosts.getChildAt(1);
        ivTabSaved = (ImageView) tabSaved.getChildAt(0);
        tvTabSaved = (TextView) tabSaved.getChildAt(1);
    }

    private void setupRecyclerView() {
        postAdapter = new ProfilePostAdapter(this, this::openPostDetail);
        rvProfileImages.setLayoutManager(new GridLayoutManager(this, 3)); // 3 cột
        rvProfileImages.setAdapter(postAdapter);
    }

    private void setupListeners() {
        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            editProfileLauncher.launch(intent);
        });

        logoutButton.setOnClickListener(v -> logout());

        // Click Tab Posts: Chỉ chuyển tab nếu không phải tab hiện tại
        tabPosts.setOnClickListener(v -> {
            if (isShowingSaved) switchTab(false);
        });

        // Click Tab Saved: Chỉ chuyển tab nếu không phải tab hiện tại
        tabSaved.setOnClickListener(v -> {
            if (!isShowingSaved) switchTab(true);
        });
    }

    // --- LOGIC GỌI API LẦN ĐẦU & CACHING ---
    private void loadAllPostsData() {
        // Load Posts của tôi
        loadMyPosts(false);
        // Load Saved Posts
        loadSavedPosts(false);

        // Mặc định hiển thị tab Posts (gọi switchTab lần đầu tiên)
        switchTab(false);
    }


    // --- LOGIC CHUYỂN TAB (CHỈ DÙNG CACHE) ---
    private void switchTab(boolean showSaved) {
        isShowingSaved = showSaved;

        // Update UI (Đổi màu tab active)
        int activeColor = ContextCompat.getColor(this, android.R.color.black);
        int inactiveColor = ContextCompat.getColor(this, android.R.color.darker_gray);

        if (!showSaved) { // Active Posts
            ivTabPosts.setColorFilter(activeColor);
            tvTabPosts.setTextColor(activeColor);
            tabPosts.setBackgroundResource(R.drawable.bg_outline_orange_box);

            ivTabSaved.setColorFilter(inactiveColor);
            tvTabSaved.setTextColor(inactiveColor);
            tabSaved.setBackground(null);

            // Dùng cache đã có
            postAdapter.setPosts(myPostsCache);
        } else { // Active Saved
            ivTabPosts.setColorFilter(inactiveColor);
            tvTabPosts.setTextColor(inactiveColor);
            tabPosts.setBackground(null);

            ivTabSaved.setColorFilter(activeColor);
            tvTabSaved.setTextColor(activeColor);
            tabSaved.setBackgroundResource(R.drawable.bg_outline_orange_box);

            // Dùng cache đã có
            postAdapter.setPosts(savedPostsCache);
        }
    }

    // --- API CALLS (Cập nhật cache) ---

    // Thêm tham số 'isRefresh' để kiểm soát việc cập nhật UI/cache
    private void loadMyPosts(boolean isRefresh) {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.getPostsByUserID(myUserId).enqueue(new Callback<PostsResponse>() {
            @Override
            public void onResponse(Call<PostsResponse> call, Response<PostsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Post> rawPosts = response.body().getPosts();
                    myPostsCache = processRawPosts(rawPosts); // Cập nhật cache

                    if (isRefresh || !isShowingSaved) {
                        // Nếu đang refresh thủ công HOẶC tab Posts đang hiển thị, cập nhật Adapter
                        postAdapter.setPosts(myPostsCache);
                    }
                } else {
                    myPostsCache = new ArrayList<>();
                    if (isRefresh || !isShowingSaved) {
                        postAdapter.setPosts(new ArrayList<>());
                    }
                }
            }
            @Override
            public void onFailure(Call<PostsResponse> call, Throwable t) {
                // ...
            }
        });
    }

    // Thêm tham số 'isRefresh' để kiểm soát việc cập nhật UI/cache
    private void loadSavedPosts(boolean isRefresh) {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.getSavedPosts().enqueue(new Callback<PostsResponse>() {
            @Override
            public void onResponse(Call<PostsResponse> call, Response<PostsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Post> rawPosts = response.body().getPosts();
                    savedPostsCache = processRawPosts(rawPosts); // Cập nhật cache

                    if (isRefresh || isShowingSaved) {
                        // Nếu đang refresh thủ công HOẶC tab Saved đang hiển thị, cập nhật Adapter
                        postAdapter.setPosts(savedPostsCache);
                    }
                } else {
                    savedPostsCache = new ArrayList<>();
                    if (isRefresh || isShowingSaved) {
                        postAdapter.setPosts(new ArrayList<>());
                    }
                }
            }
            @Override
            public void onFailure(Call<PostsResponse> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Lỗi tải bài đã lưu", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private List<Post> processRawPosts(List<Post> rawPosts) {
        List<Post> posts = new ArrayList<>();
        if (rawPosts != null) {
            for (Post r : rawPosts) {
                Post p = new Post();
                p.set_id(r.get_id());
                p.setMedia(r.getMedia());
                p.setCaption(r.getCaption());
                p.setLike(r.getLike());
                p.setComment(r.getComment());
                p.setUserID(r.getUserID());
                posts.add(p);
            }
        }
        return posts;
    }

    private void openPostDetail(Post post) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("POST_ID", post.get_id());
        intent.putExtra("CAPTION", post.getCaption());
        if (post.getMedia() != null && !post.getMedia().isEmpty()) {
            intent.putExtra("MEDIA_URL", post.getMedia().get(0));
        }
        startActivity(intent);
    }

    // ==================================================================
    // DATA LOADING (PROFILE INFO)
    // ==================================================================
    // ... (Giữ nguyên logic loadProfileFromCache, syncProfileWithServer, updateUI, logout)
    // ...

    private void loadProfileFromCache() {
        UserResponse cachedUser = userDao.getUser();
        if (cachedUser != null) {
            Log.d(TAG, "Loaded from SQLite: " + cachedUser.getName());
            updateUI(cachedUser);
        } else {
            Log.d(TAG, "SQLite empty -> Waiting for API");
        }
    }

    private void syncProfileWithServer() {
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();
        apiService.getUserById(myUserId, myUserId).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse user = response.body().getRealUser();

                    if (user != null) {
                        updateUI(user);
                        userDao.saveUser(user);
                        Log.d(TAG, "Synced with Server & Saved to SQLite");
                    }
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Log.e(TAG, "Sync failed (Offline mode active)", t);
            }
        });
    }

    private void updateUI(UserResponse user) {
        if (user == null) return;

        String name = user.getName();
        if (name != null) {
            nameTextView.setText(name);
            handleTextView.setText("@" + name.replaceAll("\\s+", "").toLowerCase());
        }

        if (user.getLink() != null && !user.getLink().isEmpty()) {
            linkTextView.setText(user.getLink().get(0));
            layoutLink.setVisibility(View.VISIBLE);
        } else {
            layoutLink.setVisibility(View.GONE);
        }

        postsCountTextView.setText(String.valueOf(user.getNumPosts()));
        followersCountTextView.setText(String.valueOf(user.getNumFollowed()));
        followingCountTextView.setText(String.valueOf(user.getNumFollowing()));

        String avatarUrl = (user.getAvatar() != null && !user.getAvatar().isEmpty())
                ? user.getAvatar() : "https://i.pravatar.cc/150?u=" + myUserId;

        Glide.with(this).load(avatarUrl).placeholder(R.drawable.chef_hat).circleCrop().into(profileImageView);

        String coverUrl = (user.getCoverImage() != null && !user.getCoverImage().isEmpty())
                ? user.getCoverImage() : null;

        if (coverUrl != null) {
            Glide.with(this).load(coverUrl).centerCrop().into(coverImageView);
        } else {
            coverImageView.setImageResource(android.R.color.darker_gray);
        }

        editButton.setVisibility(View.VISIBLE);
        logoutButton.setVisibility(View.VISIBLE);
    }

    private void logout() {
        userDao.clearUser();
        TokenManager.clearSession(this);
        RetrofitClient.clearCookies(this);

        RetrofitClient.getInstance(this).getApiService().logout().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {}
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });

        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBottomNavigation() {
        View navView = findViewById(R.id.bottom_navigation_bar);
        if (navView != null) {
            View navHome = navView.findViewById(R.id.nav_home);
            View navSearch = navView.findViewById(R.id.nav_search);
            View navAdd = navView.findViewById(R.id.nav_add);
            View navSaved = navView.findViewById(R.id.nav_saved);

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
            if (navSaved != null) navSaved.setOnClickListener(v -> {
                startActivity(new Intent(this, SavedPostsActivity.class));
                finish();
            });
        }
    }
}