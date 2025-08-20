package com.example.nurse_connect.ui.flashcards.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.nurse_connect.R;
import com.example.nurse_connect.models.FlashcardGameMode;
import java.util.List;

public class GameModeAdapter extends RecyclerView.Adapter<GameModeAdapter.GameModeViewHolder> {
    
    private List<FlashcardGameMode> gameModes;
    private OnGameModeClickListener listener;
    
    public GameModeAdapter(OnGameModeClickListener listener) {
        this.listener = listener;
        // Initialize with all available game modes
        this.gameModes = List.of(
            FlashcardGameMode.STUDY_MODE,
            FlashcardGameMode.TIMED_MODE,
            FlashcardGameMode.QUIZ_MODE,
            FlashcardGameMode.SPACED_REPETITION,
            FlashcardGameMode.MASTERY_MODE,
            FlashcardGameMode.DAILY_CHALLENGE,
            FlashcardGameMode.MATCHING_MODE,
            FlashcardGameMode.SCENARIO_MODE
        );
    }
    
    @NonNull
    @Override
    public GameModeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_game_mode, parent, false);
        return new GameModeViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull GameModeViewHolder holder, int position) {
        FlashcardGameMode gameMode = gameModes.get(position);
        holder.bind(gameMode);
    }
    
    @Override
    public int getItemCount() {
        return gameModes.size();
    }
    
    class GameModeViewHolder extends RecyclerView.ViewHolder {
        private TextView titleText;
        private TextView descriptionText;
        private View cardView;
        
        public GameModeViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.game_mode_title);
            descriptionText = itemView.findViewById(R.id.game_mode_description);
            cardView = itemView.findViewById(R.id.game_mode_card);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onGameModeClick(gameModes.get(position));
                }
            });
        }
        
        public void bind(FlashcardGameMode gameMode) {
            titleText.setText(gameMode.getDisplayName());
            descriptionText.setText(gameMode.getDescription());
        }
    }
    
    public interface OnGameModeClickListener {
        void onGameModeClick(FlashcardGameMode gameMode);
    }
}
