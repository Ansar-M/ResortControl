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

        // 1. Pull latest changes
        pullFromCloud(userId)
        
        // 2. Push local changes
        // ORDER MATTERS: Push child entities first, then parents to handle soft deletions correctly
        pushBookings(userId)
        pushCabins(userId)
        pushCenters(userId)
        
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

    suspend fun pullFromCloud(userId: String): Boolean {
        if (userId.isEmpty()) {
            Log.e(TAG, "pullFromCloud aborted: userId is empty")
            return false
        }
        Log.d(TAG, "Starting pullFromCloud for UID: $userId")
        return try {
            // Pull Centers
            val centersSnapshot = db.collection("users").document(userId)
                .collection("centers").get().await()
            val remoteCenters = centersSnapshot.toObjects<CenterEntity>()
            Log.d(TAG, "Fetched ${remoteCenters.size} centers (Raw size: ${centersSnapshot.size()})")
            mergeCenters(remoteCenters)

            // Pull Cabins
            val cabinsSnapshot = db.collection("users").document(userId)
                .collection("cabins").get().await()
            val remoteCabins = cabinsSnapshot.toObjects<CabinEntity>()
            Log.d(TAG, "Fetched ${remoteCabins.size} cabins (Raw size: ${cabinsSnapshot.size()})")
            mergeCabins(remoteCabins)

            // Pull Bookings
            val bookingsSnapshot = db.collection("users").document(userId)
                .collection("bookings").get().await()
            val remoteBookings = bookingsSnapshot.toObjects<BookingEntity>()
            Log.d(TAG, "Fetched ${remoteBookings.size} bookings (Raw size: ${bookingsSnapshot.size()})")
            mergeBookings(remoteBookings)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error during pullFromCloud: ${e.message}", e)
            false
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
            if (l == null || r.lastModified > l.lastModified) {
                resortDao.insertCenter(r.copy(isSynced = true))
                Log.d(TAG, "Merged center: ${r.name}")
            }
        }
        // Handle remote deletions: delete local if it was synced but is now missing from remote
        val remoteNames = remote.map { it.name }.toSet()
        for (l in local) {
            if (l.isSynced && !l.isDeleted && l.name !in remoteNames) {
                resortDao.hardDeleteCenter(l)
                Log.d(TAG, "Deleted center locally (removed from cloud): ${l.name}")
            }
        }
    }

    private suspend fun mergeCabins(remote: List<CabinEntity>) {
        val local = resortDao.getAllCabinsWithDeleted()
        for (r in remote) {
            val l = local.find { it.id == r.id }
            if (l == null || r.lastModified > l.lastModified) {
                resortDao.insertCabin(r.copy(isSynced = true))
                Log.d(TAG, "Merged cabin: ${r.id}")
            }
        }
        // Handle remote deletions
        val remoteIds = remote.map { it.id }.toSet()
        for (l in local) {
            if (l.isSynced && !l.isDeleted && l.id !in remoteIds) {
                resortDao.hardDeleteCabin(l)
                Log.d(TAG, "Deleted cabin locally (removed from cloud): ${l.id}")
            }
        }
    }

    private suspend fun mergeBookings(remote: List<BookingEntity>) {
        val local = resortDao.getAllBookingsWithDeleted()
        for (r in remote) {
            val l = local.find { it.id == r.id }
            if (l == null || r.lastModified > l.lastModified) {
                resortDao.insertBooking(r.copy(isSynced = true))
                Log.d(TAG, "Merged booking: ${r.id}")
            }
        }
        // Handle remote deletions
        val remoteIds = remote.map { it.id }.toSet()
        for (l in local) {
            if (l.isSynced && !l.isDeleted && l.id !in remoteIds) {
                resortDao.hardDeleteBooking(l)
                Log.d(TAG, "Deleted booking locally (removed from cloud): ${l.id}")
            }
        }
    }
}
