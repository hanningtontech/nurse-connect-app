package com.example.nurse_connect.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.nurse_connect.R;
import com.example.nurse_connect.ui.chat.AudioCallActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

public class CallNotificationService extends Service {

    private static final String TAG = "CallNotificationService";
    private static final String CHANNEL_ID = "call_notifications";
    private static final int NOTIFICATION_ID = 1001;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration callListener;

    private boolean isForegroundRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "CallNotificationService created");

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Log.e(TAG, "No authenticated user found, stopping service");
            stopSelf();
            return;
        }

        Log.d(TAG, "Service starting for user: " + currentUser.getUid());

        createNotificationChannel();
        
        // Don't start as foreground service immediately - wait until we actually need it
        // (i.e., when there are incoming calls)
        Log.d(TAG, "Service initialized, will start as foreground when needed");

        listenForIncomingCalls();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "CallNotificationService started");
        return START_STICKY; // Restart if killed
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // This is a started service, not bound
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "CallNotificationService destroyed");
        
        if (callListener != null) {
            callListener.remove();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Call Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for incoming calls");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Check if the service is currently running as a foreground service
     */
    private boolean isForegroundService() {
        // We can track this with a boolean flag since we control when we start/stop foreground
        return isForegroundRunning;
    }

    /**
     * Start the service as a foreground service when needed
     */
    private void startAsForegroundService() {
        try {
            // Only start as foreground if we're not already foreground
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                if (!isForegroundService()) {
                    startForeground(NOTIFICATION_ID, createForegroundNotification());
                    isForegroundRunning = true;
                    Log.d(TAG, "Service started as foreground service successfully");
                } else {
                    Log.d(TAG, "Service is already running as foreground");
                }
            } else {
                startForeground(NOTIFICATION_ID, createForegroundNotification());
                isForegroundRunning = true;
                Log.d(TAG, "Service started as foreground service successfully");
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not start as foreground service: " + e.getMessage());
            Log.w(TAG, "Continuing as background service");
        }
    }
    
    /**
     * Stop the foreground service when no longer needed
     */
    private void stopForegroundService() {
        try {
            stopForeground(true);
            isForegroundRunning = false;
            Log.d(TAG, "Service stopped as foreground service");
        } catch (Exception e) {
            Log.w(TAG, "Could not stop foreground service: " + e.getMessage());
        }
    }

    private Notification createForegroundNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Nurse Connect")
                .setContentText("Listening for incoming calls")
                .setSmallIcon(R.drawable.ic_phone)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void listenForIncomingCalls() {
        Log.d(TAG, "Starting to listen for incoming calls for user: " + currentUser.getUid());

        // Listen only for active incoming calls
        callListener = db.collection("calls")
                .whereEqualTo("receiverId", currentUser.getUid())
                .whereEqualTo("status", "calling")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for calls: " + error.getMessage());
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        Log.d(TAG, "Found " + value.size() + " active incoming calls");
                        
                        // Start as foreground service when we have incoming calls
                        startAsForegroundService();

                        // Process each active call
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            String callerId = doc.getString("callerId");
                            String callerName = doc.getString("callerName");
                            Long startTime = doc.getLong("startTime");
                            
                            // Only auto-accept recent calls (within last 30 seconds)
                            if (startTime != null && (System.currentTimeMillis() - startTime) < 30000) {
                                Log.d(TAG, "Auto-accepting incoming call from: " + callerName);
                                autoAcceptIncomingCall(
                                        doc.getId(),
                                        callerId,
                                        callerName,
                                        doc.getString("callerPhotoUrl")
                                );
                            } else {
                                Log.d(TAG, "Ignoring old call from: " + callerName + " (timestamp: " + startTime + ")");
                            }
                        }
                    } else {
                        Log.d(TAG, "No active incoming calls found");
                        
                        // Stop foreground service when no more incoming calls
                        stopForegroundService();
                    }
                });
    }

    private void autoAcceptIncomingCall(String callId, String callerId, String callerName, String callerPhotoUrl) {
        Log.d(TAG, "Auto-accepting incoming call for call: " + callId + " from: " + callerName);

        // Update call status to accepted in Firestore
        db.collection("calls")
                .document(callId)
                .update("status", "accepted")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Call auto-accepted in Firestore");
                    
                    // Start AudioCallActivity for the accepted call
                    Intent callIntent = new Intent(this, AudioCallActivity.class);
                    callIntent.putExtra("callId", callId);
                    callIntent.putExtra("otherUserId", callerId);
                    callIntent.putExtra("otherUserName", callerName != null ? callerName : "Unknown Caller");
                    callIntent.putExtra("isOutgoing", false);
                    callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(callIntent);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to auto-accept call", e);
                });
    }
}
