package com.example.nurse_connect.ui.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nurse_connect.adapters.PrivateChatAdapter;
import com.example.nurse_connect.databinding.FragmentDirectMessagesBinding;
import com.example.nurse_connect.models.PrivateChat;
import com.example.nurse_connect.receivers.MessageReceiver;
import com.example.nurse_connect.services.MessageListenerService;
import com.example.nurse_connect.utils.MessageCacheManager;
import com.example.nurse_connect.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectMessagesFragment extends Fragment implements PrivateChatAdapter.OnChatClickListener, MessageReceiver.MessageUpdateListener {
    
    private FragmentDirectMessagesBinding binding;
    private PrivateChatAdapter adapter;
    private List<PrivateChat> chatList;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private com.google.firebase.firestore.ListenerRegistration chatListListener;
    private BroadcastReceiver messageReceiver;
    private Map<String, com.google.firebase.firestore.ListenerRegistration> messageListeners;
    private Map<String, String> lastProcessedMessages;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDirectMessagesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupFirebase();
        setupUI();
        setupMessageReceiver();
        startMessageListenerService();
        loadPrivateChats();
    }
    
    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        messageListeners = new HashMap<>();
        lastProcessedMessages = new HashMap<>();
    }
    
    private void setupUI() {
        chatList = new ArrayList<>();
        adapter = new PrivateChatAdapter(chatList, this);
        binding.rvDirectMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvDirectMessages.setAdapter(adapter);

        // Setup swipe refresh
        binding.swipeRefresh.setOnRefreshListener(() -> {
            Log.d("DirectMessagesFragment", "Manual refresh triggered");
            loadPrivateChats();

            // For testing: create a test chat with unread messages
            // Remove this line after testing
            createTestChatWithUnreadMessages();
        });

        // Setup new conversation button
        binding.fabNewConversation.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), NewConversationActivity.class);
            startActivity(intent);
        });
    }
    
    private void loadPrivateChats() {
        if (currentUser == null) {
            showEmptyState();
            return;
        }
        
        // Query private chats where current user is a participant with real-time updates
        Log.d("DirectMessagesFragment", "Setting up Firestore listener for user: " + currentUser.getUid());

        // Store the listener registration for proper cleanup
        chatListListener = db.collection("private_chats")
                .whereArrayContains("participants", currentUser.getUid())
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    Log.d("DirectMessagesFragment", "Firestore listener triggered! Documents: " + (value != null ? value.size() : 0));

                    if (binding != null) {
                        binding.swipeRefresh.setRefreshing(false);
                    }

                    if (error != null) {
                        Log.e("DirectMessagesFragment", "Error loading chats: " + error.getMessage());
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Failed to load chats", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    // Store previous chat list for comparison
                    List<PrivateChat> previousChatList = new ArrayList<>(chatList);
                    
                    chatList.clear();
                    if (value != null && !value.isEmpty()) {
                        System.out.println("DirectMessagesFragment: Loading " + value.size() + " chats from Firestore");
                        for (QueryDocumentSnapshot document : value) {
                            PrivateChat chat = document.toObject(PrivateChat.class);
                            if (chat != null) {
                                chat.setChatId(document.getId());
                                chatList.add(chat);
                                
                                // Debug: Log unread count information
                                int unreadCount = chat.getUnreadCountForUser(currentUser.getUid());
                                Log.d("DirectMessagesFragment", "Added chat " + document.getId() +
                                                 " with last message: " + chat.getLastMessage() +
                                                 " updated at: " + chat.getUpdatedAt() +
                                                 " unread count: " + unreadCount +
                                                 " unreadCounts map: " + chat.getUnreadCounts() +
                                                 " last sender: " + chat.getLastMessageSenderId());
                            }
                        }
                        
                        // Sort chats: unread messages first, then by timestamp
                        sortChatsByPriority();

                        // Pre-load messages for all chats to eliminate loading delays
                        preloadMessagesForAllChats();

                        showChatList();
                    } else {
                        Log.d("DirectMessagesFragment", "No real chats found in Firestore, adding sample data with badges");
                        // Add sample data with proper unread counts to test badge functionality
                        addSampleChats();
                        sortChatsByPriority();
                        showChatList();
                    }
                    
                    // Use more efficient adapter updates for better performance
                    adapter.notifyDataSetChanged();

                    // Check for new messages and show notifications
                    checkForNewMessages(value);
                    
                    // Debug: Log the chat list order
                    System.out.println("DirectMessagesFragment: Final chat list order:");
                    for (int i = 0; i < Math.min(chatList.size(), 3); i++) {
                        PrivateChat chat = chatList.get(i);
                        if (chat.getLastMessageTime() != null) {
                            System.out.println("Chat " + i + ": " + chat.getLastMessage() + " at " + chat.getLastMessageTime());
                        }
                    }
                });
    }
    
    private void checkForNewMessages(com.google.firebase.firestore.QuerySnapshot value) {
        if (value == null) return;
        
        System.out.println("Checking " + value.size() + " chats for new messages");
        
        boolean hasNewMessages = false;
        
        for (QueryDocumentSnapshot document : value) {
            PrivateChat chat = document.toObject(PrivateChat.class);
            if (chat != null) {
                chat.setChatId(document.getId());
                
                // Debug: Log chat info
                System.out.println("Chat: " + chat.getChatId() + 
                                 " | Last message: " + chat.getLastMessage() + 
                                 " | Updated at: " + chat.getUpdatedAt());
                
                // Check if this is a new message from someone else
                if (chat.getLastMessageSenderId() != null && 
                    !chat.getLastMessageSenderId().equals(currentUser.getUid()) &&
                    chat.getUnreadCountForUser(currentUser.getUid()) > 0) {
                    
                    // Show notification for new message
                    showNewMessageNotification(chat);
                    hasNewMessages = true;
                }
            }
        }
        
        // If there are new messages, re-sort the chat list to move unread chats to top
        if (hasNewMessages) {
            sortChatsByPriority();
            adapter.notifyDataSetChanged();
        }
    }
    
    // Simplified message listener - removed to prevent conflicts
    // The main chat listener will handle real-time updates automatically
    
    // Simplified - removed complex update methods
    // The main chat listener will handle real-time updates automatically
    

    
    private void showNewMessageNotification(PrivateChat chat) {
        // Get the other user's name
        String otherUserId = null;
        for (String participantId : chat.getParticipants()) {
            if (!participantId.equals(currentUser.getUid())) {
                otherUserId = participantId;
                break;
            }
        }
        
        if (otherUserId != null) {
            // Create final copy for lambda
            final String finalOtherUserId = otherUserId;
            
            db.collection("users")
                    .document(finalOtherUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String senderName = documentSnapshot.getString("username");
                            String messagePreview = chat.getLastMessage();
                            String otherUserPhoto = documentSnapshot.getString("photoURL");
                            
                            // Show system notification
                            NotificationHelper.showNewMessageNotification(
                                    getContext(),
                                    senderName,
                                    messagePreview,
                                    finalOtherUserId,
                                    senderName,
                                    otherUserPhoto
                            );
                            
                            // Also show toast for immediate feedback
                            String notificationText = senderName + ": " + messagePreview;
                            Toast.makeText(getContext(), notificationText, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
    
    private void addSampleChats() {
        // Sample chat 1 - Bloodstrike (no unread - sent by current user)
        PrivateChat chat1 = new PrivateChat();
        chat1.setChatId("sample1");
        chat1.setParticipants(List.of(currentUser.getUid(), "user1"));
        chat1.setLastMessage("😂😂😂😂 peacefully");
        chat1.setLastMessageSenderId(currentUser.getUid());
        chat1.setLastMessageTime(new Date(System.currentTimeMillis() - 2 * 60 * 60 * 1000)); // 2 hours ago

        // Set unread count using the new map structure (no unread since current user sent last message)
        Map<String, Object> unreadCounts1 = new HashMap<>();
        unreadCounts1.put(currentUser.getUid(), 0); // No unread for current user
        unreadCounts1.put("user1", 0); // No unread for the other user
        chat1.setUnreadCounts(unreadCounts1);

        chat1.setPinned(true);
        chat1.setMuted(false);
        chatList.add(chat1);

        // Sample chat 2 - Essie (with 3 unread messages)
        PrivateChat chat2 = new PrivateChat();
        chat2.setChatId("sample2");
        chat2.setParticipants(List.of(currentUser.getUid(), "user2"));
        chat2.setLastMessage("https://youtube.com/live/IN0xay...");
        chat2.setLastMessageSenderId("user2");
        chat2.setLastMessageTime(new Date(System.currentTimeMillis() - 30 * 60 * 1000)); // 30 minutes ago

        // Set unread count using the new map structure (3 unread messages)
        Map<String, Object> unreadCounts2 = new HashMap<>();
        unreadCounts2.put(currentUser.getUid(), 3); // 3 unread messages for current user
        unreadCounts2.put("user2", 0); // No unread for the other user
        chat2.setUnreadCounts(unreadCounts2);

        chat2.setPinned(true);
        chat2.setMuted(false);
        chatList.add(chat2);
        
        // Sample chat 3 - MEDIA TEAM 2025 (with high unread count)
        PrivateChat chat3 = new PrivateChat();
        chat3.setChatId("sample3");
        chat3.setParticipants(List.of(currentUser.getUid(), "user3"));
        chat3.setLastMessage("+254 110 308032: Mtu haw...");
        chat3.setLastMessageSenderId("user3");
        chat3.setLastMessageTime(new Date(System.currentTimeMillis() - 5 * 60 * 1000)); // 5 minutes ago

        // Set unread count using the new map structure
        Map<String, Object> unreadCounts3 = new HashMap<>();
        unreadCounts3.put(currentUser.getUid(), 208); // High unread count for current user
        unreadCounts3.put("user3", 0); // No unread for the other user
        chat3.setUnreadCounts(unreadCounts3);

        chat3.setPinned(false);
        chat3.setMuted(true);
        chatList.add(chat3);

        // Sample chat 4 - Muse (with 1 unread message)
        PrivateChat chat4 = new PrivateChat();
        chat4.setChatId("sample4");
        chat4.setParticipants(List.of(currentUser.getUid(), "user4"));
        chat4.setLastMessage("Walai tena");
        chat4.setLastMessageSenderId("user4");
        chat4.setLastMessageTime(new Date(System.currentTimeMillis() - 1 * 60 * 1000)); // 1 minute ago

        // Set unread count using the new map structure
        Map<String, Object> unreadCounts4 = new HashMap<>();
        unreadCounts4.put(currentUser.getUid(), 1); // 1 unread message for current user
        unreadCounts4.put("user4", 0); // No unread for the other user
        chat4.setUnreadCounts(unreadCounts4);

        chat4.setPinned(false);
        chat4.setMuted(false);
        chatList.add(chat4);
    }
    
    private void showChatList() {
        binding.emptyState.setVisibility(View.GONE);
        binding.rvDirectMessages.setVisibility(View.VISIBLE);

        // Update total unread count for potential use in parent fragment
        updateTotalUnreadCount();
    }

    private void updateTotalUnreadCount() {
        if (adapter != null) {
            int totalUnread = adapter.getTotalUnreadCount();
            Log.d("DirectMessagesFragment", "Total unread messages across all chats: " + totalUnread);

            // You can use this to update a badge on the Chat tab in the bottom navigation
            // For now, we'll just log it, but you could pass this to the parent activity
        }
    }
    
    private void showEmptyState() {
        binding.emptyState.setVisibility(View.VISIBLE);
        binding.rvDirectMessages.setVisibility(View.GONE);
    }
    
    @Override
    public void onChatClick(PrivateChat chat) {
        // Get the other user's ID and info
        String otherUserId = null;
        for (String participantId : chat.getParticipants()) {
            if (!participantId.equals(currentUser.getUid())) {
                otherUserId = participantId;
                break;
            }
        }

        if (otherUserId != null) {
            // Create final copy for lambda
            final String finalOtherUserId = otherUserId;
            
            // Get other user's info
            db.collection("users")
                    .document(finalOtherUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String otherUserName = documentSnapshot.getString("username");
                            String otherUserPhoto = documentSnapshot.getString("photoURL");

                            // Open private chat
                            Intent intent = new Intent(getContext(), PrivateChatActivity.class);
                            intent.putExtra("other_user_id", finalOtherUserId);
                            intent.putExtra("other_user_name", otherUserName);
                            intent.putExtra("other_user_photo", otherUserPhoto);
                            startActivity(intent);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to load user info", Toast.LENGTH_SHORT).show();
                    });
        }
    }
    
    private void sortChatsByPriority() {
        // Sort chats by priority: unread messages first, then by timestamp
        chatList.sort((chat1, chat2) -> {
            int unread1 = chat1.getUnreadCountForUser(currentUser.getUid());
            int unread2 = chat2.getUnreadCountForUser(currentUser.getUid());
            
            // If both have unread messages, sort by timestamp (newest first)
            if (unread1 > 0 && unread2 > 0) {
                if (chat1.getLastMessageTime() != null && chat2.getLastMessageTime() != null) {
                    return chat2.getLastMessageTime().compareTo(chat1.getLastMessageTime());
                }
                return 0;
            }
            
            // If only one has unread messages, prioritize it
            if (unread1 > 0 && unread2 == 0) {
                return -1; // chat1 comes first
            }
            if (unread1 == 0 && unread2 > 0) {
                return 1; // chat2 comes first
            }
            
            // If both have no unread messages, sort by timestamp (newest first)
            if (chat1.getLastMessageTime() != null && chat2.getLastMessageTime() != null) {
                return chat2.getLastMessageTime().compareTo(chat1.getLastMessageTime());
            }
            
            return 0;
        });
    }
    
    @Override
    public void onStart() {
        super.onStart();
        // Refresh chat list when fragment becomes visible
        if (chatList != null && !chatList.isEmpty()) {
            sortChatsByPriority();
            adapter.notifyDataSetChanged();
        }
    }
    

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Clean up the Firestore listener to prevent memory leaks
        if (chatListListener != null) {
            chatListListener.remove();
            chatListListener = null;
            Log.d("DirectMessagesFragment", "Chat list listener removed");
        }

        // Unregister broadcast receiver
        if (messageReceiver != null && getContext() != null) {
            try {
                getContext().unregisterReceiver(messageReceiver);
                Log.d("DirectMessagesFragment", "Message receiver unregistered");
            } catch (IllegalArgumentException e) {
                Log.w("DirectMessagesFragment", "Message receiver was not registered");
            }
            messageReceiver = null;
        }

        // Clear the message update listener
        MessageReceiver.setMessageUpdateListener(null);

        binding = null;
    }

    // Method to manually refresh chat list for testing
    public void refreshChatList() {
        Log.d("DirectMessagesFragment", "Manual refresh triggered");
        if (currentUser != null) {
            loadPrivateChats();
        }
    }

    // Method to enable sample data for testing
    public void enableSampleData() {
        Log.d("DirectMessagesFragment", "Enabling sample data for testing");
        addSampleChats();
        sortChatsByPriority();
        showChatList();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    // Method to create a test chat with unread messages in Firestore
    public void createTestChatWithUnreadMessages() {
        if (currentUser == null) return;

        String testChatId = currentUser.getUid() + "_test_user";

        // Create a test chat document in Firestore
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("chatId", testChatId);
        chatData.put("participants", List.of(currentUser.getUid(), "test_user"));
        chatData.put("lastMessage", "This is a test message for badge testing");
        chatData.put("lastMessageSenderId", "test_user"); // Message from other user
        chatData.put("lastMessageTime", new Date());
        chatData.put("updatedAt", new Date());

        // Set unread count for current user
        Map<String, Object> unreadCounts = new HashMap<>();
        unreadCounts.put(currentUser.getUid(), 5); // 5 unread messages for current user
        unreadCounts.put("test_user", 0); // No unread for test user
        chatData.put("unreadCounts", unreadCounts);

        chatData.put("isPinned", false);
        chatData.put("isMuted", false);

        db.collection("private_chats")
                .document(testChatId)
                .set(chatData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("DirectMessagesFragment", "Test chat created successfully with unread count");
                    Toast.makeText(getContext(), "Test chat created - check for badge!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("DirectMessagesFragment", "Failed to create test chat: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to create test chat", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupMessageReceiver() {
        // Set up broadcast receiver for new message notifications
        messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if ("com.example.nurse_connect.NEW_MESSAGE".equals(action)) {
                    String chatId = intent.getStringExtra("chatId");
                    int unreadCount = intent.getIntExtra("unreadCount", 0);
                    String lastMessage = intent.getStringExtra("lastMessage");
                    String senderName = intent.getStringExtra("senderName");

                    Log.d("DirectMessagesFragment", "Received new message broadcast for chat: " + chatId);
                    onNewMessageReceived(chatId, unreadCount, lastMessage, senderName);

                } else if ("com.example.nurse_connect.BADGE_CLEARED".equals(action)) {
                    String chatId = intent.getStringExtra("chatId");

                    Log.d("DirectMessagesFragment", "Received badge cleared broadcast for chat: " + chatId);
                    clearBadgeForChat(chatId);
                }
            }
        };

        // Register the broadcast receiver for both new messages and badge clearing
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.nurse_connect.NEW_MESSAGE");
        filter.addAction("com.example.nurse_connect.BADGE_CLEARED");

        if (getContext() != null) {
            getContext().registerReceiver(messageReceiver, filter);
            Log.d("DirectMessagesFragment", "Message receiver registered for new messages and badge clearing");
        }

        // Set this fragment as the message update listener
        MessageReceiver.setMessageUpdateListener(this);
    }

    private void startMessageListenerService() {
        if (getContext() != null && currentUser != null) {
            Intent serviceIntent = new Intent(getContext(), MessageListenerService.class);
            getContext().startService(serviceIntent);
            Log.d("DirectMessagesFragment", "Message listener service started");
        }
    }

    @Override
    public void onNewMessageReceived(String chatId, int unreadCount, String lastMessage, String senderName) {
        Log.d("DirectMessagesFragment", "Processing new message for chat: " + chatId + " with unread count: " + unreadCount);

        // Update the specific chat's unread count
        if (adapter != null) {
            adapter.updateChatUnreadCount(chatId, unreadCount);
        }

        // Show a toast notification for testing
        if (getContext() != null) {
            Toast.makeText(getContext(), "New message from " + senderName + ": " + lastMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private void preloadMessagesForAllChats() {
        if (chatList == null || chatList.isEmpty()) {
            Log.d("DirectMessagesFragment", "No chats to preload messages for");
            return;
        }

        Log.d("DirectMessagesFragment", "Pre-loading messages for " + chatList.size() + " chats");

        List<String> chatIds = new ArrayList<>();
        for (PrivateChat chat : chatList) {
            if (chat.getChatId() != null && !chat.getChatId().isEmpty()) {
                chatIds.add(chat.getChatId());
            }
        }

        // Use the message cache manager to pre-load messages
        MessageCacheManager.getInstance().preloadMessagesForChats(chatIds);

        Log.d("DirectMessagesFragment", "Started pre-loading for " + chatIds.size() + " chat IDs");
    }

    private void clearBadgeForChat(String chatId) {
        Log.d("DirectMessagesFragment", "Clearing badge for chat: " + chatId);

        // Find the chat in the list and clear its unread count
        for (int i = 0; i < chatList.size(); i++) {
            PrivateChat chat = chatList.get(i);
            if (chat.getChatId().equals(chatId)) {
                // Clear unread count for current user
                if (chat.getUnreadCounts() != null) {
                    chat.getUnreadCounts().put(currentUser.getUid(), 0);
                }

                // Notify adapter to update this specific item
                if (adapter != null) {
                    adapter.notifyItemChanged(i);
                    Log.d("DirectMessagesFragment", "Badge cleared and adapter updated for position: " + i);
                }
                break;
            }
        }
    }
} 