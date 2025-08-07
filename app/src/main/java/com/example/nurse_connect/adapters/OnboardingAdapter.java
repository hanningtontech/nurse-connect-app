package com.example.nurse_connect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nurse_connect.R;
import com.example.nurse_connect.models.OnboardingItem;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {
    
    private List<OnboardingItem> items;
    
    public OnboardingAdapter(List<OnboardingItem> items) {
        this.items = items;
    }
    
    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding, parent, false);
        return new OnboardingViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        holder.bind(items.get(position), position);
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    class OnboardingViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivOnboarding;
        private TextView tvTitle;
        private TextView tvDescription;
        
        public OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
            ivOnboarding = itemView.findViewById(R.id.ivOnboarding);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }
        
        public void bind(OnboardingItem item, int position) {
            ivOnboarding.setImageResource(item.getImageRes());
            tvTitle.setText(item.getTitle());
            tvDescription.setText(item.getDescription());
        }
    }
} 