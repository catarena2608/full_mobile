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
import course.examples.nt118.databinding.ItemProfileImageBinding; // ViewBinding tự sinh từ XML item_profile_image
import course.examples.nt118.model.Post;

public class ProfilePostAdapter extends RecyclerView.Adapter<ProfilePostAdapter.PostViewHolder> {

    private final Context context;
    private List<Post> postList = new ArrayList<>();
    private final OnPostClickListener listener;

    public interface OnPostClickListener {
        void onPostClick(Post post);
    }

    public ProfilePostAdapter(Context context, OnPostClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setPosts(List<Post> posts) {
        this.postList = posts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng ViewBinding cho layout item_profile_image.xml
        ItemProfileImageBinding binding = ItemProfileImageBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new PostViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        // Load ảnh đầu tiên trong list media của post
        if (post.getMedia() != null && !post.getMedia().isEmpty()) {
            Glide.with(context)
                    .load(post.getMedia().get(0))
                    .placeholder(R.drawable.chef_hat) // Ảnh placeholder nếu chưa load xong
                    .centerCrop()
                    .into(holder.binding.ivFood);
        } else {
            // Nếu post không có ảnh thì hiện ảnh mặc định
            holder.binding.ivFood.setImageResource(R.drawable.chef_hat);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPostClick(post);
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ItemProfileImageBinding binding;

        public PostViewHolder(@NonNull ItemProfileImageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}