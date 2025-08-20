package com.example.nurse_connect.ui.admin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.nurse_connect.R;
import com.example.nurse_connect.services.PdfQuestionExtractor;
import com.example.nurse_connect.models.NursingCurriculum;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.util.ArrayList;
import java.util.List;

public class PdfUploadActivity extends AppCompatActivity {
    private static final String TAG = "PdfUploadActivity";
    private static final int PICK_PDF_REQUEST = 1;
    
    private Button btnSelectPdf;
    private Button btnUploadPdf;
    private Button btnExtractQuestions;
    private Button btnListPdfs;
    private Button btnRefreshPdfs;
    private Button btnStructuredQuiz;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private TextView tvProgress;
    
    private Uri pdfUri;
    private String uploadedPdfUrl;
    private PdfQuestionExtractor questionExtractor;
    
    // Quiz category selection
    private String selectedCareer;
    private String selectedCourse;
    private String selectedUnit;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_upload);
        
        initViews();
        questionExtractor = new PdfQuestionExtractor(this);
    }
    
    private void initViews() {
        btnSelectPdf = findViewById(R.id.btnSelectPdf);
        btnUploadPdf = findViewById(R.id.btnUploadPdf);
        btnExtractQuestions = findViewById(R.id.btnExtractQuestions);
        btnListPdfs = findViewById(R.id.btnListPdfs);
        btnRefreshPdfs = findViewById(R.id.btnRefreshPdfs);
        btnStructuredQuiz = findViewById(R.id.btnStructuredQuiz);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        tvProgress = findViewById(R.id.tvProgress);
        
        btnSelectPdf.setOnClickListener(v -> selectPdf());
        btnUploadPdf.setOnClickListener(v -> uploadPdf());
        btnExtractQuestions.setOnClickListener(v -> extractQuestions());
        btnListPdfs.setOnClickListener(v -> listExistingPdfs());
        btnRefreshPdfs.setOnClickListener(v -> listExistingPdfs());
        btnStructuredQuiz.setOnClickListener(v -> showStructuredQuizSelection());
        
        // Add generate sample questions button listener
        Button btnGenerateSample = findViewById(R.id.btnGenerateSample);
        btnGenerateSample.setOnClickListener(v -> generateSampleQuestions());
        
        // Initially disable buttons
        btnUploadPdf.setEnabled(false);
        btnExtractQuestions.setEnabled(false);
        
        updateStatus("Ready to upload PDF and extract questions");
    }
    
    private void showStructuredQuizSelection() {
        // Get the curriculum structure
        List<NursingCurriculum.CareerLevel> careerLevels = NursingCurriculum.getCurriculum();
        
        // Show career level selection dialog
        showCareerLevelDialog(careerLevels);
    }
    
    private void showCareerLevelDialog(List<NursingCurriculum.CareerLevel> careerLevels) {
        // Create list of career level names
        List<String> careerNames = new ArrayList<>();
        for (NursingCurriculum.CareerLevel career : careerLevels) {
            careerNames.add(career.getName() + "\n" + career.getDescription());
        }
        
        new AlertDialog.Builder(this)
                .setTitle("Select Your Career Level")
                .setItems(careerNames.toArray(new String[0]), (dialog, which) -> {
                    NursingCurriculum.CareerLevel selectedCareer = careerLevels.get(which);
                    showCourseDialog(selectedCareer);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showCourseDialog(NursingCurriculum.CareerLevel career) {
        List<NursingCurriculum.Course> courses = career.getCourses();
        
        // Create list of course names
        List<String> courseNames = new ArrayList<>();
        for (NursingCurriculum.Course course : courses) {
            courseNames.add(course.getName() + "\n" + course.getDescription());
        }
        
        new AlertDialog.Builder(this)
                .setTitle("Select Course for " + career.getName())
                .setItems(courseNames.toArray(new String[0]), (dialog, which) -> {
                    NursingCurriculum.Course selectedCourse = courses.get(which);
                    showUnitDialog(career, selectedCourse);
                })
                .setNegativeButton("Back", (dialog, which) -> showStructuredQuizSelection())
                .show();
    }
    
    private void showUnitDialog(NursingCurriculum.CareerLevel career, NursingCurriculum.Course course) {
        List<NursingCurriculum.Unit> units = course.getUnits();
        
        // Create list of unit names
        List<String> unitNames = new ArrayList<>();
        for (NursingCurriculum.Unit unit : units) {
            unitNames.add(unit.getName() + "\n" + unit.getDescription());
        }
        
        new AlertDialog.Builder(this)
                .setTitle("Select Unit for " + course.getName())
                .setItems(unitNames.toArray(new String[0]), (dialog, which) -> {
                    NursingCurriculum.Unit selectedUnit = units.get(which);
                    handleQuizSelection(career, course, selectedUnit);
                })
                .setNegativeButton("Back", (dialog, which) -> showCourseDialog(career))
                .show();
    }
    
    private void handleQuizSelection(NursingCurriculum.CareerLevel career, 
                                   NursingCurriculum.Course course, 
                                   NursingCurriculum.Unit unit) {
        // Store the selected values
        selectedCareer = career.getName();
        selectedCourse = course.getName();
        selectedUnit = unit.getName();
        
        // Update status to show selection
        String selection = String.format("Selected:\nCareer: %s\nCourse: %s\nUnit: %s", 
                                       selectedCareer, selectedCourse, selectedUnit);
        updateStatus(selection);
        
        // Enable extract questions button if PDF is selected
        if (uploadedPdfUrl != null) {
            btnExtractQuestions.setEnabled(true);
            updateStatus(selection + "\n\nPDF is ready. Click 'Extract Questions' to begin.");
        } else {
            updateStatus(selection + "\n\nPlease upload a PDF first, then click 'Extract Questions'.");
        }
        
        Toast.makeText(this, "Quiz category selected: " + selectedUnit, Toast.LENGTH_LONG).show();
    }
    
    private void selectPdf() {
        Intent intent = new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select PDF"), PICK_PDF_REQUEST);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK && data != null) {
            pdfUri = data.getData();
            if (pdfUri != null) {
                btnUploadPdf.setEnabled(true);
                updateStatus("PDF selected: " + getFileName(pdfUri));
            }
        }
    }
    
    private void uploadPdf() {
        if (pdfUri == null) {
            Toast.makeText(this, "Please select a PDF first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        updateStatus("Uploading PDF to Firebase Storage...");
        progressBar.setVisibility(View.VISIBLE);
        btnUploadPdf.setEnabled(false);
        
        // Create a reference to the PDF in Firebase Storage
        String fileName = "fundamentals_nursing_" + System.currentTimeMillis() + ".pdf";
        StorageReference pdfRef = FirebaseStorage.getInstance().getReference("nursing_pdfs/" + fileName);
        
        UploadTask uploadTask = pdfRef.putFile(pdfUri);
        
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            // Get the download URL
            pdfRef.getDownloadUrl().addOnSuccessListener(uri -> {
                uploadedPdfUrl = uri.toString();
                updateStatusWithPdfInfo("PDF uploaded successfully!");
                btnExtractQuestions.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "PDF uploaded successfully", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            updateStatus("Upload failed: " + e.getMessage());
            progressBar.setVisibility(View.GONE);
            btnUploadPdf.setEnabled(true);
            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
    
    private void extractQuestions() {
        if (uploadedPdfUrl == null) {
            Toast.makeText(this, "Please upload a PDF first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        updateStatus("Extracting questions from PDF...");
        progressBar.setVisibility(View.VISIBLE);
        btnExtractQuestions.setEnabled(false);
        
        // Extract questions from the uploaded PDF
        String courseName = selectedCourse != null ? selectedCourse : "Basic Nursing Skills / Nurse Aide Training";
        String unitName = selectedUnit != null ? selectedUnit : "Unit 1: Introduction to Healthcare";
        String careerName = selectedCareer != null ? selectedCareer : "Certified Nursing Assistant (CNA)";
        questionExtractor.extractQuestionsFromPdf(uploadedPdfUrl, courseName, unitName, careerName,
            new PdfQuestionExtractor.ExtractionCallback() {
                @Override
                public void onQuestionsExtracted(List<PdfQuestionExtractor.QuizQuestion> questions) {
                    updateStatus("Extracted " + questions.size() + " questions. Uploading to Firestore...");
                    
                    // Upload questions to Firestore
                    questionExtractor.uploadQuestionsToFirestore(questions, new PdfQuestionExtractor.UploadCallback() {
                        @Override
                        public void onSuccess(String message) {
                            updateStatus(message);
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(PdfUploadActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                        
                        @Override
                        public void onProgress(int current, int total) {
                            updateProgress(current, total);
                        }
                        
                        @Override
                        public void onError(String error) {
                            updateStatus("Error: " + error);
                            progressBar.setVisibility(View.GONE);
                            btnExtractQuestions.setEnabled(true);
                            Toast.makeText(PdfUploadActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    updateStatus("Extraction failed: " + error);
                    progressBar.setVisibility(View.GONE);
                    btnExtractQuestions.setEnabled(true);
                    Toast.makeText(PdfUploadActivity.this, "Extraction failed: " + error, Toast.LENGTH_LONG).show();
                }
                
                @Override
                public void onProgress(int current, int total) {
                    updateProgress(current, total);
                }
            });
    }
    
    /**
     * Generate sample questions for testing
     */
    private void generateSampleQuestions() {
        // Check if career and unit are selected
        if (selectedCareer == null || selectedUnit == null) {
            Toast.makeText(this, "Please select Career and Unit first using 'Select Quiz Category'", Toast.LENGTH_LONG).show();
            return;
        }
        
        updateStatus("Generating sample questions for " + selectedCareer + " - " + selectedUnit + "...");
        progressBar.setVisibility(View.VISIBLE);
        
        // Generate sample questions using the selected career and unit
        List<PdfQuestionExtractor.QuizQuestion> questions = questionExtractor.generateSampleQuestions(selectedUnit, selectedCareer);
        
        if (questions.isEmpty()) {
            updateStatus("No sample questions available for " + selectedCareer + " - " + selectedUnit);
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "No sample questions available for this combination", Toast.LENGTH_SHORT).show();
            return;
        }
        
        updateStatus("Generated " + questions.size() + " sample questions. Uploading to Firestore...");
        
        // Upload questions to Firestore
        questionExtractor.uploadQuestionsToFirestore(questions, new PdfQuestionExtractor.UploadCallback() {
            @Override
            public void onSuccess(String message) {
                updateStatus(message + "\n\nGenerated questions for:\nCareer: " + selectedCareer + "\nUnit: " + selectedUnit);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(PdfUploadActivity.this, message, Toast.LENGTH_LONG).show();
            }
            
            @Override
            public void onProgress(int current, int total) {
                updateProgress(current, total);
            }
            
            @Override
            public void onError(String error) {
                updateStatus("Error uploading sample questions: " + error);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(PdfUploadActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void updateStatus(String status) {
        tvStatus.setText(status);
        Log.d(TAG, "Status: " + status);
    }
    
    private void updateStatusWithPdfInfo(String status) {
        String fullStatus = status;
        if (uploadedPdfUrl != null) {
            // Extract filename from URL
            String fileName = "Unknown PDF";
            try {
                String[] urlParts = uploadedPdfUrl.split("/");
                if (urlParts.length > 0) {
                    fileName = urlParts[urlParts.length - 1].split("\\?")[0]; // Remove query parameters
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not parse filename from URL", e);
            }
            fullStatus += "\n\n📄 Selected PDF: " + fileName;
        }
        tvStatus.setText(fullStatus);
        Log.d(TAG, "Status: " + status);
    }
    
    private void updateProgress(int current, int total) {
        if (total > 0) {
            int percentage = (current * 100) / total;
            tvProgress.setText(current + "/" + total + " (" + percentage + "%)");
            progressBar.setProgress(percentage);
        }
    }
    
    private void listExistingPdfs() {
        updateStatus("Loading existing PDFs from Firebase Storage...");
        progressBar.setVisibility(View.VISIBLE);
        btnListPdfs.setEnabled(false);
        
        // List all PDFs in the nursing_pdfs folder
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("nursing_pdfs");
        
        storageRef.listAll()
                .addOnSuccessListener(listResult -> {
                    if (listResult.getItems().isEmpty()) {
                        updateStatus("No PDFs found in storage. Please upload a PDF first.");
                        progressBar.setVisibility(View.GONE);
                        btnListPdfs.setEnabled(true);
                        return;
                    }
                    
                    // Get metadata for each PDF to show file sizes
                    getPdfMetadata(listResult.getItems());
                    
                })
                .addOnFailureListener(e -> {
                    updateStatus("Failed to list PDFs: " + e.getMessage());
                    progressBar.setVisibility(View.GONE);
                    btnListPdfs.setEnabled(true);
                    Toast.makeText(this, "Failed to list PDFs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void getPdfMetadata(List<StorageReference> pdfFiles) {
        List<PdfFileInfo> pdfInfoList = new ArrayList<>();
        final int[] processed = {0};
        final int total = pdfFiles.size();
        
        for (StorageReference pdf : pdfFiles) {
            pdf.getMetadata()
                    .addOnSuccessListener(metadata -> {
                        PdfFileInfo info = new PdfFileInfo(
                            pdf.getName(),
                            pdf,
                            metadata.getSizeBytes(),
                            metadata.getCreationTimeMillis()
                        );
                        pdfInfoList.add(info);
                        
                        processed[0]++;
                        if (processed[0] == total) {
                            // All metadata retrieved, show selection dialog
                            showPdfSelectionDialogWithMetadata(pdfInfoList);
                            progressBar.setVisibility(View.GONE);
                            btnListPdfs.setEnabled(true);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // If metadata fails, still add the PDF with basic info
                        PdfFileInfo info = new PdfFileInfo(
                            pdf.getName(),
                            pdf,
                            -1,
                            -1
                        );
                        pdfInfoList.add(info);
                        
                        processed[0]++;
                        if (processed[0] == total) {
                            showPdfSelectionDialogWithMetadata(pdfInfoList);
                            progressBar.setVisibility(View.GONE);
                            btnListPdfs.setEnabled(true);
                        }
                    });
        }
    }
    
    private void showPdfSelectionDialogWithMetadata(List<PdfFileInfo> pdfInfoList) {
        // Sort by creation time (newest first)
        pdfInfoList.sort((a, b) -> Long.compare(b.creationTime, a.creationTime));
        
        // Create display names with file size
        List<String> displayNames = new ArrayList<>();
        for (PdfFileInfo info : pdfInfoList) {
            String displayName = info.fileName;
            if (info.fileSize > 0) {
                displayName += " (" + formatFileSize(info.fileSize) + ")";
            }
            displayNames.add(displayName);
        }
        
        // Create and show the dialog
        new AlertDialog.Builder(this)
                .setTitle("Select PDF to Extract Questions From")
                .setItems(displayNames.toArray(new String[0]), (dialog, which) -> {
                    PdfFileInfo selectedInfo = pdfInfoList.get(which);
                    updateStatus("Selected PDF: " + selectedInfo.fileName);
                    
                    // Get the download URL for the selected PDF
                    selectedInfo.storageReference.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                uploadedPdfUrl = uri.toString();
                                btnExtractQuestions.setEnabled(true);
                                updateStatusWithPdfInfo("PDF selected: " + selectedInfo.fileName + ". Ready to extract questions.");
                                Toast.makeText(this, "PDF selected: " + selectedInfo.fileName, Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                updateStatus("Failed to get PDF URL: " + e.getMessage());
                                Toast.makeText(this, "Failed to get PDF URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
    
    // Helper class to store PDF file information
    private static class PdfFileInfo {
        String fileName;
        StorageReference storageReference;
        long fileSize;
        long creationTime;
        
        PdfFileInfo(String fileName, StorageReference storageReference, long fileSize, long creationTime) {
            this.fileName = fileName;
            this.storageReference = storageReference;
            this.fileSize = fileSize;
            this.creationTime = creationTime;
        }
    }
    
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
