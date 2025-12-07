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
            // Gán dữ liệu vào View
            // Ví dụ: binding.tvContent.setText(notify.getContent());
            // binding.tvTime.setText(...);

            // Xử lý click
            binding.getRoot().setOnClickListener(v -> listener.onItemClick(notify));
        }
    }
}