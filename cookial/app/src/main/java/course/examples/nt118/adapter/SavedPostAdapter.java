package course.examples.nt118.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import course.examples.nt118.R;
import course.examples.nt118.databinding.ItemSavedPostBinding; // ViewBinding tự sinh
import course.examples.nt118.model.Post;

public class SavedPostAdapter extends RecyclerView.Adapter<SavedPostAdapter.SavedPostViewHolder> {

    private final Context context;
    private List<Post> postList = new ArrayList<>();
    private final OnPostClickListener listener;

    public interface OnPostClickListener {
        void onPostClick(Post post);
    }

    public SavedPostAdapter(Context context, OnPostClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<Post> list) {
        this.postList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SavedPostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSavedPostBinding binding = ItemSavedPostBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SavedPostViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SavedPostViewHolder holder, int position) {
        Post post = postList.get(position);

        // 1. Load Avatar
        if (post.getUserAvatar() != null) {
            Glide.with(context).load(post.getUserAvatar()).circleCrop().into(holder.binding.ivUserAvatar);
        } else {
            holder.binding.ivUserAvatar.setImageResource(R.drawable.ic_launcher_background);
        }

        // 2. Load Ảnh Post (Lấy ảnh đầu tiên)
        if (post.getMedia() != null && !post.getMedia().isEmpty()) {
            Glide.with(context)
                    .load(post.getMedia().get(0))
                    .centerCrop()
                    .placeholder(android.R.color.darker_gray)
                    .into(holder.binding.ivPostImage);
        } else {
            // Ảnh mặc định nếu không có media
            holder.binding.ivPostImage.setImageResource(R.drawable.ic_launcher_background);
        }

        // 3. Text Info
        holder.binding.tvUserName.setText(post.getUserName() != null ? post.getUserName() : "Ẩn danh");
        holder.binding.tvCaption.setText(post.getCaption());


        // 4. Click Event
        holder.itemView.setOnClickListener(v -> listener.onPostClick(post));
    }

    @Override
    public int getItemCount() {
        return postList != null ? postList.size() : 0;
    }

    static class SavedPostViewHolder extends RecyclerView.ViewHolder {
        ItemSavedPostBinding binding;

        public SavedPostViewHolder(@NonNull ItemSavedPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}