package com.example.nurse_connect.services;

import android.util.Log;

import com.example.nurse_connect.models.FeaturedContent;
import com.example.nurse_connect.models.StudyMaterial;
import com.example.nurse_connect.models.User;
import com.example.nurse_connect.models.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecommendationService {
    private static final String TAG = "RecommendationService";
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    public interface RecommendationCallback {
        void onSuccess(List<FeaturedContent> recommendations);
        void onFailure(Exception e);
    }

    public RecommendationService() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public void getFeaturedContent(RecommendationCallback callback) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        
        if (currentUserId == null) {
            // For guest users, show general popular content
            getPopularContent(callback);
            return;
        }

        // Get user profile to understand their course and interests
        firestore.collection("user_profiles")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(userProfileDoc -> {
                    UserProfile userProfile = userProfileDoc.exists() ?
                            userProfileDoc.toObject(UserProfile.class) : new UserProfile();

                    // Get personalized recommendations with interaction history
                    getUserInteractionHistory(currentUserId, userProfile, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get user profile, falling back to popular content", e);
                    getPopularContent(callback);
                });
    }

    private void getPersonalizedRecommendations(String userId, UserProfile userProfile, RecommendationCallback callback) {
        List<FeaturedContent> allRecommendations = new ArrayList<>();
        
        // Get study materials with high engagement
        firestore.collection("study_materials")
                .orderBy("likes", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(materialsSnapshot -> {
                    for (QueryDocumentSnapshot doc : materialsSnapshot) {
                        StudyMaterial material = doc.toObject(StudyMaterial.class);
                        if (material != null) {
                            FeaturedContent content = createFeaturedContentFromMaterial(material);
                            
                            // Check if relevant to user's course
                            if (isRelevantToCourse(material, userProfile)) {
                                content.setRelevantToCourse(true);
                                content.setEngagementScore(content.getEngagementScore() + 100); // Boost for relevance
                            }
                            
                            allRecommendations.add(content);
                        }
                    }
                    
                    // Get popular groups
                    getPopularGroups(allRecommendations, userProfile, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get study materials", e);
                    callback.onFailure(e);
                });
    }

    private void getPopularGroups(List<FeaturedContent> recommendations, UserProfile userProfile, RecommendationCallback callback) {
        firestore.collection("group_chats")
                .whereEqualTo("isPublic", true)
                .orderBy("memberCount", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(groupsSnapshot -> {
                    for (QueryDocumentSnapshot doc : groupsSnapshot) {
                        Map<String, Object> groupData = doc.getData();
                        FeaturedContent content = createFeaturedContentFromGroup(doc.getId(), groupData);
                        recommendations.add(content);
                    }
                    
                    // Get featured users
                    getFeaturedUsers(recommendations, userProfile, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get groups", e);
                    // Continue without groups
                    getFeaturedUsers(recommendations, userProfile, callback);
                });
    }

    private void getFeaturedUsers(List<FeaturedContent> recommendations, UserProfile userProfile, RecommendationCallback callback) {
        firestore.collection("community_profiles")
                .limit(5)
                .get()
                .addOnSuccessListener(usersSnapshot -> {
                    for (QueryDocumentSnapshot doc : usersSnapshot) {
                        Map<String, Object> userData = doc.getData();
                        FeaturedContent content = createFeaturedContentFromUser(doc.getId(), userData);
                        recommendations.add(content);
                    }
                    
                    // Get available quiz matches
                    getAvailableQuizMatches(recommendations, userProfile, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get users", e);
                    // Continue without featured users
                    getAvailableQuizMatches(recommendations, userProfile, callback);
                });
    }

    private void getPopularContent(RecommendationCallback callback) {
        List<FeaturedContent> recommendations = new ArrayList<>();
        
        // Get most popular study materials
        firestore.collection("study_materials")
                .orderBy("likes", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (QueryDocumentSnapshot doc : snapshot) {
                        StudyMaterial material = doc.toObject(StudyMaterial.class);
                        if (material != null) {
                            recommendations.add(createFeaturedContentFromMaterial(material));
                        }
                    }
                    
                    // Shuffle for variety
                    Collections.shuffle(recommendations);
                    callback.onSuccess(recommendations.subList(0, Math.min(5, recommendations.size())));
                })
                .addOnFailureListener(callback::onFailure);
    }

    private FeaturedContent createFeaturedContentFromMaterial(StudyMaterial material) {
        FeaturedContent content = new FeaturedContent();
        content.setId(material.getId());
        content.setTitle(material.getTitle());
        content.setDescription(material.getDescription());
        content.setImageUrl(material.getThumbnailURL());
        content.setContentType("study_material");
        content.setContentId(material.getId());
        content.setLikes(material.getLikes());
        content.setViews(material.getViews());
        content.setComments(material.getCommentCount());
        content.setAuthorName(material.getAuthorName());
        content.setAuthorPhotoUrl(material.getAuthorPhotoURL());
        content.setCategory(material.getCategory());
        content.calculateEngagementScore();
        return content;
    }

    private FeaturedContent createFeaturedContentFromGroup(String groupId, Map<String, Object> groupData) {
        FeaturedContent content = new FeaturedContent();
        content.setId(groupId);
        content.setTitle((String) groupData.get("groupName"));
        content.setDescription((String) groupData.get("description"));
        content.setContentType("group");
        content.setContentId(groupId);
        
        // Use member count as engagement metric
        Long memberCount = (Long) groupData.get("memberCount");
        if (memberCount != null) {
            content.setViews(memberCount.intValue());
            content.setEngagementScore(memberCount.intValue() * 2);
        }
        
        return content;
    }

    private void getAvailableQuizMatches(List<FeaturedContent> recommendations, UserProfile userProfile, RecommendationCallback callback) {
        // Get available quiz matches that are waiting for players
        firestore.collection("quiz_matches")
                .whereEqualTo("status", "waiting")
                .whereLessThan("currentPlayerCount", "maxPlayers")
                .limit(3)
                .get()
                .addOnSuccessListener(quizSnapshot -> {
                    for (QueryDocumentSnapshot doc : quizSnapshot) {
                        Map<String, Object> quizData = doc.getData();
                        FeaturedContent content = createFeaturedContentFromQuizMatch(doc.getId(), quizData);
                        recommendations.add(content);
                    }
                    
                    // Sort and return final recommendations
                    finalizeRecommendations(recommendations, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get quiz matches", e);
                    // Continue without quiz matches
                    finalizeRecommendations(recommendations, callback);
                });
    }
    
    private FeaturedContent createFeaturedContentFromQuizMatch(String matchId, Map<String, Object> matchData) {
        FeaturedContent content = new FeaturedContent();
        content.setId(matchId);
        content.setTitle("Quiz Battle Available!");
        content.setDescription(String.format("Join a %s quiz on %s - %s. %d/%d players joined.",
                matchData.get("career"),
                matchData.get("course"),
                matchData.get("unit"),
                matchData.get("currentPlayerCount"),
                matchData.get("maxPlayers")));
        content.setImageUrl("https://firebasestorage.googleapis.com/v0/b/nurseconnect-c68eb.firebasestorage.app/o/nursing-avatars%2Fspecial%2Fic_quiz_battle.png?alt=media");
        content.setContentType("quiz_match");
        content.setContentId(matchId);
        content.setCategory("Quiz Battle");
        content.setEngagementScore(500); // High priority for quiz matches
        return content;
    }
    
    private FeaturedContent createFeaturedContentFromUser(String userId, Map<String, Object> userData) {
        FeaturedContent content = new FeaturedContent();
        content.setId(userId);
        content.setTitle((String) userData.get("displayName"));
        content.setDescription((String) userData.get("bio"));
        content.setImageUrl((String) userData.get("photoURL"));
        content.setContentType("user");
        content.setContentId(userId);
        content.setAuthorName((String) userData.get("displayName"));
        content.setAuthorPhotoUrl((String) userData.get("photoURL"));
        
        // Use follower count as engagement metric
        Long followerCount = (Long) userData.get("followersCount");
        if (followerCount != null) {
            content.setViews(followerCount.intValue());
            content.setEngagementScore(followerCount.intValue() * 3);
        }
        
        return content;
    }

    private boolean isRelevantToCourse(StudyMaterial material, UserProfile userProfile) {
        if (userProfile.getCourse() == null || userProfile.getCourse().isEmpty()) {
            return false;
        }
        
        String userCourse = userProfile.getCourse().toLowerCase();
        String materialCategory = material.getCategory() != null ? material.getCategory().toLowerCase() : "";
        String materialTitle = material.getTitle() != null ? material.getTitle().toLowerCase() : "";
        
        // Check if material category or title contains course-related keywords
        return materialCategory.contains(userCourse) || 
               materialTitle.contains(userCourse) ||
               isRelatedToNursingSpecialization(material, userProfile);
    }

    private boolean isRelatedToNursingSpecialization(StudyMaterial material, UserProfile userProfile) {
        if (userProfile.getSpecialization() == null || userProfile.getSpecialization().isEmpty()) {
            return false;
        }

        String specialization = userProfile.getSpecialization().toLowerCase();
        String materialCategory = material.getCategory() != null ? material.getCategory().toLowerCase() : "";
        String materialTitle = material.getTitle() != null ? material.getTitle().toLowerCase() : "";

        return materialCategory.contains(specialization) || materialTitle.contains(specialization);
    }

    /**
     * Track user interaction with featured content for improving recommendations
     */
    public void trackUserInteraction(String userId, String contentId, String contentType, String interactionType) {
        if (userId == null || contentId == null) return;

        Map<String, Object> interaction = new HashMap<>();
        interaction.put("userId", userId);
        interaction.put("contentId", contentId);
        interaction.put("contentType", contentType);
        interaction.put("interactionType", interactionType); // "view", "click", "like", "download"
        interaction.put("timestamp", System.currentTimeMillis());

        firestore.collection("user_interactions")
                .add(interaction)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "User interaction tracked: " + interactionType + " on " + contentType);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to track user interaction", e);
                });
    }

    /**
     * Get user's interaction history to improve recommendations
     */
    private void getUserInteractionHistory(String userId, UserProfile userProfile, RecommendationCallback callback) {
        firestore.collection("user_interactions")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50) // Get recent 50 interactions
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, Integer> contentPreferences = new HashMap<>();
                    Map<String, Integer> categoryPreferences = new HashMap<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String contentType = doc.getString("contentType");
                        String interactionType = doc.getString("interactionType");

                        // Weight different interaction types
                        int weight = getInteractionWeight(interactionType);

                        if (contentType != null) {
                            contentPreferences.put(contentType,
                                contentPreferences.getOrDefault(contentType, 0) + weight);
                        }
                    }

                    // Use interaction history to get better recommendations
                    getPersonalizedRecommendationsWithHistory(userId, userProfile, contentPreferences, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get user interaction history", e);
                    // Fall back to basic personalized recommendations
                    getPersonalizedRecommendations(userId, userProfile, callback);
                });
    }

    private int getInteractionWeight(String interactionType) {
        switch (interactionType) {
            case "download": return 5;
            case "like": return 3;
            case "click": return 2;
            case "view": return 1;
            default: return 1;
        }
    }

    private void getPersonalizedRecommendationsWithHistory(String userId, UserProfile userProfile,
                                                          Map<String, Integer> contentPreferences,
                                                          RecommendationCallback callback) {
        List<FeaturedContent> allRecommendations = new ArrayList<>();

        // Prioritize content types based on user's interaction history
        String preferredContentType = getPreferredContentType(contentPreferences);

        // Get study materials with preference weighting
        firestore.collection("study_materials")
                .orderBy("likes", Query.Direction.DESCENDING)
                .limit(15) // Get more to filter and rank
                .get()
                .addOnSuccessListener(materialsSnapshot -> {
                    for (QueryDocumentSnapshot doc : materialsSnapshot) {
                        StudyMaterial material = doc.toObject(StudyMaterial.class);
                        if (material != null) {
                            FeaturedContent content = createFeaturedContentFromMaterial(material);

                            // Apply preference boost
                            if ("study_material".equals(preferredContentType)) {
                                content.setEngagementScore(content.getEngagementScore() + 50);
                            }

                            // Check if relevant to user's course
                            if (isRelevantToCourse(material, userProfile)) {
                                content.setRelevantToCourse(true);
                                content.setEngagementScore(content.getEngagementScore() + 100);
                            }

                            allRecommendations.add(content);
                        }
                    }

                    // Continue with groups and users
                    getPopularGroupsWithHistory(allRecommendations, userProfile, contentPreferences, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get study materials with history", e);
                    callback.onFailure(e);
                });
    }

    private String getPreferredContentType(Map<String, Integer> contentPreferences) {
        return contentPreferences.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("study_material");
    }

    private void getPopularGroupsWithHistory(List<FeaturedContent> recommendations, UserProfile userProfile,
                                           Map<String, Integer> contentPreferences, RecommendationCallback callback) {
        firestore.collection("group_chats")
                .whereEqualTo("isPublic", true)
                .orderBy("memberCount", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(groupsSnapshot -> {
                    for (QueryDocumentSnapshot doc : groupsSnapshot) {
                        Map<String, Object> groupData = doc.getData();
                        FeaturedContent content = createFeaturedContentFromGroup(doc.getId(), groupData);

                        // Apply preference boost for groups
                        if (contentPreferences.getOrDefault("group", 0) > 0) {
                            content.setEngagementScore(content.getEngagementScore() + 30);
                        }

                        recommendations.add(content);
                    }

                    // Get featured users with history
                    getFeaturedUsersWithHistory(recommendations, userProfile, contentPreferences, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get groups with history", e);
                    getFeaturedUsersWithHistory(recommendations, userProfile, contentPreferences, callback);
                });
    }

    private void getFeaturedUsersWithHistory(List<FeaturedContent> recommendations, UserProfile userProfile,
                                           Map<String, Integer> contentPreferences, RecommendationCallback callback) {
        firestore.collection("community_profiles")
                .limit(5)
                .get()
                .addOnSuccessListener(usersSnapshot -> {
                    for (QueryDocumentSnapshot doc : usersSnapshot) {
                        Map<String, Object> userData = doc.getData();
                        FeaturedContent content = createFeaturedContentFromUser(doc.getId(), userData);

                        // Apply preference boost for users
                        if (contentPreferences.getOrDefault("user", 0) > 0) {
                            content.setEngagementScore(content.getEngagementScore() + 20);
                        }

                        recommendations.add(content);
                    }

                    // Sort and return final recommendations with history weighting
                    finalizeRecommendationsWithHistory(recommendations, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get users with history", e);
                    finalizeRecommendationsWithHistory(recommendations, callback);
                });
    }

    private void finalizeRecommendationsWithHistory(List<FeaturedContent> recommendations, RecommendationCallback callback) {
        // Sort by engagement score (descending) with randomization for variety
        Collections.sort(recommendations, (a, b) -> {
            int scoreComparison = Integer.compare(b.getEngagementScore(), a.getEngagementScore());
            // Add slight randomization to prevent always showing the same order
            if (scoreComparison == 0) {
                return (int) (Math.random() * 3 - 1); // Random -1, 0, or 1
            }
            return scoreComparison;
        });

        // Limit to top 8 recommendations for the horizontal scroll
        int maxRecommendations = Math.min(8, recommendations.size());
        List<FeaturedContent> finalRecommendations = recommendations.subList(0, maxRecommendations);

        callback.onSuccess(finalRecommendations);
    }

    private void finalizeRecommendations(List<FeaturedContent> recommendations, RecommendationCallback callback) {
        // Sort by engagement score (descending)
        Collections.sort(recommendations, (a, b) -> Integer.compare(b.getEngagementScore(), a.getEngagementScore()));
        
        // Limit to top 8 recommendations for the horizontal scroll
        int maxRecommendations = Math.min(8, recommendations.size());
        List<FeaturedContent> finalRecommendations = recommendations.subList(0, maxRecommendations);
        
        callback.onSuccess(finalRecommendations);
    }
}
