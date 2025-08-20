package com.example.nurse_connect.utils;

import android.content.Context;
import android.util.Log;
import com.example.nurse_connect.models.QuizQuestion;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class QuizQuestionUploader {
    private static final String TAG = "QuizQuestionUploader";
    private static final String SAMPLE_QUESTIONS_FILE = "sample_quiz_questions.json";
    
    private FirebaseFirestore db;
    private Context context;
    
    public QuizQuestionUploader(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }
    
    public void uploadSampleQuestions(UploadCallback callback) {
        try {
            String jsonString = loadJSONFromAsset(SAMPLE_QUESTIONS_FILE);
            if (jsonString == null) {
                callback.onError("Failed to load sample questions file");
                return;
            }
            
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
            Type questionListType = new TypeToken<ArrayList<QuizQuestion>>(){}.getType();
            List<QuizQuestion> questions = gson.fromJson(jsonObject.get("questions"), questionListType);
            
            if (questions == null || questions.isEmpty()) {
                callback.onError("No questions found in the file");
                return;
            }
            
            Log.d(TAG, "Found " + questions.size() + " questions to upload");
            uploadQuestionsInBatches(questions, callback);
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing questions", e);
            callback.onError("Error parsing questions: " + e.getMessage());
        }
    }
    
    private void uploadQuestionsInBatches(List<QuizQuestion> questions, UploadCallback callback) {
        final int[] uploadedCount = {0};
        final int totalQuestions = questions.size();
        final int batchSize = 500;
        
        for (int i = 0; i < questions.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, questions.size());
            List<QuizQuestion> batch = questions.subList(i, endIndex);
            
            uploadBatch(batch, new UploadCallback() {
                @Override
                public void onSuccess() {
                    uploadedCount[0] += batch.size();
                    Log.d(TAG, "Uploaded batch: " + uploadedCount[0] + "/" + totalQuestions);
                    
                    if (uploadedCount[0] >= totalQuestions) {
                        callback.onSuccess();
                    }
                }
                
                @Override
                public void onError(String error) {
                    callback.onError("Failed to upload batch: " + error);
                }
            });
        }
    }
    
    private void uploadBatch(List<QuizQuestion> questions, UploadCallback callback) {
        List<com.google.firebase.firestore.WriteBatch> batches = new ArrayList<>();
        com.google.firebase.firestore.WriteBatch currentBatch = db.batch();
        int batchCount = 0;
        
        for (QuizQuestion question : questions) {
            String questionId = question.getQuestionId();
            if (questionId == null || questionId.isEmpty()) {
                questionId = "question_" + System.currentTimeMillis() + "_" + batchCount;
            }
            
            currentBatch.set(db.collection("quiz_questions").document(questionId), question);
            batchCount++;
            
            if (batchCount >= 500) {
                batches.add(currentBatch);
                currentBatch = db.batch();
                batchCount = 0;
            }
        }
        
        if (batchCount > 0) {
            batches.add(currentBatch);
        }
        
        executeBatches(batches, 0, callback);
    }
    
    private void executeBatches(List<com.google.firebase.firestore.WriteBatch> batches, int index, UploadCallback callback) {
        if (index >= batches.size()) {
            callback.onSuccess();
            return;
        }
        
        batches.get(index).commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Batch " + (index + 1) + " uploaded successfully");
                    executeBatches(batches, index + 1, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload batch " + (index + 1), e);
                    callback.onError("Batch upload failed: " + e.getMessage());
                });
    }
    
    private String loadJSONFromAsset(String fileName) {
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.e(TAG, "Error loading JSON file", e);
            return null;
        }
    }
    
    public void checkQuestionsExist(CheckCallback callback) {
        db.collection("quiz_questions")
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean exists = !querySnapshot.isEmpty();
                    callback.onResult(exists);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking questions", e);
                    callback.onError("Failed to check questions: " + e.getMessage());
                });
    }
    
    public void getQuestionCount(CountCallback callback) {
        db.collection("quiz_questions")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    callback.onResult(querySnapshot.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting question count", e);
                    callback.onError("Failed to get question count: " + e.getMessage());
                });
    }
    
    public interface UploadCallback {
        void onSuccess();
        void onError(String error);
    }
    
    public interface CheckCallback {
        void onResult(boolean exists);
        void onError(String error);
    }
    
    public interface CountCallback {
        void onResult(int count);
        void onError(String error);
    }
}
