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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class SearchActivity extends AppCompatActivity implements PostAdapter.PostInteractionListener {

    private static final String TAG = "SearchActivity";

    // UI
    private EditText etSearch;
    private ImageView ivSearching;
    private RecyclerView rvResults;
    private TextView tvRecentTitle, tvSeeAll;

    // Tabs
    private TextView tvTabUser, tvTabPost, tvTabTag;
    private View lineTabUser, lineTabPost, lineTabTag;

    // Data
    private String currentTab = "USER"; // USER, POST, TAG
    private String myUserId;

    // Adapters
    private UserAdapter userAdapter;
    private PostAdapter postAdapter;

    // Cache User Info
    private final Map<String, UserResponse> userCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        myUserId = TokenManager.getUserId(this);

        initViews();
        initAdapters();
        setupBottomNavigation();
        setupSearchInteractions();

        selectTab("USER");
    }

    private void initViews() {
        etSearch = findViewById(R.id.et_search);
        ivSearching = findViewById(R.id.iv_searching);
        rvResults = findViewById(R.id.rv_search_history);
        tvRecentTitle = findViewById(R.id.tv_recent);
        tvSeeAll = findViewById(R.id.tv_see_all);

        tvTabUser = findViewById(R.id.tv_tab_user);
        lineTabUser = findViewById(R.id.line_tab_user);
        tvTabPost = findViewById(R.id.tv_tab_post);
        lineTabPost = findViewById(R.id.line_tab_post);
        tvTabTag = findViewById(R.id.tv_tab_tag);
        lineTabTag = findViewById(R.id.line_tab_tag);

        rvResults.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initAdapters() {
        // 1. User Adapter
        userAdapter = new UserAdapter(this, new ArrayList<>(), false, new UserAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(UserResponse user) {
                openUserProfile(user.getId());
            }
            @Override
            public void onRemoveClick(UserResponse user, int position) { }
        });

        // 2. Post Adapter
        postAdapter = new PostAdapter(this, this);
    }

    private void openUserProfile(String targetUserId) {
        if (targetUserId == null) return;
        Intent intent;
        if (targetUserId.equals(myUserId)) {
            intent = new Intent(SearchActivity.this, ProfileActivity.class);
        } else {
            intent = new Intent(SearchActivity.this, OtherUserProfileActivity.class);
            intent.putExtra("USER_ID", targetUserId);
        }
        startActivity(intent);
    }

    // ==================================================================
    // SEARCH LOGIC
    // ==================================================================

    private void setupSearchInteractions() {
        ivSearching.setOnClickListener(v -> performSearch());
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            Toast.makeText(this, "Vui lòng nhập từ khóa", Toast.LENGTH_SHORT).show();
            return;
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);

        tvRecentTitle.setText("Kết quả tìm kiếm");
        tvSeeAll.setVisibility(View.GONE);

        userCache.clear();

        if ("USER".equals(currentTab)) {
            callApiSearchUser(query);
        } else if ("POST".equals(currentTab)) {
            callApiSearchPost(query);
        } else if ("TAG".equals(currentTab)) {
            callApiSearchPostByTag(query);
        }
    }

    // --- API CALLS ---

    private void callApiSearchUser(String query) {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.searchUsers(query, null).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<UserResponse> users = response.body().getListUsers();
                    if (users != null && !users.isEmpty()) {
                        userAdapter.setData(users);
                        rvResults.setAdapter(userAdapter);
                    } else handleEmptyResult();
                } else handleError();
            }
            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) { handleError(); }
        });
    }

    private void callApiSearchPost(String query) {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.searchPosts(query, null).enqueue(new Callback<PostsResponse>() {
            @Override
            public void onResponse(Call<PostsResponse> call, Response<PostsResponse> response) {
                handlePostSearchResponse(response);
            }
            @Override
            public void onFailure(Call<PostsResponse> call, Throwable t) { handleError(); }
        });
    }

    private void callApiSearchPostByTag(String query) {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.searchPostsByTag(query, null).enqueue(new Callback<PostsResponse>() {
            @Override
            public void onResponse(Call<PostsResponse> call, Response<PostsResponse> response) {
                handlePostSearchResponse(response);
            }
            @Override
            public void onFailure(Call<PostsResponse> call, Throwable t) { handleError(); }
        });
    }

    private void handlePostSearchResponse(Response<PostsResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
            List<Post> posts = response.body().getPosts();
            if (posts != null && !posts.isEmpty()) {
                postAdapter.setData(posts);
                rvResults.setAdapter(postAdapter);
                for (Post p : posts) fetchUserInfo(p);
            } else handleEmptyResult();
        } else handleError();
    }

    private void fetchUserInfo(Post post) {
        String uid = post.getUserID();
        if (TextUtils.isEmpty(uid)) return;

        if (userCache.containsKey(uid)) {
            updatePostUserInfo(post, userCache.get(uid));
            return;
        }

        RetrofitClient.getInstance(this).getApiService().getUserById(uid, myUserId).enqueue(new Callback<UserResponse>() {
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
            @Override public void onFailure(Call<UserResponse> call, Throwable t) {}
        });
    }

    private void updatePostUserInfo(Post post, UserResponse user) {
        // [QUAN TRỌNG] PostAdapter phải có hàm getCurrentList() như đã thêm
        List<Post> currentList = postAdapter.getCurrentList();
        if (currentList == null) return;

        for (int i = 0; i < currentList.size(); i++) {
            Post p = currentList.get(i);
            if (p != null && p.get_id().equals(post.get_id())) {
                p.setUserName(user.getName());
                p.setUserAvatar(user.getAvatar());
                p.setFollowed(user.isMeFollow());
                postAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void handleEmptyResult() {
        if ("USER".equals(currentTab)) userAdapter.setData(new ArrayList<>());
        else postAdapter.setData(new ArrayList<>());
        Toast.makeText(this, "Không tìm thấy kết quả", Toast.LENGTH_SHORT).show();
    }

    private void handleError() {
        Toast.makeText(this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
    }

    // ==================================================================
    // TAB LOGIC
    // ==================================================================

    public void onTabClicked(View view) {
        int id = view.getId();
        if (id == R.id.tab_user) selectTab("USER");
        else if (id == R.id.tab_post) selectTab("POST");
        else if (id == R.id.tab_tag) selectTab("TAG");
    }

    private void selectTab(String tabName) {
        this.currentTab = tabName;
        resetTabsUI();
        highlightTabUI(tabName);

        userAdapter.setData(new ArrayList<>());
        postAdapter.setData(new ArrayList<>());

        String query = etSearch.getText().toString().trim();
        if (!TextUtils.isEmpty(query)) performSearch();
        else showHistoryUI();
    }

    private void showHistoryUI() {
        tvRecentTitle.setText("Mới đây");
        tvSeeAll.setVisibility(View.VISIBLE);
    }

    private void highlightTabUI(String tabName) {
        int orange = Color.parseColor("#FF9800");
        if ("USER".equals(tabName)) setTabActive(tvTabUser, lineTabUser, orange);
        else if ("POST".equals(tabName)) setTabActive(tvTabPost, lineTabPost, orange);
        else if ("TAG".equals(tabName)) setTabActive(tvTabTag, lineTabTag, orange);
    }

    private void setTabActive(TextView tv, View line, int color) {
        if (tv != null) {
            tv.setTextColor(color);
            tv.setTypeface(null, Typeface.BOLD);
        }
        if (line != null) {
            line.setVisibility(View.VISIBLE);
            line.setBackgroundColor(color);
        }
    }

    private void resetTabsUI() {
        int gray = Color.parseColor("#757575");
        resetTabSingle(tvTabUser, lineTabUser, gray);
        resetTabSingle(tvTabPost, lineTabPost, gray);
        resetTabSingle(tvTabTag, lineTabTag, gray);
    }

    private void resetTabSingle(TextView tv, View line, int color) {
        if (tv != null) {
            tv.setTextColor(color);
            tv.setTypeface(null, Typeface.NORMAL);
        }
        if (line != null) line.setVisibility(View.INVISIBLE);
    }

    // ==================================================================
    // POST ADAPTER INTERACTIONS
    // ==================================================================

    @Override
    public void onLikeClicked(String postID, boolean isLiked) {
        updateLocalPostState(postID, "LIKE", !isLiked);
        Map<String, String> body = new HashMap<>();
        body.put("postID", postID);
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        Call<ResponseBody> call = !isLiked ? api.likePost(body) : api.unlikePost(body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) updateLocalPostState(postID, "LIKE", isLiked); // Revert
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                updateLocalPostState(postID, "LIKE", isLiked); // Revert
            }
        });
    }

    @Override
    public void onBookmarkClicked(String postID, boolean isBookmarked) {
        updateLocalPostState(postID, "BOOKMARK", !isBookmarked);
        Map<String, String> body = new HashMap<>();
        body.put("postID", postID);
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        Call<ResponseBody> call = !isBookmarked ? api.savePost(body) : api.unsavePost(body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {}
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    @Override
    public void onFollowClicked(String targetUserID, boolean isFollowed) {
        updateLocalPostState(targetUserID, "FOLLOW", !isFollowed);
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        Call<ResponseBody> call = !isFollowed ? api.followUser(targetUserID) : api.unfollowUser(targetUserID);
        call.enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {}
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    // [BẮT BUỘC PHẢI CÓ] Để thỏa mãn Interface, nhưng để TRỐNG để không làm gì cả
    @Override
    public void onMoreOptionClicked(Post post, View view) {
        // Không làm gì cả theo yêu cầu
    }

    @Override public void onCommentClicked(Post post) { openPostDetail(post); }
    @Override public void onPostClicked(Post post) { openPostDetail(post); }
    @Override public void onUserClick(String userID) { openUserProfile(userID); }

    private void updateLocalPostState(String id, String type, boolean newState) {
        List<Post> list = postAdapter.getCurrentList();
        if (list == null) return;

        for(int i=0; i<list.size(); i++) {
            Post p = list.get(i);
            boolean changed = false;

            if("LIKE".equals(type) && p.get_id().equals(id)) {
                p.setMeLike(newState);
                p.setLike(newState ? p.getLike()+1 : p.getLike()-1);
                changed = true;
            } else if ("BOOKMARK".equals(type) && p.get_id().equals(id)) {
                p.setBookmarked(newState);
                changed = true;
            } else if ("FOLLOW".equals(type) && p.getUserID().equals(id)) {
                p.setFollowed(newState);
                changed = true;
                if (userCache.containsKey(id)) {
                    UserResponse u = userCache.get(id);
                    if(u != null) { u.setMeFollow(newState); userCache.put(id, u); }
                }
            }

            if(changed) postAdapter.notifyItemChanged(i);
        }
    }

    private void openPostDetail(Post post) {
        Intent intent = new Intent(this, "Recipe".equalsIgnoreCase(post.getType()) ? PostRecipeActivity.class : PostDetailActivity.class);
        intent.putExtra("POST_ID", post.get_id());
        intent.putExtra("AUTHOR_ID", post.getUserID());
        startActivity(intent);
    }

    // ==================================================================
    // NAVIGATION
    // ==================================================================
    private void setupBottomNavigation() {
        View navView = findViewById(R.id.bottom_navigation_bar);
        if (navView != null) {
            navView.findViewById(R.id.nav_home).setOnClickListener(v -> {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            });
            navView.findViewById(R.id.nav_add).setOnClickListener(v -> startActivity(new Intent(this, NewPostActivity.class)));
            navView.findViewById(R.id.nav_saved).setOnClickListener(v -> startActivity(new Intent(this, SavedPostsActivity.class)));
            navView.findViewById(R.id.nav_profile).setOnClickListener(v -> {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.putExtra("USER_ID", myUserId);
                startActivity(intent);
            });
        }
    }
}