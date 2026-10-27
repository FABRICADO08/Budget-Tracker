package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.local.database.AppDatabase
import com.example.data.local.entity.RecurringOccurrence
import com.example.data.local.entity.RecurringTransaction
import com.example.domain.finance.RecurringReminderRules
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val REMINDER_CHANNEL_ID = "recurring_bill_reminders"
private const val REMINDER_CHANNEL_NAME = "Recurring bill reminders"
private const val PERIODIC_REMINDER_WORK = "recurring_bill_reminders_periodic"
private const val IMMEDIATE_REMINDER_WORK = "recurring_bill_reminders_immediate"

/**
 * Checks active schedules for the currently authenticated Firebase user and posts at most one
 * reminder per schedule occurrence. WorkManager is deliberately used instead of exact alarms:
 * delivery is best-effort and may be delayed by Doze or battery optimization.
 */
class RecurringReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
            if (!canPostNotifications(applicationContext)) return Result.success()

            createNotificationChannel(applicationContext)
            val notificationManager = NotificationManagerCompat.from(applicationContext)
            if (!notificationManager.areNotificationsEnabled()) return Result.success()

            val database = AppDatabase.getInstance(applicationContext)
            val dao = database.recurringTransactionDao()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())

            dao.getActiveForUser(userId).forEach { schedule ->
                if (!RecurringReminderRules.isWithinReminderWindow(
                        todayIso = today,
                        dueDateIso = schedule.nextDueDate,
                        reminderDaysBefore = schedule.reminderDaysBefore
                    )
                ) {
                    return@forEach
                }

                val occurrence = getOrCreateOccurrence(dao, schedule) ?: return@forEach
                if (occurrence.status.equals("paid", ignoreCase = true) ||
                    occurrence.reminderSentAt != null
                ) {
                    return@forEach
                }

                val notificationId = "${userId}:${schedule.id}:${schedule.nextDueDate}".hashCode()
                val openApp = PendingIntent.getActivity(
                    applicationContext,
                    notificationId,
                    Intent(applicationContext, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val overdue = schedule.nextDueDate < today
                val amountText = String.format(Locale.forLanguageTag("en-ZA"), "R %,.2f", schedule.amount)
                val title = when {
                    overdue -> if (schedule.type == "income") "Expected income overdue" else "Bill overdue"
                    schedule.type == "income" -> "Income expected soon"
                    else -> "Bill due soon"
                }
                val detail = "${schedule.description} · ${schedule.nextDueDate} · $amountText"
                val notification = NotificationCompat.Builder(applicationContext, REMINDER_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(detail)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
                    .setContentIntent(openApp)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()

                notificationManager.notify(notificationId, notification)
                // Persist only after notify succeeds. Unique work plus this field prevents routine
                // repeated reminders for the same schedule/date across worker executions.
                dao.markReminderSentIfPending(occurrence.id, schedule.userId, System.currentTimeMillis())
            }
            Result.success()
        } catch (_: SecurityException) {
            // The user may revoke notification permission between our pre-check and notify().
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun getOrCreateOccurrence(
        dao: com.example.data.local.dao.RecurringTransactionDao,
        schedule: RecurringTransaction
    ): RecurringOccurrence? {
        dao.getOccurrence(schedule.id, schedule.nextDueDate, schedule.userId)?.let { return it }

        val rowId = dao.insertOccurrenceIfNew(
            RecurringOccurrence(
                recurringId = schedule.id,
                dueDate = schedule.nextDueDate,
                amount = schedule.amount
            )
        )
        return if (rowId > 0L) {
            dao.getOccurrenceById(rowId, schedule.userId)
        } else {
            // The unique (recurring_id, due_date) index protects against concurrent inserts.
            dao.getOccurrence(schedule.id, schedule.nextDueDate, schedule.userId)
        }
    }

    private fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            REMINDER_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders for upcoming and overdue recurring bills or income."
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}

/** Idempotent reminder scheduling; WorkManager persists this work across app and device restarts. */
object RecurringReminderScheduler {
    fun ensureScheduled(context: Context) {
        val request = PeriodicWorkRequestBuilder<RecurringReminderWorker>(6, TimeUnit.HOURS).build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            PERIODIC_REMINDER_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /** Runs a check promptly after the user grants permission or taps the reminder control. */
    fun runNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<RecurringReminderWorker>().build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            IMMEDIATE_REMINDER_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
