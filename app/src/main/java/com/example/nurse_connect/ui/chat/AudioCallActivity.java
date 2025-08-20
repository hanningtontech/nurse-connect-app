package com.example.nurse_connect.ui.chat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
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
import com.example.nurse_connect.webrtc.RealTimeAudioManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class AudioCallActivity extends AppCompatActivity implements
        RealTimeAudioManager.AudioListener {

    private CircleImageView userProfileImage;
    private TextView userName;
    private TextView callStatus;
    private TextView callDuration;
    private LinearLayout muteButton;
    private LinearLayout endCallButton;
    private LinearLayout speakerButton;
    private LinearLayout incomingCallSection;
    private LinearLayout bottomSection;
    private LinearLayout acceptButton;
    private LinearLayout declineButton;
    private ImageView muteIcon;
    private ImageView speakerIcon;

    private String otherUserId;
    private String otherUserName;
    private boolean isOutgoing;
    private boolean isCallActive = false;
    private boolean isMuted = false;
    private boolean isSpeakerOn = false;
    private long callStartTime;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private Handler callDurationHandler;
    private Runnable callDurationRunnable;
    private MediaPlayer ringtonePlayer;
    private AudioManager systemAudioManager;
    private String callId;
    private com.google.firebase.firestore.ListenerRegistration callStatusListener;

    // Real-time audio components
    private RealTimeAudioManager realTimeAudioManager;
    private boolean isAudioInitialized = false;

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
        setContentView(R.layout.activity_audio_call);

        // Keep screen on during call
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initializeViews();
        initializeFirebase();
        getIntentData();

        // Check WebRTC permissions before proceeding
        if (!PermissionUtils.hasWebRTCPermissions(this)) {
            PermissionUtils.requestWebRTCPermissions(this);
            return;
        }

        setupCallInterface();
        setupClickListeners();

        android.util.Log.d("AudioCallActivity", "Starting call activity - isOutgoing: " + isOutgoing + ", callId: " + callId);

        if (isOutgoing) {
            initiateOutgoingCall();
        } else {
            setupIncomingCall();
        }
    }

    private void initializeViews() {
        userProfileImage = findViewById(R.id.userProfileImage);
        userName = findViewById(R.id.userName);
        callStatus = findViewById(R.id.callStatus);
        callDuration = findViewById(R.id.callDuration);
        muteButton = findViewById(R.id.muteButton);
        endCallButton = findViewById(R.id.endCallButton);
        speakerButton = findViewById(R.id.speakerButton);
        incomingCallSection = findViewById(R.id.incomingCallSection);
        bottomSection = findViewById(R.id.bottomSection);
        acceptButton = findViewById(R.id.acceptButton);
        declineButton = findViewById(R.id.declineButton);
        muteIcon = findViewById(R.id.muteIcon);
        speakerIcon = findViewById(R.id.speakerIcon);
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
        
        android.util.Log.d("AudioCallActivity", "Intent data - otherUserId: " + otherUserId + ", otherUserName: " + otherUserName + ", isOutgoing: " + isOutgoing + ", callId: " + callId);
        
        // Validate required data
        if (otherUserId == null) {
            android.util.Log.e("AudioCallActivity", "Missing otherUserId in intent");
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
        muteButton.setOnClickListener(v -> toggleMute());
        speakerButton.setOnClickListener(v -> toggleSpeaker());
        endCallButton.setOnClickListener(v -> endCall());
        // Removed accept/decline button listeners since calls are now automatic
    }

    private void initiateOutgoingCall() {
        currentCallState = CallState.CALLING;
        callStatus.setText("Calling...");

        // Show outgoing call interface
        incomingCallSection.setVisibility(View.GONE);
        bottomSection.setVisibility(View.VISIBLE);

        // Start waiting audio for caller
        playWaitingAudio();

        // Start call timeout (45 seconds)
        startCallTimeout();

        // Send call notification to other user
        sendCallNotification();
    }

    private void waitForCallDocument() {
        if (currentUser == null || otherUserId == null) return;

        // Listen for the call document to be created
        db.collection("calls")
                .whereEqualTo("callerId", currentUser.getUid())
                .whereEqualTo("receiverId", otherUserId)
                .whereEqualTo("status", "calling")
                .limit(1)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        android.util.Log.e("AudioCallActivity", "Error waiting for call document", error);
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        DocumentSnapshot document = value.getDocuments().get(0);
                        callId = document.getId();
                        android.util.Log.d("AudioCallActivity", "Call document found with ID: " + callId);
                        
                        // Don't initialize audio yet - wait for receiver to accept
                        // The WebRTC connection will be established after the receiver accepts
                    }
                });
    }

    private void setupIncomingCall() {
        currentCallState = CallState.RINGING;
        callStatus.setText("Incoming call...");
        
        // Show incoming call interface
        incomingCallSection.setVisibility(View.VISIBLE);
        bottomSection.setVisibility(View.GONE);
        
        // Play ringtone
        
        // Start listening for call status changes (for incoming calls)
        listenForCallStatusChanges();
        
        playRingtone();
    }

    private void acceptCall() {
        stopRingtone();

        // Update call status to accepted in Firestore
        if (callId != null) {
            db.collection("calls")
                    .document(callId)
                    .update("status", "accepted")
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("AudioCallActivity", "Call accepted in Firestore - waiting for status listener to handle connection");
                        // Don't call connectCall() here - let the status listener handle it
                        // This prevents race conditions and ensures proper flow
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("AudioCallActivity", "Failed to accept call in Firestore", e);
                        Toast.makeText(this, "Failed to accept call", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        } else {
            android.util.Log.e("AudioCallActivity", "CallId is null, cannot accept call");
            Toast.makeText(this, "Call error", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void declineCall() {
        stopRingtone();

        // Update call status to declined in Firestore
        if (callId != null) {
            db.collection("calls")
                    .document(callId)
                    .update("status", "declined")
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("AudioCallActivity", "Call declined in Firestore");
                        endCall();
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("AudioCallActivity", "Failed to decline call in Firestore", e);
                        endCall();
                    });
        } else {
            endCall();
        }
    }

    private void connectCall() {
        if (isCallActive) {
            android.util.Log.d("AudioCallActivity", "connectCall() called but call is already active, skipping...");
            return;
        }

        android.util.Log.d("AudioCallActivity", "Connecting call... - isOutgoing: " + isOutgoing + ", callId: " + callId);
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

        // Initialize and start WebRTC audio
        initializeAudio();

        // Start call duration timer
        startCallDurationTimer();

        // Update call status in Firestore
        if (callId != null) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("status", "connected");
            updateData.put("connectedTime", System.currentTimeMillis());

            db.collection("calls").document(callId).update(updateData)
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("AudioCallActivity", "Call status updated to connected");
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("AudioCallActivity", "Failed to update call status", e);
                    });
        }
    }

    private void endCall() {
        android.util.Log.d("AudioCallActivity", "Ending call...");
        
        // Prevent multiple calls to endCall
        if (currentCallState == CallState.ENDED) {
            android.util.Log.d("AudioCallActivity", "Call already ended, skipping endCall()");
            return;
        }
        
        // Set state to ENDED immediately to prevent circular calls
        currentCallState = CallState.ENDED;
        isCallActive = false;
        
        // Stop ringtone if playing
        stopRingtone();
        
        // Stop waiting audio and timeout
        stopWaitingAudio();
        stopCallTimeout();
        
        // Stop call duration timer
        stopCallDurationTimer();
        
        // Clean up WebRTC resources properly
        if (realTimeAudioManager != null) {
            android.util.Log.d("AudioCallActivity", "Ending WebRTC call");
            realTimeAudioManager.endCall();
            realTimeAudioManager.cleanup();
            realTimeAudioManager = null;
        }
        
        isAudioInitialized = false;
        
        // Update call status in Firestore to ended
        if (callId != null) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("status", "ended");
            updateData.put("endTime", System.currentTimeMillis());
            updateData.put("duration", callStartTime > 0 ? System.currentTimeMillis() - callStartTime : 0);

            db.collection("calls").document(callId).update(updateData)
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("AudioCallActivity", "Call ended in Firestore");
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("AudioCallActivity", "Failed to update call end status", e);
                    });
        }
        
        // Clean up call status listener
        if (callStatusListener != null) {
            callStatusListener.remove();
            callStatusListener = null;
        }
        
        // Show call ended message briefly
        callStatus.setText("Call ended");
        
        // Close activity after a short delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            finish();
        }, 1500);
    }

    private void toggleMute() {
        isMuted = !isMuted;
        muteIcon.setImageResource(isMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic_on);

        // Mute/unmute the microphone using real-time audio manager
        if (realTimeAudioManager != null) {
            realTimeAudioManager.toggleMute(isMuted);
        }

        Toast.makeText(this, isMuted ? "Muted" : "Unmuted", Toast.LENGTH_SHORT).show();
    }

    private void toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn;
        speakerIcon.setImageResource(isSpeakerOn ? R.drawable.ic_speaker_on : R.drawable.ic_speaker_off);
        
        // Toggle speaker mode using real-time audio manager
        if (realTimeAudioManager != null) {
            realTimeAudioManager.toggleSpeaker(isSpeakerOn);
        }
        
        Toast.makeText(this, isSpeakerOn ? "Speaker on" : "Speaker off", Toast.LENGTH_SHORT).show();
    }

    private void startCallDurationTimer() {
        callDurationHandler = new Handler(Looper.getMainLooper());
        callDurationRunnable = new Runnable() {
            @Override
            public void run() {
                if (isCallActive) {
                    long duration = System.currentTimeMillis() - callStartTime;
                    updateCallDuration(duration);
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

    private void updateCallDuration(long duration) {
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        String durationText = String.format("%02d:%02d", minutes, seconds);
        callDuration.setText(durationText);
    }

    private void playRingtone() {
        try {
            // Use system default ringtone since we don't have a custom one
            android.net.Uri ringtoneUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE);
            ringtonePlayer = MediaPlayer.create(this, ringtoneUri);
            if (ringtonePlayer != null) {
                ringtonePlayer.setLooping(true);
                ringtonePlayer.start();
            }
        } catch (Exception e) {
            // Fallback to no sound if ringtone fails
            e.printStackTrace();
        }
    }

    private void stopRingtone() {
        if (ringtonePlayer != null && ringtonePlayer.isPlaying()) {
            ringtonePlayer.stop();
            ringtonePlayer.release();
            ringtonePlayer = null;
        }
    }

    private void playWaitingAudio() {
        if (waitingAudioPlayer != null) {
            waitingAudioPlayer.release();
        }
        
        try {
            waitingAudioPlayer = new MediaPlayer();
            // You can replace this with a custom waiting audio file
            // For now, we'll use a simple beep sound
            waitingAudioPlayer.setDataSource(this, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
            waitingAudioPlayer.setLooping(true);
            waitingAudioPlayer.prepare();
            waitingAudioPlayer.start();
            android.util.Log.d("AudioCallActivity", "Waiting audio started");
        } catch (Exception e) {
            android.util.Log.e("AudioCallActivity", "Failed to play waiting audio", e);
        }
    }

    private void stopWaitingAudio() {
        if (waitingAudioPlayer != null && waitingAudioPlayer.isPlaying()) {
            waitingAudioPlayer.stop();
            waitingAudioPlayer.release();
            waitingAudioPlayer = null;
            android.util.Log.d("AudioCallActivity", "Waiting audio stopped");
        }
    }

    private void startCallTimeout() {
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
        
        timeoutHandler = new Handler(Looper.getMainLooper());
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                android.util.Log.d("AudioCallActivity", "Call timeout reached (45 seconds)");
                callStatus.setText("Call timeout - No answer");
                Toast.makeText(AudioCallActivity.this, "Call timed out - No answer", Toast.LENGTH_SHORT).show();
                endCall();
            }
        };
        
        timeoutHandler.postDelayed(timeoutRunnable, CALL_TIMEOUT_MS);
        android.util.Log.d("AudioCallActivity", "Call timeout started (45 seconds)");
    }

    private void stopCallTimeout() {
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            android.util.Log.d("AudioCallActivity", "Call timeout stopped");
        }
    }

    private void sendCallNotification() {
        // Send call invitation through Firestore
        Map<String, Object> callData = new HashMap<>();
        callData.put("callerId", currentUser.getUid());
        callData.put("callerName", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Unknown");
        callData.put("callerPhotoUrl", currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "");
        callData.put("receiverId", otherUserId);
        callData.put("type", "audio_call");
        callData.put("status", "calling");
        callData.put("timestamp", System.currentTimeMillis());
        callData.put("startTime", System.currentTimeMillis()); // Add startTime for notification service

        android.util.Log.d("AudioCallActivity", "Creating call from " + currentUser.getUid() + " to " + otherUserId);

        // Create a call document in Firestore
        db.collection("calls")
                .add(callData)
                .addOnSuccessListener(documentReference -> {
                    callId = documentReference.getId();
                    android.util.Log.d("AudioCallActivity", "Call created with ID: " + callId);
                    
                    // Start listening for call status changes after call is created
                    listenForCallStatusChanges();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("AudioCallActivity", "Failed to create call", e);
                    Toast.makeText(this, "Failed to initiate call", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void listenForCallStatusChanges() {
        // Wait a moment for callId to be set
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (callId != null) {
                android.util.Log.d("AudioCallActivity", "Starting to listen for call status changes for callId: " + callId);
                
                callStatusListener = db.collection("calls")
                        .document(callId)
                        .addSnapshotListener((snapshot, error) -> {
                            if (error != null) {
                                android.util.Log.e("AudioCallActivity", "Error listening to call status", error);
                                return;
                            }

                            if (snapshot != null && snapshot.exists()) {
                                String status = snapshot.getString("status");
                                android.util.Log.d("AudioCallActivity", "Call status changed to: " + status + " (current state: " + currentCallState + ", isOutgoing: " + isOutgoing + ")");

                                switch (status) {
                                    case "accepted":
                                        if (currentCallState == CallState.CALLING && isOutgoing) {
                                            android.util.Log.d("AudioCallActivity", "Call accepted by receiver, connecting call...");
                                            // Caller receives acceptance - now connect the call
                                            connectCall();
                                        } else if (currentCallState == CallState.RINGING && !isOutgoing) {
                                            android.util.Log.d("AudioCallActivity", "Receiver: Call was accepted, connecting call...");
                                            // Receiver: Call was accepted, connect the call
                                            connectCall();
                                        }
                                        break;
                                    case "declined":
                                        android.util.Log.d("AudioCallActivity", "Call declined by receiver");
                                        callStatus.setText("Call declined");
                                        stopWaitingAudio();
                                        stopCallTimeout();
                                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                            endCall();
                                        }, 2000);
                                        break;
                                    case "ended":
                                        if (isCallActive || currentCallState != CallState.ENDED) {
                                            android.util.Log.d("AudioCallActivity", "Call ended by other party");
                                            callStatus.setText("Call ended by other party");
                                            stopWaitingAudio();
                                            stopCallTimeout();
                                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                                endCall();
                                            }, 2000);
                                        }
                                        break;
                                    case "connected":
                                        // Both parties are now connected
                                        if (currentCallState == CallState.CONNECTED) {
                                            android.util.Log.d("AudioCallActivity", "Call fully connected");
                                            callStatus.setText("Connected");
                                        }
                                        break;
                                }
                            } else {
                                android.util.Log.w("AudioCallActivity", "Call document no longer exists");
                            }
                        });
            } else {
                android.util.Log.e("AudioCallActivity", "Cannot listen for call status - callId is null");
            }
        }, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.util.Log.d("AudioCallActivity", "Activity being destroyed");
        
        stopRingtone();
        stopWaitingAudio();
        stopCallTimeout();
        stopCallDurationTimer();

        // Clean up call status listener
        if (callStatusListener != null) {
            callStatusListener.remove();
            callStatusListener = null;
        }

        // Clean up audio resources if not already cleaned up
        if (realTimeAudioManager != null) {
            android.util.Log.d("AudioCallActivity", "Cleaning up audio resources in onDestroy");
            cleanupAudio();
        }

        // Clear window flags
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onBackPressed() {
        // Prevent back button during active call
        if (isCallActive) {
            Toast.makeText(this, "End call to go back", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }

    // Real-time audio initialization and management
    private void initializeAudio() {
        if (isAudioInitialized) {
            android.util.Log.d("AudioCallActivity", "Audio already initialized, skipping...");
            return;
        }

        android.util.Log.d("AudioCallActivity", "Initializing WebRTC audio for call: " + callId + " (isOutgoing: " + isOutgoing + ")");

        // Initialize real-time audio manager with WebRTC
        realTimeAudioManager = new RealTimeAudioManager(this, this);

        // Set call parameters for WebRTC
        if (callId != null && currentUser != null && otherUserId != null) {
            realTimeAudioManager.setCallParameters(
                callId,
                currentUser.getUid(),
                otherUserId,
                isOutgoing
            );
            android.util.Log.d("AudioCallActivity", "WebRTC call parameters set - IsOutgoing: " + isOutgoing);
            
            // Start the WebRTC call after setting parameters
            realTimeAudioManager.startCall();
        } else {
            android.util.Log.e("AudioCallActivity", "Missing call parameters for WebRTC initialization - callId: " + callId + ", currentUser: " + (currentUser != null ? currentUser.getUid() : "null") + ", otherUserId: " + otherUserId);
        }

        isAudioInitialized = true;
    }

    private void cleanupAudio() {
        android.util.Log.d("AudioCallActivity", "Cleaning up audio resources");
        if (realTimeAudioManager != null) {
            // Only cleanup, don't call endCall() to avoid circular calls
            realTimeAudioManager.cleanup();
            realTimeAudioManager = null;
        }

        isAudioInitialized = false;
    }

    // RealTimeAudioManager.AudioListener implementation
    @Override
    public void onCallConnected() {
        runOnUiThread(() -> {
            android.util.Log.d("AudioCallActivity", "Real-time audio call connected - audio channel established - isCallActive: " + isCallActive + ", currentCallState: " + currentCallState);
            callStatus.setText("Connected - Audio Active");
            Toast.makeText(this, "Live audio channel established", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onCallDisconnected() {
        runOnUiThread(() -> {
            android.util.Log.d("AudioCallActivity", "Real-time audio call disconnected - isCallActive: " + isCallActive + ", currentCallState: " + currentCallState);
            // Only end call if it's still active and we haven't already started ending it
            if (isCallActive && currentCallState != CallState.ENDED) {
                android.util.Log.d("AudioCallActivity", "Call was active, ending call due to disconnection");
                endCall();
            } else {
                android.util.Log.d("AudioCallActivity", "Call already ended or not active, skipping endCall() - isCallActive: " + isCallActive + ", currentCallState: " + currentCallState);
            }
        });
    }

    @Override
    public void onAudioStarted() {
        runOnUiThread(() -> {
            android.util.Log.d("AudioCallActivity", "Audio recording and playback started");
            Toast.makeText(this, "Live audio communication active", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onAudioStopped() {
        runOnUiThread(() -> {
            android.util.Log.d("AudioCallActivity", "Audio recording and playback stopped");
        });
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            android.util.Log.e("AudioCallActivity", "Audio error: " + error);
            Toast.makeText(this, "Audio error: " + error, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1004) { // WEBRTC_PERMISSION_REQUEST_CODE
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                android.util.Log.d("AudioCallActivity", "WebRTC permissions granted, proceeding with call setup");
                setupCallInterface();
                setupClickListeners();

                if (isOutgoing) {
                    initiateOutgoingCall();
                } else {
                    setupIncomingCall();
                }
            } else {
                android.util.Log.e("AudioCallActivity", "WebRTC permissions denied");
                Toast.makeText(this, "Audio permissions are required for voice calls", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}
