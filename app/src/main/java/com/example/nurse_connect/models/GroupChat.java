package com.example.nurse_connect.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupChat {
    private String groupId;
    private String title;
    private String description;
    private String createdBy;
    private List<String> members;
    private List<String> admins;
    private List<String> pendingInvitations;
    private boolean isPublic;
    private String lastMessage;
    private String lastMessageSenderId;
    private Date lastMessageTime;
    private Date createdAt;
    private Date updatedAt;
    private Map<String, Object> unreadCounts;
    private String groupPhotoURL;
    private int maxMembers;

    // Default constructor required for Firestore
    public GroupChat() {
        this.members = new ArrayList<>();
        this.admins = new ArrayList<>();
        this.pendingInvitations = new ArrayList<>();
        this.unreadCounts = new HashMap<>();
        this.maxMembers = 100; // Default max members
    }

    public GroupChat(String title, String description, String createdBy, boolean isPublic) {
        this();
        this.title = title;
        this.description = description;
        this.createdBy = createdBy;
        this.isPublic = isPublic;
        this.createdAt = new Date();
        this.updatedAt = new Date();
        
        // Creator is automatically a member and admin
        this.members.add(createdBy);
        this.admins.add(createdBy);
        
        // Initialize unread count for creator
        this.unreadCounts.put(createdBy, 0);
    }

    // Helper methods
    public boolean isMember(String userId) {
        return members != null && members.contains(userId);
    }

    public boolean isAdmin(String userId) {
        return admins != null && admins.contains(userId);
    }

    public boolean hasPendingInvitation(String userId) {
        return pendingInvitations != null && pendingInvitations.contains(userId);
    }

    public void addMember(String userId) {
        if (members == null) {
            members = new ArrayList<>();
        }
        if (!members.contains(userId)) {
            members.add(userId);
            // Initialize unread count for new member
            if (unreadCounts == null) {
                unreadCounts = new HashMap<>();
            }
            unreadCounts.put(userId, 0);
        }
    }

    public void removeMember(String userId) {
        if (members != null) {
            members.remove(userId);
        }
        if (unreadCounts != null) {
            unreadCounts.remove(userId);
        }
    }

    public void addAdmin(String userId) {
        if (admins == null) {
            admins = new ArrayList<>();
        }
        if (!admins.contains(userId)) {
            admins.add(userId);
        }
    }

    public void removeAdmin(String userId) {
        if (admins != null) {
            admins.remove(userId);
        }
    }

    public void addPendingInvitation(String userId) {
        if (pendingInvitations == null) {
            pendingInvitations = new ArrayList<>();
        }
        if (!pendingInvitations.contains(userId)) {
            pendingInvitations.add(userId);
        }
    }

    public void removePendingInvitation(String userId) {
        if (pendingInvitations != null) {
            pendingInvitations.remove(userId);
        }
    }

    public int getUnreadCountForUser(String userId) {
        if (unreadCounts != null && unreadCounts.containsKey(userId)) {
            Object count = unreadCounts.get(userId);
            if (count instanceof Number) {
                return ((Number) count).intValue();
            }
        }
        return 0;
    }

    public void setUnreadCountForUser(String userId, int count) {
        if (unreadCounts == null) {
            unreadCounts = new HashMap<>();
        }
        unreadCounts.put(userId, count);
    }

    // Getters and Setters
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }

    public List<String> getAdmins() { return admins; }
    public void setAdmins(List<String> admins) { this.admins = admins; }

    public List<String> getPendingInvitations() { return pendingInvitations; }
    public void setPendingInvitations(List<String> pendingInvitations) { this.pendingInvitations = pendingInvitations; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getLastMessageSenderId() { return lastMessageSenderId; }
    public void setLastMessageSenderId(String lastMessageSenderId) { this.lastMessageSenderId = lastMessageSenderId; }

    public Date getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(Date lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public Map<String, Object> getUnreadCounts() { return unreadCounts; }
    public void setUnreadCounts(Map<String, Object> unreadCounts) { this.unreadCounts = unreadCounts; }

    public String getGroupPhotoURL() { return groupPhotoURL; }
    public void setGroupPhotoURL(String groupPhotoURL) { this.groupPhotoURL = groupPhotoURL; }

    public int getMaxMembers() { return maxMembers; }
    public void setMaxMembers(int maxMembers) { this.maxMembers = maxMembers; }
}
