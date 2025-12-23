package course.examples.nt118.adapter;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

import course.examples.nt118.R;
import course.examples.nt118.databinding.ItemNotificationBinding;
import course.examples.nt118.model.Notify;
import course.examples.nt118.model.UserResponse;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private Context context;
    private List<Notify> notifyList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Notify notify);
    }

    public NotificationAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.notifyList = new ArrayList<>();
    }

    public void setData(List<Notify> list) {
        this.notifyList = list;
        notifyDataSetChanged();
    }

    public void addNotificationToTop(Notify newNotify) {
        if (notifyList == null) notifyList = new ArrayList<>();
        notifyList.add(0, newNotify);
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new NotificationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notify notify = notifyList.get(position);
        holder.bind(notify);
    }

    @Override
    public int getItemCount() {
        return notifyList == null ? 0 : notifyList.size();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        ItemNotificationBinding binding;

        public NotificationViewHolder(ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Notify notify) {
            // 1. Xử lý thông tin người dùng (Actor)
            String senderName = "Người dùng"; // Tên mặc định
            String avatarUrl = "";

            if (notify.getActor() != null) {
                UserResponse actor = notify.getActor();
                if (actor.getName() != null) senderName = actor.getName();
                if (actor.getAvatar() != null) avatarUrl = actor.getAvatar();
            }

            // 2. Load Avatar bằng Glide
            // 🔥 QUAN TRỌNG: Load vào imgAvatar (ImageView), không phải ivUserAvatar (CardView)
            if (!avatarUrl.isEmpty()) {
                Glide.with(context)
                        .load(avatarUrl)
                        .placeholder(R.drawable.ic_default_avatar) // Đảm bảo file này tồn tại trong drawable
                        .centerCrop() // Dùng centerCrop để ảnh lấp đầy ImageView
                        .into(binding.imgAvatar); // <--- ĐÃ SỬA: imgAvatar
            } else {
                binding.imgAvatar.setImageResource(R.drawable.ic_default_avatar); // <--- ĐÃ SỬA: imgAvatar
            }

            // 3. Tạo nội dung thông báo
            String content = "";
            String type = notify.getType();
            if (type == null) type = "";

            switch (type) {
                case "POST":
                    content = "<b>" + senderName + "</b>" + " đã đăng một bài viết mới.";
                    break;
                case "like":
                case "LIKE":
                    content = "<b>" + senderName + "</b>" + " đã thích bài viết của bạn.";
                    break;
                case "comment":
                case "COMMENT":
                    content = "<b>" + senderName + "</b>" + " đã bình luận về bài viết.";
                    break;
                case "follow":
                    content = "<b>" + senderName + "</b>" + " đã bắt đầu theo dõi bạn.";
                    break;
                default:
                    content = "<b>" + senderName + "</b>" + " đã có hoạt động mới.";
                    break;
            }

            // 4. Set Text HTML
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                binding.tvContent.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
            } else {
                binding.tvContent.setText(Html.fromHtml(content));
            }

            // 5. Thời gian
            if (notify.getCreatedAt() != null) {
                CharSequence niceDateStr = android.text.format.DateUtils.getRelativeTimeSpanString(
                        notify.getCreatedAt().getTime(),
                        System.currentTimeMillis(),
                        android.text.format.DateUtils.MINUTE_IN_MILLIS
                );
                binding.tvTime.setText(niceDateStr);
            }

            // 6. Click Event
            binding.getRoot().setOnClickListener(v -> listener.onItemClick(notify));
        }
    }
}