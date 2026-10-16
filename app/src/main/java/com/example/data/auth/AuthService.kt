package com.example.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isAnonymous: Boolean = false
)

fun FirebaseUser.toAuthUser(): AuthUser = AuthUser(
    uid = uid,
    email = email,
    displayName = displayName ?: email?.substringBefore("@") ?: "User",
    photoUrl = photoUrl?.toString(),
    isAnonymous = isAnonymous
)

class AuthService(
    private val context: Context? = null
) {
    private val auth: FirebaseAuth? by lazy {
        try {
            if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    val currentUserFlow: Flow<AuthUser?> = callbackFlow {
        val currentFirebaseAuth = auth
        if (currentFirebaseAuth == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }

        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toAuthUser())
        }
        currentFirebaseAuth.addAuthStateListener(listener)
        trySend(currentFirebaseAuth.currentUser?.toAuthUser())
        awaitClose { currentFirebaseAuth.removeAuthStateListener(listener) }
    }

    val currentFirebaseUser: FirebaseUser?
        get() = auth?.currentUser

    fun getCurrentUserId(): String? = auth?.currentUser?.uid

    suspend fun signInWithEmail(email: String, pass: String): Result<AuthUser> {
        val currentAuth = auth ?: return Result.failure(Exception("Cloud services are not initialized. Please ensure internet access."))
        return try {
            val result = currentAuth.signInWithEmailAndPassword(email.trim(), pass.trim()).await()
            val user = result.user?.toAuthUser()
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Authentication succeeded but no user returned."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String): Result<AuthUser> {
        val currentAuth = auth ?: return Result.failure(Exception("Cloud services are not initialized. Please ensure internet access."))
        return try {
            val result = currentAuth.createUserWithEmailAndPassword(email.trim(), pass.trim()).await()
            val user = result.user?.toAuthUser()
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Registration succeeded but no user returned."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogleCredential(idToken: String): Result<AuthUser> {
        val currentAuth = auth ?: return Result.failure(Exception("Cloud services are not initialized. Please ensure internet access."))
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = currentAuth.signInWithCredential(credential).await()
            val user = result.user?.toAuthUser()
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Google Sign-In succeeded but user profile was null."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Helper to invoke Credential Manager for Google Sign-In in Android 14+ / Jetpack Compose.
     */
    suspend fun launchGoogleSignIn(context: Context, serverClientId: String? = null): Result<AuthUser> {
        return try {
            val credentialManager = CredentialManager.create(context)
            
            val clientId = serverClientId ?: "892457244076-dummy.apps.googleusercontent.com"
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                signInWithGoogleCredential(googleIdTokenCredential.idToken)
            } else {
                Result.failure(Exception("Unexpected credential type: ${credential.type}"))
            }
        } catch (e: GetCredentialException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth?.signOut()
    }
}

