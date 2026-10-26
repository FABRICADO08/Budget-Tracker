package com.example.domain.finance

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Date helpers for recurring schedules. ISO yyyy-MM-dd strings sort chronologically. */
object RecurringDates {
    enum class Frequency { WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, ANNUALLY }

    fun nextDueDate(date: String, frequency: Frequency): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val parsed = requireNotNull(formatter.parse(date)) { "Date must use yyyy-MM-dd." }
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).apply {
            time = parsed
        }
        when (frequency) {
            Frequency.WEEKLY -> calendar.add(Calendar.DAY_OF_MONTH, 7)
            Frequency.BIWEEKLY -> calendar.add(Calendar.DAY_OF_MONTH, 14)
            Frequency.MONTHLY -> addCalendarMonthsClamped(calendar, 1)
            Frequency.QUARTERLY -> addCalendarMonthsClamped(calendar, 3)
            Frequency.ANNUALLY -> addCalendarMonthsClamped(calendar, 12)
        }
        return formatter.format(calendar.time)
    }

    private fun addCalendarMonthsClamped(calendar: Calendar, months: Int) {
        val originalDay = calendar.get(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.add(Calendar.MONTH, months)
        val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, minOf(originalDay, lastDay))
    }
}
