package com.example.aquallera.core.util

/** Time formatting shared by the store, order and map screens. */
object TimeUtils {

    /**
     * Rewrites any 24-hour `HH:mm` occurrences in [timeStr] to 12-hour form with AM/PM,
     * leaving times that already carry AM/PM untouched.
     *
     * Works on whole strings rather than a single time, so ranges convert in one call:
     * `"09:00 - 17:30"` becomes `"9:00 AM - 5:30 PM"`. Matches the PWA's `to12Hour`.
     */
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
