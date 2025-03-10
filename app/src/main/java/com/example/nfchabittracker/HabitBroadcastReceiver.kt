package com.example.nfchabittracker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class HabitBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.nfchabittracker.HABIT_NOTIFICATION" || intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val habitName = intent.getStringExtra("habitName") ?: "Habit Reminder"
            val notificationMessage = intent.getStringExtra("notificationMessage") ?: "It's time for your habit!"

            createNotificationChannel(context)
            showNotification(context, habitName, notificationMessage)
        }
    }

    private fun createNotificationChannel(context: Context) {
        val name = "Habit Reminders"
        val descriptionText = "Channel for habit reminder notifications"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("habitChannelId", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun showNotification(context: Context, habitName: String, message: String) {
        // Check for notification permission *immediately before* showing notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("HabitBroadcastReceiver", "Notification permission not granted, not showing notification")
                return // Do not show notification if permission is not granted
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, "habitChannelId")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your icon
            .setContentTitle(habitName)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Call notify() directly on NotificationManagerCompat instance
        NotificationManagerCompat.from(context).notify(generateNotificationId(habitName), builder.build())
    }

    private fun generateNotificationId(habitName: String): Int {
        return habitName.hashCode() // Simple hash for notification ID, consider making it more unique if needed
    }
}