package course.examples.nt118.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import course.examples.nt118.R;
import course.examples.nt118.databinding.ItemPostBinding;
import course.examples.nt118.model.Post;
import course.examples.nt118.utils.TokenManager; // Cần import để lấy ID người dùng hiện tại

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private static final String TAG = "PostAdapter";
    private List<Post> mListPosts;
    private final PostInteractionListener listener;
    private final Context context;
    private final String currentUserId; // [MỚI] Lưu ID người dùng hiện tại

    public PostAdapter(Context context, PostInteractionListener listener) {
        this.context = context;
        this.listener = listener;
        this.mListPosts = new ArrayList<>();
        // [MỚI] Lấy ID người dùng để check quyền xóa
        this.currentUserId = TokenManager.getUserId(context);
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPostBinding binding = ItemPostBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PostViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = mListPosts.get(position);
        if (post != null) {
            // Truyền thêm currentUserId vào hàm bind
            holder.bind(post, currentUserId);
        }
    }

    @Override
    public int getItemCount() {
        return mListPosts != null ? mListPosts.size() : 0;
    }

    // --- CÁC HÀM CẬP NHẬT DỮ LIỆU ---

    public void setData(List<Post> list) {
        this.mListPosts = list;
        notifyDataSetChanged();
    }

    public void addData(List<Post> list) {
        int startPos = mListPosts.size();
        this.mListPosts.addAll(list);
        notifyItemRangeInserted(startPos, list.size());
    }

    public List<Post> getCurrentList() {
        return mListPosts;
    }

    // [MỚI] Hàm hỗ trợ xóa item khỏi danh sách hiển thị
    public void removePost(String postId) {
        if (mListPosts == null) return;
        for (int i = 0; i < mListPosts.size(); i++) {
            if (mListPosts.get(i).get_id().equals(postId)) {
                mListPosts.remove(i);
                notifyItemRemoved(i);
                notifyItemRangeChanged(i, mListPosts.size());
                break;
            }
        }
    }

    // --- HÀM TIỆN ÍCH XỬ LÝ THỜI GIAN (Giữ nguyên logic của bạn) ---
    public static String getRelativeTimeSpan(String isoDateTime) {
        if (isoDateTime == null || isoDateTime.isEmpty()) return "";
        SimpleDateFormat isoFormatter;
        if (isoDateTime.contains(".")) {
            isoFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        } else {
            isoFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        }
        isoFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            Date pastDate = isoFormatter.parse(isoDateTime);
            long now = System.currentTimeMillis();
            long difference = now - pastDate.getTime();

            if (difference < 0) {
                if (difference > -60000) return "Vừa xong";
                SimpleDateFormat outputFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                return outputFormatter.format(pastDate);
            }

            long seconds = TimeUnit.MILLISECONDS.toSeconds(difference);
            if (seconds < 60) return "Vừa xong";
            long minutes = TimeUnit.MILLISECONDS.toMinutes(difference);
            if (minutes < 60) return minutes + " phút trước";
            long hours = TimeUnit.MILLISECONDS.toHours(difference);
            if (hours < 24) return hours + " giờ trước";
            long days = TimeUnit.MILLISECONDS.toDays(difference);
            if (days < 7) return days + " ngày trước";
            long weeks = days / 7;
            if (weeks < 4) return weeks + " tuần trước";

            SimpleDateFormat outputFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return outputFormatter.format(pastDate);

        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

    // --- VIEWHOLDER ---

    public static class PostViewHolder extends RecyclerView.ViewHolder {

        private final ItemPostBinding binding;
        private final PostInteractionListener listener;
        private final Context context;

        public PostViewHolder(ItemPostBinding binding, PostInteractionListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            this.context = binding.getRoot().getContext();
        }

        // [SỬA] Thêm tham số currentUserId
        public void bind(Post post, String currentUserId) {
            // 1. User Info
            binding.tvUserName.setText(post.getUserName() != null ? post.getUserName() : "Người dùng ẩn danh");

            if (post.getUserAvatar() != null && !post.getUserAvatar().isEmpty()) {
                Glide.with(context).load(post.getUserAvatar()).circleCrop().into(binding.ivUserAvatar);
            } else {
                binding.ivUserAvatar.setImageResource(R.drawable.ic_launcher_background);
            }

            // Click User Info -> Profile
            View.OnClickListener userClick = v -> {
                if (listener != null) listener.onUserClick(post.getUserID());
            };
            if (binding.layoutUserInfo != null) {
                binding.layoutUserInfo.setOnClickListener(userClick);
            } else {
                binding.ivUserAvatar.setOnClickListener(userClick);
                binding.tvUserName.setOnClickListener(userClick);
            }

            // 2. Thời gian
            if (binding.tvTimeAgo != null && post.getCreatedAt() != null) {
                String relativeTime = PostAdapter.getRelativeTimeSpan(post.getCreatedAt());
                binding.tvTimeAgo.setText(relativeTime);
                binding.tvTimeAgo.setVisibility(!relativeTime.isEmpty() ? View.VISIBLE : View.GONE);
            }

            // 3. Media
            binding.ivPostImage.setVisibility(View.VISIBLE);
            if (post.getMedia() != null && !post.getMedia().isEmpty()) {
                Glide.with(context)
                        .load(post.getMedia().get(0))
                        .placeholder(android.R.color.darker_gray)
                        .error(R.drawable.ic_launcher_background)
                        .into(binding.ivPostImage);
            } else {
                binding.ivPostImage.setImageResource(R.drawable.ic_launcher_background);
            }

            // 4. Content & Stats
            binding.tvPostCaptionUser.setText(post.getUserName() != null ? post.getUserName() : "User");
            binding.tvPostContent.setText(post.getCaption());
            binding.tvLikeCount.setText(String.valueOf(post.getLike()));
            binding.tvCommentCount.setText(String.valueOf(post.getComment()));

            // 5. Tags
            String postType = post.getType();
            if ("Recipe".equalsIgnoreCase(postType)) {
                binding.tvPostTag.setText("Công thức");
                binding.tvPostTag.setVisibility(View.VISIBLE);
            } else if ("Moment".equalsIgnoreCase(postType)) {
                binding.tvPostTag.setText("Khoảnh khắc");
                binding.tvPostTag.setVisibility(View.VISIBLE);
            } else {
                binding.tvPostTag.setVisibility(View.GONE);
            }

            // 6. Button States
            updateLikeView(binding.btnLike, post.isMeLike());
            updateFollowView(binding.btnFollow, post.isFollowed());
            updateBookmarkView(binding.btnSave, post.isBookmarked());

            // 7. Click Events (Buttons)
            binding.btnLike.setOnClickListener(v -> listener.onLikeClicked(post.get_id(), post.isMeLike()));
            binding.btnFollow.setOnClickListener(v -> listener.onFollowClicked(post.getUserID(), post.isFollowed()));
            binding.btnSave.setOnClickListener(v -> listener.onBookmarkClicked(post.get_id(), post.isBookmarked()));
            binding.btnComment.setOnClickListener(v -> listener.onCommentClicked(post));

            // Click vào ảnh/body -> Detail
            View.OnClickListener detailClick = v -> listener.onPostClicked(post);
            binding.getRoot().setOnClickListener(detailClick);
            binding.ivPostImage.setOnClickListener(detailClick);

            // ============================================================
            // 8. [MỚI] LONG CLICK ĐỂ XÓA (Chỉ cho phép nếu là chủ bài viết)
            // ============================================================
            if (post.getUserID() != null && post.getUserID().equals(currentUserId)) {
                binding.getRoot().setOnLongClickListener(v -> {
                    if (listener != null) {
                        listener.onPostLongClicked(post, v);
                        return true; // Đã xử lý sự kiện
                    }
                    return false;
                });
            } else {
                // Nếu không phải chủ, hủy sự kiện long click (tránh tái sử dụng viewholder bị lỗi)
                binding.getRoot().setOnLongClickListener(null);
            }
        }

        private void updateLikeView(ImageView view, boolean isLiked) {
            if (isLiked) {
                view.setImageResource(R.drawable.ic_heart_filled);
                view.setColorFilter(context.getResources().getColor(android.R.color.holo_red_light));
            } else {
                view.setImageResource(R.drawable.ic_heart_outline);
                view.clearColorFilter();
            }
        }

        private void updateFollowView(Button view, boolean isFollowed) {
            if (isFollowed) {
                view.setText("Following");
                view.setBackgroundColor(0xFFE0E0E0);
                view.setTextColor(0xFF000000);
            } else {
                view.setText("Follow");
                view.setBackgroundColor(0xFFFF9800);
                view.setTextColor(0xFFFFFFFF);
            }
        }

        private void updateBookmarkView(ImageView view, boolean isBookmarked) {
            if (isBookmarked) {
                view.setImageResource(R.drawable.ic_bookmark_filled);
                view.setColorFilter(context.getResources().getColor(android.R.color.black));
            } else {
                view.setImageResource(R.drawable.ic_bookmark_outline);
                view.clearColorFilter();
            }
        }
    }

    // --- INTERFACE ---
    public interface PostInteractionListener {
        void onLikeClicked(String postID, boolean isLiked);
        void onFollowClicked(String userID, boolean isFollowed);
        void onBookmarkClicked(String postID, boolean isBookmarked);
        void onCommentClicked(Post post);
        void onPostClicked(Post post);
        void onUserClick(String userID);

        // [MỚI] Sự kiện nhấn giữ để xóa
        void onPostLongClicked(Post post, View view);
    }
}