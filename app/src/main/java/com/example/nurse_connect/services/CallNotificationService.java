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
        startForeground(NOTIFICATION_ID, createForegroundNotification());

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

        // Test query first to make sure we can access Firestore
        db.collection("calls")
                .whereEqualTo("receiverId", currentUser.getUid())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Firestore test query successful. Found " + querySnapshot.size() + " total calls for this user");
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Log.d(TAG, "Existing call: " + doc.getId() + " status: " + doc.getString("status"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore test query failed", e);
                });

        // Listen for ALL calls to this user (not just "calling" status) for debugging
        callListener = db.collection("calls")
                .whereEqualTo("receiverId", currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for calls: " + error.getMessage());
                        return;
                    }

                    Log.d(TAG, "Snapshot listener triggered. Value is null: " + (value == null) + ", isEmpty: " + (value != null ? value.isEmpty() : "N/A"));

                    if (value != null && !value.isEmpty()) {
                        Log.d(TAG, "Found " + value.size() + " total calls for this user");

                        // Process each call
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            String status = doc.getString("status");
                            String callerId = doc.getString("callerId");
                            String callerName = doc.getString("callerName");
                            Long timestamp = doc.getLong("timestamp");
                            Log.d(TAG, "Processing call " + doc.getId() + " from " + callerName + " (status: " + status + ", timestamp: " + timestamp + ")");

                            if ("calling".equals(status)) {
                                Log.d(TAG, "Found active incoming call - showing notification");
                                // Show incoming call notification
                                showIncomingCallNotification(
                                        doc.getId(),
                                        callerId,
                                        callerName,
                                        doc.getString("callerPhotoUrl")
                                );
                            } else {
                                Log.d(TAG, "Call status is not 'calling', it's: " + status);
                            }
                        }
                    } else {
                        Log.d(TAG, "No calls found for this user in snapshot");
                    }
                });
    }

    private void showIncomingCallNotification(String callId, String callerId, String callerName, String callerPhotoUrl) {
        Log.d(TAG, "Showing incoming call notification for call: " + callId + " from: " + callerName);

        // Create intent to open AudioCallActivity for incoming call
        Intent callIntent = new Intent(this, AudioCallActivity.class);
        callIntent.putExtra("callId", callId);
        callIntent.putExtra("otherUserId", callerId);
        callIntent.putExtra("otherUserName", callerName != null ? callerName : "Unknown Caller");
        callIntent.putExtra("isOutgoing", false);
        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, callIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Incoming Call")
                .setContentText(callerName + " is calling...")
                .setSmallIcon(R.drawable.ic_phone)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .setOngoing(true)
                .setVibrate(new long[]{0, 1000, 500, 1000});

        // Add action buttons
        Intent declineIntent = new Intent(this, CallActionReceiver.class);
        declineIntent.setAction("DECLINE_CALL");
        declineIntent.putExtra("callId", callId);
        PendingIntent declinePendingIntent = PendingIntent.getBroadcast(
                this, 1, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent acceptIntent = new Intent(this, CallActionReceiver.class);
        acceptIntent.setAction("ACCEPT_CALL");
        acceptIntent.putExtra("callId", callId);
        acceptIntent.putExtra("otherUserId", callerId);
        acceptIntent.putExtra("otherUserName", callerName);
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(
                this, 2, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        builder.addAction(R.drawable.ic_call_end, "Decline", declinePendingIntent);
        builder.addAction(R.drawable.ic_phone, "Accept", acceptPendingIntent);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(callId.hashCode(), builder.build());
        }

        Log.d(TAG, "Incoming call notification shown for: " + callerName);
    }
}
