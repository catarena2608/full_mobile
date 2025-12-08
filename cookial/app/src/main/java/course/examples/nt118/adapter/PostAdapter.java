package course.examples.nt118.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
import course.examples.nt118.utils.TokenManager;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> mListPosts;
    private final PostInteractionListener listener;
    private final Context context;
    private final String currentUserId;

    public PostAdapter(Context context, PostInteractionListener listener) {
        this.context = context;
        this.listener = listener;
        this.mListPosts = new ArrayList<>();
        this.currentUserId = TokenManager.getUserId(context);
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPostBinding binding = ItemPostBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PostViewHolder(binding, listener, context);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = mListPosts.get(position);
        if (post != null) {
            holder.bind(post);
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
        if (list == null || list.isEmpty()) return;
        int startPos = mListPosts.size();
        this.mListPosts.addAll(list);
        notifyItemRangeInserted(startPos, list.size());
    }

    public void removePost(String postId) {
        if (mListPosts == null || mListPosts.isEmpty()) return;
        for (int i = 0; i < mListPosts.size(); i++) {
            if (mListPosts.get(i).get_id().equals(postId)) {
                mListPosts.remove(i);
                notifyItemRemoved(i);
                notifyItemRangeChanged(i, mListPosts.size());
                break;
            }
        }
    }

    // [ĐÃ BỔ SUNG] Hàm này cần thiết cho HomeActivity
    public List<Post> getCurrentList() {
        return mListPosts;
    }

    // --- HÀM TIỆN ÍCH XỬ LÝ THỜI GIAN ---
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
            if (difference < 0) difference = 0;

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

        public PostViewHolder(ItemPostBinding binding, PostInteractionListener listener, Context context) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            this.context = context;
        }

        public void bind(Post post) {
            // User Info
            binding.tvUserName.setText(post.getUserName() != null ? post.getUserName() : "Người dùng ẩn danh");
            if (post.getUserAvatar() != null && !post.getUserAvatar().isEmpty()) {
                Glide.with(context).load(post.getUserAvatar()).circleCrop().into(binding.ivUserAvatar);
            } else {
                binding.ivUserAvatar.setImageResource(R.drawable.ic_launcher_background);
            }

            View.OnClickListener userClick = v -> {
                if (listener != null) listener.onUserClick(post.getUserID());
            };
            if (binding.layoutUserInfo != null) binding.layoutUserInfo.setOnClickListener(userClick);
            else {
                binding.ivUserAvatar.setOnClickListener(userClick);
                binding.tvUserName.setOnClickListener(userClick);
            }

            // Time
            if (binding.tvTimeAgo != null && post.getCreatedAt() != null) {
                String relativeTime = PostAdapter.getRelativeTimeSpan(post.getCreatedAt());
                binding.tvTimeAgo.setText(relativeTime);
                binding.tvTimeAgo.setVisibility(!relativeTime.isEmpty() ? View.VISIBLE : View.GONE);
            }

            // Media
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

            // Content
            binding.tvPostCaptionUser.setText(post.getUserName() != null ? post.getUserName() : "User");
            binding.tvPostContent.setText(post.getCaption());
            binding.tvLikeCount.setText(String.valueOf(post.getLike()));
            binding.tvCommentCount.setText(String.valueOf(post.getComment()));

            // Tags
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

            // Buttons
            updateLikeView(binding.btnLike, post.isMeLike());
            updateFollowView(binding.btnFollow, post.isFollowed());
            updateBookmarkView(binding.btnSave, post.isBookmarked());

            // Events
            binding.btnLike.setOnClickListener(v -> listener.onLikeClicked(post.get_id(), post.isMeLike()));
            binding.btnFollow.setOnClickListener(v -> listener.onFollowClicked(post.getUserID(), post.isFollowed()));
            binding.btnSave.setOnClickListener(v -> listener.onBookmarkClicked(post.get_id(), post.isBookmarked()));
            binding.btnComment.setOnClickListener(v -> listener.onCommentClicked(post));

            View.OnClickListener detailClick = v -> listener.onPostClicked(post);
            binding.getRoot().setOnClickListener(detailClick);
            binding.ivPostImage.setOnClickListener(detailClick);

            // Nút More (3 chấm)
            binding.getRoot().setOnLongClickListener(null); // Xóa long click cũ
            if (binding.btnMore != null) {
                binding.btnMore.setOnClickListener(v -> {
                    if (listener != null) listener.onMoreOptionClicked(post, v);
                });
            }
        }

        private void updateLikeView(ImageView view, boolean isLiked) {
            if (isLiked) {
                view.setImageResource(R.drawable.ic_heart_filled);
                view.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_light));
            } else {
                view.setImageResource(R.drawable.ic_heart_outline);
                view.clearColorFilter();
            }
        }

        private void updateFollowView(Button view, boolean isFollowed) {
            if (isFollowed) {
                view.setText("Following");
                view.setBackgroundColor(Color.parseColor("#E0E0E0"));
                view.setTextColor(Color.BLACK);
            } else {
                view.setText("Follow");
                view.setBackgroundColor(Color.parseColor("#FF9800"));
                view.setTextColor(Color.WHITE);
            }
        }

        private void updateBookmarkView(ImageView view, boolean isBookmarked) {
            if (isBookmarked) {
                view.setImageResource(R.drawable.ic_bookmark_filled);
                view.setColorFilter(ContextCompat.getColor(context, android.R.color.black));
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
        void onMoreOptionClicked(Post post, View view);
    }
}