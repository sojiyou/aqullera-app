package com.example.aquallera.auth.data

import com.example.aquallera.core.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseDatabase: FirebaseDatabase,
) {
    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("Login failed"))
            ensureUserProfileExists(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { Result.success(it) } ?: Result.failure(Exception("Signup failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(fullName: String) {
        firebaseAuth.currentUser?.let {
            com.google.firebase.auth.FirebaseAuth.getInstance()
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build()
            it.updateProfile(profileUpdates).await()
        }
    }

    suspend fun createUserProfile(uid: String, fullName: String, email: String, number: String) {
        val user = User(
            uid = uid,
            fullName = fullName,
            email = email,
            number = number,
            createdAt = System.currentTimeMillis(),
        )
        firebaseDatabase.getReference("users").child(uid).setValue(user).await()
    }

    suspend fun updateUserProfile(uid: String, name: String, email: String, number: String) {
        updateProfile(name)
        val updates = mapOf(
            "name" to name,
            "email" to email,
            "number" to number,
            "fullName" to name,
        )
        firebaseDatabase.getReference("users").child(uid).updateChildren(updates).await()
    }

    suspend fun sendVerificationEmail() {
        firebaseAuth.currentUser?.sendEmailVerification()?.await()
    }

    suspend fun reloadUser() {
        firebaseAuth.currentUser?.reload()?.await()
    }

    fun isEmailVerified(): Boolean = firebaseAuth.currentUser?.isEmailVerified ?: false

    fun signOut() {
        firebaseAuth.signOut()
    }

    suspend fun isPhoneTaken(number: String): Boolean {
        val snapshot = firebaseDatabase.getReference("users")
            .orderByChild("number")
            .equalTo(number)
            .get()
            .await()
        return snapshot.exists()
    }

    suspend fun isEmailTakenInUsers(email: String): Boolean {
        val snapshot = firebaseDatabase.getReference("users")
            .orderByChild("email")
            .equalTo(email)
            .get()
            .await()
        return snapshot.exists()
    }

    suspend fun isEmailTakenInStations(email: String): Boolean {
        val snapshot = firebaseDatabase.getReference("waterStations")
            .orderByChild("email")
            .equalTo(email)
            .get()
            .await()
        return snapshot.exists()
    }

    private suspend fun ensureUserProfileExists(user: FirebaseUser) {
        val snapshot = firebaseDatabase.getReference("users").child(user.uid).get().await()
        if (!snapshot.exists()) {
            val stationSnapshot = firebaseDatabase.getReference("waterStations").child(user.uid).get().await()
            if (stationSnapshot.exists()) {
                val station = stationSnapshot.value as? Map<*, *>
                createUserProfile(
                    uid = user.uid,
                    fullName = (station?.get("ownerName") as? String)
                        ?: (station?.get("stationName") as? String) ?: "",
                    email = user.email ?: "",
                    number = (station?.get("phone") as? String) ?: "",
                )
            } else {
                createUserProfile(
                    uid = user.uid,
                    fullName = user.displayName ?: "",
                    email = user.email ?: "",
                    number = "",
                )
            }
        }
    }
}
