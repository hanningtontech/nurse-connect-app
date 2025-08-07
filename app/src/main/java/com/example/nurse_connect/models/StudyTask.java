package com.example.nurse_connect.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.List;
import java.util.Map;

public class StudyTask {
    @DocumentId
    private String taskId = "";
    private String groupId = "";
    private String createdBy = "";
    private String title = "";
    private String description = "";
    private TaskType type = TaskType.FLASHCARD;
    private Timestamp scheduledAt = Timestamp.now();
    private Timestamp dueAt = Timestamp.now();
    private Timestamp completedAt = null;
    private boolean isActive = true;
    private boolean isCompleted = false;
    private int timeLimit = 0; // in minutes, 0 means no time limit
    private List<String> assignedMembers = null;
    private Map<String, TaskSubmission> submissions = null;
    private List<TaskQuestion> questions = null;
    private TaskSettings settings = new TaskSettings();
    private Timestamp createdAt = Timestamp.now();

    public enum TaskType {
        FLASHCARD, QUIZ, ASSIGNMENT, DISCUSSION, POLL
    }

    public static class TaskQuestion {
        private String questionId = "";
        private String question = "";
        private String questionType = "text"; // text, image, video
        private String mediaUrl = "";
        private List<String> options = null;
        private String correctAnswer = "";
        private int points = 1;
        private String explanation = "";

        public TaskQuestion() {}

        // Getters and Setters
        public String getQuestionId() { return questionId; }
        public void setQuestionId(String questionId) { this.questionId = questionId; }

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }

        public String getQuestionType() { return questionType; }
        public void setQuestionType(String questionType) { this.questionType = questionType; }

        public String getMediaUrl() { return mediaUrl; }
        public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }

        public String getCorrectAnswer() { return correctAnswer; }
        public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }

        public int getPoints() { return points; }
        public void setPoints(int points) { this.points = points; }

        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
    }

    public static class TaskSubmission {
        private String userId = "";
        private String userName = "";
        private Timestamp submittedAt = Timestamp.now();
        private Map<String, String> answers = null;
        private int score = 0;
        private int totalPoints = 0;
        private boolean isCompleted = false;
        private String feedback = "";

        public TaskSubmission() {}

        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }

        public Timestamp getSubmittedAt() { return submittedAt; }
        public void setSubmittedAt(Timestamp submittedAt) { this.submittedAt = submittedAt; }

        public Map<String, String> getAnswers() { return answers; }
        public void setAnswers(Map<String, String> answers) { this.answers = answers; }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }

        public int getTotalPoints() { return totalPoints; }
        public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }

        public boolean isCompleted() { return isCompleted; }
        public void setCompleted(boolean completed) { isCompleted = completed; }

        public String getFeedback() { return feedback; }
        public void setFeedback(String feedback) { this.feedback = feedback; }
    }

    public static class TaskSettings {
        private boolean showResultsImmediately = false;
        private boolean allowRetakes = false;
        private boolean randomizeQuestions = false;
        private boolean showCorrectAnswers = true;
        private boolean requireAllMembers = false;
        private boolean sendReminders = true;
        private int reminderHours = 24;

        public TaskSettings() {}

        // Getters and Setters
        public boolean isShowResultsImmediately() { return showResultsImmediately; }
        public void setShowResultsImmediately(boolean showResultsImmediately) { this.showResultsImmediately = showResultsImmediately; }

        public boolean isAllowRetakes() { return allowRetakes; }
        public void setAllowRetakes(boolean allowRetakes) { this.allowRetakes = allowRetakes; }

        public boolean isRandomizeQuestions() { return randomizeQuestions; }
        public void setRandomizeQuestions(boolean randomizeQuestions) { this.randomizeQuestions = randomizeQuestions; }

        public boolean isShowCorrectAnswers() { return showCorrectAnswers; }
        public void setShowCorrectAnswers(boolean showCorrectAnswers) { this.showCorrectAnswers = showCorrectAnswers; }

        public boolean isRequireAllMembers() { return requireAllMembers; }
        public void setRequireAllMembers(boolean requireAllMembers) { this.requireAllMembers = requireAllMembers; }

        public boolean isSendReminders() { return sendReminders; }
        public void setSendReminders(boolean sendReminders) { this.sendReminders = sendReminders; }

        public int getReminderHours() { return reminderHours; }
        public void setReminderHours(int reminderHours) { this.reminderHours = reminderHours; }
    }

    // Default constructor for Firestore
    public StudyTask() {}

    public StudyTask(String groupId, String title, TaskType type) {
        this.groupId = groupId;
        this.title = title;
        this.type = type;
    }

    // Getters and Setters
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TaskType getType() { return type; }
    public void setType(TaskType type) { this.type = type; }

    public Timestamp getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Timestamp scheduledAt) { this.scheduledAt = scheduledAt; }

    public Timestamp getDueAt() { return dueAt; }
    public void setDueAt(Timestamp dueAt) { this.dueAt = dueAt; }

    public Timestamp getCompletedAt() { return completedAt; }
    public void setCompletedAt(Timestamp completedAt) { this.completedAt = completedAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public int getTimeLimit() { return timeLimit; }
    public void setTimeLimit(int timeLimit) { this.timeLimit = timeLimit; }

    public List<String> getAssignedMembers() { return assignedMembers; }
    public void setAssignedMembers(List<String> assignedMembers) { this.assignedMembers = assignedMembers; }

    public Map<String, TaskSubmission> getSubmissions() { return submissions; }
    public void setSubmissions(Map<String, TaskSubmission> submissions) { this.submissions = submissions; }

    public List<TaskQuestion> getQuestions() { return questions; }
    public void setQuestions(List<TaskQuestion> questions) { this.questions = questions; }

    public TaskSettings getSettings() { return settings; }
    public void setSettings(TaskSettings settings) { this.settings = settings; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
} 