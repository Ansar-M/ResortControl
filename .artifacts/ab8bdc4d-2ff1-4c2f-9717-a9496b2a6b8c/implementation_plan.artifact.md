# Implementation Plan - Offline-First Sync with Google (Firebase)

This plan details the integration of Firebase Firestore and Authentication to provide a robust offline-first experience. Data will be saved locally in Room and automatically synced to the cloud when an internet connection is available.

## User Review Required

> [!IMPORTANT]
> - **Firebase Setup**: You MUST create a project in the [Firebase Console](https://console.firebase.google.com/), add an Android app with package `com.example.resortcontrol`, and place the `google-services.json` file in your `app/` folder.
> - **Google Auth**: I will implement the **Credential Manager API** for a modern "One Tap" sign-in experience.
> - **Data Privacy**: Each user will have their own "private" data partition in Firestore based on their Google UID.

## Proposed Changes

### 1. Build & Dependencies
- **[MODIFY] [libs.versions.toml](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/gradle/libs.versions.toml)**:
    - Add Firebase BOM, Firestore, Auth, and Google Services plugin.
    - Add Credential Manager libraries.
- **[MODIFY] [build.gradle.kts (project)](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/build.gradle.kts)**: Apply Google Services plugin.
- **[MODIFY] [build.gradle.kts (app)](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/app/build.gradle.kts)**: Add dependencies and apply the plugin.

### 2. Authentication Layer
- **[NEW] `AuthManager`**:
    - Uses `CredentialManager` to handle Google Sign-In.
    - Exchanges Google ID Tokens for Firebase Auth credentials.
    - Provides a `StateFlow<UserAccount?>` for the UI.

### 3. Data Synchronization Layer
- **[MODIFY] [Database.kt](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/app/src/main/java/com/example/resortcontrol/Database.kt)**:
    - Add a `lastModified: Long` field and `isSynced: Boolean` to `CabinEntity` and `BookingEntity`.
- **[NEW] `SyncRepository`**:
    - Observes Room changes and pushes "un-synced" items to Firestore.
    - Listens to Firestore snapshots and updates Room with remote changes (Conflict resolution: Latest timestamp wins).
- **[NEW] `SyncWorker` (WorkManager)**:
    - Scheduled to run when the device is online to ensure all local changes are eventually pushed to the cloud.

### 4. UI Integration
- **[MODIFY] [MainActivity.kt](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/app/src/main/java/com/example/resortcontrol/MainActivity.kt)**:
    - Initialize Firebase.
    - Show a Sign-In screen if no user is authenticated.
    - Provide a "Syncing..." status indicator in the top bar.

## Verification Plan

### Automated Verification
- **Build Check**: Verify successful compilation with new dependencies.
- **Sync Logic**: Unit test the mapper functions and timestamp-based conflict resolution.

### Manual Verification
- **Sign In**: Verify successful login via Google account.
- **Offline Entry**: Add a cabin while in Airplane Mode, verify it's saved locally.
- **Auto-Sync**: Disable Airplane Mode, verify the cabin appears in the Firebase Console.
- **Cloud Restoration**: Uninstall the app, reinstall, sign in, and verify all data is restored.
