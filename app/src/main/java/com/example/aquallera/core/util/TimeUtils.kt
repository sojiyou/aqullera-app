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

    fun to12Hour(timeStr: String): String {
        val regex = Regex("""\b(\d{1,2}):(\d{2})\b(?!\s*(?:AM|PM))""", RegexOption.IGNORE_CASE)
        return regex.replace(timeStr) { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return@replace match.value
            val minute = match.groupValues[2].toIntOrNull() ?: return@replace match.value
            if (hour > 23 || minute > 59) return@replace match.value
            val ampm = if (hour >= 12) "PM" else "AM"
            val h12 = if (hour % 12 == 0) 12 else hour % 12
            "${h12}:${match.groupValues[2]} $ampm"
        }
    }
}
