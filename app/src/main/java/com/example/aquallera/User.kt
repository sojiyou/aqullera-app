package com.example.aquallera

data class User(
    val uid: String = "",
    val fullName: String = "",
    val name: String = "",
    val email: String = "",
    val number: String = "",
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    // No password field - handled by Firebase Auth
    val displayName: String get() = name.ifBlank { fullName }
    val displayPhone: String get() = number.ifBlank { phone }
    constructor() : this("", "", "", "", "", "", 0L)
}