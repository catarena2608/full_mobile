package course.examples.nt118;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import course.examples.nt118.adapter.ProfilePostAdapter;
import course.examples.nt118.databinding.ActivityOtherUserProfileBinding;
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

public class OtherUserProfileActivity extends AppCompatActivity {

    private static final String TAG = OtherUserProfileActivity.class.getSimpleName();
    private ActivityOtherUserProfileBinding binding;
    private ProfilePostAdapter postAdapter;

    private String targetUserId;
    private String currentUserId;
    private boolean isFollowing = false; // Trạng thái follow hiện tại

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtherUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Lấy ID
        currentUserId = TokenManager.getUserId(this);
        targetUserId = getIntent().getStringExtra("USER_ID");

        if (TextUtils.isEmpty(targetUserId)) {
            Toast.makeText(this, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Nếu bấm vào chính mình -> Chuyển sang ProfileActivity (của tôi)
        if (targetUserId.equals(currentUserId)) {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
            return;
        }

        initRecyclerView();
        setupListeners();

        // 2. Load Data
        loadUserProfile();
        loadUserPosts();
    }

    private void initRecyclerView() {
        postAdapter = new ProfilePostAdapter(this, this::openPostDetail);
        binding.rvUserPosts.setLayoutManager(new GridLayoutManager(this, 3));
        binding.rvUserPosts.setAdapter(postAdapter);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnFollow.setOnClickListener(v -> handleFollowAction());

        binding.btnMore.setOnClickListener(v ->
                Toast.makeText(this, "More options clicked", Toast.LENGTH_SHORT).show()
        );
    }

    // ==================================================================
    // 1. LOAD USER INFO
    // ==================================================================

    private void loadUserProfile() {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.getUserById(targetUserId, currentUserId).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Dùng getRealUser để xử lý wrapper
                    UserResponse user = response.body().getRealUser();
                    updateUserInfoUI(user);
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Log.e(TAG, "Load user failed", t);
            }
        });
    }

    private void updateUserInfoUI(UserResponse user) {
        if (user == null) return;

        binding.tvName.setText(user.getName());

        String handle = "@" + user.getName().replaceAll("\\s+", "").toLowerCase();
        binding.tvHandle.setText(handle);

        // Avatar
        String avatarUrl = (user.getAvatar() != null && !user.getAvatar().isEmpty())
                ? user.getAvatar() : "https://i.pravatar.cc/150?u=" + targetUserId;

        Glide.with(this).load(avatarUrl).circleCrop().into(binding.ivAvatar);

        // Cover
        if (user.getCoverImage() != null && !user.getCoverImage().isEmpty()) {
            Glide.with(this).load(user.getCoverImage()).centerCrop().into(binding.ivCover);
        }

        // Stats
        binding.tvPostCount.setText(String.valueOf(user.getNumPosts()));
        binding.tvFollowersCount.setText(String.valueOf(user.getNumFollowed()));
        binding.tvFollowingCount.setText(String.valueOf(user.getNumFollowing()));

        // Link & Bio
        if (user.getLink() != null && !user.getLink().isEmpty()) {
            binding.layoutLinkContainer.setVisibility(View.VISIBLE);
            binding.tvLink.setText(user.getLink().get(0));
        } else {
            binding.layoutLinkContainer.setVisibility(View.GONE);
        }

        // Cập nhật nút Follow dựa trên meFollow từ API
        isFollowing = user.isMeFollow();
        updateFollowButtonState();
    }

    // ==================================================================
    // 2. LOAD POSTS
    // ==================================================================

    private void loadUserPosts() {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.getPostsByUserID(targetUserId).enqueue(new Callback<PostsResponse>() {
            @Override
            public void onResponse(Call<PostsResponse> call, Response<PostsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Post> rawPosts = response.body().getPosts();
                    updatePostList(rawPosts);
                }
            }

            @Override
            public void onFailure(Call<PostsResponse> call, Throwable t) {
                Log.e(TAG, "Load posts failed", t);
            }
        });
    }

    private void updatePostList(List<Post> rawPosts) {
        List<Post> posts = new ArrayList<>();
        if (rawPosts != null) {
            for (Post r : rawPosts) {
                Post p = new Post();
                p.set_id(r.get_id());
                p.setMedia(r.getMedia());
                p.setType(r.getType());
                p.setCaption(r.getCaption());
                p.setUserID(r.getUserID());
                p.setLike(r.getLike());
                p.setComment(r.getComment());
                // Các trường khác nếu cần
                posts.add(p);
            }
        }
        postAdapter.setPosts(posts);
    }

    private void openPostDetail(Post post) {
        Intent intent;
        if ("Recipe".equalsIgnoreCase(post.getType())) {
            intent = new Intent(this, PostRecipeActivity.class);
        } else {
            intent = new Intent(this, PostDetailActivity.class);
        }
        intent.putExtra("POST_ID", post.get_id());
        intent.putExtra("AUTHOR_ID", targetUserId); // Truyền ID tác giả

        // Có thể truyền thêm data để hiện placeholder
        if (post.getMedia() != null && !post.getMedia().isEmpty()) {
            intent.putExtra("MEDIA_URL", post.getMedia().get(0));
        }
        startActivity(intent);
    }

    // ==================================================================
    // 3. FOLLOW LOGIC
    // ==================================================================

    private void handleFollowAction() {
        // Optimistic UI Update (Cập nhật giao diện trước khi gọi API)
        isFollowing = !isFollowing;
        updateFollowButtonState();

        ApiService api = RetrofitClient.getInstance(this).getApiService();
        Call<ResponseBody> call = isFollowing
                ? api.followUser(targetUserId)
                : api.unfollowUser(targetUserId);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    // Revert nếu lỗi
                    isFollowing = !isFollowing;
                    updateFollowButtonState();
                    Toast.makeText(OtherUserProfileActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                } else {
                    // Thành công -> Reload lại profile để cập nhật số lượng follower
                    loadUserProfile();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                isFollowing = !isFollowing;
                updateFollowButtonState();
                Toast.makeText(OtherUserProfileActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFollowButtonState() {
        if (isFollowing) {
            binding.btnFollow.setText("Following");
            binding.btnFollow.setBackgroundResource(R.drawable.bg_input_border); // Nền xám/trắng viền
            binding.btnFollow.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        } else {
            binding.btnFollow.setText("Follow");
            binding.btnFollow.setBackgroundResource(R.drawable.bg_button_orange); // Nền cam
            binding.btnFollow.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }
    }
}