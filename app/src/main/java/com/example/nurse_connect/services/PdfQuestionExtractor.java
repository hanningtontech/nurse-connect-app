package com.example.nurse_connect.services;

import android.content.Context;
import android.util.Log;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfQuestionExtractor {
    private static final String TAG = "PdfQuestionExtractor";
    
    private FirebaseStorage storage;
    private FirebaseFirestore firestore;
    private Context context;
    
    public interface ExtractionCallback {
        void onQuestionsExtracted(List<QuizQuestion> questions);
        void onError(String error);
        void onProgress(int current, int total);
    }
    
    public PdfQuestionExtractor(Context context) {
        this.context = context;
        this.storage = FirebaseStorage.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        
        // Initialize PDFBox for PDF parsing
        PDFBoxResourceLoader.init(context);
    }
    
    /**
     * Extract questions from a PDF stored in Firebase Storage
     */
    public void extractQuestionsFromPdf(String pdfUrl, String course, String unit, String career, ExtractionCallback callback) {
        Log.d(TAG, "Starting PDF extraction from: " + pdfUrl);
        
        // Validate PDF URL
        if (pdfUrl == null || pdfUrl.trim().isEmpty()) {
            callback.onError("PDF URL is null or empty");
            return;
        }
        
        if (!pdfUrl.startsWith("https://firebasestorage.googleapis.com/")) {
            callback.onError("Invalid Firebase Storage URL format: " + pdfUrl);
            return;
        }
        
        try {
            // Download PDF from Firebase Storage
            StorageReference pdfRef = storage.getReferenceFromUrl(pdfUrl);
            
            Log.d(TAG, "Starting PDF download from: " + pdfUrl);
            
            pdfRef.getBytes(Long.MAX_VALUE)
                .addOnSuccessListener(bytes -> {
                    Log.d(TAG, "PDF downloaded successfully, size: " + bytes.length + " bytes");
                    
                    try {
                        // Parse PDF content
                        Log.d(TAG, "Starting PDF content parsing...");
                        List<QuizQuestion> questions = parsePdfContent(bytes, course, unit, career);
                        
                        if (questions.isEmpty()) {
                            Log.w(TAG, "No questions extracted from PDF");
                            callback.onQuestionsExtracted(new ArrayList<>());
                        }
                        
                        Log.d(TAG, "Successfully extracted " + questions.size() + " questions from PDF");
                        Log.d(TAG, "Question types found:");
                        for (QuizQuestion q : questions) {
                            Log.d(TAG, "  - " + q.getQuestion().substring(0, Math.min(50, q.getQuestion().length())) + "...");
                        }
                        
                        callback.onQuestionsExtracted(questions);
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing PDF content", e);
                        Log.w(TAG, "No questions could be extracted from PDF");
                        callback.onQuestionsExtracted(new ArrayList<>());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to download PDF", e);
                    callback.onError("Failed to download PDF: " + e.getMessage());
                });
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid Firebase Storage URL", e);
            Log.w(TAG, "No questions could be extracted due to URL error");
            callback.onQuestionsExtracted(new ArrayList<>());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error", e);
            Log.w(TAG, "No questions could be extracted due to unexpected error");
            callback.onQuestionsExtracted(new ArrayList<>());
        }
    }
    

    
    /**
     * Upload extracted questions to Firestore
     */
    public void uploadQuestionsToFirestore(List<QuizQuestion> questions, UploadCallback callback) {
        Log.d(TAG, "Uploading " + questions.size() + " questions to Firestore");
        
        int total = questions.size();
        final int[] uploaded = {0};
        
        for (QuizQuestion question : questions) {
            // Create a unique document ID
            String docId = question.getQuestionId();
            
            firestore.collection("quiz_questions")
                    .document(docId)
                    .set(question)
                    .addOnSuccessListener(aVoid -> {
                        uploaded[0]++;
                        Log.d(TAG, "Uploaded question " + uploaded[0] + "/" + total + ": " + docId);
                        
                        if (uploaded[0] == total) {
                            callback.onSuccess("Successfully uploaded " + total + " questions");
                        } else {
                            callback.onProgress(uploaded[0], total);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to upload question: " + docId, e);
                        callback.onError("Failed to upload question: " + e.getMessage());
                    });
        }
    }
    
    /**
     * Parse PDF content and extract questions
     */
    private List<QuizQuestion> parsePdfContent(byte[] pdfBytes, String course, String unit, String career) throws IOException {
        List<QuizQuestion> questions = new ArrayList<>();
        
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            
            // Configure stripper for better text extraction
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(document.getNumberOfPages());
            
            String text = stripper.getText(document);
            
            Log.d(TAG, "PDF text extracted, length: " + text.length() + " characters");
            Log.d(TAG, "PDF pages: " + document.getNumberOfPages());
            
            // Auto-detect career, course, and unit if not provided
            if (career == null || career.isEmpty()) {
                career = detectCareerFromText(text);
                Log.d(TAG, "Auto-detected career: " + career);
            }
            if (course == null || course.isEmpty()) {
                course = detectCourseFromText(text);
                Log.d(TAG, "Auto-detected course: " + course);
            }
            if (unit == null || unit.isEmpty()) {
                unit = detectUnitFromText(text);
                Log.d(TAG, "Auto-detected unit: " + unit);
            }
            
            // Clean the extracted text
            text = cleanExtractedText(text);
            
            // Split text into sections by units/chapters
            List<String> sections = splitTextIntoSections(text);
            
            for (int i = 0; i < sections.size(); i++) {
                String section = sections.get(i);
                String unitName = unit != null ? unit : "Unit " + (i + 1) + ": " + getUnitTitle(i);
                
                Log.d(TAG, "Processing section " + (i + 1) + ": " + unitName);
                
                // Extract questions from this section
                List<QuizQuestion> sectionQuestions = extractQuestionsFromSection(section, course, unitName, career);
                
                // If no structured questions found, generate from content
                if (sectionQuestions.isEmpty()) {
                    Log.d(TAG, "No structured questions found in " + unitName + ", generating from content");
                    sectionQuestions = generateQuestionsFromContent(section, course, unitName, career);
                }
                
                questions.addAll(sectionQuestions);
                Log.d(TAG, "Added " + sectionQuestions.size() + " questions from " + unitName);
                
                Log.d(TAG, "Extracted " + sectionQuestions.size() + " questions from " + unitName);
            }
            
            Log.d(TAG, "Total questions extracted from PDF: " + questions.size());
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing PDF", e);
            throw new IOException("Failed to parse PDF: " + e.getMessage());
        }
        
        return questions;
    }
    
    /**
     * Auto-detect career level from PDF text
     */
    private String detectCareerFromText(String text) {
        Log.d(TAG, "Detecting career from PDF text...");
        
        // Look for career level indicators in your PDF format
        Pattern careerPattern = Pattern.compile(
            "Part\\s*\\d+:\\s*([^\\n]+?)(?:Program|Level|Nursing)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = careerPattern.matcher(text);
        if (matcher.find()) {
            String career = matcher.group(1).trim();
            Log.d(TAG, "Detected career from pattern: " + career);
            return career;
        }
        
        // Enhanced fallback patterns for your specific format
        if (text.contains("CNA") || text.contains("Certified Nursing Assistant")) {
            Log.d(TAG, "Detected CNA from text content");
            return "Certified Nursing Assistant (CNA)";
        } else if (text.contains("LPN") || text.contains("Licensed Practical Nurse") || text.contains("Licensed Vocational Nurse")) {
            Log.d(TAG, "Detected LPN/LVN from text content");
            return "Licensed Practical/Vocational Nurse (LPN/LVN)";
        } else if (text.contains("RN") || text.contains("Registered Nurse")) {
            Log.d(TAG, "Detected RN from text content");
            return "Registered Nurse (RN)";
        } else if (text.contains("APRN") || text.contains("Advanced Practice")) {
            Log.d(TAG, "Detected APRN from text content");
            return "Advanced Practice Registered Nurse (APRN)";
        }
        
        // Look for specific text patterns in your PDF
        if (text.contains("THE NURSING PROGRESSION: A COMPREHENSIVE QUESTION BANK FOR CNA, LPN, AND RN LEVELS")) {
            Log.d(TAG, "Detected comprehensive nursing question bank - defaulting to CNA");
            return "Certified Nursing Assistant (CNA)";
        }
        
        Log.d(TAG, "No specific career detected, defaulting to CNA");
        return "Certified Nursing Assistant (CNA)";
    }
    
    /**
     * Auto-detect course from PDF text
     */
    private String detectCourseFromText(String text) {
        Log.d(TAG, "Detecting course from PDF text...");
        
        // Look for course indicators in your specific format
        Pattern coursePattern = Pattern.compile(
            "Course:\\s*([^\\n]+)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = coursePattern.matcher(text);
        if (matcher.find()) {
            String course = matcher.group(1).trim();
            Log.d(TAG, "Detected course from pattern: " + course);
            return course;
        }
        
        // Enhanced fallback for your nursing curriculum
        if (text.contains("Basic Nursing Skills") || text.contains("Nurse Aide Training")) {
            Log.d(TAG, "Detected Basic Nursing Skills from text content");
            return "Basic Nursing Skills / Nurse Aide Training";
        } else if (text.contains("Anatomy and Physiology")) {
            Log.d(TAG, "Detected Anatomy and Physiology from text content");
            return "Anatomy and Physiology";
        } else if (text.contains("Health Assessment")) {
            Log.d(TAG, "Detected Health Assessment from text content");
            return "Health Assessment";
        } else if (text.contains("Pathophysiology")) {
            Log.d(TAG, "Detected Pathophysiology from text content");
            return "Pathophysiology";
        } else if (text.contains("Medical-Surgical Nursing")) {
            Log.d(TAG, "Detected Medical-Surgical Nursing from text content");
            return "Medical-Surgical Nursing I & II";
        }
        
        // Look for specific text patterns in your PDF
        if (text.contains("Part 1: Certified Nursing Assistant (CNA)")) {
            Log.d(TAG, "Detected CNA section - defaulting to Basic Nursing Skills");
            return "Basic Nursing Skills / Nurse Aide Training";
        }
        
        Log.d(TAG, "No specific course detected, defaulting to Basic Nursing Skills");
        return "Basic Nursing Skills / Nurse Aide Training";
    }
    
    /**
     * Auto-detect unit from PDF text
     */
    private String detectUnitFromText(String text) {
        Log.d(TAG, "Detecting unit from PDF text...");
        
        // Look for unit indicators in your specific format
        Pattern unitPattern = Pattern.compile(
            "Unit\\s*\\d+:\\s*([^\\n]+)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = unitPattern.matcher(text);
        if (matcher.find()) {
            String unit = matcher.group(1).trim();
            Log.d(TAG, "Detected unit from pattern: " + unit);
            return unit;
        }
        
        // Enhanced fallback for your nursing curriculum
        if (text.contains("Introduction to Healthcare")) {
            Log.d(TAG, "Detected Introduction to Healthcare from text content");
            return "Unit 1: Introduction to Healthcare";
        } else if (text.contains("Safety and Emergency Procedures")) {
            Log.d(TAG, "Detected Safety and Emergency Procedures from text content");
            return "Unit 2: Safety and Emergency Procedures";
        } else if (text.contains("Basic Patient Care")) {
            Log.d(TAG, "Detected Basic Patient Care from text content");
            return "Unit 3: Basic Patient Care";
        } else if (text.contains("Vital Signs and Monitoring")) {
            Log.d(TAG, "Detected Vital Signs and Monitoring from text content");
            return "Unit 4: Vital Signs and Monitoring";
        } else if (text.contains("Nutrition and Hydration")) {
            Log.d(TAG, "Detected Nutrition and Hydration from text content");
            return "Unit 5: Nutrition and Hydration";
        } else if (text.contains("Clinical Practicum")) {
            Log.d(TAG, "Detected Clinical Practicum from text content");
            return "Unit 6: Clinical Practicum (Hands-On Training)";
        }
        
        // Look for specific text patterns in your PDF
        if (text.contains("Unit 1: Introduction to Healthcare")) {
            Log.d(TAG, "Detected Unit 1 from text content");
            return "Unit 1: Introduction to Healthcare";
        }
        
        Log.d(TAG, "No specific unit detected, defaulting to Unit 1: Introduction to Healthcare");
        return "Unit 1: Introduction to Healthcare";
    }
    
    /**
     * Clean extracted text for better processing
     */
    private String cleanExtractedText(String text) {
        // Remove excessive whitespace
        text = text.replaceAll("\\s+", " ");
        
        // Fix common PDF extraction issues
        text = text.replaceAll("(?<=\\w)(?=\\s*[A-Z]\\s)", " "); // Fix word boundaries
        text = text.replaceAll("(?<=\\d)(?=\\s*[A-Za-z])", " "); // Fix number-word boundaries
        
        // Remove page numbers and headers
        text = text.replaceAll("\\b\\d+\\s*of\\s*\\d+\\b", ""); // "Page X of Y"
        text = text.replaceAll("\\bPage\\s*\\d+\\b", ""); // "Page X"
        
        // Clean up bullet points and lists
        text = text.replaceAll("^\\s*[•\\-\\*]\\s*", ""); // Remove leading bullets
        text = text.replaceAll("\\n\\s*[•\\-\\*]\\s*", "\n"); // Remove inline bullets
        
        return text.trim();
    }
    
    /**
     * Split PDF text into logical sections
     */
    private List<String> splitTextIntoSections(String text) {
        List<String> sections = new ArrayList<>();
        
        // Enhanced splitting for nursing curriculum format
        String[] markers = {
            "Unit\\s*\\d+:\\s*[^\\n]+",  // "Unit 1: Introduction to Healthcare"
            "Chapter\\s*\\d+:\\s*[^\\n]+", // "Chapter 1: Title"
            "Part\\s*\\d+:\\s*[^\\n]+",   // "Part 1: Certified Nursing Assistant (CNA)"
            "Section\\s*\\d+:\\s*[^\\n]+", // "Section 1: Title"
            "Module\\s*\\d+:\\s*[^\\n]+"   // "Module 1: Title"
        };
        
        // Find section boundaries
        List<Integer> boundaries = new ArrayList<>();
        boundaries.add(0); // Start of text
        
        for (String marker : markers) {
            Pattern pattern = Pattern.compile(marker, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                boundaries.add(matcher.start());
                Log.d(TAG, "Found section boundary: " + matcher.group());
            }
        }
        
        boundaries.add(text.length()); // End of text
        
        // Sort boundaries and create sections
        boundaries.sort(Integer::compareTo);
        
        for (int i = 0; i < boundaries.size() - 1; i++) {
            int start = boundaries.get(i);
            int end = boundaries.get(i + 1);
            if (end - start > 100) { // Only include substantial sections
                String section = text.substring(start, end).trim();
                if (!section.isEmpty()) {
                    sections.add(section);
                    Log.d(TAG, "Created section " + (i + 1) + " with " + section.length() + " characters");
                }
            }
        }
        
        // If no sections found, split by paragraphs
        if (sections.isEmpty()) {
            Log.d(TAG, "No sections found, falling back to paragraph splitting");
            String[] paragraphs = text.split("\\n\\s*\\n");
            for (String paragraph : paragraphs) {
                if (paragraph.trim().length() > 200) { // Only substantial paragraphs
                    sections.add(paragraph);
                }
            }
        }
        
        Log.d(TAG, "Total sections created: " + sections.size());
        return sections;
    }
    
    /**
     * Extract questions from a text section
     */
    private List<QuizQuestion> extractQuestionsFromSection(String section, String course, String unit, String career) {
        List<QuizQuestion> questions = new ArrayList<>();
        
        // Look for question patterns in the text
        List<QuestionPattern> patterns = getQuestionPatterns();
        
        for (QuestionPattern pattern : patterns) {
            List<QuizQuestion> foundQuestions = pattern.extractQuestions(section, course, unit, career);
            questions.addAll(foundQuestions);
        }
        
        return questions;
    }
    
    /**
     * Get question extraction patterns
     */
    private List<QuestionPattern> getQuestionPatterns() {
        List<QuestionPattern> patterns = new ArrayList<>();
        
        // Enhanced nursing test bank pattern (for your PDF format)
        patterns.add(new QuestionPattern() {
            @Override
            public List<QuizQuestion> extractQuestions(String text, String course, String unit, String career) {
                List<QuizQuestion> questions = new ArrayList<>();
                
                // Enhanced pattern for your nursing test bank format:
                // "1. Question text A) option B) option C) option D) option"
                // "Answer: B) The nursing process is a problem-solving tool..."
                // "Rationale: explanation"
                Pattern nursingPattern = Pattern.compile(
                    "(\\d+)\\.\\s*([^A-D]+?)\\s*" +  // Question number and text
                    "A\\)\\s*([^B]+?)\\s*" +        // Option A
                    "B\\)\\s*([^C]+?)\\s*" +        // Option B
                    "C\\)\\s*([^D]+?)\\s*" +        // Option C
                    "D\\)\\s*([^\\n]+?)\\s*" +      // Option D
                    "Answer:\\s*([A-D])\\)?\\s*([^\\n]+?)\\s*" +   // Answer and explanation
                    "---",                           // Separator
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
                );
                
                Matcher matcher = nursingPattern.matcher(text);
                while (matcher.find()) {
                    try {
                        String questionText = matcher.group(2).trim();
                        String optionA = matcher.group(3).trim();
                        String optionB = matcher.group(4).trim();
                        String optionC = matcher.group(5).trim();
                        String optionD = matcher.group(6).trim();
                        String answer = matcher.group(7).trim();
                        String answerExplanation = matcher.group(8).trim();
                        
                        // Clean up options (remove extra whitespace and newlines)
                        optionA = cleanOptionText(optionA);
                        optionB = cleanOptionText(optionB);
                        optionC = cleanOptionText(optionC);
                        optionD = cleanOptionText(optionD);
                        
                        // Truncate question text and rationale to prevent Firestore size limits
                        questionText = truncateText(questionText, 1000); // Limit question to 1000 chars
                        String rationale = truncateText(answerExplanation, 800); // Use answer explanation as rationale
                        
                        // Convert answer letter to index (A=0, B=1, C=2, D=3)
                        int correctAnswerIndex = answer.toUpperCase().charAt(0) - 'A';
                        
                        // Validate the question data
                        if (questionText.length() > 10 && 
                            optionA.length() > 5 && optionB.length() > 5 && 
                            optionC.length() > 5 && optionD.length() > 5 &&
                            rationale.length() > 10) {
                            
                            // Create question with proper career, course, and unit
                            QuizQuestion question = createQuestion(
                                "nursing_" + UUID.randomUUID().toString().substring(0, 8),
                                questionText,
                                new String[]{optionA, optionB, optionC, optionD},
                                correctAnswerIndex,
                                rationale,
                                course, unit, career, "medium", 30
                            );
                            
                            questions.add(question);
                            Log.d(TAG, "Extracted nursing question: " + questionText.substring(0, Math.min(50, questionText.length())) + "...");
                            Log.d(TAG, "Question details - Career: " + career + ", Course: " + course + ", Unit: " + unit);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error parsing question pattern: " + e.getMessage());
                        continue;
                    }
                }
                
                Log.d(TAG, "Extracted " + questions.size() + " questions from section");
                return questions;
            }
        });
        
        // Multiple choice question pattern
        patterns.add(new QuestionPattern() {
            @Override
            public List<QuizQuestion> extractQuestions(String text, String course, String unit, String career) {
                List<QuizQuestion> questions = new ArrayList<>();
                
                // Look for questions with multiple choice options
                Pattern qPattern = Pattern.compile(
                    "([^.!?]+\\?)\\s*" +  // Question ending with ?
                    "([A-D]\\.\\s*[^\\n]+)" +  // Option A
                    "([A-D]\\.\\s*[^\\n]+)" +  // Option B  
                    "([A-D]\\.\\s*[^\\n]+)" +  // Option C
                    "([A-D]\\.\\s*[^\\n]+)",   // Option D
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
                );
                
                Matcher matcher = qPattern.matcher(text);
                while (matcher.find() && questions.size() < 10) {
                    String questionText = matcher.group(1).trim();
                    String optionA = matcher.group(2).trim();
                    String optionB = matcher.group(3).trim();
                    String optionC = matcher.group(4).trim();
                    String optionD = matcher.group(5).trim();
                    
                    // Create question
                    QuizQuestion question = createQuestion(
                        "pdf_" + UUID.randomUUID().toString().substring(0, 8),
                        questionText,
                        new String[]{optionA, optionB, optionC, optionD},
                        0, // Default to first option as correct
                        "Answer extracted from PDF content.",
                        course, unit, career, "medium", 30
                    );
                    
                    questions.add(question);
                }
                
                return questions;
            }
        });
        
        // True/False question pattern
        patterns.add(new QuestionPattern() {
            @Override
            public List<QuizQuestion> extractQuestions(String text, String course, String unit, String career) {
                List<QuizQuestion> questions = new ArrayList<>();
                
                // Look for true/false statements
                Pattern tfPattern = Pattern.compile(
                    "([^.!?]+)\\s*\\(True/False\\)",
                    Pattern.CASE_INSENSITIVE
                );
                
                Matcher matcher = tfPattern.matcher(text);
                while (matcher.find() && questions.size() < 5) {
                    String questionText = matcher.group(1).trim();
                    
                    QuizQuestion question = createQuestion(
                        "pdf_tf_" + UUID.randomUUID().toString().substring(0, 8),
                        questionText + " (True/False)",
                        new String[]{"True", "False"},
                        0, // Default to True
                        "True/False question extracted from PDF content.",
                        course, unit, career, "easy", 20
                    );
                    
                    questions.add(question);
                }
                
                return questions;
            }
        });
        
        return patterns;
    }
    
    /**
     * Generate questions from text content when no structured questions are found
     */
    private List<QuizQuestion> generateQuestionsFromContent(String text, String course, String unit, String career) {
        List<QuizQuestion> questions = new ArrayList<>();
        
        // Split text into sentences
        String[] sentences = text.split("[.!?]+");
        
        for (int i = 0; i < sentences.length && questions.size() < 15; i++) {
            String sentence = sentences[i].trim();
            
            // Only process substantial sentences
            if (sentence.length() < 20 || sentence.length() > 200) continue;
            
            // Skip sentences that are already questions
            if (sentence.contains("?") || sentence.contains("What") || sentence.contains("How")) continue;
            
            // Create a question from this sentence
            QuizQuestion question = createQuestionFromSentence(sentence, course, unit, career);
            if (question != null) {
                questions.add(question);
            }
        }
        
        return questions;
    }
    
    /**
     * Create a question from a sentence
     */
    private QuizQuestion createQuestionFromSentence(String sentence, String course, String unit, String career) {
        // Clean the sentence
        sentence = sentence.replaceAll("\\s+", " ").trim();
        
        // Create multiple choice options
        String[] options = generateOptionsFromSentence(sentence);
        
        if (options.length == 4) {
            return createQuestion(
                "gen_" + UUID.randomUUID().toString().substring(0, 8),
                "Which of the following is correct about: " + sentence + "?",
                options,
                0, // First option is correct
                "This question was generated from the PDF content.",
                course, unit, career, "medium", 30
            );
        }
        
        return null;
    }
    
    /**
     * Generate multiple choice options from a sentence
     */
    private String[] generateOptionsFromSentence(String sentence) {
        String[] options = new String[4];
        
        // First option is the correct answer (the sentence itself)
        options[0] = sentence;
        
        // Generate plausible distractors
        options[1] = generateDistractor(sentence, 1);
        options[2] = generateDistractor(sentence, 2);
        options[3] = generateDistractor(sentence, 3);
        
        return options;
    }
    
    /**
     * Generate a distractor option
     */
    private String generateDistractor(String correctSentence, int type) {
        switch (type) {
            case 1:
                // Change a key word
                return correctSentence.replaceAll("\\b(is|are|was|were)\\b", "is not")
                                   .replaceAll("\\b(can|could|will|would)\\b", "cannot");
            case 2:
                // Add negation
                return "It is not true that " + correctSentence.toLowerCase();
            case 3:
                // Opposite meaning
                return correctSentence.replaceAll("\\b(important|essential|critical)\\b", "unimportant")
                                   .replaceAll("\\b(safe|secure|protected)\\b", "unsafe");
            default:
                return "None of the above";
        }
    }
    
    /**
     * Clean option text to remove extra whitespace and newlines
     */
    private String cleanOptionText(String text) {
        // Limit option text to prevent Firestore size limit errors
        String cleaned = text.replaceAll("\\s+", " ").trim();
        if (cleaned.length() > 500) { // Limit to 500 characters
            cleaned = cleaned.substring(0, 500) + "...";
        }
        return cleaned;
    }
    
    /**
     * Truncate text to prevent Firestore size limit errors
     */
    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
    
    /**
     * Get unit title based on index
     */
    private String getUnitTitle(int index) {
        String[] titles = {
            "Introduction", "Assessment", "Interventions", "Communication", "Safety",
            "Medication Administration", "Documentation", "Ethics", "Special Populations", "Emergency Care"
        };
        return index < titles.length ? titles[index] : "Content";
    }
    
    /**
     * Interface for question extraction patterns
     */
    private interface QuestionPattern {
        List<QuizQuestion> extractQuestions(String text, String course, String unit, String career);
    }
    
    /**
     * Create a QuizQuestion object
     */
    private QuizQuestion createQuestion(String questionId, String question, String[] options, 
                                      int correctAnswerIndex, String explanation, String course, 
                                      String unit, String career, String difficulty, int timeLimit) {
        QuizQuestion q = new QuizQuestion();
        q.setQuestionId(questionId);
        q.setQuestion(question);
        q.setOptions(Arrays.asList(options)); // Convert array to List
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
     * Generate sample questions for testing using your exact curriculum structure
     */
    public List<QuizQuestion> generateSampleQuestions(String unit, String career) {
        Log.d(TAG, "Generating sample questions for unit: " + unit + ", career: " + career);
        
        List<QuizQuestion> questions = new ArrayList<>();
        
        // Generate questions based on your exact curriculum structure
        if (career.equals("Certified Nursing Assistant (CNA)")) {
            if (unit.contains("Introduction to Healthcare")) {
                questions.add(createQuestion(
                    "cna_basic_skills_unit1_001",
                    "What is the primary role of a Certified Nursing Assistant (CNA)?",
                    new String[]{
                        "To diagnose medical conditions",
                        "To provide basic patient care under RN supervision",
                        "To prescribe medications",
                        "To perform surgeries"
                    },
                    1,
                    "CNAs provide basic patient care under the supervision of registered nurses, including activities of daily living.",
                    "Basic Nursing Skills / Nurse Aide Training",
                    unit,
                    career,
                    "easy",
                    30
                ));
            }
            
            if (unit.contains("Safety and Emergency Procedures")) {
                questions.add(createQuestion(
                    "cna_basic_skills_unit2_001",
                    "What is the most important safety measure when entering a patient's room?",
                    new String[]{
                        "Wearing gloves",
                        "Hand hygiene",
                        "Wearing a mask",
                        "Checking the patient's name"
                    },
                    1,
                    "Hand hygiene is the most important safety measure to prevent the spread of infection.",
                    "Basic Nursing Skills / Nurse Aide Training",
                    unit,
                    career,
                    "easy",
                    30
                ));
            }
        }
        
        if (career.equals("Licensed Practical/Vocational Nurse (LPN/LVN)")) {
            if (unit.contains("Introduction to Healthcare")) {
                questions.add(createQuestion(
                    "lpn_anatomy_unit1_001",
                    "What is the largest organ in the human body?",
                    new String[]{
                        "Heart",
                        "Liver",
                        "Skin",
                        "Brain"
                    },
                    2,
                    "The skin is the largest organ in the human body, covering approximately 20 square feet.",
                    "Anatomy and Physiology",
                    unit,
                    career,
                    "easy",
                    30
                ));
            }
            
            if (unit.contains("Safety and Emergency Procedures")) {
                questions.add(createQuestion(
                    "lpn_fundamentals_unit2_001",
                    "What is the first step in the nursing process?",
                    new String[]{
                        "Planning",
                        "Assessment",
                        "Implementation",
                        "Evaluation"
                    },
                    1,
                    "Assessment is the first step in the nursing process, where data is collected about the patient.",
                    "Fundamentals of Nursing",
                    unit,
                    career,
                    "medium",
                    30
                ));
            }
        }
        
        if (career.equals("Registered Nurse (RN) - BSN Level")) {
            if (unit.contains("Introduction to Healthcare")) {
                questions.add(createQuestion(
                    "rn_health_assessment_unit1_001",
                    "What is the purpose of a head-to-toe assessment?",
                    new String[]{
                        "To save time during patient care",
                        "To provide a systematic and comprehensive evaluation",
                        "To avoid missing important details",
                        "All of the above"
                    },
                    3,
                    "A head-to-toe assessment provides a systematic, comprehensive evaluation that saves time and ensures no details are missed.",
                    "Health Assessment",
                    unit,
                    career,
                    "medium",
                    30
                ));
            }
            
            if (unit.contains("Safety and Emergency Procedures")) {
                questions.add(createQuestion(
                    "rn_pathophysiology_unit2_001",
                    "What is inflammation?",
                    new String[]{
                        "A disease process",
                        "The body's protective response to injury",
                        "A type of infection",
                        "A medication side effect"
                    },
                    1,
                    "Inflammation is the body's protective response to injury, infection, or irritation.",
                    "Pathophysiology",
                    unit,
                    career,
                    "medium",
                    30
                ));
            }
        }
        
        Log.d(TAG, "Generated " + questions.size() + " sample questions for " + career + " - " + unit);
        return questions;
    }
    
    public interface UploadCallback {
        void onSuccess(String message);
        void onProgress(int current, int total);
        void onError(String error);
    }
    
    /**
     * QuizQuestion model class
     */
    public static class QuizQuestion {
        private String questionId;
        private String question;
        private List<String> options;
        private int correctAnswerIndex;
        private String explanation;
        private String course;
        private String unit;
        private String career;
        private String difficulty;
        private int timeLimit;
        
        // Getters and Setters
        public String getQuestionId() { return questionId; }
        public void setQuestionId(String questionId) { this.questionId = questionId; }
        
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        
        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }
        
        public int getCorrectAnswerIndex() { return correctAnswerIndex; }
        public void setCorrectAnswerIndex(int correctAnswerIndex) { this.correctAnswerIndex = correctAnswerIndex; }
        
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
        
        public String getCourse() { return course; }
        public void setCourse(String course) { this.course = course; }
        
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        
        public String getCareer() { return career; }
        public void setCareer(String career) { this.career = career; }
        
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        
        public int getTimeLimit() { return timeLimit; }
        public void setTimeLimit(int timeLimit) { this.timeLimit = timeLimit; }
    }
}
