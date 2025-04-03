package com.example.nfchabittracker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

class HabitItemAdapter(context: Context, private val habits: MutableList<Habit>) :
    ArrayAdapter<Habit>(context, R.layout.habit_item, habits) {

    private val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault()) // New time format
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val TAG = "HabitItemAdapter"

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.habit_item, parent, false)
        val habit = getItem(position) ?: return view

        val nameTextView = view.findViewById<TextView>(R.id.habitNameTextView)
        val daysTextView = view.findViewById<TextView>(R.id.habitDaysTextView)
        val timeTextView = view.findViewById<TextView>(R.id.habitTimeTextView)
        val snoozeButton = view.findViewById<Button>(R.id.snoozeButton)
        val skipDayButton = view.findViewById<Button>(R.id.skipDayButton)
        val deleteButton = view.findViewById<Button>(R.id.deleteButton)

        nameTextView?.text = habit.name
        val daysStringBuilder = StringBuilder()
        val sortedDays = habit.daysOfWeek.sorted()
        for (day in sortedDays) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_WEEK, day)
            daysStringBuilder.append(dayFormat.format(calendar.time)).append(" ")
        }
        daysTextView?.text = "Days: ${daysStringBuilder.toString().trim()}"

        // Format the time to AM/PM
        val calendarTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, habit.timeOfDayHour)
            set(Calendar.MINUTE, habit.timeOfDayMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        timeTextView?.text = "Time: ${timeFormat.format(calendarTime.time)}"

        snoozeButton?.setOnClickListener {
            val now = Calendar.getInstance()
            val habitTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, habit.timeOfDayHour)
                set(Calendar.MINUTE, habit.timeOfDayMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val today = Calendar.getInstance()
            val habitDayToday = habitTime.apply {
                set(Calendar.DAY_OF_YEAR, today.get(Calendar.DAY_OF_YEAR))
            }

            if (now.after(habitDayToday)) {
                handleSnoozeButtonClick(habit)
            } else {
                Toast.makeText(context, "Snooze available after the scheduled habit time.", Toast.LENGTH_SHORT).show()
            }
        }

        skipDayButton?.setOnClickListener {
            handleSkipDayButtonClick(habit)
        }

        deleteButton?.setOnClickListener {
            handleDeleteButtonClick(context, habit, position)
        }

        return view
    }

    private fun handleSnoozeButtonClick(habit: Habit) {
        Log.d(TAG, "Snooze button clicked for ${habit.name}")

        // Stop any existing snooze loop
        if (habit.isSnoozeLoopActive) {
            cancelSnoozeNotifications(habit)
            habit.isSnoozeLoopActive = false
            Toast.makeText(context, "Previous snooze stopped, new snooze starting in 1 minute.", Toast.LENGTH_SHORT).show()
        } else {
            // Stop any currently running or pending habit notifications for today
            cancelHabitNotificationsForToday(habit)
        }

        // Schedule new snooze loop after 1 minute
        val snoozeStartTime = System.currentTimeMillis() + 60 * 1000
        scheduleSnoozeNotifications(habit, snoozeStartTime, habit.activeSnoozeRequestCodes) // Pass the list
        habit.isSnoozeLoopActive = true
        habit.lastSnoozeStartTime = System.currentTimeMillis()
        (context as? MainActivity)?.saveHabits() // Save the updated habit
        Toast.makeText(context, "'${habit.name}' snoozed for 1 minute.", Toast.LENGTH_SHORT).show()
        notifyDataSetChanged() // Update UI if needed
    }

    private fun cancelHabitNotificationsForToday(habit: Habit) {
        val now = Calendar.getInstance()
        val todayDayOfWeek = now.get(Calendar.DAY_OF_WEEK)

        if (habit.daysOfWeek.contains(todayDayOfWeek)) {
            val intent = Intent(context, HabitBroadcastReceiver::class.java).apply {
                action = "com.example.nfchabittracker.HABIT_NOTIFICATION"
                putExtra("habitName", habit.name)
            }
            val requestCode = (habit.name + todayDayOfWeek.toString()).hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                Log.d(TAG, "Cancelled regular notification for '${habit.name}' today.")
            }
        }
    }

    private fun scheduleSnoozeNotifications(habit: Habit, startTimeMillis: Long, requestCodesList: MutableList<Int>) {
        val endTimeMillis = startTimeMillis + 2 * 60 * 1000 // 2 minutes
        var currentTimeMillis = startTimeMillis
        var iteration = 0

        while (currentTimeMillis <= endTimeMillis) {
            val intent = Intent(context, HabitBroadcastReceiver::class.java).apply {
                action = "com.example.nfchabittracker.HABIT_NOTIFICATION"
                putExtra("habitName", habit.name)
                putExtra("notificationMessage", habit.notificationMessage)
            }
            val requestCode = (habit.name + "snooze" + System.currentTimeMillis() + iteration).hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, currentTimeMillis, pendingIntent)
                } else {
                    Log.w(TAG, "Cannot schedule exact snooze alarm for '${habit.name}'. Exact alarm permission not granted.")
                    // Fallback to a non-exact alarm
                    alarmManager.set(AlarmManager.RTC_WAKEUP, currentTimeMillis, pendingIntent)
                }
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, currentTimeMillis, pendingIntent)
            }
            Log.d(TAG, "Scheduled snooze notification for '${habit.name}' at ${Date(currentTimeMillis)} (iteration $iteration, requestCode $requestCode)")
            requestCodesList.add(requestCode) // Store the request code

            currentTimeMillis += 10 * 1000 // 10 seconds interval
            iteration++
        }
        Log.d(TAG, "Snooze notifications scheduled for '${habit.name}'")
    }

    private fun cancelSnoozeNotifications(habit: Habit) {
        for (requestCode in habit.activeSnoozeRequestCodes) {
            val intent = Intent(context, HabitBroadcastReceiver::class.java).apply {
                action = "com.example.nfchabittracker.HABIT_NOTIFICATION"
                putExtra("habitName", habit.name)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE // Use FLAG_NO_CREATE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                Log.d(TAG, "Cancelled snooze notification with requestCode: $requestCode")
            }
        }
        habit.activeSnoozeRequestCodes.clear() // Clear the list of request codes
        habit.isSnoozeLoopActive = false
        // No toast here as the handleSnoozeButtonClick will handle the new snooze message
    }

    private fun handleSkipDayButtonClick(habit: Habit) {
        Log.d(TAG, "Skip Day button clicked for ${habit.name}")

        // Cancel all scheduled notifications for the current day
        val now = Calendar.getInstance()
        val todayDayOfWeek = now.get(Calendar.DAY_OF_WEEK)

        if (habit.daysOfWeek.contains(todayDayOfWeek)) {
            val intent = Intent(context, HabitBroadcastReceiver::class.java).apply {
                action = "com.example.nfchabittracker.HABIT_NOTIFICATION"
                putExtra("habitName", habit.name)
            }
            val requestCode = (habit.name + todayDayOfWeek.toString()).hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Cancelled scheduled notification for '${habit.name}' for today.")
        }

        // Cancel any active snooze loop
        cancelSnoozeNotifications(habit) // Use the improved cancellation

        habit.isSkippedToday = true
        habit.skipDayActivated = true // Set the flag
        (context as? MainActivity)?.saveHabits() // Assuming MainActivity has the saveHabits method
        Toast.makeText(context, "Notifications for '${habit.name}' skipped for today.", Toast.LENGTH_SHORT).show()
        notifyDataSetChanged() // Update the list view
    }

    private fun handleDeleteButtonClick(context: Context, habit: Habit, position: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (day in habit.daysOfWeek) {
            val intent = Intent(context, HabitBroadcastReceiver::class.java).apply {
                action = "com.example.nfchabittracker.HABIT_NOTIFICATION"
                putExtra("habitName", habit.name)
            }
            val requestCode = (habit.name + day.toString()).hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
        habits.removeAt(position)
        (context as? MainActivity)?.saveHabits()
        notifyDataSetChanged()
        (context as? MainActivity)?.updateVisibility()
    }
}