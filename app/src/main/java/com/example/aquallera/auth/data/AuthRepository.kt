package com.example.aquallera.auth.data

import com.example.aquallera.core.model.User
import com.example.aquallera.core.util.EmailService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authentication and user-profile access.
 *
 * Email verification does not use Firebase's built-in link flow: a 6-digit code is stored under
 * `/emailVerification/{uid}` and emailed via EmailJS, and every gate reads the `verified` flag
 * there. The PWA reads the same flag, so verifying in one client verifies in both.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseDatabase: FirebaseDatabase,
) {
    /** The signed-in Firebase user, or null when nobody is signed in. */
    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    /** Signs in and backfills a missing `/users` profile row (see [ensureUserProfileExists]). */
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

    /** Creates the Firebase Auth account only; the profile row and OTP are written by the caller. */
    suspend fun signUp(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { Result.success(it) } ?: Result.failure(Exception("Signup failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Updates the Auth displayName, which order creation reads for the customer name. */
    suspend fun updateProfile(fullName: String) {
        val request = UserProfileChangeRequest.Builder().setDisplayName(fullName).build()
        firebaseAuth.currentUser?.updateProfile(request)?.await()
    }

    /** Writes the initial `/users/{uid}` profile row. */
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

    /** Saves profile edits to both `/users/{uid}` and the Auth displayName. */
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

    /** Writes a fresh 6-digit code to /emailVerification/{uid} and emails it via EmailJS. */
    suspend fun sendVerificationCode(): Boolean {
        val user = firebaseAuth.currentUser ?: return false
        val email = user.email ?: return false
        val code = (100000..999999).random().toString()
        firebaseDatabase.getReference("emailVerification").child(user.uid).setValue(
            mapOf(
                "code" to code,
                "email" to email,
                "createdAt" to System.currentTimeMillis(),
                "verified" to false,
            )
        ).await()
        return EmailService.sendVerificationCode(email, code)
    }

    /** Returns null on success, otherwise an error message to show the user. */
    suspend fun verifyCode(input: String): String? {
        val user = firebaseAuth.currentUser ?: return "Not logged in."
        val ref = firebaseDatabase.getReference("emailVerification").child(user.uid)
        val snapshot = ref.get().await()
        if (!snapshot.exists()) return "No verification code found. Please resend."
        if (snapshot.child("verified").getValue(Boolean::class.java) == true) return null
        val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L
        if (System.currentTimeMillis() - createdAt > 600000) return "Code expired. Please resend."
        if (input != snapshot.child("code").getValue(String::class.java)) return "Incorrect code. Try again."
        ref.child("verified").setValue(true).await()
        return null
    }

    /** Reads the shared `/emailVerification/{uid}/verified` flag the PWA also gates on. */
    suspend fun isEmailVerified(): Boolean {
        val user = firebaseAuth.currentUser ?: return false
        return try {
            firebaseDatabase.getReference("emailVerification").child(user.uid).child("verified")
                .get().await().getValue(Boolean::class.java) == true
        } catch (_: Exception) { false }
    }

    /** Clears the local session. */
    fun signOut() {
        firebaseAuth.signOut()
    }

    /** Removes the user's profile data and their Firebase Auth account, then signs out. */
    suspend fun deleteAccount() {
        val user = firebaseAuth.currentUser ?: return
        firebaseDatabase.getReference("users").child(user.uid).removeValue().await()
        firebaseDatabase.getReference("emailVerification").child(user.uid).removeValue().await()
        user.delete().await()
        signOut()
    }

    /** Phone number from the profile row, or an empty string if unavailable. */
    suspend fun getUserPhone(uid: String): String {
        return try {
            val snapshot = firebaseDatabase.getReference("users").child(uid).child("number").get().await()
            snapshot.getValue(String::class.java) ?: ""
        } catch (_: Exception) { "" }
    }

    /** True if another account already uses this phone number. */
    suspend fun isPhoneTaken(number: String): Boolean {
        val snapshot = firebaseDatabase.getReference("users")
            .orderByChild("number")
            .equalTo(number)
            .get()
            .await()
        return snapshot.exists()
    }

    /** True if the email is already registered as a customer. */
    suspend fun isEmailTakenInUsers(email: String): Boolean {
        val snapshot = firebaseDatabase.getReference("users")
            .orderByChild("email")
            .equalTo(email)
            .get()
            .await()
        return snapshot.exists()
    }

    /** True if the email belongs to a station owner, who must use the admin site instead. */
    suspend fun isEmailTakenInStations(email: String): Boolean {
        val snapshot = firebaseDatabase.getReference("waterStations")
            .orderByChild("email")
            .equalTo(email)
            .get()
            .await()
        return snapshot.exists()
    }

    /**
     * Creates a `/users` row for an account that lacks one, seeding it from the matching
     * `/waterStations` record when present so station owners keep their name and phone.
     */
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
