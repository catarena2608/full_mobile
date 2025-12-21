package course.examples.nt118.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import course.examples.nt118.databinding.ItemNotificationBinding; // Đảm bảo import đúng binding của item
import course.examples.nt118.model.Notify;

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

    // Hàm set dữ liệu ban đầu (từ API)
    public void setData(List<Notify> list) {
        this.notifyList = list;
        notifyDataSetChanged();
    }

    // === QUAN TRỌNG: Hàm thêm thông báo mới từ Socket vào đầu danh sách ===
    public void addNotificationToTop(Notify newNotify) {
        if (notifyList == null) {
            notifyList = new ArrayList<>();
        }
        // Thêm vào vị trí 0 (đầu danh sách)
        notifyList.add(0, newNotify);
        // Thông báo cho RecyclerView biết có item mới chèn vào vị trí 0 -> Có hiệu ứng đẹp
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

            String senderName = "Người dùng"; // Mặc định nếu null
            String content = "";
            String type = notify.getType();
            if (type == null) type = "";
            switch (type) {
                case "POST":
                    content = "<b>" + senderName + "</b>" + " đã đăng tải một bài viết.";
                    break;

                case "LIKE":
                    content = "<b>" + senderName + "</b>" + " đã thích bài viết của bạn.";
                    break;

                case "COMMENT":
                    content = "<b>" + senderName + "</b>" + " đã bình luận về bài viết của bạn.";
                    break;

                default:
                    // Trường hợp không xác định hoặc mặc định
                    content = "<b>" + senderName + "</b>" + " đã có một hoạt động mới.";
                    break;
            }

            // ---------------------------------------------------------
            // 2. HIỂN THỊ LÊN VIEW (Dùng Html để in đậm tên User)
            // ---------------------------------------------------------

            // Sử dụng Html.fromHtml để render thẻ <b> (in đậm)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                binding.tvContent.setText(android.text.Html.fromHtml(content, android.text.Html.FROM_HTML_MODE_LEGACY));
            } else {
                binding.tvContent.setText(android.text.Html.fromHtml(content));
            }

            // ---------------------------------------------------------
            // 3. XỬ LÝ THỜI GIAN (Giữ nguyên code cũ của bạn)
            // ---------------------------------------------------------
            if (notify.getCreatedAt() != null) {
                CharSequence niceDateStr = android.text.format.DateUtils.getRelativeTimeSpanString(
                        notify.getCreatedAt().getTime(),
                        System.currentTimeMillis(),
                        android.text.format.DateUtils.MINUTE_IN_MILLIS
                );
                binding.tvTime.setText(niceDateStr);
            }

            // ---------------------------------------------------------
            // 4. SỰ KIỆN CLICK
            // ---------------------------------------------------------
            binding.getRoot().setOnClickListener(v -> listener.onItemClick(notify));
        }
    }
}