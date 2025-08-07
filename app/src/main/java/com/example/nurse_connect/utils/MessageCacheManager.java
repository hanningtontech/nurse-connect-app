package com.example.nurse_connect.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.nurse_connect.models.GroupMessage;
import com.example.nurse_connect.models.Message;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages message caching for instant chat loading
 * Pre-loads and caches messages for all active chats
 */
public class MessageCacheManager {

    private static final String TAG = "MessageCacheManager";
    private static final String PREFS_NAME = "message_cache";
    private static final String GROUP_MESSAGES_PREFIX = "group_messages_";
    private static final String UNREAD_COUNT_PREFIX = "unread_count_";

    private static MessageCacheManager instance;

    private final Map<String, List<Message>> messageCache = new ConcurrentHashMap<>();
    private final Map<String, ListenerRegistration> chatListeners = new HashMap<>();
    private final FirebaseFirestore db;
    private final SharedPreferences prefs;
    private final Gson gson;

    private MessageCacheManager() {
        db = FirebaseFirestore.getInstance();
        prefs = null; // Will be initialized with context
        gson = new Gson();
    }

    public MessageCacheManager(Context context) {
        db = FirebaseFirestore.getInstance();
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }
    
    public static synchronized MessageCacheManager getInstance() {
        if (instance == null) {
            instance = new MessageCacheManager();
        }
        return instance;
    }
    
    /**
     * Pre-load messages for a specific chat
     */
    public void preloadMessagesForChat(String chatId) {
        Log.d(TAG, "Pre-loading messages for chat: " + chatId);
        
        // Remove existing listener if any
        if (chatListeners.containsKey(chatId)) {
            chatListeners.get(chatId).remove();
        }
        
        // Set up real-time listener for this chat
        ListenerRegistration listener = db.collection("private_chats")
                .document(chatId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading messages for chat " + chatId + ": " + error.getMessage());
                        return;
                    }
                    
                    if (value != null) {
                        List<Message> messages = new ArrayList<>();
                        for (QueryDocumentSnapshot document : value) {
                            Message message = document.toObject(Message.class);
                            if (message != null) {
                                message.setMessageId(document.getId());
                                messages.add(message);
                            }
                        }
                        
                        // Sort messages by timestamp
                        messages.sort((m1, m2) -> {
                            if (m1.getCreatedAt() != null && m2.getCreatedAt() != null) {
                                return m1.getCreatedAt().compareTo(m2.getCreatedAt());
                            }
                            return 0;
                        });
                        
                        // Cache the messages
                        messageCache.put(chatId, messages);
                        Log.d(TAG, "Cached " + messages.size() + " messages for chat: " + chatId);
                    }
                });
        
        chatListeners.put(chatId, listener);
    }
    
    /**
     * Get cached messages for a chat (returns immediately)
     */
    public List<Message> getCachedMessages(String chatId) {
        List<Message> cached = messageCache.get(chatId);
        if (cached != null) {
            Log.d(TAG, "Returning " + cached.size() + " cached messages for chat: " + chatId);
            return new ArrayList<>(cached); // Return a copy to prevent modification
        }
        Log.d(TAG, "No cached messages found for chat: " + chatId);
        return new ArrayList<>();
    }
    
    /**
     * Check if messages are cached for a chat
     */
    public boolean hasMessagesForChat(String chatId) {
        return messageCache.containsKey(chatId) && !messageCache.get(chatId).isEmpty();
    }
    
    /**
     * Pre-load messages for multiple chats
     */
    public void preloadMessagesForChats(List<String> chatIds) {
        Log.d(TAG, "Pre-loading messages for " + chatIds.size() + " chats");
        for (String chatId : chatIds) {
            preloadMessagesForChat(chatId);
        }
    }
    
    /**
     * Clear cache for a specific chat
     */
    public void clearCacheForChat(String chatId) {
        messageCache.remove(chatId);
        if (chatListeners.containsKey(chatId)) {
            chatListeners.get(chatId).remove();
            chatListeners.remove(chatId);
        }
        Log.d(TAG, "Cleared cache for chat: " + chatId);
    }
    
    /**
     * Clear all cached messages and listeners
     */
    public void clearAllCache() {
        Log.d(TAG, "Clearing all message cache");
        messageCache.clear();
        for (ListenerRegistration listener : chatListeners.values()) {
            listener.remove();
        }
        chatListeners.clear();
    }
    
    /**
     * Get the number of cached messages for a chat
     */
    public int getCachedMessageCount(String chatId) {
        List<Message> cached = messageCache.get(chatId);
        return cached != null ? cached.size() : 0;
    }

    // Group Messages Caching (SharedPreferences-based)
    public void cacheGroupMessages(String groupId, List<GroupMessage> messages) {
        if (prefs == null) return;
        String key = GROUP_MESSAGES_PREFIX + groupId;
        String json = gson.toJson(messages);
        prefs.edit().putString(key, json).apply();
        Log.d(TAG, "Cached " + messages.size() + " group messages for group: " + groupId);
    }

    public List<GroupMessage> getCachedGroupMessages(String groupId) {
        if (prefs == null) return new ArrayList<>();
        String key = GROUP_MESSAGES_PREFIX + groupId;
        String json = prefs.getString(key, null);
        if (json != null) {
            Type listType = new TypeToken<List<GroupMessage>>(){}.getType();
            List<GroupMessage> messages = gson.fromJson(json, listType);
            Log.d(TAG, "Retrieved " + messages.size() + " cached group messages for group: " + groupId);
            return messages;
        }
        return new ArrayList<>();
    }

    // Unread Count Management
    public void setUnreadCount(String chatId, int count) {
        if (prefs == null) return;
        String key = UNREAD_COUNT_PREFIX + chatId;
        prefs.edit().putInt(key, count).apply();
        Log.d(TAG, "Set unread count for " + chatId + ": " + count);
    }

    public int getUnreadCount(String chatId) {
        if (prefs == null) return 0;
        String key = UNREAD_COUNT_PREFIX + chatId;
        return prefs.getInt(key, 0);
    }

    public void clearUnreadCount(String chatId) {
        setUnreadCount(chatId, 0);
    }
}
