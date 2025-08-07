package com.example.nurse_connect.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Broadcast receiver to handle new message notifications
 * Updates the UI when new messages are received in real-time
 */
public class MessageReceiver extends BroadcastReceiver {
    
    private static final String TAG = "MessageReceiver";
    
    public interface MessageUpdateListener {
        void onNewMessageReceived(String chatId, int unreadCount, String lastMessage, String senderName);
    }
    
    private static MessageUpdateListener listener;
    
    public static void setMessageUpdateListener(MessageUpdateListener listener) {
        MessageReceiver.listener = listener;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.example.nurse_connect.NEW_MESSAGE".equals(intent.getAction())) {
            String chatId = intent.getStringExtra("chatId");
            int unreadCount = intent.getIntExtra("unreadCount", 0);
            String lastMessage = intent.getStringExtra("lastMessage");
            String senderName = intent.getStringExtra("senderName");
            
            Log.d(TAG, "New message broadcast received for chat: " + chatId + 
                " with unread count: " + unreadCount);
            
            if (listener != null) {
                listener.onNewMessageReceived(chatId, unreadCount, lastMessage, senderName);
            }
        }
    }
}
