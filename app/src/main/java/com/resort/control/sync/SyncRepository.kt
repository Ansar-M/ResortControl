package com.resort.control.sync

import com.resort.control.BookingEntity
import com.resort.control.CabinEntity
import com.resort.control.CenterEntity
import com.resort.control.PendingDeletionEntity
import com.resort.control.ResortDao
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.tasks.await

class SyncRepository(
    private val resortDao: ResortDao,
    val storageRepository: StorageRepository = StorageRepository()
) {

    private val db = Firebase.firestore
    private val TAG = "SyncDebug"

    suspend fun syncWithCloud(userId: String) {
        Log.d(TAG, "Starting syncWithCloud for user: $userId")
        
        // 0. Process any pending image deletions from Storage
        processPendingDeletions()

        // 1. Push local changes FIRST to ensure deletions/updates reach the cloud before we pull
        // ORDER MATTERS: Push child entities first, then parents to handle soft deletions correctly
        pushBookings(userId)
        pushCabins(userId)
        pushCenters(userId)

        // 2. Pull latest changes from the cloud
        pullFromCloud(userId)
        
        Log.d(TAG, "SyncWithCloud finished")
    }

    private suspend fun processPendingDeletions() {
        val pending = resortDao.getAllPendingDeletions()
        if (pending.isEmpty()) return
        
        Log.d(TAG, "Processing ${pending.size} pending image deletions")
        for (item in pending) {
            try {
                storageRepository.deleteImage(item.url)
                resortDao.deletePendingDeletion(item)
                Log.d(TAG, "Successfully deleted pending image: ${item.url}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete pending image: ${item.url}", e)
            }
        }
    }

    private suspend fun pushCenters(userId: String) {
        val unsynced = resortDao.getUnsyncedCenters()
        Log.d(TAG, "Pushing ${unsynced.size} unsynced centers")
        for (center in unsynced) {
            try {
                if (center.isDeleted) {
                    db.collection("users").document(userId)
                        .collection("centers").document(center.name).delete().await()
                    resortDao.hardDeleteCenter(center)
                    Log.d(TAG, "Deleted center from cloud: ${center.name}")
                } else {
                    db.collection("users").document(userId)
                        .collection("centers").document(center.name)
                        .set(center.copy(isSynced = true)).await()
                    resortDao.insertCenter(center.copy(isSynced = true))
                    Log.d(TAG, "Uploaded center: ${center.name}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push center: ${center.name}", e)
            }
        }
    }

    private suspend fun pushCabins(userId: String) {
        val unsynced = resortDao.getUnsyncedCabins()
        Log.d(TAG, "Pushing ${unsynced.size} unsynced cabins")
        for (cabin in unsynced) {
            try {
                if (cabin.isDeleted) {
                    // Queue photos for deletion from Storage
                    for (photoUrl in cabin.photos) {
                        resortDao.insertPendingDeletion(PendingDeletionEntity(photoUrl))
                    }
                    
                    db.collection("users").document(userId)
                        .collection("cabins").document(cabin.id).delete().await()
                    resortDao.hardDeleteCabin(cabin)
                    Log.d(TAG, "Deleted cabin and its photos from cloud: ${cabin.id}")
                } else {
                    db.collection("users").document(userId)
                        .collection("cabins").document(cabin.id)
                        .set(cabin.copy(isSynced = true)).await()
                    resortDao.insertCabin(cabin.copy(isSynced = true))
                    Log.d(TAG, "Uploaded cabin: ${cabin.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push cabin: ${cabin.id}", e)
            }
        }
    }

    private suspend fun pushBookings(userId: String) {
        val unsynced = resortDao.getUnsyncedBookings()
        Log.d(TAG, "Pushing ${unsynced.size} unsynced bookings")
        for (booking in unsynced) {
            try {
                if (booking.isDeleted) {
                    db.collection("users").document(userId)
                        .collection("bookings").document(booking.id).delete().await()
                    resortDao.hardDeleteBooking(booking)
                    Log.d(TAG, "Deleted booking from cloud: ${booking.id}")
                } else {
                    db.collection("users").document(userId)
                        .collection("bookings").document(booking.id)
                        .set(booking.copy(isSynced = true)).await()
                    resortDao.insertBooking(booking.copy(isSynced = true))
                    Log.d(TAG, "Uploaded booking: ${booking.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push booking: ${booking.id}", e)
            }
        }
    }

    suspend fun pullFromCloud(userId: String): String? {
        if (userId.isEmpty()) {
            Log.e(TAG, "pullFromCloud aborted: userId is empty")
            return "ID пользователя пуст"
        }
        Log.d(TAG, "Starting pullFromCloud for UID: $userId")
        return try {
            // Pull Centers
            val centersSnapshot = db.collection("users").document(userId)
                .collection("centers").get().await()
            val remoteCenters = centersSnapshot.toObjects<CenterEntity>()
            Log.d(TAG, "Fetched ${remoteCenters.size} centers (Raw size: ${centersSnapshot.size()})")
            mergeCenters(remoteCenters)

            // Valid parent IDs for filtering orphaned records
            val validCenterNames = remoteCenters.filter { !it.isDeleted }.map { it.name }.toSet()

            // Pull Cabins
            val cabinsSnapshot = db.collection("users").document(userId)
                .collection("cabins").get().await()
            val allRemoteCabins = cabinsSnapshot.toObjects<CabinEntity>()
            
            // Filter out cabins that belong to non-existent or deleted centers
            val validRemoteCabins = allRemoteCabins.filter { it.centerName in validCenterNames }
            Log.d(TAG, "Fetched ${allRemoteCabins.size} cabins, kept ${validRemoteCabins.size} valid (Raw size: ${cabinsSnapshot.size()})")
            mergeCabins(validRemoteCabins)

            val validCabinIds = validRemoteCabins.filter { !it.isDeleted }.map { it.id }.toSet()

            // Pull Bookings
            val bookingsSnapshot = db.collection("users").document(userId)
                .collection("bookings").get().await()
            val allRemoteBookings = bookingsSnapshot.toObjects<BookingEntity>()
            
            // Filter out bookings that belong to non-existent or deleted cabins
            val validRemoteBookings = allRemoteBookings.filter { it.cabinId in validCabinIds }
            Log.d(TAG, "Fetched ${allRemoteBookings.size} bookings, kept ${validRemoteBookings.size} valid (Raw size: ${bookingsSnapshot.size()})")
            mergeBookings(validRemoteBookings)
            null // Success
        } catch (e: Exception) {
            Log.e(TAG, "Error during pullFromCloud: ${e.message}", e)
            e.localizedMessage ?: e.message ?: "Неизвестная ошибка сети"
        }
    }

    suspend fun clearAllCloudData(userId: String) {
        Log.d(TAG, "Clearing all cloud data for user: $userId")
        try {
            // 1. Clear Firestore Documents
            val collections = listOf("centers", "cabins", "bookings")
            for (collectionName in collections) {
                val snapshot = db.collection("users").document(userId)
                    .collection(collectionName).get().await()
                
                for (doc in snapshot.documents) {
                    doc.reference.delete().await()
                }
            }

            // 2. Clear Firebase Storage Files
            val storageRef = Firebase.storage.reference.child("users").child(userId).child("images")
            val listResult = storageRef.listAll().await()
            for (fileRef in listResult.items) {
                fileRef.delete().await()
            }
            
            Log.d(TAG, "Cloud data and storage cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cloud data", e)
            throw e
        }
    }

    private suspend fun mergeCenters(remote: List<CenterEntity>) {
        val local = resortDao.getAllCentersWithDeleted()
        for (r in remote) {
            val l = local.find { it.name == r.name }
            // Only merge if local doesn't exist or if remote is newer AND local is already synced
            // This prevents overwriting pending local changes with old cloud data
            if (l == null || (r.lastModified > l.lastModified && l.isSynced)) {
                resortDao.insertCenter(r.copy(isSynced = true))
                Log.d(TAG, "Merged center: ${r.name}")
            }
        }
        // Handle remote deletions
        val remoteNames = remote.map { it.name }.toSet()
        for (l in local) {
            if (l.isSynced && !l.isDeleted && l.name !in remoteNames) {
                try {
                    resortDao.hardDeleteCenter(l)
                    Log.d(TAG, "Deleted center locally (removed from cloud): ${l.name}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete center locally: ${l.name}", e)
                }
            }
        }
    }

    private suspend fun mergeCabins(remote: List<CabinEntity>) {
        val local = resortDao.getAllCabinsWithDeleted()
        for (r in remote) {
            val l = local.find { it.id == r.id }
            if (l == null || (r.lastModified > l.lastModified && l.isSynced)) {
                resortDao.insertCabin(r.copy(isSynced = true))
                Log.d(TAG, "Merged cabin: ${r.id}")
            }
        }
        // Handle remote deletions and orphans
        val remoteIds = remote.map { it.id }.toSet()
        for (l in local) {
            // Delete if missing from remote snapshot
            // Note: We remove the l.isSynced check here to ensure orphaned records (whose parents were deleted) 
            // are cleaned up regardless of their sync status.
            if (!l.isDeleted && l.id !in remoteIds) {
                try {
                    resortDao.hardDeleteCabin(l)
                    Log.d(TAG, "Deleted cabin locally (removed from cloud or orphan): ${l.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete cabin locally: ${l.id}", e)
                }
            }
        }
    }

    private suspend fun mergeBookings(remote: List<BookingEntity>) {
        val local = resortDao.getAllBookingsWithDeleted()
        for (r in remote) {
            val l = local.find { it.id == r.id }
            if (l == null || (r.lastModified > l.lastModified && l.isSynced)) {
                resortDao.insertBooking(r.copy(isSynced = true))
                Log.d(TAG, "Merged booking: ${r.id}")
            }
        }
        // Handle remote deletions and orphans
        val remoteIds = remote.map { it.id }.toSet()
        for (l in local) {
            // Delete if missing from remote snapshot
            if (!l.isDeleted && l.id !in remoteIds) {
                try {
                    resortDao.hardDeleteBooking(l)
                    Log.d(TAG, "Deleted booking locally (removed from cloud or orphan): ${l.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete booking locally: ${l.id}", e)
                }
            }
        }
    }
}
