# Implementation Plan - Robust Cloud Deletion and Sync Fix

The goal is to ensure that when a Center, Cabin, or Booking is deleted, it is reliably removed from both the local database and the cloud (Firestore). We must prevent the sync process from accidentally restoring items that were just deleted locally before they had a chance to sync their deletion status to the cloud.

## User Review Required

> [!IMPORTANT]
> Deleting a **Center** will now automatically mark all its **Cabins** and their **Bookings** for deletion. This ensures that the entire hierarchy is cleaned up from the cloud to save space.
> Local "Hard Deletion" (removal from DB) only happens **after** a successful cloud deletion response is received.

## Proposed Changes

### Database Layer

#### [MODIFY] [Database.kt](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/app/src/main/java/com/example/resortcontrol/Database.kt)

- **New DAO Queries**:
    - `getAllCentersWithDeleted()`, `getAllCabinsWithDeleted()`, `getAllBookingsWithDeleted()` to retrieve all records including those where `isDeleted = 1`.
    - `softDeleteCabinsByCenter(centerName: String)`
    - `softDeleteBookingsByCabin(cabinId: String)`
- **Transaction Update**:
    - Modify `softDeleteCenter` to be a `@Transaction` that also calls `softDeleteCabinsByCenter`.
    - Modify `softDeleteCabin` to be a `@Transaction` that also calls `softDeleteBookingsByCabin`.

### Sync Layer

#### [MODIFY] [SyncRepository.kt](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/app/src/main/java/com/example/resortcontrol/sync/SyncRepository.kt)

- **Merge Logic Improvement**:
    - Update `mergeCenters`, `mergeCabins`, and `mergeBookings` to use the "WithDeleted" queries.
    - A cloud record will only overwrite a local record if `remote.lastModified > local.lastModified`. This protects local deletions (which have the most recent timestamps) from being overwritten by stale cloud data during the `pull` phase.
- **Verification of Cloud Deletion**:
    - The `push` methods already handle `delete().await()` from Firestore. I will ensure these are correctly ordered.

### UI Layer

#### [MODIFY] [CabinListScreen.kt](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/app/src/main/java/com/example/resortcontrol/CabinListScreen.kt)

- Restore the missing `columns` logic to fix the build error.
- Ensure the grid layout is responsive as intended.

## Verification Plan

### Manual Verification
1. Create a Center with several Cabins and Bookings.
2. Delete the Center.
3. Verify that the Center and all its Cabins/Bookings are marked as deleted locally and subsequently removed from Firestore.
4. Verify that performing a "Sync" (or restarting the app) does not restore the deleted Center.
5. Verify the app builds successfully and the grid layout is functional.
