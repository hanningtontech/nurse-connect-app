package com.example.nurse_connect.services;

import android.util.Log;
import com.example.nurse_connect.models.QuizQuestion;
import com.google.firebase.firestore.FirebaseFirestore;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Arrays;

/**
 * HTML Question Extractor Service
 * Parses HTML files to extract quiz questions organized by career level
 */
public class HtmlQuestionExtractor {
    
    private static final String TAG = "HtmlQuestionExtractor";
    private FirebaseFirestore firestore;
    
    public interface ExtractionCallback {
        void onQuestionsExtracted(List<QuizQuestion> questions);
        void onError(String error);
        void onProgress(int current, int total);
    }
    
    public interface UploadCallback {
        void onSuccess(String message);
        void onError(String error);
        void onProgress(int current, int total);
    }
    
    public HtmlQuestionExtractor() {
        this.firestore = FirebaseFirestore.getInstance();
    }
    
    /**
     * Extract questions from HTML content
     */
    public void extractQuestionsFromHtml(String htmlContent, ExtractionCallback callback) {
        try {
            Log.d(TAG, "Starting HTML extraction...");
            
            // Parse HTML content
            Document doc = Jsoup.parse(htmlContent);
            
            // Extract questions by career level
            List<QuizQuestion> allQuestions = new ArrayList<>();
            
            // Extract CNA questions
            List<QuizQuestion> cnaQuestions = extractQuestionsByCareer(doc, "CNA", "Basic Nursing Skills / Nurse Aide Training");
            allQuestions.addAll(cnaQuestions);
            Log.d(TAG, "Extracted " + cnaQuestions.size() + " CNA questions");
            
            // Extract LPN/LVN questions
            List<QuizQuestion> lpnQuestions = extractQuestionsByCareer(doc, "LPN/LVN", "Anatomy and Physiology");
            allQuestions.addAll(lpnQuestions);
            Log.d(TAG, "Extracted " + lpnQuestions.size() + " LPN/LVN questions");
            
            // Extract RN questions
            List<QuizQuestion> rnQuestions = extractQuestionsByCareer(doc, "RN", "Health Assessment");
            allQuestions.addAll(rnQuestions);
            Log.d(TAG, "Extracted " + rnQuestions.size() + " RN questions");
            
            // Extract APRN questions
            List<QuizQuestion> aprnQuestions = extractQuestionsByCareer(doc, "APRN", "Advanced Pathophysiology");
            allQuestions.addAll(aprnQuestions);
            Log.d(TAG, "Extracted " + aprnQuestions.size() + " APRN questions");
            
            Log.d(TAG, "Total questions extracted: " + allQuestions.size());
            callback.onQuestionsExtracted(allQuestions);
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting questions from HTML", e);
            callback.onError("Failed to extract questions: " + e.getMessage());
        }
    }
    
    /**
     * Extract questions for a specific career level
     */
    private List<QuizQuestion> extractQuestionsByCareer(Document doc, String career, String defaultCourse) {
        List<QuizQuestion> questions = new ArrayList<>();
        
        // Select questions by career level
        String selector = String.format("div.question[data-career='%s']", career);
        Elements questionElements = doc.select(selector);
        
        Log.d(TAG, "Found " + questionElements.size() + " question elements for " + career);
        
        for (Element questionElement : questionElements) {
            try {
                QuizQuestion question = parseQuestionElement(questionElement, career, defaultCourse);
                if (question != null) {
                    questions.add(question);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error parsing question element for " + career + ": " + e.getMessage());
                continue;
            }
        }
        
        return questions;
    }
    
    /**
     * Parse a single question element
     */
    private QuizQuestion parseQuestionElement(Element questionElement, String career, String defaultCourse) {
        try {
            // Extract question text
            Element questionTextElement = questionElement.selectFirst(".question-text");
            if (questionTextElement == null) {
                Log.w(TAG, "Question text element not found");
                return null;
            }
            String questionText = questionTextElement.text().trim();
            
            // Extract options
            Elements optionElements = questionElement.select(".option");
            if (optionElements.size() < 4) {
                Log.w(TAG, "Question has fewer than 4 options: " + optionElements.size());
                return null;
            }
            
            String[] options = new String[4];
            int correctAnswerIndex = -1;
            
            for (int i = 0; i < Math.min(4, optionElements.size()); i++) {
                Element optionElement = optionElements.get(i);
                options[i] = optionElement.text().trim();
                
                // Check if this is the correct answer
                if (optionElement.hasAttr("data-correct") && 
                    "true".equals(optionElement.attr("data-correct"))) {
                    correctAnswerIndex = i;
                }
            }
            
            // If no correct answer found, try to find it from the text
            if (correctAnswerIndex == -1) {
                correctAnswerIndex = findCorrectAnswerFromText(questionElement);
            }
            
            // Extract rationale
            Element rationaleElement = questionElement.selectFirst(".rationale");
            String rationale = rationaleElement != null ? rationaleElement.text().trim() : "No rationale provided";
            
            // Extract course and unit from data attributes or use defaults
            String course = questionElement.hasAttr("data-course") ? 
                questionElement.attr("data-course") : defaultCourse;
            String unit = questionElement.hasAttr("data-unit") ? 
                questionElement.attr("data-unit") : "Unit 1: Introduction";
            
            // Validate the question
            if (questionText.length() < 10 || correctAnswerIndex == -1) {
                Log.w(TAG, "Invalid question data: text=" + questionText.length() + ", correct=" + correctAnswerIndex);
                return null;
            }
            
            // Create the question
            QuizQuestion question = createQuestion(
                "html_" + UUID.randomUUID().toString().substring(0, 8),
                questionText,
                Arrays.asList(options), // Convert String[] to List<String>
                correctAnswerIndex,
                rationale,
                course,
                unit,
                career,
                "medium",
                30
            );
            
            Log.d(TAG, "Successfully parsed question for " + career + ": " + questionText.substring(0, Math.min(50, questionText.length())) + "...");
            return question;
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing question element", e);
            return null;
        }
    }
    
    /**
     * Find correct answer from text if not in data attributes
     */
    private int findCorrectAnswerFromText(Element questionElement) {
        // Look for patterns like "Answer: B" or "Correct: A"
        String text = questionElement.text();
        
        // Pattern 1: "Answer: X" or "Correct: X"
        if (text.matches(".*[Aa]nswer:\\s*([A-D]).*")) {
            String match = text.replaceAll(".*[Aa]nswer:\\s*([A-D]).*", "$1");
            return match.charAt(0) - 'A';
        }
        
        // Pattern 2: "X)" at the end of the question
        if (text.matches(".*([A-D])\\)\\s*$")) {
            String match = text.replaceAll(".*([A-D])\\)\\s*$", "$1");
            return match.charAt(0) - 'A';
        }
        
        // Default to first option if no pattern found
        Log.w(TAG, "No correct answer pattern found, defaulting to option A");
        return 0;
    }
    
    /**
     * Create a QuizQuestion object
     */
    private QuizQuestion createQuestion(String questionId, String question, List<String> options,
                                      int correctAnswerIndex, String explanation, String course,
                                      String unit, String career, String difficulty, int timeLimit) {
        QuizQuestion q = new QuizQuestion();
        q.setQuestionId(questionId);
        q.setQuestion(question);
        q.setOptions(options); // Direct assignment - no conversion needed
        q.setCorrectAnswerIndex(correctAnswerIndex);
        q.setExplanation(explanation);
        q.setCourse(course);
        q.setUnit(unit);
        q.setCareer(career);
        q.setDifficulty(difficulty);
        q.setTimeLimit(timeLimit);
        return q;
    }
    
    /**
     * Upload questions to Firestore organized by career level
     */
    public void uploadQuestionsToFirestore(List<QuizQuestion> questions, UploadCallback callback) {
        Log.d(TAG, "Uploading " + questions.size() + " questions to Firestore");
        
        int total = questions.size();
        final int[] uploaded = {0};
        final int[] errors = {0};
        
        for (QuizQuestion question : questions) {
            // Determine collection based on career
            String collectionName = getCollectionNameForCareer(question.getCareer());
            
            firestore.collection(collectionName)
                    .document(question.getQuestionId())
                    .set(question)
                    .addOnSuccessListener(aVoid -> {
                        uploaded[0]++;
                        Log.d(TAG, "Uploaded question " + uploaded[0] + "/" + total + " to " + collectionName + ": " + question.getQuestionId());
                        
                        if (uploaded[0] + errors[0] == total) {
                            if (errors[0] == 0) {
                                callback.onSuccess("Successfully uploaded " + total + " questions");
                            } else {
                                callback.onSuccess("Uploaded " + uploaded[0] + " questions with " + errors[0] + " errors");
                            }
                        } else {
                            callback.onProgress(uploaded[0], total);
                        }
                    })
                    .addOnFailureListener(e -> {
                        errors[0]++;
                        Log.e(TAG, "Failed to upload question to " + collectionName + ": " + question.getQuestionId(), e);
                        
                        if (uploaded[0] + errors[0] == total) {
                            callback.onError("Uploaded " + uploaded[0] + " questions with " + errors[0] + " errors");
                        }
                    });
        }
    }
    
    /**
     * Get the appropriate Firestore collection name for a career
     */
    private String getCollectionNameForCareer(String career) {
        switch (career) {
            case "CNA":
                return "quiz_questions_cna";
            case "LPN/LVN":
                return "quiz_questions_lpn";
            case "RN":
                return "quiz_questions_rn";
            case "APRN":
                return "quiz_questions_aprn";
            default:
                return "quiz_questions_general";
        }
    }
}
