package com.example.nurse_connect.services;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import com.example.nurse_connect.models.QuizQuestion;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuizQuestionExtractor {
    private static final String TAG = "QuizQuestionExtractor";
    private static boolean isPdfBoxInitialized = false;

    public interface ExtractionCallback {
        void onSuccess(List<QuizQuestion> questions);
        void onError(String error);
        void onProgress(int progress, String message);
    }

    public static void initializePdfBox(Context context) {
        if (!isPdfBoxInitialized) {
            Log.d(TAG, "Initializing PDFBox library for quiz extraction...");
            PDFBoxResourceLoader.init(context);
            isPdfBoxInitialized = true;
            Log.d(TAG, "PDFBox library initialized successfully");
        }
    }

    public static void extractQuizQuestions(Context context, Uri pdfUri, String career, String course, String unit, ExtractionCallback callback) {
        if (!isPdfBoxInitialized) {
            initializePdfBox(context);
        }

        callback.onProgress(10, "Starting PDF extraction...");

        try {
            // Extract text from PDF
            String pdfText = extractTextFromPdf(context, pdfUri);
            if (pdfText == null || pdfText.trim().isEmpty()) {
                callback.onError("Failed to extract text from PDF");
                return;
            }

            callback.onProgress(30, "Text extracted, parsing questions...");

            // Parse questions from text
            List<QuizQuestion> questions = parseQuestionsFromText(pdfText, career, course, unit);
            
            if (questions.isEmpty()) {
                callback.onError("No quiz questions found in the PDF");
                return;
            }

            callback.onProgress(80, "Questions parsed, finalizing...");
            callback.onSuccess(questions);

        } catch (Exception e) {
            Log.e(TAG, "Error extracting quiz questions: " + e.getMessage(), e);
            callback.onError("Extraction failed: " + e.getMessage());
        }
    }

    private static String extractTextFromPdf(Context context, Uri pdfUri) throws IOException {
        InputStream inputStream = null;
        PDDocument document = null;

        try {
            inputStream = context.getContentResolver().openInputStream(pdfUri);
            if (inputStream == null) {
                throw new IOException("Could not open PDF input stream");
            }

            document = PDDocument.load(inputStream);
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);
            
            String text = textStripper.getText(document);
            Log.d(TAG, "Extracted text length: " + text.length());
            
            return text;

        } finally {
            if (document != null) {
                document.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    public static List<QuizQuestion> parseQuestionsFromText(String text, String career, String course, String unit) {
        List<QuizQuestion> questions = new ArrayList<>();
        
        // Split text into lines for easier parsing
        String[] lines = text.split("\n");
        
        // Pattern to match question numbers (e.g., "1.", "2.", "3.")
        Pattern questionPattern = Pattern.compile("^(\\d+)\\.");
        
        // Pattern to match answer options (e.g., "a)", "b)", "c)", "d)")
        Pattern optionPattern = Pattern.compile("^([a-d])\\)");
        
        // Pattern to match "Answer:" followed by the correct answer
        Pattern answerPattern = Pattern.compile("Answer:\\s*([a-d])");
        
        // Pattern to match "Rationale:" followed by explanation
        Pattern rationalePattern = Pattern.compile("Rationale:\\s*(.+)");
        
        QuizQuestion currentQuestion = null;
        StringBuilder currentRationale = new StringBuilder();
        boolean inRationale = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            // Check if this line starts a new question
            Matcher questionMatcher = questionPattern.matcher(line);
            if (questionMatcher.find()) {
                // Save previous question if exists
                if (currentQuestion != null) {
                    if (currentRationale.length() > 0) {
                        currentQuestion.setExplanation(currentRationale.toString().trim());
                    }
                    questions.add(currentQuestion);
                }

                // Start new question
                currentQuestion = new QuizQuestion();
                currentQuestion.setCareer(career);
                currentQuestion.setCourse(course);
                currentQuestion.setUnit(unit);
                currentQuestion.setQuestion(extractQuestionText(line));
                currentQuestion.setTimeLimit(30); // Default 30 seconds
                
                // Initialize options list
                currentQuestion.setOptions(new ArrayList<>());
                currentQuestion.setCorrectAnswerIndex(-1);
                
                currentRationale = new StringBuilder();
                inRationale = false;
                
                Log.d(TAG, "Found question: " + currentQuestion.getQuestion());
                continue;
            }

            // Check if this line is an answer option
            Matcher optionMatcher = optionPattern.matcher(line);
            if (optionMatcher.find() && currentQuestion != null) {
                String optionText = line.substring(line.indexOf(")") + 1).trim();
                currentQuestion.getOptions().add(optionText);
                Log.d(TAG, "Added option: " + optionText);
                continue;
            }

            // Check if this line contains the answer
            Matcher answerMatcher = answerPattern.matcher(line);
            if (answerMatcher.find() && currentQuestion != null) {
                String answerLetter = answerMatcher.group(1).toLowerCase();
                int answerIndex = answerLetter.charAt(0) - 'a'; // Convert a->0, b->1, c->2, d->3
                currentQuestion.setCorrectAnswerIndex(answerIndex);
                Log.d(TAG, "Set correct answer: " + answerLetter + " (index: " + answerIndex + ")");
                continue;
            }

            // Check if this line starts rationale
            Matcher rationaleMatcher = rationalePattern.matcher(line);
            if (rationaleMatcher.find() && currentQuestion != null) {
                inRationale = true;
                currentRationale.append(rationaleMatcher.group(1));
                continue;
            }

            // If we're in rationale section, append to current rationale
            if (inRationale && currentQuestion != null) {
                currentRationale.append(" ").append(line);
            }
        }

        // Add the last question
        if (currentQuestion != null) {
            if (currentRationale.length() > 0) {
                currentQuestion.setExplanation(currentRationale.toString().trim());
            }
            questions.add(currentQuestion);
        }

        Log.d(TAG, "Parsed " + questions.size() + " questions from PDF");
        return questions;
    }

    private static String extractQuestionText(String line) {
        // Remove the question number and dot, then trim
        String questionText = line.replaceFirst("^\\d+\\.\\s*", "").trim();
        
        // If the question text is too long, truncate it
        if (questionText.length() > 500) {
            questionText = questionText.substring(0, 500) + "...";
        }
        
        return questionText;
    }

    // Helper method to validate extracted questions
    public static boolean validateQuestion(QuizQuestion question) {
        if (question.getQuestion() == null || question.getQuestion().trim().isEmpty()) {
            return false;
        }
        
        if (question.getOptions() == null || question.getOptions().size() < 2) {
            return false;
        }
        
        if (question.getCorrectAnswerIndex() < 0 || question.getCorrectAnswerIndex() >= question.getOptions().size()) {
            return false;
        }
        
        if (question.getCareer() == null || question.getCareer().trim().isEmpty()) {
            return false;
        }
        
        if (question.getCourse() == null || question.getCourse().trim().isEmpty()) {
            return false;
        }
        
        if (question.getUnit() == null || question.getUnit().trim().isEmpty()) {
            return false;
        }
        
        return true;
    }
}
