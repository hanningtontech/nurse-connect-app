package com.example.nurse_connect.models;

import java.util.Date;

public class GroupInvitation {
    private String invitationId;
    private String groupId;
    private String groupTitle;
    private String groupDescription;
    private boolean isGroupPublic;
    private String invitedBy;
    private String invitedByName;
    private String invitedUser;
    private Date createdAt;
    private Date expiresAt;
    private String status; // "pending", "accepted", "declined", "expired"
    private String message; // Optional invitation message

    // Default constructor required for Firestore
    public GroupInvitation() {}

    public GroupInvitation(String groupId, String groupTitle, String invitedBy, 
                          String invitedByName, String invitedUser) {
        this.groupId = groupId;
        this.groupTitle = groupTitle;
        this.invitedBy = invitedBy;
        this.invitedByName = invitedByName;
        this.invitedUser = invitedUser;
        this.createdAt = new Date();
        this.status = "pending";
        
        // Set expiration to 7 days from now
        this.expiresAt = new Date(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000));
    }

    // Helper methods
    public boolean isExpired() {
        return expiresAt != null && new Date().after(expiresAt);
    }

    public boolean isPending() {
        return "pending".equals(status) && !isExpired();
    }

    public boolean isAccepted() {
        return "accepted".equals(status);
    }

    public boolean isDeclined() {
        return "declined".equals(status);
    }

    // Getters and Setters
    public String getInvitationId() { return invitationId; }
    public void setInvitationId(String invitationId) { this.invitationId = invitationId; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getGroupTitle() { return groupTitle; }
    public void setGroupTitle(String groupTitle) { this.groupTitle = groupTitle; }

    public String getGroupDescription() { return groupDescription; }
    public void setGroupDescription(String groupDescription) { this.groupDescription = groupDescription; }

    public boolean isGroupPublic() { return isGroupPublic; }
    public void setGroupPublic(boolean groupPublic) { isGroupPublic = groupPublic; }

    public String getInvitedBy() { return invitedBy; }
    public void setInvitedBy(String invitedBy) { this.invitedBy = invitedBy; }

    public String getInvitedByName() { return invitedByName; }
    public void setInvitedByName(String invitedByName) { this.invitedByName = invitedByName; }

    public String getInvitedUser() { return invitedUser; }
    public void setInvitedUser(String invitedUser) { this.invitedUser = invitedUser; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
