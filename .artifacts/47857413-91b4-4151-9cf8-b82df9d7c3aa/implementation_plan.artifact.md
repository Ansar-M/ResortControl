# Implementation Plan - UI Enhancements and Bug Fixes

This plan addresses several UI improvements and a bug related to active center persistence during language changes.

## User Review Required

> [!IMPORTANT]
> - The language toggle will be added to the side menu (drawer) for both phones and tablets to ensure UI consistency.
> - The cabin count label will be added to the mobile header.
> - The active center will no longer reset to "Recreation Center" when switching languages if a different center was previously selected.

## Proposed Changes

### UI Components

#### [MODIFY] [CabinListScreen.kt](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/app/src/main/java/com/example/resortcontrol/CabinListScreen.kt)
- **Drawer Language Toggle**: Add a new `NavigationDrawerItem` to the `ModalDrawerSheet` for language switching (EN/RU). This will make it available on both phones and tablets.
- **Mobile Cabin Count**: Add a `Text` element displaying the number of shown/total cabins in the `isCompact` (phone) header, consistent with the tablet layout.

### Logic and State Management

#### [MODIFY] [MainActivity.kt](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/app/src/main/java/com/example/resortcontrol/MainActivity.kt)
- **Active Center Persistence**: Fix the `LaunchedEffect` that observes `centerEntities`. It currently resets `activeCenter` to "Recreation Center" if `centerEntities` is empty (which happens briefly during activity recreation after a language change).
- The fix will involve checking if `activeCenter` is already non-empty before applying any default or inserting a new center.

## Verification Plan

### Automated Tests
- Build the project using `./gradlew assembleDebug` to ensure no compilation errors.

### Manual Verification
- **Language Toggle**: Open the side menu on a phone/tablet and verify the language switch button is present and functional.
- **Cabin Count**: Verify that the "X of Y properties" label is visible on a phone layout.
- **Persistence**: Select a specific center (not the default one), change the language, and verify that the same center remains active.
