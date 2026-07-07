package com.example.aquallera

object ErrorUtils {
    fun getFirebaseErrorMessage(error: Exception?): String {
        val message = error?.message ?: return "An unexpected error occurred."

        return when {
            message.contains("email-already-in-use") -> "This email is already registered."
            message.contains("invalid-email") -> "Please enter a valid email address."
            message.contains("weak-password") -> "Password must be at least 6 characters."
            message.contains("user-not-found") -> "No account found with this email."
            message.contains("wrong-password") || message.contains("invalid-credential") -> "Incorrect email or password."
            message.contains("too-many-requests") -> "Too many attempts. Please try again later."
            message.contains("network-request-failed") -> "Network error. Please check your connection."
            message.contains("user-disabled") -> "This account has been disabled."
            message.contains("internal-error") -> "An internal error occurred. Please try again."
            message.contains("permission-denied") -> "You don't have permission to perform this action."
            message.contains("operation-not-allowed") -> "This sign-in method is not enabled."
            else -> message
        }
    }
}
