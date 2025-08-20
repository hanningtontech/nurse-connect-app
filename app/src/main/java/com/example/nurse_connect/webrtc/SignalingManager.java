package com.example.nurse_connect.webrtc;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages WebRTC signaling through Firebase Firestore
 * Handles exchange of offers, answers, and ICE candidates between peers
 */
public class SignalingManager {
    private static final String TAG = "SignalingManager";
    
    private FirebaseFirestore db;
    private String callId;
    private String localUserId;
    private String remoteUserId;
    private SignalingListener listener;
    private ListenerRegistration signalingListener;
    private Gson gson;
    private WebRTCManager webRTCManager; // Reference to check peer connection state
    
    public interface SignalingListener {
        void onOfferReceived(SessionDescription offer);
        void onAnswerReceived(SessionDescription answer);
        void onIceCandidateReceived(IceCandidate candidate);
        void onCallEnded();
        void onSignalingError(String error);
    }
    
    public SignalingManager(String callId, String localUserId, String remoteUserId, SignalingListener listener, WebRTCManager webRTCManager) {
        this.db = FirebaseFirestore.getInstance();
        this.callId = callId;
        this.localUserId = localUserId;
        this.remoteUserId = remoteUserId;
        this.listener = listener;
        this.webRTCManager = webRTCManager;
        this.gson = new Gson();
        
        Log.d(TAG, "SignalingManager created for call: " + callId);
    }
    
    /**
     * Start listening for signaling messages
     */
    public void startListening() {
        Log.d(TAG, "Starting signaling listener for call: " + callId);
        
        signalingListener = db.collection("calls")
                .document(callId)
                .collection("signaling")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Signaling listener error", error);
                        if (listener != null) {
                            listener.onSignalingError("Signaling error: " + error.getMessage());
                        }
                        return;
                    }
                    
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            processSignalingMessage(doc);
                        }
                    }
                });
    }
    
    /**
     * Stop listening for signaling messages
     */
    public void stopListening() {
        if (signalingListener != null) {
            signalingListener.remove();
            signalingListener = null;
        }
        Log.d(TAG, "Stopped signaling listener");
    }
    
    /**
     * Send WebRTC offer to remote peer
     */
    public void sendOffer(SessionDescription offer) {
        Log.d(TAG, "Sending offer to remote peer");
        
        Map<String, Object> offerData = new HashMap<>();
        offerData.put("type", "offer");
        offerData.put("from", localUserId);
        offerData.put("to", remoteUserId);
        offerData.put("sdp", offer.description);
        offerData.put("timestamp", System.currentTimeMillis());
        
        db.collection("calls")
                .document(callId)
                .collection("signaling")
                .add(offerData)
                .addOnSuccessListener(documentReference -> 
                    Log.d(TAG, "Offer sent successfully"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send offer", e);
                    if (listener != null) {
                        listener.onSignalingError("Failed to send offer: " + e.getMessage());
                    }
                });
    }
    
    /**
     * Send WebRTC answer to remote peer
     */
    public void sendAnswer(SessionDescription answer) {
        Log.d(TAG, "Sending answer to remote peer");
        
        Map<String, Object> answerData = new HashMap<>();
        answerData.put("type", "answer");
        answerData.put("from", localUserId);
        answerData.put("to", remoteUserId);
        answerData.put("sdp", answer.description);
        answerData.put("timestamp", System.currentTimeMillis());
        
        db.collection("calls")
                .document(callId)
                .collection("signaling")
                .add(answerData)
                .addOnSuccessListener(documentReference -> 
                    Log.d(TAG, "Answer sent successfully"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send answer", e);
                    if (listener != null) {
                        listener.onSignalingError("Failed to send answer: " + e.getMessage());
                    }
                });
    }
    
    /**
     * Send ICE candidate to remote peer
     */
    public void sendIceCandidate(IceCandidate candidate) {
        Log.d(TAG, "Sending ICE candidate to remote peer");
        
        Map<String, Object> candidateData = new HashMap<>();
        candidateData.put("type", "ice-candidate");
        candidateData.put("from", localUserId);
        candidateData.put("to", remoteUserId);
        candidateData.put("candidate", candidate.sdp);
        candidateData.put("sdpMid", candidate.sdpMid);
        candidateData.put("sdpMLineIndex", candidate.sdpMLineIndex);
        candidateData.put("timestamp", System.currentTimeMillis());
        
        db.collection("calls")
                .document(callId)
                .collection("signaling")
                .add(candidateData)
                .addOnSuccessListener(documentReference -> 
                    Log.d(TAG, "ICE candidate sent successfully"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send ICE candidate", e);
                    if (listener != null) {
                        listener.onSignalingError("Failed to send ICE candidate: " + e.getMessage());
                    }
                });
    }
    
    /**
     * Process incoming signaling messages
     */
    private void processSignalingMessage(DocumentSnapshot doc) {
        try {
            String type = doc.getString("type");
            String from = doc.getString("from");
            String to = doc.getString("to");
            
            // Only process messages intended for this user
            if (!localUserId.equals(to) || localUserId.equals(from)) {
                return;
            }
            
            Log.d(TAG, "Processing signaling message: " + type + " from " + from);
            
            switch (type) {
                case "offer":
                    String offerSdp = doc.getString("sdp");
                    if (offerSdp != null && listener != null) {
                        SessionDescription offer = new SessionDescription(SessionDescription.Type.OFFER, offerSdp);
                        listener.onOfferReceived(offer);
                    }
                    break;
                    
                case "answer":
                    String answerSdp = doc.getString("sdp");
                    if (answerSdp != null && listener != null) {
                        // Process the answer
                        SessionDescription answer = new SessionDescription(SessionDescription.Type.ANSWER, answerSdp);
                        listener.onAnswerReceived(answer);
                    }
                    break;
                    
                case "ice-candidate":
                    String candidate = doc.getString("candidate");
                    String sdpMid = doc.getString("sdpMid");
                    Long sdpMLineIndex = doc.getLong("sdpMLineIndex");
                    
                    if (candidate != null && sdpMid != null && sdpMLineIndex != null && listener != null) {
                        IceCandidate iceCandidate = new IceCandidate(sdpMid, sdpMLineIndex.intValue(), candidate);
                        listener.onIceCandidateReceived(iceCandidate);
                    }
                    break;
                    
                case "end-call":
                    Log.d(TAG, "Call end signal received from remote peer");
                    if (listener != null) {
                        listener.onCallEnded();
                    }
                    break;
                    
                default:
                    Log.w(TAG, "Unknown signaling message type: " + type);
                    break;
            }
            
            // Delete processed message to keep collection clean
            doc.getReference().delete()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Processed signaling message deleted"))
                    .addOnFailureListener(e -> Log.w(TAG, "Failed to delete processed message", e));
                    
        } catch (Exception e) {
            Log.e(TAG, "Error processing signaling message", e);
            if (listener != null) {
                listener.onSignalingError("Error processing signaling message: " + e.getMessage());
            }
        }
    }
    
    /**
     * Send call end signal to remote peer
     */
    public void sendCallEnd() {
        Log.d(TAG, "Sending call end signal");
        
        Map<String, Object> endData = new HashMap<>();
        endData.put("type", "end-call");
        endData.put("from", localUserId);
        endData.put("to", remoteUserId);
        endData.put("timestamp", System.currentTimeMillis());
        
        db.collection("calls")
                .document(callId)
                .collection("signaling")
                .add(endData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Call end signal sent successfully");
                    // Also update the main call document status
                    updateCallStatus("ended");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send call end signal", e);
                    // Still try to update the main call document
                    updateCallStatus("ended");
                });
    }
    
    /**
     * Update the main call document status
     */
    private void updateCallStatus(String status) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("status", status);
        updateData.put("endTime", System.currentTimeMillis());
        
        db.collection("calls")
                .document(callId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Call status updated to: " + status))
                .addOnFailureListener(e -> Log.w(TAG, "Failed to update call status", e));
    }
    
    /**
     * Clean up signaling data for this call
     */
    public void cleanup() {
        stopListening();
        
        // Clean up signaling collection
        db.collection("calls")
                .document(callId)
                .collection("signaling")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }
                    Log.d(TAG, "Signaling data cleaned up");
                })
                .addOnFailureListener(e -> 
                    Log.w(TAG, "Failed to clean up signaling data", e));
    }
}
