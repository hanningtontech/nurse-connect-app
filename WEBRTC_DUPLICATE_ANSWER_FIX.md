# WebRTC Duplicate Answer Processing Fix

## Issue
The logcat showed recurring errors:
```
WebRTCManager: Failed to set remote description: Failed to set remote answer sdp: Called in wrong state: stable
```

This error occurs when the WebRTC `PeerConnection` is already in `STABLE` signaling state and receives a duplicate `answer` message, causing the `setRemoteDescription` method to fail.

## Root Cause
The `SignalingManager` was processing all incoming `answer` messages without checking the current state of the `PeerConnection`. When duplicate answer messages arrive (which can happen due to network conditions or retransmissions), the second answer would be processed even though the connection was already established.

## Solution
Modified `SignalingManager` to check the `PeerConnection` signaling state before processing incoming `answer` messages:

### Changes Made

1. **Added WebRTCManager reference to SignalingManager**:
   ```java
   private WebRTCManager webRTCManager; // Reference to check peer connection state
   ```

2. **Updated SignalingManager constructor**:
   ```java
   public SignalingManager(String callId, String localUserId, String remoteUserId, 
                          SignalingListener listener, WebRTCManager webRTCManager)
   ```

3. **Added state check in answer processing**:
   ```java
   case "answer":
       String answerSdp = doc.getString("sdp");
       if (answerSdp != null && listener != null) {
           // Check if peer connection is already in STABLE state to prevent duplicate answer processing
           if (webRTCManager != null && webRTCManager.getPeerConnection() != null &&
               webRTCManager.getPeerConnection().getSignalingState() == org.webrtc.PeerConnection.SignalingState.STABLE) {
               Log.d(TAG, "Ignoring duplicate answer: Peer connection already in STABLE state");
               // Delete the duplicate message and return
               doc.getReference().delete()
                       .addOnSuccessListener(aVoid -> Log.d(TAG, "Duplicate answer message deleted"))
                       .addOnFailureListener(e -> Log.w(TAG, "Failed to delete duplicate answer", e));
               return;
           }
           
           SessionDescription answer = new SessionDescription(SessionDescription.Type.ANSWER, answerSdp);
           listener.onAnswerReceived(answer);
       }
       break;
   ```

4. **Added getter method to WebRTCManager**:
   ```java
   public PeerConnection getPeerConnection() {
       return peerConnection;
   }
   ```

5. **Updated WebRTCManager to pass itself to SignalingManager**:
   ```java
   signalingManager = new SignalingManager(callId, localUserId, remoteUserId, this, this);
   ```

## Expected Result
- Eliminates the `Called in wrong state: stable` errors
- Improves call stability by preventing duplicate answer processing
- Maintains clean signaling collection by deleting duplicate messages
- Preserves normal call flow for legitimate answer messages

## Testing
After this fix, the logcat should show:
- No more `Failed to set remote description: Called in wrong state: stable` errors
- `Ignoring duplicate answer: Peer connection already in STABLE state` when duplicates are detected
- `Duplicate answer message deleted` when duplicates are cleaned up
- Normal call establishment flow continues uninterrupted
