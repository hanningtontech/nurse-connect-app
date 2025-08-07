package com.example.nurse_connect.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.List;
import java.util.Map;

public class ChatRoom {
    @DocumentId
    private String roomId = "";
    private String name = "";
    private String description = "";
    private String photoUrl = "";
    private ChatRoomType type = ChatRoomType.DIRECT;
    private List<String> memberIds = null;
    private Map<String, MemberRole> memberRoles = null;
    private String createdBy = "";
    private Timestamp createdAt = Timestamp.now();
    private Timestamp lastMessageAt = Timestamp.now();
    private Message lastMessage = null;
    private int unreadCount = 0;
    private boolean isActive = true;
    private boolean isPrivate = false;
    private List<String> moderators = null;
    private List<String> admins = null;
    private Boolean isPinned = false;
    private Boolean isMuted = false;
    
    // For study groups
    private String studyTopic = "";
    private String institution = "";
    private int maxMembers = 50;
    private boolean requiresApproval = false;
    private List<String> pendingMembers = null;
    
    // For global channels
    private String channelCategory = "";
    private boolean isOfficial = false;
    private List<String> rules = null;

    public enum ChatRoomType {
        DIRECT, GROUP, STUDY_GROUP, GLOBAL_CHANNEL
    }
    
    public enum MemberRole {
        MEMBER, MODERATOR, ADMIN, OWNER
    }

    // Default constructor for Firestore
    public ChatRoom() {}

    public ChatRoom(String name, ChatRoomType type) {
        this.name = name;
        this.type = type;
    }

    // Getters and Setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public ChatRoomType getType() { return type; }
    public void setType(ChatRoomType type) { this.type = type; }

    public List<String> getMemberIds() { return memberIds; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }

    public Map<String, MemberRole> getMemberRoles() { return memberRoles; }
    public void setMemberRoles(Map<String, MemberRole> memberRoles) { this.memberRoles = memberRoles; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Timestamp lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public Message getLastMessage() { return lastMessage; }
    public void setLastMessage(Message lastMessage) { this.lastMessage = lastMessage; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean aPrivate) { isPrivate = aPrivate; }

    public List<String> getModerators() { return moderators; }
    public void setModerators(List<String> moderators) { this.moderators = moderators; }

    public List<String> getAdmins() { return admins; }
    public void setAdmins(List<String> admins) { this.admins = admins; }

    public Boolean isPinned() { return isPinned; }
    public void setPinned(Boolean pinned) { isPinned = pinned; }

    public Boolean isMuted() { return isMuted; }
    public void setMuted(Boolean muted) { isMuted = muted; }

    public String getStudyTopic() { return studyTopic; }
    public void setStudyTopic(String studyTopic) { this.studyTopic = studyTopic; }

    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }

    public int getMaxMembers() { return maxMembers; }
    public void setMaxMembers(int maxMembers) { this.maxMembers = maxMembers; }

    public boolean isRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }

    public List<String> getPendingMembers() { return pendingMembers; }
    public void setPendingMembers(List<String> pendingMembers) { this.pendingMembers = pendingMembers; }

    public String getChannelCategory() { return channelCategory; }
    public void setChannelCategory(String channelCategory) { this.channelCategory = channelCategory; }

    public boolean isOfficial() { return isOfficial; }
    public void setOfficial(boolean official) { isOfficial = official; }

    public List<String> getRules() { return rules; }
    public void setRules(List<String> rules) { this.rules = rules; }
} 