package com.example.nurse_connect.ui.flashcards.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.nurse_connect.R;
import com.example.nurse_connect.models.Flashcard;
import java.util.List;

public class FlashcardDetailAdapter extends RecyclerView.Adapter<FlashcardDetailAdapter.ViewHolder> {
    private List<Flashcard> flashcards;

    public FlashcardDetailAdapter(List<Flashcard> flashcards) {
        this.flashcards = flashcards;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Flashcard flashcard = flashcards.get(position);
        holder.textView.setText(flashcard.getQuestion());
    }

    @Override
    public int getItemCount() {
        return flashcards.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}