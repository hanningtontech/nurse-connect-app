package com.example.nurse_connect.ui.flashcards;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.ActivityFlashcardHistoryBinding;
import com.example.nurse_connect.adapters.FlashcardSessionAdapter;
import com.example.nurse_connect.models.FlashcardSession;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Flashcard History Activity
 * Shows all saved flashcard sessions
 */
public class FlashcardHistoryActivity extends AppCompatActivity {
    
    private ActivityFlashcardHistoryBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FlashcardSessionAdapter adapter;
    private List<FlashcardSession> sessions;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFlashcardHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        // Initialize UI
        setupUI();
        
        // Load sessions
        loadFlashcardSessions();
    }
    
    private void setupUI() {
        // Back button
        binding.btnBack.setOnClickListener(v -> finish());
        
        // Initialize RecyclerView
        sessions = new ArrayList<>();
        adapter = new FlashcardSessionAdapter(sessions, this::onSessionClicked);
        
        binding.recyclerViewSessions.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewSessions.setAdapter(adapter);
        
        // Refresh button
        binding.btnRefresh.setOnClickListener(v -> loadFlashcardSessions());
    }
    
    private void loadFlashcardSessions() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Toast.makeText(this, "Please sign in to view history", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textNoSessions.setVisibility(View.GONE);
        
        db.collection("users").document(currentUserId)
            .collection("flashcard_sessions")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                binding.progressBar.setVisibility(View.GONE);
                
                sessions.clear();
                
                if (queryDocumentSnapshots.isEmpty()) {
                    binding.textNoSessions.setVisibility(View.VISIBLE);
                    binding.recyclerViewSessions.setVisibility(View.GONE);
                } else {
                    binding.textNoSessions.setVisibility(View.GONE);
                    binding.recyclerViewSessions.setVisibility(View.VISIBLE);
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            FlashcardSession session = document.toObject(FlashcardSession.class);
                            if (session != null) {
                                session.setSessionId(document.getId());
                                sessions.add(session);
                            }
                        } catch (Exception e) {
                            Log.e("FlashcardHistory", "Error parsing session document", e);
                        }
                    }
                    
                    adapter.notifyDataSetChanged();
                    Log.d("FlashcardHistory", "Loaded " + sessions.size() + " sessions");
                }
            })
            .addOnFailureListener(e -> {
                binding.progressBar.setVisibility(View.GONE);
                Log.e("FlashcardHistory", "Error loading sessions", e);
                Toast.makeText(this, "Failed to load history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void onSessionClicked(FlashcardSession session) {
        // Open session details
        Intent intent = new Intent(this, FlashcardSessionDetailActivity.class);
        intent.putExtra("session_id", session.getSessionId());
        startActivity(intent);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        loadFlashcardSessions();
    }
}
