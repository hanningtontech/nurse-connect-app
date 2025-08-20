package com.example.nurse_connect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nurse_connect.R;
import com.example.nurse_connect.models.FlashcardSession;

import java.util.List;

/**
 * Adapter for displaying Flashcard Sessions in RecyclerView
 */
public class FlashcardSessionAdapter extends RecyclerView.Adapter<FlashcardSessionAdapter.SessionViewHolder> {
    
    private List<FlashcardSession> sessions;
    private OnSessionClickListener listener;
    
    public interface OnSessionClickListener {
        void onSessionClick(FlashcardSession session);
    }
    
    public FlashcardSessionAdapter(List<FlashcardSession> sessions, OnSessionClickListener listener) {
        this.sessions = sessions;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_flashcard_session, parent, false);
        return new SessionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        FlashcardSession session = sessions.get(position);
        holder.bind(session);
    }
    
    @Override
    public int getItemCount() {
        return sessions.size();
    }
    
    public class SessionViewHolder extends RecyclerView.ViewHolder {
        
        private TextView textHeading;
        private TextView textScore;
        private TextView textAccuracy;
        private TextView textDateTime;
        private TextView textPerformance;
        private View performanceIndicator;
        
        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            
            textHeading = itemView.findViewById(R.id.text_heading);
            textScore = itemView.findViewById(R.id.text_score);
            textAccuracy = itemView.findViewById(R.id.text_accuracy);
            textDateTime = itemView.findViewById(R.id.text_date_time);
            textPerformance = itemView.findViewById(R.id.text_performance);
            performanceIndicator = itemView.findViewById(R.id.performance_indicator);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSessionClick(sessions.get(position));
                }
            });
        }
        
        public void bind(FlashcardSession session) {
            textHeading.setText(session.getHeading());
            textScore.setText(session.getFormattedScore());
            textAccuracy.setText(session.getFormattedAccuracy());
            textDateTime.setText(session.getFormattedDateTime());
            textPerformance.setText(session.getPerformanceLevel());
            
            // Set performance indicator color
            if (session.isPassed()) {
                if (session.getAccuracy() >= 80) {
                    performanceIndicator.setBackgroundResource(R.drawable.performance_excellent);
                } else {
                    performanceIndicator.setBackgroundResource(R.drawable.performance_good);
                }
            } else {
                performanceIndicator.setBackgroundResource(R.drawable.performance_practice);
            }
            
            // Set text colors based on performance
            if (session.isPassed()) {
                if (session.getAccuracy() >= 80) {
                    textAccuracy.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
                    textPerformance.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    textAccuracy.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_blue_dark));
                    textPerformance.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_blue_dark));
                }
            } else {
                textAccuracy.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_orange_dark));
                textPerformance.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_orange_dark));
            }
        }
    }
}
