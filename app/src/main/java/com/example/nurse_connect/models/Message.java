package com.example.nurse_connect.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class Message {
    @DocumentId
    private String messageId = "";
    private String senderId = "";
    private String senderName = "";
    private String senderPhotoUrl = "";
    private String roomId = "";
    private String content = "";
    private String mediaUrl = "";
    private String mediaType = ""; // "image", "video", "pdf", "document"
    private String fileName = "";
    private long fileSize = 0;
    private List<String> mentionedUsers = null;
    private boolean isEdited = false;
    private Timestamp editedAt = null;
    private Timestamp createdAt = Timestamp.now();
    private Timestamp readAt = null;
    private MessageStatus status = MessageStatus.SENT;
    private MessageType type = MessageType.TEXT;
    
    // For group messages
    private String groupId = "";
    private String groupName = "";
    
    // For direct messages
    private String recipientId = "";
    
    // For global chat
    private String channelId = "";
    private String channelName = "";

    // For private messages (alternative to roomId)
    private String chatId = "";
    private String receiverId = "";
    private Date timestamp;

    // For group invitations
    private String messageType = "text"; // "text", "group_invitation", "system"
    private Map<String, Object> invitationData;

    public enum MessageStatus {
        SENDING, SENT, DELIVERED, READ, FAILED
    }

    public enum MessageType {
        TEXT, IMAGE, VIDEO, PDF, DOCUMENT, SYSTEM, TASK, GROUP_INVITATION
    }

    // Default constructor for Firestore
    public Message() {}

    public Message(String senderId, String content) {
        this.senderId = senderId;
        this.content = content;
        this.type = MessageType.TEXT;
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

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public List<String> getMentionedUsers() { return mentionedUsers; }
    public void setMentionedUsers(List<String> mentionedUsers) { this.mentionedUsers = mentionedUsers; }

    public boolean isEdited() { return isEdited; }
    public void setEdited(boolean edited) { isEdited = edited; }

    public Timestamp getEditedAt() { return editedAt; }
    public void setEditedAt(Timestamp editedAt) { this.editedAt = editedAt; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getReadAt() { return readAt; }
    public void setReadAt(Timestamp readAt) { this.readAt = readAt; }

    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }

    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public Map<String, Object> getInvitationData() { return invitationData; }
    public void setInvitationData(Map<String, Object> invitationData) { this.invitationData = invitationData; }
} 