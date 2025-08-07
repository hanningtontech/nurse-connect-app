package com.example.nurse_connect.ui.chat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.nurse_connect.R;
import com.example.nurse_connect.adapters.PrivateMessageAdapter;
import com.example.nurse_connect.databinding.ActivityPrivateChatBinding;
import com.example.nurse_connect.models.Message;
import com.example.nurse_connect.models.PrivateChat;
import com.example.nurse_connect.models.User;
import com.example.nurse_connect.utils.MessageCacheManager;
import com.example.nurse_connect.ui.profile.FullScreenImageActivity;
import com.example.nurse_connect.ui.profile.UserProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.util.Log;

public class PrivateChatActivity extends AppCompatActivity {

    private ActivityPrivateChatBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private PrivateMessageAdapter messageAdapter;
    private List<Message> messagesList;
    
    private String otherUserId;
    private String otherUserName;
    private String otherUserPhotoUrl;
    private String chatId;
    private FirebaseUser currentUser;
    private com.google.firebase.firestore.ListenerRegistration messageListener;
    private com.google.firebase.firestore.ListenerRegistration messageStatusListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPrivateChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Please sign in to chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get data from intent
        otherUserId = getIntent().getStringExtra("other_user_id");
        otherUserName = getIntent().getStringExtra("other_user_name");
        otherUserPhotoUrl = getIntent().getStringExtra("other_user_photo");

        if (otherUserId == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        setupRecyclerView();
        createOrGetChatId();

        // Load cached messages first for instant display, then set up real-time listener
        loadCachedMessagesFirst();
        setupMessageListener();
    }

    private void setupUI() {
        // Setup back button
        binding.btnBack.setOnClickListener(v -> finish());
        
        // Set user info in toolbar
        if (otherUserName != null) {
            binding.toolbarTitle.setText(otherUserName);
        }
        
        // Make profile picture and name clickable
        binding.userPhoto.setOnClickListener(v -> openUserProfileImage());
        binding.toolbarTitle.setOnClickListener(v -> openUserProfile());
        
        // Load user photo
        if (otherUserPhotoUrl != null && !otherUserPhotoUrl.isEmpty()) {
            Glide.with(this)
                    .load(otherUserPhotoUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(binding.userPhoto);
        } else {
            binding.userPhoto.setImageResource(R.drawable.ic_profile_placeholder);
        }

        // Set chat background image
        String backgroundImageUrl = "https://firebasestorage.googleapis.com/v0/b/nurseconnect-c68eb.firebasestorage.app/o/nursing-avatars%2Fwallpapers%2Fnursing_wallpaper_dark.png?alt=media&token=01a3431d-b56f-4048-90e8-6e84e4f448fd";
        Glide.with(this)
                .load(backgroundImageUrl)
                .placeholder(R.drawable.chat_background)
                .error(R.drawable.chat_background)
                .centerCrop()
                .into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
                    @Override
                    public void onResourceReady(android.graphics.drawable.Drawable resource, com.bumptech.glide.request.transition.Transition<? super android.graphics.drawable.Drawable> transition) {
                        binding.recyclerView.setBackground(resource);
                    }

                    @Override
                    public void onLoadFailed(android.graphics.drawable.Drawable errorDrawable) {
                        binding.recyclerView.setBackgroundResource(R.drawable.chat_background);
                    }

                    @Override
                    public void onLoadCleared(android.graphics.drawable.Drawable placeholder) {
                        binding.recyclerView.setBackgroundResource(R.drawable.chat_background);
                    }
                });

        // Setup action buttons
        binding.btnVideoCall.setOnClickListener(v -> {
            Toast.makeText(this, "Video call feature coming soon!", Toast.LENGTH_SHORT).show();
        });
        
        binding.btnVoiceCall.setOnClickListener(v -> {
            initiateAudioCall();
        });
        
        binding.btnMore.setOnClickListener(v -> {
            Toast.makeText(this, "More options coming soon!", Toast.LENGTH_SHORT).show();
        });
        
        binding.btnAttach.setOnClickListener(v -> {
            Toast.makeText(this, "Attachment feature coming soon!", Toast.LENGTH_SHORT).show();
        });
        
        binding.btnCamera.setOnClickListener(v -> {
            Toast.makeText(this, "Camera feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Setup send button
        binding.btnSend.setOnClickListener(v -> sendMessage());
        
        // Setup message input
        binding.messageInput.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
        
        // Setup keyboard visibility listener
        setupKeyboardVisibilityListener();
        
        // Start monitoring online status
        startOnlineStatusMonitoring();
    }
    
    private void setupKeyboardVisibilityListener() {
        // Listen for layout changes to detect keyboard visibility
        binding.getRoot().getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            // Scroll to bottom when keyboard appears/disappears
            if (binding != null && messagesList != null && !messagesList.isEmpty()) {
                binding.recyclerView.post(() -> {
                    if (binding != null) {
                        binding.recyclerView.smoothScrollToPosition(messagesList.size() - 1);
                    }
                });
            }
        });
    }

    private void setupRecyclerView() {
        messagesList = new ArrayList<>();
        messageAdapter = new PrivateMessageAdapter(messagesList, currentUser.getUid());
        
        Log.d("PrivateChatActivity", "Setting up RecyclerView with currentUserId: " + currentUser.getUid());
        Log.d("PrivateChatActivity", "Other user ID: " + otherUserId);
        
        if (binding != null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setStackFromEnd(true); // This ensures latest messages are at the bottom
            binding.recyclerView.setLayoutManager(layoutManager);
            binding.recyclerView.setAdapter(messageAdapter);
            
            // Scroll to bottom when new message arrives
            messageAdapter.registerAdapterDataObserver(new androidx.recyclerview.widget.RecyclerView.AdapterDataObserver() {
                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    if (binding != null && messagesList != null && !messagesList.isEmpty()) {
                        binding.recyclerView.post(() -> {
                            if (binding != null) {
                                binding.recyclerView.smoothScrollToPosition(messagesList.size() - 1);
                            }
                        });
                    }
                }
            });
        }
    }

    private void createOrGetChatId() {
        // Create a unique chat ID by sorting user IDs alphabetically
        String user1 = currentUser.getUid();
        String user2 = otherUserId;

        if (user1.compareTo(user2) < 0) {
            chatId = user1 + "_" + user2;
        } else {
            chatId = user2 + "_" + user1;
        }

        // Ensure the chat document exists in Firestore
        createChatDocumentIfNotExists();
    }

    private void createChatDocumentIfNotExists() {
        db.collection("private_chats")
                .document(chatId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // Create the chat document
                        PrivateChat newChat = new PrivateChat();
                        newChat.setChatId(chatId);
                        newChat.setParticipants(List.of(currentUser.getUid(), otherUserId));
                        newChat.setLastMessage("");
                        newChat.setLastMessageSenderId("");
                        newChat.setLastMessageTime(new Date());
                        newChat.setUpdatedAt(new Date());

                        // Initialize unread counts for both users
                        Map<String, Object> unreadCounts = new HashMap<>();
                        unreadCounts.put(currentUser.getUid(), 0);
                        unreadCounts.put(otherUserId, 0);
                        newChat.setUnreadCounts(unreadCounts);

                        newChat.setPinned(false);
                        newChat.setMuted(false);

                        db.collection("private_chats")
                                .document(chatId)
                                .set(newChat)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("PrivateChatActivity", "Chat document created successfully: " + chatId);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("PrivateChatActivity", "Failed to create chat document: " + e.getMessage());
                                });
                    } else {
                        Log.d("PrivateChatActivity", "Chat document already exists: " + chatId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PrivateChatActivity", "Failed to check chat document existence: " + e.getMessage());
                });
    }

    private void loadCachedMessagesFirst() {
        Log.d("PrivateChatActivity", "Loading cached messages first for instant display");

        MessageCacheManager cacheManager = MessageCacheManager.getInstance();
        List<Message> cachedMessages = cacheManager.getCachedMessages(chatId);

        if (!cachedMessages.isEmpty()) {
            Log.d("PrivateChatActivity", "Found " + cachedMessages.size() + " cached messages - displaying instantly");

            messagesList.clear();
            messagesList.addAll(cachedMessages);

            if (messageAdapter != null) {
                messageAdapter.notifyDataSetChanged();
            }

            // Scroll to bottom immediately without animation to avoid flash
            if (binding != null) {
                binding.recyclerView.scrollToPosition(messagesList.size() - 1);
            }

            // Mark messages as read since user opened the chat
            markMessagesAsRead();
        } else {
            Log.d("PrivateChatActivity", "No cached messages found, will load from Firestore");
            // If no cache, load normally but this should be rare after pre-loading is implemented
            loadMessages();
        }
    }

    private void loadMessages() {
        Log.d("PrivateChatActivity", "Loading messages for chatId: " + chatId);

        // First, try to load from cache for instant display
        MessageCacheManager cacheManager = MessageCacheManager.getInstance();
        List<Message> cachedMessages = cacheManager.getCachedMessages(chatId);

        if (!cachedMessages.isEmpty()) {
            Log.d("PrivateChatActivity", "Loading " + cachedMessages.size() + " messages from cache (instant)");

            messagesList.clear();
            messagesList.addAll(cachedMessages);

            if (messageAdapter != null) {
                messageAdapter.notifyDataSetChanged();
            }

            // Scroll to bottom immediately
            if (binding != null) {
                binding.recyclerView.post(() -> {
                    binding.recyclerView.scrollToPosition(messagesList.size() - 1);
                });
            }

            // Mark messages as read since user opened the chat
            markMessagesAsRead();

            // Immediately notify DirectMessagesFragment to clear the badge
            notifyDirectMessagesFragmentBadgeCleared();

            Log.d("PrivateChatActivity", "Messages loaded instantly from cache");
        } else {
            Log.d("PrivateChatActivity", "No cached messages, loading from Firestore");

            // Fallback: Load from Firestore if not cached
            db.collection("private_chats")
                    .document(chatId)
                    .collection("messages")
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (binding == null) return;

                        Log.d("PrivateChatActivity", "Loaded " + querySnapshot.size() + " messages from Firestore");

                        messagesList.clear();
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            Message message = document.toObject(Message.class);
                            if (message != null) {
                                message.setMessageId(document.getId());
                                messagesList.add(message);
                            }
                        }

                        // Sort messages by timestamp
                        messagesList.sort((m1, m2) -> {
                            if (m1.getCreatedAt() != null && m2.getCreatedAt() != null) {
                                return m1.getCreatedAt().compareTo(m2.getCreatedAt());
                            }
                            return 0;
                        });

                        if (messageAdapter != null) {
                            messageAdapter.notifyDataSetChanged();
                        }

                        // Scroll to bottom
                        if (!messagesList.isEmpty() && binding != null) {
                            binding.recyclerView.post(() -> {
                                binding.recyclerView.scrollToPosition(messagesList.size() - 1);
                            });
                        }

                        // Mark messages as read
                        markMessagesAsRead();

                        // Immediately notify DirectMessagesFragment to clear the badge
                        notifyDirectMessagesFragmentBadgeCleared();
                    })
                    .addOnFailureListener(e -> {
                        if (binding != null) {
                            Toast.makeText(this, "Failed to load messages", Toast.LENGTH_SHORT).show();
                        }
                        Log.e("PrivateChatActivity", "Failed to load messages", e);
                    });
        }
    }

    private void setupMessageListener() {
        messageListener = db.collection("private_chats")
                .document(chatId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || binding == null) {
                        return;
                    }

                    if (value != null) {
                        messagesList.clear();
                        for (QueryDocumentSnapshot document : value) {
                            Message message = document.toObject(Message.class);
                            if (message != null) {
                                message.setMessageId(document.getId());
                                messagesList.add(message);
                            }
                        }
                        
                        // Sort messages by timestamp to ensure proper chronological order
                        messagesList.sort((m1, m2) -> {
                            if (m1.getCreatedAt() != null && m2.getCreatedAt() != null) {
                                return m1.getCreatedAt().compareTo(m2.getCreatedAt());
                            }
                            return 0;
                        });
                        
                        Log.d("PrivateChatActivity", "Real-time messages after sorting:");
                        for (int i = 0; i < messagesList.size(); i++) {
                            Message msg = messagesList.get(i);
                            Log.d("PrivateChatActivity", "Position " + i + ": " + msg.getContent() + " from " + msg.getSenderId() + " at " + msg.getCreatedAt());
                        }
                        
                        if (messageAdapter != null) {
                            messageAdapter.notifyDataSetChanged();
                        }
                        
                        // Scroll to bottom to show latest messages
                        if (!messagesList.isEmpty() && binding != null) {
                            binding.recyclerView.post(() -> {
                                binding.recyclerView.smoothScrollToPosition(messagesList.size() - 1);
                            });
                        }
                    }
                });
        
        // Add a separate listener for message status changes
        setupMessageStatusListener();
    }
    
    private void setupMessageStatusListener() {
        // Listen for status changes on messages sent by current user
        messageStatusListener = db.collection("private_chats")
                .document(chatId)
                .collection("messages")
                .whereEqualTo("senderId", currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        return;
                    }
                    
                    for (QueryDocumentSnapshot document : value) {
                        Message message = document.toObject(Message.class);
                        if (message != null) {
                            message.setMessageId(document.getId());
                            // Update the message status in the adapter
                            if (messageAdapter != null) {
                                messageAdapter.updateMessageStatus(message.getMessageId(), message.getStatus());
                            }
                        }
                    }
                });
    }

    private void sendMessage() {
        if (binding == null || currentUser == null) {
            return;
        }
        
        String messageText = binding.messageInput.getText().toString().trim();
        
        if (messageText.isEmpty()) {
            return;
        }

        // Clear input
        binding.messageInput.setText("");

        // Get current user's profile picture
        String senderPhotoUrl = currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "";

        // Create message object
        Message message = new Message();
        message.setSenderId(currentUser.getUid());
        message.setSenderName(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : currentUser.getEmail());
        message.setSenderPhotoUrl(senderPhotoUrl);
        message.setContent(messageText);
        message.setCreatedAt(new Timestamp(new Date()));
        message.setType(Message.MessageType.TEXT);
        message.setStatus(Message.MessageStatus.SENT); // Start with sent status

        Log.d("PrivateChatActivity", "Sending message: " + messageText + " with senderId: " + message.getSenderId());

        // Save message to Firestore
        db.collection("private_chats")
                .document(chatId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    Log.d("PrivateChatActivity", "Message saved successfully with ID: " + documentReference.getId());
                    // Update chat metadata
                    updateChatMetadata(messageText);
                    sendNotificationToRecipient(messageText);
                    
                    // Mark message as delivered after a short delay
                    new android.os.Handler().postDelayed(() -> {
                        documentReference.update("status", "DELIVERED");
                    }, 1000); // 1 second delay to simulate delivery
                    
                    if (binding != null) {
                        Toast.makeText(this, "Message sent!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PrivateChatActivity", "Failed to send message", e);
                    if (binding != null) {
                        Toast.makeText(this, "Failed to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateChatMetadata(String lastMessage) {
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("lastMessage", lastMessage);
        chatData.put("lastMessageSenderId", currentUser.getUid());
        chatData.put("lastMessageTime", new Date());
        chatData.put("participants", List.of(currentUser.getUid(), otherUserId));
        chatData.put("updatedAt", new Date());
        
        // Increment unread count for the other user
        db.collection("private_chats")
                .document(chatId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> currentData = documentSnapshot.getData();
                        if (currentData != null) {
                            // Get current unread count for the other user
                            Map<String, Object> unreadCountsRaw = (Map<String, Object>) currentData.get("unreadCounts");
                            Map<String, Object> unreadCounts = new HashMap<>();
                            
                            if (unreadCountsRaw != null) {
                                // Copy existing unread counts
                                unreadCounts.putAll(unreadCountsRaw);
                            }
                            
                            // Increment unread count for the other user
                            Object currentCountObj = unreadCounts.get(otherUserId);
                            int currentCount = 0;
                            if (currentCountObj instanceof Long) {
                                currentCount = ((Long) currentCountObj).intValue();
                            } else if (currentCountObj instanceof Integer) {
                                currentCount = (Integer) currentCountObj;
                            }
                            
                            unreadCounts.put(otherUserId, currentCount + 1);
                            
                            chatData.put("unreadCounts", unreadCounts);
                        }
                    }
                    
                    // Update the chat document using merge to preserve existing data
                    db.collection("private_chats")
                            .document(chatId)
                            .set(chatData, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                System.out.println("PrivateChatActivity: Chat metadata updated successfully");
                            })
                            .addOnFailureListener(e -> {
                                System.out.println("PrivateChatActivity: Failed to update chat metadata: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    // If we can't get current data, just update with basic info
                    db.collection("private_chats")
                            .document(chatId)
                            .set(chatData, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                System.out.println("PrivateChatActivity: Chat metadata updated with basic info");
                            })
                            .addOnFailureListener(e2 -> {
                                System.out.println("PrivateChatActivity: Failed to update chat metadata: " + e2.getMessage());
                            });
                });
    }

    private void sendNotificationToRecipient(String messageText) {
        // Create a notification document for the recipient
        Map<String, Object> notification = new HashMap<>();
        notification.put("recipientId", otherUserId);
        notification.put("senderId", currentUser.getUid());
        notification.put("senderName", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : currentUser.getEmail());
        notification.put("message", messageText);
        notification.put("chatId", chatId);
        notification.put("timestamp", new Date());
        notification.put("read", false);

        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    // Notification sent successfully
                })
                .addOnFailureListener(e -> {
                    // Handle notification error silently
                });
    }

    private void startOnlineStatusMonitoring() {
        // Monitor user's online status
        db.collection("users")
                .document(otherUserId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || binding == null) {
                        if (binding != null) {
                            binding.userStatus.setText("offline");
                        }
                        return;
                    }
                    
                    if (snapshot.exists()) {
                        Boolean isOnline = snapshot.getBoolean("isOnline");
                        Long lastSeen = snapshot.getLong("lastSeen");
                        
                        if (isOnline != null && isOnline) {
                            binding.userStatus.setText("online");
                        } else if (lastSeen != null) {
                            long currentTime = System.currentTimeMillis();
                            long timeDifference = currentTime - lastSeen;
                            
                            // Consider user online if they were active in the last 2 minutes
                            if (timeDifference < 2 * 60 * 1000) {
                                binding.userStatus.setText("online");
                            } else {
                                binding.userStatus.setText("offline");
                            }
                        } else {
                            binding.userStatus.setText("offline");
                        }
                    } else {
                        binding.userStatus.setText("offline");
                    }
                });
    }
    
    private void openUserProfile() {
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra("user_id", otherUserId);
        startActivity(intent);
    }
    
    private void openUserProfileImage() {
        if (otherUserPhotoUrl != null && !otherUserPhotoUrl.isEmpty()) {
            // Open full-screen image view
            Intent intent = new Intent(this, FullScreenImageActivity.class);
            intent.putExtra("image_url", otherUserPhotoUrl);
            intent.putExtra("user_name", otherUserName);
            startActivity(intent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Set user as offline when leaving the activity
        if (currentUser != null) {
            setUserOffline();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Immediately clear the badge in DirectMessagesFragment
        notifyDirectMessagesFragmentBadgeCleared();

        // Mark messages as read when user opens the conversation
        markMessagesAsRead();

        // Update current user's last seen
        updateCurrentUserLastSeen();
    }
    
    private void markMessagesAsRead() {
        // Mark all messages from the other user as read
        db.collection("private_chats")
                .document(chatId)
                .collection("messages")
                .whereEqualTo("senderId", otherUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String currentStatus = document.getString("status");
                        // Only update if not already READ
                        if (!"READ".equals(currentStatus)) {
                            // Update the message status in Firestore
                            document.getReference().update("status", "READ", "readAt", new Timestamp(new Date()));
                            
                            // Update the message in the adapter immediately
                            String messageId = document.getId();
                            if (messageAdapter != null) {
                                messageAdapter.updateMessageStatus(messageId, Message.MessageStatus.READ);
                            }
                        }
                    }
                    
                    // Reset unread count for current user
                    resetUnreadCountForCurrentUser();
                })
                .addOnFailureListener(e -> {
                    // Handle error silently
                });
    }

    private void notifyDirectMessagesFragmentBadgeCleared() {
        // Send broadcast to immediately clear the badge in DirectMessagesFragment
        Intent broadcastIntent = new Intent("com.example.nurse_connect.BADGE_CLEARED");
        broadcastIntent.putExtra("chatId", chatId);
        sendBroadcast(broadcastIntent);

        Log.d("PrivateChatActivity", "Sent badge cleared broadcast for chat: " + chatId);
    }
    
    private void resetUnreadCountForCurrentUser() {
        db.collection("private_chats")
                .document(chatId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> currentData = documentSnapshot.getData();
                        if (currentData != null) {
                            // Get current unread counts - fix type casting issue
                            Map<String, Object> unreadCountsRaw = (Map<String, Object>) currentData.get("unreadCounts");
                            Map<String, Object> unreadCounts = new HashMap<>();

                            if (unreadCountsRaw != null) {
                                // Copy existing unread counts
                                unreadCounts.putAll(unreadCountsRaw);
                            }

                            // Reset unread count for current user
                            unreadCounts.put(currentUser.getUid(), 0);

                            // Update the chat document
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("unreadCounts", unreadCounts);

                            db.collection("private_chats")
                                    .document(chatId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("PrivateChatActivity", "Unread count reset successfully for user: " + currentUser.getUid());
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("PrivateChatActivity", "Failed to reset unread count: " + e.getMessage());
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PrivateChatActivity", "Failed to get chat document for unread reset: " + e.getMessage());
                });
    }
    
    // Test method to manually update a message status to READ
    private void testUpdateMessageStatus(String messageId) {
        if (messageAdapter != null) {
            messageAdapter.updateMessageStatus(messageId, Message.MessageStatus.READ);
        }
    }
    
    private void updateCurrentUserLastSeen() {
        // Update current user's last seen timestamp and online status
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastSeen", System.currentTimeMillis());
        updates.put("isOnline", true);
        
        db.collection("users")
                .document(currentUser.getUid())
                .update(updates);
    }

    private void setUserOffline() {
        // Set current user as offline
        Map<String, Object> updates = new HashMap<>();
        updates.put("isOnline", false);
        updates.put("lastSeen", System.currentTimeMillis());
        
        db.collection("users")
                .document(currentUser.getUid())
                .update(updates);
    }

    public void addMessageInOrder(Message newMessage) {
        // Find the correct position to insert the new message
        int insertPosition = 0;
        for (int i = 0; i < messagesList.size(); i++) {
            if (messagesList.get(i).getCreatedAt() != null && newMessage.getCreatedAt() != null) {
                if (newMessage.getCreatedAt().compareTo(messagesList.get(i).getCreatedAt()) < 0) {
                    insertPosition = i;
                    break;
                }
            }
            insertPosition = i + 1;
        }
        
        messagesList.add(insertPosition, newMessage);
        if (messageAdapter != null) {
            messageAdapter.notifyItemInserted(insertPosition);
        }
    }

    private void initiateAudioCall() {
        if (otherUserId == null) {
            Toast.makeText(this, "Unable to start call", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for audio permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    AUDIO_PERMISSION_REQUEST_CODE);
            return;
        }

        // Start the audio call activity
        Intent callIntent = new Intent(this, AudioCallActivity.class);
        callIntent.putExtra("otherUserId", otherUserId);
        callIntent.putExtra("otherUserName", binding.toolbarTitle.getText().toString());
        callIntent.putExtra("isOutgoing", true);
        startActivity(callIntent);
    }

    private static final int AUDIO_PERMISSION_REQUEST_CODE = 1001;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initiateAudioCall();
            } else {
                Toast.makeText(this, "Audio permission is required for voice calls", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Set user as offline
        if (currentUser != null) {
            setUserOffline();
        }
        
        // Remove the Firestore listener to prevent memory leaks and crashes
        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }
        if (messageStatusListener != null) {
            messageStatusListener.remove();
            messageStatusListener = null;
        }
        
        binding = null;
    }
} 