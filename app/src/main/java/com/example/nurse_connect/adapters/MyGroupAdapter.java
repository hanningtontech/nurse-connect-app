package com.example.nurse_connect.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.nurse_connect.R;
import com.example.nurse_connect.models.GroupChat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyGroupAdapter extends RecyclerView.Adapter<MyGroupAdapter.GroupViewHolder> {

    private List<GroupChat> groups;
    private String currentUserId;
    private OnGroupClickListener listener;

    public interface OnGroupClickListener {
        void onGroupClick(GroupChat group);
    }

    public MyGroupAdapter(List<GroupChat> groups, String currentUserId, OnGroupClickListener listener) {
        this.groups = groups;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        GroupChat group = groups.get(position);
        holder.bind(group);
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivGroupPhoto;
        private TextView tvGroupTitle;
        private TextView tvLastMessage;
        private TextView tvLastMessageTime;
        private TextView tvUnreadCount;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            ivGroupPhoto = itemView.findViewById(R.id.ivGroupPhoto);
            tvGroupTitle = itemView.findViewById(R.id.tvGroupTitle);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvLastMessageTime = itemView.findViewById(R.id.tvLastMessageTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onGroupClick(groups.get(position));
                }
            });
        }

        public void bind(GroupChat group) {
            Log.d("MyGroupAdapter", "Binding group: " + group.getTitle() + " (ID: " + group.getGroupId() + ")");

            // Set group title
            tvGroupTitle.setText(group.getTitle());
            
            // Set last message
            if (group.getLastMessage() != null && !group.getLastMessage().trim().isEmpty()) {
                tvLastMessage.setText(group.getLastMessage());
                tvLastMessage.setVisibility(View.VISIBLE);
            } else {
                tvLastMessage.setText("No messages yet");
                tvLastMessage.setVisibility(View.VISIBLE);
            }

            // Remove group type and member count for cleaner Direct Messages style

            // Set last message time (Telegram format - actual time or date)
            if (group.getLastMessageTime() != null) {
                tvLastMessageTime.setText(getTelegramTimeFormat(group.getLastMessageTime()));
            } else {
                tvLastMessageTime.setText("");
            }

            // Set unread count with detailed logging
            int unreadCount = group.getUnreadCountForUser(currentUserId);
            Log.d("MyGroupAdapter", "Group '" + group.getTitle() + "' unread count for user " + currentUserId + ": " + unreadCount);

            if (unreadCount > 0) {
                tvUnreadCount.setText(String.valueOf(Math.min(unreadCount, 99))); // Cap at 99
                tvUnreadCount.setVisibility(View.VISIBLE);
                Log.d("MyGroupAdapter", "Showing badge for group '" + group.getTitle() + "' with count: " + unreadCount);
            } else {
                tvUnreadCount.setVisibility(View.GONE);
                Log.d("MyGroupAdapter", "Hiding badge for group '" + group.getTitle() + "' (count is 0)");
            }

            // Load group photo
            if (group.getGroupPhotoURL() != null && !group.getGroupPhotoURL().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(group.getGroupPhotoURL())
                        .placeholder(R.drawable.ic_group)
                        .error(R.drawable.ic_group)
                        .into(ivGroupPhoto);
            } else {
                ivGroupPhoto.setImageResource(R.drawable.ic_group);
            }
        }
    }

    private String getTelegramTimeFormat(Date messageTime) {
        long now = System.currentTimeMillis();
        long messageTimeMillis = messageTime.getTime();
        long diff = now - messageTimeMillis;

        // If message is from today, show time (HH:mm)
        if (diff < 24 * 60 * 60 * 1000) { // Less than 24 hours
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return timeFormat.format(messageTime);
        }

        // If message is from this week, show day name
        if (diff < 7 * 24 * 60 * 60 * 1000) { // Less than 7 days
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
            return dayFormat.format(messageTime);
        }

        // If message is older, show date (MMM dd)
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        return dateFormat.format(messageTime);
    }
}
