package course.examples.nt118;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
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

import course.examples.nt118.adapter.PostAdapter;
import course.examples.nt118.databinding.ActivityHomeBinding;
import course.examples.nt118.model.Post;
import course.examples.nt118.model.PostsResponse; // Chỉ cần cái này
import course.examples.nt118.model.UserResponse;
import course.examples.nt118.network.ApiService;
import course.examples.nt118.network.RetrofitClient;
import course.examples.nt118.utils.TokenManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity implements PostAdapter.PostInteractionListener {

    private static final String TAG = HomeActivity.class.getSimpleName();
    private ActivityHomeBinding binding;
    private PostAdapter postAdapter;

    // --- DATA & STATE ---
    private final List<Post> mAllPostsBuffer = new ArrayList<>();
    private final Map<String, UserResponse> userCache = new HashMap<>();
    private String userId;

    // Pagination controls
    private int mCurrentDisplayCount = 0;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    // Config
    private final int LIMIT_FIRST_LOAD = 5;
    private final int LIMIT_LOAD_MORE = 3;

    // ==================================================================
    // 1. LIFECYCLE LOGS
    // ==================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "1. onCreate: Khởi tạo HomeActivity");
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!checkUserSession()) return;

        setupViews();
        setupRecyclerView();
        setupBottomNavigation();

        // Load dữ liệu
        fetchAllPostsFromServer();
    }

    @Override
    protected void onStart() { super.onStart(); Log.d(TAG, "2. onStart"); }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "3. onResume: Home ready");
    }

    @Override
    protected void onPause() { super.onPause(); Log.d(TAG, "4. onPause"); }

    @Override
    protected void onStop() { super.onStop(); Log.d(TAG, "5. onStop"); }

    @Override
    protected void onRestart() { super.onRestart(); Log.d(TAG, "6. onRestart"); }

    @Override
    protected void onDestroy() { super.onDestroy(); Log.d(TAG, "7. onDestroy"); }

    // ==================================================================
    // 2. SETUP & AUTHENTICATION
    // ==================================================================

    private boolean checkUserSession() {
        userId = TokenManager.getUserId(this);
        if (TextUtils.isEmpty(userId)) {
            Log.w(TAG, "Session expired -> Redirecting to Login");
            redirectToLogin();
            return false;
        }
        Log.i(TAG, "Home session active cho UserID: " + userId);
        return true;
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupViews() {
        // Load avatar placeholder
        Glide.with(this)
                .load("https://i.pravatar.cc/150?u=" + userId)
                .placeholder(R.drawable.chef_hat)
                .circleCrop()
                .into(binding.ivCurrentUserAvatar);

        binding.ivCurrentUserAvatar.setOnClickListener(v -> openProfileScreen());
        binding.tvWhatOnYourMind.setOnClickListener(v -> openPostComposer());

        if (binding.ivNotification != null) {
            binding.ivNotification.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, NotificationActivity.class);
                startActivity(intent);
            });
        }

        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "User pull to refresh");
            fetchAllPostsFromServer();
        });
        binding.swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_orange_light);
    }

    private void setupRecyclerView() {
        postAdapter = new PostAdapter(this, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        binding.rvFeed.setLayoutManager(linearLayoutManager);
        binding.rvFeed.setAdapter(postAdapter);

        binding.rvFeed.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (dy <= 0) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= LIMIT_FIRST_LOAD) {
                        loadMoreFromBuffer();
                    }
                }
            }
        });
    }

    // ==================================================================
    // 3. DATA LOADING (API)
    // ==================================================================

    private void fetchAllPostsFromServer() {
        Log.d(TAG, "API: Fetching All Posts (Global Feed)...");
        isLoading = true;
        binding.swipeRefreshLayout.setRefreshing(true);

        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        // Call API to get posts (Newfeed) - Pass null userID to get all
        apiService.getAllPosts(null, null, null, null, null).enqueue(new Callback<PostsResponse>() {
            @Override
            public void onResponse(Call<PostsResponse> call, Response<PostsResponse> response) {
                binding.swipeRefreshLayout.setRefreshing(false);
                isLoading = false;

                if (response.isSuccessful() && response.body() != null) {
                    List<Post> rawPosts = response.body().getPosts(); // Lấy List<Post> luôn

                    if (rawPosts != null && !rawPosts.isEmpty()) {

                        // [LOGIC TO FILTER OUT MY OWN POSTS]
                        List<Post> filteredList = new ArrayList<>();
                        for (Post p : rawPosts) {
                            if (p.getUserID() != null && !p.getUserID().equals(userId)) {
                                // Set mặc định các trường UI
                                p.setUserName("Loading...");
                                p.setUserAvatar("");
                                // Lưu ý: meLike, isBookmarked đã được GSON tự map vào Post rồi
                                filteredList.add(p);
                            }
                        }

                        if (!filteredList.isEmpty()) {
                            mAllPostsBuffer.clear();
                            mAllPostsBuffer.addAll(filteredList);
                            mCurrentDisplayCount = 0;

                            postAdapter.setData(new ArrayList<>());
                            loadMoreFromBuffer();
                        } else {
                            Toast.makeText(HomeActivity.this, "Không có bài viết mới từ người khác", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(HomeActivity.this, "Danh sách trống", Toast.LENGTH_SHORT).show();
                    }
                } else if (response.code() == 401) {
                    TokenManager.clearSession(HomeActivity.this);
                    RetrofitClient.clearCookies(HomeActivity.this);
                    redirectToLogin();
                } else {
                    Log.e(TAG, "API Failed. Code: " + response.code());
                    Toast.makeText(HomeActivity.this, "Lỗi Server: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PostsResponse> call, Throwable t) {
                isLoading = false;
                binding.swipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, "Network Error", t);
                Toast.makeText(HomeActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMoreFromBuffer() {
        if (mCurrentDisplayCount >= mAllPostsBuffer.size()) {
            isLastPage = true;
            return;
        }

        isLoading = true;
        boolean isFirstLoad = (mCurrentDisplayCount == 0);

        if (!isFirstLoad) binding.progressBarLoadMore.setVisibility(View.VISIBLE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFirstLoad) binding.progressBarLoadMore.setVisibility(View.GONE);

            int limit = isFirstLoad ? LIMIT_FIRST_LOAD : LIMIT_LOAD_MORE;
            int endIndex = Math.min(mCurrentDisplayCount + limit, mAllPostsBuffer.size());

            if (endIndex > mCurrentDisplayCount) {
                List<Post> subList = new ArrayList<>(mAllPostsBuffer.subList(mCurrentDisplayCount, endIndex));

                if (isFirstLoad) postAdapter.setData(subList);
                else postAdapter.addData(subList);

                for (Post p : subList) fetchUserInfo(p);

                mCurrentDisplayCount = endIndex;
            }

            isLoading = false;
        }, 500);
    }

    // ==================================================================
    // 4. HELPER MAPPING & USER INFO
    // ==================================================================

    // Hàm này không cần nữa vì ta dùng trực tiếp Post, nhưng có thể giữ lại nếu muốn tùy chỉnh thêm
    // Tuy nhiên, logic gán mặc định đã được đưa vào fetchAllPostsFromServer rồi.

    private void fetchUserInfo(Post post) {
        String uid = post.getUserID();
        if (TextUtils.isEmpty(uid)) return;

        if (userCache.containsKey(uid)) {
            updatePostUserInfo(post, userCache.get(uid));
            return;
        }

        RetrofitClient.getInstance(this).getApiService().getUserById(uid, userId).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse user = response.body().getRealUser();
                    if (user != null) {
                        userCache.put(uid, user);
                        updatePostUserInfo(post, user);
                    }
                }
            }
            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) { }
        });
    }

    private void updatePostUserInfo(Post post, UserResponse user) {
        List<Post> currentList = postAdapter.getCurrentList();
        for (int i = 0; i < currentList.size(); i++) {
            Post p = currentList.get(i);
            if (p != null && p.get_id().equals(post.get_id())) {
                p.setUserName(user.getName());
                p.setUserAvatar(user.getAvatar());

                // Cập nhật follow status từ User API
                p.setFollowed(user.isMeFollow());

                postAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    // ==================================================================
    // 5. INTERACTION LOGIC
    // ==================================================================

    @Override
    public void onLikeClicked(String postID, boolean isLiked) {
        updateLocalState(postID, "LIKE", !isLiked);

        Map<String, String> body = new HashMap<>();
        body.put("postID", postID);
        Call<ResponseBody> call = !isLiked ?
                RetrofitClient.getInstance(this).getApiService().likePost(body) :
                RetrofitClient.getInstance(this).getApiService().unlikePost(body);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) updateLocalState(postID, "LIKE", isLiked); // Revert
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                updateLocalState(postID, "LIKE", isLiked);
            }
        });
    }

    @Override
    public void onBookmarkClicked(String postID, boolean isBookmarked) {
        updateLocalState(postID, "BOOKMARK", !isBookmarked);

        Map<String, String> body = Collections.singletonMap("postID", postID);
        Call<ResponseBody> call = !isBookmarked ?
                RetrofitClient.getInstance(this).getApiService().savePost(body) :
                RetrofitClient.getInstance(this).getApiService().unsavePost(body);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    updateLocalState(postID, "BOOKMARK", isBookmarked);
                } else {
                    Toast.makeText(HomeActivity.this, !isBookmarked ? "Đã lưu" : "Đã bỏ lưu", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                updateLocalState(postID, "BOOKMARK", isBookmarked);
            }
        });
    }

    @Override
    public void onFollowClicked(String targetUserID, boolean isFollowed) {
        updateLocalState(targetUserID, "FOLLOW", !isFollowed);

        Call<ResponseBody> call = !isFollowed ?
                RetrofitClient.getInstance(this).getApiService().followUser(targetUserID) :
                RetrofitClient.getInstance(this).getApiService().unfollowUser(targetUserID);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) updateLocalState(targetUserID, "FOLLOW", isFollowed);
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                updateLocalState(targetUserID, "FOLLOW", isFollowed);
            }
        });
    }

    // Hàm update UI chung để đỡ lặp code
    private void updateLocalState(String id, String type, boolean newState) {
        List<Post> currentList = postAdapter.getCurrentList();
        for (int i = 0; i < currentList.size(); i++) {
            Post p = currentList.get(i);
            boolean changed = false;

            if ("LIKE".equals(type) && p.get_id().equals(id)) {
                p.setMeLike(newState);
                p.setLike(newState ? p.getLike() + 1 : Math.max(0, p.getLike() - 1));
                changed = true;
            } else if ("BOOKMARK".equals(type) && p.get_id().equals(id)) {
                p.setBookmarked(newState);
                changed = true;
            } else if ("FOLLOW".equals(type) && p.getUserID().equals(id)) {
                p.setFollowed(newState);
                changed = true;
            }

            if (changed) postAdapter.notifyItemChanged(i);
        }
    }

    @Override
    public void onCommentClicked(Post post) { openPostDetail(post); }
    @Override
    public void onPostClicked(Post post) { openPostDetail(post); }
    @Override
    public void onUserClick(String targetUserID) {
        if (TextUtils.isEmpty(targetUserID)) return;

        if (targetUserID.equals(userId)) {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, OtherUserProfileActivity.class);
            intent.putExtra("USER_ID", targetUserID);
            startActivity(intent);
        }
    }

    private void openPostDetail(Post post) {
        Intent intent;
        if ("Recipe".equalsIgnoreCase(post.getType())) {
            intent = new Intent(this, PostRecipeActivity.class);
        } else {
            intent = new Intent(this, PostDetailActivity.class);
        }

        intent.putExtra("POST_ID", post.get_id());
        intent.putExtra("USER_NAME", post.getUserName());
        intent.putExtra("USER_AVATAR", post.getUserAvatar());
        intent.putExtra("CAPTION", post.getCaption());
        intent.putExtra("LIKES", post.getLike());
        intent.putExtra("COMMENTS", post.getComment());
        intent.putExtra("IS_LIKED", post.isMeLike());
        intent.putExtra("IS_BOOKMARKED", post.isBookmarked());
        intent.putExtra("IS_FOLLOWED", post.isFollowed());
        intent.putExtra("AUTHOR_ID", post.getUserID());

        if (post.getMedia() != null && !post.getMedia().isEmpty()) {
            intent.putExtra("MEDIA_URL", post.getMedia().get(0));
        }
        startActivity(intent);
    }

    private void openProfileScreen() { startActivity(new Intent(this, ProfileActivity.class)); }
    private void openPostComposer() { startActivity(new Intent(this, NewPostActivity.class)); }

    private void setupBottomNavigation() {
        View navView = binding.bottomNavigationBar.getRoot();
        if (navView == null) return;

        View navHome = navView.findViewById(R.id.nav_home);
        View navSearch = navView.findViewById(R.id.nav_search);
        View navAdd = navView.findViewById(R.id.nav_add);
        View navSaved = navView.findViewById(R.id.nav_saved);
        View navProfile = navView.findViewById(R.id.nav_profile);

        if (navHome != null) navHome.setOnClickListener(v -> binding.rvFeed.smoothScrollToPosition(0));
        if (navSearch != null) navSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        if (navAdd != null) navAdd.setOnClickListener(v -> openPostComposer());
        if (navSaved != null) navSaved.setOnClickListener(v -> startActivity(new Intent(this, SavedPostsActivity.class)));
        if (navProfile != null) navProfile.setOnClickListener(v -> openProfileScreen());
    }
}