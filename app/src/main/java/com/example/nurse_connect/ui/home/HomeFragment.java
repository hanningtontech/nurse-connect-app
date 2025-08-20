package com.example.nurse_connect.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.nurse_connect.R;
import com.example.nurse_connect.adapters.FeaturedContentAdapter;
import com.example.nurse_connect.adapters.NotificationAdapter;
import com.example.nurse_connect.databinding.FragmentHomeBinding;
import com.example.nurse_connect.models.FeaturedContent;
import com.example.nurse_connect.models.NotificationItem;
import com.example.nurse_connect.models.QuickAction;
import com.example.nurse_connect.services.NotificationService;
import com.example.nurse_connect.services.RecommendationService;
import com.example.nurse_connect.ui.community.CommunityHubActivity;
import com.example.nurse_connect.ui.community.PublicGroupsActivity;
import com.example.nurse_connect.ui.community.MyGroupsActivity;
import com.example.nurse_connect.ui.studyhub.StudyHubFragment;
import com.example.nurse_connect.ui.flashcards.FlashcardSetupActivity;
import com.example.nurse_connect.ui.flashcards.FlashcardGameModeSelectionActivity;
import com.example.nurse_connect.ui.search.UniversalSearchActivity;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment implements FeaturedContentAdapter.OnFeaturedContentClickListener,
        NotificationAdapter.OnNotificationClickListener {

    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private FeaturedContentAdapter featuredContentAdapter;
    private RecommendationService recommendationService;
    private NotificationService notificationService;
    private Handler autoScrollHandler;
    private Runnable autoScrollRunnable;
    private int currentFeaturedPosition = 0;
    private List<FeaturedContent> featuredContentList;
    private List<QuickAction> quickActions;
    private List<NotificationItem> allNotifications;
    private PopupWindow notificationPopup;
    private java.util.Set<String> processedMessages;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeServices();
        setupUI();
        loadFeaturedContent();
        setupQuickActions();
        loadFeed();
    }
    
    private void initializeServices() {
        recommendationService = new RecommendationService();
        notificationService = new NotificationService();
        featuredContentList = new ArrayList<>();
        quickActions = new ArrayList<>();
        allNotifications = new ArrayList<>();
        processedMessages = new java.util.HashSet<>();
        autoScrollHandler = new Handler();
    }

    private void setupUI() {
        // Setup Featured Content RecyclerView
        setupFeaturedContentRecyclerView();

        // Setup Feed RecyclerView
        binding.rvFeed.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO: Set adapter for feed items

        // Setup SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            loadFeaturedContent();
            loadFeed();
        });

        // Setup Notification functionality
        setupNotificationIcon();

        // Setup Search functionality
        setupSearchFunctionality();
    }

    private void setupFeaturedContentRecyclerView() {
        featuredContentAdapter = new FeaturedContentAdapter(featuredContentList, this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.HORIZONTAL, false);
        binding.rvFeaturedContent.setLayoutManager(layoutManager);
        binding.rvFeaturedContent.setAdapter(featuredContentAdapter);

        // Add snap helper for smooth scrolling
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(binding.rvFeaturedContent);

        // Setup auto-scroll
        setupAutoScroll();
    }

    private void setupNotificationIcon() {
        // Load notification count initially
        loadNotificationCount();

        // Setup click listener for notification icon
        binding.notificationContainer.setOnClickListener(v -> {
            showNotificationSpinner();
        });
    }

    private void setupSearchFunctionality() {
        binding.searchBar.setOnClickListener(v -> {
            // Track search usage
            trackSearchUsage();

            // Open universal search activity
            Intent intent = new Intent(requireContext(), UniversalSearchActivity.class);
            startActivity(intent);
        });
    }
    
    private void setupAutoScroll() {
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (featuredContentList.size() > 1) {
                    currentFeaturedPosition = (currentFeaturedPosition + 1) % featuredContentList.size();
                    binding.rvFeaturedContent.smoothScrollToPosition(currentFeaturedPosition);
                    updatePageIndicator();
                }
                autoScrollHandler.postDelayed(this, 5000); // Auto-scroll every 5 seconds
            }
        };
    }

    private void startAutoScroll() {
        stopAutoScroll();
        if (featuredContentList.size() > 1) {
            autoScrollHandler.postDelayed(autoScrollRunnable, 5000);
        }
    }

    private void stopAutoScroll() {
        if (autoScrollHandler != null && autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
    }

    private void loadFeaturedContent() {
        recommendationService.getFeaturedContent(new RecommendationService.RecommendationCallback() {
            @Override
            public void onSuccess(List<FeaturedContent> recommendations) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        featuredContentList.clear();
                        featuredContentList.addAll(recommendations);
                        featuredContentAdapter.notifyDataSetChanged();
                        setupPageIndicator();
                        startAutoScroll();

                        // Track featured content views
                        for (FeaturedContent content : recommendations) {
                            trackFeaturedContentView(content);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load featured content", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Failed to load featured content", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void setupPageIndicator() {
        binding.llPageIndicator.removeAllViews();

        for (int i = 0; i < featuredContentList.size(); i++) {
            ImageView dot = new ImageView(getContext());
            dot.setImageResource(R.drawable.ic_circle);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            dot.setLayoutParams(params);

            // Set initial state
            if (i == currentFeaturedPosition) {
                dot.setColorFilter(getResources().getColor(R.color.theme_primary, null));
            } else {
                dot.setColorFilter(getResources().getColor(R.color.theme_medium_gray, null));
            }

            binding.llPageIndicator.addView(dot);
        }
    }

    private void updatePageIndicator() {
        for (int i = 0; i < binding.llPageIndicator.getChildCount(); i++) {
            ImageView dot = (ImageView) binding.llPageIndicator.getChildAt(i);
            if (i == currentFeaturedPosition) {
                dot.setColorFilter(getResources().getColor(R.color.theme_primary, null));
            } else {
                dot.setColorFilter(getResources().getColor(R.color.theme_medium_gray, null));
            }
        }
    }

    private void setupQuickActions() {
        // Define available quick actions
        quickActions.clear();
        quickActions.addAll(Arrays.asList(
            new QuickAction("community_hub", "Community Hub", R.drawable.ic_group, "activity", CommunityHubActivity.class, false),
            new QuickAction("public_groups", "Public Groups", R.drawable.ic_public, "activity", PublicGroupsActivity.class, false),
            new QuickAction("my_groups", "My Groups", R.drawable.ic_group, "activity", MyGroupsActivity.class, true),
            new QuickAction("study_materials", "Study Materials", R.drawable.ic_book, "fragment", null, false),
            new QuickAction("create_task", "Create Task", R.drawable.ic_task, "activity", null, true),
            new QuickAction("flashcards", "Flashcards", R.drawable.ic_quiz, "activity", null, true)
        ));

        // Shuffle for random order
        Collections.shuffle(quickActions);

        // Create action buttons dynamically
        createQuickActionButtons();
    }

    private void createQuickActionButtons() {
        binding.gridQuickActions.removeAllViews();

        for (int i = 0; i < Math.min(6, quickActions.size()); i++) {
            QuickAction action = quickActions.get(i);
            View actionView = createQuickActionView(action);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);

            actionView.setLayoutParams(params);
            binding.gridQuickActions.addView(actionView);
        }
    }

    private View createQuickActionView(QuickAction action) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_quick_action, null);

        ImageView icon = view.findViewById(R.id.ivActionIcon);
        android.widget.TextView title = view.findViewById(R.id.tvActionTitle);

        icon.setImageResource(action.getIconResource());
        title.setText(action.getTitle());

        view.setOnClickListener(v -> handleQuickActionClick(action));

        return view;
    }

    private void handleQuickActionClick(QuickAction action) {
        // Track quick action usage for analytics
        trackQuickActionUsage(action);

        // Check if authentication is required
        if (action.isRequiresAuth() && FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please sign in to access this feature", Toast.LENGTH_SHORT).show();
            return;
        }

        switch (action.getId()) {
            case "community_hub":
                Intent communityIntent = new Intent(requireContext(), CommunityHubActivity.class);
                startActivity(communityIntent);
                break;
            case "public_groups":
                Intent publicGroupsIntent = new Intent(requireContext(), PublicGroupsActivity.class);
                startActivity(publicGroupsIntent);
                break;
            case "my_groups":
                Intent myGroupsIntent = new Intent(requireContext(), MyGroupsActivity.class);
                startActivity(myGroupsIntent);
                break;
            case "study_materials":
                // Navigate to Study Hub tab
                navigateToStudyHub();
                break;
            case "create_task":
                // Navigate to task creation - for now show community hub
                Intent taskIntent = new Intent(requireContext(), CommunityHubActivity.class);
                taskIntent.putExtra("show_create_task", true);
                startActivity(taskIntent);
                break;
            case "flashcards":
                // Open Flashcard game mode selection
                Intent flashcardsIntent = new Intent(requireContext(), FlashcardGameModeSelectionActivity.class);
                startActivity(flashcardsIntent);
                break;
            default:
                if (action.getTargetActivity() != null) {
                    Intent intent = new Intent(requireContext(), action.getTargetActivity());
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), action.getTitle() + " coming soon!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void navigateToStudyHub() {
        if (getActivity() != null && getActivity() instanceof com.example.nurse_connect.MainActivity) {
            com.example.nurse_connect.MainActivity mainActivity = (com.example.nurse_connect.MainActivity) getActivity();

            // Find and click the Study Hub navigation item
            View studyHubNav = mainActivity.findViewById(R.id.nav_study_hub);
            if (studyHubNav != null) {
                studyHubNav.performClick();
            }
        }
    }

    @Override
    public void onFeaturedContentClick(FeaturedContent content) {
        // Track user interaction for improving recommendations
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUserId != null) {
            recommendationService.trackUserInteraction(currentUserId, content.getContentId(),
                    content.getContentType(), "click");
        }

        // Handle featured content click based on content type
        switch (content.getContentType()) {
            case "study_material":
                // Open study material details
                openStudyMaterial(content);
                break;
            case "group":
                // Open group details
                openGroup(content);
                break;
            case "user":
                // Open user profile
                openUserProfile(content);
                break;
            case "task":
                // Open task details
                openTask(content);
                break;
            case "quiz_match":
                // Open quiz match
                openQuizMatch(content);
                break;
            default:
                Toast.makeText(getContext(), "Opening " + content.getTitle(), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void openStudyMaterial(FeaturedContent content) {
        // Navigate to Study Hub and show the specific material
        if (getActivity() != null && getActivity() instanceof com.example.nurse_connect.MainActivity) {
            com.example.nurse_connect.MainActivity mainActivity = (com.example.nurse_connect.MainActivity) getActivity();

            // Switch to Study Hub tab
            mainActivity.findViewById(R.id.nav_study_hub).performClick();

            // Show material details
            Toast.makeText(getContext(), "Opening: " + content.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openGroup(FeaturedContent content) {
        // Navigate to group details - check if it's a public group
        try {
            Intent intent = new Intent(requireContext(), com.example.nurse_connect.ui.community.PublicGroupsActivity.class);
            intent.putExtra("highlight_group_id", content.getContentId());
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Opening group: " + content.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openUserProfile(FeaturedContent content) {
        // Navigate to user profile or community hub
        try {
            Intent intent = new Intent(requireContext(), com.example.nurse_connect.ui.community.GlobalNurseHubActivity.class);
            intent.putExtra("highlight_user_id", content.getContentId());
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Opening profile: " + content.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openTask(FeaturedContent content) {
        // Navigate to task details - for now show in community hub
        try {
            Intent intent = new Intent(requireContext(), CommunityHubActivity.class);
            intent.putExtra("highlight_task_id", content.getContentId());
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Opening task: " + content.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openQuizMatch(FeaturedContent content) {
        // Navigate to quiz waiting room to join the match
        try {
            Intent intent = new Intent(requireContext(), com.example.nurse_connect.ui.quiz.QuizWaitingRoomActivity.class);
            intent.putExtra("match_id", content.getContentId());
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to open quiz match: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFeed() {
        // TODO: Load feed data from repository
        // For now, just stop refreshing
        binding.swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        startAutoScroll();

        // Track home fragment view
        trackHomeFragmentView();

        // Refresh featured content when user returns
        if (featuredContentList.isEmpty()) {
            loadFeaturedContent();
        }

        // Clear sample notifications first
        notificationService.clearAllSampleNotifications();

        // Fix malformed notifications in database
        notificationService.fixMalformedNotifications();

        // Clean up old notifications
        notificationService.cleanupOldNotifications();

        // Refresh notification count after cleanup
        new Handler().postDelayed(() -> loadNotificationCount(), 1000);

        // Listen to real-time message notifications
        setupRealTimeNotificationListener();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoScroll();
    }

    // Public method to refresh content from MainActivity if needed
    public void refreshContent() {
        loadFeaturedContent();
        setupQuickActions(); // Randomize quick actions
    }

    // Analytics and engagement tracking methods
    private void trackQuickActionUsage(QuickAction action) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (currentUserId != null && recommendationService != null) {
            // Track quick action usage
            recommendationService.trackUserInteraction(currentUserId, action.getId(),
                    "quick_action", "click");

            // Also track in a separate analytics collection for quick actions
            trackQuickActionAnalytics(currentUserId, action);
        }
    }

    private void trackQuickActionAnalytics(String userId, QuickAction action) {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("userId", userId);
        analytics.put("actionId", action.getId());
        analytics.put("actionTitle", action.getTitle());
        analytics.put("actionType", action.getActionType());
        analytics.put("timestamp", System.currentTimeMillis());
        analytics.put("sessionId", getSessionId());

        // Store in Firebase for analytics
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("quick_action_analytics")
                .add(analytics)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Quick action analytics tracked: " + action.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to track quick action analytics", e);
                });
    }

    private void trackFeaturedContentView(FeaturedContent content) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (currentUserId != null && recommendationService != null) {
            recommendationService.trackUserInteraction(currentUserId, content.getContentId(),
                    content.getContentType(), "view");
        }
    }

    private void trackSearchUsage() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (currentUserId != null) {
            Map<String, Object> searchAnalytics = new HashMap<>();
            searchAnalytics.put("userId", currentUserId);
            searchAnalytics.put("action", "search_opened");
            searchAnalytics.put("timestamp", System.currentTimeMillis());
            searchAnalytics.put("sessionId", getSessionId());

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("search_analytics")
                    .add(searchAnalytics)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Search usage tracked");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to track search usage", e);
                    });
        }
    }

    private String getSessionId() {
        // Generate or retrieve session ID for this app session
        if (getActivity() != null) {
            return getActivity().getClass().getSimpleName() + "_" + System.currentTimeMillis();
        }
        return "unknown_session_" + System.currentTimeMillis();
    }

    private void trackHomeFragmentView() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (currentUserId != null) {
            Map<String, Object> viewAnalytics = new HashMap<>();
            viewAnalytics.put("userId", currentUserId);
            viewAnalytics.put("screen", "home_fragment");
            viewAnalytics.put("timestamp", System.currentTimeMillis());
            viewAnalytics.put("featuredContentCount", featuredContentList.size());
            viewAnalytics.put("quickActionsCount", quickActions.size());

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("screen_analytics")
                    .add(viewAnalytics)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Home fragment view tracked");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to track home fragment view", e);
                    });
        }
    }

    // Notification methods
    private void loadNotificationCount() {
        notificationService.getUnreadCount(new NotificationService.NotificationCountCallback() {
            @Override
            public void onSuccess(int unreadCount) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            updateNotificationBadge(unreadCount);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load notification count", e);
            }
        });
    }

    private void updateNotificationBadge(int count) {
        TextView badge = binding.tvNotificationBadge;
        if (count > 0) {
            badge.setVisibility(View.VISIBLE);
            badge.setText(count > 99 ? "99+" : String.valueOf(count));
        } else {
            badge.setVisibility(View.GONE);
        }
    }

    private void showNotificationSpinner() {
        if (notificationPopup != null && notificationPopup.isShowing()) {
            notificationPopup.dismiss();
            return;
        }

        // Load notifications first
        notificationService.getNotifications(new NotificationService.NotificationCallback() {
            @Override
            public void onSuccess(List<NotificationItem> notifications, int unreadCount) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        allNotifications.clear();
                        allNotifications.addAll(notifications);
                        updateNotificationBadge(unreadCount);
                        createAndShowNotificationPopup();
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load notifications", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void createAndShowNotificationPopup() {
        View popupView = LayoutInflater.from(getContext()).inflate(R.layout.notification_spinner, null);

        // Calculate popup width based on screen size
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int popupWidth = Math.min(screenWidth - 32, (int) (screenWidth * 0.9)); // 90% of screen width, max screen-32dp

        notificationPopup = new PopupWindow(popupView,
                popupWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);

        // Setup RecyclerView
        androidx.recyclerview.widget.RecyclerView rvNotifications = popupView.findViewById(R.id.rvNotifications);
        NotificationAdapter adapter = new NotificationAdapter(new ArrayList<>(allNotifications), this);
        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        rvNotifications.setAdapter(adapter);

        // Setup filter chips
        setupNotificationFilters(popupView, adapter);

        // Setup close button
        ImageView btnClose = popupView.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> notificationPopup.dismiss());

        // Setup mark all read button
        com.google.android.material.button.MaterialButton btnMarkAllRead = popupView.findViewById(R.id.btnMarkAllRead);
        btnMarkAllRead.setOnClickListener(v -> {
            notificationService.markAllAsRead();
            updateNotificationBadge(0);

            // Clear all notifications from the list (they disappear)
            allNotifications.clear();
            adapter.updateNotifications(new ArrayList<>());

            // Show empty state
            androidx.recyclerview.widget.RecyclerView recyclerView = popupView.findViewById(R.id.rvNotifications);
            LinearLayout emptyState = popupView.findViewById(R.id.emptyState);
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);

            // Show confirmation
            Toast.makeText(getContext(), "All notifications marked as read", Toast.LENGTH_SHORT).show();
        });

        // Show empty state if no notifications
        LinearLayout emptyState = popupView.findViewById(R.id.emptyState);
        if (allNotifications.isEmpty()) {
            rvNotifications.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvNotifications.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }

        // Show popup with better positioning
        notificationPopup.setOutsideTouchable(true);
        notificationPopup.setFocusable(true);
        notificationPopup.setElevation(8);

        // Calculate position to align popup to the right edge of the screen with some margin
        int[] location = new int[2];
        binding.notificationContainer.getLocationOnScreen(location);
        int xOffset = screenWidth - popupWidth - 16; // 16dp margin from right edge
        int yOffset = 8; // Small offset below the notification icon

        notificationPopup.showAtLocation(binding.notificationContainer, Gravity.NO_GRAVITY,
                xOffset, location[1] + binding.notificationContainer.getHeight() + yOffset);
    }

    private void setupNotificationFilters(View popupView, NotificationAdapter adapter) {
        Chip chipUnread = popupView.findViewById(R.id.chipUnread);
        Chip chipAll = popupView.findViewById(R.id.chipAll);
        Chip chipMessages = popupView.findViewById(R.id.chipMessages);
        Chip chipFollows = popupView.findViewById(R.id.chipFollows);
        Chip chipTasks = popupView.findViewById(R.id.chipTasks);

        chipUnread.setOnClickListener(v -> filterNotifications("unread", adapter));
        chipAll.setOnClickListener(v -> {
            // Load all notifications (including read ones)
            loadAllNotifications(adapter);
        });
        chipMessages.setOnClickListener(v -> filterNotifications("message", adapter));
        chipFollows.setOnClickListener(v -> filterNotifications("follow", adapter));
        chipTasks.setOnClickListener(v -> filterNotifications("task", adapter));
    }

    private void filterNotifications(String type, NotificationAdapter adapter) {
        List<NotificationItem> filteredNotifications = new ArrayList<>();

        if ("unread".equals(type)) {
            // Show only unread notifications
            for (NotificationItem notification : allNotifications) {
                if (!notification.isRead()) {
                    filteredNotifications.add(notification);
                }
            }
        } else if ("all".equals(type)) {
            filteredNotifications.addAll(allNotifications);
        } else {
            // Filter by type (message, follow, task)
            for (NotificationItem notification : allNotifications) {
                if (type.equals(notification.getType())) {
                    filteredNotifications.add(notification);
                }
            }
        }

        adapter.updateNotifications(filteredNotifications);
    }

    private void loadAllNotifications(NotificationAdapter adapter) {
        notificationService.getAllNotifications(new NotificationService.NotificationCallback() {
            @Override
            public void onSuccess(List<NotificationItem> notifications, int unreadCount) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        allNotifications.clear();
                        allNotifications.addAll(notifications);
                        adapter.updateNotifications(new ArrayList<>(allNotifications));
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load all notifications", e);
            }
        });
    }

    @Override
    public void onNotificationClick(NotificationItem notification) {
        // Mark as read
        if (!notification.isRead()) {
            notificationService.markAsRead(notification.getId());
            notification.setRead(true);
            loadNotificationCount(); // Refresh badge
        }

        // Remove the notification from the list (it disappears)
        allNotifications.remove(notification);

        // Handle notification click based on type
        handleNotificationAction(notification);

        // Dismiss popup
        if (notificationPopup != null && notificationPopup.isShowing()) {
            notificationPopup.dismiss();
        }
    }

    @Override
    public void onActionClick(NotificationItem notification) {
        // Mark as read and remove from list
        if (!notification.isRead()) {
            notificationService.markAsRead(notification.getId());
            notification.setRead(true);
            loadNotificationCount(); // Refresh badge
        }

        // Remove the notification from the list (it disappears)
        allNotifications.remove(notification);

        handleNotificationAction(notification);

        // Dismiss popup
        if (notificationPopup != null && notificationPopup.isShowing()) {
            notificationPopup.dismiss();
        }
    }

    private void handleNotificationAction(NotificationItem notification) {
        switch (notification.getType()) {
            case "message":
                // Navigate to the specific chat
                navigateToChat(notification);
                break;
            case "follow":
                // Open user profile
                Toast.makeText(getContext(), "Opening profile of " + notification.getFromUserName(), Toast.LENGTH_SHORT).show();
                break;
            case "task":
                // Open task details
                Toast.makeText(getContext(), "Opening task: " + notification.getTitle(), Toast.LENGTH_SHORT).show();
                break;
            case "suggestion":
                // Open suggestion
                Toast.makeText(getContext(), "Opening suggestion: " + notification.getTitle(), Toast.LENGTH_SHORT).show();
                break;
            case "like":
            case "comment":
                // Open post
                Toast.makeText(getContext(), "Opening post", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(getContext(), "Opening: " + notification.getTitle(), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void navigateToChat(NotificationItem notification) {
        String chatId = notification.getTargetId();
        String chatIdFromCompat = notification.getChatId();

        // Use whichever chat ID is available
        String finalChatId = chatId != null ? chatId : chatIdFromCompat;

        if (finalChatId != null) {
            // First, switch to the Chat tab in MainActivity
            if (getActivity() != null && getActivity() instanceof com.example.nurse_connect.MainActivity) {
                com.example.nurse_connect.MainActivity mainActivity = (com.example.nurse_connect.MainActivity) getActivity();

                // Find and click the Chat navigation item
                View chatNav = mainActivity.findViewById(R.id.nav_chat);
                if (chatNav != null) {
                    chatNav.performClick();

                    // Give a small delay for the fragment to load, then navigate to specific chat
                    new Handler().postDelayed(() -> {
                        navigateToSpecificChat(finalChatId, notification.getFromUserId());
                    }, 300);
                }
            }
        } else {
            Toast.makeText(getContext(), "Opening chat with " + notification.getFromUserName(), Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToSpecificChat(String chatId, String otherUserId) {
        try {
            // Check if it's a private chat or group chat
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("private_chats")
                    .document(chatId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        // Check if fragment is still attached
                        if (!isAdded() || getContext() == null) {
                            Log.w(TAG, "Fragment not attached, skipping navigation");
                            return;
                        }

                        if (documentSnapshot.exists()) {
                            // It's a private chat - open PrivateChatActivity
                            Intent intent = new Intent(requireContext(), com.example.nurse_connect.ui.chat.PrivateChatActivity.class);
                            intent.putExtra("chatId", chatId);
                            intent.putExtra("otherUserId", otherUserId);
                            startActivity(intent);
                        } else {
                            // Check if it's a group chat
                            checkAndOpenGroupChat(chatId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to check chat type", e);
                        if (isAdded() && getContext() != null) {
                            Toast.makeText(getContext(), "Failed to open chat", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to chat", e);
            Toast.makeText(getContext(), "Error opening chat", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAndOpenGroupChat(String groupId) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("group_chats")
                .document(groupId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Check if fragment is still attached
                    if (!isAdded() || getContext() == null) {
                        Log.w(TAG, "Fragment not attached, skipping group chat navigation");
                        return;
                    }

                    if (documentSnapshot.exists()) {
                        // It's a group chat - open GroupChatActivity
                        Intent intent = new Intent(requireContext(), com.example.nurse_connect.ui.chat.GroupChatActivity.class);
                        intent.putExtra("groupId", groupId);
                        startActivity(intent);
                    } else {
                        Toast.makeText(getContext(), "Chat not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check group chat", e);
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Failed to open group chat", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Real-time notification listener for incoming messages
    private void setupRealTimeNotificationListener() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (currentUserId != null) {
            // Listen to private chat messages
            setupPrivateChatListener(currentUserId);

            // Listen to group chat messages
            setupGroupChatListener(currentUserId);
        }
    }

    private void setupPrivateChatListener(String currentUserId) {
        // Listen to private chats where user is a participant
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("private_chats")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed for private chats", e);
                        return;
                    }

                    if (querySnapshot != null) {
                        for (com.google.firebase.firestore.DocumentChange dc : querySnapshot.getDocumentChanges()) {
                            if (dc.getType() == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {
                                // Check if there's a new message
                                Map<String, Object> chatData = dc.getDocument().getData();
                                checkForNewPrivateMessage(currentUserId, dc.getDocument().getId(), chatData);
                            }
                        }
                    }
                });
    }

    private void setupGroupChatListener(String currentUserId) {
        // Listen to group chats where user is a member
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("group_chats")
                .whereArrayContains("members", currentUserId)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed for group chats", e);
                        return;
                    }

                    if (querySnapshot != null) {
                        for (com.google.firebase.firestore.DocumentChange dc : querySnapshot.getDocumentChanges()) {
                            if (dc.getType() == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {
                                // Check if there's a new message
                                Map<String, Object> groupData = dc.getDocument().getData();
                                checkForNewGroupMessage(currentUserId, dc.getDocument().getId(), groupData);
                            }
                        }
                    }
                });
    }

    private void checkForNewPrivateMessage(String currentUserId, String chatId, Map<String, Object> chatData) {
        String lastMessageSenderId = (String) chatData.get("lastMessageSenderId");
        String lastMessage = (String) chatData.get("lastMessage");
        Object lastMessageTime = chatData.get("lastMessageTime");

        // Only create notification if the message is from someone else
        if (lastMessageSenderId != null && !lastMessageSenderId.equals(currentUserId) && lastMessage != null) {

            // Check if we already processed this message (prevent duplicates)
            String messageKey = chatId + "_" + lastMessageSenderId + "_" + lastMessage;
            if (processedMessages.contains(messageKey)) {
                Log.d(TAG, "Message already processed, skipping notification");
                return;
            }
            processedMessages.add(messageKey);
            // Get sender name from participants
            @SuppressWarnings("unchecked")
            java.util.List<String> participants = (java.util.List<String>) chatData.get("participants");

            if (participants != null) {
                String senderId = lastMessageSenderId;

                // Get sender's display name
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(senderId)
                        .get()
                        .addOnSuccessListener(userDoc -> {
                            // Check if fragment is still attached
                            if (!isAdded() || getActivity() == null) {
                                Log.w(TAG, "Fragment not attached, skipping notification creation");
                                return;
                            }

                            String senderName = userDoc.exists() ? userDoc.getString("displayName") : "Unknown User";

                            // Format the message preview (limit to 50 characters)
                            String messagePreview = lastMessage;
                            if (messagePreview != null && messagePreview.length() > 50) {
                                messagePreview = messagePreview.substring(0, 50) + "...";
                            }

                            // Create simple message notification showing sender
                            String notificationTitle = "Message from " + senderName;
                            String notificationMessage = "Tap to view conversation";
                            notificationService.createNotification(currentUserId, "message", notificationTitle,
                                    notificationMessage, senderId, senderName, chatId);

                            Log.d(TAG, "Created private message notification: " + notificationTitle);

                            // Refresh notification count
                            loadNotificationCount();
                        });
            }
        }
    }

    private void checkForNewGroupMessage(String currentUserId, String groupId, Map<String, Object> groupData) {
        String lastMessageSenderId = (String) groupData.get("lastMessageSenderId");
        String lastMessage = (String) groupData.get("lastMessage");
        String groupName = (String) groupData.get("groupName");

        // Only create notification if the message is from someone else
        if (lastMessageSenderId != null && !lastMessageSenderId.equals(currentUserId) && lastMessage != null) {

            // Check if we already processed this message (prevent duplicates)
            String messageKey = groupId + "_" + lastMessageSenderId + "_" + lastMessage;
            if (processedMessages.contains(messageKey)) {
                Log.d(TAG, "Group message already processed, skipping notification");
                return;
            }
            processedMessages.add(messageKey);
            // Get sender's display name
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(lastMessageSenderId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        // Check if fragment is still attached
                        if (!isAdded() || getActivity() == null) {
                            Log.w(TAG, "Fragment not attached, skipping group notification creation");
                            return;
                        }

                        String senderName = userDoc.exists() ? userDoc.getString("displayName") : "Unknown User";

                        // Format the message preview (limit to 40 characters for group messages)
                        String messagePreview = lastMessage;
                        if (messagePreview != null && messagePreview.length() > 40) {
                            messagePreview = messagePreview.substring(0, 40) + "...";
                        }

                        // Create simple group message notification
                        String notificationTitle = "Message from " + senderName;
                        String notificationMessage = "In " + (groupName != null ? groupName : "Group Chat") + " • Tap to view";

                        notificationService.createNotification(currentUserId, "message", notificationTitle,
                                notificationMessage, lastMessageSenderId, senderName, groupId);

                        Log.d(TAG, "Created group message notification: " + notificationTitle);

                        // Refresh notification count
                        loadNotificationCount();
                    });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopAutoScroll();
        if (notificationPopup != null && notificationPopup.isShowing()) {
            notificationPopup.dismiss();
        }
        binding = null;
    }
}