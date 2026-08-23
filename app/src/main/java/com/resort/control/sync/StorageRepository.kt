package com.resort.control.sync

import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.io.File

class StorageRepository {

    private val storage = Firebase.storage

    suspend fun uploadImage(userId: String, localPath: String): String? {
        val file = File(localPath)
        if (!file.exists()) return null
        
        val storageRef = storage.reference
            .child("users")
            .child(userId)
            .child("images")
            .child(file.name)
            
        return try {
            storageRef.putFile(android.net.Uri.fromFile(file)).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadImage(url: String, targetDir: File): String? {
        return try {
            val storageRef = storage.getReferenceFromUrl(url)
            val fileName = storageRef.name
            val localFile = File(targetDir, fileName)
            
            if (localFile.exists()) return localFile.absolutePath
            
            storageRef.getFile(localFile).await()
            localFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteImage(url: String) {
        if (!url.startsWith("http")) return
        try {
            val storageRef = storage.getReferenceFromUrl(url)
            storageRef.delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
