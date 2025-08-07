package com.example.nurse_connect.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.ActivityGlobalNurseHubBinding;
import com.example.nurse_connect.services.CommunityProfileService;
import com.example.nurse_connect.ui.comments.CommentsActivity;
import com.example.nurse_connect.utils.OnSwipeTouchListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.bumptech.glide.Glide;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.HashMap;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.view.View;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class GlobalNurseHubActivity extends AppCompatActivity {

    private ActivityGlobalNurseHubBinding binding;
    private CommunityProfileService profileService;
    private FirebaseFirestore db;
    private boolean hasCommunityProfile = false;

    private final ActivityResultLauncher<Intent> createPostLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Post was created successfully, refresh the feed
                    Toast.makeText(this, "Post created successfully!", Toast.LENGTH_SHORT).show();
                    // Refresh posts from database
                    loadPostsFromFirebase();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGlobalNurseHubBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase services
        profileService = new CommunityProfileService();
        db = FirebaseFirestore.getInstance();

        setupUI();
        loadRecommendationData();
        loadPostsFromFirebase();
        checkCommunityProfileStatus();
    }

    private void setupUI() {
        // Back button
        binding.btnBack.setOnClickListener(v -> finish());

        // Profile button
        binding.btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, CommunityProfileViewActivity.class);
            startActivity(intent);
        });

        // Load current user's profile picture in header
        loadCurrentUserProfilePicture();

        // Create post button (only visible for users with community profile)
        binding.btnCreatePost.setOnClickListener(v -> {
            if (hasCommunityProfile) {
                openCreatePostActivity();
            } else {
                Toast.makeText(this, "Please create a community profile first", Toast.LENGTH_SHORT).show();
            }
        });

        // Create community profile button
        binding.btnCreateCommunityProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, CommunityProfileActivity.class);
            startActivity(intent);
        });

        // Set up click listeners for recommendation items
        setupRecommendationClickListeners();
        
        // Set up click listeners for public post items
        setupPublicPostClickListeners();
    }

    private void setupRecommendationClickListeners() {
        // This method is no longer needed as we're using dynamic user recommendation cards
    }

    private void setupPublicPostClickListeners() {
        // This is now handled in setupPostInteractions for each individual post
        // No need to set up click listeners here anymore
    }

    private void loadRecommendationData() {
        // Load recommended users from Firestore
        loadRecommendedUsers();
    }
    
    private void loadRecommendedUsers() {
        // Get users who have community profiles and are not the current user
        String currentUserId = profileService.getCurrentUserId();
        
        if (currentUserId == null) {
            loadBasicRecommendations(null);
            return;
        }
        
        // First, get all posts to calculate user engagement scores
        db.collection("posts")
                .get()
                .addOnSuccessListener(postsSnapshot -> {
                    // Calculate engagement scores for each user
                    Map<String, Integer> userEngagementScores = calculateUserEngagementScores(postsSnapshot);
                    
                    // Get user's follow data and post likes
                    getUserInteractionData(currentUserId, userEngagementScores, postsSnapshot);
                })
                .addOnFailureListener(e -> {
                    // If posts loading fails, show basic recommendations
                    loadBasicRecommendations(currentUserId);
                });
    }
    
    private Map<String, Integer> calculateUserEngagementScores(QuerySnapshot postsSnapshot) {
        Map<String, Integer> userEngagementScores = new HashMap<>();
        
        for (QueryDocumentSnapshot post : postsSnapshot) {
            String postUserId = post.getString("userId");
            if (postUserId != null) {
                // Get like count
                Long likesCount = post.getLong("likes");
                int likes = likesCount != null ? likesCount.intValue() : 0;
                
                // Get comment count
                Long commentsCount = post.getLong("comments");
                int comments = commentsCount != null ? commentsCount.intValue() : 0;
                
                // Calculate engagement score (likes + comments)
                int engagementScore = likes + comments;
                
                // Add to user's total score
                int currentScore = userEngagementScores.getOrDefault(postUserId, 0);
                userEngagementScores.put(postUserId, currentScore + engagementScore);
            }
        }
        
        return userEngagementScores;
    }
    
    private void getUserInteractionData(String currentUserId, Map<String, Integer> userEngagementScores, QuerySnapshot postsSnapshot) {
        // Get user's follows and post likes to boost recommendations
        db.collection("user_follows")
                .whereEqualTo("followerId", currentUserId)
                .get()
                .addOnSuccessListener(followsSnapshot -> {
                    Set<String> followedUsers = new HashSet<>();
                    for (QueryDocumentSnapshot follow : followsSnapshot) {
                        String followedUserId = follow.getString("followedId");
                        if (followedUserId != null) {
                            followedUsers.add(followedUserId);
                        }
                    }
                    
                    // Get user's post likes
                    getUserPostLikes(currentUserId, userEngagementScores, followedUsers, postsSnapshot);
                })
                .addOnFailureListener(e -> {
                    // If follows loading fails, continue without follow data
                    getUserPostLikes(currentUserId, userEngagementScores, new HashSet<>(), postsSnapshot);
                });
    }
    
    private void getUserPostLikes(String currentUserId, Map<String, Integer> userEngagementScores, Set<String> followedUsers, QuerySnapshot postsSnapshot) {
        // Count how many posts from each user the current user has liked
        Map<String, Integer> userLikedPostCounts = new HashMap<>();
        
        for (QueryDocumentSnapshot post : postsSnapshot) {
            String postId = post.getId();
            String postUserId = post.getString("userId");
            
            if (postUserId != null && !postUserId.equals(currentUserId)) {
                // Check if current user has liked this post
                db.collection("posts").document(postId).collection("likes").document(currentUserId)
                        .get()
                        .addOnSuccessListener(likeDoc -> {
                            if (likeDoc.exists()) {
                                int currentCount = userLikedPostCounts.getOrDefault(postUserId, 0);
                                userLikedPostCounts.put(postUserId, currentCount + 1);
                            }
                        });
            }
        }
        
        // Get community profiles and apply interaction boosts
        db.collection("community_profiles")
                .get()
                .addOnSuccessListener(profilesSnapshot -> {
                    // Create list of users with their engagement scores and interaction boosts
                    List<UserRecommendation> userRecommendations = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : profilesSnapshot) {
                        String userId = document.getId();
                        
                        // Skip current user
                        if (userId.equals(currentUserId)) {
                            continue;
                        }
                        
                        // Get base engagement score
                        int baseEngagementScore = userEngagementScores.getOrDefault(userId, 0);
                        
                        // Apply interaction boosts
                        int interactionBoost = 0;
                        
                        // Boost for followed users (high priority)
                        if (followedUsers.contains(userId)) {
                            interactionBoost += 1000; // High boost for followed users
                        }
                        
                        // Boost for users whose posts you've liked multiple times
                        int likedPostCount = userLikedPostCounts.getOrDefault(userId, 0);
                        if (likedPostCount >= 2) {
                            interactionBoost += 500; // High boost for users you've liked multiple posts from
                        } else if (likedPostCount == 1) {
                            interactionBoost += 200; // Medium boost for users you've liked one post from
                        }
                        
                        // Calculate final score
                        int finalScore = baseEngagementScore + interactionBoost;
                        
                        // Create user recommendation object
                        UserRecommendation recommendation = new UserRecommendation(userId, document, finalScore, followedUsers.contains(userId), likedPostCount);
                        userRecommendations.add(recommendation);
                    }
                    
                    // Sort by final score (highest first)
                    Collections.sort(userRecommendations, (r1, r2) -> Integer.compare(r2.engagementScore, r1.engagementScore));
                    
                    // Display top 10 recommended users
                    displayRecommendedUsers(userRecommendations.subList(0, Math.min(10, userRecommendations.size())));
                })
                .addOnFailureListener(e -> {
                    // Hide recommendations section on error
                    binding.recommendationsTitle.setVisibility(View.GONE);
                    binding.recommendationsContainer.setVisibility(View.GONE);
                });
    }
    
    private void displayRecommendedUsers(List<UserRecommendation> recommendations) {
        LinearLayout container = binding.recommendationsContainer;
        container.removeAllViews(); // Clear existing views
        
        for (UserRecommendation recommendation : recommendations) {
            // Create user recommendation card with interaction data
            View userCard = createUserRecommendationCard(recommendation.document, recommendation.engagementScore, recommendation.isFollowed, recommendation.likedPostCount);
            if (userCard != null) {
                container.addView(userCard);
            }
        }
        
        // Hide section if no recommendations
        if (container.getChildCount() == 0) {
            binding.recommendationsTitle.setVisibility(View.GONE);
            container.setVisibility(View.GONE);
        }
    }
    
    private void loadBasicRecommendations(String currentUserId) {
        // Fallback to basic recommendations if engagement calculation fails
        db.collection("community_profiles")
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    LinearLayout container = binding.recommendationsContainer;
                    container.removeAllViews();
                    
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String userId = document.getId();
                        
                        // Skip current user
                        if (currentUserId != null && userId.equals(currentUserId)) {
                            continue;
                        }
                        
                        // Create user recommendation card
                        View userCard = createUserRecommendationCard(document);
                        if (userCard != null) {
                            container.addView(userCard);
                        }
                    }
                    
                    // Hide section if no recommendations
                    if (container.getChildCount() == 0) {
                        binding.recommendationsTitle.setVisibility(View.GONE);
                        container.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    // Hide recommendations section on error
                    binding.recommendationsTitle.setVisibility(View.GONE);
                    binding.recommendationsContainer.setVisibility(View.GONE);
                });
    }
    
    // Helper class to store user recommendation data with engagement score
    private static class UserRecommendation {
        String userId;
        QueryDocumentSnapshot document;
        int engagementScore;
        boolean isFollowed;
        int likedPostCount;
        
        UserRecommendation(String userId, QueryDocumentSnapshot document, int engagementScore) {
            this.userId = userId;
            this.document = document;
            this.engagementScore = engagementScore;
            this.isFollowed = false;
            this.likedPostCount = 0;
        }
        
        UserRecommendation(String userId, QueryDocumentSnapshot document, int engagementScore, boolean isFollowed, int likedPostCount) {
            this.userId = userId;
            this.document = document;
            this.engagementScore = engagementScore;
            this.isFollowed = isFollowed;
            this.likedPostCount = likedPostCount;
        }
    }
    
    private View createUserRecommendationCard(QueryDocumentSnapshot document) {
        return createUserRecommendationCard(document, 0);
    }
    
    private View createUserRecommendationCard(QueryDocumentSnapshot document, int engagementScore) {
        return createUserRecommendationCard(document, engagementScore, false, 0);
    }
    
    private View createUserRecommendationCard(QueryDocumentSnapshot document, int engagementScore, boolean isFollowed, int likedPostCount) {
        try {
            // Inflate the user recommendation card layout
            View userCard = LayoutInflater.from(this).inflate(R.layout.user_recommendation_card, null);
            
            // Get user data from community profile
            String userId = document.getId();
            String nurseType = document.getString("nurseType");
            String bio = document.getString("bio");
            
            // Get user details from main user profile
            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(userSnapshot -> {
                        if (userSnapshot.exists()) {
                            String username = userSnapshot.getString("username");
                            String photoURL = userSnapshot.getString("photoURL");
                            
                            // Set username
                            TextView usernameText = userCard.findViewById(R.id.user_username);
                            if (usernameText != null && username != null) {
                                usernameText.setText(username);
                            }
                            
                            // Set profile picture
                            ImageView profileImage = userCard.findViewById(R.id.user_profile_image);
                            if (profileImage != null) {
                                if (photoURL != null && !photoURL.isEmpty()) {
                                    Glide.with(this)
                                            .load(photoURL)
                                            .placeholder(R.drawable.ic_profile_placeholder)
                                            .error(R.drawable.ic_profile_placeholder)
                                            .circleCrop()
                                            .into(profileImage);
                                } else {
                                    profileImage.setImageResource(R.drawable.ic_profile_placeholder);
                                }
                            }
                            
                            // Set course/specialization
                            TextView courseText = userCard.findViewById(R.id.user_course);
                            if (courseText != null) {
                                String courseInfo = getCourseInfo(document, userSnapshot);
                                courseText.setText(courseInfo);
                            }
                            
                            // Set nurse type badge
                            TextView nurseTypeText = userCard.findViewById(R.id.user_nurse_type);
                            if (nurseTypeText != null && nurseType != null) {
                                nurseTypeText.setText(nurseType);
                            }
                            
                            // Show interaction indicators
                            View engagementIndicator = userCard.findViewById(R.id.engagement_indicator);
                            TextView engagementText = userCard.findViewById(R.id.engagement_text);
                            if (engagementIndicator != null && engagementText != null) {
                                if (isFollowed) {
                                    // Show "Following" indicator for followed users
                                    engagementIndicator.setVisibility(View.VISIBLE);
                                    engagementText.setText("Following");
                                    engagementText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                                } else if (likedPostCount >= 2) {
                                    // Show "Liked Posts" indicator for users you've liked multiple posts from
                                    engagementIndicator.setVisibility(View.VISIBLE);
                                    engagementText.setText("Liked " + likedPostCount + " posts");
                                    engagementText.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                                } else if (engagementScore >= 10) {
                                    // Show engagement indicator for popular users
                                    engagementIndicator.setVisibility(View.VISIBLE);
                                    if (engagementScore >= 50) {
                                        engagementText.setText("Very Popular");
                                    } else if (engagementScore >= 20) {
                                        engagementText.setText("Popular");
                                    } else {
                                        engagementText.setText("Active");
                                    }
                                    engagementText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                                } else {
                                    engagementIndicator.setVisibility(View.GONE);
                                }
                            }
                            
                            // Make card clickable to open user profile
                            userCard.setOnClickListener(v -> openUserProfile(userId));
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle error - card will show with default values
                    });
            
            return userCard;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    private String getCourseInfo(QueryDocumentSnapshot communityDoc, DocumentSnapshot userDoc) {
        String nurseType = communityDoc.getString("nurseType");
        
        if ("Student Nurse".equals(nurseType)) {
            String nursingCourse = communityDoc.getString("nursingCourse");
            String nursingLevel = communityDoc.getString("nursingLevel");
            
            if (nursingCourse != null && !nursingCourse.isEmpty()) {
                return nursingCourse + (nursingLevel != null ? " (" + nursingLevel + ")" : "");
            } else if (nursingLevel != null && !nursingLevel.isEmpty()) {
                return nursingLevel;
            } else {
                return "Student Nurse";
            }
        } else if ("Registered Nurse".equals(nurseType) || "Practicing Nurse".equals(nurseType)) {
            String specialization = communityDoc.getString("specialization");
            String institution = communityDoc.getString("institution");
            
            if (specialization != null && !specialization.isEmpty()) {
                return specialization + (institution != null ? " at " + institution : "");
            } else if (institution != null && !institution.isEmpty()) {
                return institution;
            } else {
                return nurseType;
            }
        } else {
            return "Nurse";
        }
    }

    private void loadPostsFromFirebase() {
        db.collection("posts")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10) // Limit to 10 most recent posts
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    displayPosts(queryDocumentSnapshots);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load posts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void displayPosts(QuerySnapshot queryDocumentSnapshots) {
        LinearLayout publicPostsContainer = findViewById(R.id.public_posts_container);
        if (publicPostsContainer == null) return;

        // Clear existing posts
        publicPostsContainer.removeAllViews();

        if (queryDocumentSnapshots.isEmpty()) {
            // Show a message when no posts are available
            TextView noPostsText = new TextView(this);
            noPostsText.setText("No posts available yet. Be the first to share!");
            noPostsText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            noPostsText.setPadding(32, 32, 32, 32);
            publicPostsContainer.addView(noPostsText);
            return;
        }

        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
            View postView = createPostView(document);
            if (postView != null) {
                publicPostsContainer.addView(postView);
            }
        }
    }

    private View createPostView(QueryDocumentSnapshot document) {
        try {
            // Inflate the post item layout
            View postView = LayoutInflater.from(this).inflate(R.layout.public_post_item, null);
            
            // Get post data
            String content = document.getString("content");
            String hashtags = document.getString("hashtags");
            String postType = document.getString("postType");
            String mediaUrl = document.getString("mediaUrl");
            Long createdAt = document.getLong("createdAt");
            String userId = document.getString("userId");
            String cropAspectRatio = document.getString("cropAspectRatio");
            Double originalAspectRatio = document.getDouble("originalAspectRatio");
            String postId = document.getId();
            
            // Get like and comment counts
            Long likesCount = document.getLong("likes");
            Long commentsCount = document.getLong("comments");
            
            // Set post content with expandable functionality
            TextView captionText = postView.findViewById(R.id.post_caption);
            TextView readMoreButton = postView.findViewById(R.id.read_more_button);
            
            if (captionText != null && content != null) {
                String fullText = content;
                if (hashtags != null && !hashtags.isEmpty()) {
                    fullText += " " + hashtags;
                }
                
                // Set initial text (truncated)
                captionText.setText(fullText);
                
                // Check if text needs "Read more" button
                captionText.post(() -> {
                    if (captionText.getLineCount() > 2) {
                        readMoreButton.setVisibility(View.VISIBLE);
                        
                        // Set click listener for expand/collapse
                        readMoreButton.setOnClickListener(v -> {
                            if (captionText.getMaxLines() == 2) {
                                // Expand
                                captionText.setMaxLines(Integer.MAX_VALUE);
                                readMoreButton.setText("Show less");
                            } else {
                                // Collapse
                                captionText.setMaxLines(2);
                                readMoreButton.setText("Read more");
                            }
                        });
                    } else {
                        readMoreButton.setVisibility(View.GONE);
                    }
                });
            }
            
            // Set post time
            TextView timeText = postView.findViewById(R.id.post_time);
            if (timeText != null && createdAt != null) {
                timeText.setText(formatTime(createdAt));
            }
            
            // Set username (we'll need to fetch from users collection)
            TextView usernameText = postView.findViewById(R.id.post_username);
            ImageView profileImage = postView.findViewById(R.id.post_profile_image); // Assuming this is the profile image in the post item
            if (usernameText != null && userId != null) {
                loadUserProfileData(userId, usernameText, profileImage);
            }
            
            // Set like and comment counts
            TextView likesCountText = postView.findViewById(R.id.likes_count);
            TextView commentsCountText = postView.findViewById(R.id.comments_count);
            
            if (likesCountText != null) {
                likesCountText.setText(likesCount != null ? likesCount + " likes" : "0 likes");
            }
            
            if (commentsCountText != null) {
                commentsCountText.setText(commentsCount != null ? commentsCount + " comments" : "0 comments");
            }
            
            // Handle media if present
            if (mediaUrl != null && !mediaUrl.isEmpty() && (postType.equals("image") || postType.equals("video"))) {
                ImageView postImage = postView.findViewById(R.id.post_image);
                if (postImage != null) {
                    // Set dynamic aspect ratio based on stored crop information
                    postImage.post(() -> {
                        int width = postImage.getWidth();
                        if (width > 0) {
                            int height;
                            if (cropAspectRatio != null && !cropAspectRatio.equals("free")) {
                                if (cropAspectRatio.equals("9:16")) {
                                    // Portrait crop - make it taller
                                    height = (int) (width * 16.0 / 9.0);
                                } else {
                                    // Landscape crop (16:9) - standard height
                                    height = (int) (width * 9.0 / 16.0);
                                }
                            } else {
                                // Free-form cropping or fallback - use original aspect ratio
                                if (originalAspectRatio != null) {
                                    if (originalAspectRatio < 1.0) {
                                        // Portrait image - make it taller
                                        height = (int) (width / originalAspectRatio);
                                    } else {
                                        // Landscape or square - use original ratio
                                        height = (int) (width / originalAspectRatio);
                                    }
                                } else {
                                    // No aspect ratio info - use flexible height
                                    height = (int) (width * 1.2);
                                }
                            }
                            ViewGroup.LayoutParams params = postImage.getLayoutParams();
                            params.height = height;
                            postImage.setLayoutParams(params);
                        }
                    });
                    
                    if (postType.equals("image")) {
                        // Load image using Glide
                        postImage.setVisibility(View.VISIBLE);
                        Glide.with(this)
                                .load(mediaUrl)
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_image_placeholder)
                                .centerCrop()
                                .into(postImage);
                    } else if (postType.equals("video")) {
                        // For videos, show a video thumbnail or play button
                        postImage.setVisibility(View.VISIBLE);
                        postImage.setImageResource(R.drawable.ic_video);
                        // TODO: Add video thumbnail loading logic
                    }
                }
            }
            
            // Set up like and comment functionality
            setupPostInteractions(postView, postId, userId);
            
            // Set up double-tap to like and swipe gestures
            setupPostGestures(postView, postId, userId);
            
            return postView;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private void setupPostInteractions(View postView, String postId, String postUserId) {
        // Like button functionality
        View likeButton = postView.findViewById(R.id.like_button);
        ImageView likeIcon = postView.findViewById(R.id.like_icon);
        TextView likesCountText = postView.findViewById(R.id.likes_count);

        if (likeButton != null) {
            // Check if current user has liked this post
            checkIfUserLikedPost(postId, likeIcon, likesCountText);

            likeButton.setOnClickListener(v -> {
                toggleLike(postId, likeIcon, likesCountText);
            });
        }

        // Comment button functionality
        View commentButton = postView.findViewById(R.id.comment_button);
        if (commentButton != null) {
            commentButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, CommentsActivity.class);
                intent.putExtra("post_id", postId);
                startActivity(intent);
            });
        }
    }
        // Share button functionality - removed as requested
       // View shareButton = postView.findViewById(R.id.share_button);
       // if (shareButton != null) {
          //  shareButton.setVisibility(View.GONE);
  //      }
  //  }
    
    private void checkIfUserLikedPost(String postId, ImageView likeIcon, TextView likesCountText) {
        String currentUserId = profileService.getCurrentUserId();
        if (currentUserId == null) return;
        
        db.collection("posts").document(postId)
                .collection("likes").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // User has liked this post
                        likeIcon.setImageResource(R.drawable.ic_favorite_filled);
                    } else {
                        // User hasn't liked this post
                        likeIcon.setImageResource(R.drawable.ic_favorite_border);
                    }
                })
                .addOnFailureListener(e -> {
                    // On error, assume not liked
                    likeIcon.setImageResource(R.drawable.ic_favorite_border);
                });
    }
    
    private void toggleLike(String postId, ImageView likeIcon, TextView likesCountText) {
        String currentUserId = profileService.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "Please sign in to like posts", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check current like status
        db.collection("posts").document(postId)
                .collection("likes").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Unlike the post
                        unlikePost(postId, currentUserId, likeIcon, likesCountText);
                    } else {
                        // Like the post
                        likePost(postId, currentUserId, likeIcon, likesCountText);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update like", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void likePost(String postId, String userId, ImageView likeIcon, TextView likesCountText) {
        // Add like to subcollection
        db.collection("posts").document(postId)
                .collection("likes").document(userId)
                .set(new HashMap<>())
                .addOnSuccessListener(aVoid -> {
                    // Update like icon
                    likeIcon.setImageResource(R.drawable.ic_favorite_filled);
                    
                    // Update like count
                    updateLikeCount(postId, likesCountText, 1);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to like post", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void unlikePost(String postId, String userId, ImageView likeIcon, TextView likesCountText) {
        // Remove like from subcollection
        db.collection("posts").document(postId)
                .collection("likes").document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Update like icon
                    likeIcon.setImageResource(R.drawable.ic_favorite_border);
                    
                    // Update like count
                    updateLikeCount(postId, likesCountText, -1);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to unlike post", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void updateLikeCount(String postId, TextView likesCountText, int change) {
        // Update the like count in the main post document
        db.collection("posts").document(postId)
                .update("likes", com.google.firebase.firestore.FieldValue.increment(change))
                .addOnSuccessListener(aVoid -> {
                    // Update the display
                    if (likesCountText != null) {
                        String currentText = likesCountText.getText().toString();
                        int currentLikes = 0;
                        if (currentText.contains("likes")) {
                            currentLikes = Integer.parseInt(currentText.split(" ")[0]);
                        }
                        int newLikes = Math.max(0, currentLikes + change);
                        likesCountText.setText(newLikes + " likes");
                    }
                })
                .addOnFailureListener(e -> {
                    // Silently fail - the UI will be updated on next refresh
                });
    }

    private void loadUserProfileData(String userId, TextView usernameText, ImageView profileImage) {
        // First try to load from community profile, then fall back to main user profile
        db.collection("community_profiles")
                .document(userId)
                .get()
                .addOnSuccessListener(communitySnapshot -> {
                    if (communitySnapshot.exists() && communitySnapshot.contains("photoURL")) {
                        String communityPhotoURL = communitySnapshot.getString("photoURL");
                        if (communityPhotoURL != null && !communityPhotoURL.isEmpty()) {
                            // Use community profile picture
                            loadProfilePictureAndUsername(userId, usernameText, profileImage, communityPhotoURL);
                            return;
                        }
                    }
                    
                    // If no community profile picture, try main user profile
                    loadFromMainUserProfile(userId, usernameText, profileImage);
                })
                .addOnFailureListener(e -> {
                    // If community profile check fails, try main user profile
                    loadFromMainUserProfile(userId, usernameText, profileImage);
                });
    }
    
    private void loadFromMainUserProfile(String userId, TextView usernameText, ImageView profileImage) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String photoURL = documentSnapshot.getString("photoURL");
                        
                        loadProfilePictureAndUsername(userId, usernameText, profileImage, photoURL);
                    } else {
                        // Set default values if user not found
                        usernameText.setText("Anonymous Nurse");
                        if (profileImage != null) {
                            profileImage.setImageResource(R.drawable.ic_profile_placeholder);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    usernameText.setText("Anonymous Nurse");
                    if (profileImage != null) {
                        profileImage.setImageResource(R.drawable.ic_profile_placeholder);
                    }
                });
    }
    
    private void loadProfilePictureAndUsername(String userId, TextView usernameText, ImageView profileImage, String photoURL) {
        // Get username from main user profile
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        
                        // Set username
                        if (username != null && !username.isEmpty()) {
                            usernameText.setText(username);
                        } else {
                            usernameText.setText("Anonymous Nurse");
                        }
                        
                        // Load profile picture
                        if (profileImage != null) {
                            if (photoURL != null && !photoURL.isEmpty()) {
                                // Load profile picture using Glide
                                Glide.with(this)
                                        .load(photoURL)
                                        .placeholder(R.drawable.ic_profile_placeholder)
                                        .error(R.drawable.ic_profile_placeholder)
                                        .circleCrop()
                                        .into(profileImage);
                            } else {
                                // Set default profile placeholder
                                profileImage.setImageResource(R.drawable.ic_profile_placeholder);
                            }
                        }
                        
                        // Make profile image and username clickable to open user profile
                        if (profileImage != null) {
                            profileImage.setOnClickListener(v -> openUserProfile(userId));
                        }
                        
                        if (usernameText != null) {
                            usernameText.setOnClickListener(v -> openUserProfile(userId));
                        }
                        
                    } else {
                        usernameText.setText("Anonymous Nurse");
                        if (profileImage != null) {
                            profileImage.setImageResource(R.drawable.ic_profile_placeholder);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    usernameText.setText("Anonymous Nurse");
                    if (profileImage != null) {
                        profileImage.setImageResource(R.drawable.ic_profile_placeholder);
                    }
                });
    }

    private String formatTime(Long timestamp) {
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - timestamp;
        
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d ago";
        } else if (hours > 0) {
            return hours + "h ago";
        } else if (minutes > 0) {
            return minutes + "m ago";
        } else {
            return "Just now";
        }
    }

    private void checkCommunityProfileStatus() {
        // Check if user is authenticated first
        if (!profileService.isUserAuthenticated()) {
            // User not authenticated, show community notice
            hasCommunityProfile = false;
            updateUIForCommunityProfile();
            return;
        }

        // Check if user has community profile from Firebase
        profileService.checkCommunityProfileExists(new CommunityProfileService.ProfileCheckCallback() {
            @Override
            public void onProfileExists(String profileId) {
                hasCommunityProfile = true;
                updateUIForCommunityProfile();
            }

            @Override
            public void onProfileNotExists() {
                hasCommunityProfile = false;
                updateUIForCommunityProfile();
            }

            @Override
            public void onError(String error) {
                // On error, assume no profile exists
                hasCommunityProfile = false;
                updateUIForCommunityProfile();
            }
        });
    }

    private void updateUIForCommunityProfile() {
        if (hasCommunityProfile) {
            // User has community profile - show create post button, hide notice
            binding.btnCreatePost.setVisibility(View.VISIBLE);
            binding.cardCommunityNotice.setVisibility(View.GONE);
            
            // Add click listener to view profile
            binding.btnCreateCommunityProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, CommunityProfileViewActivity.class);
                startActivity(intent);
            });
        } else {
            // User doesn't have community profile - hide create post button, show notice
            binding.btnCreatePost.setVisibility(View.GONE);
            binding.cardCommunityNotice.setVisibility(View.VISIBLE);
            
            // Keep original create profile functionality
            binding.btnCreateCommunityProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, CommunityProfileActivity.class);
                startActivity(intent);
            });
        }
    }

    private void openCreatePostActivity() {
        Intent intent = new Intent(this, CreatePostActivity.class);
        createPostLauncher.launch(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh posts when returning to the activity
        loadPostsFromFirebase();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    private void setupPostGestures(View postView, String postId, String postUserId) {
        ImageView postImage = postView.findViewById(R.id.post_image);
        ImageView likeIcon = postView.findViewById(R.id.like_icon);
        TextView likesCountText = postView.findViewById(R.id.likes_count);
        
        if (postImage != null && postImage.getVisibility() == View.VISIBLE) {
            // Create gesture detector for double-tap
            GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    // Double-tap detected - like the post
                    toggleLike(postId, likeIcon, likesCountText);
                    
                    // Show heart animation
                    showHeartAnimation(postImage, e.getX(), e.getY());
                    
                    return true;
                }
            });
            
            // Create swipe listener
            OnSwipeTouchListener swipeListener = new OnSwipeTouchListener(this) {
                @Override
                public void onSwipeLeft() {
                    // Open the post author's profile
                    openUserProfile(postUserId);
                }
                
                @Override
                public void onSwipeRight() {
                    // Do nothing for swipe right
                }
                
                @Override
                public void onSwipeUp() {
                    // Do nothing for swipe up
                }
                
                @Override
                public void onSwipeDown() {
                    // Do nothing for swipe down
                }
                
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // Handle double-tap first
                    gestureDetector.onTouchEvent(event);
                    // Then handle swipe
                    return super.onTouch(v, event);
                }
            };
            
            postImage.setOnTouchListener(swipeListener);
        }
    }
    
    private void showHeartAnimation(View parentView, float x, float y) {
        // Create heart image view
        ImageView heartView = new ImageView(this);
        heartView.setImageResource(R.drawable.ic_favorite_filled);
        heartView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
        heartView.setX(x - 50);
        heartView.setY(y - 50);
        heartView.setScaleX(0);
        heartView.setScaleY(0);
        heartView.setAlpha(0.8f);
        
        // Add to parent view
        if (parentView.getParent() instanceof ViewGroup) {
            ((ViewGroup) parentView.getParent()).addView(heartView);
            
            // Animate heart
            heartView.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .alpha(1f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        heartView.animate()
                                .scaleX(0)
                                .scaleY(0)
                                .alpha(0f)
                                .translationY(-100)
                                .setDuration(300)
                                .withEndAction(() -> {
                                    ((ViewGroup) parentView.getParent()).removeView(heartView);
                                })
                                .start();
                    })
                    .start();
        }
    }
    
    private void openUserProfile(String userId) {
        // First get the username from Firestore, then open the profile
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String displayName = documentSnapshot.getString("displayName");
                        
                        // Use displayName if available, otherwise use username
                        String userName = (displayName != null && !displayName.isEmpty()) ? displayName : username;
                        
                        // Open UserProfileActivity
                        Intent intent = new Intent(this, com.example.nurse_connect.ui.profile.UserProfileActivity.class);
                        intent.putExtra("user_id", userId);
                        intent.putExtra("user_name", userName);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadCurrentUserProfilePicture() {
        String currentUserId = profileService.getCurrentUserId();
        if (currentUserId == null) return;

        // First try to load from community profile, then fall back to main user profile
        db.collection("community_profiles")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(communitySnapshot -> {
                    if (communitySnapshot.exists() && communitySnapshot.contains("photoURL")) {
                        String communityPhotoURL = communitySnapshot.getString("photoURL");
                        if (communityPhotoURL != null && !communityPhotoURL.isEmpty()) {
                            // Use community profile picture
                            loadHeaderProfilePicture(communityPhotoURL);
                            return;
                        }
                    }
                    
                    // If no community profile picture, try main user profile
                    loadHeaderFromMainUserProfile(currentUserId);
                })
                .addOnFailureListener(e -> {
                    // If community profile check fails, try main user profile
                    loadHeaderFromMainUserProfile(currentUserId);
                });
    }
    
    private void loadHeaderFromMainUserProfile(String currentUserId) {
        db.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String photoURL = documentSnapshot.getString("photoURL");
                        loadHeaderProfilePicture(photoURL);
                    } else {
                        binding.btnProfile.post(() -> {
                            binding.btnProfile.setImageResource(R.drawable.ic_profile_placeholder);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    binding.btnProfile.post(() -> {
                        binding.btnProfile.setImageResource(R.drawable.ic_profile_placeholder);
                    });
                });
    }
    
    private void loadHeaderProfilePicture(String photoURL) {
        if (photoURL != null && !photoURL.isEmpty()) {
            binding.btnProfile.post(() -> {
                Glide.with(this)
                        .load(photoURL)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(binding.btnProfile);
            });
        } else {
            binding.btnProfile.post(() -> {
                binding.btnProfile.setImageResource(R.drawable.ic_profile_placeholder);
            });
        }
    }
} 