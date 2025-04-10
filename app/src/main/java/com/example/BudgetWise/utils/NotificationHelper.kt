package com.example.BudgetWise.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.BudgetWise.R
import com.example.BudgetWise.activities.MainActivity

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "finance_tracker_channel"
        private const val BUDGET_NOTIFICATION_ID = 1001
        private const val REMINDER_NOTIFICATION_ID = 1002
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Finance Tracker Notifications"
            val descriptionText = "Notifications for budget alerts and reminders"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showBudgetWarningNotification(currentSpending: Double, budget: Double, currency: String) {
        val sharedPrefManager = SharedPrefManager.getInstance(context)
        if (!sharedPrefManager.areNotificationsEnabled()) return

        val percentSpent = (currentSpending / budget) * 100
        val threshold = sharedPrefManager.getBudgetThreshold()

        if (percentSpent >= threshold) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val message = if (currentSpending >= budget) {
                "You've exceeded your monthly budget of $currency$budget"
            } else {
                "You've used ${percentSpent.toInt()}% of your monthly budget"
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Create this icon
                .setContentTitle("Budget Alert")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                notify(BUDGET_NOTIFICATION_ID, builder.build())
            }
        }
    }

    fun showDailyReminderNotification() {
        val sharedPrefManager = SharedPrefManager.getInstance(context)
        if (!sharedPrefManager.areNotificationsEnabled()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Create this icon
            .setContentTitle("Daily Reminder")
            .setContentText("Don't forget to record today's expenses!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(REMINDER_NOTIFICATION_ID, builder.build())
        }
    }
}