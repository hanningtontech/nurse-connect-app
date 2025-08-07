package com.example.nurse_connect.ui.comments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nurse_connect.R;
import com.example.nurse_connect.adapters.CommentAdapter;
import com.example.nurse_connect.databinding.ActivityCommentsBinding;
import com.example.nurse_connect.models.Comment;
import com.example.nurse_connect.services.CommunityProfileService;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentsActivity extends AppCompatActivity {

    private ActivityCommentsBinding binding;
    private CommentAdapter commentAdapter;
    private List<Comment> commentsList;
    private FirebaseFirestore db;
    private CommunityProfileService profileService;
    private String postId;
    private String replyingToCommentId = null;
    private String replyingToUsername = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCommentsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get post ID from intent
        postId = getIntent().getStringExtra("post_id");
        if (postId == null) {
            Toast.makeText(this, "Post ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        profileService = new CommunityProfileService();

        // Initialize comments list
        commentsList = new ArrayList<>();
        commentAdapter = new CommentAdapter(commentsList, this::onReplyClicked, this::onLikeCommentClicked);

        // Setup RecyclerView
        binding.commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.commentsRecyclerView.setAdapter(commentAdapter);

        // Setup UI
        setupUI();
        
        // Load comments
        loadComments();
    }

    private void setupUI() {
        // Back button
        binding.btnBack.setOnClickListener(v -> finish());

        // Comment input
        binding.commentInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.btnSendComment.setEnabled(s.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Send comment button
        binding.btnSendComment.setOnClickListener(v -> sendComment());

        // Cancel reply button
        binding.btnCancelReply.setOnClickListener(v -> cancelReply());
    }

    private void loadComments() {
        db.collection("posts").document(postId)
                .collection("comments")
                .orderBy("createdAt")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    commentsList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Comment comment = document.toObject(Comment.class);
                        if (comment != null) {
                            comment.setId(document.getId());
                            commentsList.add(comment);
                        }
                    }
                    commentAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load comments: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendComment() {
        String commentText = binding.commentInput.getText().toString().trim();
        if (commentText.isEmpty()) return;

        String currentUserId = profileService.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "Please sign in to comment", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create comment data
        Map<String, Object> commentData = new HashMap<>();
        commentData.put("userId", currentUserId);
        commentData.put("text", commentText);
        commentData.put("createdAt", Long.valueOf(System.currentTimeMillis()));
        commentData.put("likes", 0);
        commentData.put("replyCount", 0);

        // If replying to a comment, add parent comment info
        if (replyingToCommentId != null) {
            commentData.put("parentCommentId", replyingToCommentId);
            commentData.put("replyingToUsername", replyingToUsername);
        }

        // Add comment to Firebase
        db.collection("posts").document(postId)
                .collection("comments")
                .add(commentData)
                .addOnSuccessListener(documentReference -> {
                    // Update comment count in main post
                    db.collection("posts").document(postId)
                            .update("comments", com.google.firebase.firestore.FieldValue.increment(1));

                    // Clear input and reset reply state
                    binding.commentInput.setText("");
                    cancelReply();

                    // Reload comments
                    loadComments();

                    Toast.makeText(this, "Comment added successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to add comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void onReplyClicked(Comment comment) {
        replyingToCommentId = comment.getId();
        replyingToUsername = comment.getUsername();
        
        // Show reply section
        binding.replyToSection.setVisibility(View.VISIBLE);
        TextView replyText = binding.replyToSection.findViewById(R.id.reply_to_text);
        replyText.setText("Replying to @" + replyingToUsername);
        
        // Focus on input
        binding.commentInput.requestFocus();
    }

    private void onLikeCommentClicked(Comment comment) {
        String currentUserId = profileService.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "Please sign in to like comments", Toast.LENGTH_SHORT).show();
            return;
        }

        // Toggle like
        db.collection("posts").document(postId)
                .collection("comments").document(comment.getId())
                .collection("likes").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Unlike
                        unlikeComment(comment.getId());
                    } else {
                        // Like
                        likeComment(comment.getId());
                    }
                });
    }

    private void likeComment(String commentId) {
        String currentUserId = profileService.getCurrentUserId();
        
        db.collection("posts").document(postId)
                .collection("comments").document(commentId)
                .collection("likes").document(currentUserId)
                .set(new HashMap<>())
                .addOnSuccessListener(aVoid -> {
                    // Update like count
                    db.collection("posts").document(postId)
                            .collection("comments").document(commentId)
                            .update("likes", com.google.firebase.firestore.FieldValue.increment(1));
                    
                    // Reload comments to update UI
                    loadComments();
                });
    }

    private void unlikeComment(String commentId) {
        String currentUserId = profileService.getCurrentUserId();
        
        db.collection("posts").document(postId)
                .collection("comments").document(commentId)
                .collection("likes").document(currentUserId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Update like count
                    db.collection("posts").document(postId)
                            .collection("comments").document(commentId)
                            .update("likes", com.google.firebase.firestore.FieldValue.increment(-1));
                    
                    // Reload comments to update UI
                    loadComments();
                });
    }

    private void cancelReply() {
        replyingToCommentId = null;
        replyingToUsername = null;
        binding.replyToSection.setVisibility(View.GONE);
        binding.commentInput.clearFocus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 