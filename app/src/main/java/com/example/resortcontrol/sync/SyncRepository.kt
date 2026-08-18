package com.example.resortcontrol.sync

import com.example.resortcontrol.BookingEntity
import com.example.resortcontrol.CabinEntity
import com.example.resortcontrol.CenterEntity
import com.example.resortcontrol.ResortDao
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.tasks.await

class SyncRepository(private val resortDao: ResortDao) {

    private val db = Firebase.firestore

    suspend fun syncWithCloud(userId: String) {
        // First, pull latest changes to avoid overwriting remote data with stale local data
        pullFromCloud(userId)
        
        // Then, push local changes (including deletions)
        pushCenters(userId)
        pushCabins(userId)
        pushBookings(userId)
    }

    private suspend fun pushCenters(userId: String) {
        val unsynced = resortDao.getUnsyncedCenters()
        for (center in unsynced) {
            try {
                if (center.isDeleted) {
                    db.collection("users").document(userId)
                        .collection("centers").document(center.name).delete().await()
                    resortDao.hardDeleteCenter(center)
                } else {
                    db.collection("users").document(userId)
                        .collection("centers").document(center.name)
                        .set(center.copy(isSynced = true)).await()
                    resortDao.insertCenter(center.copy(isSynced = true))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun pushCabins(userId: String) {
        val unsynced = resortDao.getUnsyncedCabins()
        for (cabin in unsynced) {
            try {
                if (cabin.isDeleted) {
                    db.collection("users").document(userId)
                        .collection("cabins").document(cabin.id).delete().await()
                    resortDao.hardDeleteCabin(cabin)
                } else {
                    db.collection("users").document(userId)
                        .collection("cabins").document(cabin.id)
                        .set(cabin.copy(isSynced = true)).await()
                    resortDao.insertCabin(cabin.copy(isSynced = true))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun pushBookings(userId: String) {
        val unsynced = resortDao.getUnsyncedBookings()
        for (booking in unsynced) {
            try {
                if (booking.isDeleted) {
                    db.collection("users").document(userId)
                        .collection("bookings").document(booking.id).delete().await()
                    resortDao.hardDeleteBooking(booking)
                } else {
                    db.collection("users").document(userId)
                        .collection("bookings").document(booking.id)
                        .set(booking.copy(isSynced = true)).await()
                    resortDao.insertBooking(booking.copy(isSynced = true))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun pullFromCloud(userId: String) {
        try {
            // Pull Centers
            val remoteCenters = db.collection("users").document(userId)
                .collection("centers").get().await().toObjects<CenterEntity>()
            mergeCenters(remoteCenters)

            // Pull Cabins
            val remoteCabins = db.collection("users").document(userId)
                .collection("cabins").get().await().toObjects<CabinEntity>()
            mergeCabins(remoteCabins)

            // Pull Bookings
            val remoteBookings = db.collection("users").document(userId)
                .collection("bookings").get().await().toObjects<BookingEntity>()
            mergeBookings(remoteBookings)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun mergeCenters(remote: List<CenterEntity>) {
        val local = resortDao.getAllCenters()
        for (r in remote) {
            val l = local.find { it.name == r.name }
            if (l == null || r.lastModified > l.lastModified) {
                resortDao.insertCenter(r.copy(isSynced = true))
            }
        }
    }

    private suspend fun mergeCabins(remote: List<CabinEntity>) {
        val local = resortDao.getAllCabins()
        for (r in remote) {
            val l = local.find { it.id == r.id }
            if (l == null || r.lastModified > l.lastModified) {
                resortDao.insertCabin(r.copy(isSynced = true))
            }
        }
    }

    private suspend fun mergeBookings(remote: List<BookingEntity>) {
        val local = resortDao.getAllBookings()
        for (r in remote) {
            val l = local.find { it.id == r.id }
            if (l == null || r.lastModified > l.lastModified) {
                resortDao.insertBooking(r.copy(isSynced = true))
            }
        }
    }
}
