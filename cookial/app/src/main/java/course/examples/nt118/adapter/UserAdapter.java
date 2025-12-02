package course.examples.nt118.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import course.examples.nt118.R;
import course.examples.nt118.model.UserResponse;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<UserResponse> mListUsers;
    private final OnUserClickListener mListener;
    private Context context;
    private boolean isHistoryMode; // Biến cờ để quyết định có hiện nút X hay không

    // Constructor
    // isHistoryMode = true -> Hiện nút X (dùng cho lịch sử)
    // isHistoryMode = false -> Ẩn nút X (dùng cho kết quả tìm kiếm)
    public UserAdapter(Context context, List<UserResponse> list, boolean isHistoryMode, OnUserClickListener listener) {
        this.context = context;
        this.mListUsers = (list != null) ? list : new ArrayList<>();
        this.isHistoryMode = isHistoryMode;
        this.mListener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserResponse user = mListUsers.get(position);
        if (user == null) return;

        // 1. Bind Data
        // Backend trả về name (Tên hiển thị) và có thể user_name (ID người dùng)
        // Bạn kiểm tra lại model UserResponse xem field tên là gì nhé
        holder.tvFullname.setText(user.getName());

        // Giả sử model có field username, nếu không có thì dùng email hoặc ẩn đi
        // holder.tvUsername.setText("@" + user.getUserName());
        holder.tvUsername.setText(user.getEmail()); // Tạm dùng email nếu chưa có username

        // Load Avatar
        String avatarUrl = (user.getAvatar() != null && !user.getAvatar().isEmpty())
                ? user.getAvatar()
                : "https://i.pravatar.cc/150?u=" + user.getId();

        Glide.with(context)
                .load(avatarUrl)
                .placeholder(R.drawable.chef_hat)
                .circleCrop()
                .into(holder.ivAvatar);

        // 2. Xử lý nút Remove (Dấu X)
        if (isHistoryMode) {
            holder.btnRemove.setVisibility(View.VISIBLE);
            holder.btnRemove.setOnClickListener(v -> mListener.onRemoveClick(user, position));
        } else {
            // Nếu là kết quả tìm kiếm thì ẩn nút X đi (hoặc đổi thành nút Follow tùy logic sau này)
            holder.btnRemove.setVisibility(View.GONE);
        }

        // 3. Click vào item -> Mở Profile
        holder.itemView.setOnClickListener(v -> mListener.onUserClick(user));
    }

    @Override
    public int getItemCount() {
        return mListUsers.size();
    }

    public void setData(List<UserResponse> list) {
        this.mListUsers = list;
        notifyDataSetChanged();
    }

    // --- ViewHolder ---
    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar, btnRemove;
        TextView tvUsername, tvFullname;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ ID theo file xml item_search
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            btnRemove = itemView.findViewById(R.id.btn_remove);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvFullname = itemView.findViewById(R.id.tv_fullname);
        }
    }

    // --- Interface giao tiếp ---
    public interface OnUserClickListener {
        void onUserClick(UserResponse user);       // Mở profile
        void onRemoveClick(UserResponse user, int position); // Xóa khỏi lịch sử
    }
}