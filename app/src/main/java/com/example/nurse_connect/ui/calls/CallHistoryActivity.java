package com.example.nurse_connect.ui.calls;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.nurse_connect.R;
import com.example.nurse_connect.adapters.CallHistoryAdapter;
import com.example.nurse_connect.models.CallLog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CallHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CallHistoryAdapter adapter;
    private List<CallLog> callLogs;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Spinner filterSpinner;
    private TextView emptyStateText;
    private TextView totalCallsText;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String currentFilter = "all"; // all, video, audio

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_history);

        initializeViews();
        initializeFirebase();
        setupToolbar();
        setupRecyclerView();
        setupFilterSpinner();
        setupSwipeRefresh();
        loadCallHistory();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        filterSpinner = findViewById(R.id.filterSpinner);
        emptyStateText = findViewById(R.id.emptyStateText);
        totalCallsText = findViewById(R.id.totalCallsText);
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Call History");
        }
    }

    private void setupRecyclerView() {
        callLogs = new ArrayList<>();
        adapter = new CallHistoryAdapter(this, callLogs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupFilterSpinner() {
        String[] filterOptions = {"All Calls", "Video Calls Only", "Audio Calls Only"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, filterOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(spinnerAdapter);

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        currentFilter = "all";
                        break;
                    case 1:
                        currentFilter = "video";
                        break;
                    case 2:
                        currentFilter = "audio";
                        break;
                }
                loadCallHistory();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentFilter = "all";
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadCallHistory);
    }

    private void loadCallHistory() {
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to view call history", Toast.LENGTH_SHORT).show();
            return;
        }

        swipeRefreshLayout.setRefreshing(true);
        callLogs.clear();

        Query query = db.collection("calls")
                .whereEqualTo("callerId", currentUser.getUid())
                .orderBy("startTime", Query.Direction.DESCENDING);

        // Add filter for video calls only if selected
        if ("video".equals(currentFilter)) {
            query = query.whereEqualTo("callType", "video");
        } else if ("audio".equals(currentFilter)) {
            query = query.whereEqualTo("callType", "audio");
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        CallLog callLog = createCallLogFromDocument(document, true);
                        if (callLog != null) {
                            callLogs.add(callLog);
                        }
                    }

                    // Also get calls where user was the receiver
                    Query receiverQuery = db.collection("calls")
                            .whereEqualTo("receiverId", currentUser.getUid())
                            .orderBy("startTime", Query.Direction.DESCENDING);

                    if ("video".equals(currentFilter)) {
                        receiverQuery = receiverQuery.whereEqualTo("callType", "video");
                    } else if ("audio".equals(currentFilter)) {
                        receiverQuery = receiverQuery.whereEqualTo("callType", "audio");
                    }

                    receiverQuery.get()
                            .addOnSuccessListener(receiverSnapshots -> {
                                for (QueryDocumentSnapshot document : receiverSnapshots) {
                                    CallLog callLog = createCallLogFromDocument(document, false);
                                    if (callLog != null) {
                                        callLogs.add(callLog);
                                    }
                                }

                                // Sort by start time (most recent first)
                                callLogs.sort((c1, c2) -> Long.compare(c2.getStartTime(), c1.getStartTime()));

                                updateUI();
                                swipeRefreshLayout.setRefreshing(false);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to load call history", Toast.LENGTH_SHORT).show();
                                swipeRefreshLayout.setRefreshing(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load call history", Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private CallLog createCallLogFromDocument(QueryDocumentSnapshot document, boolean isOutgoing) {
        try {
            String callId = document.getId();
            String callerId = document.getString("callerId");
            String receiverId = document.getString("receiverId");
            String callerName = document.getString("callerName");
            String receiverName = document.getString("receiverName");
            String callType = document.getString("callType");
            String status = document.getString("status");
            Long startTime = document.getLong("startTime");
            Long endTime = document.getLong("endTime");
            Long duration = document.getLong("duration");
            String callerPhotoUrl = document.getString("callerPhotoUrl");
            String receiverPhotoUrl = document.getString("receiverPhotoUrl");

            if (startTime == null) {
                startTime = System.currentTimeMillis();
            }

            // Determine the other user's info
            String otherUserId, otherUserName, otherPhotoUrl;
            if (isOutgoing) {
                otherUserId = receiverId;
                otherUserName = receiverName;
                otherPhotoUrl = receiverPhotoUrl;
            } else {
                otherUserId = callerId;
                otherUserName = callerName;
                otherPhotoUrl = callerPhotoUrl;
            }

            return new CallLog(
                    callId,
                    otherUserId,
                    otherUserName,
                    otherPhotoUrl,
                    callType,
                    status,
                    startTime,
                    endTime,
                    duration,
                    isOutgoing
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void updateUI() {
        adapter.notifyDataSetChanged();
        
        if (callLogs.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
            String filterText = "video".equals(currentFilter) ? "video" : 
                              "audio".equals(currentFilter) ? "audio" : "";
            emptyStateText.setText("No " + filterText + " calls found");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
            
            // Update total calls count
            String filterText = "video".equals(currentFilter) ? "Video Calls" : 
                              "audio".equals(currentFilter) ? "Audio Calls" : "Total Calls";
            totalCallsText.setText(filterText + ": " + callLogs.size());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
