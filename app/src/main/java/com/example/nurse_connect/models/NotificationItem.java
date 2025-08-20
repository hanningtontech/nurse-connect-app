package com.example.nurse_connect.models;

import com.google.firebase.Timestamp;
import java.io.Serializable;

public class NotificationItem implements Serializable {
    private String id;
    private String type; // "message", "follow", "task", "suggestion", "like", "comment"
    private String title;
    private String message;
    private String fromUserId;
    private String fromUserName;
    private String fromUserPhotoUrl;
    private String targetId; // ID of the related content (chat, task, etc.)
    private Object timestamp; // Can be either long or Timestamp
    private boolean isRead;
    private String actionText; // "Reply", "View", "Accept", etc.

    // Additional fields for compatibility with existing chat notifications
    private String recipientId;
    private String senderId;
    private String senderName;
    private String chatId;

    public NotificationItem() {}

    public NotificationItem(String id, String type, String title, String message, 
                           String fromUserId, String fromUserName) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getFromUserName() { return fromUserName; }
    public void setFromUserName(String fromUserName) { this.fromUserName = fromUserName; }

    public String getFromUserPhotoUrl() { return fromUserPhotoUrl; }
    public void setFromUserPhotoUrl(String fromUserPhotoUrl) { this.fromUserPhotoUrl = fromUserPhotoUrl; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public long getTimestamp() {
        if (timestamp instanceof Timestamp) {
            return ((Timestamp) timestamp).toDate().getTime();
        } else if (timestamp instanceof Long) {
            return (Long) timestamp;
        } else if (timestamp instanceof Number) {
            return ((Number) timestamp).longValue();
        }
        return System.currentTimeMillis(); // fallback
    }

    public void setTimestamp(Object timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public void setIsRead(boolean read) { isRead = read; } // For Firestore compatibility

    public String getActionText() { return actionText; }
    public void setActionText(String actionText) { this.actionText = actionText; }

    // Compatibility fields for existing chat notifications
    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    // Helper methods
    public String getTimeAgo() {
        long now = System.currentTimeMillis();
        long timestampLong = getTimestamp(); // Use the getter method
        long diff = now - timestampLong;
        
        if (diff < 60000) { // Less than 1 minute
            return "Just now";
        } else if (diff < 3600000) { // Less than 1 hour
            return (diff / 60000) + "m ago";
        } else if (diff < 86400000) { // Less than 1 day
            return (diff / 3600000) + "h ago";
        } else { // More than 1 day
            return (diff / 86400000) + "d ago";
        }
    }

    public int getIconResource() {
        switch (type) {
            case "message":
                return com.example.nurse_connect.R.drawable.ic_chat;
            case "follow":
                return com.example.nurse_connect.R.drawable.ic_person_add;
            case "task":
                return com.example.nurse_connect.R.drawable.ic_task;
            case "suggestion":
                return com.example.nurse_connect.R.drawable.ic_lightbulb;
            case "like":
                return com.example.nurse_connect.R.drawable.ic_favorite;
            case "comment":
                return com.example.nurse_connect.R.drawable.ic_comment;
            default:
                return com.example.nurse_connect.R.drawable.ic_notifications;
        }
    }
}
