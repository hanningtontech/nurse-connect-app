# 🏥 Nurse Connect

A comprehensive real-time communication and study platform designed specifically for nursing students and professionals.

## 📱 Features

### 🎧 Real-Time Audio Calling
- **Live voice communication** between users
- **No ringtone on caller's side** - only shows "Calling..." status
- **Automatic ringtone on receiver's side** with full-screen notification
- **Accept/Decline functionality** with real-time audio streaming
- **Mute/unmute** and **speaker toggle** during calls
- **Call duration tracking** and proper resource management

### 💬 Advanced Messaging System
- **Direct Messages**: Private one-on-one conversations
- **Group Chats**: Create and manage study groups
- **Public Groups**: Join community discussions
- **Group Invitations**: Invite system for private groups
- **Real-time message delivery** with read receipts
- **Message caching** for offline viewing

### 🎨 Dynamic Theming
- **Light and Dark themes** with custom color schemes
- **Automatic theme switching** based on system preferences
- **Consistent theming** across all activities and fragments

### 👥 Community Features
- **User profiles** with academic information
- **Study groups** for collaborative learning
- **Community hub** for nursing discussions
- **User search** and connection features

### 📚 Study Materials
- **Document sharing** and management
- **PDF viewer** with annotation support
- **Favorites system** for important materials
- **Download management** for offline access

## 🛠️ Technical Stack

- **Language**: Java
- **Platform**: Android (API 24+)
- **Database**: Firebase Firestore
- **Authentication**: Firebase Auth
- **Storage**: Firebase Storage
- **Real-time Communication**: Custom audio streaming with Android AudioRecord/AudioTrack
- **Architecture**: MVVM with Repository pattern
- **UI**: Material Design components

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK API 24 or higher
- Firebase project with Firestore, Auth, and Storage enabled

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/YOUR_USERNAME/nurse-connect-app.git
   cd nurse-connect-app
   ```

2. **Set up Firebase**
   - Create a new Firebase project
   - Enable Firestore Database, Authentication, and Storage
   - Download `google-services.json` and place it in the `app/` directory
   - Update Firestore security rules (see `firestore.rules`)

3. **Build and run**
   ```bash
   ./gradlew assembleDebug
   ```

### Firebase Configuration

#### Firestore Security Rules
The app requires specific Firestore security rules for proper functionality. Copy the rules from `firestore.rules` to your Firebase console.

#### Authentication
Enable the following sign-in methods in Firebase Auth:
- Email/Password
- Google Sign-In (optional)

## 📋 App Structure

```
app/src/main/java/com/example/nurse_connect/
├── adapters/          # RecyclerView adapters
├── data/             # Repository classes
├── models/           # Data models
├── services/         # Background services
├── ui/               # Activities and fragments
├── utils/            # Utility classes
├── viewmodels/       # MVVM ViewModels
└── webrtc/           # Real-time audio components
```

## 🎯 Key Components

### Audio Calling System
- **RealTimeAudioManager**: Handles live audio recording and playback
- **CallNotificationService**: Manages incoming call notifications
- **AudioCallActivity**: Call interface with controls

### Messaging System
- **MessageListenerService**: Real-time message synchronization
- **PrivateChatActivity**: Direct messaging interface
- **GroupChatActivity**: Group conversation management

### Theme System
- **ThemeManager**: Dynamic theme switching
- **Custom color schemes** for light and dark modes
- **Consistent styling** across all components

## 🔧 Configuration

### Audio Permissions
The app requires the following permissions for audio calling:
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

### Network Configuration
Ensure your `network_security_config.xml` allows necessary connections for Firebase and media streaming.

## 🧪 Testing

### Audio Calling
1. Install the app on two devices
2. Log in with different accounts
3. Start a call from one device
4. Accept the call on the receiving device
5. Test mute, speaker, and end call functions

### Messaging
1. Create direct messages between users
2. Test group creation and invitations
3. Verify real-time message delivery
4. Test offline message caching

## 📱 Screenshots

*Add screenshots of your app here*

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Firebase for backend services
- Material Design for UI components
- Android AudioRecord/AudioTrack for real-time audio
- The nursing community for inspiration and feedback

## 📞 Support

For support, email your-email@example.com or create an issue in this repository.

---

**Built with ❤️ for the nursing community**
