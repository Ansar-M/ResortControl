# Implementation Plan - Adaptive UI Polish & Deletion Fixes

This plan focuses on making the app look professional on both phones and tablets, while fixing the reliability of the deletion features.

## User Review Required

> [!IMPORTANT]
> - **Phone UI**: On phones, the property list will now show cards in a single column but with a more compact design to avoid oversized images.
> - **Deletion Parity**: The "Delete Center" button will now be available in the **main header on tablets** and in the **side menu on phones**, ensuring it's always accessible.
> - **Confirmation Dialogs**: No object (Center or Cabin) will be deleted without a clear confirmation popup.

## Proposed Changes

### 1. Cabin List Screen (Phone vs Tablet)
- **[MODIFY] [CabinListScreen.kt](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/app/src/main/java/com/example/resortcontrol/CabinListScreen.kt)**:
    - **Adaptive Grid**: Use `GridCells.Adaptive(300.dp)` on tablets and a fixed 1 column on phones.
    - **Compact Cards**: Update `CabinGridCard` to handle different widths gracefully. On phones, the image height will be limited.
    - **Tablet Deletion**: Add a trash icon next to the recreation center name in the top header when on a large screen.
    - **Drawer Fix**: Fix the side menu item layout so text and buttons don't overlap on narrow screens.

### 2. Deletion Logic & Sync
- **[MODIFY] [MainActivity.kt](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/app/src/main/java/com/example/resortcontrol/MainActivity.kt)**:
    - Improve `onDeleteCenter` to automatically switch to the next available center after deletion.
    - Ensure `onDeleteCabin` correctly triggers a database refresh.
- **[MODIFY] [SyncRepository.kt](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/app/src/main/java/com/example/resortcontrol/sync/SyncRepository.kt)**:
    - Ensure that when a Center is deleted, all its associated Cabins are also marked as `isDeleted` in Firestore.

### 3. UI/UX Polish
- **Confirmation Popups**: Centralize the deletion confirmation logic to avoid accidental data loss.
- **Header Spacing**: Standardize margins and padding for mobile headers.

## Verification Plan

### Manual Verification
1. **Phone Test**: Open on a phone. Verify the list is a single column and the trash icon in the drawer works.
2. **Tablet Test**: Open on a tablet. Verify the 3-column layout and locate the new trash icon in the top header.
3. **Deletion Flow**: Delete a center on one device and verify it disappears (after sync) on the other device.
