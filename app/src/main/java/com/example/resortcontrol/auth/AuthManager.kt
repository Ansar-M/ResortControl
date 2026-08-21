package com.example.resortcontrol.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.resortcontrol.UserAccount
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {

    private val auth: FirebaseAuth = Firebase.auth
    private val credentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            Log.d("AuthManager", "AuthState changed: ${firebaseUser?.email}")
            if (firebaseUser != null) {
                _currentUser.value = UserAccount(
                    name = firebaseUser.displayName ?: "User",
                    email = firebaseUser.email ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString()
                )
            } else {
                _currentUser.value = null
            }
        }
    }

    suspend fun signInWithGoogle(webClientId: String) {
        Log.d("AuthManager", "Starting sign in with Web Client ID: $webClientId")
        
        // Clear credential state to ensure the account picker is always shown and allows switching
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("AuthManager", "Failed to clear credential state", e)
        }

        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        try {
            Log.d("AuthManager", "Calling getCredential...")
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            Log.d("AuthManager", "Credential received type: ${credential.type}")
            
            val googleIdTokenCredential = when {
                credential is GoogleIdTokenCredential -> {
                    Log.d("AuthManager", "Direct GoogleIdTokenCredential instance")
                    credential
                }
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                    Log.d("AuthManager", "Converting from CustomCredential data")
                    GoogleIdTokenCredential.createFrom(credential.data)
                }
                else -> {
                    Log.e("AuthManager", "Unexpected credential type: ${credential.type}")
                    null
                }
            }

            if (googleIdTokenCredential != null) {
                Log.d("AuthManager", "Exchanging ID Token for Firebase credential...")
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                
                Log.d("AuthManager", "Firebase sign in successful: ${authResult.user?.uid}")
                authResult.user?.let { firebaseUser ->
                    val account = UserAccount(
                        name = firebaseUser.displayName ?: "User",
                        email = firebaseUser.email ?: "",
                        photoUrl = firebaseUser.photoUrl?.toString()
                    )
                    _currentUser.value = account
                    Log.d("AuthManager", "CurrentUser updated in Flow")
                }
            } else {
                Log.e("AuthManager", "googleIdTokenCredential is null after parsing")
            }
        } catch (e: androidx.credentials.exceptions.GetCredentialException) {
            Log.e("AuthManager", "Credential Manager Error: ${e.type} - ${e.message}", e)
            throw e
        } catch (e: Exception) {
            Log.e("AuthManager", "Unexpected error during sign in", e)
            throw e
        }
    }

    suspend fun signOut() {
        try {
            auth.signOut()
            Log.d("AuthManager", "Firebase signed out. Clearing credential state...")
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            Log.d("AuthManager", "Credential state cleared successfully.")
        } catch (e: Exception) {
            Log.e("AuthManager", "Error during sign out or clearing state", e)
        } finally {
            _currentUser.value = null
        }
    }
}
