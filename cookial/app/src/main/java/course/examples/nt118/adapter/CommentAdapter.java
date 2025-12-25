package course.examples.nt118.adapter;

import android.app.Activity;
import android.content.Context;
import android.text.format.DateUtils; // [MỚI] Import thư viện xử lý thời gian
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Date;
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

    private static final Map<String, UserResponse> userCache = new HashMap<>();
    private final String currentUserId;

    public interface CommentInteractionListener {
        void onReplyClick(Comment comment);
        void onUserClick(String userId);
        void onReportClick(Comment comment);
    }

    public CommentAdapter(Context context, String currentUserId, CommentInteractionListener interactionListener) {
        this.context = context;
        this.currentUserId = currentUserId;
        this.interactionListener = interactionListener;
    }

    // ... (Các hàm submitList, addData, clearData giữ nguyên) ...
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

    public static void clearCache() {
        if (userCache != null) {
            userCache.clear();
        }
    }

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
        holder.bind(comment);
        setIndentation(holder.itemView, comment.getDepth());
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    private void setIndentation(View view, int depth) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        int leftMargin = dpToPx(context, depth * 32);
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
        private String currentUserIdBinding;

        public CommentViewHolder(@NonNull ItemCommentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Comment comment) {
            // 1. Set nội dung
            binding.tvCmtContent.setText(comment.getContent());

            // 2. [MỚI] Set thời gian (tính từ createdAt)
            // Giả sử trong XML id của TextView thời gian là tvCmtTime
            if (binding.tvCmtTime != null) {
                binding.tvCmtTime.setText(getTimeAgo(comment.getCreatedAt()));
            }

            // 3. Lưu UserID
            String userId = comment.getUserId();
            this.currentUserIdBinding = userId;

            // 4. Xử lý User Info
            if (userId != null && !userId.isEmpty()) {
                if (userCache.containsKey(userId)) {
                    updateUserUI(userCache.get(userId));
                } else {
                    binding.tvCmtName.setText("Loading...");
                    binding.ivCmtAvatar.setImageResource(R.drawable.ic_default_avatar);
                    fetchUserInfo(userId);
                }
            } else {
                binding.tvCmtName.setText("Unknown User");
                binding.ivCmtAvatar.setImageResource(R.drawable.ic_default_avatar);
            }

            setupClickListeners(comment, userId);
        }

        // [MỚI] Hàm tính toán thời gian (VD: 5 phút trước, 1 giờ trước)
        private String getTimeAgo(Date date) {
            if (date == null) return "Vừa xong";

            long time = date.getTime();
            long now = System.currentTimeMillis();

            // Sử dụng DateUtils để tự động format
            CharSequence ago = DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS);
            return ago.toString();
        }

        private void setupClickListeners(Comment comment, String userId) {
            if (binding.tvReply != null) {
                binding.tvReply.setOnClickListener(v -> {
                    if (interactionListener != null) interactionListener.onReplyClick(comment);
                });
            }
            View.OnClickListener profileClick = v -> {
                if (interactionListener != null && userId != null) {
                    interactionListener.onUserClick(userId);
                }
            };
            binding.ivCmtAvatar.setOnClickListener(profileClick);
            binding.tvCmtName.setOnClickListener(profileClick);

            if (binding.tvReport != null) {
                binding.tvReport.setOnClickListener(v -> {
                    if (interactionListener != null) interactionListener.onReportClick(comment);
                });
            }
        }

        // ... (Các hàm fetchUserInfo và updateUserUI giữ nguyên như cũ) ...

        private void updateUserUI(UserResponse user) {
            if (user == null || !isValidContextForGlide(context)) return;
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
            apiService.getUserById(userId, currentUserId).enqueue(new Callback<UserResponse>() {
                @Override
                public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        UserResponse realUser = response.body().getRealUser();
                        if (realUser != null) {
                            userCache.put(userId, realUser);
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

    private boolean isValidContextForGlide(Context context) {
        if (context == null) return false;
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            return !activity.isDestroyed() && !activity.isFinishing();
        }
        return true;
    }
}