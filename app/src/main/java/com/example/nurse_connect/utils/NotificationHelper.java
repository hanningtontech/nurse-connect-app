package com.example.nurse_connect.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.nurse_connect.R;
import com.example.nurse_connect.ui.chat.PrivateChatActivity;

public class NotificationHelper {
    
    private static final String CHANNEL_ID = "nurse_connect_messages";
    private static final String CHANNEL_NAME = "Messages";
    private static final String CHANNEL_DESCRIPTION = "New message notifications";
    private static final int NOTIFICATION_ID = 1001;
    
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.enableLights(true);
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    public static void showNewMessageNotification(Context context, String senderName, String messagePreview, String otherUserId, String otherUserName, String otherUserPhoto) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager == null) return;
        
        // Create intent to open the chat
        Intent intent = new Intent(context, PrivateChatActivity.class);
        intent.putExtra("other_user_id", otherUserId);
        intent.putExtra("other_user_name", otherUserName);
        intent.putExtra("other_user_photo", otherUserPhoto);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat)
                .setContentTitle(senderName)
                .setContentText(messagePreview)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messagePreview))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500, 200, 500}) // Vibration pattern
                .setDefaults(NotificationCompat.DEFAULT_SOUND);
        
        // Show notification
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
    
    public static void cancelNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }
} 