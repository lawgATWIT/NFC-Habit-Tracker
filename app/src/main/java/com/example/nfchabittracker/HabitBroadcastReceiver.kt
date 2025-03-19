package com.example.nfchabittracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HabitBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "HabitBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val habitName = intent.getStringExtra("habitName") ?: "Habit Reminder"
        val notificationMessage = intent.getStringExtra("notificationMessage") ?: "Time for your habit!"
        Log.d(TAG, "Received alarm for '$habitName' with message '$notificationMessage'")

        // Load habits from SharedPreferences
        val sharedPreferences = context.getSharedPreferences("HabitPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val habitsJson = sharedPreferences.getString("habitList", null)

        if (habitsJson != null) {
            val type = object : TypeToken<List<Habit>>() {}.type
            val habitList: List<Habit> = gson.fromJson(habitsJson, type) ?: emptyList()
            val habit = habitList.find { it.name == habitName }

            if (habit != null) {
                // Check if habit is snoozed
                if (habit.snoozedUntil == 0L || System.currentTimeMillis() > habit.snoozedUntil) {
                    // Not snoozed or snooze expired, proceed with notification
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            Log.e(TAG, "Cannot show notification: Missing POST_NOTIFICATIONS permission")
                            return
                        }
                    }
                    createNotificationChannel(context)
                    showNotification(context, habitName, notificationMessage)
                } else {
                    Log.d(TAG, "Habit '$habitName' is snoozed until ${habit.snoozedUntil}, skipping notification")
                }
            } else {
                Log.e(TAG, "Habit '$habitName' not found in shared preferences, skipping notification")
            }
        } else {
            Log.e(TAG, "No habits found in shared preferences, skipping notification")
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "habitChannelId",
                "Habit Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Channel for habit reminders" }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    private fun showNotification(context: Context, habitName: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = (habitName + System.currentTimeMillis()).hashCode() // Unique ID for each notification
        val notification = NotificationCompat.Builder(context, "habitChannelId")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(habitName)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
        Log.d(TAG, "Notification shown for '$habitName' with ID $notificationId")
    }
}