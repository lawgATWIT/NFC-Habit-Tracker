package com.example.nfchabittracker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager // Added this import
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

class CreateHabitActivity : AppCompatActivity() {

    private lateinit var habitNameEditText: EditText
    private lateinit var notificationMessageEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var hourNumberPicker: NumberPicker
    private lateinit var minuteNumberPicker: NumberPicker
    private lateinit var mondayCheckBox: CheckBox
    private lateinit var tuesdayCheckBox: CheckBox
    private lateinit var wednesdayCheckBox: CheckBox
    private lateinit var thursdayCheckBox: CheckBox
    private lateinit var fridayCheckBox: CheckBox
    private lateinit var saturdayCheckBox: CheckBox
    private lateinit var sundayCheckBox: CheckBox

    private var selectedHour: Int = 0
    private var selectedMinute: Int = 0

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        private const val NOTIFICATION_INTERVAL_MS = 10_000L // 10 seconds
        private const val NOTIFICATION_DURATION_MS = 120_000L // 2 minutes
        private const val TAG = "CreateHabitActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_habit)
        Log.d(TAG, "CreateHabitActivity started")

        habitNameEditText = findViewById(R.id.habitNameEditText)
        notificationMessageEditText = findViewById(R.id.notificationMessageEditText)
        saveButton = findViewById(R.id.saveButton)
        hourNumberPicker = findViewById(R.id.hourNumberPicker)
        minuteNumberPicker = findViewById(R.id.minuteNumberPicker)
        mondayCheckBox = findViewById(R.id.mondayCheckBox)
        tuesdayCheckBox = findViewById(R.id.tuesdayCheckBox)
        wednesdayCheckBox = findViewById(R.id.wednesdayCheckBox)
        thursdayCheckBox = findViewById(R.id.thursdayCheckBox)
        fridayCheckBox = findViewById(R.id.fridayCheckBox)
        saturdayCheckBox = findViewById(R.id.saturdayCheckBox)
        sundayCheckBox = findViewById(R.id.sundayCheckBox)

        hourNumberPicker.minValue = 0
        hourNumberPicker.maxValue = 23
        minuteNumberPicker.minValue = 0
        minuteNumberPicker.maxValue = 59

        val calendar = Calendar.getInstance()
        selectedHour = calendar.get(Calendar.HOUR_OF_DAY)
        selectedMinute = calendar.get(Calendar.MINUTE)
        hourNumberPicker.value = selectedHour
        minuteNumberPicker.value = selectedMinute

        hourNumberPicker.setOnValueChangedListener { _, _, newVal -> selectedHour = newVal }
        minuteNumberPicker.setOnValueChangedListener { _, _, newVal -> selectedMinute = newVal }

        saveButton.setOnClickListener {
            Log.d(TAG, "Save button clicked")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
                } else {
                    saveHabitAndScheduleAlarms()
                }
            } else {
                saveHabitAndScheduleAlarms()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            saveHabitAndScheduleAlarms()
        } else {
            Toast.makeText(this, "Notification permission required", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveHabitAndScheduleAlarms() {
        val name = habitNameEditText.text.toString()
        val message = notificationMessageEditText.text.toString()

        if (name.isBlank() || message.isBlank()) {
            Toast.makeText(this, "Please enter a name and message", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedDays = mutableSetOf<Int>()
        if (mondayCheckBox.isChecked) selectedDays.add(Calendar.MONDAY)
        if (tuesdayCheckBox.isChecked) selectedDays.add(Calendar.TUESDAY)
        if (wednesdayCheckBox.isChecked) selectedDays.add(Calendar.WEDNESDAY)
        if (thursdayCheckBox.isChecked) selectedDays.add(Calendar.THURSDAY)
        if (fridayCheckBox.isChecked) selectedDays.add(Calendar.FRIDAY)
        if (saturdayCheckBox.isChecked) selectedDays.add(Calendar.SATURDAY)
        if (sundayCheckBox.isChecked) selectedDays.add(Calendar.SUNDAY)

        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "Please select at least one day", Toast.LENGTH_SHORT).show()
            return
        }

        val habit = Habit(name, message, selectedDays, selectedHour, selectedMinute)
        val sharedPreferences = getSharedPreferences("HabitPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val habitList = mutableListOf<Habit>()
        val habitsJson = sharedPreferences.getString("habitList", null)
        if (habitsJson != null) {
            val type = object : TypeToken<List<Habit>>() {}.type
            habitList.addAll(gson.fromJson(habitsJson, type) ?: emptyList())
        }
        habitList.add(habit)
        sharedPreferences.edit().putString("habitList", gson.toJson(habitList)).apply()
        Log.d(TAG, "Habit '$name' saved")

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(this, "Please grant exact alarm permission", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            return
        }

        scheduleHabitNotifications(alarmManager, habit)
        Toast.makeText(this, "Habit '$name' created", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun scheduleHabitNotifications(alarmManager: AlarmManager, habit: Habit) {
        for (day in habit.daysOfWeek) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, day)
                set(Calendar.HOUR_OF_DAY, habit.timeOfDayHour)
                set(Calendar.MINUTE, habit.timeOfDayMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis < System.currentTimeMillis()) add(Calendar.WEEK_OF_YEAR, 1)
            }

            val startTime = calendar.timeInMillis
            val endTime = startTime + NOTIFICATION_DURATION_MS
            var currentTime = startTime
            var iteration = 0

            while (currentTime <= endTime) {
                val intent = Intent(this, HabitBroadcastReceiver::class.java).apply {
                    action = "com.example.nfchabittracker.HABIT_NOTIFICATION"
                    putExtra("habitName", habit.name)
                    putExtra("notificationMessage", habit.notificationMessage)
                }
                val requestCode = (habit.name + day.toString() + iteration).hashCode() // Unique per iteration
                val pendingIntent = PendingIntent.getBroadcast(
                    this,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, currentTime, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, currentTime, pendingIntent)
                }
                Log.d(TAG, "Scheduled notification for '${habit.name}' on day $day at ${Calendar.getInstance().apply { timeInMillis = currentTime }.time} (iteration $iteration, requestCode $requestCode)")

                currentTime += NOTIFICATION_INTERVAL_MS
                iteration++
            }
        }
    }
}