package com.example.nurse_connect.data;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.nurse_connect.models.ChatRoom;
import com.example.nurse_connect.models.Message;
import com.example.nurse_connect.models.StudyTask;
import com.example.nurse_connect.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRepository {
    private static final String TAG = "ChatRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    
    // Collection references
    private final CollectionReference chatRoomsRef = db.collection("chatRooms");
    private final CollectionReference messagesRef = db.collection("messages");
    private final CollectionReference studyTasksRef = db.collection("studyTasks");
    private final CollectionReference usersRef = db.collection("users");
    
    // LiveData for real-time updates
    private final MutableLiveData<List<ChatRoom>> userChatRooms = new MutableLiveData<>();
    private final MutableLiveData<List<Message>> chatMessages = new MutableLiveData<>();
    private final MutableLiveData<List<StudyTask>> groupTasks = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Get current user ID
    private String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    // Chat Room Operations
    public LiveData<List<ChatRoom>> getUserChatRooms() {
        loadUserChatRooms();
        return userChatRooms;
    }

    public void loadUserChatRooms() {
        String userId = getCurrentUserId();
        if (userId == null) {
            errorMessage.setValue("User not authenticated");
            return;
        }

        isLoading.setValue(true);
        
        // Query chat rooms where user is a member
        chatRoomsRef.whereArrayContains("memberIds", userId)
                .orderBy("lastMessageAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    isLoading.setValue(false);
                    if (error != null) {
                        errorMessage.setValue("Error loading chat rooms: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        List<ChatRoom> chatRooms = new ArrayList<>();
                        for (DocumentSnapshot document : value.getDocuments()) {
                            ChatRoom chatRoom = document.toObject(ChatRoom.class);
                            if (chatRoom != null) {
                                chatRoom.setRoomId(document.getId());
                                chatRooms.add(chatRoom);
                            }
                        }
                        userChatRooms.setValue(chatRooms);
                    }
                });
    }

    public void createDirectMessage(String recipientId, OnCompleteListener<DocumentReference> listener) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            errorMessage.setValue("User not authenticated");
            return;
        }

        // Check if direct message already exists
        chatRoomsRef.whereEqualTo("type", ChatRoom.ChatRoomType.DIRECT)
                .whereArrayContains("memberIds", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean exists = false;
                        for (DocumentSnapshot document : task.getResult()) {
                            ChatRoom room = document.toObject(ChatRoom.class);
                            if (room != null && room.getMemberIds().contains(recipientId)) {
                                exists = true;
                                break;
                            }
                        }
                        
                        if (!exists) {
                            createNewDirectMessage(currentUserId, recipientId, listener);
                        }
                    }
                });
    }

    private void createNewDirectMessage(String currentUserId, String recipientId, OnCompleteListener<DocumentReference> listener) {
        // Get user details for room name
        usersRef.document(recipientId).get().addOnSuccessListener(documentSnapshot -> {
            User recipient = documentSnapshot.toObject(User.class);
            String roomName = recipient != null ? recipient.getDisplayName() : "Unknown User";
            
            ChatRoom chatRoom = new ChatRoom();
            chatRoom.setType(ChatRoom.ChatRoomType.DIRECT);
            chatRoom.setName(roomName);
            chatRoom.setCreatedBy(currentUserId);
            
            List<String> memberIds = new ArrayList<>();
            memberIds.add(currentUserId);
            memberIds.add(recipientId);
            chatRoom.setMemberIds(memberIds);
            
            chatRoomsRef.add(chatRoom).addOnCompleteListener(listener);
        });
    }

    public void createStudyGroup(ChatRoom studyGroup, OnCompleteListener<DocumentReference> listener) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            errorMessage.setValue("User not authenticated");
            return;
        }

        studyGroup.setCreatedBy(currentUserId);
        studyGroup.setType(ChatRoom.ChatRoomType.STUDY_GROUP);
        
        // Add creator as admin
        if (studyGroup.getMemberIds() == null) {
            studyGroup.setMemberIds(new ArrayList<>());
        }
        studyGroup.getMemberIds().add(currentUserId);
        
        Map<String, ChatRoom.MemberRole> memberRoles = new HashMap<>();
        memberRoles.put(currentUserId, ChatRoom.MemberRole.OWNER);
        studyGroup.setMemberRoles(memberRoles);
        
        chatRoomsRef.add(studyGroup).addOnCompleteListener(listener);
    }

    // Message Operations
    public LiveData<List<Message>> getChatMessages(String roomId) {
        loadChatMessages(roomId);
        return chatMessages;
    }

    public void loadChatMessages(String roomId) {
        isLoading.setValue(true);
        
        messagesRef.whereEqualTo("roomId", roomId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    isLoading.setValue(false);
                    if (error != null) {
                        errorMessage.setValue("Error loading messages: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        List<Message> messages = new ArrayList<>();
                        for (DocumentSnapshot document : value.getDocuments()) {
                            Message message = document.toObject(Message.class);
                            if (message != null) {
                                message.setMessageId(document.getId());
                                messages.add(message);
                            }
                        }
                        chatMessages.setValue(messages);
                    }
                });
    }

    public void sendMessage(Message message, OnCompleteListener<DocumentReference> listener) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            errorMessage.setValue("User not authenticated");
            return;
        }

        message.setSenderId(currentUserId);
        message.setStatus(Message.MessageStatus.SENDING);
        
        // Get user details
        usersRef.document(currentUserId).get().addOnSuccessListener(documentSnapshot -> {
            User user = documentSnapshot.toObject(User.class);
            if (user != null) {
                message.setSenderName(user.getDisplayName());
                message.setSenderPhotoUrl(user.getPhotoURL());
            }
            
            messagesRef.add(message).addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    // Update last message in chat room
                    updateChatRoomLastMessage(message.getRoomId(), message);
                    
                    // Update message status to sent
                    task.getResult().update("status", Message.MessageStatus.SENT);
                }
                if (listener != null) {
                    listener.onComplete(task);
                }
            });
        });
    }

    private void updateChatRoomLastMessage(String roomId, Message message) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", message);
        updates.put("lastMessageAt", message.getCreatedAt());
        
        chatRoomsRef.document(roomId).update(updates);
    }

    public void markMessageAsRead(String messageId) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("readAt", com.google.firebase.Timestamp.now());
        updates.put("status", Message.MessageStatus.READ);
        
        messagesRef.document(messageId).update(updates);
    }

    // Study Task Operations
    public LiveData<List<StudyTask>> getGroupTasks(String groupId) {
        loadGroupTasks(groupId);
        return groupTasks;
    }

    public void loadGroupTasks(String groupId) {
        isLoading.setValue(true);
        
        studyTasksRef.whereEqualTo("groupId", groupId)
                .orderBy("scheduledAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    isLoading.setValue(false);
                    if (error != null) {
                        errorMessage.setValue("Error loading tasks: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        List<StudyTask> tasks = new ArrayList<>();
                        for (DocumentSnapshot document : value.getDocuments()) {
                            StudyTask task = document.toObject(StudyTask.class);
                            if (task != null) {
                                task.setTaskId(document.getId());
                                tasks.add(task);
                            }
                        }
                        groupTasks.setValue(tasks);
                    }
                });
    }

    public void createStudyTask(StudyTask task, OnCompleteListener<DocumentReference> listener) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            errorMessage.setValue("User not authenticated");
            return;
        }

        task.setCreatedBy(currentUserId);
        studyTasksRef.add(task).addOnCompleteListener(listener);
    }

    public void submitTaskAnswer(String taskId, StudyTask.TaskSubmission submission, OnCompleteListener<Void> listener) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            errorMessage.setValue("User not authenticated");
            return;
        }

        submission.setUserId(currentUserId);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("submissions." + currentUserId, submission);
        
        studyTasksRef.document(taskId).update(updates).addOnCompleteListener(listener);
    }

    // Global Chat Operations
    public void loadGlobalChannels() {
        chatRoomsRef.whereEqualTo("type", ChatRoom.ChatRoomType.GLOBAL_CHANNEL)
                .whereEqualTo("isActive", true)
                .orderBy("name")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<ChatRoom> channels = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            ChatRoom channel = document.toObject(ChatRoom.class);
                            if (channel != null) {
                                channel.setRoomId(document.getId());
                                channels.add(channel);
                            }
                        }
                        // You can create a separate LiveData for global channels if needed
                    }
                });
    }

    // Utility methods
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void clearError() {
        errorMessage.setValue(null);
    }
} 