package com.example.nurse_connect.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.nurse_connect.data.CommentRepository;
import com.example.nurse_connect.models.Comment;

import java.util.ArrayList;
import java.util.List;

public class CommentViewModel extends ViewModel {
    
    private CommentRepository repository;
    private MutableLiveData<List<Comment>> comments;
    private MutableLiveData<List<Comment>> replies;
    private MutableLiveData<Boolean> isLoading;
    private MutableLiveData<String> errorMessage;
    private MutableLiveData<String> commentAdded;
    private MutableLiveData<String> replyAdded;
    
    public CommentViewModel() {
        repository = new CommentRepository();
        comments = new MutableLiveData<>(new ArrayList<>());
        replies = new MutableLiveData<>(new ArrayList<>());
        isLoading = new MutableLiveData<>(false);
        errorMessage = new MutableLiveData<>();
        commentAdded = new MutableLiveData<>();
        replyAdded = new MutableLiveData<>();
    }
    
    public LiveData<List<Comment>> getComments() {
        return comments;
    }
    
    public LiveData<List<Comment>> getReplies() {
        return replies;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<String> getCommentAdded() {
        return commentAdded;
    }
    
    public LiveData<String> getReplyAdded() {
        return replyAdded;
    }
    
    public void loadComments(String materialId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.loadComments(materialId, new CommentRepository.CommentsCallback() {
            @Override
            public void onSuccess(List<Comment> commentsList) {
                comments.setValue(commentsList);
                isLoading.setValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue("Failed to load comments: " + e.getMessage());
                isLoading.setValue(false);
            }
        });
    }
    
    public void loadReplies(String parentCommentId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.loadReplies(parentCommentId, new CommentRepository.RepliesCallback() {
            @Override
            public void onSuccess(List<Comment> repliesList) {
                replies.setValue(repliesList);
                isLoading.setValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue("Failed to load replies: " + e.getMessage());
                isLoading.setValue(false);
            }
        });
    }
    
    public void addComment(String materialId, String commentText) {
        if (commentText == null || commentText.trim().isEmpty()) {
            errorMessage.setValue("Comment cannot be empty");
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.addComment(materialId, commentText.trim(), new CommentRepository.AddCommentCallback() {
            @Override
            public void onSuccess() {
                // Notify that comment was added successfully
                commentAdded.setValue(materialId);
                // Reload comments to get the updated list
                loadComments(materialId);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue("Failed to add comment: " + e.getMessage());
                isLoading.setValue(false);
            }
        });
    }
    
    public void addReply(String materialId, String parentCommentId, String replyText) {
        if (replyText == null || replyText.trim().isEmpty()) {
            errorMessage.setValue("Reply cannot be empty");
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.addReply(materialId, parentCommentId, replyText.trim(), new CommentRepository.AddReplyCallback() {
            @Override
            public void onSuccess() {
                // Notify that reply was added successfully
                replyAdded.setValue(parentCommentId);
                // Reload comments to get the updated list with new reply counts
                loadComments(materialId);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue("Failed to add reply: " + e.getMessage());
                isLoading.setValue(false);
            }
        });
    }
    
    public void clearError() {
        errorMessage.setValue(null);
    }
    
    public void clearCommentAdded() {
        commentAdded.setValue(null);
    }
    
    public void clearReplyAdded() {
        replyAdded.setValue(null);
    }
    
    public void deleteComment(String commentId, String materialId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.deleteComment(commentId, materialId, new CommentRepository.DeleteCommentCallback() {
            @Override
            public void onSuccess() {
                // Reload comments to reflect the deletion
                loadComments(materialId);
                isLoading.setValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue("Failed to delete comment: " + e.getMessage());
                isLoading.setValue(false);
            }
        });
    }
    
    public String getCurrentUserId() {
        return repository.getCurrentUserId();
    }
} 