package com.example.nurse_connect.services;

import android.util.Log;

import com.example.nurse_connect.models.NotificationItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationService {
    private static final String TAG = "NotificationService";
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    public interface NotificationCallback {
        void onSuccess(List<NotificationItem> notifications, int unreadCount);
        void onFailure(Exception e);
    }

    public interface NotificationCountCallback {
        void onSuccess(int unreadCount);
        void onFailure(Exception e);
    }

    public NotificationService() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public void getNotifications(NotificationCallback callback) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        
        if (currentUserId == null) {
            callback.onSuccess(new ArrayList<>(), 0);
            return;
        }

        Log.d(TAG, "Loading notifications for user: " + currentUserId);

        firestore.collection("notifications")
                .whereEqualTo("recipientId", currentUserId)
                .whereEqualTo("isRead", false)  // Only show unread notifications
                .limit(50)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Notifications query successful, found " + querySnapshot.size() + " documents");
                    List<NotificationItem> notifications = new ArrayList<>();
                    int unreadCount = 0;
                    
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            NotificationItem notification = doc.toObject(NotificationItem.class);
                            if (notification != null) {
                                notification.setId(doc.getId());

                                // Handle compatibility with existing chat notifications
                                if (notification.getFromUserName() == null && notification.getSenderName() != null) {
                                    notification.setFromUserName(notification.getSenderName());
                                }
                                if (notification.getFromUserId() == null && notification.getSenderId() != null) {
                                    notification.setFromUserId(notification.getSenderId());
                                }
                                if (notification.getTargetId() == null && notification.getChatId() != null) {
                                    notification.setTargetId(notification.getChatId());
                                }

                                // Set default values for missing fields
                                if (notification.getType() == null) {
                                    notification.setType("message"); // Default for chat notifications
                                }

                                // Fix title to show sender name prominently
                                String senderName = notification.getFromUserName();
                                if (senderName == null && notification.getSenderName() != null) {
                                    senderName = notification.getSenderName();
                                }

                                String currentTitle = notification.getTitle();

                                // Always prioritize sender name as title for message notifications
                                if ("message".equals(notification.getType()) && senderName != null) {
                                    // Check if current title looks like message content
                                    boolean titleLooksLikeMessage = currentTitle == null ||
                                        currentTitle.length() < 15 || // Short titles are likely message content
                                        !currentTitle.contains(" ") || // No spaces likely means it's message content
                                        currentTitle.equals(notification.getMessage()) || // Title same as message
                                        (currentTitle.length() < 8 && !currentTitle.equals(senderName)); // Very short and not sender name

                                    if (titleLooksLikeMessage) {
                                        notification.setTitle(senderName);
                                        Log.d(TAG, "Fixed malformed title '" + currentTitle + "' to sender name: " + senderName);
                                    }
                                } else if (senderName != null) {
                                    // For non-message notifications, still fix if title is clearly malformed
                                    if (currentTitle == null || (currentTitle.length() < 8 && !currentTitle.contains(" "))) {
                                        notification.setTitle(senderName);
                                    }
                                } else {
                                    if (notification.getTitle() == null) {
                                        notification.setTitle("Unknown User");
                                    }
                                }

                                if (notification.getActionText() == null) {
                                    if ("message".equals(notification.getType())) {
                                        notification.setActionText("View");
                                    } else {
                                        notification.setActionText("View");
                                    }
                                }

                                // Ensure message content doesn't duplicate sender name
                                String message = notification.getMessage();
                                if (message != null && senderName != null) {
                                    // Remove sender name from beginning of message if it exists
                                    String senderPrefix = senderName + ": ";
                                    if (message.startsWith(senderPrefix)) {
                                        message = message.substring(senderPrefix.length());
                                        notification.setMessage("💬 " + message);
                                    } else if (!message.startsWith("💬") && !message.startsWith("👥")) {
                                        // Add message icon if not present
                                        notification.setMessage("💬 " + message);
                                    }
                                }

                                notifications.add(notification);

                                if (!notification.isRead()) {
                                    unreadCount++;
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to parse notification document: " + doc.getId(), e);
                            // Continue with other notifications
                        }
                    }

                    // Sort notifications by timestamp (newest first)
                    notifications.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                    callback.onSuccess(notifications, unreadCount);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load notifications", e);
                    callback.onFailure(e);
                });
    }

    public void getAllNotifications(NotificationCallback callback) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            callback.onSuccess(new ArrayList<>(), 0);
            return;
        }

        Log.d(TAG, "Loading all notifications for user: " + currentUserId);

        firestore.collection("notifications")
                .whereEqualTo("recipientId", currentUserId)
                .limit(50)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "All notifications query successful, found " + querySnapshot.size() + " documents");
                    List<NotificationItem> notifications = new ArrayList<>();
                    int unreadCount = 0;

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            NotificationItem notification = doc.toObject(NotificationItem.class);
                            if (notification != null) {
                                notification.setId(doc.getId());

                                // Handle compatibility with existing chat notifications
                                if (notification.getFromUserName() == null && notification.getSenderName() != null) {
                                    notification.setFromUserName(notification.getSenderName());
                                }
                                if (notification.getFromUserId() == null && notification.getSenderId() != null) {
                                    notification.setFromUserId(notification.getSenderId());
                                }
                                if (notification.getTargetId() == null && notification.getChatId() != null) {
                                    notification.setTargetId(notification.getChatId());
                                }

                                // Set default values for missing fields
                                if (notification.getType() == null) {
                                    notification.setType("message"); // Default for chat notifications
                                }
                                if (notification.getTitle() == null) {
                                    if (notification.getFromUserName() != null) {
                                        notification.setTitle("New message from " + notification.getFromUserName());
                                    } else {
                                        notification.setTitle("New Message");
                                    }
                                }
                                if (notification.getActionText() == null) {
                                    notification.setActionText("Reply");
                                }

                                notifications.add(notification);

                                if (!notification.isRead()) {
                                    unreadCount++;
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to parse notification document: " + doc.getId(), e);
                            // Continue with other notifications
                        }
                    }

                    // Sort notifications by timestamp (newest first)
                    notifications.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                    callback.onSuccess(notifications, unreadCount);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load all notifications", e);
                    callback.onFailure(e);
                });
    }

    public void getUnreadCount(NotificationCountCallback callback) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        
        if (currentUserId == null) {
            callback.onSuccess(0);
            return;
        }

        firestore.collection("notifications")
                .whereEqualTo("recipientId", currentUserId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    callback.onSuccess(querySnapshot.size());
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void markAsRead(String notificationId) {
        if (notificationId == null) return;
        
        firestore.collection("notifications")
                .document(notificationId)
                .update("isRead", true)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification marked as read: " + notificationId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to mark notification as read", e);
                });
    }

    public void markAllAsRead() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        
        if (currentUserId == null) return;

        firestore.collection("notifications")
                .whereEqualTo("recipientId", currentUserId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        doc.getReference().update("isRead", true);
                    }
                    Log.d(TAG, "All notifications marked as read");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to mark all notifications as read", e);
                });
    }

    public void createNotification(String toUserId, String type, String title, String message, 
                                 String fromUserId, String fromUserName, String targetId) {
        if (toUserId == null || toUserId.equals(fromUserId)) return; // Don't notify self
        
        NotificationItem notification = new NotificationItem();
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setFromUserId(fromUserId);
        notification.setFromUserName(fromUserName);
        notification.setTargetId(targetId);
        notification.setTimestamp(System.currentTimeMillis());
        notification.setRead(false);
        
        // Set action text based on type
        switch (type) {
            case "message":
                notification.setActionText("Reply");
                break;
            case "follow":
                notification.setActionText("View Profile");
                break;
            case "task":
                notification.setActionText("View Task");
                break;
            case "suggestion":
                notification.setActionText("View");
                break;
            case "like":
            case "comment":
                notification.setActionText("View Post");
                break;
            default:
                notification.setActionText("View");
                break;
        }

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("recipientId", toUserId);
        notificationData.put("type", notification.getType());
        notificationData.put("title", notification.getTitle());
        notificationData.put("message", notification.getMessage());
        notificationData.put("fromUserId", notification.getFromUserId());
        notificationData.put("fromUserName", notification.getFromUserName());
        notificationData.put("targetId", notification.getTargetId());
        notificationData.put("timestamp", notification.getTimestamp());
        notificationData.put("isRead", notification.isRead());
        notificationData.put("actionText", notification.getActionText());

        firestore.collection("notifications")
                .add(notificationData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Notification created: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create notification", e);
                });
    }

    // Helper methods to create specific notification types
    public void createMessageNotification(String toUserId, String fromUserId, String fromUserName,
                                        String chatId, String messagePreview) {
        // Remove any existing unread notifications for this chat from this user
        removeDuplicateMessageNotifications(chatId, fromUserId);

        // Create the new notification
        createNotification(toUserId, "message", "New Message",
                fromUserName + ": " + messagePreview, fromUserId, fromUserName, chatId);
    }

    public void createFollowNotification(String toUserId, String fromUserId, String fromUserName) {
        createNotification(toUserId, "follow", "New Follower", 
                fromUserName + " started following you", fromUserId, fromUserName, fromUserId);
    }

    public void createTaskNotification(String toUserId, String fromUserId, String fromUserName, 
                                     String taskId, String taskTitle) {
        createNotification(toUserId, "task", "Task Assignment", 
                fromUserName + " assigned you a task: " + taskTitle, fromUserId, fromUserName, taskId);
    }

    public void createSuggestionNotification(String toUserId, String suggestionTitle, String suggestionId) {
        createNotification(toUserId, "suggestion", "New Suggestion", 
                "We have a new suggestion for you: " + suggestionTitle, null, "Nurse Connect", suggestionId);
    }

    public void createLikeNotification(String toUserId, String fromUserId, String fromUserName, 
                                     String postId, String postType) {
        createNotification(toUserId, "like", "New Like", 
                fromUserName + " liked your " + postType, fromUserId, fromUserName, postId);
    }

    public void createCommentNotification(String toUserId, String fromUserId, String fromUserName,
                                        String postId, String commentPreview) {
        createNotification(toUserId, "comment", "New Comment",
                fromUserName + " commented: " + commentPreview, fromUserId, fromUserName, postId);
    }

    // Clean up old notifications to prevent badge count issues
    public void cleanupOldNotifications() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) return;

        // Delete notifications older than 30 days
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);

        firestore.collection("notifications")
                .whereEqualTo("recipientId", currentUserId)
                .whereLessThan("timestamp", thirtyDaysAgo)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        doc.getReference().delete();
                    }
                    Log.d(TAG, "Cleaned up " + querySnapshot.size() + " old notifications");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to cleanup old notifications", e);
                });
    }

    // Clear all sample/test notifications and malformed notifications
    public void clearAllSampleNotifications() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) return;

        firestore.collection("notifications")
                .whereEqualTo("recipientId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int deletedCount = 0;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        // Check if it's a sample notification (has sample user IDs or specific test content)
                        String fromUserId = doc.getString("fromUserId");
                        String fromUserName = doc.getString("fromUserName");
                        String title = doc.getString("title");

                        boolean shouldDelete = false;

                        // Delete sample notifications
                        if (fromUserId != null && (fromUserId.startsWith("sample_user_") ||
                            "Sarah Johnson".equals(fromUserName) ||
                            "Dr. Emily Chen".equals(fromUserName) ||
                            "Prof. Michael Brown".equals(fromUserName) ||
                            "Jessica Martinez".equals(fromUserName) ||
                            "Alex Thompson".equals(fromUserName) ||
                            "Nurse Connect".equals(fromUserName))) {
                            shouldDelete = true;
                        }

                        // Delete malformed notifications where title looks like message content
                        if (title != null && title.length() < 10 && !title.contains(" ") &&
                            fromUserName != null && !title.equals(fromUserName)) {
                            shouldDelete = true;
                            Log.d(TAG, "Deleting malformed notification with title: " + title);
                        }

                        if (shouldDelete) {
                            doc.getReference().delete();
                            deletedCount++;
                        }
                    }
                    Log.d(TAG, "Cleared " + deletedCount + " sample/malformed notifications");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to clear sample notifications", e);
                });
    }

    // Fix all existing malformed notifications in the database
    public void fixMalformedNotifications() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) return;

        firestore.collection("notifications")
                .whereEqualTo("recipientId", currentUserId)
                .whereEqualTo("type", "message")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int fixedCount = 0;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        String title = doc.getString("title");
                        String fromUserName = doc.getString("fromUserName");
                        String senderName = doc.getString("senderName");

                        // Determine the correct sender name
                        String correctSenderName = fromUserName != null ? fromUserName : senderName;

                        if (correctSenderName != null && title != null) {
                            // Check if title looks like message content
                            boolean needsFix = title.length() < 15 ||
                                             !title.contains(" ") ||
                                             (title.length() < 8 && !title.equals(correctSenderName));

                            if (needsFix) {
                                // Update the notification in the database
                                doc.getReference().update("title", correctSenderName)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Fixed notification title from '" + title + "' to '" + correctSenderName + "'");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to fix notification title", e);
                                    });
                                fixedCount++;
                            }
                        }
                    }
                    if (fixedCount > 0) {
                        Log.d(TAG, "Fixed " + fixedCount + " malformed notifications in database");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fix malformed notifications", e);
                });
    }

    // Remove duplicate notifications for the same chat/message
    public void removeDuplicateMessageNotifications(String chatId, String fromUserId) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) return;

        firestore.collection("notifications")
                .whereEqualTo("recipientId", currentUserId)
                .whereEqualTo("type", "message")
                .whereEqualTo("targetId", chatId)
                .whereEqualTo("fromUserId", fromUserId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Keep only the most recent notification, delete the rest
                    if (querySnapshot.size() > 1) {
                        java.util.List<com.google.firebase.firestore.QueryDocumentSnapshot> docs =
                                new java.util.ArrayList<>();
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                            docs.add(doc);
                        }

                        // Sort by timestamp (newest first)
                        docs.sort((a, b) -> {
                            Object timestampA = a.get("timestamp");
                            Object timestampB = b.get("timestamp");

                            long timeA = getTimestampAsLong(timestampA);
                            long timeB = getTimestampAsLong(timestampB);

                            return Long.compare(timeB, timeA);
                        });

                        // Delete all except the first (newest) one
                        for (int i = 1; i < docs.size(); i++) {
                            docs.get(i).getReference().delete();
                        }

                        Log.d(TAG, "Removed " + (docs.size() - 1) + " duplicate notifications");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to remove duplicate notifications", e);
                });
    }

    private long getTimestampAsLong(Object timestamp) {
        if (timestamp instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) timestamp).toDate().getTime();
        } else if (timestamp instanceof Long) {
            return (Long) timestamp;
        } else if (timestamp instanceof Number) {
            return ((Number) timestamp).longValue();
        }
        return System.currentTimeMillis();
    }
}
