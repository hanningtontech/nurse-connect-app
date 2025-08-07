package com.example.nurse_connect.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nurse_connect.R;
import com.example.nurse_connect.adapters.GroupMessageAdapter;
import com.example.nurse_connect.databinding.ActivityGroupChatBinding;
import com.example.nurse_connect.models.GroupChat;
import com.example.nurse_connect.models.GroupMessage;
import com.example.nurse_connect.utils.MessageCacheManager;
import com.example.nurse_connect.utils.SwipeToReplyHelper;
import com.example.nurse_connect.utils.ThemeManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
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

public class GroupChatActivity extends AppCompatActivity implements
        GroupMessageAdapter.OnMessageActionListener,
        SwipeToReplyHelper.SwipeToReplyListener {

    private ActivityGroupChatBinding binding;
    private GroupMessageAdapter adapter;
    private List<GroupMessage> messageList;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String groupId;
    private String groupTitle;
    private GroupChat currentGroup;
    private MessageCacheManager cacheManager;
    private Gson gson;
    private GroupMessage replyingToMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before setting content view
        ThemeManager.getInstance(this).applyTheme();
        super.onCreate(savedInstanceState);
        
        binding = ActivityGroupChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get intent data
        groupId = getIntent().getStringExtra("group_id");
        groupTitle = getIntent().getStringExtra("group_title");

        if (groupId == null) {
            Toast.makeText(this, "Error: Invalid group", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupFirebase();
        setupUI();
        loadGroupInfo();
        loadCachedMessages();
        loadMessages();
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        cacheManager = new MessageCacheManager(this);
        gson = new Gson();
    }

    private void setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Set initial group title
        if (groupTitle != null) {
            binding.tvGroupTitle.setText(groupTitle);
        }

        // Setup RecyclerView
        messageList = new ArrayList<>();
        adapter = new GroupMessageAdapter(messageList, currentUser.getUid(), this);
        binding.rvMessages.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMessages.setAdapter(adapter);

        // Setup swipe-to-reply
        SwipeToReplyHelper swipeToReplyHelper = new SwipeToReplyHelper(this, this);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeToReplyHelper);
        itemTouchHelper.attachToRecyclerView(binding.rvMessages);

        // Setup click listeners
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        
        binding.fabSend.setOnClickListener(v -> sendMessage());
        
        // Make header clickable to open group info
        View.OnClickListener openGroupInfo = v -> {
            Intent intent = new Intent(this, GroupInfoActivity.class);
            intent.putExtra("group_id", groupId);
            startActivity(intent);
        };

        binding.ivGroupInfo.setOnClickListener(openGroupInfo);
        binding.tvGroupTitle.setOnClickListener(openGroupInfo);
        binding.tvMemberCount.setOnClickListener(openGroupInfo);
    }

    private void loadCachedMessages() {
        // Load cached messages first for instant display
        List<GroupMessage> cachedMessages = cacheManager.getCachedGroupMessages(groupId);
        if (!cachedMessages.isEmpty()) {
            messageList.clear();
            messageList.addAll(cachedMessages);
            adapter.notifyDataSetChanged();

            // Scroll to bottom
            if (!messageList.isEmpty()) {
                binding.rvMessages.scrollToPosition(messageList.size() - 1);
            }

            Log.d("GroupChat", "Loaded " + cachedMessages.size() + " cached messages");
        }
    }

    private void loadGroupInfo() {
        // Use real-time listener to keep group data updated
        db.collection("group_chats")
                .document(groupId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    // Check if activity is still alive
                    if (binding == null || isFinishing() || isDestroyed()) {
                        Log.d("GroupChat", "Activity destroyed, ignoring group info update");
                        return;
                    }

                    if (error != null) {
                        Log.e("GroupChat", "Error loading group info", error);
                        Toast.makeText(this, "Error loading group info", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        currentGroup = documentSnapshot.toObject(GroupChat.class);
                        if (currentGroup != null) {
                            currentGroup.setGroupId(documentSnapshot.getId());
                            updateGroupUI();

                            // Check if current user is a member, if not, show warning but don't block
                            if (!currentGroup.isMember(currentUser.getUid())) {
                                Log.w("GroupChat", "User " + currentUser.getUid() + " is not in members list: " + currentGroup.getMembers());
                            }
                        }
                    }
                });
    }

    private void updateGroupUI() {
        if (currentGroup == null || binding == null) return;

        binding.tvGroupTitle.setText(currentGroup.getTitle());

        int memberCount = currentGroup.getMembers() != null ? currentGroup.getMembers().size() : 0;
        binding.tvMemberCount.setText(memberCount + " member" + (memberCount == 1 ? "" : "s"));
    }

    private void loadMessages() {
        // Load messages for this group with real-time updates
        // Removed orderBy to avoid index requirement - we'll sort in memory
        db.collection("group_messages")
                .whereEqualTo("groupId", groupId)
                .addSnapshotListener((value, error) -> {
                    // Check if activity is still alive
                    if (binding == null || isFinishing() || isDestroyed()) {
                        Log.d("GroupChat", "Activity destroyed, ignoring message update");
                        return;
                    }

                    if (error != null) {
                        Log.e("GroupChat", "Error loading messages", error);
                        Toast.makeText(this, "Error loading messages", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    messageList.clear();

                    if (value != null) {
                        Log.d("GroupChat", "Loaded " + value.size() + " messages for group: " + groupId);
                        for (QueryDocumentSnapshot document : value) {
                            GroupMessage message = document.toObject(GroupMessage.class);
                            message.setMessageId(document.getId());
                            messageList.add(message);
                            Log.d("GroupChat", "Message: " + message.getContent() + " from " + message.getSenderName());
                        }

                        // Sort messages by timestamp manually (since we removed orderBy)
                        messageList.sort((m1, m2) -> {
                            if (m1.getTimestamp() != null && m2.getTimestamp() != null) {
                                return m1.getTimestamp().compareTo(m2.getTimestamp());
                            }
                            return 0;
                        });
                    } else {
                        Log.d("GroupChat", "No messages found for group: " + groupId);
                    }

                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }

                    // Cache the messages
                    if (cacheManager != null) {
                        cacheManager.cacheGroupMessages(groupId, messageList);
                    }

                    // Clear unread count for this group
                    clearUnreadCount();

                    // Scroll to bottom - check binding is still valid
                    if (!messageList.isEmpty() && binding != null && binding.rvMessages != null) {
                        binding.rvMessages.scrollToPosition(messageList.size() - 1);
                    }
                });
    }

    private void sendMessage() {
        String messageText = binding.etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        if (currentUser == null || currentGroup == null) {
            Toast.makeText(this, "Error: Unable to send message", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user is still a member (but allow sending if group data is still loading)
        if (currentGroup != null && currentGroup.getMembers() != null &&
            !currentGroup.getMembers().isEmpty() && !currentGroup.isMember(currentUser.getUid())) {
            Toast.makeText(this, "You are no longer a member of this group", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create message
        String messageId = db.collection("group_messages").document().getId();

        GroupMessage message = new GroupMessage(
                currentUser.getUid(),
                currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Unknown User",
                groupId,
                messageText
        );
        message.setMessageId(messageId);
        message.setSenderPhotoUrl(currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null);

        // Clear input
        binding.etMessage.setText("");

        // Send message
        db.collection("group_messages")
                .document(messageId)
                .set(message)
                .addOnSuccessListener(aVoid -> {
                    // Update group's last message
                    updateGroupLastMessage(messageText);
                })
                .addOnFailureListener(e -> {
                    Log.e("GroupChat", "Error sending message", e);
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateGroupLastMessage(String messageText) {
        // First get the current group data to access member list
        db.collection("group_chats")
                .document(groupId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> groupData = documentSnapshot.getData();
                        if (groupData != null && groupData.containsKey("members")) {
                            @SuppressWarnings("unchecked")
                            List<String> members = (List<String>) groupData.get("members");

                            Log.d("GroupChat", "Group members: " + members);
                            Log.d("GroupChat", "Current user: " + currentUser.getUid());

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("lastMessage", messageText);
                            updates.put("lastMessageSenderId", currentUser.getUid());
                            updates.put("lastMessageTime", new Date());
                            updates.put("updatedAt", new Date());

                            // Increment unread count for all members except the sender
                            if (members != null) {
                                for (String memberId : members) {
                                    if (!memberId.equals(currentUser.getUid())) {
                                        updates.put("unreadCounts." + memberId, com.google.firebase.firestore.FieldValue.increment(1));
                                        Log.d("GroupChat", "Incrementing unread count for member: " + memberId);
                                    }
                                }
                            }

                            // Update the group document
                            db.collection("group_chats")
                                    .document(groupId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("GroupChat", "Updated group last message and unread counts for " +
                                               (members != null ? members.size() - 1 : 0) + " members");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("GroupChat", "Error updating group last message", e);
                                    });
                        } else {
                            Log.e("GroupChat", "Group data is null or missing members field");
                        }
                    } else {
                        Log.e("GroupChat", "Group document does not exist");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("GroupChat", "Error getting group data for unread count update", e);
                });
    }

    private void clearUnreadCount() {
        // Clear unread count in Firestore
        db.collection("group_chats")
                .document(groupId)
                .update("unreadCounts." + currentUser.getUid(), 0)
                .addOnSuccessListener(aVoid -> {
                    Log.d("GroupChat", "Cleared unread count for group: " + groupId);
                    // Also clear in local cache
                    if (cacheManager != null) {
                        cacheManager.clearUnreadCount(groupId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("GroupChat", "Error clearing unread count", e);
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_group_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_group_info) {
            Intent intent = new Intent(this, GroupInfoActivity.class);
            intent.putExtra("group_id", groupId);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_clear_chat) {
            showClearChatDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showClearChatDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Chat")
                .setMessage("This will delete all messages in this group for you. This action cannot be undone.")
                .setPositiveButton("Clear", (dialog, which) -> clearChat())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearChat() {
        // TODO: Implement clear chat functionality
        Toast.makeText(this, "Clear chat functionality coming soon!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh group info when activity resumes to get latest member list
        if (currentGroup != null) {
            Log.d("GroupChat", "Activity resumed, refreshing group info");
            loadGroupInfo();
        }
    }

    // GroupMessageAdapter.OnMessageActionListener implementation
    @Override
    public void onReplyToMessage(GroupMessage message) {
        replyingToMessage = message;
        String replyText = "@" + message.getSenderName() + " ";
        binding.etMessage.setText(replyText);
        binding.etMessage.setSelection(replyText.length());
        binding.etMessage.requestFocus();

        // Show reply indicator
        showReplyIndicator(message);

        Toast.makeText(this, "Replying to " + message.getSenderName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProfileClick(String userId, String userName) {
        // TODO: Open user profile when ProfileActivity is created
        Toast.makeText(this, "Profile for " + userName + " - Coming soon!", Toast.LENGTH_SHORT).show();
    }

    // SwipeToReplyHelper.SwipeToReplyListener implementation
    @Override
    public void onSwipeToReply(int position) {
        if (position >= 0 && position < messageList.size()) {
            onReplyToMessage(messageList.get(position));
        }
    }

    private void showReplyIndicator(GroupMessage message) {
        // TODO: Add a reply indicator UI above the message input
        // For now, we'll just show it in the message text
        String replyPreview = "Replying to: " + message.getContent();
        if (replyPreview.length() > 50) {
            replyPreview = replyPreview.substring(0, 47) + "...";
        }
        // You can add a TextView above the EditText to show this
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
