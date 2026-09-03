package com.example.aquallera.core.model

/** Profile row under `/users`; supports two field-name conventions used by the admin site and the PWA. */
data class User(
    val uid: String = "",
    val fullName: String = "",
    val name: String = "",
    val email: String = "",
    val number: String = "",
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis(),
) {
    val displayName: String get() = name.ifBlank { fullName }
    val displayPhone: String get() = number.ifBlank { phone }
}
