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

    // Call states
    private enum CallState {
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
        setupCallInterface();
        setupClickListeners();
        
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
        acceptButton.setOnClickListener(v -> acceptCall());
        declineButton.setOnClickListener(v -> declineCall());
    }

    private void initiateOutgoingCall() {
        currentCallState = CallState.CALLING;
        callStatus.setText("Calling...");

        // Show outgoing call interface
        incomingCallSection.setVisibility(View.GONE);
        bottomSection.setVisibility(View.VISIBLE);

        // DON'T play ringtone on caller's side - only receiver should ring
        // playRingtone();

        // Initialize real-time audio for caller
        initializeAudio();

        // Send call notification to other user
        sendCallNotification();

        // Listen for call status changes (when other user accepts/declines)
        listenForCallStatusChanges();
    }

    private void setupIncomingCall() {
        currentCallState = CallState.RINGING;
        callStatus.setText("Incoming call...");
        
        // Show incoming call interface
        incomingCallSection.setVisibility(View.VISIBLE);
        bottomSection.setVisibility(View.GONE);
        
        // Play ringtone
        playRingtone();
    }

    private void acceptCall() {
        stopRingtone();

        // Initialize real-time audio for receiver
        initializeAudio();

        // Update call status to accepted in Firestore
        if (callId != null) {
            db.collection("calls")
                    .document(callId)
                    .update("status", "accepted")
                    .addOnSuccessListener(aVoid -> android.util.Log.d("AudioCallActivity", "Call accepted in Firestore"))
                    .addOnFailureListener(e -> android.util.Log.e("AudioCallActivity", "Failed to accept call in Firestore", e));
        }

        connectCall();
    }

    private void declineCall() {
        stopRingtone();

        // Update call status to declined in Firestore
        if (callId != null) {
            db.collection("calls")
                    .document(callId)
                    .update("status", "declined")
                    .addOnSuccessListener(aVoid -> android.util.Log.d("AudioCallActivity", "Call declined in Firestore"))
                    .addOnFailureListener(e -> android.util.Log.e("AudioCallActivity", "Failed to decline call in Firestore", e));
        }

        endCall();
    }

    private void connectCall() {
        currentCallState = CallState.CONNECTED;
        isCallActive = true;
        callStartTime = System.currentTimeMillis();

        stopRingtone();
        callStatus.setText("Connected");

        // Show call controls
        incomingCallSection.setVisibility(View.GONE);
        bottomSection.setVisibility(View.VISIBLE);
        callDuration.setVisibility(View.VISIBLE);

        // Start call duration timer
        startCallDurationTimer();

        // Start real-time audio if not already done
        if (!isAudioInitialized) {
            initializeAudio();
        }

        // Start the audio call
        if (realTimeAudioManager != null) {
            realTimeAudioManager.startCall();
        }

        Toast.makeText(this, "Call connected - Audio channel active", Toast.LENGTH_SHORT).show();
    }

    private void endCall() {
        currentCallState = CallState.ENDED;
        isCallActive = false;

        stopRingtone();
        stopCallDurationTimer();

        // Cleanup audio resources
        cleanupAudio();

        // Update call status in Firestore
        if (callId != null) {
            db.collection("calls")
                    .document(callId)
                    .update("status", "ended")
                    .addOnSuccessListener(aVoid -> android.util.Log.d("AudioCallActivity", "Call ended in Firestore"))
                    .addOnFailureListener(e -> android.util.Log.e("AudioCallActivity", "Failed to end call in Firestore", e));
        }

        // Clean up listener
        if (callStatusListener != null) {
            callStatusListener.remove();
        }

        Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT).show();
        finish();
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
        if (ringtonePlayer != null) {
            ringtonePlayer.stop();
            ringtonePlayer.release();
            ringtonePlayer = null;
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

        android.util.Log.d("AudioCallActivity", "Creating call from " + currentUser.getUid() + " to " + otherUserId);

        // Create a call document in Firestore
        db.collection("calls")
                .add(callData)
                .addOnSuccessListener(documentReference -> {
                    callId = documentReference.getId();
                    android.util.Log.d("AudioCallActivity", "Call created with ID: " + callId);
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
                callStatusListener = db.collection("calls")
                        .document(callId)
                        .addSnapshotListener((snapshot, error) -> {
                            if (error != null) {
                                android.util.Log.e("AudioCallActivity", "Error listening to call status", error);
                                return;
                            }

                            if (snapshot != null && snapshot.exists()) {
                                String status = snapshot.getString("status");
                                android.util.Log.d("AudioCallActivity", "Call status changed to: " + status);

                                if ("accepted".equals(status) && currentCallState == CallState.CALLING) {
                                    connectCall();
                                } else if ("declined".equals(status)) {
                                    callStatus.setText("Call declined");
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        endCall();
                                    }, 2000);
                                }
                            }
                        });
            }
        }, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRingtone();
        stopCallDurationTimer();

        // Clean up call status listener
        if (callStatusListener != null) {
            callStatusListener.remove();
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
        if (isAudioInitialized) return;

        android.util.Log.d("AudioCallActivity", "Initializing real-time audio for call: " + callId);

        // Initialize real-time audio manager
        realTimeAudioManager = new RealTimeAudioManager(this, this);

        isAudioInitialized = true;
    }

    private void cleanupAudio() {
        if (realTimeAudioManager != null) {
            realTimeAudioManager.endCall();
            realTimeAudioManager = null;
        }

        isAudioInitialized = false;
    }

    // RealTimeAudioManager.AudioListener implementation
    @Override
    public void onCallConnected() {
        runOnUiThread(() -> {
            android.util.Log.d("AudioCallActivity", "Real-time audio call connected - audio channel established");
            callStatus.setText("Connected - Audio Active");
            Toast.makeText(this, "Live audio channel established", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onCallDisconnected() {
        runOnUiThread(() -> {
            android.util.Log.d("AudioCallActivity", "Real-time audio call disconnected");
            if (isCallActive) {
                endCall();
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
}
