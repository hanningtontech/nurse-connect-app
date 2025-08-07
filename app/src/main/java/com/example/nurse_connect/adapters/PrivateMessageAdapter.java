package com.example.nurse_connect.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nurse_connect.R;
import com.example.nurse_connect.models.Message;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PrivateMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final String TAG = "PrivateMessageAdapter";
    private static final int VIEW_TYPE_MY_MESSAGE = 1;
    private static final int VIEW_TYPE_OTHER_MESSAGE = 2;
    private static final int VIEW_TYPE_INVITATION = 3;
    
    private List<Message> messages = new ArrayList<>();
    private String currentUserId;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private SimpleDateFormat debugFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    
    public PrivateMessageAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        Log.d(TAG, "Adapter initialized with currentUserId: " + currentUserId);
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
                View otherMessageView = inflater.inflate(R.layout.item_message_received, parent, false);
                return new OtherMessageViewHolder(otherMessageView);
            case VIEW_TYPE_INVITATION:
                View invitationView = inflater.inflate(R.layout.item_group_invitation_message, parent, false);
                return new InvitationMessageViewHolder(invitationView);
            default:
                View defaultView = inflater.inflate(R.layout.item_message_received, parent, false);
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
        } else if (holder instanceof InvitationMessageViewHolder) {
            ((InvitationMessageViewHolder) holder).bind(message);
        }
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);

        // Check if it's an invitation message
        if ("group_invitation".equals(message.getMessageType()) ||
            (message.getInvitationData() != null && !message.getInvitationData().isEmpty())) {
            return VIEW_TYPE_INVITATION;
        }

        // Regular message logic
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_MY_MESSAGE;
        } else {
            return VIEW_TYPE_OTHER_MESSAGE;
        }
    }
    

    
    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }
    
    public void addMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }
    
    public void updateMessageStatus(String messageId, Message.MessageStatus status) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getMessageId().equals(messageId)) {
                messages.get(i).setStatus(status);
                notifyItemChanged(i);
                break;
            }
        }
    }
    
    class MyMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMessage;
        private TextView tvTime;
        private ImageView ivStatus;
        
        public MyMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivStatus = itemView.findViewById(R.id.ivStatus);
        }
        
        public void bind(Message message) {
            tvMessage.setText(message.getContent());
            
            // Fix timestamp display
            Timestamp timestamp = message.getCreatedAt();
            if (timestamp != null) {
                Date date = timestamp.toDate();
                String timeString = timeFormat.format(date);
                String debugTimeString = debugFormat.format(date);
                tvTime.setText(timeString);
                Log.d(TAG, "My message time: " + timeString + " (debug: " + debugTimeString + ") for timestamp: " + timestamp);
            } else {
                tvTime.setText("");
                Log.w(TAG, "My message has null timestamp");
            }
            
            // Update status indicator
            switch (message.getStatus()) {
                case SENDING:
                    ivStatus.setImageResource(R.drawable.ic_single_check);
                    ivStatus.setColorFilter(itemView.getContext().getColor(android.R.color.darker_gray));
                    break;
                case SENT:
                    ivStatus.setImageResource(R.drawable.ic_single_check);
                    ivStatus.setColorFilter(itemView.getContext().getColor(android.R.color.darker_gray));
                    break;
                case DELIVERED:
                    ivStatus.setImageResource(R.drawable.ic_double_check);
                    ivStatus.setColorFilter(itemView.getContext().getColor(android.R.color.darker_gray));
                    break;
                case READ:
                    ivStatus.setImageResource(R.drawable.ic_double_check);
                    ivStatus.setColorFilter(itemView.getContext().getColor(android.R.color.holo_blue_light));
                    break;
                case FAILED:
                    ivStatus.setImageResource(R.drawable.ic_single_check);
                    ivStatus.setColorFilter(itemView.getContext().getColor(android.R.color.holo_red_light));
                    break;
            }
        }
    }
    
    class OtherMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMessage;
        private TextView tvTime;
        
        public OtherMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
        
        public void bind(Message message) {
            tvMessage.setText(message.getContent());
            
            // Fix timestamp display
            Timestamp timestamp = message.getCreatedAt();
            if (timestamp != null) {
                Date date = timestamp.toDate();
                String timeString = timeFormat.format(date);
                String debugTimeString = debugFormat.format(date);
                tvTime.setText(timeString);
                Log.d(TAG, "Other message time: " + timeString + " (debug: " + debugTimeString + ") for timestamp: " + timestamp);
            } else {
                tvTime.setText("");
                Log.w(TAG, "Other message has null timestamp");
            }
        }
    }
} 