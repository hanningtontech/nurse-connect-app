package com.example.nurse_connect.ui.chat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.nurse_connect.R;
import com.example.nurse_connect.utils.PermissionUtils;
import com.example.nurse_connect.webrtc.VideoWebRTCManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.webrtc.EglBase;
import org.webrtc.SurfaceViewRenderer;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class VideoCallActivity extends AppCompatActivity implements
        VideoWebRTCManager.VideoWebRTCListener {

    private SurfaceViewRenderer localVideoView;
    private SurfaceViewRenderer remoteVideoView;
    private CircleImageView userProfileImage;
    private TextView userName;
    private TextView callStatus;
    private TextView callDuration;
    private LinearLayout muteButton;
    private LinearLayout endCallButton;
    private LinearLayout speakerButton;
    private LinearLayout cameraSwitchButton;
    private LinearLayout incomingCallSection;
    private LinearLayout bottomSection;
    private LinearLayout acceptButton;
    private LinearLayout declineButton;
    private ImageView muteIcon;
    private ImageView speakerIcon;
    private ImageView cameraSwitchIcon;

    private String otherUserId;
    private String otherUserName;
    private boolean isOutgoing;
    private boolean isCallActive = false;
    private boolean isMuted = false;
    private boolean isSpeakerOn = false;
    private boolean isCameraOn = true;
    private boolean isFrontCamera = true;
    private long callStartTime;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private Handler callDurationHandler;
    private Runnable callDurationRunnable;
    private MediaPlayer ringtonePlayer;
    private AudioManager systemAudioManager;
    private String callId;
    private com.google.firebase.firestore.ListenerRegistration callStatusListener;

    // Video WebRTC components
    private VideoWebRTCManager videoWebRTCManager;
    private boolean isVideoInitialized = false;

    // Call timeout and waiting audio
    private static final long CALL_TIMEOUT_MS = 45000; // 45 seconds
    private Handler timeoutHandler;
    private Runnable timeoutRunnable;
    private MediaPlayer waitingAudioPlayer;

    // Call states
    public enum CallState {
        CALLING, RINGING, CONNECTED, ENDED
    }
    private CallState currentCallState = CallState.CALLING;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        // Keep screen on during call
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initializeViews();
        initializeFirebase();
        getIntentData();

        // Check WebRTC permissions before proceeding
        if (!PermissionUtils.hasVideoWebRTCPermissions(this)) {
            PermissionUtils.requestVideoWebRTCPermissions(this);
            return;
        }

        setupCallInterface();
        setupClickListeners();

        android.util.Log.d("VideoCallActivity", "Starting video call activity - isOutgoing: " + isOutgoing + ", callId: " + callId);

        if (isOutgoing) {
            initiateOutgoingCall();
        } else {
            setupIncomingCall();
        }
    }

    private void initializeViews() {
        localVideoView = findViewById(R.id.localVideoView);
        remoteVideoView = findViewById(R.id.remoteVideoView);
        userProfileImage = findViewById(R.id.userProfileImage);
        userName = findViewById(R.id.userName);
        callStatus = findViewById(R.id.callStatus);
        callDuration = findViewById(R.id.callDuration);
        muteButton = findViewById(R.id.muteButton);
        endCallButton = findViewById(R.id.endCallButton);
        speakerButton = findViewById(R.id.speakerButton);
        cameraSwitchButton = findViewById(R.id.cameraSwitchButton);
        incomingCallSection = findViewById(R.id.incomingCallSection);
        bottomSection = findViewById(R.id.bottomSection);
        acceptButton = findViewById(R.id.acceptButton);
        declineButton = findViewById(R.id.declineButton);
        muteIcon = findViewById(R.id.muteIcon);
        speakerIcon = findViewById(R.id.speakerIcon);
        cameraSwitchIcon = findViewById(R.id.cameraSwitchIcon);
        
        // Log view initialization for debugging
        android.util.Log.d("VideoCallActivity", "Views initialized - localVideoView: " + (localVideoView != null) + ", remoteVideoView: " + (remoteVideoView != null));
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    private void getIntentData() {
        otherUserId = getIntent().getStringExtra("otherUserId");
        otherUserName = getIntent().getStringExtra("otherUserName");
        isOutgoing = getIntent().getBooleanExtra("isOutgoing", true);
        callId = getIntent().getStringExtra("callId"); // For incoming calls
        
        android.util.Log.d("VideoCallActivity", "Intent data - otherUserId: " + otherUserId + ", otherUserName: " + otherUserName + ", isOutgoing: " + isOutgoing + ", callId: " + callId);
        
        // Validate required data
        if (otherUserId == null) {
            android.util.Log.e("VideoCallActivity", "Missing otherUserId in intent");
            Toast.makeText(this, "Call error: Missing user information", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    private void setupCallInterface() {
        userName.setText(otherUserName != null ? otherUserName : "Unknown User");
        
        // Load user profile image
        if (otherUserId != null) {
            loadUserProfileImage();
        }

        // Setup audio manager
        systemAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
    }

    private void loadUserProfileImage() {
        db.collection("users")
                .document(otherUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String photoURL = documentSnapshot.getString("photoURL");
                        if (photoURL != null && !photoURL.isEmpty()) {
                            Glide.with(this)
                                    .load(photoURL)
                                    .placeholder(R.drawable.ic_profile_placeholder)
                                    .into(userProfileImage);
                        }
                    }
                });
    }

    private void setupClickListeners() {
        // Mute button
        muteButton.setOnClickListener(v -> toggleMute());
        
        // Speaker button
        speakerButton.setOnClickListener(v -> toggleSpeaker());
        
        // Camera switch button
        cameraSwitchButton.setOnClickListener(v -> switchCamera());
        
        // End call button
        endCallButton.setOnClickListener(v -> endCall());
        
        // Accept call button
        acceptButton.setOnClickListener(v -> {
            android.util.Log.d("VideoCallActivity", "Accept button clicked!");
            Toast.makeText(this, "Accept button clicked!", Toast.LENGTH_SHORT).show();
            acceptCall();
        });
        
        // Decline call button
        declineButton.setOnClickListener(v -> declineCall());
    }

    private void initiateOutgoingCall() {
        android.util.Log.d("VideoCallActivity", "Initiating outgoing video call");
        currentCallState = CallState.CALLING;
        callStatus.setText("Calling...");
        
        // Show calling interface
        incomingCallSection.setVisibility(View.GONE);
        bottomSection.setVisibility(View.VISIBLE);
        
        // Start call timeout
        startCallTimeout();
        
        // Initialize video WebRTC
        initializeVideo();
        
        // Update call status in Firestore
        if (callId != null) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("status", "calling");
            updateData.put("callType", "video");
            updateData.put("startTime", System.currentTimeMillis());

            db.collection("calls").document(callId).update(updateData)
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("VideoCallActivity", "Outgoing call status updated");
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("VideoCallActivity", "Failed to update call status", e);
                    });
        }
    }

    private void setupIncomingCall() {
        android.util.Log.d("VideoCallActivity", "Setting up incoming video call - callId: " + callId);
        currentCallState = CallState.RINGING;
        callStatus.setText("Incoming video call...");
        
        // Show incoming call interface
        incomingCallSection.setVisibility(View.VISIBLE);
        bottomSection.setVisibility(View.GONE);
        
        android.util.Log.d("VideoCallActivity", "Incoming call section visibility: " + (incomingCallSection.getVisibility() == View.VISIBLE));
        android.util.Log.d("VideoCallActivity", "Bottom section visibility: " + (bottomSection.getVisibility() == View.VISIBLE));
        
        // Ensure accept and decline buttons are clickable
        acceptButton.setClickable(true);
        acceptButton.setEnabled(true);
        declineButton.setClickable(true);
        declineButton.setEnabled(true);
        
        android.util.Log.d("VideoCallActivity", "Accept button - clickable: " + acceptButton.isClickable() + ", enabled: " + acceptButton.isEnabled() + ", visible: " + (acceptButton.getVisibility() == View.VISIBLE));
        
        // Add touch listener to debug button responsiveness
        acceptButton.setOnTouchListener((v, event) -> {
            android.util.Log.d("VideoCallActivity", "Accept button touch event: " + event.getAction());
            return false; // Let the click listener handle it
        });
        
        // Test button position and size
        acceptButton.post(() -> {
            android.util.Log.d("VideoCallActivity", "Accept button bounds: " + acceptButton.getLeft() + ", " + acceptButton.getTop() + ", " + acceptButton.getRight() + ", " + acceptButton.getBottom());
            android.util.Log.d("VideoCallActivity", "Accept button width: " + acceptButton.getWidth() + ", height: " + acceptButton.getHeight());
        });
        
        // Start ringtone
        startRingtone();
        
        // Start call timeout
        startCallTimeout();
        
        // Listen for call status changes
        if (callId != null) {
            callStatusListener = db.collection("calls").document(callId)
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null) {
                            android.util.Log.e("VideoCallActivity", "Error listening to call status", e);
                            return;
                        }

                        if (snapshot != null && snapshot.exists()) {
                            String status = snapshot.getString("status");
                            android.util.Log.d("VideoCallActivity", "Call status changed to: " + status + " - currentCallState: " + currentCallState);
                            
                            if ("accepted".equals(status)) {
                                android.util.Log.d("VideoCallActivity", "Call accepted, connecting...");
                                connectCall();
                            } else if ("declined".equals(status) || "ended".equals(status)) {
                                android.util.Log.d("VideoCallActivity", "Call " + status + ", ending...");
                                endCall();
                            }
                        } else {
                            android.util.Log.w("VideoCallActivity", "Call document does not exist");
                        }
                    });
        } else {
            android.util.Log.e("VideoCallActivity", "No callId available for incoming call");
        }
    }

    private void acceptCall() {
        android.util.Log.d("VideoCallActivity", "acceptCall() method called - callId: " + callId + ", currentCallState: " + currentCallState);
        
        // Update call status to accepted
        if (callId != null) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("status", "accepted");
            updateData.put("acceptedTime", System.currentTimeMillis());

            db.collection("calls").document(callId).update(updateData)
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("VideoCallActivity", "Call accepted");
                        connectCall();
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("VideoCallActivity", "Failed to accept call", e);
                        Toast.makeText(this, "Failed to accept call", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void declineCall() {
        android.util.Log.d("VideoCallActivity", "Declining incoming video call");
        
        // Update call status to declined
        if (callId != null) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("status", "declined");
            updateData.put("endedTime", System.currentTimeMillis());

            db.collection("calls").document(callId).update(updateData)
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("VideoCallActivity", "Call declined");
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("VideoCallActivity", "Failed to decline call", e);
                        finish();
                    });
        } else {
            finish();
        }
    }

    private void connectCall() {
        if (isCallActive) {
            android.util.Log.d("VideoCallActivity", "connectCall() called but call is already active, skipping...");
            return;
        }

        android.util.Log.d("VideoCallActivity", "Connecting video call... - isOutgoing: " + isOutgoing + ", callId: " + callId);
        isCallActive = true;
        currentCallState = CallState.CONNECTED;
        callStartTime = System.currentTimeMillis();

        // Update UI for connected state
        incomingCallSection.setVisibility(View.GONE);
        bottomSection.setVisibility(View.VISIBLE);
        callStatus.setText("Connected");

        // Stop ringtone if playing
        stopRingtone();

        // Stop waiting audio and timeout
        stopWaitingAudio();
        stopCallTimeout();

        // Initialize and start video WebRTC
        android.util.Log.d("VideoCallActivity", "About to initialize video in connectCall");
        initializeVideo();

        // Start call duration timer
        startCallDurationTimer();

        // Update call status in Firestore
        if (callId != null) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("status", "connected");
            updateData.put("connectedTime", System.currentTimeMillis());

            db.collection("calls").document(callId).update(updateData)
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("VideoCallActivity", "Call status updated to connected");
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("VideoCallActivity", "Failed to update call status", e);
                    });
        }
    }

    private void initializeVideo() {
        if (isVideoInitialized) {
            android.util.Log.d("VideoCallActivity", "Video already initialized, skipping...");
            return;
        }

        android.util.Log.d("VideoCallActivity", "Initializing video WebRTC - isOutgoing: " + isOutgoing);
        
        try {
            // Create video WebRTC manager first to get EglBase
            videoWebRTCManager = new VideoWebRTCManager(this, this);
            
            // Initialize video views with proper EglBase context
            EglBase eglBase = videoWebRTCManager.getEglBase();
            android.util.Log.d("VideoCallActivity", "Initializing local video view with EglBase context");
            localVideoView.init(eglBase.getEglBaseContext(), null);
            android.util.Log.d("VideoCallActivity", "Initializing remote video view with EglBase context");
            remoteVideoView.init(eglBase.getEglBaseContext(), null);
            
            android.util.Log.d("VideoCallActivity", "Video views initialized successfully");
            
            // Start the call
            if (isOutgoing) {
                android.util.Log.d("VideoCallActivity", "Starting outgoing call");
                videoWebRTCManager.startCall(callId, currentUser.getUid(), otherUserId);
            } else {
                android.util.Log.d("VideoCallActivity", "Answering incoming call");
                videoWebRTCManager.answerCall(callId, currentUser.getUid(), otherUserId);
            }
            
            isVideoInitialized = true;
            
        } catch (Exception e) {
            android.util.Log.e("VideoCallActivity", "Error initializing video", e);
            Toast.makeText(this, "Failed to initialize video: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleMute() {
        if (videoWebRTCManager != null) {
            isMuted = !isMuted;
            videoWebRTCManager.toggleMute(isMuted);
            
            // Update UI
            muteIcon.setImageResource(isMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic);
            muteButton.setBackgroundResource(isMuted ? R.drawable.call_button_active : R.drawable.call_button_normal);
            
            Toast.makeText(this, isMuted ? "Microphone muted" : "Microphone unmuted", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleSpeaker() {
        if (systemAudioManager != null) {
            isSpeakerOn = !isSpeakerOn;
            
            if (isSpeakerOn) {
                systemAudioManager.setMode(AudioManager.MODE_NORMAL);
                systemAudioManager.setSpeakerphoneOn(true);
                speakerIcon.setImageResource(R.drawable.ic_volume_up);
                speakerButton.setBackgroundResource(R.drawable.call_button_active);
            } else {
                systemAudioManager.setSpeakerphoneOn(false);
                systemAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                speakerIcon.setImageResource(R.drawable.ic_volume_down);
                speakerButton.setBackgroundResource(R.drawable.call_button_normal);
            }
            
            Toast.makeText(this, isSpeakerOn ? "Speaker on" : "Speaker off", Toast.LENGTH_SHORT).show();
        }
    }

    private void switchCamera() {
        if (videoWebRTCManager != null) {
            isFrontCamera = !isFrontCamera;
            videoWebRTCManager.switchCamera(isFrontCamera);
            
            Toast.makeText(this, isFrontCamera ? "Front camera" : "Back camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void endCall() {
        android.util.Log.d("VideoCallActivity", "endCall() called - currentCallState: " + currentCallState + ", isCallActive: " + isCallActive + ", isOutgoing: " + isOutgoing);
        
        if (currentCallState == CallState.ENDED) {
            android.util.Log.d("VideoCallActivity", "Call already ended, skipping...");
            return;
        }

        android.util.Log.d("VideoCallActivity", "Ending video call");
        currentCallState = CallState.ENDED;

        // Stop all timers and audio
        stopCallDurationTimer();
        stopRingtone();
        stopWaitingAudio();
        stopCallTimeout();

        // Cleanup video WebRTC
        if (videoWebRTCManager != null) {
            videoWebRTCManager.endCall();
            videoWebRTCManager.cleanup();
        }

        // Update call status in Firestore
        if (callId != null) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("status", "ended");
            updateData.put("endedTime", System.currentTimeMillis());
            updateData.put("duration", isCallActive ? System.currentTimeMillis() - callStartTime : 0);

            db.collection("calls").document(callId).update(updateData)
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("VideoCallActivity", "Call ended successfully");
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("VideoCallActivity", "Failed to update call end status", e);
                    });
        }

        // Remove call status listener
        if (callStatusListener != null) {
            callStatusListener.remove();
            callStatusListener = null;
        }

        finish();
    }

    private void startCallDurationTimer() {
        callDurationHandler = new Handler(Looper.getMainLooper());
        callDurationRunnable = new Runnable() {
            @Override
            public void run() {
                if (isCallActive && currentCallState == CallState.CONNECTED) {
                    long duration = System.currentTimeMillis() - callStartTime;
                    long seconds = (duration / 1000) % 60;
                    long minutes = (duration / (1000 * 60)) % 60;
                    long hours = (duration / (1000 * 60 * 60)) % 24;
                    
                    String timeString;
                    if (hours > 0) {
                        timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                    } else {
                        timeString = String.format("%02d:%02d", minutes, seconds);
                    }
                    
                    callDuration.setText(timeString);
                    callDurationHandler.postDelayed(this, 1000);
                }
            }
        };
        callDurationHandler.post(callDurationRunnable);
    }

    private void stopCallDurationTimer() {
        if (callDurationHandler != null && callDurationRunnable != null) {
            callDurationHandler.removeCallbacks(callDurationRunnable);
        }
    }

    private void startCallTimeout() {
        timeoutHandler = new Handler(Looper.getMainLooper());
        timeoutRunnable = () -> {
            android.util.Log.d("VideoCallActivity", "Call timeout reached");
            if (currentCallState != CallState.CONNECTED && currentCallState != CallState.ENDED) {
                Toast.makeText(this, "Call timed out", Toast.LENGTH_SHORT).show();
                endCall();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, CALL_TIMEOUT_MS);
    }

    private void stopCallTimeout() {
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
    }

    private void startRingtone() {
        try {
            if (ringtonePlayer == null) {
                ringtonePlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_RINGTONE_URI);
                ringtonePlayer.setLooping(true);
            }
            ringtonePlayer.start();
        } catch (Exception e) {
            android.util.Log.e("VideoCallActivity", "Error starting ringtone", e);
        }
    }

    private void stopRingtone() {
        if (ringtonePlayer != null && ringtonePlayer.isPlaying()) {
            ringtonePlayer.stop();
            ringtonePlayer.release();
            ringtonePlayer = null;
        }
    }

    private void startWaitingAudio() {
        try {
            if (waitingAudioPlayer == null) {
                waitingAudioPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
                waitingAudioPlayer.setLooping(true);
                waitingAudioPlayer.setVolume(0.5f, 0.5f);
            }
            waitingAudioPlayer.start();
        } catch (Exception e) {
            android.util.Log.e("VideoCallActivity", "Error starting waiting audio", e);
        }
    }

    private void stopWaitingAudio() {
        if (waitingAudioPlayer != null && waitingAudioPlayer.isPlaying()) {
            waitingAudioPlayer.stop();
            waitingAudioPlayer.release();
            waitingAudioPlayer = null;
        }
    }

    @Override
    public void onCallConnected() {
        runOnUiThread(() -> {
            android.util.Log.d("VideoCallActivity", "Video call connected - isCallActive: " + isCallActive + ", currentCallState: " + currentCallState);
            callStatus.setText("Connected - Video Active");
            Toast.makeText(this, "Video call connected", Toast.LENGTH_SHORT).show();
            
            // Ensure call is marked as active when connected
            if (!isCallActive) {
                android.util.Log.d("VideoCallActivity", "Marking call as active on connection");
                isCallActive = true;
                currentCallState = CallState.CONNECTED;
            }
            
            // Ensure local video view is visible and properly configured
            if (localVideoView != null) {
                android.util.Log.d("VideoCallActivity", "Ensuring local video view is visible on call connection");
                localVideoView.setVisibility(View.VISIBLE);
                
                // If local video track is available, connect it
                if (videoWebRTCManager != null) {
                    videoWebRTCManager.connectLocalVideo(localVideoView);
                    android.util.Log.d("VideoCallActivity", "Local video reconnected on call connection");
                }
            }
        });
    }

    @Override
    public void onCallDisconnected() {
        runOnUiThread(() -> {
            android.util.Log.d("VideoCallActivity", "Video call disconnected - isCallActive: " + isCallActive + ", currentCallState: " + currentCallState + ", isOutgoing: " + isOutgoing);
            
            // Only end the call if it's already active or if it's an outgoing call that failed to connect
            if ((isCallActive && currentCallState != CallState.ENDED) || 
                (isOutgoing && currentCallState == CallState.CALLING)) {
                android.util.Log.d("VideoCallActivity", "Ending call due to disconnection");
                endCall();
            } else {
                android.util.Log.d("VideoCallActivity", "Ignoring disconnection - call not active or incoming call not yet accepted");
            }
        });
    }

    @Override
    public void onVideoStarted() {
        runOnUiThread(() -> {
            android.util.Log.d("VideoCallActivity", "Video started");
            Toast.makeText(this, "Video started", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onVideoStopped() {
        runOnUiThread(() -> {
            android.util.Log.d("VideoCallActivity", "Video stopped");
        });
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            android.util.Log.e("VideoCallActivity", "Video error: " + error + " - isCallActive: " + isCallActive + ", currentCallState: " + currentCallState);
            Toast.makeText(this, "Video error: " + error, Toast.LENGTH_SHORT).show();
            
            // If there's a critical error and the call is not yet connected, end the call
            if (!isCallActive && currentCallState != CallState.CONNECTED) {
                android.util.Log.d("VideoCallActivity", "Ending call due to critical error before connection");
                endCall();
            }
        });
    }

    @Override
    public void onLocalVideoReady(SurfaceViewRenderer localView) {
        runOnUiThread(() -> {
            android.util.Log.d("VideoCallActivity", "onLocalVideoReady callback received - isOutgoing: " + isOutgoing + ", isCallActive: " + isCallActive);
            android.util.Log.d("VideoCallActivity", "Local video ready - localView parameter: " + (localView != null));
            
            // The localView parameter is null from VideoWebRTCManager, so we use our own localVideoView
            if (videoWebRTCManager != null && localVideoView != null) {
                videoWebRTCManager.connectLocalVideo(localVideoView);
                android.util.Log.d("VideoCallActivity", "Local video connected to UI");
                
                // Make sure the local video view is visible for both outgoing and incoming calls
                localVideoView.setVisibility(View.VISIBLE);
                android.util.Log.d("VideoCallActivity", "Local video view made visible");
            } else {
                android.util.Log.w("VideoCallActivity", "Cannot connect local video: manager=" + (videoWebRTCManager != null) + ", view=" + (localVideoView != null));
            }
        });
    }

    @Override
    public void onRemoteVideoReady(SurfaceViewRenderer remoteView) {
        runOnUiThread(() -> {
            android.util.Log.d("VideoCallActivity", "Remote video ready");
            // Remote video view is already set up in the layout
        });
    }
    
    @Override
    public void onRemoteVideoTrackReceived(org.webrtc.VideoTrack remoteVideoTrack) {
        runOnUiThread(() -> {
            android.util.Log.d("VideoCallActivity", "onRemoteVideoTrackReceived callback received - isOutgoing: " + isOutgoing);
            android.util.Log.d("VideoCallActivity", "Remote video track received");
            
            if (videoWebRTCManager != null && remoteVideoView != null) {
                videoWebRTCManager.connectRemoteVideo(remoteVideoView);
                android.util.Log.d("VideoCallActivity", "Remote video connected to UI");
                
                // Make sure remote video view is visible
                remoteVideoView.setVisibility(View.VISIBLE);
            } else {
                android.util.Log.w("VideoCallActivity", "Cannot connect remote video: manager=" + (videoWebRTCManager != null) + ", view=" + (remoteVideoView != null));
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Prevent back button from ending call accidentally
        Toast.makeText(this, "Use the end call button to hang up", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Cleanup
        if (videoWebRTCManager != null) {
            // Disconnect video tracks before cleanup
            if (localVideoView != null) {
                videoWebRTCManager.disconnectLocalVideo(localVideoView);
            }
            if (remoteVideoView != null) {
                videoWebRTCManager.disconnectRemoteVideo(remoteVideoView);
            }
            videoWebRTCManager.cleanup();
        }
        
        if (localVideoView != null) {
            localVideoView.release();
        }
        
        if (remoteVideoView != null) {
            remoteVideoView.release();
        }
        
        stopRingtone();
        stopWaitingAudio();
        stopCallTimeout();
        stopCallDurationTimer();
        
        if (callStatusListener != null) {
            callStatusListener.remove();
        }
    }
}
