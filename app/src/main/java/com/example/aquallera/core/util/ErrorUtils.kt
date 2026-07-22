package com.example.aquallera.core.util

object ErrorUtils {
    private val authErrors = mapOf(
        "ERROR_EMAIL_ALREADY_IN_USE" to "This email is already registered. Try logging in instead.",
        "ERROR_INVALID_EMAIL" to "Please enter a valid email address.",
        "ERROR_WEAK_PASSWORD" to "Password should be at least 6 characters.",
        "ERROR_USER_DISABLED" to "This account has been disabled.",
        "ERROR_USER_NOT_FOUND" to "No account found with this email.",
        "ERROR_WRONG_PASSWORD" to "Incorrect password.",
        "ERROR_INVALID_CREDENTIAL" to "Invalid email or password.",
        "ERROR_TOO_MANY_REQUESTS" to "Too many attempts. Please try again later.",
        "ERROR_NETWORK_REQUEST_FAILED" to "Network error. Please check your internet connection.",
        "ERROR_INTERNAL_ERROR" to "An internal error occurred. Please try again.",
        "ERROR_OPERATION_NOT_ALLOWED" to "Email/password sign-in is not enabled.",
    )

    fun getFirebaseErrorMessage(exception: Exception): String {
        val message = exception.message ?: return "An unexpected error occurred."
        val key = authErrors.keys.find { message.contains(it, ignoreCase = true) }
        return authErrors[key] ?: message
    }
}
