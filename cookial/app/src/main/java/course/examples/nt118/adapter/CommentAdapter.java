package course.examples.nt118.adapter;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import course.examples.nt118.R;
import course.examples.nt118.databinding.ItemCommentBinding;
import course.examples.nt118.model.Comment;
import course.examples.nt118.model.UserResponse;
import course.examples.nt118.network.ApiService;
import course.examples.nt118.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private static final String TAG = "CommentAdapter";
    private final Context context;
    private final List<Comment> commentList = new ArrayList<>();
    private final CommentInteractionListener interactionListener;

    // Cache user info: Static để giữ lại thông tin User giữa các lần xoay màn hình/reload nhẹ
    private static final Map<String, UserResponse> userCache = new HashMap<>();

    // ID người dùng hiện tại (để check follow/block khi gọi API lấy info người khác)
    private final String currentUserId;

    public interface CommentInteractionListener {
        void onReplyClick(Comment comment);          // Click nút Phản hồi
        void onUserClick(String userId);             // Click Avatar/Tên -> Xem profile
        void onReportClick(Comment comment);         // Click nút Báo cáo
    }

    public CommentAdapter(Context context, String currentUserId, CommentInteractionListener interactionListener) {
        this.context = context;
        this.currentUserId = currentUserId;
        this.interactionListener = interactionListener;
    }

    // ==========================================
    // DATA MANIPULATION
    // ==========================================

    public void submitList(List<Comment> comments) {
        commentList.clear();
        if (comments != null) {
            commentList.addAll(comments);
        }
        notifyDataSetChanged();
    }

    public void addData(List<Comment> newComments) {
        if (newComments != null && !newComments.isEmpty()) {
            int startPos = commentList.size();
            commentList.addAll(newComments);
            notifyItemRangeInserted(startPos, newComments.size());
        }
    }

    public void clearData() {
        commentList.clear();
        notifyDataSetChanged();
    }

    // [QUAN TRỌNG] Hàm này để Activity gọi khi onDestroy, tránh leak memory hoặc dữ liệu cũ
    public static void clearCache() {
        if (userCache != null) {
            userCache.clear();
        }
    }

    // ==========================================
    // VIEW HOLDER CREATION & BINDING
    // ==========================================

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCommentBinding binding = ItemCommentBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new CommentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        // 1. Bind dữ liệu nội dung
        holder.bind(comment);

        // 2. Xử lý thụt đầu dòng cho Reply (UI Hierarchy)
        setIndentation(holder.itemView, comment.getDepth());
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    /**
     * Xử lý thụt đầu dòng dựa trên độ sâu (depth) của comment
     */
    private void setIndentation(View view, int depth) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        // Mỗi cấp thụt vào 32dp
        int leftMargin = dpToPx(context, depth * 32);

        // Chỉ set lại nếu margin thay đổi để tránh requestLayout liên tục
        if (params.leftMargin != leftMargin) {
            params.setMargins(leftMargin, params.topMargin, params.rightMargin, params.bottomMargin);
            view.setLayoutParams(params);
        }
    }

    private int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    // ==========================================
    // VIEW HOLDER CLASS
    // ==========================================

    class CommentViewHolder extends RecyclerView.ViewHolder {
        private final ItemCommentBinding binding;

        // Giữ ID của user đang được bind vào ViewHolder này
        private String currentUserIdBinding;

        public CommentViewHolder(@NonNull ItemCommentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Comment comment) {
            // 1. Set nội dung
            binding.tvCmtContent.setText(comment.getContent());

            // 2. Lưu UserID của comment này
            String userId = comment.getUserId();
            this.currentUserIdBinding = userId;

            // 3. Xử lý User Info (Avatar & Name)
            if (userId != null && !userId.isEmpty()) {
                if (userCache.containsKey(userId)) {
                    updateUserUI(userCache.get(userId));
                } else {
                    // Chưa có trong cache -> Hiện loading -> Gọi API
                    binding.tvCmtName.setText("Loading...");
                    binding.ivCmtAvatar.setImageResource(R.drawable.ic_default_avatar);
                    fetchUserInfo(userId);
                }
            } else {
                binding.tvCmtName.setText("Unknown User");
                binding.ivCmtAvatar.setImageResource(R.drawable.ic_default_avatar);
            }

            // 4. Gán sự kiện Click
            setupClickListeners(comment, userId);
        }

        private void setupClickListeners(Comment comment, String userId) {
            // Sự kiện Reply
            if (binding.tvReply != null) {
                binding.tvReply.setOnClickListener(v -> {
                    if (interactionListener != null) interactionListener.onReplyClick(comment);
                });
            }

            // Sự kiện Click vào Avatar/Tên -> Xem Profile
            View.OnClickListener profileClick = v -> {
                if (interactionListener != null && userId != null) {
                    interactionListener.onUserClick(userId);
                }
            };
            binding.ivCmtAvatar.setOnClickListener(profileClick);
            binding.tvCmtName.setOnClickListener(profileClick);

            // Sự kiện Click Báo cáo (Report)
            if (binding.tvReport != null) {
                binding.tvReport.setOnClickListener(v -> {
                    if (interactionListener != null) {
                        interactionListener.onReportClick(comment);
                    }
                });
            }
        }

        private void updateUserUI(UserResponse user) {
            if (user == null) return;

            // Check context để tránh crash Glide
            if (!isValidContextForGlide(context)) return;

            binding.tvCmtName.setText(user.getName());

            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                Glide.with(context)
                        .load(user.getAvatar())
                        .placeholder(R.drawable.ic_default_avatar)
                        .error(R.drawable.ic_default_avatar)
                        .circleCrop()
                        .into(binding.ivCmtAvatar);
            } else {
                binding.ivCmtAvatar.setImageResource(R.drawable.ic_default_avatar);
            }
        }

        private void fetchUserInfo(String userId) {
            ApiService apiService = RetrofitClient.getInstance(context).getApiService();

            // Gọi API lấy thông tin user (kèm currentUserId để check follow)
            apiService.getUserById(userId, currentUserId).enqueue(new Callback<UserResponse>() {
                @Override
                public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        // Tùy vào JSON trả về, bạn lấy getRealUser() hoặc lấy trực tiếp body()
                        UserResponse realUser = response.body().getRealUser();
                        // Nếu server trả về phẳng: UserResponse realUser = response.body();

                        if (realUser != null) {
                            // Lưu vào cache
                            userCache.put(userId, realUser);

                            // Update UI nếu ViewHolder vẫn đang giữ userId này
                            if (userId.equals(currentUserIdBinding)) {
                                updateUserUI(realUser);
                            }
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "Failed to load user info: " + t.getMessage());
                }
            });
        }
    }

    // Helper check context cho Glide
    private boolean isValidContextForGlide(Context context) {
        if (context == null) return false;
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            return !activity.isDestroyed() && !activity.isFinishing();
        }
        return true;
    }
}