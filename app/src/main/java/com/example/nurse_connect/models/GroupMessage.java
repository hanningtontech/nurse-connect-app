package com.example.nurse_connect.models;

import java.util.Date;

public class GroupMessage {
    private String messageId;
    private String senderId;
    private String senderName;
    private String senderPhotoUrl;
    private String groupId;
    private String content;
    private Date timestamp;
    private String messageType;

    // Default constructor required for Firestore
    public GroupMessage() {}

    public GroupMessage(String senderId, String senderName, String groupId, String content) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.groupId = groupId;
        this.content = content;
        this.timestamp = new Date();
        this.messageType = "text";
    }

    // Getters and Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderPhotoUrl() { return senderPhotoUrl; }
    public void setSenderPhotoUrl(String senderPhotoUrl) { this.senderPhotoUrl = senderPhotoUrl; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
}
