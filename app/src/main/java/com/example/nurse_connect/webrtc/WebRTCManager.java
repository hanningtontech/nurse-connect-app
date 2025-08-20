package com.example.nurse_connect.webrtc;

import static android.net.wifi.SupplicantState.COMPLETED;
import static android.net.wifi.SupplicantState.DISCONNECTED;
import static com.example.nurse_connect.models.Message.MessageStatus.FAILED;
import static com.example.nurse_connect.ui.chat.VideoCallActivity.CallState.CONNECTED;
import static java.lang.Thread.State.NEW;

import static javax.net.ssl.SSLEngineResult.Status.CLOSED;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Manages WebRTC peer connections and audio streams for real-time audio calling
 */
public class WebRTCManager implements SignalingManager.SignalingListener {
    private static final String TAG = "WebRTCManager";
    
    private Context context;
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private AudioSource audioSource;
    private AudioTrack localAudioTrack;
    private AudioManager audioManager;
    private EglBase eglBase;
    
    private SignalingManager signalingManager;
    private WebRTCListener listener;
    private boolean isInitiator = false;
    private boolean isAudioEnabled = true;
    private boolean isSpeakerEnabled = false;
    
    // ICE servers for NAT traversal
    private List<PeerConnection.IceServer> iceServers;
    
    public interface WebRTCListener {
        void onCallConnected();
        void onCallDisconnected();
        void onAudioStarted();
        void onAudioStopped();
        void onError(String error);
        void onRemoteAudioReceived();
    }
    
    public WebRTCManager(Context context, WebRTCListener listener) {
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
        Log.d(TAG, "Initializing WebRTC");
        
        // Initialize PeerConnectionFactory
        PeerConnectionFactory.InitializationOptions initOptions = 
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions();
        PeerConnectionFactory.initialize(initOptions);
        
        // Create EglBase for video (even though we're only using audio)
        eglBase = EglBase.create();
        
        // Create PeerConnectionFactory
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(null) // Use default
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
                .createPeerConnectionFactory();
        
        Log.d(TAG, "WebRTC initialized successfully");
    }
    
    /**
     * Get the current peer connection instance
     * @return The peer connection or null if not created
     */
    public PeerConnection getPeerConnection() {
        return peerConnection;
    }

    /**
     * Setup ICE servers for NAT traversal
     */
    private void setupIceServers() {
        iceServers = new ArrayList<>();
        
        // Google's public STUN servers
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer());
        
        // Add TURN servers for better connectivity (you can replace these with your own TURN servers)
        // For now, using free TURN servers (these may not always work reliably)
        iceServers.add(PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443?transport=tcp")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer());
        
        Log.d(TAG, "ICE servers configured");
    }
    
    /**
     * Start a call as the initiator
     */
    public void startCall(String callId, String localUserId, String remoteUserId) {
        Log.d(TAG, "Starting call as initiator");
        this.isInitiator = true;
        
        // Initialize signaling
        signalingManager = new SignalingManager(callId, localUserId, remoteUserId, this, this);
        signalingManager.startListening();
        
        // Create peer connection
        createPeerConnection();
        
        // Create and add local audio stream
        createLocalAudioStream();
        
        // Create offer
        createOffer();
    }
    
    /**
     * Answer an incoming call
     */
    public void answerCall(String callId, String localUserId, String remoteUserId) {
        Log.d(TAG, "Answering incoming call");
        this.isInitiator = false;
        
        // Initialize signaling
        signalingManager = new SignalingManager(callId, localUserId, remoteUserId, this, this);
        signalingManager.startListening();
        
        // Create peer connection
        createPeerConnection();
        
        // Create and add local audio stream
        createLocalAudioStream();
        
        // Wait for offer from initiator
    }
    
    /**
     * Create peer connection with ICE servers
     */
    private void createPeerConnection() {
        Log.d(TAG, "Creating peer connection");
        
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED; // Enable TCP for better connectivity
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN; // Explicitly use Unified Plan
        
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnectionObserver());
        
        if (peerConnection == null) {
            Log.e(TAG, "Failed to create peer connection");
            if (listener != null) {
                listener.onError("Failed to create peer connection");
            }
        } else {
            Log.d(TAG, "Peer connection created successfully with Unified Plan SDP semantics");
        }
    }
    
    /**
     * Create local audio stream
     */
    private void createLocalAudioStream() {
        Log.d(TAG, "Creating local audio stream");
        
        // Create audio constraints
        MediaConstraints audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googHighpassFilter", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        
        // Create audio source
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        
        // Create local audio track
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio_track", audioSource);
        localAudioTrack.setEnabled(isAudioEnabled);
        
        // Add track directly to peer connection (Unified Plan compatible)
        if (peerConnection != null) {
            peerConnection.addTrack(localAudioTrack);
            Log.d(TAG, "Local audio track added to peer connection");
        }
        
        // Configure audio manager for voice call
        configureAudioManager();
    }
    
    /**
     * Configure audio manager for voice call
     */
    private void configureAudioManager() {
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(isSpeakerEnabled);
            Log.d(TAG, "Audio manager configured for voice call");
        }
    }
    
    /**
     * Create WebRTC offer
     */
    private void createOffer() {
        Log.d(TAG, "Creating WebRTC offer");
        
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));
        
        peerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d(TAG, "Offer created successfully");
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {}
                    
                    @Override
                    public void onSetSuccess() {
                        Log.d(TAG, "Local description set successfully");
                        // Send offer through signaling
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
     * Create WebRTC answer
     */
    private void createAnswer() {
        Log.d(TAG, "Creating WebRTC answer");
        
        // Check if peer connection is in the right state
        if (peerConnection == null || peerConnection.signalingState() != PeerConnection.SignalingState.HAVE_REMOTE_OFFER) {
            Log.e(TAG, "Cannot create answer - peer connection not in correct state: " + 
                  (peerConnection != null ? peerConnection.signalingState() : "null"));
            if (listener != null) {
                listener.onError("Cannot create answer - wrong peer connection state");
            }
            return;
        }
        
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));
        
        peerConnection.createAnswer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d(TAG, "Answer created successfully");
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {}
                    
                    @Override
                    public void onSetSuccess() {
                        Log.d(TAG, "Local description set successfully");
                        // Send answer through signaling
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
     * Toggle audio mute
     */
    public void toggleMute(boolean mute) {
        isAudioEnabled = !mute;
        if (localAudioTrack != null) {
            localAudioTrack.setEnabled(isAudioEnabled);
            Log.d(TAG, "Audio " + (mute ? "muted" : "unmuted"));
        }
    }

    /**
     * Toggle speaker
     */
    public void toggleSpeaker(boolean speaker) {
        isSpeakerEnabled = speaker;
        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(isSpeakerEnabled);
            Log.d(TAG, "Speaker " + (speaker ? "enabled" : "disabled"));
        }
    }

    /**
     * End the call
     */
    public void endCall() {
        Log.d(TAG, "Ending call");

        // Send end call signal
        if (signalingManager != null) {
            signalingManager.sendCallEnd();
            signalingManager.cleanup();
            signalingManager = null;
        }

        // Close peer connection
        if (peerConnection != null) {
            peerConnection.close();
            peerConnection = null;
        }

        // Stop local audio track
        if (localAudioTrack != null) {
            localAudioTrack.dispose();
            localAudioTrack = null;
        }

        // Dispose audio source
        if (audioSource != null) {
            audioSource.dispose();
            audioSource = null;
        }

        // Reset audio manager
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.setSpeakerphoneOn(false);
        }

        if (listener != null) {
            listener.onCallDisconnected();
        }
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        endCall();

        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
            peerConnectionFactory = null;
        }

        if (eglBase != null) {
            eglBase.release();
            eglBase = null;
        }

        Log.d(TAG, "WebRTC resources cleaned up");
    }

    // SignalingManager.SignalingListener implementation
    @Override
    public void onOfferReceived(SessionDescription offer) {
        Log.d(TAG, "Offer received from remote peer");

        if (peerConnection == null) {
            Log.e(TAG, "Cannot set remote description - peer connection is null");
            if (listener != null) {
                listener.onError("Peer connection not initialized");
            }
            return;
        }

        // Check if we can set remote description
        PeerConnection.SignalingState currentState = peerConnection.signalingState();
        if (currentState != PeerConnection.SignalingState.STABLE && 
            currentState != PeerConnection.SignalingState.HAVE_LOCAL_OFFER) {
            Log.e(TAG, "Cannot set remote description - wrong signaling state: " + currentState);
            if (listener != null) {
                listener.onError("Cannot set remote description - wrong signaling state: " + currentState);
            }
            return;
        }

        peerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {}

            @Override
            public void onSetSuccess() {
                Log.d(TAG, "Remote description set successfully");
                // Create answer
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

    @Override
    public void onAnswerReceived(SessionDescription answer) {
        Log.d(TAG, "Answer received from remote peer");

        if (peerConnection == null) {
            Log.e(TAG, "Cannot set remote description - peer connection is null");
            if (listener != null) {
                listener.onError("Peer connection not initialized");
            }
            return;
        }

        // Check if we can set remote description
        PeerConnection.SignalingState currentState = peerConnection.signalingState();
        if (currentState != PeerConnection.SignalingState.HAVE_LOCAL_OFFER) {
            Log.e(TAG, "Cannot set remote description - wrong signaling state: " + currentState);
            if (listener != null) {
                listener.onError("Failed to set remote answer sdp: Called in wrong state - " + currentState);
            }
            return;
        }

        peerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {}

            @Override
            public void onSetSuccess() {
                Log.d(TAG, "Remote description set successfully");
                // Connection should be established now
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

    @Override
    public void onIceCandidateReceived(IceCandidate candidate) {
        Log.d(TAG, "ICE candidate received from remote peer: " + candidate.sdp + " (type: " + candidate.sdpMid + ")");
        if (peerConnection != null) {
            // Check if peer connection is in a state where we can add ICE candidates
            PeerConnection.SignalingState signalingState = peerConnection.signalingState();
            if (signalingState == PeerConnection.SignalingState.CLOSED) {
                Log.w(TAG, "Cannot add ICE candidate - peer connection is closed");
                return;
            }
            
            peerConnection.addIceCandidate(candidate);
            Log.d(TAG, "ICE candidate added to peer connection");
        } else {
            Log.w(TAG, "Cannot add ICE candidate - peer connection is null");
        }
    }

    @Override
    public void onCallEnded() {
        Log.d(TAG, "Call ended by remote peer");
        endCall();
    }

    @Override
    public void onSignalingError(String error) {
        Log.e(TAG, "Signaling error: " + error);
        if (listener != null) {
            listener.onError("Signaling error: " + error);
        }
    }

    /**
     * PeerConnection observer to handle connection events
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
                case NEW:
                    Log.d(TAG, "ICE connection state: NEW - Initial state");
                    break;
                case CHECKING:
                    Log.d(TAG, "ICE connection state: CHECKING - Checking connectivity");
                    break;
                case CONNECTED:
                    Log.d(TAG, "ICE connection state: CONNECTED - Media can flow");
                    if (listener != null) {
                        listener.onCallConnected();
                        listener.onAudioStarted();
                    }
                    break;
                case COMPLETED:
                    Log.d(TAG, "ICE connection state: COMPLETED - All ICE candidates have been considered");
                    if (listener != null) {
                        listener.onCallConnected();
                        listener.onAudioStarted();
                    }
                    break;
                case DISCONNECTED:
                    Log.d(TAG, "ICE connection state: DISCONNECTED - Connection lost temporarily");
                    if (listener != null) {
                        listener.onCallDisconnected();
                    }
                    break;
                case FAILED:
                    Log.d(TAG, "ICE connection state: FAILED - Connection failed permanently");
                    if (listener != null) {
                        listener.onCallDisconnected();
                        listener.onError("ICE connection failed");
                    }
                    break;
                case CLOSED:
                    Log.d(TAG, "ICE connection state: CLOSED - Connection closed");
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
            Log.d(TAG, "New ICE candidate generated: " + iceCandidate.sdp + " (type: " + iceCandidate.sdpMid + ")");
            // Send ICE candidate to remote peer
            if (signalingManager != null) {
                signalingManager.sendIceCandidate(iceCandidate);
            }
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            Log.d(TAG, "ICE candidates removed");
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.d(TAG, "Remote stream added");
            if (listener != null) {
                listener.onRemoteAudioReceived();
            }
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.d(TAG, "Remote stream removed");
        }

        @Override
        public void onDataChannel(org.webrtc.DataChannel dataChannel) {
            Log.d(TAG, "Data channel created");
        }

        @Override
        public void onRenegotiationNeeded() {
            Log.d(TAG, "Renegotiation needed");
        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
            Log.d(TAG, "Track added");
        }
    }
}
