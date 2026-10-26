package com.example.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
    private val dayMonthFormat = SimpleDateFormat("EEE, MMM d", Locale.US)

    fun getTodayIso(): String {
        return isoFormat.format(Date())
    }

    fun formatDisplayDate(isoDate: String): String {
        return try {
            val date = isoFormat.parse(isoDate) ?: return isoDate
            val todayIso = getTodayIso()
            if (isoDate == todayIso) return "Today, ${dayMonthFormat.format(date)}"

            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayIso = isoFormat.format(cal.time)
            if (isoDate == yesterdayIso) return "Yesterday, ${dayMonthFormat.format(date)}"

            displayFormat.format(date)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun addDaysToIso(isoDate: String, days: Int): String {
        return try {
            val date = isoFormat.parse(isoDate) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = date
            cal.add(Calendar.DAY_OF_YEAR, days)
            isoFormat.format(cal.time)
        } catch (e: Exception) {
            isoDate
        }
    }
}
