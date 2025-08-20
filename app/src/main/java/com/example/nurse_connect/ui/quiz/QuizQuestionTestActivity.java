package com.example.nurse_connect.ui.quiz;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nurse_connect.databinding.ActivityQuizQuestionTestBinding;
import com.example.nurse_connect.models.QuizQuestion;
import com.example.nurse_connect.services.QuizQuestionExtractor;

import java.util.List;

public class QuizQuestionTestActivity extends AppCompatActivity {
    private static final String TAG = "QuizQuestionTest";
    private ActivityQuizQuestionTestBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuizQuestionTestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupUI();
    }

    private void setupUI() {
        binding.btnTestExtraction.setOnClickListener(v -> testExtraction());
        binding.btnClearResults.setOnClickListener(v -> clearResults());
    }

    private void testExtraction() {
        binding.btnTestExtraction.setEnabled(false);
        binding.tvResults.setText("Testing extraction...");

        // Sample text that matches your PDF format
        String sampleText = 
            "1. What is the primary responsibility of a CNA? a) Diagnosing medical conditions b) Assisting patients with activities of daily living (ADLs) c) Prescribing medication d) Performing surgical procedures\n" +
            "Answer: b)\n" +
            "Rationale: CNAs are trained to provide direct patient care, which includes helping with ADLs like bathing, dressing, and eating. Diagnosis, prescription, and surgery are roles for licensed nurses and physicians.\n\n" +
            "2. The law that protects patient confidentiality is known as: a) OSHA b) FDA c) HIPAA d) CDC\n" +
            "Answer: c)\n" +
            "Rationale: The Health Insurance Portability and Accountability Act (HIPAA) sets the standard for protecting sensitive patient health information.\n\n" +
            "3. Which of the following is an example of objective information? a) The patient says they feel dizzy. b) The patient's blood pressure is 120/80 mmHg. c) The patient's wife says he was confused. d) The patient complains of a headache.\n" +
            "Answer: b)\n" +
            "Rationale: Objective data is measurable and observable (a fact), like a vital sign. Subjective data is what the patient reports or feels (a symptom).";

        // Test the parsing logic directly
        List<QuizQuestion> questions = QuizQuestionExtractor.parseQuestionsFromText(sampleText, 
            "Certified Nursing Assistant (CNA)", 
            "Basic Nursing Skills / Nurse Aide Training", 
            "Unit 1: Introduction to Healthcare");

        binding.btnTestExtraction.setEnabled(true);

        if (questions.isEmpty()) {
            binding.tvResults.setText("❌ No questions extracted. Check the parsing logic.");
            return;
        }

        // Display results
        StringBuilder results = new StringBuilder();
        results.append("✅ Successfully extracted ").append(questions.size()).append(" questions!\n\n");

        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion q = questions.get(i);
            results.append("Question ").append(i + 1).append(":\n");
            results.append("Text: ").append(q.getQuestion()).append("\n");
            results.append("Options: ").append(q.getOptions().size()).append("\n");
            results.append("Correct: ").append((char)('a' + q.getCorrectAnswerIndex())).append(")\n");
            results.append("Career: ").append(q.getCareer()).append("\n");
            results.append("Course: ").append(q.getCourse()).append("\n");
            results.append("Unit: ").append(q.getUnit()).append("\n");
            if (q.getExplanation() != null) {
                results.append("Explanation: ").append(q.getExplanation()).append("\n");
            }
            results.append("\n");
        }

        binding.tvResults.setText(results.toString());
        Log.d(TAG, "Test extraction completed with " + questions.size() + " questions");
    }

    private void clearResults() {
        binding.tvResults.setText("");
    }

    // Make the parseQuestionsFromText method accessible for testing
    private static List<QuizQuestion> parseQuestionsFromText(String text, String career, String course, String unit) {
        return QuizQuestionExtractor.parseQuestionsFromText(text, career, course, unit);
    }
}
