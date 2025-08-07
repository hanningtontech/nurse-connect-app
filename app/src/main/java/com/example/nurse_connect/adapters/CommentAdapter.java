package com.example.nurse_connect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nurse_connect.R;
import com.example.nurse_connect.models.Comment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> comments;
    private OnReplyClickListener replyClickListener;
    private OnLikeClickListener likeClickListener;

    public interface OnReplyClickListener {
        void onReplyClick(Comment comment);
    }

    public interface OnLikeClickListener {
        void onLikeClick(Comment comment);
    }

    public CommentAdapter(List<Comment> comments, OnReplyClickListener replyClickListener, OnLikeClickListener likeClickListener) {
        this.comments = comments;
        this.replyClickListener = replyClickListener;
        this.likeClickListener = likeClickListener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.bind(comment);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public void updateComments(List<Comment> newComments) {
        this.comments = newComments;
        notifyDataSetChanged();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {
        private TextView commentUsername;
        private TextView commentTime;
        private TextView commentText;
        private TextView btnLikeComment;
        private TextView btnReply;
        private TextView replyCount;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            commentUsername = itemView.findViewById(R.id.comment_username);
            commentTime = itemView.findViewById(R.id.comment_time);
            commentText = itemView.findViewById(R.id.comment_text);
            btnLikeComment = itemView.findViewById(R.id.btn_like_comment);
            btnReply = itemView.findViewById(R.id.btn_reply);
            replyCount = itemView.findViewById(R.id.reply_count);
        }

        public void bind(Comment comment) {
            // Set username
            commentUsername.setText(comment.getUsername() != null ? comment.getUsername() : "Anonymous");

            // Set time
            commentTime.setText(formatTime(comment.getCreatedAt()));

            // Set comment text
            commentText.setText(comment.getText());

            // Set like text
            if (comment.getLikes() > 0) {
                btnLikeComment.setText(comment.getLikes() + " likes");
            } else {
                btnLikeComment.setText("Like");
            }

            // Set reply count
            if (comment.getReplyCount() > 0) {
                replyCount.setText(comment.getReplyCount() + " replies");
                replyCount.setVisibility(View.VISIBLE);
            } else {
                replyCount.setVisibility(View.GONE);
            }

            // Set click listeners
            btnLikeComment.setOnClickListener(v -> {
                if (likeClickListener != null) {
                    likeClickListener.onLikeClick(comment);
                }
            });

            btnReply.setOnClickListener(v -> {
                if (replyClickListener != null) {
                    replyClickListener.onReplyClick(comment);
                }
            });
        }

        private String formatTime(Long timestamp) {
            if (timestamp == null) return "Just now";
            
            long currentTime = System.currentTimeMillis();
            long diff = currentTime - timestamp;
            
            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;
            
            if (days > 0) {
                return days + "d ago";
            } else if (hours > 0) {
                return hours + "h ago";
            } else if (minutes > 0) {
                return minutes + "m ago";
            } else {
                return "Just now";
            }
        }
    }
} 