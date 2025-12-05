package course.examples.nt118.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import course.examples.nt118.R;
import course.examples.nt118.databinding.ItemNotificationBinding;
import course.examples.nt118.model.Notify;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotiViewHolder> {

    private final Context context;
    private List<Notify> notiList;
    private final OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notify notification);
    }

    public NotificationAdapter(Context context, OnNotificationClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.notiList = new ArrayList<>();
    }

    // ================== QUẢN LÝ DỮ LIỆU ==================

    // 1. Set dữ liệu ban đầu (Load từ API)
    public void setData(List<Notify> list) {
        this.notiList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    // 2. Thêm thông báo mới vào đầu (Dùng cho Socket)
    public void addNotificationToTop(Notify notification) {
        if (this.notiList == null) {
            this.notiList = new ArrayList<>();
        }
        this.notiList.add(0, notification);
        notifyItemInserted(0); // Hiệu ứng chèn mượt mà
    }

    // ================== VIEWHOLDER & BINDING ==================

    @NonNull
    @Override
    public NotiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new NotiViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NotiViewHolder holder, int position) {
        Notify noti = notiList.get(position);
        if (noti == null) return;

        // 1. Avatar User
        // LƯU Ý: Model hiện tại chỉ có 'actorID' (String), chưa có URL ảnh.
        // Tạm thời hiển thị ảnh mặc định.
        // Muốn hiển thị ảnh thật, bạn cần sửa Model Notify chứa Object User hoặc gọi API lấy info User.
        Glide.with(context)
                .load(R.drawable.ic_default_avatar) // Đảm bảo bạn có hình này trong drawable
                .placeholder(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(holder.binding.imgAvatar);

        // 2. Nội dung thông báo
        // Sử dụng hàm getDescription() đã viết trong Model
        String content = noti.getDescription();
        // VD: "đã thích bài viết của bạn"

        // Bạn có thể nối thêm tên (tạm thời dùng ID hoặc chữ "Người dùng")
        // String fullText = "Người dùng " + content;
        holder.binding.tvContent.setText(content);

        // 3. Xử lý Thời gian (Time Ago: "Vừa xong", "5 phút trước")
        String timeAgo = formatTimeAgo(noti.getCreatedAt());
        holder.binding.tvTime.setText(timeAgo);

        // 4. Sự kiện click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNotificationClick(noti);
        });

        // 5. Đổi màu nền: Chưa đọc (đậm) vs Đã đọc (mờ/nhạt)
        if (!noti.isRead()) {
            // Chưa đọc: Màu nền nổi hoặc Text đậm
            holder.itemView.setAlpha(1.0f);
            // holder.binding.getRoot().setBackgroundColor(context.getResources().getColor(R.color.blue_light));
        } else {
            // Đã đọc
            holder.itemView.setAlpha(0.7f);
            // holder.binding.getRoot().setBackgroundColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() {
        return notiList != null ? notiList.size() : 0;
    }

    static class NotiViewHolder extends RecyclerView.ViewHolder {
        ItemNotificationBinding binding;

        public NotiViewHolder(@NonNull ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    // ================== HELPER: TIME AGO ==================

    private String formatTimeAgo(String dateString) {
        if (dateString == null) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(dateString);

            if (date != null) {
                long time = date.getTime();
                long now = System.currentTimeMillis();

                // Sử dụng hàm có sẵn của Android để tạo chuỗi "5 mins ago", "Yesterday"
                CharSequence ago = DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS);
                return ago.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dateString; // Fallback về chuỗi gốc nếu lỗi
    }
}