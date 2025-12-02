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

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private static final String TAG = "PostAdapter"; // Thêm TAG để dễ lọc log
    private List<Post> mListPosts;
    private final PostInteractionListener listener;
    private final Context context;

    public PostAdapter(Context context, PostInteractionListener listener) {
        this.context = context;
        this.listener = listener;
        this.mListPosts = new ArrayList<>();
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
        int startPos = mListPosts.size();
        this.mListPosts.addAll(list);
        notifyItemRangeInserted(startPos, list.size());
    }

    public List<Post> getCurrentList() {
        return mListPosts;
    }

    // --- HÀM TIỆN ÍCH XỬ LÝ THỜI GIAN TƯƠNG ĐỐI (RELATIVE TIME) ---

    /**
     * Chuyển đổi chuỗi thời gian ISO 8601 (UTC) thành chuỗi thời gian tương đối (ví dụ: "5 phút trước").
     * @param isoDateTime Chuỗi thời gian từ API (ví dụ: "2025-12-01T11:44:23.473Z")
     * @return Chuỗi thời gian tương đối
     */
    public static String getRelativeTimeSpan(String isoDateTime) {
        if (isoDateTime == null || isoDateTime.isEmpty()) return "";

        SimpleDateFormat isoFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        isoFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            Date pastDate = isoFormatter.parse(isoDateTime);
            long now = System.currentTimeMillis();
            long difference = now - pastDate.getTime();

            if (difference < 0) {
                SimpleDateFormat outputFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                outputFormatter.setTimeZone(TimeZone.getDefault());
                Log.e(TAG, "Post time is in the future. Difference: " + difference);
                return outputFormatter.format(pastDate);
            }

            long seconds = TimeUnit.MILLISECONDS.toSeconds(difference);
            if (seconds < 60) {
                return "Vừa xong";
            }

            long minutes = TimeUnit.MILLISECONDS.toMinutes(difference);
            if (minutes < 60) {
                return minutes + " phút trước";
            }

            long hours = TimeUnit.MILLISECONDS.toHours(difference);
            if (hours < 24) {
                return hours + " giờ trước";
            }

            long days = TimeUnit.MILLISECONDS.toDays(difference);
            if (days < 7) {
                return days + " ngày trước";
            }

            long weeks = days / 7;
            if (weeks < 4) {
                return weeks + " tuần trước";
            }

            // Nếu quá 4 tuần, hiển thị ngày tháng cụ thể
            SimpleDateFormat outputFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            outputFormatter.setTimeZone(TimeZone.getDefault());

            return outputFormatter.format(pastDate);

        } catch (ParseException e) {
            Log.e(TAG, "LỖI PARSE DATE: Chuỗi: " + isoDateTime + ", Lỗi: " + e.getMessage());
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

        public void bind(Post post) {
            // 1. User Info
            binding.tvUserName.setText(
                    post.getUserName() != null ? post.getUserName() : "Người dùng ẩn danh"
            );

            if (post.getUserAvatar() != null && !post.getUserAvatar().isEmpty()) {
                Glide.with(context).load(post.getUserAvatar()).circleCrop().into(binding.ivUserAvatar);
            } else {
                binding.ivUserAvatar.setImageResource(R.drawable.ic_launcher_background);
            }

            // Bắt sự kiện click vào User Info
            if (binding.layoutUserInfo != null) {
                binding.layoutUserInfo.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onUserClick(post.getUserID());
                    }
                });
            } else {
                binding.ivUserAvatar.setOnClickListener(v -> listener.onUserClick(post.getUserID()));
                binding.tvUserName.setOnClickListener(v -> listener.onUserClick(post.getUserID()));
            }

            // 2. GIỮ NGUYÊN tvUserSubtitle ("Cooking Studio")
            // Không set gì ở đây để không ghi đè giá trị mặc định của nó.

            // *******************************************************************
            // 3. Thời gian đăng bài: CHỈ SỬA tvTimeAgo (vị trí cuối bài)
            // *******************************************************************
            if (binding.tvTimeAgo != null && post.getCreatedAt() != null) {

                // *** LOG TEST QUAN TRỌNG: Kiểm tra xem hàm có được gọi không ***
                Log.d(TAG, "Đang xử lý Post ID: " + post.get_id() + ", CreatedAt: " + post.getCreatedAt());

                String relativeTime = PostAdapter.getRelativeTimeSpan(post.getCreatedAt());

                if (!relativeTime.isEmpty()) {
                    binding.tvTimeAgo.setText(relativeTime);
                    binding.tvTimeAgo.setVisibility(View.VISIBLE);
                } else {
                    binding.tvTimeAgo.setText("");
                    binding.tvTimeAgo.setVisibility(View.GONE);
                }
            } else if (binding.tvTimeAgo != null) {
                // Đảm bảo ẩn đi nếu không có dữ liệu
                binding.tvTimeAgo.setVisibility(View.GONE);
            }

            // 4. Media (Ảnh bài viết)
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

            // 5. Content
            binding.tvPostCaptionUser.setText(post.getUserName() != null ? post.getUserName() : "Người dùng ẩn danh");
            binding.tvPostContent.setText(post.getCaption());
            binding.tvLikeCount.setText(String.valueOf(post.getLike()));
            binding.tvCommentCount.setText(String.valueOf(post.getComment()));

            // 6. Tag
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

            // 7. Button States
            updateLikeView(binding.btnLike, post.isMeLike());
            updateFollowView(binding.btnFollow, post.isFollowed());
            updateBookmarkView(binding.btnSave, post.isBookmarked());

            // 8. Events
            binding.btnLike.setOnClickListener(v -> listener.onLikeClicked(post.get_id(), post.isMeLike()));

            // Click Follow
            binding.btnFollow.setOnClickListener(v -> listener.onFollowClicked(post.getUserID(), post.isFollowed()));

            binding.btnSave.setOnClickListener(v -> listener.onBookmarkClicked(post.get_id(), post.isBookmarked()));
            binding.btnComment.setOnClickListener(v -> listener.onCommentClicked(post));

            View.OnClickListener detailClick = v -> listener.onPostClicked(post);
            binding.getRoot().setOnClickListener(detailClick);
            binding.ivPostImage.setOnClickListener(detailClick);
        }

        // --- Helper Methods ---

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
                // Nền xám, chữ đen
                view.setBackgroundColor(0xFFE0E0E0); // Mã màu xám nhạt (hoặc dùng R.color...)
                view.setTextColor(0xFF000000);       // Chữ đen
            } else {
                view.setText("Follow");
                // Nền cam, chữ trắng
                view.setBackgroundColor(0xFFFF9800); // Mã màu cam
                view.setTextColor(0xFFFFFFFF);       // Chữ trắng
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

    // Cập nhật Interface thêm onUserClick
    public interface PostInteractionListener {
        void onLikeClicked(String postID, boolean isLiked);
        void onFollowClicked(String userID, boolean isFollowed);
        void onBookmarkClicked(String postID, boolean isBookmarked);
        void onCommentClicked(Post post);
        void onPostClicked(Post post);
        void onUserClick(String userID); // Thêm hàm này để xem Profile
    }
}