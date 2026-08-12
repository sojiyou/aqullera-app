package com.example.aquallera.core.model

/** Profile row under `/users`; keeps both the old (name/number) and new field spellings. */
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
