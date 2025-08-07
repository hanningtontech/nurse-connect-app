package com.example.nurse_connect.adapters;

import static com.example.nurse_connect.models.ChatRoom.ChatRoomType.DIRECT;
import static com.example.nurse_connect.models.ChatRoom.ChatRoomType.GLOBAL_CHANNEL;
import static com.example.nurse_connect.models.ChatRoom.ChatRoomType.STUDY_GROUP;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.nurse_connect.R;
import com.example.nurse_connect.models.ChatRoom;
import com.example.nurse_connect.models.Message;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder> {
    
    private List<ChatRoom> chatRooms = new ArrayList<>();
    private OnChatRoomClickListener listener;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yy", Locale.getDefault());
    
    public interface OnChatRoomClickListener {
        void onChatRoomClick(ChatRoom chatRoom);
        void onChatRoomLongClick(ChatRoom chatRoom, View view);
    }
    
    public ChatRoomAdapter(OnChatRoomClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ChatRoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_room, parent, false);
        return new ChatRoomViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ChatRoomViewHolder holder, int position) {
        ChatRoom chatRoom = chatRooms.get(position);
        holder.bind(chatRoom);
    }
    
    @Override
    public int getItemCount() {
        return chatRooms.size();
    }
    
    public void setChatRooms(List<ChatRoom> chatRooms) {
        this.chatRooms = chatRooms != null ? chatRooms : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void addChatRoom(ChatRoom chatRoom) {
        chatRooms.add(0, chatRoom);
        notifyItemInserted(0);
    }
    
    public void updateChatRoom(ChatRoom updatedChatRoom) {
        for (int i = 0; i < chatRooms.size(); i++) {
            if (chatRooms.get(i).getRoomId().equals(updatedChatRoom.getRoomId())) {
                chatRooms.set(i, updatedChatRoom);
                notifyItemChanged(i);
                break;
            }
        }
    }
    
    class ChatRoomViewHolder extends RecyclerView.ViewHolder {
        private CircleImageView ivProfile;
        private TextView tvName;
        private TextView tvNameEmojis;
        private TextView tvLastMessage;
        private TextView tvTime;
        private TextView tvUnreadCount;
        private ImageView ivPinned;
        private ImageView ivMuted;
        
        public ChatRoomViewHolder(@NonNull View itemView) {
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
                    listener.onChatRoomClick(chatRooms.get(position));
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onChatRoomLongClick(chatRooms.get(position), v);
                    return true;
                }
                return false;
            });
        }
        
        public void bind(ChatRoom chatRoom) {
            // Set name
            tvName.setText(chatRoom.getName());
            
            // Set emojis based on chat type
            setChatTypeEmojis(tvNameEmojis, chatRoom.getType());
            
            // Set profile image
            if (chatRoom.getPhotoUrl() != null && !chatRoom.getPhotoUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(chatRoom.getPhotoUrl())
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(ivProfile);
            } else {
                // Set default avatar based on chat type
                switch (chatRoom.getType()) {
                    case DIRECT:
                        ivProfile.setImageResource(R.drawable.ic_person);
                        break;
                    case STUDY_GROUP:
                        ivProfile.setImageResource(R.drawable.ic_group);
                        break;
                    case GLOBAL_CHANNEL:
                        ivProfile.setImageResource(R.drawable.ic_channel);
                        break;
                    default:
                        ivProfile.setImageResource(R.drawable.ic_profile_placeholder);
                        break;
                }
            }
            
            // Set last message
            Message lastMessage = chatRoom.getLastMessage();
            if (lastMessage != null) {
                String messagePreview = getMessagePreview(lastMessage);
                tvLastMessage.setText(messagePreview);
                
                // Set time with proper formatting
                Timestamp lastMessageTime = chatRoom.getLastMessageAt();
                if (lastMessageTime != null) {
                    String timeText = formatTime(lastMessageTime.toDate());
                    tvTime.setText(timeText);
                    
                    // Make recent timestamps green (within last 24 hours)
                    long currentTime = System.currentTimeMillis();
                    long messageTime = lastMessageTime.toDate().getTime();
                    long diff = currentTime - messageTime;
                    
                    if (diff < 24 * 60 * 60 * 1000) { // Within 24 hours
                        tvTime.setTextColor(itemView.getContext().getColor(android.R.color.holo_green_light));
                    } else {
                        tvTime.setTextColor(itemView.getContext().getColor(android.R.color.darker_gray));
                    }
                }
            } else {
                tvLastMessage.setText("No messages yet");
                tvTime.setText("");
            }
            
            // Set unread count
            int unreadCount = chatRoom.getUnreadCount();
            if (unreadCount > 0) {
                tvUnreadCount.setVisibility(View.VISIBLE);
                if (unreadCount > 99) {
                    tvUnreadCount.setText("99+");
                } else {
                    tvUnreadCount.setText(String.valueOf(unreadCount));
                }
            } else {
                tvUnreadCount.setVisibility(View.GONE);
            }
            
            // Set pinned status (you can implement this based on your data model)
            boolean isPinned = chatRoom.isPinned() != null ? chatRoom.isPinned() : false;
            ivPinned.setVisibility(isPinned ? View.VISIBLE : View.GONE);
            
            // Set muted status (you can implement this based on your data model)
            boolean isMuted = chatRoom.isMuted() != null ? chatRoom.isMuted() : false;
            ivMuted.setVisibility(isMuted ? View.VISIBLE : View.GONE);
        }
        
        private void setChatTypeEmojis(TextView emojiView, ChatRoom.ChatRoomType chatType) {
            switch (chatType) {
                case STUDY_GROUP:
                    emojiView.setText("📚");
                    break;
                case GLOBAL_CHANNEL:
                    emojiView.setText("🌍");
                    break;
                case DIRECT:
                    emojiView.setText("💬");
                    break;
                default:
                    emojiView.setText("💬");
                    break;
            }
        }
        
        private String getMessagePreview(Message message) {
            switch (message.getType()) {
                case IMAGE:
                    return "📷 Image";
                case VIDEO:
                    return "🎥 Video";
                case PDF:
                    return "📄 PDF";
                case DOCUMENT:
                    return "📎 Document";
                case TASK:
                    return "📋 Task";
                case SYSTEM:
                    return "ℹ️ " + message.getContent();
                default:
                    return message.getContent();
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
    }
} 