package com.example.nurse_connect.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.nurse_connect.R;
import com.example.nurse_connect.models.CallLog;
import com.example.nurse_connect.ui.chat.PrivateChatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class CallHistoryAdapter extends RecyclerView.Adapter<CallHistoryAdapter.CallHistoryViewHolder> {

    private Context context;
    private List<CallLog> callLogs;
    private SimpleDateFormat dateFormat;

    public CallHistoryAdapter(Context context, List<CallLog> callLogs) {
        this.context = context;
        this.callLogs = callLogs;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
    }

    @NonNull
    @Override
    public CallHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_call_history, parent, false);
        return new CallHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CallHistoryViewHolder holder, int position) {
        CallLog callLog = callLogs.get(position);
        holder.bind(callLog);
    }

    @Override
    public int getItemCount() {
        return callLogs.size();
    }

    public void updateCallLogs(List<CallLog> newCallLogs) {
        this.callLogs = newCallLogs;
        notifyDataSetChanged();
    }

    class CallHistoryViewHolder extends RecyclerView.ViewHolder {
        private CircleImageView userPhoto;
        private TextView userName;
        private TextView callType;
        private TextView callStatus;
        private TextView callTime;
        private TextView callDuration;
        private ImageView callDirectionIcon;
        private ImageView callTypeIcon;

        public CallHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            userPhoto = itemView.findViewById(R.id.userPhoto);
            userName = itemView.findViewById(R.id.userName);
            callType = itemView.findViewById(R.id.callType);
            callStatus = itemView.findViewById(R.id.callStatus);
            callTime = itemView.findViewById(R.id.callTime);
            callDuration = itemView.findViewById(R.id.callDuration);
            callDirectionIcon = itemView.findViewById(R.id.callDirectionIcon);
            callTypeIcon = itemView.findViewById(R.id.callTypeIcon);

            // Make the entire item clickable to open chat
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    CallLog callLog = callLogs.get(position);
                    openChatWithUser(callLog);
                }
            });
        }

        public void bind(CallLog callLog) {
            // Set user name
            userName.setText(callLog.getOtherUserName() != null ? callLog.getOtherUserName() : "Unknown User");

            // Load user photo
            if (callLog.getOtherPhotoUrl() != null && !callLog.getOtherPhotoUrl().isEmpty()) {
                Glide.with(context)
                        .load(callLog.getOtherPhotoUrl())
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(userPhoto);
            } else {
                userPhoto.setImageResource(R.drawable.ic_profile_placeholder);
            }

            // Set call type
            if (callLog.isVideoCall()) {
                callType.setText("Video Call");
                callTypeIcon.setImageResource(R.drawable.ic_videocam);
            } else {
                callType.setText("Audio Call");
                callTypeIcon.setImageResource(R.drawable.ic_call);
            }

            // Set call status and duration
            if (callLog.isCompleted()) {
                callStatus.setText(callLog.getFormattedDuration());
                callStatus.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
            } else if (callLog.isMissed()) {
                callStatus.setText("Missed");
                callStatus.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
            } else {
                callStatus.setText("Unknown");
                callStatus.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
            }

            // Set call time
            callTime.setText(dateFormat.format(new Date(callLog.getStartTime())));

            // Set call direction icon
            if (callLog.isOutgoing()) {
                callDirectionIcon.setImageResource(R.drawable.ic_call_made);
                callDirectionIcon.setColorFilter(context.getResources().getColor(android.R.color.holo_green_dark));
            } else {
                callDirectionIcon.setImageResource(R.drawable.ic_call_received);
                callDirectionIcon.setColorFilter(context.getResources().getColor(android.R.color.holo_blue_dark));
            }

            // Show/hide duration based on call status
            if (callLog.isCompleted() && callLog.getDuration() != null && callLog.getDuration() > 0) {
                callDuration.setVisibility(View.VISIBLE);
                callDuration.setText(callLog.getFormattedDuration());
            } else {
                callDuration.setVisibility(View.GONE);
            }
        }

        private void openChatWithUser(CallLog callLog) {
            Intent intent = new Intent(context, PrivateChatActivity.class);
            intent.putExtra("otherUserId", callLog.getOtherUserId());
            intent.putExtra("otherUserName", callLog.getOtherUserName());
            intent.putExtra("otherUserPhotoUrl", callLog.getOtherPhotoUrl());
            context.startActivity(intent);
        }
    }
}
