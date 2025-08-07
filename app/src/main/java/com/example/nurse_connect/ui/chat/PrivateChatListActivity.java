package com.example.nurse_connect.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.nurse_connect.R;
import com.example.nurse_connect.adapters.PrivateChatAdapter;
import com.example.nurse_connect.databinding.ActivityPrivateChatListBinding;
import com.example.nurse_connect.models.PrivateChat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PrivateChatListActivity extends AppCompatActivity implements PrivateChatAdapter.OnChatClickListener {

    private ActivityPrivateChatListBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private PrivateChatAdapter chatAdapter;
    private List<PrivateChat> chatList;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPrivateChatListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Please sign in to view chats", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        setupRecyclerView();
        loadPrivateChats();
    }

    private void setupUI() {
        // Setup toolbar
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbarTitle.setText("Private Messages");

        // Setup empty state
        binding.emptyState.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        chatList = new ArrayList<>();
        chatAdapter = new PrivateChatAdapter(chatList, this);
        
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(chatAdapter);
    }

    private void loadPrivateChats() {
        db.collection("private_chats")
                .whereArrayContains("participants", currentUser.getUid())
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<PrivateChat> chats = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        PrivateChat chat = document.toObject(PrivateChat.class);
                        if (chat != null) {
                            chat.setChatId(document.getId());
                            chats.add(chat);
                        }
                    }
                    
                    if (chats.isEmpty()) {
                        showEmptyState();
                    } else {
                        showChatList(chats);
                    }
                })
                .addOnFailureListener(e -> {
                    showEmptyState();
                });
    }

    private void checkForNewNotifications() {
        db.collection("notifications")
                .whereEqualTo("recipientId", currentUser.getUid())
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Refresh the chat list to show new messages
                        loadPrivateChats();
                    }
                });
    }

    private void showChatList(List<PrivateChat> chats) {
        chatList.clear();
        chatList.addAll(chats);
        chatAdapter.notifyDataSetChanged();
        
        binding.emptyState.setVisibility(View.GONE);
        binding.recyclerView.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        chatList.clear();
        chatAdapter.notifyDataSetChanged();
        
        binding.emptyState.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
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
                            Intent intent = new Intent(this, PrivateChatActivity.class);
                            intent.putExtra("other_user_id", finalOtherUserId);
                            intent.putExtra("other_user_name", otherUserName);
                            intent.putExtra("other_user_photo", otherUserPhoto);
                            startActivity(intent);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to load user info", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 