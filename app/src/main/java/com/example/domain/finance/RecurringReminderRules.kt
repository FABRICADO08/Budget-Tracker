package com.example.domain.finance

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Date-window rules shared by recurring reminders and tests.
 *
 * Dates are ISO-8601 calendar days (yyyy-MM-dd), so comparisons are made on calendar days,
 * not milliseconds elapsed. Reminder timing is intentionally a date window, not an exact alarm.
 */
object RecurringReminderRules {
    fun isWithinReminderWindow(
        todayIso: String,
        dueDateIso: String,
        reminderDaysBefore: Int
    ): Boolean {
        if (reminderDaysBefore !in 0..30) return false
        val today = parseIso(todayIso) ?: return false
        val dueDate = parseIso(dueDateIso) ?: return false
        val earliestReminderDate = Calendar.getInstance(utc, Locale.ROOT).apply {
            time = dueDate
            add(Calendar.DAY_OF_MONTH, -reminderDaysBefore)
        }.time
        return format(today) >= format(earliestReminderDate)
    }

    private fun parseIso(value: String): Date? {
        if (!value.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return null
        return runCatching {
            formatter().parse(value)
        }.getOrNull()
    }

    private fun format(value: Date): String = formatter().format(value)

    private fun formatter() = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        isLenient = false
        timeZone = utc
    }

    private val utc: TimeZone
        get() = TimeZone.getTimeZone("UTC")
}
