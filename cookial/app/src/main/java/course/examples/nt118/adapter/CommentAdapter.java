package course.examples.nt118.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import course.examples.nt118.R;
import course.examples.nt118.databinding.ItemCommentBinding;
import course.examples.nt118.model.Comment;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final Context context;
    private final List<Comment> commentList = new ArrayList<>();

    // [THÊM] Listener để bắn sự kiện reply ra Activity
    private final OnReplyClickListener replyListener;

    public interface OnReplyClickListener {
        void onReplyClick(Comment comment);
    }

    // Constructor cập nhật thêm listener
    public CommentAdapter(Context context, OnReplyClickListener replyListener) {
        this.context = context;
        this.replyListener = replyListener;
    }

    // 1. Dùng cho lần load đầu tiên (xóa cũ, thêm mới)
    public void submitList(List<Comment> comments) {
        commentList.clear();
        if (comments != null) commentList.addAll(comments);
        notifyDataSetChanged();
    }

    // 2. Dùng cho Load More (thêm vào cuối danh sách)
    public void addData(List<Comment> newComments) {
        if (newComments != null && !newComments.isEmpty()) {
            int startPos = commentList.size();
            commentList.addAll(newComments);
            notifyItemRangeInserted(startPos, newComments.size());
        }
    }

    // 3. Hàm lấy danh sách hiện tại (Fix lỗi cannot resolve method)
    public List<Comment> getCurrentList() {
        return commentList;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCommentBinding binding = ItemCommentBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new CommentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        // Bind dữ liệu cơ bản
        holder.binding.tvCmtName.setText(comment.getUserName());
        holder.binding.tvCmtContent.setText(comment.getContent());
        // Hiển thị thời gian nếu có (ví dụ holder.binding.tvTime.setText(...))

        // Load Avatar
        if (comment.getUserAvatar() != null && !comment.getUserAvatar().isEmpty()) {
            Glide.with(context)
                    .load(comment.getUserAvatar())
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .circleCrop()
                    .into(holder.binding.ivCmtAvatar);
        } else {
            holder.binding.ivCmtAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        // [THÊM] Sự kiện bấm vào nút "Trả lời" (Reply)
        // Giả sử trong item_comment.xml có TextView id là tv_reply
        if (holder.binding.tvReply != null) {
            holder.binding.tvReply.setOnClickListener(v -> {
                if (replyListener != null) {
                    replyListener.onReplyClick(comment);
                }
            });
        }

        // Nếu item_comment chưa có tvReply, bạn có thể set sự kiện click vào toàn bộ item hoặc content
        /*
        holder.binding.getRoot().setOnLongClickListener(v -> {
             if (replyListener != null) replyListener.onReplyClick(comment);
             return true;
        });
        */
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        ItemCommentBinding binding;

        public CommentViewHolder(@NonNull ItemCommentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}