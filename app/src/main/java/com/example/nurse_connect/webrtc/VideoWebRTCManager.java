package com.example.nurse_connect.webrtc;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Capturer;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages WebRTC peer connections and video streams for real-time video calling
 */
public class VideoWebRTCManager implements SignalingManager.SignalingListener {
    private static final String TAG = "VideoWebRTCManager";
    
    private Context context;
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private AudioSource audioSource;
    private AudioTrack localAudioTrack;
    private VideoSource videoSource;
    private VideoTrack localVideoTrack;
    private VideoTrack remoteVideoTrack;
    private VideoCapturer videoCapturer;
    private AudioManager audioManager;
    private EglBase eglBase;
    
    private SignalingManager signalingManager;
    private VideoWebRTCListener listener;
    private boolean isInitiator = false;
    private boolean isAudioEnabled = true;
    private boolean isVideoEnabled = true;
    private boolean isSpeakerEnabled = false;
    private boolean isFrontCamera = true;
    
    // ICE servers for NAT traversal
    private List<PeerConnection.IceServer> iceServers;
    
    public interface VideoWebRTCListener {
        void onCallConnected();
        void onCallDisconnected();
        void onVideoStarted();
        void onVideoStopped();
        void onError(String error);
        void onLocalVideoReady(SurfaceViewRenderer localView);
        void onRemoteVideoReady(SurfaceViewRenderer remoteView);
        void onRemoteVideoTrackReceived(VideoTrack remoteVideoTrack);
    }
    
    public VideoWebRTCManager(Context context, VideoWebRTCListener listener) {
        this.context = context;
        this.listener = listener;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        
        initializeWebRTC();
        setupIceServers();
    }
    
    /**
     * Initialize WebRTC components
     */
    private void initializeWebRTC() {
        Log.d(TAG, "Initializing Video WebRTC");
        
        // Initialize PeerConnectionFactory
        PeerConnectionFactory.InitializationOptions initOptions = 
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions();
        PeerConnectionFactory.initialize(initOptions);
        
        // Create EglBase for video
        eglBase = EglBase.create();
        
        // Create PeerConnectionFactory
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(null) // Use default
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
                .createPeerConnectionFactory();
        
        Log.d(TAG, "Video WebRTC initialized successfully");
    }
    
    /**
     * Get the current peer connection instance
     * @return The peer connection or null if not created
     */
    public PeerConnection getPeerConnection() {
        return peerConnection;
    }
    
    /**
     * Get the EglBase instance for SurfaceViewRenderer initialization
     * @return The EglBase instance
     */
    public EglBase getEglBase() {
        return eglBase;
    }
    
    /**
     * Connect remote video track to a SurfaceViewRenderer
     * @param remoteView The SurfaceViewRenderer to display remote video
     */
    public void connectRemoteVideo(SurfaceViewRenderer remoteView) {
        Log.d(TAG, "connectRemoteVideo called - track=" + (remoteVideoTrack != null) + ", view=" + (remoteView != null));
        if (remoteVideoTrack != null && remoteView != null) {
            Log.d(TAG, "Connecting remote video track to SurfaceViewRenderer");
            remoteVideoTrack.addSink(remoteView);
            Log.d(TAG, "Remote video track successfully added to SurfaceViewRenderer");
        } else {
            Log.w(TAG, "Cannot connect remote video: track=" + (remoteVideoTrack != null) + ", view=" + (remoteView != null));
        }
    }
    
    /**
     * Disconnect remote video track from SurfaceViewRenderer
     * @param remoteView The SurfaceViewRenderer to disconnect
     */
    public void disconnectRemoteVideo(SurfaceViewRenderer remoteView) {
        if (remoteVideoTrack != null && remoteView != null) {
            Log.d(TAG, "Disconnecting remote video track from SurfaceViewRenderer");
            remoteVideoTrack.removeSink(remoteView);
        }
    }
    
    /**
     * Connect local video track to a SurfaceViewRenderer
     * @param localView The SurfaceViewRenderer to display local video
     */
    public void connectLocalVideo(SurfaceViewRenderer localView) {
        Log.d(TAG, "connectLocalVideo called - track=" + (localVideoTrack != null) + ", view=" + (localView != null));
        if (localVideoTrack != null && localView != null) {
            Log.d(TAG, "Connecting local video track to SurfaceViewRenderer");
            localVideoTrack.addSink(localView);
            Log.d(TAG, "Local video track successfully added to SurfaceViewRenderer");
        } else {
            Log.w(TAG, "Cannot connect local video: track=" + (localVideoTrack != null) + ", view=" + (localView != null));
        }
    }
    
    /**
     * Disconnect local video track from SurfaceViewRenderer
     * @param localView The SurfaceViewRenderer to disconnect
     */
    public void disconnectLocalVideo(SurfaceViewRenderer localView) {
        if (localVideoTrack != null && localView != null) {
            Log.d(TAG, "Disconnecting local video track from SurfaceViewRenderer");
            localVideoTrack.removeSink(localView);
        }
    }
    
    /**
     * Setup ICE servers for NAT traversal
     */
    private void setupIceServers() {
        iceServers = new ArrayList<>();
        
        // Add STUN servers
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer());
        
        // Add TURN servers (you can add your own TURN servers here for better connectivity)
        // iceServers.add(PeerConnection.IceServer.builder("turn:your-turn-server.com:3478")
        //     .setUsername("username")
        //     .setPassword("password")
        //     .createIceServer());
        
        Log.d(TAG, "ICE servers configured: " + iceServers.size() + " servers");
    }
    
    /**
     * Start an outgoing video call
     */
    public void startCall(String callId, String localUserId, String remoteUserId) {
        Log.d(TAG, "Starting outgoing video call - callId: " + callId + ", localUserId: " + localUserId + ", remoteUserId: " + remoteUserId);
        
        isInitiator = true;
        
        // Initialize signaling manager with null WebRTCManager since VideoWebRTCManager is separate
        signalingManager = new SignalingManager(callId, localUserId, remoteUserId, this, null);
        
        // Create peer connection
        createPeerConnection();
        
        // Create local media streams
        createLocalMediaStreams();
        
        // Create and send offer
        createOffer();
    }
    
    /**
     * Answer an incoming video call
     */
    public void answerCall(String callId, String localUserId, String remoteUserId) {
        Log.d(TAG, "Answering incoming video call - callId: " + callId + ", localUserId: " + localUserId + ", remoteUserId: " + remoteUserId);
        
        isInitiator = false;
        
        // Initialize signaling manager with null WebRTCManager since VideoWebRTCManager is separate
        signalingManager = new SignalingManager(callId, localUserId, remoteUserId, this, null);
        
        // Create peer connection
        createPeerConnection();
        
        // Create local media streams
        createLocalMediaStreams();
        
        // Wait for offer from initiator
        Log.d(TAG, "Waiting for offer from initiator");
    }
    
    /**
     * Create peer connection with ICE servers
     */
    private void createPeerConnection() {
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnectionObserver());
        Log.d(TAG, "Peer connection created");
    }
    
    /**
     * Create local audio and video streams
     */
    private void createLocalMediaStreams() {
        // Create audio source and track
        MediaConstraints audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googHighpassFilter", "true"));
        
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio0", audioSource);
        
        // Create video source and track
        videoSource = peerConnectionFactory.createVideoSource(false);
        localVideoTrack = peerConnectionFactory.createVideoTrack("video0", videoSource);
        
        // Create video capturer
        createVideoCapturer();
        
        // Add tracks to peer connection
        peerConnection.addTrack(localAudioTrack);
        peerConnection.addTrack(localVideoTrack);
        
        // Notify that local video track is ready
        if (listener != null) {
            Log.d(TAG, "Notifying listener that local video track is ready");
            listener.onLocalVideoReady(null);
        }
        
        Log.d(TAG, "Local media streams created");
    }
    
    /**
     * Create video capturer for camera
     */
    private void createVideoCapturer() {
        try {
            // Use Camera2Capturer for better performance
            videoCapturer = new Camera2Capturer(context, isFrontCamera ? "1" : "0", new CameraVideoCapturer.CameraEventsHandler() {
                @Override
                public void onCameraError(String errorDescription) {
                    Log.e(TAG, "Camera error: " + errorDescription);
                    if (listener != null) {
                        listener.onError("Camera error: " + errorDescription);
                    }
                }
                
                @Override
                public void onCameraDisconnected() {
                    Log.d(TAG, "Camera disconnected");
                }
                
                @Override
                public void onCameraFreezed(String errorDescription) {
                    Log.e(TAG, "Camera freezed: " + errorDescription);
                }
                
                @Override
                public void onCameraOpening(String cameraName) {
                    Log.d(TAG, "Camera opening: " + cameraName);
                }
                
                @Override
                public void onFirstFrameAvailable() {
                    Log.d(TAG, "First frame available");
                    if (listener != null) {
                        listener.onVideoStarted();
                    }
                }
                
                @Override
                public void onCameraClosed() {
                    Log.d(TAG, "Camera closed");
                }
            });
            
            // Initialize capturer
            org.webrtc.SurfaceTextureHelper surfaceTextureHelper = org.webrtc.SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
            videoCapturer.initialize(surfaceTextureHelper, context, videoSource.getCapturerObserver());
            videoCapturer.startCapture(640, 480, 30);
            
            Log.d(TAG, "Video capturer created and started");
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating video capturer", e);
            if (listener != null) {
                listener.onError("Failed to initialize camera: " + e.getMessage());
            }
        }
    }
    
    /**
     * Configure audio manager for video calls
     */
    private void configureAudioManager() {
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(isSpeakerEnabled);
        audioManager.setMicrophoneMute(!isAudioEnabled);
    }
    
    /**
     * Create and send offer
     */
    private void createOffer() {
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        constraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        
        peerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {}
                    
                    @Override
                    public void onSetSuccess() {
                        Log.d(TAG, "Local description set successfully");
                        if (signalingManager != null) {
                            signalingManager.sendOffer(sessionDescription);
                        }
                    }
                    
                    @Override
                    public void onCreateFailure(String s) {}
                    
                    @Override
                    public void onSetFailure(String s) {
                        Log.e(TAG, "Failed to set local description: " + s);
                        if (listener != null) {
                            listener.onError("Failed to set local description: " + s);
                        }
                    }
                }, sessionDescription);
            }
            
            @Override
            public void onSetSuccess() {}
            
            @Override
            public void onCreateFailure(String s) {
                Log.e(TAG, "Failed to create offer: " + s);
                if (listener != null) {
                    listener.onError("Failed to create offer: " + s);
                }
            }
            
            @Override
            public void onSetFailure(String s) {}
        }, constraints);
    }
    
    /**
     * Create and send answer
     */
    private void createAnswer() {
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        constraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        
        peerConnection.createAnswer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {}
                    
                    @Override
                    public void onSetSuccess() {
                        Log.d(TAG, "Local description set successfully");
                        if (signalingManager != null) {
                            signalingManager.sendAnswer(sessionDescription);
                        }
                    }
                    
                    @Override
                    public void onCreateFailure(String s) {}
                    
                    @Override
                    public void onSetFailure(String s) {
                        Log.e(TAG, "Failed to set local description: " + s);
                        if (listener != null) {
                            listener.onError("Failed to set local description: " + s);
                        }
                    }
                }, sessionDescription);
            }
            
            @Override
            public void onSetSuccess() {}
            
            @Override
            public void onCreateFailure(String s) {
                Log.e(TAG, "Failed to create answer: " + s);
                if (listener != null) {
                    listener.onError("Failed to create answer: " + s);
                }
            }
            
            @Override
            public void onSetFailure(String s) {}
        }, constraints);
    }
    
    /**
     * Toggle microphone mute
     */
    public void toggleMute(boolean mute) {
        isAudioEnabled = !mute;
        if (localAudioTrack != null) {
            localAudioTrack.setEnabled(isAudioEnabled);
        }
        configureAudioManager();
        Log.d(TAG, "Microphone " + (isAudioEnabled ? "unmuted" : "muted"));
    }
    
    /**
     * Toggle speaker
     */
    public void toggleSpeaker(boolean speaker) {
        isSpeakerEnabled = speaker;
        configureAudioManager();
        Log.d(TAG, "Speaker " + (isSpeakerEnabled ? "enabled" : "disabled"));
    }
    
    /**
     * Switch camera (front/back)
     */
    public void switchCamera(boolean isFront) {
        if (videoCapturer != null && videoCapturer instanceof CameraVideoCapturer) {
            CameraVideoCapturer cameraCapturer = (CameraVideoCapturer) videoCapturer;
            cameraCapturer.switchCamera(new CameraVideoCapturer.CameraSwitchHandler() {
                @Override
                public void onCameraSwitchDone(boolean isFrontCamera) {
                    Log.d(TAG, "Camera switched to " + (isFrontCamera ? "front" : "back"));
                }
                
                @Override
                public void onCameraSwitchError(String errorDescription) {
                    Log.e(TAG, "Camera switch error: " + errorDescription);
                    if (listener != null) {
                        listener.onError("Camera switch failed: " + errorDescription);
                    }
                }
            });
        }
    }
    
    /**
     * End the call and cleanup resources
     */
    public void endCall() {
        Log.d(TAG, "Ending video call");
        
        if (signalingManager != null) {
            signalingManager.sendCallEnd();
        }
        
        if (peerConnection != null) {
            peerConnection.close();
            peerConnection = null;
        }
        
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
                videoCapturer.dispose();
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping video capturer", e);
            }
            videoCapturer = null;
        }
        
        if (listener != null) {
            listener.onCallDisconnected();
        }
    }
    
    /**
     * Cleanup all resources
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up Video WebRTC resources");
        
        endCall();
        
        if (localAudioTrack != null) {
            localAudioTrack.dispose();
            localAudioTrack = null;
        }
        
        if (localVideoTrack != null) {
            localVideoTrack.dispose();
            localVideoTrack = null;
        }
        
        if (remoteVideoTrack != null) {
            remoteVideoTrack.dispose();
            remoteVideoTrack = null;
        }
        
        if (audioSource != null) {
            audioSource.dispose();
            audioSource = null;
        }
        
        if (videoSource != null) {
            videoSource.dispose();
            videoSource = null;
        }
        
        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
            peerConnectionFactory = null;
        }
        
        if (eglBase != null) {
            eglBase.release();
            eglBase = null;
        }
        
        if (signalingManager != null) {
            signalingManager.cleanup();
            signalingManager = null;
        }
    }
    
    // SignalingManager.SignalingListener implementation
    @Override
    public void onOfferReceived(SessionDescription offer) {
        Log.d(TAG, "Offer received from initiator");
        
        if (peerConnection != null) {
            peerConnection.setRemoteDescription(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {}
                
                @Override
                public void onSetSuccess() {
                    Log.d(TAG, "Remote description set successfully");
                    createAnswer();
                }
                
                @Override
                public void onCreateFailure(String s) {}
                
                @Override
                public void onSetFailure(String s) {
                    Log.e(TAG, "Failed to set remote description: " + s);
                    if (listener != null) {
                        listener.onError("Failed to set remote description: " + s);
                    }
                }
            }, offer);
        }
    }
    
    @Override
    public void onAnswerReceived(SessionDescription answer) {
        Log.d(TAG, "Answer received from remote peer");
        
        if (peerConnection != null) {
            peerConnection.setRemoteDescription(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {}
                
                @Override
                public void onSetSuccess() {
                    Log.d(TAG, "Remote description set successfully");
                }
                
                @Override
                public void onCreateFailure(String s) {}
                
                @Override
                public void onSetFailure(String s) {
                    Log.e(TAG, "Failed to set remote description: " + s);
                    if (listener != null) {
                        listener.onError("Failed to set remote description: " + s);
                    }
                }
            }, answer);
        }
    }
    
    @Override
    public void onIceCandidateReceived(IceCandidate candidate) {
        Log.d(TAG, "ICE candidate received from remote peer");
        
        if (peerConnection != null) {
            peerConnection.addIceCandidate(candidate);
        }
    }
    
    @Override
    public void onCallEnded() {
        Log.d(TAG, "Call ended by remote peer");
        if (listener != null) {
            listener.onCallDisconnected();
        }
    }
    
    @Override
    public void onSignalingError(String error) {
        Log.e(TAG, "Signaling error: " + error);
        if (listener != null) {
            listener.onError("Signaling error: " + error);
        }
    }
    
    /**
     * PeerConnection observer to handle connection state changes
     */
    private class PeerConnectionObserver implements PeerConnection.Observer {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.d(TAG, "Signaling state changed: " + signalingState);
        }
        
        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.d(TAG, "ICE connection state changed: " + iceConnectionState);
            
            switch (iceConnectionState) {
                case CONNECTED:
                    Log.d(TAG, "ICE connection established");
                    if (listener != null) {
                        listener.onCallConnected();
                    }
                    break;
                case DISCONNECTED:
                    Log.d(TAG, "ICE connection disconnected");
                    break;
                case FAILED:
                    Log.e(TAG, "ICE connection failed");
                    if (listener != null) {
                        listener.onError("ICE connection failed");
                    }
                    break;
                case CLOSED:
                    Log.d(TAG, "ICE connection closed");
                    if (listener != null) {
                        listener.onCallDisconnected();
                    }
                    break;
            }
        }
        
        @Override
        public void onIceConnectionReceivingChange(boolean b) {
            Log.d(TAG, "ICE connection receiving change: " + b);
        }
        
        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            Log.d(TAG, "ICE gathering state changed: " + iceGatheringState);
        }
        
        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            Log.d(TAG, "Local ICE candidate generated");
            if (signalingManager != null) {
                signalingManager.sendIceCandidate(iceCandidate);
            }
        }
        
        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            Log.d(TAG, "ICE candidates removed: " + iceCandidates.length);
        }
        
        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.d(TAG, "onAddStream (deprecated) called. Remote stream added: " + mediaStream.videoTracks.size() + " video tracks, " + mediaStream.audioTracks.size() + " audio tracks");
            
            // For UNIFIED_PLAN, onAddTrack is preferred.
            // The logic for handling remote video track is now in onAddTrack.
        }
        
        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.d(TAG, "Remote stream removed");
        }
        
        @Override
        public void onDataChannel(org.webrtc.DataChannel dataChannel) {
            Log.d(TAG, "Data channel received");
        }
        
        @Override
        public void onRenegotiationNeeded() {
            Log.d(TAG, "Renegotiation needed");
        }
        
        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
            Log.d(TAG, "onAddTrack called. Remote track added: " + rtpReceiver.track().kind() + " from " + mediaStreams.length + " streams");
            
            // Handle remote video track
            if (rtpReceiver.track() instanceof VideoTrack) {
                remoteVideoTrack = (VideoTrack) rtpReceiver.track();
                Log.d(TAG, "Remote video track received and stored via onAddTrack");
                
                // Notify listener that remote video track is received
                if (listener != null) {
                    Log.d(TAG, "Notifying listener about remote video track");
                    listener.onRemoteVideoTrackReceived(remoteVideoTrack);
                } else {
                    Log.w(TAG, "Listener is null, cannot notify about remote video track");
                }
            }
            
            // Also handle audio track if needed
            if (rtpReceiver.track() instanceof AudioTrack) {
                Log.d(TAG, "Remote audio track received via onAddTrack");
                // Audio track is handled automatically by WebRTC
            }
        }
    }
}
