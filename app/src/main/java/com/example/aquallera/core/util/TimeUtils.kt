package com.example.aquallera.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtils {
    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.US)
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.US)

    fun formatDateTime(timestamp: Long): String = dateTimeFormat.format(Date(timestamp))

    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))

    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

    fun formatRelative(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            diff < 604_800_000 -> "${diff / 86_400_000}d ago"
            else -> formatDate(timestamp)
        }
    }
}
