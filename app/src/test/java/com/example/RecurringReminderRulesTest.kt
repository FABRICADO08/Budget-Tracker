package com.example

import com.example.domain.finance.RecurringReminderRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringReminderRulesTest {
    @Test
    fun reminderWindowStartsOnConfiguredLeadDay() {
        assertFalse(
            RecurringReminderRules.isWithinReminderWindow(
                todayIso = "2026-10-24",
                dueDateIso = "2026-10-26",
                reminderDaysBefore = 1
            )
        )
        assertTrue(
            RecurringReminderRules.isWithinReminderWindow(
                todayIso = "2026-10-25",
                dueDateIso = "2026-10-26",
                reminderDaysBefore = 1
            )
        )
        assertTrue(
            RecurringReminderRules.isWithinReminderWindow(
                todayIso = "2026-10-26",
                dueDateIso = "2026-10-26",
                reminderDaysBefore = 1
            )
        )
    }

    @Test
    fun overdueScheduleRemainsEligibleUntilReminderHasBeenRecorded() {
        assertTrue(
            RecurringReminderRules.isWithinReminderWindow(
                todayIso = "2026-11-02",
                dueDateIso = "2026-10-26",
                reminderDaysBefore = 0
            )
        )
    }

    @Test
    fun reminderWindowHandlesMonthAndYearBoundaries() {
        assertTrue(
            RecurringReminderRules.isWithinReminderWindow(
                todayIso = "2026-12-31",
                dueDateIso = "2027-01-01",
                reminderDaysBefore = 1
            )
        )
        assertFalse(
            RecurringReminderRules.isWithinReminderWindow(
                todayIso = "2026-12-30",
                dueDateIso = "2027-01-01",
                reminderDaysBefore = 1
            )
        )
    }

    @Test
    fun malformedDatesAndInvalidLeadTimesAreIgnored() {
        assertFalse(
            RecurringReminderRules.isWithinReminderWindow(
                todayIso = "2026-02-30",
                dueDateIso = "2026-03-01",
                reminderDaysBefore = 1
            )
        )
        assertFalse(
            RecurringReminderRules.isWithinReminderWindow(
                todayIso = "2026-10-26",
                dueDateIso = "2026-10-26",
                reminderDaysBefore = 31
            )
        )
    }
}
