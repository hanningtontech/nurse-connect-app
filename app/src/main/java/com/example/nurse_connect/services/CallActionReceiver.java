package com.example.nurse_connect.services;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.nurse_connect.ui.chat.AudioCallActivity;
import com.example.nurse_connect.ui.chat.VideoCallActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public class CallActionReceiver extends BroadcastReceiver {

    private static final String TAG = "CallActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String callId = intent.getStringExtra("callId");

        Log.d(TAG, "Received action: " + action + " for call: " + callId);

        if (callId == null) {
            return;
        }

        // Cancel the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(callId.hashCode());
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if ("ACCEPT_CALL".equals(action)) {
            // Update call status to accepted
            db.collection("calls")
                    .document(callId)
                    .update("status", "accepted")
                    .addOnSuccessListener(aVoid -> {
                        // Determine call type and start appropriate activity
                        String callType = intent.getStringExtra("callType");
                        Intent callIntent;
                        
                        if ("video".equals(callType)) {
                            callIntent = new Intent(context, VideoCallActivity.class);
                        } else {
                            callIntent = new Intent(context, AudioCallActivity.class);
                        }
                        
                        callIntent.putExtra("callId", callId);
                        callIntent.putExtra("otherUserId", intent.getStringExtra("otherUserId"));
                        callIntent.putExtra("otherUserName", intent.getStringExtra("otherUserName"));
                        callIntent.putExtra("isOutgoing", false);
                        callIntent.putExtra("callType", callType);
                        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(callIntent);
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to accept call", e));

        } else if ("DECLINE_CALL".equals(action)) {
            // Update call status to declined
            db.collection("calls")
                    .document(callId)
                    .update("status", "declined")
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Call declined"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to decline call", e));
        }
    }
}
