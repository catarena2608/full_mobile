package course.examples.nt118;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import course.examples.nt118.adapter.PostAdapter;
import course.examples.nt118.adapter.UserAdapter;
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

public class SearchActivity extends AppCompatActivity {

    private static final String TAG = SearchActivity.class.getSimpleName();

    // UI
    private EditText etSearch;
    private RecyclerView rvResults;
    private TextView tvRecentTitle, tvSeeAll;

    // Tabs UI Refs
    private TextView tvTabUser, tvTabPost;
    private View lineTabUser, lineTabPost;

    // Data & Adapters
    private String currentTab = "USER"; // Mặc định là tìm kiếm User
    private String userId; // ID người dùng hiện tại
    private final Map<String, UserResponse> userCache = new HashMap<>();

    private UserAdapter userAdapter;
    private PostAdapter postAdapter;

    // ==================================================================
    // 1. LIFECYCLE
    // ==================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "1. onCreate: Init SearchActivity");
        setContentView(R.layout.activity_search);

        userId = TokenManager.getUserId(this);

        initViews();
        initAdapters();
        setupBottomNavigation();

        // Mặc định chọn tab User
        selectTab("USER");

        setupSearchListener();
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

    // ==================================================================
    // 2. SETUP UI & ADAPTERS
    // ==================================================================

    private void initViews() {
        etSearch = findViewById(R.id.et_search);
        rvResults = findViewById(R.id.rv_search_history);
        tvRecentTitle = findViewById(R.id.tv_recent);
        tvSeeAll = findViewById(R.id.tv_see_all);

        // Tab Refs
        tvTabUser = findViewById(R.id.tv_tab_user);
        lineTabUser = findViewById(R.id.line_tab_user);
        tvTabPost = findViewById(R.id.tv_tab_post);
        lineTabPost = findViewById(R.id.line_tab_post);

        rvResults.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initAdapters() {
        // 1. UserAdapter
        userAdapter = new UserAdapter(this, new ArrayList<>(), false, new UserAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(UserResponse user) {
                Intent intent = new Intent(SearchActivity.this, ProfileActivity.class);
                intent.putExtra("USER_ID", user.getId());
                startActivity(intent);
            }
            @Override
            public void onRemoveClick(UserResponse user, int position) { }
        });

        // 2. PostAdapter
        postAdapter = new PostAdapter(this, new PostAdapter.PostInteractionListener() {
            @Override public void onLikeClicked(String pId, boolean liked) {
                // Implement like logic here if needed (copy from HomeActivity)
            }
            @Override public void onFollowClicked(String uId, boolean followed) {
                // Implement follow logic here if needed
            }
            @Override public void onBookmarkClicked(String pId, boolean marked) {
                // Implement bookmark logic here if needed
            }
            @Override public void onCommentClicked(Post post) { openPostDetail(post); }
            @Override public void onPostClicked(Post post) { openPostDetail(post); }
            @Override public void onUserClick(String uId) {
                Intent intent = new Intent(SearchActivity.this, ProfileActivity.class);
                intent.putExtra("USER_ID", uId);
                startActivity(intent);
            }
        });
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
        // Add states if available in search results
        intent.putExtra("IS_LIKED", post.isMeLike());
        intent.putExtra("IS_BOOKMARKED", post.isBookmarked());
        intent.putExtra("IS_FOLLOWED", post.isFollowed());
        intent.putExtra("AUTHOR_ID", post.getUserID());

        if (post.getMedia() != null && !post.getMedia().isEmpty()) {
            intent.putExtra("MEDIA_URL", post.getMedia().get(0));
        }
        startActivity(intent);
    }

    // ==================================================================
    // 3. TAB LOGIC
    // ==================================================================

    public void onTabClicked(View view) {
        int id = view.getId();
        if (id == R.id.tab_user) {
            selectTab("USER");
        } else if (id == R.id.tab_post) {
            selectTab("POST");
        } else {
            Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectTab(String tabName) {
        this.currentTab = tabName;
        resetTabs();

        int orange = Color.parseColor("#FF9800");

        if (tabName.equals("USER")) {
            if (tvTabUser != null) {
                tvTabUser.setTextColor(orange);
                tvTabUser.setTypeface(null, Typeface.BOLD);
            }
            if (lineTabUser != null) {
                lineTabUser.setVisibility(View.VISIBLE);
                lineTabUser.setBackgroundColor(orange);
            }
            if (userAdapter != null) rvResults.setAdapter(userAdapter);

        } else if (tabName.equals("POST")) {
            if (tvTabPost != null) {
                tvTabPost.setTextColor(orange);
                tvTabPost.setTypeface(null, Typeface.BOLD);
            }
            if (lineTabPost != null) {
                lineTabPost.setVisibility(View.VISIBLE);
                lineTabPost.setBackgroundColor(orange);
            }
            if (postAdapter != null) rvResults.setAdapter(postAdapter);
        }

        String query = etSearch.getText().toString().trim();
        if (!TextUtils.isEmpty(query)) {
            performSearch(query);
        } else {
            showHistoryUI();
        }
    }

    private void resetTabs() {
        int gray = Color.parseColor("#757575");
        if (tvTabUser != null) {
            tvTabUser.setTextColor(gray);
            tvTabUser.setTypeface(null, Typeface.NORMAL);
        }
        if (lineTabUser != null) lineTabUser.setVisibility(View.INVISIBLE);

        if (tvTabPost != null) {
            tvTabPost.setTextColor(gray);
            tvTabPost.setTypeface(null, Typeface.NORMAL);
        }
        if (lineTabPost != null) lineTabPost.setVisibility(View.INVISIBLE);
    }

    // ==================================================================
    // 4. SEARCH LOGIC
    // ==================================================================

    private void setupSearchListener() {
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText().toString().trim();
                if (!TextUtils.isEmpty(query)) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    performSearch(query);
                }
                return true;
            }
            return false;
        });
    }

    private void performSearch(String query) {
        tvRecentTitle.setText("Kết quả tìm kiếm");
        tvSeeAll.setVisibility(View.GONE);

        if ("USER".equals(currentTab)) {
            searchUsers(query);
        } else if ("POST".equals(currentTab)) {
            searchPosts(query);
        }
    }

    private void searchUsers(String query) {
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();
        apiService.searchUsers(query, "name").enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = response.body().string();
                        List<UserResponse> list = new Gson().fromJson(json, new TypeToken<List<UserResponse>>(){}.getType());
                        if (list != null && !list.isEmpty()) {
                            userAdapter.setData(list);
                        } else {
                            userAdapter.setData(new ArrayList<>());
                            Toast.makeText(SearchActivity.this, "Không tìm thấy user nào", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Parse Error", e);
                    }
                } else {
                    userAdapter.setData(new ArrayList<>());
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(SearchActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchPosts(String query) {
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();
        apiService.searchPosts(query, null).enqueue(new Callback<PostsResponse>() {
            @Override
            public void onResponse(Call<PostsResponse> call, Response<PostsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // [UPDATED] Get List<Post> directly
                    List<Post> rawPosts = response.body().getPosts();

                    if (rawPosts != null && !rawPosts.isEmpty()) {
                        // Map data (meLike, bookmarked are auto-mapped if JSON is correct)
                        List<Post> uiPosts = mapPostsToUiModel(rawPosts);
                        postAdapter.setData(uiPosts);

                        // Fetch user info for each post
                        for (Post p : uiPosts) {
                            fetchUserInfo(p);
                        }
                    } else {
                        postAdapter.setData(new ArrayList<>());
                        Toast.makeText(SearchActivity.this, "Không tìm thấy bài viết nào", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    postAdapter.setData(new ArrayList<>());
                }
            }
            @Override
            public void onFailure(Call<PostsResponse> call, Throwable t) {
                Toast.makeText(SearchActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==================================================================
    // 5. HELPERS
    // ==================================================================

    // [UPDATED] Takes List<Post> directly
    private List<Post> mapPostsToUiModel(List<Post> remotePosts) {
        List<Post> mapped = new ArrayList<>();
        for (Post r : remotePosts) {
            // If Post class structure matches API response structure perfectly (which it should now),
            // we can just use the object directly, but we need to init UI fields.

            // However, to be safe and consistent with HomeActivity logic:
            Post p = new Post();
            p.set_id(r.get_id());
            p.setUserID(r.getUserID());
            p.setCaption(r.getCaption());
            p.setType(r.getType());
            p.setMedia(r.getMedia());
            p.setLike(r.getLike());
            p.setComment(r.getComment());

            p.setMeLike(r.isMeLike());
            p.setBookmarked(r.isBookmarked());

            p.setFollowed(false); // Wait for fetchUserInfo

            p.setUserName("Loading...");
            p.setUserAvatar("");
            mapped.add(p);
        }
        return mapped;
    }

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
                    // [UPDATED] Use getRealUser()
                    UserResponse user = response.body().getRealUser();
                    if (user != null) {
                        userCache.put(uid, user);
                        updatePostUserInfo(post, user);
                    }
                }
            }
            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {}
        });
    }

    private void updatePostUserInfo(Post post, UserResponse user) {
        List<Post> currentList = postAdapter.getCurrentList();
        for (int i = 0; i < currentList.size(); i++) {
            Post p = currentList.get(i);
            if (p != null && p.get_id().equals(post.get_id())) {
                p.setUserName(user.getName());
                p.setUserAvatar(user.getAvatar());
                p.setFollowed(user.isMeFollow()); // Update follow status
                postAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void showHistoryUI() {
        tvRecentTitle.setText("Mới đây");
        tvSeeAll.setVisibility(View.VISIBLE);
        if ("USER".equals(currentTab) && userAdapter != null) {
            userAdapter.setData(new ArrayList<>());
        } else if (postAdapter != null) {
            postAdapter.setData(new ArrayList<>());
        }
    }

    private void setupBottomNavigation() {
        View navView = findViewById(R.id.bottom_navigation_bar);
        if (navView != null) {
            View navHome = navView.findViewById(R.id.nav_home);
            View navSearch = navView.findViewById(R.id.nav_search); // Current
            View navAdd = navView.findViewById(R.id.nav_add);
            View navSaved = navView.findViewById(R.id.nav_saved);
            View navProfile = navView.findViewById(R.id.nav_profile);

            if (navHome != null) navHome.setOnClickListener(v -> {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            });
            if (navAdd != null) navAdd.setOnClickListener(v -> {
                startActivity(new Intent(this, NewPostActivity.class));
            });
            if (navSaved != null) navSaved.setOnClickListener(v -> {
                startActivity(new Intent(this, SavedPostsActivity.class));
            });
            if (navProfile != null) navProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.putExtra("USER_ID", userId);
                startActivity(intent);
            });
        }
    }
}