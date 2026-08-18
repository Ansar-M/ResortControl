# Implementation Plan - Adaptive UI Optimization

This plan addresses the layout issues on phone screens and ensures a consistent, responsive experience across different device form factors (Phone, Tablet, Foldable).

## User Review Required

> [!IMPORTANT]
> - **WindowSizeClass**: I will use the standard Android `WindowSizeClass` API to detect the current screen format.
> - **Grid Adjustment**: The property grid will now automatically adjust its column count (1 on phones, 2 on medium screens, 3+ on tablets).
> - **Header Consolidation**: On small screens, the top search bar and action buttons will be reorganized to prevent overlapping.
> - **Drawer Behavior**: The side menu will remain a Modal Drawer on phones but can be optimized for larger screens.

## Proposed Changes

### 1. Build & Setup
- **[MODIFY] [build.gradle.kts (app)](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/app/build.gradle.kts)**: Add `androidx.compose.material3:material3-window-size-class` dependency.

### 2. Adaptive Logic Integration
- **[MODIFY] [MainActivity.kt](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/app/src/main/java/com/example/resortcontrol/MainActivity.kt)**:
    - Capture the `WindowSizeClass` using `calculateWindowSizeClass(this)`.
    - Pass the `WindowSizeClass` to `CabinListScreen` and `CabinDetailScreen`.

### 3. Cabin List Optimization
- **[MODIFY] [CabinListScreen.kt](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/app/src/main/java/com/example/resortcontrol/CabinListScreen.kt)**:
    - **Responsive Grid**: Replace `GridCells.Fixed(3)` with logic that chooses 1, 2, or 3 columns based on the window width.
    - **Adaptive Header**:
        - On "Compact" (Phone) screens: Move the search bar to its own row or make it expandable. Hide text labels on smaller buttons (icons only).
        - On "Expanded" (Tablet) screens: Maintain the current side-by-side layout.
    - **Drawer Fixes**: Ensure the "Delete Center" button is properly padded and accessible on all screen sizes.

### 4. Property Detail Optimization
- **[MODIFY] [CabinDetailScreen.kt](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/app/src/main/java/com/example/resortcontrol/CabinDetailScreen.kt)**:
    - **Responsive Photos**: Adjust image width (e.g., `fillMaxWidth()` on phones vs. `300.dp` on tablets).
    - **Layout Scaling**: Adjust horizontal padding and font sizes for a tighter fit on phone screens.

## Verification Plan

### Automated Verification
- Run `./gradlew :app:assembleDebug` to ensure dependency compatibility.

### Manual Verification
1. **Phone Test**: Open the app on a phone emulator (e.g., Pixel 8). Verify that the search bar doesn't overlap with icons and the property list is a single column.
2. **Tablet Test**: Open the app on a tablet emulator (e.g., Pixel Tablet). Verify that the grid remains 3 columns and the layout uses the extra space effectively.
3. **Delete Logic**: Verify that deleting a center works on both devices and the button is consistently visible in the side menu.
