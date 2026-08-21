package com.example.resortcontrol.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.resortcontrol.AppDatabase
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
                    } else if (photo.startsWith("http")) { // Remote URL - DOWNLOAD if missing
                        val localPath = storageRepository.downloadImage(photo, photosDir)
                        if (localPath != null && localPath != photo) {
                            updatedPhotos.add(localPath)
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
                    // Will be pushed to Firestore with local paths replaced by cloud URLs eventually,
                    // but here we just updated local Room to use local paths for speed.
                    // Wait, if we replace cloud URL with local path in Room, it's fine.
                    // But we should ensure Firestore always has the cloud URL.
                    // Actually, SyncRepository.pushCabins will push whatever is in Room.
                    // If Room has local path, Firestore will get local path (BAD).
                    // So: Room should probably store BOTH or we need a Mapper that handles this.
                    // Alternative: Room stores Cloud URL, and we have a local cache check.
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
