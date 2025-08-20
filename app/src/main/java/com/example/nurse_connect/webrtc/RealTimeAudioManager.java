package com.example.nurse_connect.webrtc;

import android.content.Context;
import android.util.Log;

/**
 * WebRTC-based Real-time Audio Manager
 * This class wraps the WebRTCManager to provide the same interface as the old RealTimeAudioManager
 * but uses WebRTC for actual real-time audio communication
 */
public class RealTimeAudioManager implements WebRTCManager.WebRTCListener {
    private static final String TAG = "RealTimeAudioManager";

    private Context context;
    private WebRTCManager webRTCManager;
    private AudioListener listener;

    private String callId;
    private String localUserId;
    private String remoteUserId;
    private boolean isInitiator = false;

    public interface AudioListener {
        void onCallConnected();
        void onCallDisconnected();
        void onAudioStarted();
        void onAudioStopped();
        void onError(String error);
    }

    public RealTimeAudioManager(Context context, AudioListener listener) {
        this.context = context;
        this.listener = listener;

        // Initialize WebRTC manager
        webRTCManager = new WebRTCManager(context, this);

        Log.d(TAG, "RealTimeAudioManager initialized with WebRTC");
    }

    /**
     * Set call parameters for WebRTC connection
     */
    public void setCallParameters(String callId, String localUserId, String remoteUserId, boolean isInitiator) {
        this.callId = callId;
        this.localUserId = localUserId;
        this.remoteUserId = remoteUserId;
        this.isInitiator = isInitiator;

        Log.d(TAG, "Call parameters set - CallId: " + callId + ", IsInitiator: " + isInitiator);
    }

    public void startCall() {
        if (callId == null || localUserId == null || remoteUserId == null) {
            Log.e(TAG, "Call parameters not set. Call setCallParameters() first.");
            if (listener != null) {
                listener.onError("Call parameters not set");
            }
            return;
        }

        try {
            Log.d(TAG, "Starting WebRTC call");

            if (isInitiator) {
                // Start call as initiator
                webRTCManager.startCall(callId, localUserId, remoteUserId);
            } else {
                // Answer incoming call
                webRTCManager.answerCall(callId, localUserId, remoteUserId);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to start WebRTC call", e);
            if (listener != null) {
                listener.onError("Failed to start call: " + e.getMessage());
            }
        }
    }

    public void toggleMute(boolean mute) {
        if (webRTCManager != null) {
            webRTCManager.toggleMute(mute);
            Log.d(TAG, "Audio " + (mute ? "muted" : "unmuted"));
        }
    }

    public void toggleSpeaker(boolean speakerOn) {
        if (webRTCManager != null) {
            webRTCManager.toggleSpeaker(speakerOn);
            Log.d(TAG, "Speaker " + (speakerOn ? "on" : "off"));
        }
    }

    public void endCall() {
        Log.d(TAG, "Ending WebRTC call");

        if (webRTCManager != null) {
            webRTCManager.endCall();
        }

        // Reset call parameters
        callId = null;
        localUserId = null;
        remoteUserId = null;
        isInitiator = false;
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (webRTCManager != null) {
            webRTCManager.cleanup();
            webRTCManager = null;
        }
        Log.d(TAG, "RealTimeAudioManager cleaned up");
    }

    // WebRTCManager.WebRTCListener implementation
    @Override
    public void onCallConnected() {
        Log.d(TAG, "WebRTC call connected");
        if (listener != null) {
            listener.onCallConnected();
        }
    }

    @Override
    public void onCallDisconnected() {
        Log.d(TAG, "WebRTC call disconnected");
        if (listener != null) {
            listener.onCallDisconnected();
        }
    }

    @Override
    public void onAudioStarted() {
        Log.d(TAG, "WebRTC audio started");
        if (listener != null) {
            listener.onAudioStarted();
        }
    }

    @Override
    public void onAudioStopped() {
        Log.d(TAG, "WebRTC audio stopped");
        if (listener != null) {
            listener.onAudioStopped();
        }
    }

    @Override
    public void onError(String error) {
        Log.e(TAG, "WebRTC error: " + error);
        if (listener != null) {
            listener.onError(error);
        }
    }

    @Override
    public void onRemoteAudioReceived() {
        Log.d(TAG, "Remote audio stream received");
        // This indicates that we're receiving audio from the remote peer
    }
}
