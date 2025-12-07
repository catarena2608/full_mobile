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

    private static final String TAG = "ProfileActivity";

    // --- UI Components ---
    private ImageView coverImageView, profileImageView;
    private TextView nameTextView, handleTextView, linkTextView;
    private TextView postsCountTextView, followersCountTextView, followingCountTextView;
    private AppCompatButton editButton, shareButton, logoutButton;
    private LinearLayout layoutLink;
    private RecyclerView rvProfileImages;

    // --- Tabs UI ---
    private LinearLayout tabPosts, tabSaved;
    private TextView tvTabPosts, tvTabSaved;
    private ImageView ivTabPosts, ivTabSaved;

    // --- Data Management ---
    private String myUserId;
    private UserDao userDao;
    private UserResponse currentUser;
    private ProfilePostAdapter postAdapter;

    // State
    private boolean isShowingSaved = false; // false = Tab Post, true = Tab Saved

    // Cache Data (Tránh gọi API liên tục khi chuyển Tab)
    private List<Post> myPostsCache = new ArrayList<>();
    private List<Post> savedPostsCache = new ArrayList<>();

    // --- Launchers ---

    // 1. Xử lý kết quả trả về từ EditProfileActivity
    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // Kiểm tra xem dữ liệu có thay đổi không
                    boolean isUpdated = result.getData().getBooleanExtra("UPDATED", false);
                    if (isUpdated) {
                        Log.d(TAG, "Profile updated via EditActivity -> Reloading from SQLite");
                        // CHỈ CẦN LOAD TỪ CACHE (Vì EditActivity đã save vào DB rồi)
                        loadProfileFromCache();
                    }
                }
            }
    );

    // 2. Launcher cho QR Code (Nếu cần xử lý sau này)
    private final ActivityResultLauncher<Intent> qrCodeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> { /* Có thể xử lý logic sau khi share QR xong */ }
    );

    // ==================================================================
    // 1. LIFECYCLE
    // ==================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Khởi tạo Core Data
        userDao = new UserDao(this);
        myUserId = TokenManager.getUserId(this);

        if (TextUtils.isEmpty(myUserId)) {
            Toast.makeText(this, "Phiên đăng nhập không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupListeners();
        setupBottomNavigation();

        // 1. Load User Info
        loadProfileFromCache();     // Hiển thị ngay lập tức từ SQLite
        syncProfileWithServer();    // Gọi API để cập nhật mới nhất (nếu có khác biệt)

        // 2. Load Posts
        loadAllPostsData();         // Load cả Posts và Saved Posts
    }

    // ==================================================================
    // 2. INIT & SETUP
    // ==================================================================

    private void initViews() {
        // Profile Header
        coverImageView = findViewById(R.id.tv_cover);
        profileImageView = findViewById(R.id.iv_avatar);
        nameTextView = findViewById(R.id.tv_name);
        handleTextView = findViewById(R.id.tv_handle);
        linkTextView = findViewById(R.id.tv_link);
        layoutLink = findViewById(R.id.layout_link);

        // Stats
        postsCountTextView = findViewById(R.id.tv_post_count);
        followersCountTextView = findViewById(R.id.tv_followers_count);
        followingCountTextView = findViewById(R.id.tv_following_count);

        // Buttons
        editButton = findViewById(R.id.btn_edit_profile);
        shareButton = findViewById(R.id.btn_share_profile);
        logoutButton = findViewById(R.id.btn_logout);

        // RecyclerView
        rvProfileImages = findViewById(R.id.rv_profile_images);

        // Tabs
        LinearLayout layoutTabsParent = findViewById(R.id.layout_tabs);
        tabPosts = (LinearLayout) layoutTabsParent.getChildAt(0); // Giả sử layout XML đúng thứ tự
        tabSaved = (LinearLayout) layoutTabsParent.getChildAt(1);

        ivTabPosts = (ImageView) tabPosts.getChildAt(0);
        tvTabPosts = (TextView) tabPosts.getChildAt(1);
        ivTabSaved = (ImageView) tabSaved.getChildAt(0);
        tvTabSaved = (TextView) tabSaved.getChildAt(1);
    }

    private void setupRecyclerView() {
        postAdapter = new ProfilePostAdapter(this, this::openPostDetail);
        // Grid 3 cột chuẩn Instagram style
        rvProfileImages.setLayoutManager(new GridLayoutManager(this, 3));
        rvProfileImages.setAdapter(postAdapter);
    }

    private void setupListeners() {
        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            editProfileLauncher.launch(intent);
        });

        shareButton.setOnClickListener(v -> {
            if (currentUser == null) return;
            Intent intent = new Intent(ProfileActivity.this, MyQrCodeActivity.class);
            intent.putExtra("USER_ID", currentUser.getId());
            intent.putExtra("NAME", currentUser.getName());
            qrCodeLauncher.launch(intent);
        });

        logoutButton.setOnClickListener(v -> logout());

        // Tab Switching Logic
        tabPosts.setOnClickListener(v -> {
            if (isShowingSaved) switchTab(false);
        });

        tabSaved.setOnClickListener(v -> {
            if (!isShowingSaved) switchTab(true);
        });
    }

    // ==================================================================
    // 3. PROFILE DATA LOGIC (CACHE + SYNC)
    // ==================================================================

    private void loadProfileFromCache() {
        currentUser = userDao.getUser();
        if (currentUser != null) {
            updateUI(currentUser);
        } else {
            Log.d(TAG, "Cache empty, waiting for API...");
        }
    }

    private void syncProfileWithServer() {
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();
        apiService.getUserById(myUserId, myUserId).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Xử lý wrapper (nếu API trả về object bọc realUser)
                    UserResponse fetchedUser = response.body().getRealUser();
                    if (fetchedUser == null) fetchedUser = response.body(); // Fallback nếu không có getRealUser

                    if (fetchedUser != null) {
                        currentUser = fetchedUser;
                        updateUI(currentUser);
                        // Lưu vào SQLite để dùng cho lần sau
                        userDao.saveUser(currentUser);
                    }
                }
            }
            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Log.e(TAG, "Sync failed, sticking to cache", t);
            }
        });
    }

    private void updateUI(UserResponse user) {
        if (user == null) return;

        nameTextView.setText(user.getName());
        // Tạo handle từ tên (ví dụ: "Nguyen Van A" -> "@nguyenvana")
        String handle = "@" + (user.getName() != null ? user.getName().replaceAll("\\s+", "").toLowerCase() : "user");
        handleTextView.setText(handle);

        // Link logic
        if (user.getLink() != null && !user.getLink().isEmpty()) {
            linkTextView.setText(user.getLink().get(0));
            layoutLink.setVisibility(View.VISIBLE);
        } else {
            layoutLink.setVisibility(View.GONE);
        }

        // Counts
        postsCountTextView.setText(String.valueOf(user.getNumPosts()));
        followersCountTextView.setText(String.valueOf(user.getNumFollowed()));
        followingCountTextView.setText(String.valueOf(user.getNumFollowing()));

        // Images with Glide
        String avatarUrl = (user.getAvatar() != null && !user.getAvatar().isEmpty())
                ? user.getAvatar() : "https://i.pravatar.cc/150?u=" + myUserId;

        Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.chef_hat) // Đảm bảo bạn có resource này
                .circleCrop()
                .into(profileImageView);

        if (user.getCoverImage() != null && !user.getCoverImage().isEmpty()) {
            Glide.with(this).load(user.getCoverImage()).centerCrop().into(coverImageView);
        } else {
            coverImageView.setImageResource(android.R.color.darker_gray);
        }
    }

    // ==================================================================
    // 4. POSTS LOGIC & TABS
    // ==================================================================

    private void loadAllPostsData() {
        loadMyPosts();
        loadSavedPosts();
        switchTab(false); // Mặc định hiển thị Post của tôi
    }

    private void switchTab(boolean showSaved) {
        isShowingSaved = showSaved;
        updateTabUI(showSaved);

        // Switch data source instantly from Cache
        if (showSaved) {
            postAdapter.setPosts(savedPostsCache);
        } else {
            postAdapter.setPosts(myPostsCache);
        }
    }

    private void updateTabUI(boolean showSaved) {
        int activeColor = ContextCompat.getColor(this, android.R.color.black);
        int inactiveColor = ContextCompat.getColor(this, android.R.color.darker_gray);

        // Tab Posts
        ivTabPosts.setColorFilter(showSaved ? inactiveColor : activeColor);
        tvTabPosts.setTextColor(showSaved ? inactiveColor : activeColor);
        tabPosts.setBackgroundResource(showSaved ? 0 : R.drawable.bg_outline_orange_box);

        // Tab Saved
        ivTabSaved.setColorFilter(showSaved ? activeColor : inactiveColor);
        tvTabSaved.setTextColor(showSaved ? activeColor : inactiveColor);
        tabSaved.setBackgroundResource(showSaved ? R.drawable.bg_outline_orange_box : 0);
    }

    // --- API Fetching ---

    private void loadMyPosts() {
        RetrofitClient.getInstance(this).getApiService().getPostsByUserID(myUserId).enqueue(new Callback<PostsResponse>() {
            @Override
            public void onResponse(Call<PostsResponse> call, Response<PostsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    myPostsCache = processRawPosts(response.body().getPosts());
                    // Nếu đang ở tab này thì update UI luôn
                    if (!isShowingSaved) postAdapter.setPosts(myPostsCache);
                }
            }
            @Override
            public void onFailure(Call<PostsResponse> call, Throwable t) { Log.e(TAG, "Load My Posts Failed", t); }
        });
    }

    private void loadSavedPosts() {
        RetrofitClient.getInstance(this).getApiService().getSavedPosts().enqueue(new Callback<PostsResponse>() {
            @Override
            public void onResponse(Call<PostsResponse> call, Response<PostsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    savedPostsCache = processRawPosts(response.body().getPosts());
                    // Nếu đang ở tab này thì update UI luôn
                    if (isShowingSaved) postAdapter.setPosts(savedPostsCache);
                }
            }
            @Override
            public void onFailure(Call<PostsResponse> call, Throwable t) { Log.e(TAG, "Load Saved Posts Failed", t); }
        });
    }

    // Helper: Chuyển đổi dữ liệu để tránh null safety issues
    private List<Post> processRawPosts(List<Post> rawPosts) {
        if (rawPosts == null) return new ArrayList<>();
        // Clone list để đảm bảo an toàn dữ liệu
        return new ArrayList<>(rawPosts);
    }

    private void openPostDetail(Post post) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("POST_ID", post.get_id());
        // Truyền thêm dữ liệu cần thiết để hiển thị nhanh
        startActivity(intent);
    }

    // ==================================================================
    // 5. NAVIGATION & LOGOUT
    // ==================================================================

    private void logout() {
        // 1. Clear Local Data
        userDao.clearUser();
        TokenManager.clearSession(this);
        RetrofitClient.clearCookies(this);

        // 2. Call API Logout (Best effort)
        RetrofitClient.getInstance(this).getApiService().logout().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {}
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });

        // 3. Navigate to Login
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBottomNavigation() {
        View navView = findViewById(R.id.bottom_navigation_bar);
        if (navView == null) return;

        // Helper để set click listener gọn hơn
        setNavListener(navView, R.id.nav_home, HomeActivity.class);
        setNavListener(navView, R.id.nav_search, SearchActivity.class);
        setNavListener(navView, R.id.nav_add, NewPostActivity.class);
        setNavListener(navView, R.id.nav_saved, SavedPostsActivity.class);
    }

    private void setNavListener(View parent, int id, Class<?> targetActivity) {
        View view = parent.findViewById(id);
        if (view != null) {
            view.setOnClickListener(v -> {
                startActivity(new Intent(this, targetActivity));
                if (targetActivity != NewPostActivity.class) {
                    finish(); // Chỉ finish nếu chuyển sang màn hình chính khác, NewPost thường là activity con
                }
            });
        }
    }
}