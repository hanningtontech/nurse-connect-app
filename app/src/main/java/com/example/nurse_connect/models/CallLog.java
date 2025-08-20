package com.example.nurse_connect.models;

public class CallLog {
    private String callId;
    private String otherUserId;
    private String otherUserName;
    private String otherPhotoUrl;
    private String callType;
    private String status;
    private long startTime;
    private Long endTime;
    private Long duration;
    private boolean isOutgoing;

    public CallLog(String callId, String otherUserId, String otherUserName, String otherPhotoUrl,
                   String callType, String status, long startTime, Long endTime, Long duration, boolean isOutgoing) {
        this.callId = callId;
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
        this.otherPhotoUrl = otherPhotoUrl;
        this.callType = callType;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.isOutgoing = isOutgoing;
    }

    // Getters
    public String getCallId() { return callId; }
    public String getOtherUserId() { return otherUserId; }
    public String getOtherUserName() { return otherUserName; }
    public String getOtherPhotoUrl() { return otherPhotoUrl; }
    public String getCallType() { return callType; }
    public String getStatus() { return status; }
    public long getStartTime() { return startTime; }
    public Long getEndTime() { return endTime; }
    public Long getDuration() { return duration; }
    public boolean isOutgoing() { return isOutgoing; }

    // Setters
    public void setCallId(String callId) { this.callId = callId; }
    public void setOtherUserId(String otherUserId) { this.otherUserId = otherUserId; }
    public void setOtherUserName(String otherUserName) { this.otherUserName = otherUserName; }
    public void setOtherPhotoUrl(String otherPhotoUrl) { this.otherPhotoUrl = otherPhotoUrl; }
    public void setCallType(String callType) { this.callType = callType; }
    public void setStatus(String status) { this.status = status; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }
    public void setDuration(Long duration) { this.duration = duration; }
    public void setOutgoing(boolean outgoing) { isOutgoing = outgoing; }

    // Helper methods
    public boolean isVideoCall() {
        return "video".equals(callType);
    }

    public boolean isAudioCall() {
        return "audio".equals(callType);
    }

    public boolean isCompleted() {
        return "ended".equals(status) || "connected".equals(status);
    }

    public boolean isMissed() {
        return "declined".equals(status) || "ended".equals(status) && duration != null && duration < 1000;
    }

    public String getFormattedDuration() {
        if (duration == null || duration <= 0) {
            return "0:00";
        }
        
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        return String.format("%d:%02d", minutes, seconds);
    }
}
