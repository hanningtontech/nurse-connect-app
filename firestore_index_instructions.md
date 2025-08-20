# Firestore Index Creation Instructions

## Required Composite Index for Private Chats

Your app is getting a `FAILED_PRECONDITION` error because a composite index is missing for the `private_chats` collection.

### Index Details:
- **Collection**: `private_chats`
- **Fields**: 
  1. `participants` (Array)
  2. `updatedAt` (Descending)
  3. `__name__` (Descending)

### How to Create the Index:

1. **Go to Firebase Console**:
   - Visit: https://console.firebase.google.com/
   - Select your project: `nurseconnect-c68eb`

2. **Navigate to Firestore Database**:
   - Click on "Firestore Database" in the left sidebar
   - Click on the "Indexes" tab

3. **Create Composite Index**:
   - Click "Create Index" button
   - Set the following:
     - **Collection ID**: `private_chats`
     - **Fields**:
       - Field: `participants`, Type: `Array`
       - Field: `updatedAt`, Type: `Descending`
       - Field: `__name__`, Type: `Descending`
   - Click "Create"

4. **Wait for Index to Build**:
   - The index will show as "Building" initially
   - This may take a few minutes depending on your data size
   - Once it shows "Enabled", the error should be resolved

### Alternative Quick Method:
You can also click the direct link from your logcat:
```
https://console.firebase.google.com/v1/r/project/nurseconnect-c68eb/firestore/indexes?create_composite=Clhwcm9qZWN0cy9udXJzZWNvbm5lY3QtYzY4ZWIvZGF0YWJhc2VzLyhkZWZhdWx0KS9jb2xsZWN0aW9uR3JvdXBzL3ByaXZhdGVfY2hhdHMvaW5kZXhlcy9fEAEaEAoMcGFydGljaXBhbnRzGAEaDQoJdXBkYXRlZEF0EAIaDAoIX19uYW1lX18QAg
```

### Expected Result:
After the index is created and enabled, the `DirectMessagesFragment` should successfully load private chats without the `FAILED_PRECONDITION` error.
