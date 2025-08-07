package com.example.nurse_connect.ui.chat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nurse_connect.R;
import com.example.nurse_connect.adapters.MessageAdapter;
import com.example.nurse_connect.databinding.ActivityChatBinding;
import com.example.nurse_connect.models.ChatRoom;
import com.example.nurse_connect.models.Message;
import com.example.nurse_connect.viewmodels.ChatViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class ChatActivity extends AppCompatActivity implements MessageAdapter.OnMessageClickListener {
    
    private ActivityChatBinding binding;
    private ChatViewModel chatViewModel;
    private MessageAdapter messageAdapter;
    
    private String roomId;
    private String roomName;
    private ChatRoom.ChatRoomType roomType;
    
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Intent> pickDocumentLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Get intent data
        roomId = getIntent().getStringExtra("roomId");
        roomName = getIntent().getStringExtra("roomName");
        roomType = ChatRoom.ChatRoomType.valueOf(getIntent().getStringExtra("roomType"));
        
        if (roomId == null) {
            Toast.makeText(this, "Error: Invalid chat room", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        setupToolbar();
        setupViewModel();
        setupUI();
        setupActivityResults();
        observeData();
        loadMessages();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(roomName);
        }
    }
    
    private void setupViewModel() {
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
    }
    
    private void setupUI() {
        // Setup RecyclerView
        messageAdapter = new MessageAdapter(this);
        binding.rvMessages.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMessages.setAdapter(messageAdapter);
        
        // Setup send button
        binding.btnSend.setOnClickListener(v -> sendMessage());
        
        // Setup attachment button
        binding.btnAttachment.setOnClickListener(v -> showAttachmentOptions());
        
        // Setup message input
        binding.etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
        
        // Setup typing indicator
        binding.etMessage.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                // TODO: Implement typing indicator
            }
        });
    }
    
    private void setupActivityResults() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openImagePicker();
                    } else {
                        Toast.makeText(this, "Permission required to select images", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            // TODO: Upload image and send message
                            Toast.makeText(this, "Image selected: " + imageUri.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
        
        pickDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri documentUri = result.getData().getData();
                        if (documentUri != null) {
                            // TODO: Upload document and send message
                            Toast.makeText(this, "Document selected: " + documentUri.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }
    
    private void observeData() {
        chatViewModel.getChatMessages(roomId).observe(this, this::updateMessages);
        chatViewModel.getIsSendingMessage().observe(this, this::updateSendingState);
        chatViewModel.getSendMessageError().observe(this, this::showSendError);
        chatViewModel.getIsLoading().observe(this, this::updateLoadingState);
    }
    
    private void loadMessages() {
        // Messages are loaded automatically by the ViewModel
    }
    
    private void updateMessages(List<Message> messages) {
        if (messages != null) {
            messageAdapter.setMessages(messages);
            // Scroll to bottom for new messages
            if (messages.size() > 0) {
                binding.rvMessages.smoothScrollToPosition(messages.size() - 1);
            }
        }
    }
    
    private void updateSendingState(Boolean isSending) {
        if (isSending != null) {
            binding.btnSend.setEnabled(!isSending);
            binding.progressBar.setVisibility(isSending ? View.VISIBLE : View.GONE);
        }
    }
    
    private void updateLoadingState(Boolean isLoading) {
        if (isLoading != null) {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }
    
    private void showSendError(String error) {
        if (error != null && !error.isEmpty()) {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void sendMessage() {
        String messageText = binding.etMessage.getText().toString().trim();
        if (!messageText.isEmpty()) {
            chatViewModel.sendMessage(roomId, messageText);
            binding.etMessage.setText("");
        }
    }
    
    private void showAttachmentOptions() {
        String[] options = {"Image", "Document", "Camera"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Send Attachment")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            selectImage();
                            break;
                        case 1:
                            selectDocument();
                            break;
                        case 2:
                            openCamera();
                            break;
                    }
                })
                .show();
    }
    
    private void selectImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            openImagePicker();
        }
    }
    
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }
    
    private void selectDocument() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pickDocumentLauncher.launch(intent);
    }
    
    private void openCamera() {
        // TODO: Implement camera functionality
        Toast.makeText(this, "Camera functionality coming soon", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onMessageClick(Message message) {
        // Handle message click (e.g., show message details)
    }
    
    @Override
    public void onMessageLongClick(Message message, View view) {
        // Show message options (copy, delete, etc.)
        showMessageOptions(message);
    }
    
    @Override
    public void onMediaClick(Message message) {
        // Handle media click (open image, play video, etc.)
        switch (message.getType()) {
            case IMAGE:
                // TODO: Open image viewer
                break;
            case VIDEO:
                // TODO: Open video player
                break;
            case PDF:
            case DOCUMENT:
                // TODO: Open document viewer
                break;
        }
    }
    
    private void showMessageOptions(Message message) {
        String[] options = {"Copy", "Reply", "Forward", "Delete"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Message Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            copyMessage(message);
                            break;
                        case 1:
                            replyToMessage(message);
                            break;
                        case 2:
                            forwardMessage(message);
                            break;
                        case 3:
                            deleteMessage(message);
                            break;
                    }
                })
                .show();
    }
    
    private void copyMessage(Message message) {
        android.content.ClipboardManager clipboard = 
                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Message", message.getContent());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Message copied", Toast.LENGTH_SHORT).show();
    }
    
    private void replyToMessage(Message message) {
        // TODO: Implement reply functionality
        Toast.makeText(this, "Reply functionality coming soon", Toast.LENGTH_SHORT).show();
    }
    
    private void forwardMessage(Message message) {
        // TODO: Implement forward functionality
        Toast.makeText(this, "Forward functionality coming soon", Toast.LENGTH_SHORT).show();
    }
    
    private void deleteMessage(Message message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Message")
                .setMessage("Are you sure you want to delete this message?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // TODO: Implement delete functionality
                    Toast.makeText(this, "Delete functionality coming soon", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_chat_info) {
            showChatInfo();
            return true;
        } else if (id == R.id.action_search) {
            // TODO: Implement search functionality
            Toast.makeText(this, "Search functionality coming soon", Toast.LENGTH_SHORT).show();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void showChatInfo() {
        // TODO: Show chat room information
        Toast.makeText(this, "Chat info coming soon", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 