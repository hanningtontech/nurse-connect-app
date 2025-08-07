package com.example.nurse_connect.webrtc;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RealTimeAudioManager {
    private static final String TAG = "RealTimeAudioManager";

    // Audio configuration
    private static final int SAMPLE_RATE = 8000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    private Context context;
    private AudioManager audioManager;
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private ExecutorService executorService;

    private boolean isRecording = false;
    private boolean isPlaying = false;
    private boolean isMuted = false;
    private boolean isSpeakerOn = false;

    private AudioListener listener;

    public interface AudioListener {
        void onCallConnected();
        void onCallDisconnected();
        void onAudioStarted();
        void onAudioStopped();
        void onError(String error);
    }

    public RealTimeAudioManager(Context context, AudioListener listener) {
        this.context = context;
        this.listener = listener;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.executorService = Executors.newFixedThreadPool(2);
        initializeAudio();
    }
    
    private void initializeAudio() {
        try {
            // Initialize AudioRecord for recording
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    BUFFER_SIZE
            );

            // Initialize AudioTrack for playback
            audioTrack = new AudioTrack(
                    AudioManager.STREAM_VOICE_CALL,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AUDIO_FORMAT,
                    BUFFER_SIZE,
                    AudioTrack.MODE_STREAM
            );

            Log.d(TAG, "Audio components initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize audio", e);
            if (listener != null) {
                listener.onError("Failed to initialize audio: " + e.getMessage());
            }
        }
    }

    public void startCall() {
        if (audioRecord == null || audioTrack == null) {
            Log.e(TAG, "Audio components not initialized");
            return;
        }

        try {
            // Configure audio manager for voice call
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(isSpeakerOn);

            // Start recording and playing
            startRecording();
            startPlaying();

            if (listener != null) {
                listener.onCallConnected();
                listener.onAudioStarted();
            }

            Log.d(TAG, "Audio call started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start call", e);
            if (listener != null) {
                listener.onError("Failed to start call: " + e.getMessage());
            }
        }
    }
    
    private void startRecording() {
        if (audioRecord != null && !isRecording) {
            isRecording = true;
            audioRecord.startRecording();

            executorService.execute(() -> {
                byte[] buffer = new byte[BUFFER_SIZE];
                while (isRecording && audioRecord != null) {
                    int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                    if (bytesRead > 0 && !isMuted) {
                        // In a real implementation, this would send audio data to the other user
                        // For now, we'll just simulate the audio processing
                        processAudioData(buffer, bytesRead);
                    }
                }
            });

            Log.d(TAG, "Audio recording started");
        }
    }
    
    private void startPlaying() {
        if (audioTrack != null && !isPlaying) {
            isPlaying = true;
            audioTrack.play();

            executorService.execute(() -> {
                byte[] buffer = new byte[BUFFER_SIZE];
                while (isPlaying && audioTrack != null) {
                    // In a real implementation, this would receive audio data from the other user
                    // For now, we'll simulate receiving audio data
                    int bytesToWrite = simulateReceivedAudio(buffer);
                    if (bytesToWrite > 0) {
                        audioTrack.write(buffer, 0, bytesToWrite);
                    }

                    try {
                        Thread.sleep(20); // Small delay to prevent busy waiting
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });

            Log.d(TAG, "Audio playback started");
        }
    }
    
    private void processAudioData(byte[] buffer, int bytesRead) {
        // In a real implementation, this would encode and send audio data to the other user
        // For demonstration, we'll just log that audio is being processed
        // Log.d(TAG, "Processing " + bytesRead + " bytes of audio data");
    }

    private int simulateReceivedAudio(byte[] buffer) {
        // In a real implementation, this would receive and decode audio data from the other user
        // For demonstration, we'll return 0 to indicate no audio data
        return 0;
    }
    
    public void toggleMute(boolean mute) {
        this.isMuted = mute;
        Log.d(TAG, "Audio " + (mute ? "muted" : "unmuted"));
    }

    public void toggleSpeaker(boolean speakerOn) {
        this.isSpeakerOn = speakerOn;
        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(speakerOn);
            Log.d(TAG, "Speaker " + (speakerOn ? "on" : "off"));
        }
    }

    public boolean isMuted() {
        return isMuted;
    }

    public boolean isSpeakerOn() {
        return isSpeakerOn;
    }

    public void endCall() {
        // Stop recording
        if (isRecording) {
            isRecording = false;
            if (audioRecord != null) {
                try {
                    audioRecord.stop();
                    audioRecord.release();
                    audioRecord = null;
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping audio record", e);
                }
            }
        }

        // Stop playing
        if (isPlaying) {
            isPlaying = false;
            if (audioTrack != null) {
                try {
                    audioTrack.stop();
                    audioTrack.release();
                    audioTrack = null;
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping audio track", e);
                }
            }
        }

        // Reset audio manager
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.setSpeakerphoneOn(false);
        }

        // Shutdown executor
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        if (listener != null) {
            listener.onCallDisconnected();
            listener.onAudioStopped();
        }

        Log.d(TAG, "Call ended and resources cleaned up");
    }
}
