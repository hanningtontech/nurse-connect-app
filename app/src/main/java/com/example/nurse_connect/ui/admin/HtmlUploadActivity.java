package com.example.nurse_connect.ui.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nurse_connect.R;
import com.example.nurse_connect.models.QuizQuestion;
import com.example.nurse_connect.services.HtmlQuestionExtractor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class HtmlUploadActivity extends AppCompatActivity {
    
    private static final String TAG = "HtmlUploadActivity";
    private static final int PICK_HTML_FILE = 1;
    
    private Button btnSelectHtml;
    private Button btnExtractQuestions;
    private ProgressBar progressBar;
    private TextView textStatus;
    private TextView textQuestionCount;
    
    private HtmlQuestionExtractor htmlExtractor;
    private List<QuizQuestion> extractedQuestions;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_html_upload);
        
        // Initialize views
        btnSelectHtml = findViewById(R.id.btnSelectHtml);
        btnExtractQuestions = findViewById(R.id.btnExtractQuestions);
        progressBar = findViewById(R.id.progressBar);
        textStatus = findViewById(R.id.textStatus);
        textQuestionCount = findViewById(R.id.textQuestionCount);
        
        // Initialize HTML extractor
        htmlExtractor = new HtmlQuestionExtractor();
        
        // Setup click listeners
        btnSelectHtml.setOnClickListener(v -> selectHtmlFile());
        btnExtractQuestions.setOnClickListener(v -> extractQuestions());
        
        // Initial state
        btnExtractQuestions.setEnabled(false);
        textStatus.setText("Select an HTML file to extract questions");
        textQuestionCount.setText("");
        
        Log.d(TAG, "HtmlUploadActivity initialized");
    }
    
    /**
     * Select HTML file from device
     */
    private void selectHtmlFile() {
        // For now, we'll use a sample HTML file from assets
        // In a real app, you'd use Intent.ACTION_GET_CONTENT to let user select file
        loadSampleHtmlFile();
    }
    
    /**
     * Load sample HTML file from assets
     */
    private void loadSampleHtmlFile() {
        try {
            Log.d(TAG, "Loading sample HTML file from assets");
            textStatus.setText("Loading sample HTML file...");
            
            // Load the sample HTML file
            InputStream inputStream = getAssets().open("cna_questions_template.html");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder htmlContent = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                htmlContent.append(line).append("\n");
            }
            
            reader.close();
            inputStream.close();
            
            Log.d(TAG, "HTML file loaded successfully, size: " + htmlContent.length() + " characters");
            textStatus.setText("HTML file loaded successfully. Ready to extract questions.");
            btnExtractQuestions.setEnabled(true);
            
            // Store the HTML content for extraction
            extractedQuestions = null;
            textQuestionCount.setText("");
            
        } catch (IOException e) {
            Log.e(TAG, "Error loading HTML file", e);
            textStatus.setText("Error loading HTML file: " + e.getMessage());
            Toast.makeText(this, "Error loading HTML file", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Extract questions from HTML content
     */
    private void extractQuestions() {
        if (extractedQuestions != null) {
            // Questions already extracted, upload them
            uploadQuestions();
            return;
        }
        
        Log.d(TAG, "Starting HTML question extraction");
        textStatus.setText("Extracting questions from HTML...");
        btnExtractQuestions.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        
        // Load HTML content again for extraction
        try {
            InputStream inputStream = getAssets().open("cna_questions_template.html");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder htmlContent = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                htmlContent.append(line).append("\n");
            }
            
            reader.close();
            inputStream.close();
            
            // Extract questions from HTML
            htmlExtractor.extractQuestionsFromHtml(htmlContent.toString(), new HtmlQuestionExtractor.ExtractionCallback() {
                @Override
                public void onQuestionsExtracted(List<QuizQuestion> questions) {
                    runOnUiThread(() -> {
                        extractedQuestions = questions;
                        progressBar.setVisibility(View.GONE);
                        btnExtractQuestions.setEnabled(true);
                        btnExtractQuestions.setText("Upload " + questions.size() + " Questions");
                        
                        textStatus.setText("Successfully extracted " + questions.size() + " questions from HTML");
                        textQuestionCount.setText("Questions found: " + questions.size());
                        
                        Log.d(TAG, "Extraction completed: " + questions.size() + " questions");
                        
                        // Show question summary
                        showQuestionSummary(questions);
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnExtractQuestions.setEnabled(false);
                        textStatus.setText("Error: " + error);
                        Log.e(TAG, "Extraction error: " + error);
                        Toast.makeText(HtmlUploadActivity.this, "Extraction failed: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
                
                @Override
                public void onProgress(int current, int total) {
                    // Progress updates if needed
                }
            });
            
        } catch (IOException e) {
            Log.e(TAG, "Error loading HTML for extraction", e);
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                btnExtractQuestions.setEnabled(false);
                textStatus.setText("Error loading HTML: " + e.getMessage());
                Toast.makeText(HtmlUploadActivity.this, "Error loading HTML", Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    /**
     * Show summary of extracted questions
     */
    private void showQuestionSummary(List<QuizQuestion> questions) {
        StringBuilder summary = new StringBuilder();
        summary.append("Questions extracted:\n\n");
        
        for (int i = 0; i < Math.min(5, questions.size()); i++) {
            QuizQuestion q = questions.get(i);
            summary.append((i + 1) + ". ").append(q.getQuestion().substring(0, Math.min(50, q.getQuestion().length()))).append("...\n");
            summary.append("   Career: ").append(q.getCareer()).append("\n");
            summary.append("   Course: ").append(q.getCourse()).append("\n");
            summary.append("   Unit: ").append(q.getUnit()).append("\n\n");
        }
        
        if (questions.size() > 5) {
            summary.append("... and ").append(questions.size() - 5).append(" more questions");
        }
        
        textQuestionCount.setText(summary.toString());
    }
    
    /**
     * Upload extracted questions to Firestore
     */
    private void uploadQuestions() {
        if (extractedQuestions == null || extractedQuestions.isEmpty()) {
            Toast.makeText(this, "No questions to upload", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "Starting upload of " + extractedQuestions.size() + " questions");
        textStatus.setText("Uploading questions to Firestore...");
        btnExtractQuestions.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        
        htmlExtractor.uploadQuestionsToFirestore(extractedQuestions, new HtmlQuestionExtractor.UploadCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnExtractQuestions.setEnabled(true);
                    btnExtractQuestions.setText("Questions Uploaded!");
                    textStatus.setText(message);
                    
                    Log.d(TAG, "Upload completed: " + message);
                    Toast.makeText(HtmlUploadActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnExtractQuestions.setEnabled(true);
                    btnExtractQuestions.setText("Upload Failed");
                    textStatus.setText("Upload failed: " + error);
                    
                    Log.e(TAG, "Upload error: " + error);
                    Toast.makeText(HtmlUploadActivity.this, "Upload failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onProgress(int current, int total) {
                runOnUiThread(() -> {
                    textStatus.setText("Uploading questions: " + current + "/" + total);
                    progressBar.setProgress((current * 100) / total);
                });
            }
        });
    }
}
