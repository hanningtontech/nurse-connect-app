package com.example.nurse_connect.services;

import android.util.Log;

import com.example.nurse_connect.models.GroupInvitation;
import com.example.nurse_connect.models.Message;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupInvitationService {
    
    private static final String TAG = "GroupInvitationService";
    private FirebaseFirestore db;

    public GroupInvitationService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void sendGroupInvitationMessage(GroupInvitation invitation, String currentUserId) {
        // Find or create private chat between inviter and invitee
        String chatId = generateChatId(currentUserId, invitation.getInvitedUser());
        
        // First, ensure private chat exists
        ensurePrivateChatExists(chatId, currentUserId, invitation.getInvitedUser(), invitation);
    }

    private void ensurePrivateChatExists(String chatId, String senderId, String receiverId, GroupInvitation invitation) {
        // Check if private chat exists
        db.collection("private_chats")
                .document(chatId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Chat exists, send invitation message
                        sendInvitationMessage(chatId, senderId, invitation);
                    } else {
                        // Create new private chat first, then send invitation
                        createPrivateChatAndSendInvitation(chatId, senderId, receiverId, invitation);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking private chat", e);
                });
    }

    private void sendInvitationMessage(String chatId, String senderId, GroupInvitation invitation) {
        // Create invitation message using the same structure as regular messages
        Message invitationMessage = new Message();
        invitationMessage.setSenderId(senderId);
        invitationMessage.setSenderName(invitation.getInvitedByName());
        invitationMessage.setContent("📨 Group Invitation: " + invitation.getGroupTitle());
        invitationMessage.setCreatedAt(com.google.firebase.Timestamp.now());
        invitationMessage.setType(Message.MessageType.SYSTEM);
        invitationMessage.setStatus(Message.MessageStatus.SENT);

        // Set invitation data
        Map<String, Object> invitationData = new HashMap<>();
        invitationData.put("invitationId", invitation.getInvitationId());
        invitationData.put("groupId", invitation.getGroupId());
        invitationData.put("groupTitle", invitation.getGroupTitle());
        invitationData.put("groupDescription", invitation.getGroupDescription());
        invitationData.put("isGroupPublic", invitation.isGroupPublic());
        invitationData.put("invitedBy", invitation.getInvitedBy());
        invitationData.put("invitedByName", invitation.getInvitedByName());
        invitationData.put("status", "pending");

        invitationMessage.setInvitationData(invitationData);
        invitationMessage.setMessageType("group_invitation");

        // Save message to the private chat's messages subcollection
        db.collection("private_chats")
                .document(chatId)
                .collection("messages")
                .add(invitationMessage)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Invitation message saved to private chat: " + chatId);

                    // Update chat metadata
                    updateChatWithInvitation(chatId, invitation.getInvitedUser(), invitationMessage);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving invitation message", e);
                });
    }

    private void updateChatWithInvitation(String chatId, String receiverId, Message invitationMessage) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", invitationMessage.getContent());
        updates.put("lastMessageSenderId", invitationMessage.getSenderId());
        updates.put("lastMessageTime", new Date());
        updates.put("updatedAt", new Date());

        // Increment unread count for receiver
        updates.put("unreadCounts." + receiverId, com.google.firebase.firestore.FieldValue.increment(1));

        db.collection("private_chats")
                .document(chatId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Private chat updated with invitation");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating private chat", e);
                });
    }

    private void createPrivateChatAndSendInvitation(String chatId, String senderId, String receiverId, GroupInvitation invitation) {
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("chatId", chatId);
        chatData.put("participants", List.of(senderId, receiverId));
        chatData.put("createdAt", new Date());
        chatData.put("updatedAt", new Date());
        chatData.put("lastMessage", "📨 Group Invitation: " + invitation.getGroupTitle());
        chatData.put("lastMessageSenderId", senderId);
        chatData.put("lastMessageTime", new Date());

        // Initialize unread counts
        Map<String, Object> unreadCounts = new HashMap<>();
        unreadCounts.put(senderId, 0);
        unreadCounts.put(receiverId, 1); // New invitation for receiver
        chatData.put("unreadCounts", unreadCounts);

        db.collection("private_chats")
                .document(chatId)
                .set(chatData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "New private chat created, now sending invitation message");
                    // Now send the invitation message
                    sendInvitationMessage(chatId, senderId, invitation);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating private chat", e);
                });
    }

    public void handleInvitationResponse(String invitationId, String groupId, String userId, boolean accepted) {
        // Update invitation status
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", accepted ? "accepted" : "declined");

        db.collection("group_invitations")
                .document(invitationId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (accepted) {
                        // Add user to group
                        addUserToGroup(groupId, userId);
                    }

                    // Remove from pending invitations
                    removeFromPendingInvitations(groupId, userId);

                    // Update the invitation message status in private chat
                    updateInvitationMessageStatus(invitationId, userId, accepted ? "accepted" : "declined");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating invitation status", e);
                });
    }

    public void handleInvitationResponseAndCleanup(String invitationId, String groupId, String userId, boolean accepted, com.example.nurse_connect.models.Message invitationMessage) {
        // Update invitation status
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", accepted ? "accepted" : "declined");

        db.collection("group_invitations")
                .document(invitationId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (accepted) {
                        // Add user to group
                        addUserToGroup(groupId, userId);

                        // Replace invitation message with a simple "Joined group" message
                        replaceInvitationWithJoinMessage(invitationMessage, groupId);
                    } else {
                        // Replace invitation message with a simple "Declined invitation" message
                        replaceInvitationWithDeclineMessage(invitationMessage, groupId);
                    }

                    // Remove from pending invitations
                    removeFromPendingInvitations(groupId, userId);

                    // Delete the invitation document
                    deleteInvitation(invitationId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating invitation status", e);
                });
    }

    private void updateInvitationMessageStatus(String invitationId, String userId, String status) {
        // Find and update the invitation message in private chats
        // This is a bit complex as we need to find the message across all private chats
        // For now, we'll rely on the UI update in the ViewHolder
        Log.d(TAG, "Invitation status updated to: " + status + " for invitation: " + invitationId);
    }

    private void addUserToGroup(String groupId, String userId) {
        // Add user to group members
        db.collection("group_chats")
                .document(groupId)
                .update("members", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                .addOnSuccessListener(aVoid -> {
                    // Initialize unread count for new member
                    db.collection("group_chats")
                            .document(groupId)
                            .update("unreadCounts." + userId, 0)
                            .addOnSuccessListener(aVoid2 -> {
                                // Send welcome message to group
                                sendWelcomeMessage(groupId, userId);
                            });

                    Log.d(TAG, "User added to group successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding user to group", e);
                });
    }

    private void sendWelcomeMessage(String groupId, String userId) {
        // Get user's name first
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String userName = "New member";
                    if (documentSnapshot.exists()) {
                        String displayName = documentSnapshot.getString("displayName");
                        String username = documentSnapshot.getString("username");
                        userName = displayName != null ? displayName : (username != null ? username : "New member");
                    }

                    // Make userName effectively final for lambda
                    final String finalUserName = userName;

                    // Create welcome message
                    Map<String, Object> welcomeMessage = new HashMap<>();
                    welcomeMessage.put("senderId", "system");
                    welcomeMessage.put("senderName", "System");
                    welcomeMessage.put("groupId", groupId);
                    welcomeMessage.put("content", "🎉 " + finalUserName + " joined the group!");
                    welcomeMessage.put("timestamp", new Date());
                    welcomeMessage.put("messageType", "system");

                    // Send welcome message
                    db.collection("group_messages")
                            .add(welcomeMessage)
                            .addOnSuccessListener(documentReference -> {
                                Log.d(TAG, "Welcome message sent for user: " + finalUserName);

                                // Update group's last message and increment unread counts
                                updateGroupLastMessageForWelcome(groupId, "🎉 " + finalUserName + " joined the group!", userId);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error sending welcome message", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user info for welcome message", e);
                });
    }

    private void updateGroupLastMessageForWelcome(String groupId, String messageText, String newMemberId) {
        // Get group data to access member list
        db.collection("group_chats")
                .document(groupId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> groupData = documentSnapshot.getData();
                        if (groupData != null && groupData.containsKey("members")) {
                            @SuppressWarnings("unchecked")
                            List<String> members = (List<String>) groupData.get("members");

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("lastMessage", messageText);
                            updates.put("lastMessageSenderId", "system");
                            updates.put("lastMessageTime", new Date());
                            updates.put("updatedAt", new Date());

                            // Increment unread count for all members except the new member
                            if (members != null) {
                                for (String memberId : members) {
                                    if (!memberId.equals(newMemberId)) {
                                        updates.put("unreadCounts." + memberId, com.google.firebase.firestore.FieldValue.increment(1));
                                    }
                                }
                            }

                            // Update the group document
                            db.collection("group_chats")
                                    .document(groupId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Updated group last message and unread counts for welcome message");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error updating group for welcome message", e);
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting group data for welcome message update", e);
                });
    }

    private void replaceInvitationWithJoinMessage(com.example.nurse_connect.models.Message invitationMessage, String groupId) {
        // Get group title first
        db.collection("group_chats")
                .document(groupId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String groupTitle = "the group";
                    if (documentSnapshot.exists()) {
                        groupTitle = documentSnapshot.getString("title");
                        if (groupTitle == null) groupTitle = "the group";
                    }

                    // Find the private chat and message to update
                    findAndUpdateInvitationMessage(invitationMessage, "✅ Joined " + groupTitle, "text");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting group title for join message", e);
                    findAndUpdateInvitationMessage(invitationMessage, "✅ Joined the group", "text");
                });
    }

    private void replaceInvitationWithDeclineMessage(com.example.nurse_connect.models.Message invitationMessage, String groupId) {
        // Get group title first
        db.collection("group_chats")
                .document(groupId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String groupTitle = "the group";
                    if (documentSnapshot.exists()) {
                        groupTitle = documentSnapshot.getString("title");
                        if (groupTitle == null) groupTitle = "the group";
                    }

                    // Find the private chat and message to update
                    findAndUpdateInvitationMessage(invitationMessage, "❌ Declined invitation to " + groupTitle, "text");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting group title for decline message", e);
                    findAndUpdateInvitationMessage(invitationMessage, "❌ Declined group invitation", "text");
                });
    }

    private void findAndUpdateInvitationMessage(com.example.nurse_connect.models.Message invitationMessage, String newContent, String newType) {
        // We need to find the private chat that contains this invitation message
        // Since we don't have the chat ID directly, we'll search for it
        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("private_chats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot chatDoc : queryDocumentSnapshots) {
                        String chatId = chatDoc.getId();

                        // Search for the invitation message in this chat
                        db.collection("private_chats")
                                .document(chatId)
                                .collection("messages")
                                .whereEqualTo("messageType", "group_invitation")
                                .get()
                                .addOnSuccessListener(messageSnapshots -> {
                                    for (com.google.firebase.firestore.QueryDocumentSnapshot messageDoc : messageSnapshots) {
                                        com.example.nurse_connect.models.Message msg = messageDoc.toObject(com.example.nurse_connect.models.Message.class);

                                        // Check if this is our invitation message by comparing invitation data
                                        if (msg.getInvitationData() != null && invitationMessage.getInvitationData() != null) {
                                            String msgInvitationId = (String) msg.getInvitationData().get("invitationId");
                                            String targetInvitationId = (String) invitationMessage.getInvitationData().get("invitationId");

                                            if (msgInvitationId != null && msgInvitationId.equals(targetInvitationId)) {
                                                // Found the message, update it
                                                updateMessageToRegularText(chatId, messageDoc.getId(), newContent, newType);
                                                return;
                                            }
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding invitation message to update", e);
                });
    }

    private void updateMessageToRegularText(String chatId, String messageId, String newContent, String newType) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("content", newContent);
        updates.put("messageType", newType);
        updates.put("invitationData", null); // Remove invitation data
        updates.put("type", com.example.nurse_connect.models.Message.MessageType.TEXT);

        db.collection("private_chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully converted invitation message to regular text");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating invitation message", e);
                });
    }

    private void deleteInvitation(String invitationId) {
        db.collection("group_invitations")
                .document(invitationId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Invitation document deleted: " + invitationId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting invitation document", e);
                });
    }

    private void removeFromPendingInvitations(String groupId, String userId) {
        db.collection("group_chats")
                .document(groupId)
                .update("pendingInvitations", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User removed from pending invitations");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing from pending invitations", e);
                });
    }

    private String generateChatId(String userId1, String userId2) {
        // Create consistent chat ID regardless of user order
        return userId1.compareTo(userId2) < 0 ? 
                userId1 + "_" + userId2 : 
                userId2 + "_" + userId1;
    }
}
