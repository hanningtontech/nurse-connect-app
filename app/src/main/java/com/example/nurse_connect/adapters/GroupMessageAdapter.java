package com.example.nurse_connect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.nurse_connect.R;
import com.example.nurse_connect.models.GroupMessage;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class GroupMessageAdapter extends RecyclerView.Adapter<GroupMessageAdapter.MessageViewHolder> {

    private List<GroupMessage> messages;
    private String currentUserId;
    private OnMessageActionListener listener;

    public interface OnMessageActionListener {
        void onReplyToMessage(GroupMessage message);
        void onProfileClick(String userId, String userName);
    }

    public GroupMessageAdapter(List<GroupMessage> messages, String currentUserId, OnMessageActionListener listener) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        GroupMessage message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout layoutSentMessage;
        private LinearLayout layoutReceivedMessage;
        private TextView tvSentMessage;
        private TextView tvSentTime;
        private TextView tvReceivedMessage;
        private TextView tvReceivedTime;
        private TextView tvSenderName;
        private ImageView ivSenderProfile;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutSentMessage = itemView.findViewById(R.id.layoutSentMessage);
            layoutReceivedMessage = itemView.findViewById(R.id.layoutReceivedMessage);
            tvSentMessage = itemView.findViewById(R.id.tvSentMessage);
            tvSentTime = itemView.findViewById(R.id.tvSentTime);
            tvReceivedMessage = itemView.findViewById(R.id.tvReceivedMessage);
            tvReceivedTime = itemView.findViewById(R.id.tvReceivedTime);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            ivSenderProfile = itemView.findViewById(R.id.ivSenderProfile);

            // Set up swipe-to-reply for both message layouts
            setupSwipeToReply();
        }

        private void setupSwipeToReply() {
            View.OnLongClickListener replyListener = v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onReplyToMessage(messages.get(position));
                }
                return true;
            };

            // Add long click listeners for swipe-to-reply
            layoutSentMessage.setOnLongClickListener(replyListener);
            layoutReceivedMessage.setOnLongClickListener(replyListener);
            tvSentMessage.setOnLongClickListener(replyListener);
            tvReceivedMessage.setOnLongClickListener(replyListener);
        }

        public void bind(GroupMessage message) {
            boolean isSentByCurrentUser = message.getSenderId().equals(currentUserId);

            if (isSentByCurrentUser) {
                // Show sent message layout
                layoutSentMessage.setVisibility(View.VISIBLE);
                layoutReceivedMessage.setVisibility(View.GONE);

                tvSentMessage.setText(message.getContent());

                if (message.getTimestamp() != null) {
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    tvSentTime.setText(timeFormat.format(message.getTimestamp()));
                }
            } else {
                // Show received message layout
                layoutSentMessage.setVisibility(View.GONE);
                layoutReceivedMessage.setVisibility(View.VISIBLE);

                tvReceivedMessage.setText(message.getContent());

                // Set sender name
                String senderName = message.getSenderName();
                if (senderName == null || senderName.trim().isEmpty()) {
                    senderName = "Unknown User";
                }
                tvSenderName.setText(senderName);

                if (message.getTimestamp() != null) {
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    tvReceivedTime.setText(timeFormat.format(message.getTimestamp()));
                }

                // Load sender profile image
                if (message.getSenderPhotoUrl() != null && !message.getSenderPhotoUrl().isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(message.getSenderPhotoUrl())
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .into(ivSenderProfile);
                } else {
                    ivSenderProfile.setImageResource(R.drawable.ic_person);
                }

                // Set up profile click listeners
                View.OnClickListener profileClickListener = v -> {
                    if (listener != null) {
                        listener.onProfileClick(message.getSenderId(), message.getSenderName());
                    }
                };

                ivSenderProfile.setOnClickListener(profileClickListener);
                tvSenderName.setOnClickListener(profileClickListener);
            }
        }
    }
}
