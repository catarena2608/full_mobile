package course.examples.nt118;

import android.content.Context;
import android.content.Intent; // Import Intent
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
import course.examples.nt118.databinding.ActivityPostDetailBinding;
import course.examples.nt118.model.Comment;
import course.examples.nt118.model.CommentListResponse;
import course.examples.nt118.model.CommentResponse;
import course.examples.nt118.network.ApiService;
import course.examples.nt118.network.RetrofitClient;
import course.examples.nt118.utils.TokenManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostDetailActivity extends AppCompatActivity {

    private static final String TAG = PostDetailActivity.class.getSimpleName();

    private ActivityPostDetailBinding binding;
    private CommentAdapter commentAdapter;

    private String postId;
    private String currentUserId;

    // Dữ liệu bài viết
    private boolean isLiked = false;
    private boolean isBookmarked = false;
    private boolean isFollowed = false;
    private String authorId; // ID của người đăng bài

    // Pagination State
    private String nextCursor = null;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    // Reply State
    private String replyToCommentId = null;
    private String replyToUserName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "1. onCreate");
        binding = ActivityPostDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUserId = TokenManager.getUserId(this);
        postId = getIntent().getStringExtra("POST_ID");

        if (TextUtils.isEmpty(postId)) {
            finish();
            return;
        }

        setupViews();
        setupRecyclerView();

        loadPostData();
        fetchComments();
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

    private void setupViews() {
        binding.btnBack.setOnClickListener(v -> finish());

        if (binding.btnSendComment != null) {
            binding.btnSendComment.setOnClickListener(v -> handleSendComment());
        }

        if (binding.btnCloseReply != null) {
            binding.btnCloseReply.setOnClickListener(v -> cancelReplyMode());
        }

        // --- GÁN SỰ KIỆN TƯƠNG TÁC ---
        binding.includedPostContent.btnLike.setOnClickListener(v -> handleLikeClick());
        binding.includedPostContent.btnSave.setOnClickListener(v -> handleBookmarkClick());

        if (binding.includedPostContent.btnFollow != null) {
            binding.includedPostContent.btnFollow.setOnClickListener(v -> handleFollowClick());
        }

        // [THÊM MỚI] Click vào Avatar hoặc Tên để mở Profile
        View.OnClickListener profileClickListener = v -> openUserProfile();
        binding.includedPostContent.ivUserAvatar.setOnClickListener(profileClickListener);
        binding.includedPostContent.tvUserName.setOnClickListener(profileClickListener);
        // Nếu layout item_post có layout bao quanh user info (như layout_user_info đã sửa ở bài trước), gán click vào đó luôn
        if (binding.includedPostContent.layoutUserInfo != null) {
            binding.includedPostContent.layoutUserInfo.setOnClickListener(profileClickListener);
        }
    }

    // [THÊM MỚI] Hàm mở Profile
    private void openUserProfile() {
        if (TextUtils.isEmpty(authorId)) return;

        Intent intent = new Intent(this, OtherUserProfileActivity.class);
        intent.putExtra("USER_ID", authorId);
        startActivity(intent);
    }

    private void loadPostData() {
        String userName = getIntent().getStringExtra("USER_NAME");
        String userAvatar = getIntent().getStringExtra("USER_AVATAR");
        String caption = getIntent().getStringExtra("CAPTION");
        String mediaUrl = getIntent().getStringExtra("MEDIA_URL");
        int likes = getIntent().getIntExtra("LIKES", 0);
        int comments = getIntent().getIntExtra("COMMENTS", 0);

        isLiked = getIntent().getBooleanExtra("IS_LIKED", false);
        isBookmarked = getIntent().getBooleanExtra("IS_BOOKMARKED", false);
        isFollowed = getIntent().getBooleanExtra("IS_FOLLOWED", false);

        // [QUAN TRỌNG] Lấy Author ID từ Intent để dùng cho nút Follow và mở Profile
        authorId = getIntent().getStringExtra("AUTHOR_ID");

        // Update UI
        binding.includedPostContent.tvUserName.setText(userName != null ? userName : "Đang tải...");
        binding.includedPostContent.tvPostContent.setText(caption != null ? caption : "");
        binding.includedPostContent.tvLikeCount.setText(likes + ""); // Sửa lại setText String
        binding.includedPostContent.tvCommentCount.setText(comments + "");

        Glide.with(this)
                .load(userAvatar)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(binding.includedPostContent.ivUserAvatar);

        binding.includedPostContent.ivPostImage.setVisibility(View.VISIBLE);
        if (mediaUrl != null && !mediaUrl.isEmpty()) {
            Glide.with(this).load(mediaUrl).into(binding.includedPostContent.ivPostImage);
        } else {
            binding.includedPostContent.ivPostImage.setImageResource(R.drawable.ic_launcher_background);
        }

        updateLikeButtonUI();
        updateBookmarkButtonUI();
        updateFollowButtonUI();
    }

    // --- UI UPDATE METHODS ---

    private void updateLikeButtonUI() {
        if (isLiked) {
            binding.includedPostContent.btnLike.setImageResource(R.drawable.ic_heart_filled);
            binding.includedPostContent.btnLike.setColorFilter(getResources().getColor(android.R.color.holo_red_light));
        } else {
            binding.includedPostContent.btnLike.setImageResource(R.drawable.ic_heart_outline);
            binding.includedPostContent.btnLike.clearColorFilter();
        }
    }

    private void updateBookmarkButtonUI() {
        if (isBookmarked) {
            binding.includedPostContent.btnSave.setImageResource(R.drawable.ic_bookmark_filled);
            binding.includedPostContent.btnSave.setColorFilter(getResources().getColor(android.R.color.black));
        } else {
            binding.includedPostContent.btnSave.setImageResource(R.drawable.ic_bookmark_outline);
            binding.includedPostContent.btnSave.clearColorFilter();
        }
    }

    private void updateFollowButtonUI() {
        if (binding.includedPostContent.btnFollow == null) return;

        if (isFollowed) {
            binding.includedPostContent.btnFollow.setText("Following");
            binding.includedPostContent.btnFollow.setBackgroundColor(getResources().getColor(android.R.color.darker_gray)); // Màu xám
            binding.includedPostContent.btnFollow.setTextColor(getResources().getColor(android.R.color.black));
        } else {
            binding.includedPostContent.btnFollow.setText("Follow");
            binding.includedPostContent.btnFollow.setBackgroundColor(0xFFFF9800); // Màu cam (hoặc lấy từ R.color)
            binding.includedPostContent.btnFollow.setTextColor(getResources().getColor(android.R.color.white));
        }
    }

    // --- API ACTION HANDLERS ---
    // (Giữ nguyên code logic Like/Save/Follow cũ)

    private void handleLikeClick() {
        boolean newState = !isLiked;
        isLiked = newState;
        updateLikeButtonUI();

        Map<String, String> body = new HashMap<>();
        body.put("postID", postId);

        ApiService api = RetrofitClient.getInstance(this).getApiService();
        Call<ResponseBody> call = newState ? api.likePost(body) : api.unlikePost(body);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    isLiked = !newState;
                    updateLikeButtonUI();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                isLiked = !newState;
                updateLikeButtonUI();
            }
        });
    }

    private void handleBookmarkClick() {
        boolean newState = !isBookmarked;
        isBookmarked = newState;
        updateBookmarkButtonUI();

        Map<String, String> body = Collections.singletonMap("postID", postId);
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        Call<ResponseBody> call = newState ? api.savePost(body) : api.unsavePost(body);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    isBookmarked = !newState;
                    updateBookmarkButtonUI();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                isBookmarked = !newState;
                updateBookmarkButtonUI();
            }
        });
    }

    private void handleFollowClick() {
        if (TextUtils.isEmpty(authorId) || authorId.equals(currentUserId)) return;

        boolean newState = !isFollowed;
        isFollowed = newState;
        updateFollowButtonUI();

        ApiService api = RetrofitClient.getInstance(this).getApiService();
        Call<ResponseBody> call = newState ? api.followUser(authorId) : api.unfollowUser(authorId);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    isFollowed = !newState;
                    updateFollowButtonUI();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                isFollowed = !newState;
                updateFollowButtonUI();
            }
        });
    }

    // --- RECYCLER VIEW & COMMENTS ---

    private void setupRecyclerView() {
        commentAdapter = new CommentAdapter(this, this::activateReplyMode);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.rvComments.setLayoutManager(layoutManager);
        binding.rvComments.setAdapter(commentAdapter);

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

        ApiService apiService = RetrofitClient.getInstance(this).getApiService();
        apiService.getComments(postId, nextCursor).enqueue(new Callback<CommentListResponse>() {
            @Override
            public void onResponse(@NonNull Call<CommentListResponse> call, @NonNull Response<CommentListResponse> response) {
                isLoading = false;
                if (response.isSuccessful() && response.body() != null) {
                    List<CommentResponse> rawComments = response.body().getComments();
                    if (rawComments != null && !rawComments.isEmpty()) {
                        List<Comment> mappedComments = mapToComment(rawComments);
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
            }
        });
    }

    private void handleSendComment() {
        String content = binding.etCommentInput.getText().toString().trim();
        if (TextUtils.isEmpty(content)) return;

        binding.etCommentInput.setText("");
        hideKeyboard();

        Map<String, Object> body = new HashMap<>();
        body.put("postID", postId);
        body.put("content", content);
        if (replyToCommentId != null) {
            body.put("reply", replyToCommentId);
        }

        ApiService apiService = RetrofitClient.getInstance(this).getApiService();
        apiService.addComment(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    cancelReplyMode();
                    nextCursor = null;
                    isLastPage = false;
                    fetchComments();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) { }
        });
    }

    // ... (Các hàm helper: activateReplyMode, cancelReplyMode, hideKeyboard, showKeyboard, mapToComment giữ nguyên từ code cũ) ...
    // Copy lại các hàm helper đó vào đây
    private void activateReplyMode(Comment comment) {
        replyToCommentId = comment.getId();
        replyToUserName = comment.getUserName();
        if (binding.layoutReplying != null) {
            binding.layoutReplying.setVisibility(View.VISIBLE);
            binding.tvReplyingTo.setText("Đang trả lời " + replyToUserName);
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
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(binding.etCommentInput, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private List<Comment> mapToComment(List<CommentResponse> responses) {
        Map<String, Comment> idToComment = new HashMap<>();
        List<Comment> topComments = new ArrayList<>();

        for (CommentResponse cr : responses) {
            Comment c = convertSingleResponse(cr);
            idToComment.put(c.getId(), c);
        }
        for (CommentResponse cr : responses) {
            Comment parent = idToComment.get(cr.get_id());
            if (parent != null && cr.getReplies() != null) {
                for (CommentResponse r1 : cr.getReplies()) {
                    Comment reply1 = convertSingleResponse(r1);
                    parent.getReplies().add(reply1);
                    if (r1.getReplies() != null) {
                        for (CommentResponse r2 : r1.getReplies()) {
                            Comment reply2 = convertSingleResponse(r2);
                            reply1.getReplies().add(reply2);
                        }
                    }
                }
            }
        }
        for (CommentResponse cr : responses) {
            if (cr.getDepth() == 0) {
                Comment c = idToComment.get(cr.get_id());
                if (c != null) topComments.add(c);
            }
        }
        return topComments;
    }

    private Comment convertSingleResponse(CommentResponse res) {
        Comment c = new Comment();
        c.setId(res.get_id());
        c.setPostId(res.getPostId());
        c.setUserId(res.getUserId());
        c.setUserName(res.getUserName());
        c.setUserAvatar(res.getUserAvatar());
        c.setContent(res.getContent());
        c.setCreatedAt(res.getCreatedAt());
        c.setReplies(new ArrayList<>());
        return c;
    }
}