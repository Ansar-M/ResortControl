package com.resort.control.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.resort.control.AppDatabase
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.io.File

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val userId = Firebase.auth.currentUser?.uid ?: return@withContext Result.failure()
        
        val database = AppDatabase.getDatabase(applicationContext)
        val resortDao = database.resortDao()
        val storageRepository = StorageRepository()
        val syncRepository = SyncRepository(resortDao, storageRepository)
        val photosDir = File(applicationContext.filesDir, "cabin_photos")
        if (!photosDir.exists()) photosDir.mkdirs()

        try {
            // 1. Sync data (Firestore) - This now PULLS then PUSHES
            syncRepository.syncWithCloud(userId)
            
            // 2. Sync images (Storage)
            val cabins = resortDao.getAllCabins()
            for (cabin in cabins) {
                val updatedPhotos = mutableListOf<String>()
                var changed = false
                
                for (photo in cabin.photos) {
                    if (photo.startsWith("/")) { // Local path - UPLOAD
                        val cloudUrl = storageRepository.uploadImage(userId, photo)
                        if (cloudUrl != null) {
                            updatedPhotos.add(cloudUrl)
                            changed = true
                        } else {
                            updatedPhotos.add(photo) 
                        }
                    } else {
                        updatedPhotos.add(photo)
                    }
                }
                
                if (changed) {
                    resortDao.insertCabin(cabin.copy(photos = updatedPhotos, isSynced = false))
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
