package com.example.aquallera

object TimeUtils {
    fun to12Hour(timeStr: String): String {
        if (timeStr.isBlank()) return timeStr
        return try {
            val parts = timeStr.split(":")
            if (parts.size < 2) return timeStr
            val hour = parts[0].toInt()
            val minute = parts[1]
            val amPm = if (hour < 12) "AM" else "PM"
            val hour12 = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            "$hour12:$minute $amPm"
        } catch (e: NumberFormatException) {
            timeStr
        }
    }
}
