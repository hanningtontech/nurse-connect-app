# Stack Overflow Fix for Call Termination

## Problem Identified

The app was experiencing a **circular call stack overflow** during call termination, causing the app to crash. The stack trace showed an infinite loop between:

1. `AudioCallActivity.onCallDisconnected()` → `endCall()`
2. `endCall()` → `cleanupAudio()` → `realTimeAudioManager.endCall()`
3. `realTimeAudioManager.endCall()` → `WebRTCManager.endCall()` → `onCallDisconnected()`
4. Back to step 1, creating an infinite loop

## Root Cause

The issue was caused by:
- `onCallDisconnected()` calling `endCall()` when the call was active
- `endCall()` calling `cleanupAudio()` which called `realTimeAudioManager.endCall()`
- `realTimeAudioManager.endCall()` triggering `onCallDisconnected()` again
- This created a circular dependency that caused stack overflow

## Solution Implemented

### 1. **Added State Protection in `endCall()`**
```java
// Prevent multiple calls to endCall
if (currentCallState == CallState.ENDED) {
    android.util.Log.d("AudioCallActivity", "Call already ended, skipping endCall()");
    return;
}

// Set state to ENDED immediately to prevent circular calls
currentCallState = CallState.ENDED;
isCallActive = false;
```

### 2. **Modified `onCallDisconnected()` to Check State**
```java
@Override
public void onCallDisconnected() {
    runOnUiThread(() -> {
        android.util.Log.d("AudioCallActivity", "Real-time audio call disconnected");
        // Only end call if it's still active and we haven't already started ending it
        if (isCallActive && currentCallState != CallState.ENDED) {
            android.util.Log.d("AudioCallActivity", "Call was active, ending call due to disconnection");
            endCall();
        } else {
            android.util.Log.d("AudioCallActivity", "Call already ended or not active, skipping endCall()");
        }
    });
}
```

### 3. **Restructured `endCall()` Method**
- Moved state setting to the beginning to prevent circular calls
- Directly handle WebRTC cleanup instead of calling `cleanupAudio()`
- Added proper null checks and logging

### 4. **Updated `cleanupAudio()` Method**
- Now only used for cleanup during activity destruction
- Removed the call to `realTimeAudioManager.endCall()` to prevent circular calls
- Only performs resource cleanup

### 5. **Enhanced `onDestroy()` Method**
- Added proper audio resource cleanup
- Added null checks and logging
- Ensures all resources are properly released

## Key Changes Made

### AudioCallActivity.java
- **State protection**: Added checks to prevent multiple `endCall()` invocations
- **Circular call prevention**: Restructured call flow to avoid infinite loops
- **Better resource management**: Proper cleanup sequence without circular dependencies
- **Enhanced logging**: Added debug logs to track call flow

## Call Termination Flow Now Works As Follows

1. **User ends call** → `endCall()` is called
2. **State check** → If already ended, return immediately
3. **Set state** → Mark call as ENDED to prevent further calls
4. **Cleanup resources** → Stop timers, ringtone, WebRTC
5. **Update Firestore** → Mark call as ended in database
6. **Cleanup listeners** → Remove Firestore listeners
7. **Close activity** → Finish after short delay

## Testing

The fix prevents:
- ✅ Stack overflow crashes during call termination
- ✅ Infinite loops in call cleanup
- ✅ Multiple simultaneous call endings
- ✅ Resource leaks during activity destruction

## Debugging

Use these logcat filters to monitor the fix:
```
AudioCallActivity|WebRTCManager|RealTimeAudioManager
```

Look for these key log messages:
- `"Call already ended, skipping endCall()"`
- `"Call was active, ending call due to disconnection"`
- `"Call already ended or not active, skipping endCall()"`
- `"Cleaning up audio resources"`

This fix ensures stable call termination without crashes or infinite loops.
