package com.example.nurse_connect.ui.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nurse_connect.R;
import com.example.nurse_connect.adapters.FollowedUsersAdapter;
import com.example.nurse_connect.databinding.ActivityQuizWaitingRoomBinding;
import com.example.nurse_connect.models.QuizMatch;
import com.example.nurse_connect.models.User;
import com.example.nurse_connect.services.QuizMatchService;
import com.example.nurse_connect.ui.chat.PrivateChatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class QuizWaitingRoomActivity extends AppCompatActivity implements 
        QuizMatchService.QuizMatchCallback, FollowedUsersAdapter.OnUserClickListener {
    
    private ActivityQuizWaitingRoomBinding binding;
    private QuizMatchService quizService;
    private FirebaseFirestore db;
    private String currentUserId;
    private QuizMatch currentMatch;
    private FollowedUsersAdapter followedUsersAdapter;
    private List<User> followedUsers;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuizWaitingRoomBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize services
        quizService = new QuizMatchService();
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Get match ID from intent
        String matchId = getIntent().getStringExtra("match_id");
        if (matchId == null) {
            Toast.makeText(this, "Invalid match", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        setupUI();
        loadFollowedUsers();
        
        // Listen to match updates
        quizService.listenToMatch(matchId, this);
    }
    
    private void setupUI() {
        // Setup back button
        binding.btnBack.setOnClickListener(v -> finish());
        
        // Setup followed users RecyclerView
        followedUsers = new ArrayList<>();
        followedUsersAdapter = new FollowedUsersAdapter(this, followedUsers, this);
        binding.recyclerViewFollowedUsers.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewFollowedUsers.setAdapter(followedUsersAdapter);
        
        // Setup refresh button
        binding.btnRefresh.setOnClickListener(v -> refreshMatchStatus());
    }
    
    private void loadFollowedUsers() {
        // Load users that the current user follows
        db.collection("users")
                .document(currentUserId)
                .collection("following")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    followedUsers.clear();
                    for (var document : queryDocumentSnapshots) {
                        String followedUserId = document.getId();
                        // Get user details
                        db.collection("users").document(followedUserId).get()
                                .addOnSuccessListener(userDoc -> {
                                    User user = userDoc.toObject(User.class);
                                    if (user != null) {
                                        user.setUid(followedUserId);
                                        followedUsers.add(user);
                                        followedUsersAdapter.notifyDataSetChanged();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading followed users", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void refreshMatchStatus() {
        // Refresh the match status
        quizService.refreshMatchStatus(currentMatch.getMatchId());
    }
    
    @Override
    public void onMatchUpdated(QuizMatch match) {
        runOnUiThread(() -> {
            currentMatch = match;
            updateMatchInfo();
            
            // Check if match is ready to start
            if (match.isReadyToStart()) {
                // Navigate to quiz match
                Intent intent = new Intent(this, QuizMatchActivity.class);
                intent.putExtra("match_id", match.getMatchId());
                startActivity(intent);
                finish();
            }
        });
    }
    
    private void updateMatchInfo() {
        if (currentMatch != null) {
            binding.textMatchInfo.setText(String.format("Quiz: %s - %s - %s", 
                    currentMatch.getCareer(), currentMatch.getCourse(), currentMatch.getUnit()));
            binding.textPlayerCount.setText(String.format("Players: %d/%d", 
                    currentMatch.getCurrentPlayerCount(), currentMatch.getMaxPlayers()));
            binding.textStatus.setText("Waiting for players to join...");
        }
    }
    
    @Override
    public void onUserClick(User user) {
        // Send quiz invitation to this user
        sendQuizInvitation(user);
    }
    
    private void sendQuizInvitation(User user) {
        if (currentMatch == null) return;
        
        // Create invitation message
        String invitationMessage = String.format(
            "🎯 Quiz Invitation!\n\n" +
            "You're invited to join a nursing quiz battle:\n" +
            "• Career: %s\n" +
            "• Course: %s\n" +
            "• Unit: %s\n" +
            "• Players: %d/%d\n\n" +
            "Click to join!",
            currentMatch.getCareer(),
            currentMatch.getCourse(),
            currentMatch.getUnit(),
            currentMatch.getCurrentPlayerCount(),
            currentMatch.getMaxPlayers()
        );
        
        // Navigate to private chat with invitation
        Intent intent = new Intent(this, PrivateChatActivity.class);
        intent.putExtra("other_user_id", user.getUid());
        intent.putExtra("other_user_name", user.getDisplayName());
        intent.putExtra("other_user_photo", user.getPhotoURL());
        intent.putExtra("quiz_invitation", true);
        intent.putExtra("match_id", currentMatch.getMatchId());
        intent.putExtra("invitation_message", invitationMessage);
        startActivity(intent);
    }
    
    @Override
    public void onMatchCompleted(QuizMatch match) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Quiz completed!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
    
    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (quizService != null) {
            quizService.cleanup();
        }
    }
}
