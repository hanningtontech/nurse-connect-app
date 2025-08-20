package com.example.nurse_connect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nurse_connect.R;
import com.example.nurse_connect.models.FlashcardSession;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class SavedSessionsAdapter extends RecyclerView.Adapter<SavedSessionsAdapter.ViewHolder> {
    
    private List<FlashcardSession> sessions;
    private OnSessionClickListener listener;
    
    public interface OnSessionClickListener {
        void onSessionClick(FlashcardSession session);
    }
    
    public SavedSessionsAdapter(List<FlashcardSession> sessions, OnSessionClickListener listener) {
        this.sessions = sessions;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_session, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FlashcardSession session = sessions.get(position);
        holder.bind(session);
    }
    
    @Override
    public int getItemCount() {
        return sessions != null ? sessions.size() : 0;
    }
    
    public void updateSessions(List<FlashcardSession> newSessions) {
        this.sessions = newSessions;
        notifyDataSetChanged();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSessionTitle;
        private TextView tvSessionDate;
        private TextView tvSessionScore;
        private TextView tvPerformance;
        private View indicatorPerformance;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSessionTitle = itemView.findViewById(R.id.tv_session_title);
            tvSessionDate = itemView.findViewById(R.id.tv_session_date);
            tvSessionScore = itemView.findViewById(R.id.tv_session_score);
            tvPerformance = itemView.findViewById(R.id.tv_performance);
            indicatorPerformance = itemView.findViewById(R.id.indicator_performance);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSessionClick(sessions.get(position));
                }
            });
        }
        
        public void bind(FlashcardSession session) {
            // Set session title
            tvSessionTitle.setText(session.getHeading());
            
            // Set session date
            if (session.getTimestamp() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String dateStr = dateFormat.format(session.getTimestamp().toDate());
                tvSessionDate.setText(dateStr);
            }
            
            // Set session score
            int totalCards = session.getTotalAnswered();
            int correctAnswers = session.getCorrectAnswers();
            String scoreText = correctAnswers + "/" + totalCards;
            tvSessionScore.setText(scoreText);
            
            // Set performance indicator
            double accuracy = totalCards > 0 ? (double) correctAnswers / totalCards * 100 : 0;
            String performanceText;
            int performanceColor;
            
            if (accuracy >= 80) {
                performanceText = "Excellent";
                performanceColor = itemView.getContext().getColor(R.color.performance_excellent);
            } else if (accuracy >= 60) {
                performanceText = "Good";
                performanceColor = itemView.getContext().getColor(R.color.performance_good);
            } else if (accuracy >= 40) {
                performanceText = "Fair";
                performanceColor = itemView.getContext().getColor(R.color.performance_fair);
            } else {
                performanceText = "Needs Practice";
                performanceColor = itemView.getContext().getColor(R.color.performance_practice);
            }
            
            tvPerformance.setText(performanceText);
            indicatorPerformance.setBackgroundColor(performanceColor);
        }
    }
}
