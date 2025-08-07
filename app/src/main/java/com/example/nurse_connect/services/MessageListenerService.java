package com.example.nurse_connect.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.nurse_connect.models.PrivateChat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Background service to maintain real-time message listeners
 * This ensures messages are received even when the app is in background
 */
public class MessageListenerService extends Service {
    
    private static final String TAG = "MessageListenerService";
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private ListenerRegistration chatListListener;
    private Map<String, Integer> lastKnownUnreadCounts = new HashMap<>();
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MessageListenerService created");
        
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        
        if (currentUser != null) {
            setupChatListener();
        }
    }
    
    private void setupChatListener() {
        Log.d(TAG, "Setting up chat listener for user: " + currentUser.getUid());
        
        chatListListener = db.collection("private_chats")
                .whereArrayContains("participants", currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Chat listener error: " + error.getMessage());
                        return;
                    }
                    
                    if (value != null) {
                        Log.d(TAG, "Chat listener triggered with " + value.size() + " chats");
                        
                        for (QueryDocumentSnapshot document : value) {
                            PrivateChat chat = document.toObject(PrivateChat.class);
                            if (chat != null) {
                                chat.setChatId(document.getId());
                                
                                int currentUnreadCount = chat.getUnreadCountForUser(currentUser.getUid());
                                String chatKey = chat.getChatId();
                                Integer previousCount = lastKnownUnreadCounts.get(chatKey);
                                
                                // Check if this is a new message
                                if (previousCount == null || currentUnreadCount > previousCount) {
                                    Log.d(TAG, "New message detected in chat " + chatKey + 
                                        ". Previous: " + previousCount + ", Current: " + currentUnreadCount);
                                    
                                    // Send broadcast to update UI
                                    Intent broadcastIntent = new Intent("com.example.nurse_connect.NEW_MESSAGE");
                                    broadcastIntent.putExtra("chatId", chat.getChatId());
                                    broadcastIntent.putExtra("unreadCount", currentUnreadCount);
                                    broadcastIntent.putExtra("lastMessage", chat.getLastMessage());
                                    broadcastIntent.putExtra("senderName", getOtherUserName(chat));
                                    sendBroadcast(broadcastIntent);
                                }
                                
                                // Update the last known count
                                lastKnownUnreadCounts.put(chatKey, currentUnreadCount);
                            }
                        }
                    }
                });
    }
    
    private String getOtherUserName(PrivateChat chat) {
        // This is a simplified version - you might want to fetch the actual user name
        if (chat.getParticipants() != null && chat.getParticipants().size() >= 2) {
            for (String participantId : chat.getParticipants()) {
                if (!participantId.equals(currentUser.getUid())) {
                    return participantId; // Return the other user's ID for now
                }
            }
        }
        return "Unknown User";
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MessageListenerService started");
        return START_STICKY; // Restart service if killed by system
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MessageListenerService destroyed");
        
        if (chatListListener != null) {
            chatListListener.remove();
            chatListListener = null;
        }
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // This is a started service, not a bound service
    }
}
