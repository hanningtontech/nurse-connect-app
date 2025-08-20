# Flashcard System for Nurse Connect

## Overview
A comprehensive Quizlet-like flashcard system integrated into the Nurse Connect app, designed specifically for nursing education with spaced repetition learning algorithms.

## Features

### 🎯 Core Functionality
- **Career-based Learning**: Organized by nursing career levels (CNA, LPN, RN, BSN, MSN, DNP)
- **Course Structure**: Hierarchical organization by course and unit
- **Daily Study Sessions**: 20 flashcards per day with adaptive difficulty
- **Spaced Repetition**: Intelligent scheduling based on performance
- **Progress Tracking**: Comprehensive learning analytics and statistics

### 📚 Learning Experience
- **Multiple Choice Questions**: Interactive question format with immediate feedback
- **Answer Rationale**: Detailed explanations for correct answers
- **Performance Feedback**: Encouraging messages and progress indicators
- **Gamification**: Coin rewards for correct answers and streak tracking

### 🔄 Study Management
- **Deck Management**: Create and manage flashcard decks by topic
- **Enrollment System**: Subscribe to specific learning paths
- **Goal Setting**: Daily and weekly study targets
- **Streak Tracking**: Consecutive days of study motivation

## Architecture

### Models
- **Flashcard**: Individual flashcard with question, answer, rationale, and metadata
- **FlashcardDeck**: Collection of flashcards for a specific career/course/unit
- **User Progress**: Learning history, accuracy rates, and study patterns

### Services
- **FlashcardService**: Core business logic for flashcard operations
- **Learning Algorithms**: Spaced repetition and adaptive difficulty
- **Progress Tracking**: User performance analytics and statistics

### Activities
- **FlashcardSetupActivity**: Select career, course, and unit for study
- **FlashcardActivity**: Main study interface with interactive flashcards
- **FlashcardResultsActivity**: Session completion and results summary
- **FlashcardProgressActivity**: Learning progress and statistics (placeholder)
- **FlashcardSettingsActivity**: Customize study preferences (placeholder)

## Data Structure

### Firestore Collections
- `flashcards`: Individual flashcard content and metadata
- `flashcard_decks`: Organized collections by career/course/unit
- `user_progress`: Individual learning progress and statistics

### Flashcard Fields
- Question, answer, rationale
- Multiple choice options
- Career, course, unit categorization
- Difficulty level and source information
- Learning metadata (review count, accuracy, confidence)

### Deck Fields
- Name, description, and categorization
- Flashcard collection and count
- Learning settings and goals
- User enrollment and progress tracking

## Learning Algorithm

### Spaced Repetition
- **Correct Answers**: Increase review intervals (1 day → 3 days → 7 days → 14 days → 30 days)
- **Incorrect Answers**: Review in 1 day for reinforcement
- **Adaptive Scheduling**: Adjusts based on individual performance

### Performance Tracking
- **Confidence Score**: Calculated from accuracy over time
- **Review History**: Tracks when cards were last reviewed
- **Next Review**: Automatically schedules optimal review times

## User Experience

### Study Flow
1. **Setup**: Select career, course, and unit
2. **Study**: Answer 20 daily flashcards with immediate feedback
3. **Review**: See correct answers and detailed rationales
4. **Progress**: Track accuracy and learning statistics
5. **Repeat**: Daily sessions with spaced repetition

### Navigation
- Accessible from Community Hub → Public Tasks → Flashcards
- Seamless integration with existing app navigation
- Consistent UI/UX with app design language

## Future Enhancements

### PDF Integration
- **Automatic Extraction**: Generate flashcards from uploaded PDFs
- **Testbank Processing**: Convert existing question banks to flashcard format
- **Content Management**: Admin tools for managing flashcard content

### Advanced Features
- **Social Learning**: Share decks and study with peers
- **Competitions**: Leaderboards and study challenges
- **Analytics Dashboard**: Detailed learning insights and recommendations
- **Mobile Offline**: Download decks for offline study

### AI Integration
- **Smart Recommendations**: Suggest study focus areas
- **Difficulty Adjustment**: AI-powered question difficulty optimization
- **Learning Paths**: Personalized study sequences

## Technical Implementation

### Dependencies
- Firebase Firestore for data storage
- ViewBinding for UI management
- Material Design components
- Custom drawable resources

### Performance
- Efficient data loading with pagination
- Optimized Firestore queries
- Smooth animations and transitions
- Responsive UI design

## Getting Started

### For Users
1. Navigate to Community Hub
2. Select "Public Tasks"
3. Choose "🎯 Flashcards"
4. Select your nursing career, course, and unit
5. Start studying with 20 daily flashcards

### For Developers
1. Review the model classes in `models/` package
2. Examine the service layer in `services/FlashcardService.java`
3. Check UI implementations in `ui/flashcards/` package
4. Test with sample data generation

## Testing

### Sample Data
The system currently generates sample flashcards for testing:
- 20 questions per deck
- Multiple choice format
- Realistic nursing content
- Immediate feedback and scoring

### Integration Points
- Seamlessly integrated with existing quiz system
- Uses same career/course/unit structure
- Consistent with app navigation patterns
- Follows established design guidelines

## Conclusion

The flashcard system provides a robust foundation for nursing education within the Nurse Connect app. With its spaced repetition algorithm, comprehensive progress tracking, and intuitive user interface, it offers an engaging learning experience that complements the existing quiz system.

The modular architecture allows for easy expansion and integration with future features like PDF processing and AI-powered recommendations.
