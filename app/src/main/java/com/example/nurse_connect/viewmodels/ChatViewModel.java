package com.example.nurse_connect.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.nurse_connect.data.ChatRepository;
import com.example.nurse_connect.models.ChatRoom;
import com.example.nurse_connect.models.Message;
import com.example.nurse_connect.models.StudyTask;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;

import java.util.List;

public class ChatViewModel extends ViewModel {
    private final ChatRepository chatRepository = new ChatRepository();
    
    // LiveData for UI
    private final MutableLiveData<List<ChatRoom>> directMessages = new MutableLiveData<>();
    private final MutableLiveData<List<ChatRoom>> studyGroups = new MutableLiveData<>();
    private final MutableLiveData<List<ChatRoom>> globalChannels = new MutableLiveData<>();
    private final MutableLiveData<List<Message>> currentChatMessages = new MutableLiveData<>();
    private final MutableLiveData<List<StudyTask>> currentGroupTasks = new MutableLiveData<>();
    private final MutableLiveData<ChatRoom> selectedChatRoom = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSendingMessage = new MutableLiveData<>();
    private final MutableLiveData<String> sendMessageError = new MutableLiveData<>();

    // Get repository LiveData
    public LiveData<Boolean> getIsLoading() {
        return chatRepository.getIsLoading();
    }

    public LiveData<String> getErrorMessage() {
        return chatRepository.getErrorMessage();
    }

    // Direct Messages
    public LiveData<List<ChatRoom>> getDirectMessages() {
        loadDirectMessages();
        return directMessages;
    }

    public void loadDirectMessages() {
        chatRepository.getUserChatRooms().observeForever(chatRooms -> {
            if (chatRooms != null) {
                List<ChatRoom> directChats = chatRooms.stream()
                        .filter(room -> room.getType() == ChatRoom.ChatRoomType.DIRECT)
                        .collect(java.util.stream.Collectors.toList());
                directMessages.setValue(directChats);
            }
        });
    }

    public void startDirectMessage(String recipientId) {
        chatRepository.createDirectMessage(recipientId, new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(Task<DocumentReference> task) {
                if (task.isSuccessful()) {
                    // Navigate to chat or refresh list
                    loadDirectMessages();
                }
            }
        });
    }

    // Study Groups
    public LiveData<List<ChatRoom>> getStudyGroups() {
        loadStudyGroups();
        return studyGroups;
    }

    public void loadStudyGroups() {
        chatRepository.getUserChatRooms().observeForever(chatRooms -> {
            if (chatRooms != null) {
                List<ChatRoom> groupChats = chatRooms.stream()
                        .filter(room -> room.getType() == ChatRoom.ChatRoomType.STUDY_GROUP)
                        .collect(java.util.stream.Collectors.toList());
                studyGroups.setValue(groupChats);
            }
        });
    }

    public void createStudyGroup(ChatRoom studyGroup) {
        chatRepository.createStudyGroup(studyGroup, new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(Task<DocumentReference> task) {
                if (task.isSuccessful()) {
                    loadStudyGroups();
                }
            }
        });
    }

    // Global Channels
    public LiveData<List<ChatRoom>> getGlobalChannels() {
        loadGlobalChannels();
        return globalChannels;
    }

    public void loadGlobalChannels() {
        // For now, we'll create some default channels
        // In a real app, these would come from the database
        List<ChatRoom> channels = new java.util.ArrayList<>();
        
        ChatRoom generalChannel = new ChatRoom("General Discussion", ChatRoom.ChatRoomType.GLOBAL_CHANNEL);
        generalChannel.setDescription("General nursing discussions and questions");
        generalChannel.setChannelCategory("General");
        generalChannel.setOfficial(true);
        channels.add(generalChannel);
        
        ChatRoom studyHelpChannel = new ChatRoom("Study Help", ChatRoom.ChatRoomType.GLOBAL_CHANNEL);
        studyHelpChannel.setDescription("Get help with nursing studies and assignments");
        studyHelpChannel.setChannelCategory("Academic");
        studyHelpChannel.setOfficial(true);
        channels.add(studyHelpChannel);
        
        ChatRoom qaChannel = new ChatRoom("Q&A Forum", ChatRoom.ChatRoomType.GLOBAL_CHANNEL);
        qaChannel.setDescription("Ask and answer nursing-related questions");
        qaChannel.setChannelCategory("Academic");
        qaChannel.setOfficial(true);
        channels.add(qaChannel);
        
        globalChannels.setValue(channels);
    }

    // Chat Messages
    public LiveData<List<Message>> getChatMessages(String roomId) {
        chatRepository.getChatMessages(roomId).observeForever(messages -> {
            currentChatMessages.setValue(messages);
        });
        return currentChatMessages;
    }

    public void sendMessage(String roomId, String content) {
        isSendingMessage.setValue(true);
        sendMessageError.setValue(null);
        
        Message message = new Message();
        message.setRoomId(roomId);
        message.setContent(content);
        message.setType(Message.MessageType.TEXT);
        
        chatRepository.sendMessage(message, new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(Task<DocumentReference> task) {
                isSendingMessage.setValue(false);
                if (!task.isSuccessful()) {
                    sendMessageError.setValue("Failed to send message: " + task.getException().getMessage());
                }
            }
        });
    }

    public void sendMediaMessage(String roomId, String mediaUrl, String mediaType, String fileName) {
        isSendingMessage.setValue(true);
        sendMessageError.setValue(null);
        
        Message message = new Message();
        message.setRoomId(roomId);
        message.setMediaUrl(mediaUrl);
        message.setMediaType(mediaType);
        message.setFileName(fileName);
        
        // Set message type based on media type
        switch (mediaType.toLowerCase()) {
            case "image":
                message.setType(Message.MessageType.IMAGE);
                break;
            case "video":
                message.setType(Message.MessageType.VIDEO);
                break;
            case "pdf":
                message.setType(Message.MessageType.PDF);
                break;
            default:
                message.setType(Message.MessageType.DOCUMENT);
                break;
        }
        
        chatRepository.sendMessage(message, new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(Task<DocumentReference> task) {
                isSendingMessage.setValue(false);
                if (!task.isSuccessful()) {
                    sendMessageError.setValue("Failed to send media: " + task.getException().getMessage());
                }
            }
        });
    }

    public void markMessageAsRead(String messageId) {
        chatRepository.markMessageAsRead(messageId);
    }

    // Study Tasks
    public LiveData<List<StudyTask>> getGroupTasks(String groupId) {
        chatRepository.getGroupTasks(groupId).observeForever(tasks -> {
            currentGroupTasks.setValue(tasks);
        });
        return currentGroupTasks;
    }

    public void createStudyTask(StudyTask task) {
        chatRepository.createStudyTask(task, new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(Task<DocumentReference> task) {
                if (task.isSuccessful()) {
                    // Refresh tasks list
                    getGroupTasks(task.getResult().getId());
                }
            }
        });
    }

    public void submitTaskAnswer(String taskId, StudyTask.TaskSubmission submission) {
        chatRepository.submitTaskAnswer(taskId, submission, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(Task<Void> task) {
                if (!task.isSuccessful()) {
                    // Handle error
                }
            }
        });
    }

    // Selected Chat Room
    public LiveData<ChatRoom> getSelectedChatRoom() {
        return selectedChatRoom;
    }

    public void setSelectedChatRoom(ChatRoom chatRoom) {
        selectedChatRoom.setValue(chatRoom);
    }

    // Message sending status
    public LiveData<Boolean> getIsSendingMessage() {
        return isSendingMessage;
    }

    public LiveData<String> getSendMessageError() {
        return sendMessageError;
    }

    // Utility methods
    public void clearError() {
        chatRepository.clearError();
        sendMessageError.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up any resources if needed
    }
} 