package com.example.nurse_connect.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nurse_connect.R;

public class SwipeToReplyHelper extends ItemTouchHelper.SimpleCallback {

    private Context context;
    private SwipeToReplyListener listener;
    private Drawable replyIcon;
    private boolean swipeBack = false;
    private float replyButtonProgress = 0f;
    private long lastReplyButtonAnimationTime = 0;
    private RecyclerView.ViewHolder currentItemViewHolder = null;

    public interface SwipeToReplyListener {
        void onSwipeToReply(int position);
    }

    public SwipeToReplyHelper(Context context, SwipeToReplyListener listener) {
        super(0, ItemTouchHelper.LEFT);
        this.context = context;
        this.listener = listener;
        this.replyIcon = ContextCompat.getDrawable(context, R.drawable.ic_reply);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // This won't be called because we handle the swipe manually
    }

    @Override
    public int convertToAbsoluteDirection(int flags, int layoutDirection) {
        if (swipeBack) {
            swipeBack = false;
            return 0;
        }
        return super.convertToAbsoluteDirection(flags, layoutDirection);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            setTouchListener(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        if (viewHolder.getAdapterPosition() == -1) {
            return;
        }

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            View itemView = viewHolder.itemView;
            float alpha = 1.0f;
            
            if (dX < 0) { // Swiping left
                // Limit the swipe distance
                float maxSwipe = itemView.getWidth() * 0.3f;
                dX = Math.max(dX, -maxSwipe);
                
                // Calculate alpha based on swipe progress
                alpha = 1.0f - Math.abs(dX) / maxSwipe;
                
                // Draw reply icon
                if (replyIcon != null) {
                    int iconSize = 64;
                    int iconMargin = 32;
                    
                    int iconTop = itemView.getTop() + (itemView.getHeight() - iconSize) / 2;
                    int iconLeft = itemView.getRight() - iconMargin - iconSize;
                    int iconRight = itemView.getRight() - iconMargin;
                    int iconBottom = iconTop + iconSize;
                    
                    replyIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    replyIcon.setAlpha((int) (255 * (1 - alpha)));
                    replyIcon.draw(c);
                }
            }
            
            itemView.setTranslationX(dX);
            itemView.setAlpha(alpha);
        }
        
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void setTouchListener(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                swipeBack = event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP;
                
                if (swipeBack) {
                    if (Math.abs(dX) >= 200) { // Threshold for triggering reply
                        if (listener != null) {
                            listener.onSwipeToReply(viewHolder.getAdapterPosition());
                        }
                    }
                }
                
                return false;
            }
        });
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        recyclerView.setOnTouchListener(null);
        viewHolder.itemView.setAlpha(1.0f);
        viewHolder.itemView.setTranslationX(0f);
    }
}
