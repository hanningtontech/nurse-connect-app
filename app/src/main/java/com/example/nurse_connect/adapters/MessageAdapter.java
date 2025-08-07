package com.example.nurse_connect.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.nurse_connect.R;
import com.example.nurse_connect.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int VIEW_TYPE_MY_MESSAGE = 1;
    private static final int VIEW_TYPE_OTHER_MESSAGE = 2;
    private static final int VIEW_TYPE_SYSTEM_MESSAGE = 3;
    
    private List<Message> messages = new ArrayList<>();
    private String currentUserId;
    private OnMessageClickListener listener;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    
    public interface OnMessageClickListener {
        void onMessageClick(Message message);
        void onMessageLongClick(Message message, View view);
        void onMediaClick(Message message);
    }
    
    public MessageAdapter(OnMessageClickListener listener) {
        this.listener = listener;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        
        switch (viewType) {
            case VIEW_TYPE_MY_MESSAGE:
                View myMessageView = inflater.inflate(R.layout.item_message_sent, parent, false);
                return new MyMessageViewHolder(myMessageView);
            case VIEW_TYPE_OTHER_MESSAGE:
                View otherMessageView = inflater.inflate(R.layout.item_message_received_group, parent, false);
                return new OtherMessageViewHolder(otherMessageView);
            case VIEW_TYPE_SYSTEM_MESSAGE:
                View systemMessageView = inflater.inflate(R.layout.item_message_system, parent, false);
                return new SystemMessageViewHolder(systemMessageView);
            default:
                View defaultView = inflater.inflate(R.layout.item_message_received_group, parent, false);
                return new OtherMessageViewHolder(defaultView);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        
        if (holder instanceof MyMessageViewHolder) {
            ((MyMessageViewHolder) holder).bind(message);
        } else if (holder instanceof OtherMessageViewHolder) {
            ((OtherMessageViewHolder) holder).bind(message);
        } else if (holder instanceof SystemMessageViewHolder) {
            ((SystemMessageViewHolder) holder).bind(message);
        }
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        
        if (message.getType() == Message.MessageType.SYSTEM) {
            return VIEW_TYPE_SYSTEM_MESSAGE;
        } else if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_MY_MESSAGE;
        } else {
            return VIEW_TYPE_OTHER_MESSAGE;
        }
    }
    
    public void setMessages(List<Message> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void addMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }
    
    public void updateMessage(Message updatedMessage) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getMessageId().equals(updatedMessage.getMessageId())) {
                messages.set(i, updatedMessage);
                notifyItemChanged(i);
                break;
            }
        }
    }
    
    // My Message ViewHolder
    class MyMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMessage;
        private TextView tvTime;
        private TextView tvStatus;
        private ImageView ivMedia;
        private View mediaContainer;
        
        public MyMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            ivMedia = itemView.findViewById(R.id.ivMedia);
            mediaContainer = itemView.findViewById(R.id.mediaContainer);
            
            setupClickListeners();
        }
        
        private void setupClickListeners() {
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMessageClick(messages.get(position));
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMessageLongClick(messages.get(position), v);
                    return true;
                }
                return false;
            });
            
            if (mediaContainer != null) {
                mediaContainer.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        listener.onMediaClick(messages.get(position));
                    }
                });
            }
        }
        
        public void bind(Message message) {
            // Set message content
            if (message.getType() == Message.MessageType.TEXT) {
                tvMessage.setVisibility(View.VISIBLE);
                tvMessage.setText(message.getContent());
                if (mediaContainer != null) {
                    mediaContainer.setVisibility(View.GONE);
                }
            } else {
                tvMessage.setVisibility(View.GONE);
                if (mediaContainer != null) {
                    mediaContainer.setVisibility(View.VISIBLE);
                    setupMediaContent(message);
                }
            }
            
            // Set time
            if (message.getCreatedAt() != null) {
                tvTime.setText(timeFormat.format(message.getCreatedAt().toDate()));
            }
            
            // Set status
            if (message.getStatus() != null) {
                switch (message.getStatus()) {
                    case SENDING:
                        tvStatus.setText("⏳");
                        break;
                    case SENT:
                        tvStatus.setText("✓");
                        break;
                    case DELIVERED:
                        tvStatus.setText("✓✓");
                        break;
                    case READ:
                        tvStatus.setText("✓✓");
                        tvStatus.setTextColor(itemView.getContext().getColor(R.color.blue));
                        break;
                    case FAILED:
                        tvStatus.setText("❌");
                        break;
                }
            }
            
            // Show edited indicator
            if (message.isEdited()) {
                tvMessage.setText(tvMessage.getText() + " (edited)");
            }
        }
        
        private void setupMediaContent(Message message) {
            if (ivMedia != null) {
                switch (message.getType()) {
                    case IMAGE:
                        Glide.with(itemView.getContext())
                                .load(message.getMediaUrl())
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_image_placeholder)
                                .into(ivMedia);
                        break;
                    case VIDEO:
                        ivMedia.setImageResource(R.drawable.ic_video);
                        break;
                    case PDF:
                        ivMedia.setImageResource(R.drawable.ic_pdf);
                        break;
                    case DOCUMENT:
                        ivMedia.setImageResource(R.drawable.ic_document);
                        break;
                }
            }
        }
    }
    
    // Other Message ViewHolder
    class OtherMessageViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivProfile;
        private TextView tvSenderName;
        private TextView tvMessage;
        private TextView tvTime;
        private ImageView ivMedia;
        private View mediaContainer;
        
        public OtherMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivMedia = itemView.findViewById(R.id.ivMedia);
            mediaContainer = itemView.findViewById(R.id.mediaContainer);
            
            setupClickListeners();
        }
        
        private void setupClickListeners() {
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMessageClick(messages.get(position));
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMessageLongClick(messages.get(position), v);
                    return true;
                }
                return false;
            });
            
            if (mediaContainer != null) {
                mediaContainer.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        listener.onMediaClick(messages.get(position));
                    }
                });
            }
        }
        
        public void bind(Message message) {
            // Set sender info
            tvSenderName.setText(message.getSenderName());
            
            // Set profile image
            if (message.getSenderPhotoUrl() != null && !message.getSenderPhotoUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(message.getSenderPhotoUrl())
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(ivProfile);
            } else {
                ivProfile.setImageResource(R.drawable.ic_profile_placeholder);
            }
            
            // Set message content
            if (message.getType() == Message.MessageType.TEXT) {
                tvMessage.setVisibility(View.VISIBLE);
                tvMessage.setText(message.getContent());
                if (mediaContainer != null) {
                    mediaContainer.setVisibility(View.GONE);
                }
            } else {
                tvMessage.setVisibility(View.GONE);
                if (mediaContainer != null) {
                    mediaContainer.setVisibility(View.VISIBLE);
                    setupMediaContent(message);
                }
            }
            
            // Set time
            if (message.getCreatedAt() != null) {
                tvTime.setText(timeFormat.format(message.getCreatedAt().toDate()));
            }
            
            // Show edited indicator
            if (message.isEdited()) {
                tvMessage.setText(tvMessage.getText() + " (edited)");
            }
        }
        
        private void setupMediaContent(Message message) {
            if (ivMedia != null) {
                switch (message.getType()) {
                    case IMAGE:
                        Glide.with(itemView.getContext())
                                .load(message.getMediaUrl())
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_image_placeholder)
                                .into(ivMedia);
                        break;
                    case VIDEO:
                        ivMedia.setImageResource(R.drawable.ic_video);
                        break;
                    case PDF:
                        ivMedia.setImageResource(R.drawable.ic_pdf);
                        break;
                    case DOCUMENT:
                        ivMedia.setImageResource(R.drawable.ic_document);
                        break;
                }
            }
        }
    }
    
    // System Message ViewHolder
    class SystemMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSystemMessage;
        private TextView tvTime;
        
        public SystemMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSystemMessage = itemView.findViewById(R.id.tvSystemMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
        
        public void bind(Message message) {
            tvSystemMessage.setText(message.getContent());
            
            if (message.getCreatedAt() != null) {
                tvTime.setText(timeFormat.format(message.getCreatedAt().toDate()));
            }
        }
    }
} 