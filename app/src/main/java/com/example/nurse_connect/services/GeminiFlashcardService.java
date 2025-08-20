package com.example.nurse_connect.services;

import android.util.Log;

import com.example.nurse_connect.models.Flashcard;
import com.example.nurse_connect.models.FlashcardGameMode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Gemini AI-powered Flashcard Generation Service (REST via Retrofit)
 */
public class GeminiFlashcardService {
	private static final String TAG = "GeminiFlashcardService";
	private static final String MODEL_NAME = "gemini-1.5-flash";
	private static final String API_KEY = "AIzaSyBQzJmJeFvVb6O5nLX1lAWXhaRtelMowrQ";
	
	// Rate limiting configuration
	private static final int MAX_RETRIES = 5; // Increased retries
	private static final long BASE_DELAY_MS = 2000; // 2 seconds base delay
	private static final long MAX_DELAY_MS = 60000; // 60 seconds max delay
	
	// Rate limiting state
	private final AtomicInteger requestCount = new AtomicInteger(0);
	private final AtomicLong lastRequestTime = new AtomicLong(0);
	private final Object rateLimitLock = new Object();
	
	private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
	private final Gson gson;
	private final GeminiApi api;
	
	// Store the last raw AI response for debugging/access
	private String lastRawResponse = null;

	public interface FlashcardGenerationCallback {
		void onFlashcardsGenerated(List<Flashcard> flashcards);
		void onError(String error);
	}
	
	/**
	 * Get the last raw AI response for debugging purposes
	 */
	public String getLastRawResponse() {
		return lastRawResponse;
	}
	
	/**
	 * Interface for accessing raw AI response
	 */
	public interface RawResponseCallback {
		void onRawResponseReceived(String rawResponse);
		void onError(String error);
	}

	public GeminiFlashcardService() {
		// Create a more lenient Gson parser to handle malformed JSON from AI
		gson = new GsonBuilder()
			.setLenient()
			.create();
		HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Log.d(TAG, message));
		logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

		OkHttpClient httpClient = new OkHttpClient.Builder()
				.addInterceptor(logging)
				.build();

		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl("https://generativelanguage.googleapis.com/")
				.client(httpClient)
				.addConverterFactory(GsonConverterFactory.create(gson))
				.build();

		api = retrofit.create(GeminiApi.class);
		Log.d(TAG, "Gemini AI service initialized successfully");
	}

	// Retrofit API
	interface GeminiApi {
		@POST("v1beta/models/{model}:generateContent")
		Call<GenerateContentResponseDto> generateContent(
				@Path("model") String model,
				@Query("key") String apiKey,
				@Body GenerateContentRequestDto request
		);
	}

	// Request DTOs
	static class GenerateContentRequestDto {
		List<ContentDto> contents;
		GenerationConfigDto generationConfig;
		GenerateContentRequestDto(List<ContentDto> contents, GenerationConfigDto gen) {
			this.contents = contents;
			this.generationConfig = gen;
		}
	}
	static class ContentDto { String role; List<PartDto> parts; }
	static class PartDto { String text; }
	static class GenerationConfigDto { Double temperature; Integer maxOutputTokens; }

	// Response DTOs (minimal for text extraction)
	static class GenerateContentResponseDto { List<CandidateDto> candidates; }
	static class CandidateDto { ContentOutDto content; }
	static class ContentOutDto { List<PartOutDto> parts; }
	static class PartOutDto { String text; }

	/**
	 * Public API: Generate flashcards for a topic
	 */
	public void generateFlashcards(String career, String course, String unit, int count, FlashcardGenerationCallback callback) {
		Log.d(TAG, "Generating " + count + " flashcards for " + career + " - " + course + " - " + unit);

		// Use the enhanced prompt that includes game mode and difficulty
		String prompt = buildPrompt(career, course, unit, count, FlashcardGameMode.STUDY_MODE, "Medium", "No time limit");

		PartDto part = new PartDto();
		part.text = prompt;
		ContentDto content = new ContentDto();
		content.role = "user";
		content.parts = Collections.singletonList(part);

		GenerationConfigDto gen = new GenerationConfigDto();
		gen.temperature = 0.7; // balanced randomness
		gen.maxOutputTokens = 2048;

		GenerateContentRequestDto req = new GenerateContentRequestDto(Collections.singletonList(content), gen);

		api.generateContent(MODEL_NAME, API_KEY, req).enqueue(new Callback<GenerateContentResponseDto>() {
			@Override public void onResponse(Call<GenerateContentResponseDto> call, retrofit2.Response<GenerateContentResponseDto> response) {
				if (!response.isSuccessful() || response.body() == null) {
					String err = "Gemini response error: " + response.code();
					Log.e(TAG, err);
					callback.onError(err);
					return;
				}
				String text = extractText(response.body());
				if (text == null || text.trim().isEmpty()) {
					callback.onError("Empty AI response");
					return;
				}
				try {
					List<Flashcard> flashcards = parseJsonFlashcards(text, career, course, unit);
					if (flashcards.isEmpty()) {
						callback.onError("Failed to parse AI JSON");
					} else {
						callback.onFlashcardsGenerated(flashcards);
					}
				} catch (Exception e) {
					Log.e(TAG, "Parse error", e);
					callback.onError("Parse error: " + e.getMessage());
				}
			}
			@Override public void onFailure(Call<GenerateContentResponseDto> call, Throwable t) {
				Log.e(TAG, "Gemini call failed", t);
				callback.onError("Gemini call failed: " + t.getMessage());
			}
		});
	}

	public void generateFlashcardsWithAI(String career, String course, String unit, int count,
										 FlashcardGameMode gameMode, String difficulty, String timeLimit,
										 FlashcardService.FlashcardCallback callback) {
		// Call the overloaded method with empty history
		generateFlashcardsWithAI(career, course, unit, count, gameMode, difficulty, timeLimit, new ArrayList<>(), callback);
	}
	
	public void generateFlashcardsWithAI(String career, String course, String unit, int count,
										 FlashcardGameMode gameMode, String difficulty, String timeLimit,
										 List<String> userHistory, FlashcardService.FlashcardCallback callback) {
		Log.d(TAG, "Generating " + count + " flashcards with Gemini AI for " + career + " - " + course + " - " + unit);
		Log.d(TAG, "User history count: " + (userHistory != null ? userHistory.size() : 0));
		
		// Start with attempt 1
		generateFlashcardsWithAIInternal(career, course, unit, count, gameMode, difficulty, timeLimit, userHistory, callback, 1);
	}
	
	/**
	 * Generate flashcards and also provide access to the raw AI response
	 */
	public void generateFlashcardsWithRawResponse(String career, String course, String unit, int count,
												FlashcardGameMode gameMode, String difficulty, String timeLimit,
												List<String> userHistory, 
												FlashcardService.FlashcardCallback flashcardCallback,
												RawResponseCallback rawResponseCallback) {
		Log.d(TAG, "Generating flashcards with raw response access for " + career + " - " + course + " - " + unit);
		
		// Use enhanced prompt with memory integration
		executorService.execute(() -> {
			try {
				// Use the provided user history to avoid repetition
				String prompt = buildEnhancedPromptWithMemory(career, course, unit, count, gameMode, difficulty, timeLimit, userHistory);
				
				Log.d(TAG, "Generating flashcards with enhanced prompt for " + career + " - " + course + " - " + unit);
				Log.d(TAG, "Prompt length: " + prompt.length() + " characters");
				Log.d(TAG, "Prompt preview: " + prompt.substring(0, Math.min(500, prompt.length())));
				
				// Make API call with optimized parameters
				PartDto part = new PartDto();
				part.text = prompt;
				ContentDto content = new ContentDto();
				content.role = "user";
				content.parts = Collections.singletonList(part);
				
				GenerationConfigDto gen = new GenerationConfigDto();
				gen.temperature = 0.9; // Higher temperature for more randomness and creativity
				gen.maxOutputTokens = 6000; // Increased tokens for better quality
				
				GenerateContentRequestDto req = new GenerateContentRequestDto(Collections.singletonList(content), gen);
				
				Call<GenerateContentResponseDto> call = api.generateContent(MODEL_NAME, API_KEY, req);
				
				call.enqueue(new Callback<GenerateContentResponseDto>() {
					@Override
					public void onResponse(Call<GenerateContentResponseDto> call, Response<GenerateContentResponseDto> response) {
						Log.d(TAG, "=== AI API RESPONSE RECEIVED ===");
						Log.d(TAG, "Response code: " + response.code());
						Log.d(TAG, "Response successful: " + response.isSuccessful());
						Log.d(TAG, "Response body is null: " + (response.body() == null));
						
						if (response.isSuccessful() && response.body() != null) {
							Log.d(TAG, "Response body candidates count: " + (response.body().candidates != null ? response.body().candidates.size() : "null"));
							if (response.body().candidates != null && !response.body().candidates.isEmpty()) {
								Log.d(TAG, "First candidate content: " + response.body().candidates.get(0).content);
								if (response.body().candidates.get(0).content != null && response.body().candidates.get(0).content.parts != null) {
									Log.d(TAG, "First candidate parts count: " + response.body().candidates.get(0).content.parts.size());
								}
							}
							
							String text = extractText(response.body());
							if (text == null || text.trim().isEmpty()) {
								Log.e(TAG, "Extracted text is null or empty");
								flashcardCallback.onError("Empty AI response");
								return;
							}
							
							// Provide raw response to the callback
							if (rawResponseCallback != null) {
								rawResponseCallback.onRawResponseReceived(text);
							}
							
							Log.d(TAG, "Extracted text length: " + text.length());
							Log.d(TAG, "Extracted text preview: " + text.substring(0, Math.min(200, text.length())));
							
							try {
								List<Flashcard> flashcards = parseJsonFlashcards(text, career, course, unit);
								if (flashcards.isEmpty()) {
									Log.e(TAG, "Failed to parse AI JSON - no flashcards generated");
									flashcardCallback.onError("Failed to parse AI JSON");
								} else {
									// Shuffle the flashcards for additional randomness
									Collections.shuffle(flashcards);
									Log.d(TAG, "Successfully generated and shuffled " + flashcards.size() + " flashcards");
									flashcardCallback.onFlashcardsLoaded(flashcards);
								}
							} catch (Exception e) {
								Log.e(TAG, "Error parsing AI response", e);
								flashcardCallback.onError("Failed to parse AI response: " + e.getMessage());
							}
						} else {
							Log.e(TAG, "API response error - code: " + response.code() + ", body null: " + (response.body() == null));
							flashcardCallback.onError("API response error: " + response.code());
						}
						Log.d(TAG, "=== AI API RESPONSE PROCESSING COMPLETE ===");
					}
					
					@Override
					public void onFailure(Call<GenerateContentResponseDto> call, Throwable t) {
						String errorMsg = "Network error: " + t.getMessage();
						flashcardCallback.onError(errorMsg);
						if (rawResponseCallback != null) {
							rawResponseCallback.onError(errorMsg);
						}
					}
				});
				
			} catch (Exception e) {
				String errorMsg = "Generation error: " + e.getMessage();
				Log.e(TAG, errorMsg, e);
				flashcardCallback.onError(errorMsg);
				if (rawResponseCallback != null) {
					rawResponseCallback.onError(errorMsg);
				}
			}
		});
	}

	private String buildJsonPrompt(String career, String course, String unit, int count) {
		// Ask for strict JSON to avoid parsing issues
		return "Generate exactly " + count + " nursing flashcards as valid JSON ONLY. " +
				"Return ONLY a JSON object with a 'flashcards' array. " +
				"Each flashcard must have: question (string), options (array of exactly 4 strings), correctIndex (0-3), rationale (string). " +
				"Topic: career='" + career + "', course='" + course + "', unit='" + unit + "'. " +
				"Make options concise and plausible. " +
				"Return ONLY the JSON, no markdown, no prose, no explanations. " +
				"Example format: {\"flashcards\":[{\"question\":\"What is...\",\"options\":[\"Option A\",\"Option B\",\"Option C\",\"Option D\"],\"correctIndex\":1,\"rationale\":\"Because...\"}]}";
	}

		private String buildPrompt(String career, String course, String unit, int count, 
                               FlashcardGameMode gameMode, String difficulty, String timeLimit) {
		// Ask for strict JSON to avoid parsing issues
		StringBuilder prompt = new StringBuilder();
		prompt.append("Generate ").append(count).append(" nursing flashcards for:\n");
		prompt.append("Career: ").append(career).append("\n");
		prompt.append("Course: ").append(course).append("\n");
		prompt.append("Unit: ").append(unit).append("\n");
		prompt.append("Game Mode: ").append(gameMode.getDisplayName()).append("\n");
		prompt.append("Difficulty: ").append(difficulty).append("\n");
		prompt.append("Time Limit: ").append(timeLimit).append("\n\n");
		
		// Add mode-specific instructions
		switch (gameMode) {
			case TIMED_MODE:
				prompt.append("For timed mode, create questions that can be answered quickly but require knowledge.\n");
				break;
			case QUIZ_MODE:
				prompt.append("For quiz mode, create clear multiple-choice questions with detailed rationales.\n");
				break;
			case SPACED_REPETITION:
				prompt.append("For spaced repetition, create foundational concepts that build upon each other.\n");
				break;
			case MASTERY_MODE:
				prompt.append("For mastery mode, create challenging questions that test deep understanding.\n");
				break;
			case DAILY_CHALLENGE:
				prompt.append("For daily challenge, create engaging questions that motivate daily practice.\n");
				break;
			case MATCHING_MODE:
				prompt.append("For matching mode, create clear term-definition pairs.\n");
				break;
			case SCENARIO_MODE:
				prompt.append("For scenario mode, create realistic clinical scenarios with multiple-choice answers.\n");
				break;
			default:
				prompt.append("Create comprehensive nursing questions with clear explanations.\n");
				break;
		}
		
		// Add difficulty-specific instructions
		switch (difficulty.toLowerCase()) {
			case "easy":
				prompt.append("Difficulty: Basic concepts, straightforward questions, clear answers.\n");
				break;
			case "medium":
				prompt.append("Difficulty: Intermediate concepts, some analysis required, detailed rationales.\n");
				break;
			case "hard":
				prompt.append("Difficulty: Advanced concepts, critical thinking required, complex scenarios.\n");
				break;
			case "expert":
				prompt.append("Difficulty: Expert-level concepts, synthesis required, challenging scenarios.\n");
				break;
		}
		
		prompt.append("\nCRITICAL: Return ONLY valid JSON in this exact format:\n");
		prompt.append("[\n");
		prompt.append("  {\n");
		prompt.append("    \"question\": \"Your question here?\",\n");
		prompt.append("    \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"],\n");
		prompt.append("    \"correctAnswer\": \"Option A\",\n");
		prompt.append("    \"rationale\": \"Explanation of why this answer is correct\"\n");
		prompt.append("  }\n");
		prompt.append("]\n");
		prompt.append("\nIMPORTANT:\n");
		prompt.append("- NO markdown formatting\n");
		prompt.append("- NO additional text or explanations\n");
		prompt.append("- NO trailing commas\n");
		prompt.append("- ONLY the JSON array above\n");
		prompt.append("- Ensure all quotes are properly escaped\n");
		
		Log.d(TAG, "Built prompt: " + prompt.toString());
		return prompt.toString();
	}
	
	private String buildEnhancedPromptWithMemory(String career, String course, String unit, int count, 
			FlashcardGameMode gameMode, String difficulty, String timeLimit, List<String> userHistory) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("Generate EXACTLY ").append(count).append(" UNIQUE and RANDOMIZED high-quality nursing flashcards for:\n");
		prompt.append("Career: ").append(career).append("\n");
		prompt.append("Course: ").append(course).append("\n");
		prompt.append("Unit: ").append(unit).append("\n");
		prompt.append("Game Mode: ").append(gameMode.getDisplayName()).append("\n");
		prompt.append("Difficulty: ").append(difficulty).append("\n");
		prompt.append("Time Limit: ").append(timeLimit).append("\n\n");
		
		// Add randomization instructions
		prompt.append("RANDOMIZATION REQUIREMENTS:\n");
		prompt.append("- Each question must be completely different from typical textbook questions\n");
		prompt.append("- Use real-world scenarios and clinical situations\n");
		prompt.append("- Vary question types: knowledge, application, analysis, evaluation\n");
		prompt.append("- Mix different topics within the unit to avoid repetition\n");
		prompt.append("- Use current nursing practices and evidence-based approaches\n");
		prompt.append("- Generate questions based on current timestamp: ").append(System.currentTimeMillis()).append("\n");
		prompt.append("- Focus on practical, clinical scenarios rather than theoretical concepts\n");
		prompt.append("- Include questions about recent healthcare developments and best practices\n\n");
		
		// Add user history to avoid repetition
		if (userHistory != null && !userHistory.isEmpty()) {
			prompt.append("USER STUDY HISTORY (AVOID THESE TOPICS/QUESTIONS):\n");
			prompt.append("The user has already studied these topics. Generate NEW and DIFFERENT questions:\n");
			for (String historyItem : userHistory) {
				prompt.append("- ").append(historyItem).append("\n");
			}
			prompt.append("\nIMPORTANT: Do NOT repeat any of the above topics or create similar questions.\n");
			prompt.append("Focus on NEW areas within the unit that haven't been covered yet.\n\n");
		}
		
		// Enhanced mode-specific instructions
		switch (gameMode) {
			case STUDY_MODE:
				prompt.append("For STUDY MODE: Create comprehensive questions that build foundational knowledge. ");
				prompt.append("Include detailed rationales that explain the 'why' behind each answer. ");
				prompt.append("Questions should be clear, engaging, and promote deep learning.\n");
				break;
			case TIMED_MODE:
				prompt.append("For TIMED MODE: Create questions that can be answered in 30-60 seconds. ");
				prompt.append("Focus on quick recall and application of knowledge.\n");
				break;
			case QUIZ_MODE:
				prompt.append("For QUIZ MODE: Create clear multiple-choice questions with comprehensive rationales. ");
				prompt.append("Include common misconceptions as distractors.\n");
				break;
			case SPACED_REPETITION:
				prompt.append("For SPACED REPETITION: Create foundational concepts that build upon each other. ");
				prompt.append("Focus on core principles that need regular reinforcement.\n");
				break;
			case MASTERY_MODE:
				prompt.append("For MASTERY MODE: Create challenging questions that test deep understanding. ");
				prompt.append("Include scenario-based questions and critical thinking elements.\n");
				break;
			case DAILY_CHALLENGE:
				prompt.append("For DAILY CHALLENGE: Create engaging questions that motivate daily practice. ");
				prompt.append("Mix easy and challenging questions to maintain motivation.\n");
				break;
			default:
				prompt.append("Create comprehensive nursing questions with clear explanations.\n");
				break;
		}
		
		// Enhanced difficulty-specific instructions
		switch (difficulty.toLowerCase()) {
			case "easy":
				prompt.append("Difficulty: Basic concepts, straightforward questions, clear answers. ");
				prompt.append("Focus on fundamental knowledge and terminology.\n");
				break;
			case "medium":
				prompt.append("Difficulty: Intermediate concepts, some analysis required, detailed rationales. ");
				prompt.append("Include application of knowledge to simple scenarios.\n");
				break;
			case "hard":
				prompt.append("Difficulty: Advanced concepts, critical thinking required, complex scenarios. ");
				prompt.append("Include multi-step reasoning and clinical judgment.\n");
				break;
			case "expert":
				prompt.append("Difficulty: Expert-level concepts, synthesis required, challenging scenarios. ");
				prompt.append("Include complex clinical situations and evidence-based reasoning.\n");
				break;
		}
		
		// Quality enhancement instructions
		prompt.append("\nQUALITY REQUIREMENTS:\n");
		prompt.append("- Questions must be clinically relevant and accurate\n");
		prompt.append("- All options should be plausible and well-distributed\n");
		prompt.append("- Rationales should explain both why the correct answer is right AND why others are wrong\n");
		prompt.append("- Use clear, professional nursing language\n");
		prompt.append("- Avoid ambiguous or overly complex wording\n");
		prompt.append("- Ensure questions test understanding, not just memorization\n\n");
		
		prompt.append("CRITICAL: Return ONLY valid JSON in this exact format:\n");
		prompt.append("[\n");
		prompt.append("  {\n");
		prompt.append("    \"question\": \"Your question here?\",\n");
		prompt.append("    \"options\": [\"First option text here\", \"Second option text here\", \"Third option text here\", \"Fourth option text here\"],\n");
		prompt.append("    \"correctAnswer\": \"EXACT TEXT of the correct option from the options array above\",\n");
		prompt.append("    \"rationale\": \"Detailed explanation of why this answer is correct and why others are incorrect\"\n");
		prompt.append("  }\n");
		prompt.append("]\n");
		
		prompt.append("COUNT REQUIREMENT:\n");
		prompt.append("- You MUST generate EXACTLY ").append(count).append(" flashcards\n");
		prompt.append("- Do NOT generate more or fewer than ").append(count).append(" flashcards\n");
		prompt.append("- Each flashcard must be unique and different from the others\n");
		
		prompt.append("ANSWER POSITIONING REQUIREMENTS:\n");
		prompt.append("- Randomize the position of correct answers (don't always put them first)\n");
		prompt.append("- The correctAnswer field must EXACTLY match one of the options in the options array\n");
		prompt.append("- Distribute correct answers across different positions (0, 1, 2, 3) randomly\n");
		prompt.append("- Ensure all options are plausible but clearly different\n\n");
		prompt.append("\nIMPORTANT:\n");
		prompt.append("- NO markdown formatting\n");
		prompt.append("- NO additional text or explanations\n");
		prompt.append("- NO trailing commas\n");
		prompt.append("- ONLY the JSON array above\n");
		prompt.append("- Ensure all quotes are properly escaped\n");
		prompt.append("- Make questions engaging and memorable\n");
		prompt.append("- Each option must be a complete, meaningful statement\n");
		prompt.append("- Do NOT use generic text like 'Option A', 'Option B', etc.\n");
		prompt.append("- Make all options plausible but clearly different\n");
		
		Log.d(TAG, "Built enhanced prompt with memory integration");
		Log.d(TAG, "Final prompt length: " + prompt.length() + " characters");
		Log.d(TAG, "User history count: " + (userHistory != null ? userHistory.size() : 0));
		return prompt.toString();
	}

	private String extractText(GenerateContentResponseDto body) {
		if (body.candidates == null || body.candidates.isEmpty()) {
			Log.e(TAG, "AI response has no candidates");
			return null;
		}
		
		CandidateDto c = body.candidates.get(0);
		if (c.content == null || c.content.parts == null || c.content.parts.isEmpty()) {
			Log.e(TAG, "AI response candidate has no content or parts");
			return null;
		}
		
		StringBuilder sb = new StringBuilder();
		for (PartOutDto p : c.content.parts) {
			if (p != null && p.text != null) {
				sb.append(p.text);
				Log.d(TAG, "Part text: '" + p.text + "'");
			}
		}
		
		String rawText = sb.toString();
		
		// Store the raw response for later access
		lastRawResponse = rawText;
		
		// Log the COMPLETE raw response
		Log.d(TAG, "=== COMPLETE RAW AI RESPONSE START ===");
		Log.d(TAG, "Raw AI response length: " + rawText.length() + " characters");
		Log.d(TAG, "Raw AI response content:");
		Log.d(TAG, rawText);
		Log.d(TAG, "=== COMPLETE RAW AI RESPONSE END ===");
		
		// Also log in chunks for better readability in logcat
		if (rawText.length() > 1000) {
			Log.d(TAG, "Raw AI response is long, logging in chunks:");
			int chunkSize = 1000;
			for (int i = 0; i < rawText.length(); i += chunkSize) {
				int end = Math.min(i + chunkSize, rawText.length());
				String chunk = rawText.substring(i, end);
				Log.d(TAG, "Chunk " + (i/chunkSize + 1) + " (" + i + "-" + (end-1) + "): " + chunk);
			}
		}
		
		// Additional debugging: Check for JSON structure
		if (rawText.contains("[") && rawText.contains("]")) {
			Log.d(TAG, "AI response contains JSON array markers [ and ]");
		}
		if (rawText.contains("{") && rawText.contains("}")) {
			Log.d(TAG, "AI response contains JSON object markers { and }");
		}
		
		// Check for common AI response patterns
		if (rawText.contains("```json")) {
			Log.d(TAG, "AI response contains markdown JSON code blocks");
		}
		if (rawText.contains("```")) {
			Log.d(TAG, "AI response contains markdown code blocks");
		}
		
		return rawText;
	}

	private List<Flashcard> parseJsonFlashcards(String json, String career, String course, String unit) {
		List<Flashcard> result = new ArrayList<>();
		
		Log.d(TAG, "Starting to parse JSON response for flashcards");
		
		// Try multiple parsing strategies
		result = tryParseJson(json, career, course, unit);
		if (!result.isEmpty()) {
			Log.d(TAG, "Successfully parsed " + result.size() + " flashcards using JSON parsing");
			return result;
		}
		
		Log.w(TAG, "JSON parsing failed, trying structured text parsing");
		
		// If JSON parsing fails, try to extract structured data manually
		result = tryParseStructuredText(json, career, course, unit);
		if (!result.isEmpty()) {
			Log.d(TAG, "Successfully parsed " + result.size() + " flashcards using structured text parsing");
			return result;
		}
		
		// Last resort: generate fallback flashcards
		Log.w(TAG, "All parsing methods failed, generating fallback flashcards");
		result = generateFallbackFlashcards(career, course, unit);
		
		return result;
	}
	
	private List<Flashcard> tryParseJson(String json, String career, String course, String unit) {
		List<Flashcard> result = new ArrayList<>();
		Log.d(TAG, "=== STARTING JSON PARSING ===");
		Log.d(TAG, "Input JSON length: " + json.length());
		Log.d(TAG, "Input JSON preview: " + json.substring(0, Math.min(300, json.length())));
		
		try {
			// First, try to extract JSON from markdown if present
			String cleanJson = extractJsonFromMarkdown(json);
			Log.d(TAG, "Cleaned JSON length: " + cleanJson.length());
			Log.d(TAG, "Cleaned JSON preview: " + cleanJson.substring(0, Math.min(300, cleanJson.length())));
			
			// Try to parse as a direct JSON array first (since AI seems to be generating arrays)
			Log.d(TAG, "Attempting to parse as JSON array...");
			try {
				JsonArray arr = gson.fromJson(cleanJson, JsonArray.class);
				if (arr != null) {
					Log.d(TAG, "SUCCESS: Parsed as JSON array with " + arr.size() + " elements");
					Log.d(TAG, "First array element: " + arr.get(0));
					return parseFlashcardArray(arr, career, course, unit);
				} else {
					Log.d(TAG, "JSON array parsing returned null");
				}
			} catch (Exception e) {
				Log.d(TAG, "Failed to parse as JSON array: " + e.getMessage());
				Log.d(TAG, "Exception type: " + e.getClass().getSimpleName());
			}
			
			// Try to parse as a JSON object with flashcards array
			Log.d(TAG, "Attempting to parse as JSON object...");
			try {
				JsonObject root = gson.fromJson(cleanJson, JsonObject.class);
				if (root != null) {
					Log.d(TAG, "JSON object parsed successfully");
					Log.d(TAG, "Object keys: " + root.keySet());
					if (root.has("flashcards")) {
						Log.d(TAG, "Found flashcards array in JSON object");
						JsonArray arr = root.getAsJsonArray("flashcards");
						Log.d(TAG, "Flashcards array size: " + arr.size());
						return parseFlashcardArray(arr, career, course, unit);
					} else {
						Log.d(TAG, "No 'flashcards' key found in JSON object");
					}
				} else {
					Log.d(TAG, "JSON object parsing returned null");
				}
			} catch (Exception e) {
				Log.d(TAG, "Failed to parse as JSON object: " + e.getMessage());
				Log.d(TAG, "Exception type: " + e.getClass().getSimpleName());
			}
			
		} catch (Exception e) {
			Log.e(TAG, "JSON parsing failed with exception", e);
		}
		
		Log.d(TAG, "=== JSON PARSING FAILED - RETURNING EMPTY LIST ===");
		return result;
	}
	
	private List<Flashcard> parseFlashcardArray(JsonArray arr, String career, String course, String unit) {
		List<Flashcard> result = new ArrayList<>();
		for (JsonElement el : arr) {
			if (!el.isJsonObject()) continue;
			JsonObject o = el.getAsJsonObject();
			
			String question = safeString(o, "question");
			String rationale = safeString(o, "rationale");
			
			// Handle different possible field names
			JsonArray opts = null;
			if (o.has("options") && o.get("options").isJsonArray()) {
				opts = o.getAsJsonArray("options");
			} else if (o.has("choices") && o.get("choices").isJsonArray()) {
				opts = o.getAsJsonArray("choices");
			}
			
			if (question == null || rationale == null || opts == null || opts.size() < 4) {
				Log.w(TAG, "Skipping invalid flashcard: question=" + question + ", rationale=" + rationale + ", options=" + opts);
				continue;
			}
			
			List<String> options = new ArrayList<>();
			for (int i = 0; i < 4; i++) {
				String option = opts.get(i).getAsString();
				options.add(option);
				Log.d(TAG, "Parsed option " + i + ": '" + option + "'");
			}
			
			// Handle different ways the correct answer might be specified
			String correctAnswer = null;
			if (o.has("correctAnswer")) {
				correctAnswer = safeString(o, "correctAnswer");
			} else if (o.has("correctIndex")) {
				int correctIndex = o.get("correctIndex").getAsInt();
				if (correctIndex >= 0 && correctIndex < options.size()) {
					correctAnswer = options.get(correctIndex);
				}
			}
			
			if (correctAnswer == null) {
				correctAnswer = options.get(0); // Default to first option
			}
			
			// RANDOMIZE the order of options to ensure variety
			List<String> randomizedOptions = new ArrayList<>(options);
			Collections.shuffle(randomizedOptions);
			
			// Find the correct answer in the randomized options
			String randomizedCorrectAnswer = correctAnswer;
			for (String option : randomizedOptions) {
				if (option.equals(correctAnswer)) {
					randomizedCorrectAnswer = option;
					break;
				}
			}
			
			Log.d(TAG, "Creating flashcard with question: '" + question + "', correctAnswer: '" + randomizedCorrectAnswer + "', options count: " + randomizedOptions.size());
			Log.d(TAG, "Original options: " + options);
			Log.d(TAG, "Randomized options: " + randomizedOptions);
			
			Flashcard card = new Flashcard(question, randomizedCorrectAnswer, rationale, career, course, unit);
			card.setOptions(randomizedOptions);
			card.setSource("Gemini AI (Randomized)");
			card.setDifficulty("medium");
			card.setFlashcardId("gemini_" + System.currentTimeMillis() + "_" + result.size());
			result.add(card);
		}
		return result;
	}
	
	private List<Flashcard> tryParseStructuredText(String text, String career, String course, String unit) {
		List<Flashcard> result = new ArrayList<>();
		try {
			// Try to extract structured data from text
			String[] lines = text.split("\n");
			String currentQuestion = null;
			List<String> currentOptions = new ArrayList<>();
			String currentRationale = null;
			
			for (String line : lines) {
				line = line.trim();
				if (line.startsWith("Question:") || line.startsWith("Q:")) {
					// Save previous flashcard if exists
					if (currentQuestion != null && currentOptions.size() >= 4 && currentRationale != null) {
						Flashcard card = createFlashcardFromText(currentQuestion, currentOptions, currentRationale, career, course, unit, result.size());
						result.add(card);
					}
					
					// Start new flashcard
					currentQuestion = line.substring(line.indexOf(":") + 1).trim();
					currentOptions.clear();
					currentRationale = null;
				} else if (line.startsWith("A:") || line.startsWith("Answer:") || line.startsWith("Rationale:")) {
					currentRationale = line.substring(line.indexOf(":") + 1).trim();
				} else if (line.matches("^[A-D]\\.\\s.*")) {
					// Option line like "A. Option text"
					String option = line.substring(line.indexOf(".") + 1).trim();
					currentOptions.add(option);
				}
			}
			
			// Add the last flashcard
			if (currentQuestion != null && currentOptions.size() >= 4 && currentRationale != null) {
				Flashcard card = createFlashcardFromText(currentQuestion, currentOptions, currentRationale, career, course, unit, result.size());
				result.add(card);
			}
			
		} catch (Exception e) {
			Log.e(TAG, "Structured text parsing failed", e);
		}
		return result;
	}
	
	private Flashcard createFlashcardFromText(String question, List<String> options, String rationale, 
											String career, String course, String unit, int index) {
		// Generate a meaningful title based on the unit
		String title = generateTitleFromUnit(unit, index);
		
		Flashcard card = new Flashcard(title, question, options.get(0), rationale, career, course, unit);
		card.setOptions(options);
		card.setSource("Gemini AI (Text Parsed)");
		card.setDifficulty("medium");
		card.setFlashcardId("gemini_text_" + System.currentTimeMillis() + "_" + index);
		return card;
	}
	
	/**
	 * Generate a meaningful title based on the unit name and question index
	 */
	private String generateTitleFromUnit(String unit, int index) {
		if (unit == null || unit.trim().isEmpty()) {
			return "Study Question " + (index + 1);
		}
		
		String cleanUnit = unit.trim();
		String baseTitle;
		
		// Extract the main topic from the unit
		if (cleanUnit.toLowerCase().contains("introduction")) {
			baseTitle = "Introduction to Healthcare";
		} else if (cleanUnit.toLowerCase().contains("basic") || cleanUnit.toLowerCase().contains("fundamental")) {
			baseTitle = "Basic Healthcare Concepts";
		} else if (cleanUnit.toLowerCase().contains("patient") || cleanUnit.toLowerCase().contains("care")) {
			baseTitle = "Patient Care Fundamentals";
		} else if (cleanUnit.toLowerCase().contains("safety") || cleanUnit.toLowerCase().contains("infection")) {
			baseTitle = "Safety & Infection Control";
		} else if (cleanUnit.toLowerCase().contains("communication")) {
			baseTitle = "Healthcare Communication";
		} else if (cleanUnit.toLowerCase().contains("documentation")) {
			baseTitle = "Medical Documentation";
		} else if (cleanUnit.toLowerCase().contains("ethics") || cleanUnit.toLowerCase().contains("legal")) {
			baseTitle = "Healthcare Ethics & Legal";
		} else if (cleanUnit.toLowerCase().contains("emergency") || cleanUnit.toLowerCase().contains("crisis")) {
			baseTitle = "Emergency Response";
		} else if (cleanUnit.toLowerCase().contains("medication") || cleanUnit.toLowerCase().contains("pharmacology")) {
			baseTitle = "Medication Safety";
		} else if (cleanUnit.toLowerCase().contains("assessment") || cleanUnit.toLowerCase().contains("vital")) {
			baseTitle = "Patient Assessment";
		} else if (cleanUnit.toLowerCase().contains("hygiene") || cleanUnit.toLowerCase().contains("grooming")) {
			baseTitle = "Personal Hygiene & Grooming";
		} else if (cleanUnit.toLowerCase().contains("mobility") || cleanUnit.toLowerCase().contains("transfer")) {
			baseTitle = "Patient Mobility & Transfers";
		} else if (cleanUnit.toLowerCase().contains("nutrition") || cleanUnit.toLowerCase().contains("feeding")) {
			baseTitle = "Nutrition & Feeding";
		} else if (cleanUnit.toLowerCase().contains("elimination") || cleanUnit.toLowerCase().contains("toileting")) {
			baseTitle = "Elimination Assistance";
		} else if (cleanUnit.toLowerCase().contains("comfort") || cleanUnit.toLowerCase().contains("pain")) {
			baseTitle = "Patient Comfort & Pain Management";
		} else if (cleanUnit.toLowerCase().contains("rehabilitation") || cleanUnit.toLowerCase().contains("therapy")) {
			baseTitle = "Rehabilitation & Therapy";
		} else if (cleanUnit.toLowerCase().contains("pediatric") || cleanUnit.toLowerCase().contains("child")) {
			baseTitle = "Pediatric Care";
		} else if (cleanUnit.toLowerCase().contains("geriatric") || cleanUnit.toLowerCase().contains("elderly")) {
			baseTitle = "Geriatric Care";
		} else if (cleanUnit.toLowerCase().contains("mental") || cleanUnit.toLowerCase().contains("psychiatric")) {
			baseTitle = "Mental Health Care";
		} else if (cleanUnit.toLowerCase().contains("obstetric") || cleanUnit.toLowerCase().contains("maternity")) {
			baseTitle = "Maternal & Newborn Care";
		} else if (cleanUnit.toLowerCase().contains("surgical") || cleanUnit.toLowerCase().contains("operative")) {
			baseTitle = "Surgical Care";
		} else if (cleanUnit.toLowerCase().contains("cardiac") || cleanUnit.toLowerCase().contains("heart")) {
			baseTitle = "Cardiac Care";
		} else if (cleanUnit.toLowerCase().contains("respiratory") || cleanUnit.toLowerCase().contains("lung")) {
			baseTitle = "Respiratory Care";
		} else if (cleanUnit.toLowerCase().contains("diabetes") || cleanUnit.toLowerCase().contains("endocrine")) {
			baseTitle = "Diabetes & Endocrine Care";
		} else if (cleanUnit.toLowerCase().contains("wound") || cleanUnit.toLowerCase().contains("dressing")) {
			baseTitle = "Wound Care & Dressing";
		} else if (cleanUnit.toLowerCase().contains("ostomy") || cleanUnit.toLowerCase().contains("colostomy")) {
			baseTitle = "Ostomy Care";
		} else if (cleanUnit.toLowerCase().contains("catheter") || cleanUnit.toLowerCase().contains("urinary")) {
			baseTitle = "Catheter Care";
		} else if (cleanUnit.toLowerCase().contains("oxygen") || cleanUnit.toLowerCase().contains("respiratory")) {
			baseTitle = "Oxygen Therapy";
		} else if (cleanUnit.toLowerCase().contains("iv") || cleanUnit.toLowerCase().contains("intravenous")) {
			baseTitle = "IV Therapy & Care";
		} else if (cleanUnit.toLowerCase().contains("specimen") || cleanUnit.toLowerCase().contains("lab")) {
			baseTitle = "Specimen Collection & Lab";
		} else if (cleanUnit.toLowerCase().contains("death") || cleanUnit.toLowerCase().contains("dying")) {
			baseTitle = "End-of-Life Care";
		} else {
			// For any other units, create a title from the unit name
			String[] words = cleanUnit.split("\\s+");
			if (words.length > 0) {
				// Capitalize first letter of each word and join
				StringBuilder title = new StringBuilder();
				for (String word : words) {
					if (!word.isEmpty()) {
						title.append(Character.toUpperCase(word.charAt(0)))
							  .append(word.substring(1).toLowerCase())
							  .append(" ");
					}
				}
				baseTitle = title.toString().trim();
			} else {
				baseTitle = cleanUnit;
			}
		}
		
		// Add question number for variety
		return baseTitle + " - Question " + (index + 1);
	}

	private List<Flashcard> generateFallbackFlashcards(String career, String course, String unit) {
		List<Flashcard> result = new ArrayList<>();
		
		Log.w(TAG, "Generating fallback flashcards due to parsing failure");
		
		// Generate 10 basic flashcards as fallback with RANDOMIZED correct answer positions
		String[] questions = {
			"What is the primary role of a " + career + " in healthcare?",
			"Which of the following is a key skill for " + course + "?",
			"What safety protocol is essential in " + unit + "?",
			"How should a " + career + " communicate with patients?",
			"What documentation is required for " + unit + " procedures?",
			"What is the most important aspect of patient safety?",
			"How should healthcare workers handle confidential information?",
			"What should you do if you notice a patient's condition worsening?",
			"What is the proper way to wash hands in healthcare?",
			"Why is teamwork important in healthcare settings?"
		};
		
		// More meaningful options instead of generic "Option B", "Option C", etc.
		String[][] allOptions = {
			{
				"Patient care and support",
				"Administrative tasks only", 
				"Financial management",
				"Marketing and sales"
			},
			{
				"Critical thinking and clinical judgment",
				"Basic computer skills only",
				"Advanced mathematics",
				"Foreign language proficiency"
			},
			{
				"Hand hygiene and infection control",
				"Fashion and appearance",
				"Music and entertainment",
				"Food and nutrition"
			},
			{
				"Clear and simple language",
				"Technical medical jargon",
				"Slang and informal terms",
				"Abbreviations only"
			},
			{
				"Official medical records",
				"Personal notes only",
				"Social media posts",
				"Shopping lists"
			},
			{
				"Preventing harm to patients",
				"Completing tasks quickly",
				"Following facility policies",
				"Maintaining cleanliness"
			},
			{
				"Keep it strictly confidential",
				"Share with family members",
				"Discuss with other patients",
				"Post on social media"
			},
			{
				"Immediately notify a supervisor",
				"Wait and see if it improves",
				"Try to fix it yourself",
				"Ignore the changes"
			},
			{
				"Wash with soap and water for 20 seconds",
				"Quick rinse with water only",
				"Use hand sanitizer only",
				"Wipe hands on clothing"
			},
			{
				"Improves patient outcomes and safety",
				"Reduces individual workload",
				"Increases facility profits",
				"Makes shifts go faster"
			}
		};
		
		String[] rationales = {
			"Patient care and support is the primary responsibility of healthcare professionals.",
			"Critical thinking and clinical judgment are essential for making sound clinical decisions.",
			"Hand hygiene and infection control are fundamental to preventing infection transmission.",
			"Clear and simple language ensures patient understanding and safety.",
			"Official medical records are required for legal and clinical purposes.",
			"Preventing harm to patients is the fundamental principle of healthcare safety.",
			"Patient confidentiality is a legal and ethical requirement in healthcare.",
			"Immediate notification of changes in patient condition is crucial for patient safety.",
			"Proper hand hygiene requires soap, water, and adequate time to prevent infection.",
			"Teamwork in healthcare leads to better patient outcomes and reduced medical errors."
		};
		
		// Create flashcards with RANDOMIZED correct answer positions
		for (int i = 0; i < questions.length; i++) {
			// Randomize the correct answer position (0-3)
			int correctPosition = (int)(Math.random() * 4);
			
			// Get the correct answer text
			String correctAnswer = allOptions[i][correctPosition];
			
			// Generate meaningful title based on unit
			String title = generateTitleFromUnit(unit, i);
			
			// Create flashcard with randomized correct answer
			Flashcard card = new Flashcard(title, questions[i], correctAnswer, rationales[i], career, course, unit);
			card.setOptions(List.of(allOptions[i]));
			card.setSource("Fallback Generated (Randomized)");
			card.setDifficulty("medium");
			card.setFlashcardId("fallback_" + System.currentTimeMillis() + "_" + i);
			result.add(card);
			
			Log.d(TAG, "Generated fallback flashcard " + i + " with correct answer at position " + correctPosition + ": " + correctAnswer);
		}
		
		return result;
	}

	private String extractJsonFromMarkdown(String text) {
		Log.d(TAG, "=== EXTRACTING JSON FROM MARKDOWN ===");
		Log.d(TAG, "Original text length: " + text.length());
		Log.d(TAG, "Original text starts with: '" + text.substring(0, Math.min(50, text.length())) + "'");
		Log.d(TAG, "Original text ends with: '" + text.substring(Math.max(0, text.length() - 50)) + "'");
		
		// Remove markdown code fences if present
		String cleaned = text.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "");
		Log.d(TAG, "After removing markdown fences, length: " + cleaned.length());
		
		// Remove any leading/trailing whitespace
		cleaned = cleaned.trim();
		Log.d(TAG, "After trimming, length: " + cleaned.length());
		
		Log.d(TAG, "Cleaned text starts with: '" + cleaned.substring(0, Math.min(50, cleaned.length())) + "'");
		Log.d(TAG, "Cleaned text ends with: '" + cleaned.substring(Math.max(0, cleaned.length() - 50)) + "'");
		
		// If it starts with [ and ends with ], it's a JSON array
		if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
			Log.d(TAG, "SUCCESS: Detected JSON array format - starts with [ and ends with ]");
			return cleaned;
		}
		// If it starts with { and ends with }, it's a JSON object
		if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
			Log.d(TAG, "SUCCESS: Detected JSON object format - starts with { and ends with }");
			return cleaned;
		}
		
		// Try to find JSON array boundaries first (since AI seems to be generating arrays)
		int arrayStart = cleaned.indexOf("[");
		int arrayEnd = cleaned.lastIndexOf("]");
		Log.d(TAG, "JSON array boundaries - start: " + arrayStart + ", end: " + arrayEnd);
		if (arrayStart >= 0 && arrayEnd > arrayStart) {
			String extracted = cleaned.substring(arrayStart, arrayEnd + 1);
			Log.d(TAG, "SUCCESS: Extracted JSON array from boundaries, length: " + extracted.length());
			Log.d(TAG, "Extracted array starts with: '" + extracted.substring(0, Math.min(50, extracted.length())) + "'");
			return extracted;
		}
		
		// Try to find JSON object boundaries
		int objStart = cleaned.indexOf("{");
		int objEnd = cleaned.lastIndexOf("}");
		Log.d(TAG, "JSON object boundaries - start: " + objStart + ", end: " + objEnd);
		if (objStart >= 0 && objEnd > objStart) {
			String extracted = cleaned.substring(objStart, objEnd + 1);
			Log.d(TAG, "SUCCESS: Extracted JSON object from boundaries, length: " + extracted.length());
			Log.d(TAG, "Extracted object starts with: '" + extracted.substring(0, Math.min(50, extracted.length())) + "'");
			return extracted;
		}
		
		Log.w(TAG, "Could not extract valid JSON boundaries, returning cleaned text as-is");
		Log.d(TAG, "Final cleaned text length: " + cleaned.length());
		return cleaned;
	}

	private String safeString(JsonObject obj, String key) {
		return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsString() : null;
	}

	// This method will be called from the activity with pre-loaded history
	private List<String> getUserStudyHistory(String career, String course, String unit) {
		// For now, return empty list - history will be passed from the calling activity
		// This avoids async Firebase calls in the service
		return new ArrayList<>();
	}

	/**
	 * Test method to verify AI is working
	 */
	public void testAI(String career, String course, String unit, FlashcardService.FlashcardCallback callback) {
		// First test our JSON parsing with the exact format we saw in logs
		String testJson = "[{\"question\":\"You're assisting a patient with a known latex allergy. Which action demonstrates the BEST infection control practice?\",\"options\":[\"Use latex gloves for all procedures.\",\"Ensure all equipment and supplies are latex-free.\",\"Inform the patient about the risks of latex allergy but proceed with latex gloves if necessary.\",\"Only use latex gloves if other options are unavailable.\"],\"correctAnswer\":\"Ensure all equipment and supplies are latex-free.\",\"rationale\":\"Avoiding latex completely is crucial for patients with latex allergies.\"}]";
		
		Log.d(TAG, "Testing JSON parsing with sample data...");
		try {
			List<Flashcard> testCards = parseJsonFlashcards(testJson, career, course, unit);
			Log.d(TAG, "JSON parsing test successful! Generated " + testCards.size() + " cards");
		} catch (Exception e) {
			Log.e(TAG, "JSON parsing test failed: " + e.getMessage());
		}
		
		String testPrompt = "Generate 2 simple nursing flashcards for " + career + " - " + course + " - " + unit + 
			" in this exact JSON format: [{\"question\":\"What is...?\",\"options\":[\"Option A\",\"Option B\",\"Option C\",\"Option D\"],\"correctAnswer\":\"Option A\",\"rationale\":\"Because...\"}]";
		
		PartDto part = new PartDto();
		part.text = testPrompt;
		ContentDto content = new ContentDto();
		content.role = "user";
		content.parts = Collections.singletonList(part);
		
		GenerationConfigDto gen = new GenerationConfigDto();
		gen.temperature = 0.9;
		gen.maxOutputTokens = 1000;
		
		GenerateContentRequestDto req = new GenerateContentRequestDto(Collections.singletonList(content), gen);
		
		api.generateContent(MODEL_NAME, API_KEY, req).enqueue(new Callback<GenerateContentResponseDto>() {
			@Override
			public void onResponse(Call<GenerateContentResponseDto> call, retrofit2.Response<GenerateContentResponseDto> response) {
				if (response.isSuccessful() && response.body() != null) {
					String text = extractText(response.body());
					Log.d(TAG, "AI Test Response: " + text);
					if (text != null && !text.trim().isEmpty()) {
						try {
							List<Flashcard> flashcards = parseJsonFlashcards(text, career, course, unit);
							callback.onFlashcardsLoaded(flashcards);
						} catch (Exception e) {
							callback.onError("Test parse error: " + e.getMessage());
						}
					} else {
						callback.onError("Empty AI test response");
					}
				} else {
					callback.onError("AI test failed: " + response.code());
				}
			}
			
			@Override
			public void onFailure(Call<GenerateContentResponseDto> call, Throwable t) {
				callback.onError("AI test network error: " + t.getMessage());
			}
		});
	}

	public void cleanup() {
		if (executorService != null) executorService.shutdown();
	}

	/**
	 * Check if we can make a request based on rate limiting
	 */
	private boolean canMakeRequest() {
		synchronized (rateLimitLock) {
			long now = System.currentTimeMillis();
			long timeSinceLastRequest = now - lastRequestTime.get();
			
			// Allow 1 request per second minimum
			if (timeSinceLastRequest < 1000) {
				return false;
			}
			
			// Update last request time
			lastRequestTime.set(now);
			requestCount.incrementAndGet();
			return true;
		}
	}
	
	/**
	 * Calculate exponential backoff delay for retries
	 */
	private long calculateBackoffDelay(int attempt) {
		long delay = BASE_DELAY_MS * (long) Math.pow(2, attempt - 1);
		return Math.min(delay, MAX_DELAY_MS);
	}
	
	/**
	 * Retry API call with exponential backoff
	 */
	private void retryApiCall(String career, String course, String unit, int count,
							 FlashcardGameMode gameMode, String difficulty, String timeLimit,
							 List<String> userHistory, FlashcardService.FlashcardCallback callback,
							 int attempt) {
		if (attempt > MAX_RETRIES) {
			Log.w(TAG, "All retry attempts exhausted, providing fallback flashcards");
			// Provide fallback flashcards instead of just showing an error
			provideFallbackFlashcards(career, course, unit, count, callback);
			return;
		}
		
		long delay = calculateBackoffDelay(attempt);
		Log.d(TAG, "Retrying API call in " + delay + "ms (attempt " + attempt + "/" + MAX_RETRIES + ")");
		
		executorService.schedule(() -> {
			// Recursive call to retry
			generateFlashcardsWithAIInternal(career, course, unit, count, gameMode, difficulty, timeLimit, userHistory, callback, attempt);
		}, delay, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Internal method for generating flashcards with retry logic
	 */
	private void generateFlashcardsWithAIInternal(String career, String course, String unit, int count,
												FlashcardGameMode gameMode, String difficulty, String timeLimit,
												List<String> userHistory, FlashcardService.FlashcardCallback callback,
												int attempt) {
		// Check rate limiting
		if (!canMakeRequest()) {
			Log.d(TAG, "Rate limit check failed, retrying in 1 second...");
			executorService.schedule(() -> {
				generateFlashcardsWithAIInternal(career, course, unit, count, gameMode, difficulty, timeLimit, userHistory, callback, attempt);
			}, 1000, TimeUnit.MILLISECONDS);
			return;
		}
		
		try {
			// Use the provided user history to avoid repetition
			String prompt = buildEnhancedPromptWithMemory(career, course, unit, count, gameMode, difficulty, timeLimit, userHistory);
			
			Log.d(TAG, "Generating flashcards with enhanced prompt for " + career + " - " + course + " - " + unit);
			Log.d(TAG, "Prompt length: " + prompt.length() + " characters");
			Log.d(TAG, "Prompt preview: " + prompt.substring(0, Math.min(500, prompt.length())));
			
			// Make API call with optimized parameters
			PartDto part = new PartDto();
			part.text = prompt;
			ContentDto content = new ContentDto();
			content.role = "user";
			content.parts = Collections.singletonList(part);
			
			GenerationConfigDto gen = new GenerationConfigDto();
			gen.temperature = 0.9; // Higher temperature for more randomness and creativity
			gen.maxOutputTokens = 6000; // Increased tokens for better quality
			
			GenerateContentRequestDto req = new GenerateContentRequestDto(Collections.singletonList(content), gen);
			
			Call<GenerateContentResponseDto> call = api.generateContent(MODEL_NAME, API_KEY, req);
			
			call.enqueue(new Callback<GenerateContentResponseDto>() {
				@Override
				public void onResponse(Call<GenerateContentResponseDto> call, Response<GenerateContentResponseDto> response) {
					Log.d(TAG, "=== AI API RESPONSE RECEIVED ===");
					Log.d(TAG, "Response code: " + response.code());
					Log.d(TAG, "Response successful: " + response.isSuccessful());
					Log.d(TAG, "Response body is null: " + (response.body() == null));
					
					if (response.isSuccessful() && response.body() != null) {
						Log.d(TAG, "Response body candidates count: " + (response.body().candidates != null ? response.body().candidates.size() : "null"));
						if (response.body().candidates != null && !response.body().candidates.isEmpty()) {
							Log.d(TAG, "First candidate content: " + response.body().candidates.get(0).content);
							if (response.body().candidates.get(0).content != null && response.body().candidates.get(0).content.parts != null) {
								Log.d(TAG, "First candidate parts count: " + response.body().candidates.get(0).content.parts.size());
							}
						}
						
						String text = extractText(response.body());
						if (text == null || text.trim().isEmpty()) {
							Log.e(TAG, "Extracted text is null or empty");
							callback.onError("Empty AI response");
							return;
						}
						
						Log.d(TAG, "Extracted text length: " + text.length());
						Log.d(TAG, "Extracted text preview: " + text.substring(0, Math.min(200, text.length())));
						
						try {
							List<Flashcard> flashcards = parseJsonFlashcards(text, career, course, unit);
							if (flashcards.isEmpty()) {
								Log.e(TAG, "Failed to parse AI JSON - no flashcards generated");
								callback.onError("Failed to parse AI JSON");
							} else {
								// Shuffle the flashcards for additional randomness
								Collections.shuffle(flashcards);
								Log.d(TAG, "Successfully generated and shuffled " + flashcards.size() + " flashcards");
								callback.onFlashcardsLoaded(flashcards);
							}
						} catch (Exception e) {
							Log.e(TAG, "Error parsing AI response", e);
							callback.onError("Failed to parse AI response: " + e.getMessage());
						}
					} else {
						// Handle specific error codes
						if (response.code() == 429) {
							// Rate limit exceeded - retry with exponential backoff
							Log.w(TAG, "Rate limit exceeded (429) - retrying with backoff (attempt " + attempt + ")");
							retryApiCall(career, course, unit, count, gameMode, difficulty, timeLimit, userHistory, callback, attempt + 1);
						} else if (response.code() == 403) {
							// API key issues
							Log.e(TAG, "API key error (403) - check API key configuration");
							callback.onError("AI service configuration error. Please contact support.");
						} else if (response.code() >= 500) {
							// Server errors - retry with backoff
							Log.e(TAG, "Server error (" + response.code() + ") - retrying with backoff (attempt " + attempt + ")");
							retryApiCall(career, course, unit, count, gameMode, difficulty, timeLimit, userHistory, callback, attempt + 1);
						} else {
							// Other client errors
							Log.e(TAG, "API response error - code: " + response.code() + ", body null: " + (response.body() == null));
							callback.onError("AI service error: " + response.code());
						}
					}
					Log.d(TAG, "=== AI API RESPONSE PROCESSING COMPLETE ===");
				}
				
				@Override
				public void onFailure(Call<GenerateContentResponseDto> call, Throwable t) {
					// Network errors - retry with backoff
					Log.e(TAG, "Network error, retrying with backoff (attempt " + attempt + ")", t);
					retryApiCall(career, course, unit, count, gameMode, difficulty, timeLimit, userHistory, callback, attempt + 1);
				}
			});
		} catch (Exception e) {
			Log.e(TAG, "Error in generateFlashcardsWithAIInternal", e);
			callback.onError("Internal error: " + e.getMessage());
		}
	}

	/**
	 * Provide fallback sample flashcards when AI service is unavailable
	 */
	private void provideFallbackFlashcards(String career, String course, String unit, int count,
										  FlashcardService.FlashcardCallback callback) {
		Log.d(TAG, "Providing fallback sample flashcards for " + career + " - " + course + " - " + unit);
		Log.d(TAG, "Requested count: " + count + ", will generate exactly this many questions");
		
		List<Flashcard> fallbackCards = new ArrayList<>();
		
		// Generate unit-specific flashcards - ALWAYS generate exactly the requested count
		// Create questions based on the actual unit content
		String unitTitle = unit.replace("Unit ", "").replace(":", "");
		
		// Generate questions based on unit content
		if (unit.toLowerCase().contains("introduction") || unit.toLowerCase().contains("basic")) {
			// Unit 1: Introduction to Healthcare
			addUnitSpecificQuestions(fallbackCards, career, course, unit, unitTitle, "Introduction to Healthcare", count);
		} else if (unit.toLowerCase().contains("documentation") || unit.toLowerCase().contains("reporting")) {
			// Unit 5: Documentation and Reporting
			addUnitSpecificQuestions(fallbackCards, career, course, unit, unitTitle, "Documentation and Reporting", count);
		} else if (unit.toLowerCase().contains("infection") || unit.toLowerCase().contains("control")) {
			// Unit 2: Infection Control
			addUnitSpecificQuestions(fallbackCards, career, course, unit, unitTitle, "Infection Control", count);
		} else if (unit.toLowerCase().contains("vital") || unit.toLowerCase().contains("signs")) {
			// Unit 3: Vital Signs
			addUnitSpecificQuestions(fallbackCards, career, course, unit, unitTitle, "Vital Signs", count);
		} else if (unit.toLowerCase().contains("patient") || unit.toLowerCase().contains("care")) {
			// Unit 4: Patient Care
			addUnitSpecificQuestions(fallbackCards, career, course, unit, unitTitle, "Patient Care", count);
		} else {
			// Generic unit - create questions based on the unit name
			addUnitSpecificQuestions(fallbackCards, career, course, unit, unitTitle, unitTitle, count);
		}
		
		// Ensure we have exactly the requested count
		while (fallbackCards.size() < count) {
			Flashcard genericCard = createGenericQuestion(fallbackCards.size() + 1, career, course, unit, unitTitle);
			fallbackCards.add(genericCard);
		}
		
		Log.d(TAG, "Generated " + fallbackCards.size() + " fallback flashcards for unit: " + unit);
		callback.onFlashcardsLoaded(fallbackCards);
	}
	
	private void addUnitSpecificQuestions(List<Flashcard> cards, String career, String course, String unit, String unitNumber, String unitTitle, int targetCount) {
		// Add unit-specific questions based on the actual unit content
		String[] questions = getUnitQuestions(unitTitle, career);
		String[] answers = getUnitAnswers(unitTitle);
		String[] rationales = getUnitRationales(unitTitle);
		String[][] options = getUnitOptions(unitTitle);
		
		for (int i = 0; i < Math.min(questions.length, targetCount); i++) {
			if (cards.size() >= targetCount) break;
			
			Flashcard card = new Flashcard(
				unitTitle + " - Question " + (i + 1),
				questions[i],
				answers[i],
				rationales[i],
				career, course, unit
			);
			card.setOptions(Arrays.asList(options[i]));
			card.setSource("Fallback Generated (" + unitTitle + ")");
			card.setDifficulty("easy");
			card.setFlashcardId("fallback_" + unitNumber + "_" + System.currentTimeMillis() + "_" + (i + 1));
			cards.add(card);
		}
	}
	
	private Flashcard createGenericQuestion(int questionNumber, String career, String course, String unit, String unitTitle) {
		// Create a generic question if we need more to reach the target count
		Flashcard card = new Flashcard(
			unitTitle + " - Question " + questionNumber,
			"What is an important aspect of " + unitTitle.toLowerCase() + " in " + career + " practice?",
			"Following established protocols and maintaining patient safety",
			"Following protocols ensures consistent, safe care while maintaining patient safety is the foundation of healthcare practice.",
			career, course, unit
		);
		card.setOptions(Arrays.asList(
			"Following established protocols and maintaining patient safety",
			"Ignoring difficult situations",
			"Working as quickly as possible",
			"Avoiding documentation"
		));
		card.setSource("Fallback Generated (Generic)");
		card.setDifficulty("easy");
		card.setFlashcardId("fallback_generic_" + System.currentTimeMillis() + "_" + questionNumber);
		return card;
	}
	
	private String[] getUnitQuestions(String unitTitle, String career) {
		switch (unitTitle.toLowerCase()) {
			case "introduction to healthcare":
				return new String[]{
					"What is the primary role of a Certified Nursing Assistant (CNA)?",
					"Which of the following is NOT a responsibility of a CNA?",
					"What is the most important aspect of patient care?",
					"What should you do before entering a patient's room?",
					"Why is proper hand hygiene important in healthcare?",
					"What is the chain of command in healthcare?",
					"How should you address a patient?",
					"What is the purpose of patient confidentiality?",
					"What should you do if you witness unsafe practices?",
					"What is the role of teamwork in healthcare?"
				};
			case "documentation and reporting":
				return new String[]{
					"What is the purpose of accurate documentation in healthcare?",
					"When should you document patient care?",
					"What information should be included in patient notes?",
					"How should you report changes in patient condition?",
					"What is the importance of timely reporting?",
					"How should you document medication administration?",
					"What is the purpose of incident reports?",
					"How should you handle confidential patient information?",
					"What is the role of electronic health records?",
					"How should you document patient education?"
				};
			case "infection control":
				return new String[]{
					"What is the most effective way to prevent infection transmission?",
					"When should you use personal protective equipment (PPE)?",
					"What is the proper handwashing technique?",
					"How should you handle contaminated materials?",
					"What is the purpose of isolation precautions?",
					"How should you clean patient equipment?",
					"What is the importance of vaccination for healthcare workers?",
					"How should you handle blood spills?",
					"What is the purpose of standard precautions?",
					"How should you prevent healthcare-associated infections?"
				};
			case "vital signs":
				return new String[]{
					"What are the five main vital signs?",
					"How do you measure blood pressure accurately?",
					"What is the normal range for adult temperature?",
					"How do you count respiratory rate?",
					"What is the normal pulse rate for adults?",
					"How do you measure oxygen saturation?",
					"What factors can affect vital signs?",
					"When should you report abnormal vital signs?",
					"How do you document vital signs?",
					"What is the importance of baseline vital signs?"
				};
			case "patient care":
				return new String[]{
					"How do you assist with patient mobility?",
					"What is proper body mechanics for lifting?",
					"How do you help with patient hygiene?",
					"What is the importance of patient positioning?",
					"How do you assist with feeding?",
					"What is proper bed-making technique?",
					"How do you help with toileting?",
					"What is the importance of patient comfort?",
					"How do you assist with range of motion exercises?",
					"What is proper patient transfer technique?"
				};
			default:
				return new String[]{
					"What is the primary role of a " + career + "?",
					"Which of the following is NOT a responsibility of a " + career + "?",
					"What is the most important aspect of " + unitTitle.toLowerCase() + "?",
					"How should you approach " + unitTitle.toLowerCase() + "?",
					"Why is " + unitTitle.toLowerCase() + " important in healthcare?",
					"What are the key principles of " + unitTitle.toLowerCase() + "?",
					"How do you ensure quality in " + unitTitle.toLowerCase() + "?",
					"What is the role of communication in " + unitTitle.toLowerCase() + "?",
					"How do you maintain safety in " + unitTitle.toLowerCase() + "?",
					"What is the importance of " + unitTitle.toLowerCase() + " for patients?"
				};
		}
	}
	
	private String[] getUnitAnswers(String unitTitle) {
		switch (unitTitle.toLowerCase()) {
			case "introduction to healthcare":
				return new String[]{
					"To provide basic patient care under the supervision of registered nurses",
					"Prescribing medications",
					"Patient safety and dignity",
					"Knock and announce yourself",
					"To prevent the spread of infection",
					"Follow the established hierarchy and report to your supervisor",
					"Use their preferred name and title",
					"To protect patient privacy and build trust",
					"Report it immediately to your supervisor",
					"To provide coordinated, comprehensive care"
				};
			case "documentation and reporting":
				return new String[]{
					"To ensure continuity of care and legal protection",
					"Immediately after providing care",
					"Date, time, care provided, and patient response",
					"Immediately to the nurse in charge",
					"To ensure prompt intervention if needed",
					"Document time, dose, route, and patient response",
					"To identify and prevent future incidents",
					"Keep it secure and only share with authorized personnel",
					"To maintain accurate, accessible patient records",
					"Document what was taught and patient understanding"
				};
			case "infection control":
				return new String[]{
					"Proper hand hygiene",
					"When there is risk of exposure to bodily fluids",
					"Wet hands, apply soap, scrub for 20 seconds, rinse, dry",
					"Dispose of in designated biohazard containers",
					"To prevent transmission of infectious diseases",
					"Clean with approved disinfectants between patients",
					"To protect yourself and patients from preventable diseases",
					"Wear gloves, clean with bleach solution, dispose properly",
					"To treat all patients as potentially infectious",
					"By following proper protocols and maintaining cleanliness"
				};
			case "vital signs":
				return new String[]{
					"Temperature, pulse, respiration, blood pressure, oxygen saturation",
					"Use proper cuff size, patient seated, arm at heart level",
					"97.8°F to 99.0°F (36.5°C to 37.2°C)",
					"Count breaths for one full minute",
					"60-100 beats per minute",
					"Using a pulse oximeter on finger or earlobe",
					"Age, activity, medications, stress, illness",
					"When values are outside normal ranges",
					"Record values, time, and any relevant factors",
					"To detect changes and trends in patient condition"
				};
			case "patient care":
				return new String[]{
					"Assess patient ability, use proper techniques, ensure safety",
					"Bend knees, keep back straight, use leg muscles",
					"Respect privacy, maintain dignity, ensure comfort",
					"To prevent complications and promote healing",
					"Assess swallowing ability, provide appropriate assistance",
					"Use clean linens, maintain proper tension, ensure comfort",
					"Provide privacy, assist as needed, maintain dignity",
					"To promote healing and patient satisfaction",
					"Follow prescribed routine, support joints, avoid pain",
					"Use proper equipment, communicate clearly, ensure safety"
				};
			default:
				return new String[]{
					"To provide quality care and support to patients",
					"Performing procedures outside scope of practice",
					"Patient safety and quality outcomes",
					"With knowledge, skill, and compassion",
					"To ensure optimal patient outcomes and safety",
					"Safety, quality, compassion, and evidence-based practice",
					"By following protocols and maintaining standards",
					"To ensure clear understanding and coordination",
					"By following safety protocols and using proper techniques",
					"For optimal health outcomes and patient satisfaction"
				};
		}
	}
	
	private String[] getUnitRationales(String unitTitle) {
		switch (unitTitle.toLowerCase()) {
			case "introduction to healthcare":
				return new String[]{
					"CNAs work under the supervision of RNs and provide fundamental patient care including bathing, feeding, and monitoring vital signs.",
					"CNAs cannot prescribe medications. This is outside their scope of practice and requires advanced nursing education.",
					"Patient safety and dignity are the foundation of all healthcare practices. CNAs must always prioritize these principles.",
					"Always knock and announce yourself before entering a patient's room to respect their privacy and dignity.",
					"Proper hand hygiene is the single most effective way to prevent the spread of healthcare-associated infections.",
					"Following the chain of command ensures proper communication and accountability in healthcare settings.",
					"Using preferred names and titles shows respect and helps build therapeutic relationships.",
					"Patient confidentiality is essential for building trust and maintaining professional standards.",
					"Reporting unsafe practices protects patients and maintains quality care standards.",
					"Teamwork ensures coordinated, comprehensive care and better patient outcomes."
				};
			case "documentation and reporting":
				return new String[]{
					"Accurate documentation ensures continuity of care, legal protection, and quality improvement.",
					"Timely documentation ensures accuracy and provides immediate access to care information.",
					"Complete documentation includes all relevant information for comprehensive care planning.",
					"Immediate reporting ensures prompt intervention and prevents complications.",
					"Timely reporting allows for immediate assessment and intervention when needed.",
					"Complete medication documentation ensures safety and legal compliance.",
					"Incident reports help identify problems and prevent future occurrences.",
					"Confidentiality protects patient privacy and maintains professional standards.",
					"Electronic records improve accessibility, accuracy, and care coordination.",
					"Documenting education ensures continuity and tracks patient understanding."
				};
			case "infection control":
				return new String[]{
					"Hand hygiene is the most effective single measure to prevent infection transmission.",
					"PPE provides a barrier against exposure to potentially infectious materials.",
					"Proper handwashing technique ensures effective removal of pathogens.",
					"Proper disposal prevents contamination and protects others from exposure.",
					"Isolation precautions prevent transmission of infectious diseases to others.",
					"Cleaning between patients prevents cross-contamination.",
					"Vaccination protects healthcare workers and prevents disease transmission.",
					"Proper blood spill cleanup prevents exposure and contamination.",
					"Standard precautions treat all patients as potentially infectious.",
					"Following protocols and maintaining cleanliness prevents infections."
				};
			case "vital signs":
				return new String[]{
					"These five measurements provide essential information about patient health status.",
					"Proper technique ensures accurate readings and reliable assessment.",
					"Normal temperature range indicates healthy body function.",
					"Accurate respiratory assessment requires full minute observation.",
					"Normal pulse rate indicates healthy cardiovascular function.",
					"Pulse oximetry provides non-invasive oxygen level measurement.",
					"Multiple factors can influence vital sign values.",
					"Abnormal values may indicate health problems requiring attention.",
					"Complete documentation provides baseline and trend information.",
					"Baseline values help identify changes and trends over time."
				};
			case "patient care":
				return new String[]{
					"Proper mobility assistance ensures patient safety and promotes independence.",
					"Good body mechanics prevent injury and ensure safe patient handling.",
					"Proper hygiene promotes comfort, dignity, and infection prevention.",
					"Proper positioning prevents complications and promotes healing.",
					"Safe feeding assistance ensures adequate nutrition and prevents choking.",
					"Proper bed-making ensures patient comfort and safety.",
					"Proper toileting assistance maintains dignity and prevents accidents.",
					"Patient comfort promotes healing and satisfaction.",
					"Range of motion exercises maintain joint function and prevent stiffness.",
					"Safe transfer techniques prevent injury to both patient and caregiver."
				};
			default:
				return new String[]{
					"Providing quality care is the primary responsibility of all healthcare professionals.",
					"Performing procedures outside scope of practice is unsafe and illegal.",
					"Patient safety and quality outcomes are the foundation of healthcare.",
					"Knowledge, skill, and compassion ensure effective care delivery.",
					"Optimal outcomes and safety are the goals of all healthcare interventions.",
					"These principles guide all healthcare practices and decisions.",
					"Following protocols and standards ensures consistent quality care.",
					"Clear communication ensures coordinated and effective care.",
					"Safety protocols and proper techniques prevent injury and ensure quality.",
					"Optimal health outcomes and satisfaction are the goals of patient care."
				};
		}
	}
	
	private String[][] getUnitOptions(String unitTitle) {
		switch (unitTitle.toLowerCase()) {
			case "introduction to healthcare":
				return new String[][]{
					{"To provide basic patient care under the supervision of registered nurses", "To diagnose patient conditions", "To prescribe medications", "To perform surgical procedures"},
					{"Assisting with daily living activities", "Monitoring vital signs", "Prescribing medications", "Helping with mobility"},
					{"Speed of service", "Patient safety and dignity", "Cost efficiency", "Documentation completion"},
					{"Knock and announce yourself", "Enter immediately if the door is open", "Wait for the patient to call you", "Ask another staff member to enter first"},
					{"To prevent the spread of infection", "To keep hands soft", "To follow facility policy", "To save time between patients"},
					{"Follow the established hierarchy and report to your supervisor", "Handle it yourself", "Ignore the situation", "Tell other staff members"},
					{"Use their preferred name and title", "Call them by their first name", "Use medical terminology", "Address them formally"},
					{"To protect patient privacy and build trust", "To follow facility policy", "To avoid legal issues", "To save time"},
					{"Report it immediately to your supervisor", "Handle it yourself", "Tell other patients", "Ignore the situation"},
					{"To provide coordinated, comprehensive care", "To reduce staff workload", "To follow facility policy", "To save time"}
				};
			case "documentation and reporting":
				return new String[][]{
					{"To ensure continuity of care and legal protection", "To save time", "To follow facility policy", "To avoid work"},
					{"Immediately after providing care", "At the end of the shift", "When convenient", "Only when asked"},
					{"Date, time, care provided, and patient response", "Just the care provided", "Only patient complaints", "Staff names"},
					{"Immediately to the nurse in charge", "At the end of the shift", "When convenient", "Only if serious"},
					{"To ensure prompt intervention if needed", "To follow facility policy", "To avoid work", "To save time"},
					{"Document time, dose, route, and patient response", "Just document the medication", "Only if there's a problem", "At the end of the shift"},
					{"To identify and prevent future incidents", "To avoid legal issues", "To follow facility policy", "To save time"},
					{"Keep it secure and only share with authorized personnel", "Share with other staff", "Tell family members", "Post on social media"},
					{"To maintain accurate, accessible patient records", "To save paper", "To follow facility policy", "To avoid work"},
					{"Document what was taught and patient understanding", "Just document the teaching", "Only if patient seems confused", "At the end of the shift"}
				};
			case "infection control":
				return new String[][]{
					{"Proper hand hygiene", "Wearing gloves", "Using masks", "Cleaning surfaces"},
					{"When there is risk of exposure to bodily fluids", "Only when working with sick patients", "When convenient", "Never"},
					{"Wet hands, apply soap, scrub for 20 seconds, rinse, dry", "Quick rinse with water", "Use hand sanitizer only", "Wipe hands on clothing"},
					{"Dispose of in designated biohazard containers", "Throw in regular trash", "Leave in patient room", "Reuse if possible"},
					{"To prevent transmission of infectious diseases", "To save money", "To follow facility policy", "To avoid work"},
					{"Clean with approved disinfectants between patients", "Clean only when dirty", "Use water only", "Never clean"},
					{"To protect yourself and patients from preventable diseases", "To follow facility policy", "To avoid work", "To save money"},
					{"Wear gloves, clean with bleach solution, dispose properly", "Wipe with tissue", "Leave for housekeeping", "Ignore the spill"},
					{"To treat all patients as potentially infectious", "To save time", "To follow facility policy", "To avoid work"},
					{"By following proper protocols and maintaining cleanliness", "By wearing gloves only", "By avoiding sick patients", "By cleaning occasionally"}
				};
			case "vital signs":
				return new String[][]{
					{"Temperature, pulse, respiration, blood pressure, oxygen saturation", "Heart rate only", "Blood pressure only", "Temperature only"},
					{"Use proper cuff size, patient seated, arm at heart level", "Any position is fine", "Standing position", "Lying down"},
					{"97.8°F to 99.0°F (36.5°C to 37.2°C)", "95.0°F to 97.0°F", "100.0°F to 102.0°F", "90.0°F to 95.0°F"},
					{"Count breaths for one full minute", "Count for 30 seconds", "Estimate the rate", "Ask the patient"},
					{"60-100 beats per minute", "40-60 beats per minute", "100-120 beats per minute", "80-120 beats per minute"},
					{"Using a pulse oximeter on finger or earlobe", "Looking at skin color", "Asking the patient", "Estimating"},
					{"Age, activity, medications, stress, illness", "Only age", "Only medications", "Only stress"},
					{"When values are outside normal ranges", "Only if very high", "Only if very low", "Never"},
					{"Record values, time, and any relevant factors", "Just the values", "Only if abnormal", "At the end of the shift"},
					{"To detect changes and trends in patient condition", "To follow facility policy", "To avoid work", "To save time"}
				};
			case "patient care":
				return new String[][]{
					{"Assess patient ability, use proper techniques, ensure safety", "Just help them move", "Do it for them", "Tell them to do it themselves"},
					{"Bend knees, keep back straight, use leg muscles", "Bend at the waist", "Use back muscles", "Any technique is fine"},
					{"Respect privacy, maintain dignity, ensure comfort", "Just get it done quickly", "Ignore patient preferences", "Do minimal care"},
					{"To prevent complications and promote healing", "To save time", "To follow facility policy", "To avoid work"},
					{"Assess swallowing ability, provide appropriate assistance", "Just give them food", "Ignore difficulties", "Let them feed themselves"},
					{"Use clean linens, maintain proper tension, ensure comfort", "Any linens are fine", "Ignore patient comfort", "Do it quickly"},
					{"Provide privacy, assist as needed, maintain dignity", "Just get it done", "Ignore patient needs", "Do minimal assistance"},
					{"To promote healing and patient satisfaction", "To save time", "To follow facility policy", "To avoid work"},
					{"Follow prescribed routine, support joints, avoid pain", "Do any exercises", "Ignore patient pain", "Skip exercises"},
					{"Use proper equipment, communicate clearly, ensure safety", "Any equipment is fine", "Skip communication", "Ignore safety"}
				};
			default:
				return new String[][]{
					{"To provide quality care and support to patients", "To avoid work", "To follow facility policy", "To save time"},
					{"Performing procedures outside scope of practice", "Following protocols", "Maintaining safety", "Providing care"},
					{"Patient safety and quality outcomes", "Speed of service", "Cost efficiency", "Documentation completion"},
					{"With knowledge, skill, and compassion", "Quickly and efficiently", "Following protocols only", "When convenient"},
					{"To ensure optimal patient outcomes and safety", "To save time", "To follow facility policy", "To avoid work"},
					{"Safety, quality, compassion, and evidence-based practice", "Speed, efficiency, and cost", "Protocols and policies", "Rules and regulations"},
					{"By following protocols and maintaining standards", "By working quickly", "By avoiding difficult cases", "By following orders"},
					{"To ensure clear understanding and coordination", "To save time", "To follow facility policy", "To avoid work"},
					{"By following safety protocols and using proper techniques", "By working quickly", "By avoiding difficult cases", "By following orders"},
					{"For optimal health outcomes and patient satisfaction", "To save time", "To follow facility policy", "To avoid work"}
				};
		}
	}

	/**
	 * Check if the AI service is currently available
	 */
	public boolean isServiceAvailable() {
		synchronized (rateLimitLock) {
			long now = System.currentTimeMillis();
			long timeSinceLastRequest = now - lastRequestTime.get();
			// Service is available if we haven't made a request in the last 10 seconds
			return timeSinceLastRequest > 10000;
		}
	}
	
	/**
	 * Get recommended wait time before next request
	 */
	public long getRecommendedWaitTime() {
		synchronized (rateLimitLock) {
			long now = System.currentTimeMillis();
			long timeSinceLastRequest = now - lastRequestTime.get();
			long recommendedWait = Math.max(0, 10000 - timeSinceLastRequest);
			return recommendedWait;
		}
	}
	
	/**
	 * Get the current request count for monitoring
	 */
	public int getRequestCount() {
		return requestCount.get();
	}
}
