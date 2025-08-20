package com.example.nurse_connect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.nurse_connect.R;
import com.example.nurse_connect.models.NotificationItem;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<NotificationItem> notifications;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationItem notification);
        void onActionClick(NotificationItem notification);
    }

    public NotificationAdapter(List<NotificationItem> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem notification = notifications.get(position);
        holder.bind(notification);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void updateNotifications(List<NotificationItem> newNotifications) {
        this.notifications.clear();
        this.notifications.addAll(newNotifications);
        notifyDataSetChanged();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private CircleImageView ivUserPhoto;
        private ImageView ivNotificationIcon;
        private TextView tvMessageType;
        private TextView tvTitle;
        private TextView tvMessage;
        private TextView tvTimeAgo;
        private MaterialButton btnAction;
        private View unreadIndicator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            
            ivUserPhoto = itemView.findViewById(R.id.ivUserPhoto);
            ivNotificationIcon = itemView.findViewById(R.id.ivNotificationIcon);
            tvMessageType = itemView.findViewById(R.id.tvMessageType);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimeAgo = itemView.findViewById(R.id.tvTimeAgo);
            btnAction = itemView.findViewById(R.id.btnAction);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onNotificationClick(notifications.get(getAdapterPosition()));
                }
            });

            btnAction.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onActionClick(notifications.get(getAdapterPosition()));
                }
            });
        }

        public void bind(NotificationItem notification) {
            // Set message type label
            String messageTypeText = getMessageTypeText(notification.getType());
            tvMessageType.setText(messageTypeText);

            // Set title (sender name) and message
            String title = notification.getTitle();
            if (title == null || title.isEmpty()) {
                title = notification.getFromUserName() != null ? notification.getFromUserName() : "Unknown User";
            }
            tvTitle.setText(title);
            tvMessage.setText(notification.getMessage());
            tvTimeAgo.setText(notification.getTimeAgo());

            // Set notification icon
            ivNotificationIcon.setImageResource(notification.getIconResource());

            // Set user photo
            if (notification.getFromUserPhotoUrl() != null && !notification.getFromUserPhotoUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(notification.getFromUserPhotoUrl())
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(ivUserPhoto);
                ivUserPhoto.setVisibility(View.VISIBLE);
            } else {
                ivUserPhoto.setVisibility(View.GONE);
            }

            // Set action button
            if (notification.getActionText() != null && !notification.getActionText().isEmpty()) {
                btnAction.setText(notification.getActionText());
                btnAction.setVisibility(View.VISIBLE);
            } else {
                btnAction.setVisibility(View.GONE);
            }

            // Show/hide unread indicator
            unreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

            // Set background based on read status
            if (notification.isRead()) {
                itemView.setAlpha(0.7f);
            } else {
                itemView.setAlpha(1.0f);
            }

            // Set different styling based on notification type
            switch (notification.getType()) {
                case "message":
                    ivNotificationIcon.setColorFilter(itemView.getContext().getColor(R.color.theme_primary));
                    break;
                case "follow":
                    ivNotificationIcon.setColorFilter(itemView.getContext().getColor(R.color.theme_secondary));
                    break;
                case "task":
                    ivNotificationIcon.setColorFilter(itemView.getContext().getColor(R.color.theme_accent));
                    break;
                case "like":
                    ivNotificationIcon.setColorFilter(itemView.getContext().getColor(R.color.theme_secondary));
                    break;
                case "comment":
                    ivNotificationIcon.setColorFilter(itemView.getContext().getColor(R.color.theme_primary));
                    break;
                default:
                    ivNotificationIcon.setColorFilter(itemView.getContext().getColor(R.color.theme_text_secondary));
                    break;
            }
        }

        private String getMessageTypeText(String type) {
            switch (type != null ? type : "message") {
                case "message":
                    return "💬 Message";
                case "follow":
                    return "👤 New follower";
                case "task":
                    return "📋 Task assigned";
                case "suggestion":
                    return "💡 Suggestion";
                case "like":
                    return "❤️ New like";
                case "comment":
                    return "💬 New comment";
                default:
                    return "🔔 Notification";
            }
        }
    }
}
