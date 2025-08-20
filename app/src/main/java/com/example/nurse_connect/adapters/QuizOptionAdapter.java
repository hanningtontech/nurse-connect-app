package com.example.nurse_connect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nurse_connect.R;

import java.util.ArrayList;
import java.util.List;

public class QuizOptionAdapter extends RecyclerView.Adapter<QuizOptionAdapter.OptionViewHolder> {
    
    private List<String> options;
    private OnOptionClickListener listener;
    private int selectedPosition = -1;
    private int correctPosition = -1;
    private boolean isEnabled = true;
    
    public interface OnOptionClickListener {
        void onOptionClick(int position);
    }
    
    public QuizOptionAdapter(OnOptionClickListener listener) {
        this.options = new ArrayList<>();
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quiz_option, parent, false);
        return new OptionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull OptionViewHolder holder, int position) {
        String option = options.get(position);
        holder.bind(option, position);
    }
    
    @Override
    public int getItemCount() {
        return options.size();
    }
    
    public void setOptions(List<String> options) {
        this.options = new ArrayList<>(options);
        clearSelection();
        notifyDataSetChanged();
    }
    
    public void setCorrectAnswer(int position) {
        this.correctPosition = position;
    }
    
    public void clearSelection() {
        selectedPosition = -1;
        correctPosition = -1;
        notifyDataSetChanged();
    }
    
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        notifyDataSetChanged();
    }
    
    public void showCorrectAnswer(int selectedPos) {
        this.selectedPosition = selectedPos;
        this.correctPosition = selectedPos;
        notifyDataSetChanged();
    }
    
    public void showIncorrectAnswer(int selectedPos, int correctPos) {
        this.selectedPosition = selectedPos;
        this.correctPosition = correctPos;
        notifyDataSetChanged();
    }
    
    public void setSelectedOption(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }
    
    class OptionViewHolder extends RecyclerView.ViewHolder {
        private TextView textOption;
        private View cardView;
        
        public OptionViewHolder(@NonNull View itemView) {
            super(itemView);
            textOption = itemView.findViewById(R.id.text_option);
            cardView = itemView.findViewById(R.id.card_option);
            
            cardView.setOnClickListener(v -> {
                if (isEnabled && listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        selectedPosition = position;
                        listener.onOptionClick(position);
                        notifyDataSetChanged();
                    }
                }
            });
        }
        
        public void bind(String option, int position) {
            // Add option letter (A, B, C, D) - ensure clean formatting
            char optionLetter = (char) ('A' + position);
            String displayText = optionLetter + ". " + option.trim();
            textOption.setText(displayText);
            
            // Set background drawable based on state using our new modern styles
            if (!isEnabled) {
                // Disabled state
                cardView.setBackgroundResource(R.drawable.quiz_option_button_normal);
                cardView.setAlpha(0.6f);
                textOption.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.medical_text_tertiary));
            } else if (position == correctPosition && position == selectedPosition) {
                // Correct answer selected
                cardView.setBackgroundResource(R.drawable.quiz_option_button_correct);
                cardView.setAlpha(1.0f);
                textOption.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.medical_success));
            } else if (position == correctPosition && selectedPosition != -1) {
                // Show correct answer when user selected wrong
                cardView.setBackgroundResource(R.drawable.quiz_option_button_correct);
                cardView.setAlpha(1.0f);
                textOption.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.medical_success));
            } else if (position == selectedPosition) {
                // Incorrect answer selected
                cardView.setBackgroundResource(R.drawable.quiz_option_button_incorrect);
                cardView.setAlpha(1.0f);
                textOption.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.medical_error));
            } else {
                // Default state
                cardView.setBackgroundResource(R.drawable.quiz_option_button_normal);
                cardView.setAlpha(1.0f);
                textOption.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.medical_text_primary));
            }
            
            // Set click enabled state
            cardView.setClickable(isEnabled);
            
            // Apply modern typography with smaller text size for options
            textOption.setTextAppearance(itemView.getContext(), R.style.TextAppearance_Medical_AnswerOption);
        }
    }
}
