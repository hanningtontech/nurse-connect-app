package com.example.nurse_connect.models;

import com.google.firebase.Timestamp;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class PrivateChat {
    private String chatId;
    private List<String> participants;
    private String lastMessage;
    private String lastMessageSenderId;
    private Date lastMessageTime;
    private Date updatedAt;
    private int unreadCount;
    private Map<String, Object> unreadCounts;
    private Boolean isPinned;
    private Boolean isMuted;

    // Default constructor for Firestore
    public PrivateChat() {}

    public PrivateChat(List<String> participants, String lastMessage, Date lastMessageTime) {
        this.participants = participants;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.updatedAt = new Date();
        this.unreadCount = 0;
        this.isPinned = false;
        this.isMuted = false;
    }

    // Getters and Setters
    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMessageSenderId() {
        return lastMessageSenderId;
    }

    public void setLastMessageSenderId(String lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }

    public Date getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Date lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
    
    public Map<String, Object> getUnreadCounts() {
        return unreadCounts;
    }
    
    public void setUnreadCounts(Map<String, Object> unreadCounts) {
        this.unreadCounts = unreadCounts;
    }
    
    public int getUnreadCountForUser(String userId) {
        if (unreadCounts != null && unreadCounts.containsKey(userId)) {
            Object value = unreadCounts.get(userId);
            if (value instanceof Long) {
                return ((Long) value).intValue();
            } else if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    public Boolean isPinned() {
        return isPinned;
    }

    public void setPinned(Boolean pinned) {
        isPinned = pinned;
    }

    public Boolean isMuted() {
        return isMuted;
    }

    public void setMuted(Boolean muted) {
        isMuted = muted;
    }
} 