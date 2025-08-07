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
import com.example.nurse_connect.models.PrivateChat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class PrivateChatAdapter extends RecyclerView.Adapter<PrivateChatAdapter.ChatViewHolder> {

    private List<PrivateChat> chatList;
    private OnChatClickListener listener;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yy", Locale.getDefault());
    private static Map<String, Integer> previousUnreadCounts = new HashMap<>();

    public interface OnChatClickListener {
        void onChatClick(PrivateChat chat);
    }

    public PrivateChatAdapter(List<PrivateChat> chatList, OnChatClickListener listener) {
        this.chatList = chatList;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_room, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        PrivateChat chat = chatList.get(position);
        holder.bind(chat);
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    // Method to update a specific chat's unread count without refreshing entire list
    public void updateChatUnreadCount(String chatId, int newUnreadCount) {
        for (int i = 0; i < chatList.size(); i++) {
            PrivateChat chat = chatList.get(i);
            if (chat.getChatId().equals(chatId)) {
                // Update the unread count in the model
                if (chat.getUnreadCounts() == null) {
                    chat.setUnreadCounts(new HashMap<>());
                }
                chat.getUnreadCounts().put(currentUser.getUid(), newUnreadCount);

                // Notify only this specific item
                notifyItemChanged(i);
                Log.d("PrivateChatAdapter", "Updated unread count for chat " + chatId + " to " + newUnreadCount);
                break;
            }
        }
    }

    // Method to get total unread count across all chats
    public int getTotalUnreadCount() {
        int total = 0;
        for (PrivateChat chat : chatList) {
            total += chat.getUnreadCountForUser(currentUser.getUid());
        }
        return total;
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivProfile;
        TextView tvName;
        TextView tvNameEmojis;
        TextView tvLastMessage;
        TextView tvTime;
        TextView tvUnreadCount;
        ImageView ivPinned;
        ImageView ivMuted;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvName = itemView.findViewById(R.id.tvName);
            tvNameEmojis = itemView.findViewById(R.id.tvNameEmojis);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
            ivPinned = itemView.findViewById(R.id.ivPinned);
            ivMuted = itemView.findViewById(R.id.ivMuted);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onChatClick(chatList.get(position));
                }
            });
        }

        void bind(PrivateChat chat) {
            // Get the other user's ID
            String otherUserId = null;
            for (String participantId : chat.getParticipants()) {
                if (!participantId.equals(currentUser.getUid())) {
                    otherUserId = participantId;
                    break;
                }
            }

            if (otherUserId != null) {
                // Load other user's info
                db.collection("users")
                        .document(otherUserId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String username = documentSnapshot.getString("username");
                                String photoURL = documentSnapshot.getString("photoURL");

                                // Set username
                                if (username != null) {
                                    tvName.setText(username);
                                }

                                // Set emojis (you can customize this based on user preferences or roles)
                                setUserEmojis(tvNameEmojis, username);

                                // Load profile photo
                                if (photoURL != null && !photoURL.isEmpty()) {
                                    Glide.with(itemView.getContext())
                                            .load(photoURL)
                                            .placeholder(R.drawable.ic_profile_placeholder)
                                            .error(R.drawable.ic_profile_placeholder)
                                            .into(ivProfile);
                                } else {
                                    ivProfile.setImageResource(R.drawable.ic_profile_placeholder);
                                }
                            }
                        });
            }

            // Set last message with status indicators
            if (chat.getLastMessage() != null) {
                String lastMessage = chat.getLastMessage();
                // Check if message is from current user
                boolean isFromCurrentUser = chat.getLastMessageSenderId() != null && 
                                          chat.getLastMessageSenderId().equals(currentUser.getUid());
                
                if (isFromCurrentUser) {
                    tvLastMessage.setText("✓✓ You: " + lastMessage);
                } else {
                    tvLastMessage.setText(lastMessage);
                }
            } else {
                tvLastMessage.setText("No messages yet");
            }

            // Set message time with proper formatting
            if (chat.getLastMessageTime() != null) {
                String timeText = formatTime(chat.getLastMessageTime());
                tvTime.setText(timeText);
                
                // Make recent timestamps green (within last 24 hours)
                long currentTime = System.currentTimeMillis();
                long messageTime = chat.getLastMessageTime().getTime();
                long diff = currentTime - messageTime;
                
                if (diff < 24 * 60 * 60 * 1000) { // Within 24 hours
                    tvTime.setTextColor(itemView.getContext().getColor(android.R.color.holo_green_light));
                } else {
                    tvTime.setTextColor(itemView.getContext().getColor(android.R.color.darker_gray));
                }
            } else {
                tvTime.setText("");
            }

            // Set unread count for current user
            int unreadCount = chat.getUnreadCountForUser(currentUser.getUid());
            Log.d("PrivateChatAdapter", "Chat " + chat.getChatId() +
                             " unread count for user " + currentUser.getUid() + ": " + unreadCount +
                             " unreadCounts map: " + chat.getUnreadCounts());

            // Check if this is a new message (unread count increased)
            String chatKey = chat.getChatId() + "_" + currentUser.getUid();
            Integer previousCount = previousUnreadCounts.get(chatKey);
            boolean isNewMessage = previousCount != null && unreadCount > previousCount;

            // Update the previous count
            previousUnreadCounts.put(chatKey, unreadCount);

            // Enhanced badge display logic
            if (unreadCount > 0) {
                tvUnreadCount.setVisibility(View.VISIBLE);

                // Set badge text with better formatting
                if (unreadCount > 999) {
                    tvUnreadCount.setText("999+");
                } else if (unreadCount > 99) {
                    tvUnreadCount.setText("99+");
                } else {
                    tvUnreadCount.setText(String.valueOf(unreadCount));
                }

                // Enhanced visual feedback for unread messages
                tvName.setTextColor(itemView.getContext().getColor(android.R.color.white));
                tvName.setTypeface(null, android.graphics.Typeface.BOLD);
                tvLastMessage.setTextColor(itemView.getContext().getColor(android.R.color.white));
                tvLastMessage.setTypeface(null, android.graphics.Typeface.BOLD);

                // Enhanced badge color coding with improved visual design
                if (unreadCount > 10) {
                    tvUnreadCount.setBackgroundResource(R.drawable.bg_unread_badge_red_enhanced);
                } else {
                    tvUnreadCount.setBackgroundResource(R.drawable.bg_unread_badge_enhanced);
                }

                // Animate the badge only for new messages with improved detection
                if (isNewMessage && unreadCount > 0) {
                    Log.d("PrivateChatAdapter", "Animating badge for new message. Previous: " + previousCount + ", Current: " + unreadCount);
                    animateBadge(tvUnreadCount);
                }

                Log.d("PrivateChatAdapter", "Showing badge with count: " + unreadCount);
            } else {
                tvUnreadCount.setVisibility(View.GONE);

                // Reset text styling for read messages
                tvName.setTextColor(itemView.getContext().getColor(android.R.color.white));
                tvName.setTypeface(null, android.graphics.Typeface.NORMAL);
                tvLastMessage.setTextColor(itemView.getContext().getColor(android.R.color.darker_gray));
                tvLastMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
                
                System.out.println("PrivateChatAdapter: Hiding badge");
            }

            // Set pinned status (you can implement this based on your data model)
            boolean isPinned = chat.isPinned() != null ? chat.isPinned() : false;
            ivPinned.setVisibility(isPinned ? View.VISIBLE : View.GONE);

            // Set muted status (you can implement this based on your data model)
            boolean isMuted = chat.isMuted() != null ? chat.isMuted() : false;
            ivMuted.setVisibility(isMuted ? View.VISIBLE : View.GONE);
        }

        private void setUserEmojis(TextView emojiView, String username) {
            // You can customize this based on user roles, preferences, or other criteria
            if (username != null) {
                if (username.toLowerCase().contains("nurse")) {
                    emojiView.setText("👩‍⚕️");
                } else if (username.toLowerCase().contains("doctor")) {
                    emojiView.setText("👨‍⚕️");
                } else if (username.toLowerCase().contains("student")) {
                    emojiView.setText("🎓");
                } else {
                    emojiView.setText("💬");
                }
            } else {
                emojiView.setText("💬");
            }
        }

        private String formatTime(Date date) {
            long currentTime = System.currentTimeMillis();
            long messageTime = date.getTime();
            long diff = currentTime - messageTime;

            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (days > 7) {
                return dateFormat.format(date);
            } else if (days > 0) {
                return days + "d";
            } else if (hours > 0) {
                return timeFormat.format(date);
            } else if (minutes > 0) {
                return minutes + "m";
            } else {
                return "now";
            }
        }
        
        private void animateBadge(TextView badge) {
            // Enhanced animation with pulse effect for new messages
            badge.setAlpha(0.7f);
            badge.animate()
                    .alpha(1.0f)
                    .scaleX(1.3f)
                    .scaleY(1.3f)
                    .setDuration(300)
                    .withEndAction(() ->
                        badge.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(200)
                                .withEndAction(() -> {
                                    // Add a subtle pulse effect
                                    badge.animate()
                                            .scaleX(1.1f)
                                            .scaleY(1.1f)
                                            .setDuration(150)
                                            .withEndAction(() ->
                                                badge.animate()
                                                        .scaleX(1.0f)
                                                        .scaleY(1.0f)
                                                        .setDuration(150)
                                                        .start()
                                            )
                                            .start();
                                })
                                .start()
                    )
                    .start();
        }
    }
} 