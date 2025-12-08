package course.examples.nt118;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupMenu; // [MỚI]
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
import course.examples.nt118.model.PostsResponse; // [MỚI] Dùng cho response xóa post
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
    private String authorId;

    // State variables
    private boolean isLiked = false;
    private boolean isBookmarked = false;
    private boolean isFollowed = false;

    // Pagination
    private String nextCursor = null;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    // Reply State
    private String replyToCommentId = null;
    @SuppressWarnings("unused")
    private String replyToUserName = null;

    // ==================================================================
    // 1. LIFECYCLE
    // ==================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPostDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUserId = TokenManager.getUserId(this);
        postId = getIntent().getStringExtra("POST_ID");
        // Lấy authorId sớm để xử lý logic nút More
        authorId = getIntent().getStringExtra("AUTHOR_ID");

        if (TextUtils.isEmpty(postId)) {
            Toast.makeText(this, "Bài viết không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupViews();
        setupRecyclerView();

        loadPostData();
        fetchComments();
    }

    // ==================================================================
    // 2. SETUP VIEWS
    // ==================================================================

    private void setupViews() {
        binding.btnBack.setOnClickListener(v -> finish());

        if (binding.btnSendComment != null)
            binding.btnSendComment.setOnClickListener(v -> handleSendComment());

        if (binding.btnCloseReply != null)
            binding.btnCloseReply.setOnClickListener(v -> cancelReplyMode());

        binding.includedPostContent.btnLike.setOnClickListener(v -> handleLikeClick());
        binding.includedPostContent.btnSave.setOnClickListener(v -> handleBookmarkClick());

        if (binding.includedPostContent.btnFollow != null) {
            binding.includedPostContent.btnFollow.setOnClickListener(v -> handleFollowClick());
        }

        View.OnClickListener authorProfileClick = v -> openUserProfile(authorId);
        binding.includedPostContent.ivUserAvatar.setOnClickListener(authorProfileClick);
        binding.includedPostContent.tvUserName.setOnClickListener(authorProfileClick);
        if (binding.includedPostContent.layoutUserInfo != null) {
            binding.includedPostContent.layoutUserInfo.setOnClickListener(authorProfileClick);
        }

        // [MỚI] GẮN SỰ KIỆN CHO NÚT MORE (3 CHẤM)
        if (binding.includedPostContent.btnMore != null) {
            binding.includedPostContent.btnMore.setOnClickListener(v -> showMoreMenu(v));
        }
    }

    private void setupRecyclerView() {
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
                showReportCommentDialog(comment); // Đổi tên hàm để tránh nhầm lẫn
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

    // ==================================================================
    // [MỚI] LOGIC MENU MORE (DELETE / REPORT POST)
    // ==================================================================

    private void showMoreMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);

        // Kiểm tra quyền sở hữu bài viết
        if (authorId != null && authorId.equals(currentUserId)) {
            // Là chủ bài viết -> Thêm option Xóa
            popupMenu.getMenu().add(0, 1, 0, "Xóa bài viết");
        } else {
            // Của người khác -> Thêm option Báo cáo
            popupMenu.getMenu().add(0, 2, 0, "Báo cáo bài viết");
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: // Xóa
                    showDeletePostConfirmDialog();
                    return true;
                case 2: // Báo cáo
                    showReportPostReasonDialog();
                    return true;
            }
            return false;
        });

        popupMenu.show();
    }

    // --- XÓA BÀI VIẾT ---
    private void showDeletePostConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bài viết")
                .setMessage("Bạn có chắc chắn muốn xóa bài viết này không? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> performDeletePost())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performDeletePost() {
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();
        apiService.deletePost(postId).enqueue(new Callback<PostsResponse>() {
            @Override
            public void onResponse(@NonNull Call<PostsResponse> call, @NonNull Response<PostsResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PostDetailActivity.this, "Đã xóa bài viết", Toast.LENGTH_SHORT).show();
                    // Xóa xong thì đóng Activity quay về màn hình trước
                    finish();
                } else {
                    Toast.makeText(PostDetailActivity.this, "Lỗi khi xóa: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PostsResponse> call, @NonNull Throwable t) {
                Toast.makeText(PostDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- BÁO CÁO BÀI VIẾT ---
    private void showReportPostReasonDialog() {
        String[] reasons = {
                "Nội dung spam",
                "Hình ảnh nhạy cảm",
                "Thông tin sai lệch",
                "Quấy rối hoặc bắt nạt",
                "Vi phạm bản quyền"
        };

        new AlertDialog.Builder(this)
                .setTitle("Tại sao bạn báo cáo bài viết này?")
                .setSingleChoiceItems(reasons, -1, (dialog, which) -> {
                    String selectedReason = reasons[which];
                    performReportPost(selectedReason);
                    dialog.dismiss();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performReportPost(String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("target", postId);
        body.put("reason", reason);

        ApiService apiService = RetrofitClient.getInstance(this).getApiService();
        apiService.reportPost(body).enqueue(new Callback<PostsResponse>() {
            @Override
            public void onResponse(@NonNull Call<PostsResponse> call, @NonNull Response<PostsResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PostDetailActivity.this, "Đã gửi báo cáo bài viết.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PostDetailActivity.this, "Gửi báo cáo thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PostsResponse> call, @NonNull Throwable t) {
                Toast.makeText(PostDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // ==================================================================
    // 3. DATA LOADING
    // ==================================================================

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

    // ==================================================================
    // 4. SEND COMMENT & REPLY
    // ==================================================================

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
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PostDetailActivity.this, "Đã gửi bình luận", Toast.LENGTH_SHORT).show();
                    cancelReplyMode();
                    nextCursor = null;
                    isLastPage = false;
                    fetchComments();
                } else {
                    Toast.makeText(PostDetailActivity.this, "Gửi thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Toast.makeText(PostDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==================================================================
    // 5. HELPER METHODS UI/UX
    // ==================================================================

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
        authorId = getIntent().getStringExtra("AUTHOR_ID");

        binding.includedPostContent.tvUserName.setText(userName != null ? userName : "User");
        binding.includedPostContent.tvPostContent.setText(caption != null ? caption : "");
        binding.includedPostContent.tvLikeCount.setText(String.valueOf(likes));
        binding.includedPostContent.tvCommentCount.setText(String.valueOf(comments));

        Glide.with(this).load(userAvatar).circleCrop().into(binding.includedPostContent.ivUserAvatar);

        if (mediaUrl != null && !mediaUrl.isEmpty()) {
            binding.includedPostContent.ivPostImage.setVisibility(View.VISIBLE);
            Glide.with(this).load(mediaUrl).into(binding.includedPostContent.ivPostImage);
        } else {
            binding.includedPostContent.ivPostImage.setVisibility(View.GONE);
        }

        updateLikeButtonUI();
        updateBookmarkButtonUI();
        updateFollowButtonUI();
    }

    // ==================================================================
    // 6. INTERACTIONS (Like, Save, Follow)
    // ==================================================================

    private void handleLikeClick() {
        boolean newState = !isLiked;
        isLiked = newState;
        updateLikeButtonUI();
        Map<String, String> body = new HashMap<>();
        body.put("postID", postId);
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        Call<ResponseBody> call = newState ? api.likePost(body) : api.unlikePost(body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) { if(!response.isSuccessful()) { isLiked = !newState; updateLikeButtonUI(); } }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) { isLiked = !newState; updateLikeButtonUI(); }
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
            @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) { if(!response.isSuccessful()) { isBookmarked = !newState; updateBookmarkButtonUI(); } }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) { isBookmarked = !newState; updateBookmarkButtonUI(); }
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
            @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) { if(!response.isSuccessful()) { isFollowed = !newState; updateFollowButtonUI(); } }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) { isFollowed = !newState; updateFollowButtonUI(); }
        });
    }

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
            binding.includedPostContent.btnFollow.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            binding.includedPostContent.btnFollow.setTextColor(getResources().getColor(android.R.color.black));
        } else {
            binding.includedPostContent.btnFollow.setText("Follow");
            binding.includedPostContent.btnFollow.setBackgroundColor(0xFFFF9800);
            binding.includedPostContent.btnFollow.setTextColor(getResources().getColor(android.R.color.white));
        }
    }

    // ==================================================================
    // 7. REPORT LOGIC FOR COMMENTS (BÁO CÁO BÌNH LUẬN)
    // ==================================================================

    // Đổi tên để phân biệt với báo cáo bài viết
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

        ApiService apiService = RetrofitClient.getInstance(this).getApiService();
        apiService.reportComment(body).enqueue(new Callback<CommentResponse>() {
            @Override
            public void onResponse(@NonNull Call<CommentResponse> call, @NonNull Response<CommentResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PostDetailActivity.this, "Đã gửi báo cáo bình luận.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PostDetailActivity.this, "Gửi báo cáo thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CommentResponse> call, @NonNull Throwable t) {
                Toast.makeText(PostDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (commentAdapter != null) CommentAdapter.clearCache();
        binding = null;
    }
}