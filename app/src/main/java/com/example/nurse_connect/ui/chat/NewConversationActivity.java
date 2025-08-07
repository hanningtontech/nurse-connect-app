package com.example.nurse_connect.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nurse_connect.adapters.UserSearchAdapter;
import com.example.nurse_connect.databinding.ActivityNewConversationBinding;
import com.example.nurse_connect.models.User;
import com.example.nurse_connect.utils.ThemeManager;
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

public class NewConversationActivity extends AppCompatActivity implements UserSearchAdapter.OnUserClickListener {

    private ActivityNewConversationBinding binding;
    private UserSearchAdapter adapter;
    private List<User> userList;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before setting content view
        ThemeManager.getInstance(this).applyTheme();
        super.onCreate(savedInstanceState);
        
        binding = ActivityNewConversationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupFirebase();
        setupUI();
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    private void setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Setup RecyclerView
        userList = new ArrayList<>();
        adapter = new UserSearchAdapter(userList, this);
        binding.rvUsers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvUsers.setAdapter(adapter);

        // Setup search functionality
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() >= 2) {
                    searchUsers(query);
                } else {
                    clearResults();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup toolbar navigation
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void searchUsers(String query) {
        if (currentUser == null) return;

        showLoading();

        // Search users by username (case-insensitive)
        db.collection("users")
                .whereGreaterThanOrEqualTo("username", query.toLowerCase())
                .whereLessThanOrEqualTo("username", query.toLowerCase() + "\uf8ff")
                .limit(20)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    hideLoading();
                    List<User> searchResults = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        user.setUid(document.getId());
                        
                        // Don't include current user in results
                        if (!user.getUid().equals(currentUser.getUid())) {
                            searchResults.add(user);
                        }
                    }

                    if (searchResults.isEmpty()) {
                        showEmptyResults();
                    } else {
                        showResults(searchResults);
                    }
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    Log.e("NewConversation", "Error searching users", e);
                    Toast.makeText(this, "Error searching users", Toast.LENGTH_SHORT).show();
                });
    }

    private void clearResults() {
        userList.clear();
        adapter.notifyDataSetChanged();
        showEmptyState();
    }

    private void showResults(List<User> results) {
        binding.emptyState.setVisibility(View.GONE);
        binding.rvUsers.setVisibility(View.VISIBLE);
        adapter.updateUsers(results);
    }

    private void showEmptyResults() {
        binding.rvUsers.setVisibility(View.GONE);
        binding.emptyState.setVisibility(View.VISIBLE);
        // Update empty state text for no results
    }

    private void showEmptyState() {
        binding.rvUsers.setVisibility(View.GONE);
        binding.emptyState.setVisibility(View.VISIBLE);
    }

    private void showLoading() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.emptyState.setVisibility(View.GONE);
    }

    private void hideLoading() {
        binding.progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onUserClick(User user) {
        // Same as message click for now
        onMessageClick(user);
    }

    @Override
    public void onMessageClick(User user) {
        // Create or find existing private chat
        createOrFindPrivateChat(user);
    }

    private void createOrFindPrivateChat(User otherUser) {
        if (currentUser == null) return;

        showLoading();

        // Check if chat already exists
        db.collection("private_chats")
                .whereArrayContains("participants", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    String existingChatId = null;
                    
                    // Look for existing chat with this user
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        List<String> participants = (List<String>) document.get("participants");
                        if (participants != null && participants.contains(otherUser.getUid())) {
                            existingChatId = document.getId();
                            break;
                        }
                    }

                    if (existingChatId != null) {
                        // Open existing chat
                        openPrivateChat(otherUser, existingChatId);
                    } else {
                        // Create new chat
                        createNewPrivateChat(otherUser);
                    }
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    Log.e("NewConversation", "Error checking existing chats", e);
                    Toast.makeText(this, "Error creating conversation", Toast.LENGTH_SHORT).show();
                });
    }

    private void createNewPrivateChat(User otherUser) {
        String chatId = generateChatId(currentUser.getUid(), otherUser.getUid());
        
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("chatId", chatId);
        chatData.put("participants", List.of(currentUser.getUid(), otherUser.getUid()));
        chatData.put("createdAt", new Date());
        chatData.put("updatedAt", new Date());
        chatData.put("lastMessage", "");
        chatData.put("lastMessageSenderId", "");
        chatData.put("lastMessageTime", new Date());
        
        // Initialize unread counts
        Map<String, Object> unreadCounts = new HashMap<>();
        unreadCounts.put(currentUser.getUid(), 0);
        unreadCounts.put(otherUser.getUid(), 0);
        chatData.put("unreadCounts", unreadCounts);

        db.collection("private_chats")
                .document(chatId)
                .set(chatData)
                .addOnSuccessListener(aVoid -> {
                    hideLoading();
                    openPrivateChat(otherUser, chatId);
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    Log.e("NewConversation", "Error creating chat", e);
                    Toast.makeText(this, "Error creating conversation", Toast.LENGTH_SHORT).show();
                });
    }

    private void openPrivateChat(User otherUser, String chatId) {
        Intent intent = new Intent(this, PrivateChatActivity.class);
        intent.putExtra("other_user_id", otherUser.getUid());
        intent.putExtra("other_user_name", otherUser.getDisplayName());
        intent.putExtra("other_user_photo", otherUser.getPhotoURL());
        intent.putExtra("chat_id", chatId);
        startActivity(intent);
        finish(); // Close this activity
    }

    private String generateChatId(String userId1, String userId2) {
        // Create consistent chat ID regardless of user order
        return userId1.compareTo(userId2) < 0 ? 
                userId1 + "_" + userId2 : 
                userId2 + "_" + userId1;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
