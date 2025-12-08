package course.examples.nt118;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import course.examples.nt118.model.PostsResponse;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!checkUserSession()) return;

        setupViews();
        setupRecyclerView();
        setupBottomNavigation();

        fetchAllPostsFromServer();
    }

    // ==================================================================
    // 2. SETUP & AUTHENTICATION
    // ==================================================================

    private boolean checkUserSession() {
        userId = TokenManager.getUserId(this);
        if (TextUtils.isEmpty(userId)) {
            redirectToLogin();
            return false;
        }
        return true;
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupViews() {
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

        binding.swipeRefreshLayout.setOnRefreshListener(this::fetchAllPostsFromServer);
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
        userCache.clear();
        isLoading = true;
        binding.swipeRefreshLayout.setRefreshing(true);

        ApiService apiService = RetrofitClient.getInstance(this).getApiService();
        apiService.getAllPosts(null, null, null, null, null).enqueue(new Callback<PostsResponse>() {
            @Override
            public void onResponse(Call<PostsResponse> call, Response<PostsResponse> response) {
                binding.swipeRefreshLayout.setRefreshing(false);
                isLoading = false;

                if (response.isSuccessful() && response.body() != null) {
                    List<Post> rawPosts = response.body().getPosts();
                    if (rawPosts != null && !rawPosts.isEmpty()) {
                        List<Post> processedList = new ArrayList<>();
                        for (Post p : rawPosts) {
                            if (p.getUserID() != null) {
                                p.setUserName("Loading...");
                                p.setUserAvatar("");
                                processedList.add(p);
                            }
                        }

                        if (!processedList.isEmpty()) {
                            mAllPostsBuffer.clear();
                            mAllPostsBuffer.addAll(processedList);
                            mCurrentDisplayCount = 0;
                            postAdapter.setData(new ArrayList<>());
                            loadMoreFromBuffer();
                        } else {
                            Toast.makeText(HomeActivity.this, "Không có bài viết nào", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else if (response.code() == 401) {
                    TokenManager.clearSession(HomeActivity.this);
                    redirectToLogin();
                } else {
                    Toast.makeText(HomeActivity.this, "Lỗi Server: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PostsResponse> call, Throwable t) {
                isLoading = false;
                binding.swipeRefreshLayout.setRefreshing(false);
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
    // 4. USER INFO
    // ==================================================================

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
                p.setFollowed(user.isMeFollow());
                postAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    // ==================================================================
    // 5. INTERACTION LOGIC (Like, Follow, Save)
    // ==================================================================

    @Override
    public void onLikeClicked(String postID, boolean isLiked) {
        updateLocalState(postID, "LIKE", !isLiked);
        Map<String, String> body = new HashMap<>();
        body.put("postID", postID);
        Call<ResponseBody> call = !isLiked ?
                RetrofitClient.getInstance(this).getApiService().likePost(body) :
                RetrofitClient.getInstance(this).getApiService().unlikePost(body);
        call.enqueue(simpleCallback(postID, "LIKE", isLiked));
    }

    @Override
    public void onBookmarkClicked(String postID, boolean isBookmarked) {
        updateLocalState(postID, "BOOKMARK", !isBookmarked);
        Map<String, String> body = Collections.singletonMap("postID", postID);
        Call<ResponseBody> call = !isBookmarked ?
                RetrofitClient.getInstance(this).getApiService().savePost(body) :
                RetrofitClient.getInstance(this).getApiService().unsavePost(body);
        call.enqueue(simpleCallback(postID, "BOOKMARK", isBookmarked));
    }

    @Override
    public void onFollowClicked(String targetUserID, boolean isFollowed) {
        updateLocalState(targetUserID, "FOLLOW", !isFollowed);
        Call<ResponseBody> call = !isFollowed ?
                RetrofitClient.getInstance(this).getApiService().followUser(targetUserID) :
                RetrofitClient.getInstance(this).getApiService().unfollowUser(targetUserID);
        call.enqueue(simpleCallback(targetUserID, "FOLLOW", isFollowed));
    }

    // Callback rút gọn để revert UI nếu lỗi
    private Callback<ResponseBody> simpleCallback(String id, String type, boolean oldState) {
        return new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) updateLocalState(id, type, oldState);
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                updateLocalState(id, type, oldState);
            }
        };
    }

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
        if ("FOLLOW".equals(type) && userCache.containsKey(id)) {
            UserResponse cachedUser = userCache.get(id);
            if (cachedUser != null) {
                cachedUser.setMeFollow(newState);
                userCache.put(id, cachedUser);
            }
        }
    }

    @Override
    public void onCommentClicked(Post post) { openPostDetail(post); }
    @Override
    public void onPostClicked(Post post) { openPostDetail(post); }
    @Override
    public void onUserClick(String targetUserID) {
        if (targetUserID.equals(userId)) startActivity(new Intent(this, ProfileActivity.class));
        else {
            Intent intent = new Intent(this, OtherUserProfileActivity.class);
            intent.putExtra("USER_ID", targetUserID);
            startActivity(intent);
        }
    }

    // ==================================================================
    // 6. [MỚI] MENU TÙY CHỌN (DELETE / REPORT)
    // ==================================================================

    @Override
    public void onMoreOptionClicked(Post post, View view) {
        // Tạo PopupMenu gắn vào nút 3 chấm
        PopupMenu popupMenu = new PopupMenu(this, view);

        // Kiểm tra quyền sở hữu
        if (userId.equals(post.getUserID())) {
            // Là chủ bài viết -> Thêm option Xóa
            popupMenu.getMenu().add(0, 1, 0, "Xóa bài viết");
        } else {
            // Của người khác -> Thêm option Báo cáo
            popupMenu.getMenu().add(0, 2, 0, "Báo cáo bài viết");
        }

        // Bắt sự kiện chọn item
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: // Xóa
                    showDeleteConfirmationDialog(post);
                    return true;
                case 2: // Báo cáo
                    showReportReasonDialog(post);
                    return true;
                default:
                    return false;
            }
        });

        popupMenu.show();
    }

    // --- LOGIC XÓA BÀI VIẾT ---
    private void showDeleteConfirmationDialog(Post post) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bài viết")
                .setMessage("Bạn có chắc chắn muốn xóa bài viết này không?")
                .setPositiveButton("Xóa", (dialog, which) -> performDeletePost(post))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performDeletePost(Post post) {
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();
        apiService.deletePost(post.get_id()).enqueue(new Callback<PostsResponse>() {
            @Override
            public void onResponse(@NonNull Call<PostsResponse> call, @NonNull Response<PostsResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(HomeActivity.this, "Đã xóa bài viết", Toast.LENGTH_SHORT).show();
                    postAdapter.removePost(post.get_id());
                    removeFromBuffer(post.get_id());
                } else {
                    Toast.makeText(HomeActivity.this, "Lỗi khi xóa: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<PostsResponse> call, @NonNull Throwable t) {
                Toast.makeText(HomeActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- LOGIC BÁO CÁO BÀI VIẾT ---
    private void showReportReasonDialog(Post post) {
        // Danh sách lý do báo cáo
        final String[] reasons = {
                "Nội dung spam",
                "Hình ảnh nhạy cảm/khiêu dâm",
                "Thông tin sai lệch",
                "Quấy rối hoặc bắt nạt",
                "Vi phạm quyền sở hữu trí tuệ",
                "Khác"
        };

        // Biến tạm để lưu lựa chọn (dùng mảng 1 phần tử để final access)
        final int[] selectedPosition = {-1};

        new AlertDialog.Builder(this)
                .setTitle("Tại sao bạn báo cáo bài viết này?")
                .setSingleChoiceItems(reasons, -1, (dialog, which) -> {
                    selectedPosition[0] = which;
                })
                .setPositiveButton("Gửi", (dialog, which) -> {
                    if (selectedPosition[0] >= 0) {
                        performReportPost(post, reasons[selectedPosition[0]]);
                    } else {
                        Toast.makeText(HomeActivity.this, "Vui lòng chọn lý do", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performReportPost(Post post, String reason) {
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        // Tạo body request
        Map<String, Object> body = new HashMap<>();
        body.put("target", post.get_id());
        body.put("reason", reason);
        // Lưu ý: Key "reason" cần khớp với Backend của bạn. Nếu backend cần key khác (vd: "content", "subject"), hãy sửa lại ở đây.

        apiService.reportPost(body).enqueue(new Callback<PostsResponse>() {
            @Override
            public void onResponse(@NonNull Call<PostsResponse> call, @NonNull Response<PostsResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(HomeActivity.this, "Đã gửi báo cáo. Cảm ơn bạn!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(HomeActivity.this, "Gửi báo cáo thất bại: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PostsResponse> call, @NonNull Throwable t) {
                Toast.makeText(HomeActivity.this, "Lỗi kết nối khi báo cáo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeFromBuffer(String postId) {
        for (int i = 0; i < mAllPostsBuffer.size(); i++) {
            if (mAllPostsBuffer.get(i).get_id().equals(postId)) {
                mAllPostsBuffer.remove(i);
                break;
            }
        }
        if (mCurrentDisplayCount > 0) mCurrentDisplayCount--;
    }

    // ==================================================================
    // 7. NAVIGATION
    // ==================================================================

    private void openPostDetail(Post post) {
        Intent intent = new Intent(this, "Recipe".equalsIgnoreCase(post.getType()) ? PostRecipeActivity.class : PostDetailActivity.class);
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
        if (post.getMedia() != null && !post.getMedia().isEmpty()) intent.putExtra("MEDIA_URL", post.getMedia().get(0));
        startActivity(intent);
    }

    private void openProfileScreen() { startActivity(new Intent(this, ProfileActivity.class)); }
    private void openPostComposer() { startActivity(new Intent(this, NewPostActivity.class)); }

    private void setupBottomNavigation() {
        View navView = binding.bottomNavigationBar.getRoot();
        if (navView == null) return;
        navView.findViewById(R.id.nav_home).setOnClickListener(v -> binding.rvFeed.smoothScrollToPosition(0));
        navView.findViewById(R.id.nav_search).setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        navView.findViewById(R.id.nav_add).setOnClickListener(v -> openPostComposer());
        navView.findViewById(R.id.nav_saved).setOnClickListener(v -> startActivity(new Intent(this, SavedPostsActivity.class)));
        navView.findViewById(R.id.nav_profile).setOnClickListener(v -> openProfileScreen());
    }
}