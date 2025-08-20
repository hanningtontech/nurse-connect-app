package com.example.nurse_connect.services;

import android.util.Log;

import com.example.nurse_connect.models.MatchmakingQueue;
import com.example.nurse_connect.models.PlayerStats;
import com.example.nurse_connect.models.QuizMatch;
import com.example.nurse_connect.models.QuizQuestion;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.Map;

public class QuizMatchService {
    private static final String TAG = "QuizMatchService";
    private static final long MAX_WAIT_TIME = 60000; // 1 minute max wait
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration matchListener;
    private ListenerRegistration queueListener;
    
    public interface MatchmakingCallback {
        void onMatchFound(QuizMatch match);
        void onMatchmakingTimeout();
        void onError(String error);
    }
    
    public interface QuizMatchCallback {
        void onMatchUpdated(QuizMatch match);
        void onMatchCompleted(QuizMatch match);
        void onError(String error);
    }
    
    public QuizMatchService() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }
    
    /**
     * Create a new match and wait for players to join
     */
    public void createMatchAndWait(String course, String unit, String career, 
                                  int targetPlayerCount, MatchmakingCallback callback) {
        String currentUserId = auth.getCurrentUser().getUid();
        String playerName = auth.getCurrentUser().getDisplayName();
        
        Log.d(TAG, "🎮 Creating new match for " + targetPlayerCount + " players");
        Log.d(TAG, "🎮 Course: " + course + ", Unit: " + unit + ", Career: " + career);
        
        // Create the match immediately
        String matchId = UUID.randomUUID().toString();
        QuizMatch match = new QuizMatch(matchId, course, unit, career);
        match.setTargetPlayerCount(targetPlayerCount);
        
        // Add the creator as the first player
        match.addPlayer(currentUserId, playerName);
        
        // Set the creator as ready
        match.setPlayerReady(currentUserId, true);
        
        // FIXED: Fetch questions immediately when creating the match
        Log.d(TAG, "🎮 Fetching " + match.getTotalQuestions() + " questions for new match");
        Log.d(TAG, "🎮 Course: " + course + ", Unit: " + unit + ", Career: " + career);
        Log.d(TAG, "🎮 Total questions needed: " + match.getTotalQuestions());
        
        // Check database status first
        checkDatabaseStatus();
        
        getRandomQuestions(course, unit, career, match.getTotalQuestions(), questionIds -> {
            Log.d(TAG, "🎮 Questions retrieved for new match: " + questionIds.size() + " questions");
            Log.d(TAG, "🎮 Question IDs: " + questionIds);
            
            // Set the questions in the match
            match.setQuestionIds(questionIds);
            Log.d(TAG, "🎮 Questions set in match object");
            
            if (!questionIds.isEmpty()) {
                match.setCurrentQuestionId(questionIds.get(0));
                Log.d(TAG, "🎮 First question ID set: " + questionIds.get(0));
                Log.d(TAG, "🎮 Match questionIds size: " + match.getQuestionIds().size());
                Log.d(TAG, "🎮 Match currentQuestionId: " + match.getCurrentQuestionId());
            } else {
                Log.w(TAG, "⚠️ No questions found for new match!");
                Log.w(TAG, "⚠️ This will cause the match to fail when it starts!");
            }
            
            // Save the match to Firestore with questions
            Log.d(TAG, "🎮 About to save match to Firestore...");
            Log.d(TAG, "🎮 Final check - Match questionIds size: " + match.getQuestionIds().size());
            Log.d(TAG, "🎮 Final check - Current question ID: " + match.getCurrentQuestionId());
            
            db.collection("quiz_matches")
                    .document(matchId)
                    .set(match)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "🎮 Match created successfully with questions: " + matchId);
                        Log.d(TAG, "🎮 Questions loaded: " + questionIds.size());
                        Log.d(TAG, "🎮 Match questionIds size: " + match.getQuestionIds().size());
                        Log.d(TAG, "🎮 Current question ID: " + match.getCurrentQuestionId());
                        Log.d(TAG, "🎮 Waiting for " + (targetPlayerCount - 1) + " more players to join...");
                        
                        // Return the match to the creator
                        callback.onMatchFound(match);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to create match", e);
                        callback.onError("Failed to create match: " + e.getMessage());
                    });
        });
    }
    
    /**
     * Find available matches to join (default 2 players)
     */
    public void findAvailableMatches(String course, String unit, String career, 
                                   MatchmakingCallback callback) {
        findAvailableMatches(course, unit, career, 2, callback); // Default to 2 players
    }
    
    /**
     * Find available matches to join with specific player count and skill-based matching
     */
    public void findAvailableMatches(String course, String unit, String career, 
                                   int targetPlayerCount, MatchmakingCallback callback) {
        Log.d(TAG, "🔍 Looking for available matches to join with skill-based matching");
        Log.d(TAG, "🔍 Course: " + course + ", Unit: " + unit + ", Career: " + career);
        
        String currentUserId = auth.getCurrentUser().getUid();
        
        // First, get the current player's stats to determine their rank
        db.collection("player_stats")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(playerDoc -> {
                    String playerRank = "Bronze"; // Default rank
                    
                    if (playerDoc.exists()) {
                        PlayerStats playerStats = playerDoc.toObject(PlayerStats.class);
                        if (playerStats != null) {
                            playerRank = playerStats.getCurrentRank();
                            Log.d(TAG, "🔍 Current player rank: " + playerRank);
                        }
                    }
                    
                    // Now search for matches with skill-based filtering
                    searchMatchesWithSkillMatching(course, unit, career, targetPlayerCount, playerRank, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get player stats, using default rank", e);
                    // Fallback to default rank if stats retrieval fails
                    searchMatchesWithSkillMatching(course, unit, career, targetPlayerCount, "Bronze", callback);
                });
    }
    
    /**
     * Search for matches with skill-based matching
     */
    private void searchMatchesWithSkillMatching(String course, String unit, String career, 
                                              int targetPlayerCount, String playerRank, 
                                              MatchmakingCallback callback) {
        Log.d(TAG, "🔍 Searching for matches with rank: " + playerRank);
        
        // First, try to find matches with exact rank match
        db.collection("quiz_matches")
                .whereEqualTo("course", course)
                .whereEqualTo("unit", unit)
                .whereEqualTo("career", career)
                .whereEqualTo("status", "waiting")
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<QuizMatch> availableMatches = new ArrayList<>();
                    
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        QuizMatch match = doc.toObject(QuizMatch.class);
                        if (match != null && !match.isMatchFull()) {
                            // Check if the match has compatible player ranks
                            if (isRankCompatible(match, playerRank)) {
                                availableMatches.add(match);
                                Log.d(TAG, "🔍 Found rank-compatible match: " + match.getMatchId() + 
                                      " (Players: " + match.getPlayerIds().size() + "/" + match.getTargetPlayerCount() + 
                                      ", Rank: " + playerRank + ")");
                            }
                        }
                    }
                    
                    if (!availableMatches.isEmpty()) {
                        // Sort by rank compatibility (exact matches first, then nearby ranks)
                        sortMatchesByRankCompatibility(availableMatches, playerRank);
                        
                        // Join the best available match
                        QuizMatch matchToJoin = availableMatches.get(0);
                        Log.d(TAG, "🎯 Joining rank-compatible match: " + matchToJoin.getMatchId());
                        joinExistingMatch(matchToJoin.getMatchId(), callback);
                    } else {
                        Log.d(TAG, "🔍 No rank-compatible matches found, creating new one for " + targetPlayerCount + " players");
                        // No available matches, create a new one with the player's rank
                        createMatchAndWait(course, unit, career, targetPlayerCount, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to find available matches", e);
                    callback.onError("Failed to find available matches: " + e.getMessage());
                });
    }
    
    /**
     * Check if a match has compatible player ranks
     */
    private boolean isRankCompatible(QuizMatch match, String playerRank) {
        // For now, allow all ranks to play together
        // In the future, you could implement stricter rank matching
        return true;
    }
    
    /**
     * Sort matches by rank compatibility
     */
    private void sortMatchesByRankCompatibility(List<QuizMatch> matches, String playerRank) {
        // Simple sorting: exact rank matches first, then by player count
        matches.sort((m1, m2) -> {
            // Prefer matches with fewer players (closer to starting)
            int playerDiff = m1.getPlayerIds().size() - m2.getPlayerIds().size();
            if (playerDiff != 0) {
                return playerDiff;
            }
            
            // If player counts are equal, prefer newer matches
            return Long.compare(m1.getStartTime(), m2.getStartTime());
        });
    }
    
    /**
     * Join an existing match
     */
    public void joinExistingMatch(String matchId, MatchmakingCallback callback) {
        String currentUserId = auth.getCurrentUser().getUid();
        String playerName = auth.getCurrentUser().getDisplayName();
        
        Log.d(TAG, "🎯 Attempting to join existing match: " + matchId);
        
        // Get the match document
        db.collection("quiz_matches")
                .document(matchId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        QuizMatch match = documentSnapshot.toObject(QuizMatch.class);
                        if (match != null) {
                            // Check if match is full
                            if (match.isMatchFull()) {
                                Log.d(TAG, "❌ Match is full, cannot join");
                                callback.onError("Match is full");
                                return;
                            }
                            
                            // Check if match is still waiting
                            if (!"waiting".equals(match.getStatus())) {
                                Log.d(TAG, "❌ Match is not in waiting status, cannot join");
                                callback.onError("Match is not accepting players");
                                return;
                            }
                            
                            // Add player to match
                            match.addPlayer(currentUserId, playerName);
                            
                            // Set the joining player as ready
                            match.setPlayerReady(currentUserId, true);
                            
                            // Check if match is now full and should start
                            if (match.isMatchFull()) {
                                Log.d(TAG, "🎯 Match is now full! Setting status to active");
                                match.setStatus("active");
                                match.setStartTime(System.currentTimeMillis());
                                match.setQuestionStartTime(System.currentTimeMillis());
                            }
                            
                            // Update match in Firestore
                            db.collection("quiz_matches")
                                    .document(matchId)
                                    .set(match)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "🎯 Successfully joined match: " + matchId);
                                        Log.d(TAG, "🎯 Players: " + match.getPlayerIds().size() + "/" + match.getTargetPlayerCount());
                                        if (match.isMatchFull()) {
                                            Log.d(TAG, "🎯 Match is full and ready to start!");
                                        }
                                        callback.onMatchFound(match);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to join match", e);
                                        callback.onError("Failed to join match: " + e.getMessage());
                                    });
                        } else {
                            callback.onError("Failed to parse match");
                        }
                    } else {
                        callback.onError("Match not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get match", e);
                    callback.onError("Failed to get match: " + e.getMessage());
                });
    }
    
    /**
     * Join matchmaking queue for a specific course/unit/career
     */
    public void joinMatchmaking(String course, String unit, String career, 
                               MatchmakingCallback callback) {
        String currentUserId = auth.getCurrentUser().getUid();
        String playerName = auth.getCurrentUser().getDisplayName();
        
        MatchmakingQueue queueEntry = new MatchmakingQueue(currentUserId, playerName, 
                                                          course, unit, career);
        
        Log.d(TAG, "Looking for existing compatible players before joining queue...");
        
        // FIRST: Check if there's already a compatible player waiting
        db.collection("matchmaking_queue")
                .whereEqualTo("course", course)
                .whereEqualTo("unit", unit)
                .whereEqualTo("career", career)
                .whereEqualTo("status", "waiting")
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<MatchmakingQueue> compatiblePlayers = new ArrayList<>();
                    
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        MatchmakingQueue queue = doc.toObject(MatchmakingQueue.class);
                        if (queue != null && !queue.getPlayerId().equals(currentUserId)) {
                            compatiblePlayers.add(queue);
                        }
                    }
                    
                    if (!compatiblePlayers.isEmpty()) {
                        // Found a match immediately! Don't add to queue, create match
                        MatchmakingQueue opponent = compatiblePlayers.get(0);
                        Log.d(TAG, "Found immediate match with player: " + opponent.getPlayerId());
                        createQuizMatch(queueEntry, opponent, callback);
                    } else {
                        // No match found, add to queue and wait
                        Log.d(TAG, "No immediate match found, adding to queue: " + queueEntry.getQueueId());
                        db.collection("matchmaking_queue")
                                .document(queueEntry.getQueueId())
                                .set(queueEntry)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Added to matchmaking queue");
                                    
                                    // Wait a bit for other players to join, then look for matches
                                    new android.os.Handler().postDelayed(() -> {
                                        Log.d(TAG, "Starting match search after delay...");
                                        findMatch(queueEntry, callback);
                                    }, 2000); // Wait 2 seconds
                                    
                                    // Also set up a periodic check every 3 seconds to catch any missed matches
                                    new android.os.Handler().postDelayed(() -> {
                                        Log.d(TAG, "🔄 Periodic match check for player: " + queueEntry.getPlayerId());
                                        periodicMatchCheck(queueEntry, callback);
                                    }, 5000); // Check again after 5 seconds
                                    
                                    // Set timeout
                                    startMatchmakingTimeout(queueEntry.getQueueId(), callback);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to join matchmaking", e);
                                    callback.onError("Failed to join matchmaking: " + e.getMessage());
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking for existing players", e);
                    // Fallback: add to queue anyway
                    Log.d(TAG, "Fallback: adding to queue due to error");
                    db.collection("matchmaking_queue")
                            .document(queueEntry.getQueueId())
                            .set(queueEntry)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Added to matchmaking queue (fallback)");
                                findMatch(queueEntry, callback);
                                startMatchmakingTimeout(queueEntry.getQueueId(), callback);
                            })
                            .addOnFailureListener(fallbackError -> {
                                Log.e(TAG, "Failed to join matchmaking queue (fallback)", fallbackError);
                                callback.onError("Failed to join matchmaking: " + fallbackError.getMessage());
                            });
                });
    }
    
    /**
     * Find a match for the current player
     */
    private void findMatch(MatchmakingQueue playerQueue, MatchmakingCallback callback) {
        Log.d(TAG, "🔍 Finding match for player: " + playerQueue.getPlayerId());
        Log.d(TAG, "🔍 Looking for players with: course=" + playerQueue.getCourse() + 
              ", unit=" + playerQueue.getUnit() + ", career=" + playerQueue.getCareer());
        
        // Look for compatible players in queue
        db.collection("matchmaking_queue")
                .whereEqualTo("course", playerQueue.getCourse())
                .whereEqualTo("unit", playerQueue.getUnit())
                .whereEqualTo("career", playerQueue.getCareer())
                .whereEqualTo("status", "waiting")
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<MatchmakingQueue> compatiblePlayers = new ArrayList<>();
                    
                    Log.d(TAG, "🔍 Found " + querySnapshot.size() + " players in queue");
                    
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        MatchmakingQueue queue = doc.toObject(MatchmakingQueue.class);
                        if (queue != null && !queue.getPlayerId().equals(playerQueue.getPlayerId())) {
                            compatiblePlayers.add(queue);
                            Log.d(TAG, "🔍 Compatible player found: " + queue.getPlayerId());
                        }
                    }
                    
                    Log.d(TAG, "🔍 Total compatible players: " + compatiblePlayers.size());
                    
                    if (!compatiblePlayers.isEmpty()) {
                        // Found a match! Pick the first compatible player
                        MatchmakingQueue opponent = compatiblePlayers.get(0);
                        Log.d(TAG, "🎯 Creating match between " + playerQueue.getPlayerId() + " and " + opponent.getPlayerId());
                        createQuizMatch(playerQueue, opponent, callback);
                    } else {
                        // No match found, wait for opponent to join
                        Log.d(TAG, "⏳ No match found, waiting for opponent to join...");
                        waitForOpponent(playerQueue, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding match", e);
                    callback.onError("Error finding match: " + e.getMessage());
                });
    }
    
    /**
     * Periodic check to ensure no matches are missed
     */
    private void periodicMatchCheck(MatchmakingQueue playerQueue, MatchmakingCallback callback) {
        Log.d(TAG, "🔄 Periodic match check for player: " + playerQueue.getPlayerId());
        
        // Check if we're still in the queue
        db.collection("matchmaking_queue")
                .document(playerQueue.getQueueId())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && "waiting".equals(doc.get("status"))) {
                        Log.d(TAG, "🔄 Player still in queue, checking for opponents...");
                        
                        // Look for compatible players
                        db.collection("matchmaking_queue")
                                .whereEqualTo("course", playerQueue.getCourse())
                                .whereEqualTo("unit", playerQueue.getUnit())
                                .whereEqualTo("career", playerQueue.getCareer())
                                .whereEqualTo("status", "waiting")
                                .limit(10)
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    List<MatchmakingQueue> compatiblePlayers = new ArrayList<>();
                                    
                                    for (DocumentSnapshot queueDoc : querySnapshot.getDocuments()) {
                                        MatchmakingQueue queue = queueDoc.toObject(MatchmakingQueue.class);
                                        if (queue != null && !queue.getPlayerId().equals(playerQueue.getPlayerId())) {
                                            compatiblePlayers.add(queue);
                                        }
                                    }
                                    
                                    if (!compatiblePlayers.isEmpty()) {
                                        Log.d(TAG, "🎯 Periodic check found opponent! Creating match...");
                                        MatchmakingQueue opponent = compatiblePlayers.get(0);
                                        
                                        // Clean up any existing listeners
                                        if (queueListener != null) {
                                            queueListener.remove();
                                            queueListener = null;
                                        }
                                        
                                        createQuizMatch(playerQueue, opponent, callback);
                                    } else {
                                        Log.d(TAG, "🔄 Periodic check: no opponents found yet");
                                    }
                                });
                    } else {
                        Log.d(TAG, "🔄 Player no longer in queue, skipping periodic check");
                    }
                });
    }
    
    /**
     * Wait for an opponent to join the queue
     */
    private void waitForOpponent(MatchmakingQueue playerQueue, MatchmakingCallback callback) {
        Log.d(TAG, "⏳ Player " + playerQueue.getPlayerId() + " waiting for opponents...");
        
        queueListener = db.collection("matchmaking_queue")
                .whereEqualTo("course", playerQueue.getCourse())
                .whereEqualTo("unit", playerQueue.getUnit())
                .whereEqualTo("career", playerQueue.getCareer())
                .whereEqualTo("status", "waiting")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening for opponents", e);
                        return;
                    }
                    
                    if (querySnapshot != null) {
                        Log.d(TAG, "👥 Queue update detected. Total players: " + querySnapshot.size());
                        
                        List<MatchmakingQueue> compatiblePlayers = new ArrayList<>();
                        
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            MatchmakingQueue queue = doc.toObject(MatchmakingQueue.class);
                            if (queue != null && !queue.getPlayerId().equals(playerQueue.getPlayerId())) {
                                compatiblePlayers.add(queue);
                                Log.d(TAG, "👥 Found compatible opponent: " + queue.getPlayerId());
                            }
                        }
                        
                        Log.d(TAG, "👥 Total compatible opponents: " + compatiblePlayers.size());
                        
                        if (!compatiblePlayers.isEmpty()) {
                            // Found an opponent! Pick the first one
                            MatchmakingQueue opponent = compatiblePlayers.get(0);
                            Log.d(TAG, "🎯 Creating match between " + playerQueue.getPlayerId() + " and " + opponent.getPlayerId());
                            
                            // Clean up the listener before creating match
                            if (queueListener != null) {
                                queueListener.remove();
                                queueListener = null;
                            }
                            
                            createQuizMatch(playerQueue, opponent, callback);
                        } else {
                            Log.d(TAG, "⏳ Still waiting for opponents...");
                        }
                    }
                });
    }
    
    /**
     * Create a quiz match between players
     */
    private void createQuizMatch(MatchmakingQueue player1, MatchmakingQueue player2, 
                                MatchmakingCallback callback) {
        createQuizMatchWithPlayerCount(player1, player2, 2, callback);
    }
    
    /**
     * Create a quiz match with a specific target player count
     */
    private void createQuizMatchWithPlayerCount(MatchmakingQueue player1, MatchmakingQueue player2, 
                                               int targetPlayerCount, MatchmakingCallback callback) {
        String matchId = UUID.randomUUID().toString();
        QuizMatch match = new QuizMatch(matchId, player1.getCourse(), 
                                       player1.getUnit(), player1.getCareer());
        
        // Set target player count
        match.setTargetPlayerCount(targetPlayerCount);
        
        // Add both players to the match
        match.addPlayer(player1.getPlayerId(), player1.getPlayerName());
        match.addPlayer(player2.getPlayerId(), player2.getPlayerName());
        
        // Set both players as ready
        match.setPlayerReady(player1.getPlayerId(), true);
        match.setPlayerReady(player2.getPlayerId(), true);
        
        // Get random questions for this match
        getRandomQuestions(match.getCourse(), match.getUnit(), match.getCareer(), 
                          match.getTotalQuestions(), questionIds -> {
                              Log.d(TAG, "Questions retrieved for match: " + questionIds.size() + " questions");
                              
                              match.setQuestionIds(questionIds);
                              if (!questionIds.isEmpty()) {
                                  match.setCurrentQuestionId(questionIds.get(0));
                                  Log.d(TAG, "First question ID set: " + questionIds.get(0));
                              } else {
                                  Log.w(TAG, "No questions found for match!");
                              }
                              
                              // Save the match to Firestore
                              db.collection("quiz_matches")
                                      .document(matchId)
                                      .set(match)
                                      .addOnSuccessListener(aVoid -> {
                                          // Update queue status for both players
                                          updateQueueStatus(player1.getQueueId(), "matched");
                                          updateQueueStatus(player2.getQueueId(), "matched");
                                          
                                          Log.d(TAG, "🎯 Quiz match created: " + matchId + " with " + questionIds.size() + " questions");
                                          Log.d(TAG, "🎯 Target players: " + targetPlayerCount + ", Current players: " + match.getPlayerIds().size());
                                          callback.onMatchFound(match);
                                      })
                                      .addOnFailureListener(e -> {
                                          Log.e(TAG, "Failed to create match", e);
                                          callback.onError("Failed to create match: " + e.getMessage());
                                      });
                          });
    }
    
    /**
     * Check if there are any questions in the database
     */
    public void checkDatabaseStatus() {
        Log.d(TAG, "🔍 Checking database status...");
        
        db.collection("quiz_questions")
                .limit(5)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "🔍 Database check - Total questions found: " + querySnapshot.size());
                    if (querySnapshot.size() > 0) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            QuizQuestion question = doc.toObject(QuizQuestion.class);
                            if (question != null) {
                                Log.d(TAG, "🔍 Sample question: ID=" + doc.getId() + 
                                      ", Course=" + question.getCourse() + 
                                      ", Unit=" + question.getUnit() + 
                                      ", Career=" + question.getCareer());
                            }
                        }
                    } else {
                        Log.w(TAG, "⚠️ No questions found in database!");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to check database status", e);
                });
    }
    
    /**
     * Get random questions for a quiz match
     */
    public void getRandomQuestions(String course, String unit, String career, 
                                   int count, QuestionCallback callback) {
        Log.d(TAG, "🔍 Looking for questions: course=" + course + ", unit=" + unit + ", career=" + career);
        Log.d(TAG, "🔍 Requesting " + count + " questions");
        
        // Determine the appropriate collection based on career
        String collectionName = getCollectionNameForCareer(career);
        Log.d(TAG, "🔍 Using collection: " + collectionName);
        
        // First try exact match
        db.collection(collectionName)
                .whereEqualTo("course", course)
                .whereEqualTo("unit", unit)
                .whereEqualTo("career", career)
                .limit(count * 3)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> questionIds = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        questionIds.add(doc.getId());
                    }
                    
                    Log.d(TAG, "🔍 Found " + questionIds.size() + " questions with exact match in " + collectionName);
                    Log.d(TAG, "🔍 Question IDs: " + questionIds);
                    
                    // If we have enough questions, use them
                    if (questionIds.size() >= count) {
                        Collections.shuffle(questionIds);
                        if (questionIds.size() > count) {
                            questionIds = questionIds.subList(0, count);
                        }
                        Log.d(TAG, "✅ Returning " + questionIds.size() + " questions from exact match");
                        callback.onQuestionsRetrieved(questionIds);
                        return;
                    }
                    
                    // If not enough questions, try to get questions from the same course only
                    Log.d(TAG, "Not enough questions, trying course-only match in " + collectionName);
                    
                    // Create a final copy of questionIds for the lambda
                    final List<String> finalQuestionIds = new ArrayList<>(questionIds);
                    
                    db.collection(collectionName)
                            .whereEqualTo("course", course)
                            .limit(count * 3)
                            .get()
                            .addOnSuccessListener(courseQuerySnapshot -> {
                                List<String> courseQuestionIds = new ArrayList<>();
                                for (DocumentSnapshot doc : courseQuerySnapshot.getDocuments()) {
                                    if (!finalQuestionIds.contains(doc.getId())) {
                                        courseQuestionIds.add(doc.getId());
                                    }
                                }
                                
                                // Create a new combined list
                                List<String> combinedQuestionIds = new ArrayList<>(finalQuestionIds);
                                combinedQuestionIds.addAll(courseQuestionIds);
                                Log.d(TAG, "🔍 Total questions found in " + collectionName + ": " + combinedQuestionIds.size());
                                Log.d(TAG, "🔍 Combined question IDs: " + combinedQuestionIds);
                                
                                if (combinedQuestionIds.size() > count) {
                                    Collections.shuffle(combinedQuestionIds);
                                    combinedQuestionIds = combinedQuestionIds.subList(0, count);
                                }
                                
                                Log.d(TAG, "✅ Returning " + combinedQuestionIds.size() + " questions from combined match");
                                callback.onQuestionsRetrieved(combinedQuestionIds);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Failed to get course questions from " + collectionName, e);
                                Log.d(TAG, "⚠️ Returning " + finalQuestionIds.size() + " questions from exact match only");
                                callback.onQuestionsRetrieved(finalQuestionIds); // Return what we have
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to get questions from " + collectionName, e);
                    // If exact match fails, try course-only match immediately
                    Log.d(TAG, "🔄 Exact match failed, trying course-only match in " + collectionName);
                    db.collection(collectionName)
                            .whereEqualTo("course", course)
                            .limit(count * 3)
                            .get()
                            .addOnSuccessListener(courseQuerySnapshot -> {
                                List<String> courseQuestionIds = new ArrayList<>();
                                for (DocumentSnapshot doc : courseQuerySnapshot.getDocuments()) {
                                    courseQuestionIds.add(doc.getId());
                                }
                                
                                Log.d(TAG, "🔍 Found " + courseQuestionIds.size() + " questions with course-only match in " + collectionName);
                                Log.d(TAG, "🔍 Course question IDs: " + courseQuestionIds);
                                
                                if (courseQuestionIds.size() > count) {
                                    Collections.shuffle(courseQuestionIds);
                                    courseQuestionIds = courseQuestionIds.subList(0, count);
                                }
                                
                                Log.d(TAG, "✅ Returning " + courseQuestionIds.size() + " questions from course-only match");
                                callback.onQuestionsRetrieved(courseQuestionIds);
                            })
                            .addOnFailureListener(courseError -> {
                                Log.e(TAG, "❌ Failed to get course questions from " + collectionName, courseError);
                                Log.w(TAG, "⚠️ No questions found, returning empty list");
                                callback.onQuestionsRetrieved(new ArrayList<>());
                            });
                });
    }
    
    /**
     * Get the appropriate Firestore collection name for a career
     */
    private String getCollectionNameForCareer(String career) {
        // For now, use the general quiz_questions collection
        // This can be updated later if you want to separate questions by career level
        return "quiz_questions";
    }
    
    /**
     * Listen for match updates
     */
    public void listenToMatch(String matchId, QuizMatchCallback callback) {
        matchListener = db.collection("quiz_matches")
                .document(matchId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to match", e);
                        callback.onError("Error listening to match: " + e.getMessage());
                        return;
                    }
                    
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        QuizMatch match = documentSnapshot.toObject(QuizMatch.class);
                        if (match != null) {
                            if ("completed".equals(match.getStatus())) {
                                callback.onMatchCompleted(match);
                            } else {
                                callback.onMatchUpdated(match);
                            }
                        }
                    }
                });
    }
    
    /**
     * Submit an answer for the current question
     */
    public void submitAnswer(String matchId, int selectedAnswer, AnswerCallback callback) {
        String currentUserId = auth.getCurrentUser().getUid();
        
        db.collection("quiz_matches")
                .document(matchId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    QuizMatch match = documentSnapshot.toObject(QuizMatch.class);
                    if (match == null || !"active".equals(match.getStatus())) {
                        callback.onAnswerResult(false, "Match not active");
                        return;
                    }
                    
                    // Get the current question
                    db.collection("quiz_questions")
                            .document(match.getCurrentQuestionId())
                            .get()
                            .addOnSuccessListener(questionDoc -> {
                                QuizQuestion question = questionDoc.toObject(QuizQuestion.class);
                                if (question == null) {
                                    callback.onAnswerResult(false, "Question not found");
                                    return;
                                }
                                
                                boolean isCorrect = question.isAnswerCorrect(selectedAnswer);
                                
                                if (isCorrect) {
                                    // Correct answer - mark player as answered and award point
                                    if (!match.getPlayersAnsweredCurrentQuestion().getOrDefault(currentUserId, false)) {
                                        match.markPlayerAnswered(currentUserId);
                                        match.incrementPlayerScore(currentUserId);
                                        Log.d(TAG, "🎯 Player " + currentUserId + " answered correctly! Point awarded.");
                                    }
                                    
                                    // Mark question as completed
                                    match.setCurrentQuestionCompleted(true);
                                    match.setCurrentQuestionAnsweredBy(currentUserId);
                                    
                                    // Update match in Firestore
                                    db.collection("quiz_matches")
                                            .document(matchId)
                                            .set(match)
                                            .addOnSuccessListener(aVoid -> {
                                                callback.onAnswerResult(true, "Correct! Point awarded. Waiting for opponent...");
                                                
                                                // Check if we should move to next question
                                                checkAndMoveToNextQuestion(matchId, match);
                                            });
                                } else {
                                    // Incorrect answer - allow retry unless question is already completed
                                    if (match.isCurrentQuestionCompleted()) {
                                        callback.onAnswerResult(false, "Question already completed by opponent. Moving to next question...");
                                        // Move to next question since opponent already got it right
                                        checkAndMoveToNextQuestion(matchId, match);
                                    } else {
                                        Log.d(TAG, "❌ Player " + currentUserId + " answered incorrectly. Can retry...");
                                        
                                        callback.onAnswerResult(false, "Incorrect answer. Try again or wait for opponent...");
                                    }
                                }
                            });
                });
    }
    
    /**
     * Check if all players have answered and move to next question if needed
     */
    private void checkAndMoveToNextQuestion(String matchId, QuizMatch match) {
        // If all players have answered or question is completed, show next question button
        if (match.haveAllPlayersAnswered() || match.isCurrentQuestionCompleted()) {
            Log.d(TAG, "🔄 Question completed. All players answered: " + match.haveAllPlayersAnswered() + 
                  ", Question completed: " + match.isCurrentQuestionCompleted());
            
            // Set match status to show next question button
            match.setStatus("question_completed");
            match.setNextQuestionReadyTime(System.currentTimeMillis());
            
            // Update match in Firestore
            db.collection("quiz_matches")
                    .document(matchId)
                    .set(match)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ Question completed status set. Waiting for players to press Next...");
                        
                        // Start 15-second auto-advance timer
                        startAutoAdvanceTimer(matchId, match);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Failed to update match for question completion", e);
                    });
        }
    }
    
    /**
     * Start auto-advance timer for next question
     */
    private void startAutoAdvanceTimer(String matchId, QuizMatch match) {
        new android.os.Handler().postDelayed(() -> {
            // Check if match still exists and hasn't been manually advanced
            db.collection("quiz_matches")
                    .document(matchId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        QuizMatch currentMatch = documentSnapshot.toObject(QuizMatch.class);
                        if (currentMatch != null && "question_completed".equals(currentMatch.getStatus())) {
                            Log.d(TAG, "⏰ Auto-advancing to next question after 15 seconds");
                            advanceToNextQuestion(matchId, currentMatch);
                        }
                    });
        }, 15000); // 15 seconds
    }
    
    /**
     * Manually advance to next question (called when player presses Next button)
     */
    public void advanceToNextQuestion(String matchId, MatchmakingCallback callback) {
        String currentUserId = auth.getCurrentUser().getUid();
        
        db.collection("quiz_matches")
                .document(matchId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    QuizMatch match = documentSnapshot.toObject(QuizMatch.class);
                    if (match != null) {
                        // Mark this player as having pressed next
                        match.markPlayerPressedNext(currentUserId);
                        
                        // Check if both players have pressed next
                        if (match.haveAllPlayersPressedNext()) {
                            Log.d(TAG, "🎯 Both players pressed next, advancing to next question");
                            advanceToNextQuestion(matchId, match);
                        } else {
                            Log.d(TAG, "⏳ Player " + currentUserId + " pressed next, waiting for opponent...");
                            // Update match to show waiting status
                            db.collection("quiz_matches")
                                    .document(matchId)
                                    .set(match);
                        }
                        
                        callback.onMatchFound(match); // Notify UI to refresh
                    }
                });
    }
    
    /**
     * Advance to next question or end match
     */
    private void advanceToNextQuestion(String matchId, QuizMatch match) {
        if (match.getCurrentQuestionIndex() < match.getQuestionIds().size() - 1) {
            // More questions available
            match.nextQuestion();
            match.setStatus("active");
            
            // Update match in Firestore
            db.collection("quiz_matches")
                    .document(matchId)
                    .set(match)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ Advanced to question " + (match.getCurrentQuestionIndex() + 1) + 
                              " of " + match.getQuestionIds().size());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Failed to advance to next question", e);
                    });
        } else {
            // No more questions - end match
            Log.d(TAG, "🏁 All questions completed. Ending match...");
            endMatch(matchId, match);
        }
    }
    
    /**
     * End the match and determine winner
     */
    private void endMatch(String matchId, QuizMatch match) {
        match.setStatus("completed");
        match.setEndTime(System.currentTimeMillis());
        
        // Find winner based on scores
        String winnerId = null;
        int highestScore = -1;
        for (Map.Entry<String, Integer> entry : match.getPlayerScores().entrySet()) {
            if (entry.getValue() > highestScore) {
                highestScore = entry.getValue();
                winnerId = entry.getKey();
            }
        }
        
        // Create final variable for lambda expression
        final String finalWinnerId = winnerId;
        final int finalHighestScore = highestScore;
        
        match.setWinnerId(winnerId);
        
        // Update match in Firestore
        db.collection("quiz_matches")
                .document(matchId)
                .set(match)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "🏆 Match completed! Winner: " + finalWinnerId + " with score: " + finalHighestScore);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to end match", e);
                });
    }
    
    /**
     * Update player's ready status
     */
    public void setPlayerReady(String matchId, boolean ready) {
        String currentUserId = auth.getCurrentUser().getUid();
        
        // Use a transaction to avoid race conditions
        db.runTransaction(transaction -> {
            DocumentSnapshot documentSnapshot = transaction.get(
                db.collection("quiz_matches").document(matchId)
            );
            
            if (!documentSnapshot.exists()) {
                throw new RuntimeException("Match not found");
            }
            
            QuizMatch match = documentSnapshot.toObject(QuizMatch.class);
            if (match == null) {
                throw new RuntimeException("Failed to parse match");
            }
            
            Log.d(TAG, "=== BEFORE setting ready status ===");
            Log.d(TAG, "Current player: " + currentUserId);
            Log.d(TAG, "Current ready status: " + match.isPlayerReady(currentUserId));
            Log.d(TAG, "All players ready: " + match.areAllPlayersReady());
            Log.d(TAG, "Match status: " + match.getStatus());
            Log.d(TAG, "Players ready map: " + match.getPlayersReady());
            Log.d(TAG, "Total players: " + match.getPlayerIds().size());
            Log.d(TAG, "Player IDs: " + match.getPlayerIds());
            
            // Set this player as ready
            match.setPlayerReady(currentUserId, ready);
            
            Log.d(TAG, "=== AFTER setting ready status ===");
            Log.d(TAG, "Player " + currentUserId + " ready status set to: " + ready);
            Log.d(TAG, "New ready status: " + match.isPlayerReady(currentUserId));
            Log.d(TAG, "All players ready: " + match.areAllPlayersReady());
            Log.d(TAG, "Match status: " + match.getStatus());
            Log.d(TAG, "Players ready map: " + match.getPlayersReady());
            
            // If all players are ready, start the match
            if (match.areAllPlayersReady() && "waiting".equals(match.getStatus())) {
                Log.d(TAG, "🎯 STARTING MATCH! Setting status to active");
                Log.d(TAG, "🎯 Target players: " + match.getTargetPlayerCount() + ", Current players: " + match.getPlayerIds().size());
                Log.d(TAG, "🎯 Questions loaded: " + match.getQuestionIds().size());
                Log.d(TAG, "🎯 Current question ID: " + match.getCurrentQuestionId());
                
                if (match.getQuestionIds().isEmpty()) {
                    Log.e(TAG, "🚨 CRITICAL ERROR: Match is starting with NO QUESTIONS!");
                    Log.e(TAG, "🚨 This will cause the quiz to fail!");
                }
                
                match.setStatus("active");
                match.setStartTime(System.currentTimeMillis());
                match.setQuestionStartTime(System.currentTimeMillis());
                
                // Initialize question state for the first question
                match.resetQuestionState();
                Log.d(TAG, "🎯 Initialized question state for new match");
            } else {
                Log.d(TAG, "❌ Not starting match yet:");
                Log.d(TAG, "  - All players ready: " + match.areAllPlayersReady());
                Log.d(TAG, "  - Match status: " + match.getStatus());
                Log.d(TAG, "  - Target players: " + match.getTargetPlayerCount() + ", Current players: " + match.getPlayerIds().size());
                Log.d(TAG, "  - Questions loaded: " + match.getQuestionIds().size());
                Log.d(TAG, "  - Current question ID: " + match.getCurrentQuestionId());
            }
            
            // Update the match in the transaction
            transaction.set(db.collection("quiz_matches").document(matchId), match);
            
            return match;
        })
        .addOnSuccessListener(match -> {
            Log.d(TAG, "✅ Match updated successfully via transaction");
            Log.d(TAG, "✅ Final match state:");
            Log.d(TAG, "✅ - Status: " + match.getStatus());
            Log.d(TAG, "✅ - Questions loaded: " + match.getQuestionIds().size());
            Log.d(TAG, "✅ - Current question ID: " + match.getCurrentQuestionId());
            Log.d(TAG, "✅ - Players ready: " + match.getPlayersReady());
        })
        .addOnFailureListener(e -> {
            Log.e(TAG, "Failed to update match via transaction", e);
        });
    }
    
    // Helper methods and interfaces
    private void updateQueueStatus(String queueId, String status) {
        db.collection("matchmaking_queue")
                .document(queueId)
                .update("status", status);
    }
    
    private void startMatchmakingTimeout(String queueId, MatchmakingCallback callback) {
        // Use a handler to timeout after MAX_WAIT_TIME
        new android.os.Handler().postDelayed(() -> {
            // Check if still in queue
            db.collection("matchmaking_queue")
                    .document(queueId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            MatchmakingQueue queue = doc.toObject(MatchmakingQueue.class);
                            if (queue != null && "waiting".equals(queue.getStatus())) {
                                // Remove from queue and notify timeout
                                updateQueueStatus(queueId, "expired");
                                callback.onMatchmakingTimeout();
                            }
                        }
                    });
        }, MAX_WAIT_TIME);
    }
    
    public void cleanup() {
        Log.d(TAG, "🧹 Cleaning up QuizMatchService listeners");
        
        if (matchListener != null) {
            Log.d(TAG, "🧹 Removing match listener");
            matchListener.remove();
            matchListener = null;
        }
        
        if (queueListener != null) {
            Log.d(TAG, "🧹 Removing queue listener");
            queueListener.remove();
            queueListener = null;
        }
        
        Log.d(TAG, "🧹 Cleanup completed");
    }
    
    /**
     * Safely detach a specific listener
     */
    public void detachMatchListener() {
        if (matchListener != null) {
            Log.d(TAG, "🧹 Detaching match listener");
            matchListener.remove();
            matchListener = null;
        }
    }
    
    /**
     * Safely detach queue listener
     */
    public void detachQueueListener() {
        if (queueListener != null) {
            Log.d(TAG, "🧹 Detaching queue listener");
            queueListener.remove();
            queueListener = null;
        }
    }
    
    /**
     * Refresh match status by re-fetching from database
     */
    public void refreshMatchStatus(String matchId) {
        Log.d(TAG, "🔄 Refreshing match status for: " + matchId);
        
        db.collection("quiz_matches")
                .document(matchId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        QuizMatch match = documentSnapshot.toObject(QuizMatch.class);
                        if (match != null) {
                            Log.d(TAG, "🔄 Match status refreshed successfully");
                            // The listener will automatically update the UI
                        }
                    } else {
                        Log.w(TAG, "⚠️ Match not found during refresh: " + matchId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to refresh match status", e);
                });
    }
    
    // Callback interfaces
    public interface QuestionCallback {
        void onQuestionsRetrieved(List<String> questionIds);
    }
    
    public interface AnswerCallback {
        void onAnswerResult(boolean correct, String message);
    }
}
