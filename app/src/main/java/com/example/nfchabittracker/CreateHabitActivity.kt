package com.example.nfchabittracker

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log // Import Log
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class CreateHabitActivity : AppCompatActivity() {

    private lateinit var habitNameEditText: EditText
    private lateinit var notificationMessageEditText: EditText
    private lateinit var mondayCheckBox: CheckBox
    private lateinit var tuesdayCheckBox: CheckBox
    private lateinit var wednesdayCheckBox: CheckBox
    private lateinit var thursdayCheckBox: CheckBox
    private lateinit var fridayCheckBox: CheckBox
    private lateinit var saturdayCheckBox: CheckBox
    private lateinit var sundayCheckBox: CheckBox
    private lateinit var hourNumberPicker: NumberPicker
    private lateinit var minuteNumberPicker: NumberPicker
    private lateinit var saveHabitButton: Button

    private var selectedHour = 0
    private var selectedMinute = 0
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String> // for permission request

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_habit)

        sharedPreferences = getSharedPreferences("HabitPrefs", Context.MODE_PRIVATE)

        habitNameEditText = findViewById(R.id.habitNameEditText)
        notificationMessageEditText = findViewById(R.id.notificationMessageEditText)
        mondayCheckBox = findViewById(R.id.mondayCheckBox)
        tuesdayCheckBox = findViewById(R.id.tuesdayCheckBox)
        wednesdayCheckBox = findViewById(R.id.wednesdayCheckBox)
        thursdayCheckBox = findViewById(R.id.thursdayCheckBox)
        fridayCheckBox = findViewById(R.id.fridayCheckBox)
        saturdayCheckBox = findViewById(R.id.saturdayCheckBox)
        sundayCheckBox = findViewById(R.id.sundayCheckBox)
        hourNumberPicker = findViewById(R.id.hourNumberPicker)
        minuteNumberPicker = findViewById(R.id.minuteNumberPicker)
        saveHabitButton = findViewById(R.id.saveHabitButton)

        setupTimeNumberPickers()

        saveHabitButton.setOnClickListener {
            saveHabit()
        }

        // Initialize permission request launcher
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    // Notification permission granted, proceed with scheduling (saveHabit will handle scheduling)
                    Toast.makeText(this, "Notification permission granted.", Toast.LENGTH_SHORT).show()
                } else {
                    // Explain that notifications are needed for the app to function correctly
                    Toast.makeText(
                        this,
                        "Notifications are needed to remind you of your habits.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun setupTimeNumberPickers() {
        hourNumberPicker.minValue = 0
        hourNumberPicker.maxValue = 23
        minuteNumberPicker.minValue = 0
        minuteNumberPicker.maxValue = 59
        minuteNumberPicker.setFormatter { String.format("%02d", it) }

        val calendar = Calendar.getInstance()
        selectedHour = calendar.get(Calendar.HOUR_OF_DAY)
        selectedMinute = calendar.get(Calendar.MINUTE)
        hourNumberPicker.value = selectedHour
        minuteNumberPicker.value = selectedMinute

        hourNumberPicker.setOnValueChangedListener { _, _, newVal -> selectedHour = newVal }
        minuteNumberPicker.setOnValueChangedListener { _, _, newVal -> selectedMinute = newVal }
    }

    private fun saveHabit() {
        val habitName = habitNameEditText.text.toString()
        val notificationMessage = notificationMessageEditText.text.toString()
        if (habitName.isEmpty()) {
            Toast.makeText(this, "Habit name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val daysOfWeek = mutableSetOf<Int>()
        if (mondayCheckBox.isChecked) daysOfWeek.add(Calendar.MONDAY)
        if (tuesdayCheckBox.isChecked) daysOfWeek.add(Calendar.TUESDAY)
        if (wednesdayCheckBox.isChecked) daysOfWeek.add(Calendar.WEDNESDAY)
        if (thursdayCheckBox.isChecked) daysOfWeek.add(Calendar.THURSDAY)
        if (fridayCheckBox.isChecked) daysOfWeek.add(Calendar.FRIDAY)
        if (saturdayCheckBox.isChecked) daysOfWeek.add(Calendar.SATURDAY)
        if (sundayCheckBox.isChecked) daysOfWeek.add(Calendar.SUNDAY)

        if (daysOfWeek.isEmpty()) {
            Toast.makeText(this, "Please select at least one day of the week", Toast.LENGTH_SHORT).show()
            return
        }

        // Check and request notification permission *before* scheduling (for Post Notifications - Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (Tiramisu) and above
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request notification permission
                requestNotificationPermission()
                return // Exit saveHabit - notification will be scheduled after permission is granted (or not, if denied)
            }
        }


        val habit = Habit(habitName, notificationMessage, daysOfWeek, selectedHour, selectedMinute)
        saveHabitToSharedPreferences(habit)
        scheduleNotification(habit)

        Toast.makeText(this, "Habit saved!", Toast.LENGTH_SHORT).show()
        finish()
    }


    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }


    private fun saveHabitToSharedPreferences(habit: Habit) {
        val habitList = loadHabitsFromSharedPreferences().toMutableList()
        habitList.add(habit)
        val habitsJson = gson.toJson(habitList)
        sharedPreferences.edit().putString("habitList", habitsJson).apply()
    }

    private fun loadHabitsFromSharedPreferences(): List<Habit> {
        val habitsJson = sharedPreferences.getString("habitList", null)
        return if (habitsJson != null) {
            val type = object : TypeToken<List<Habit>>() {}.type
            gson.fromJson(habitsJson, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun scheduleNotification(habit: Habit) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Check for SCHEDULE_EXACT_ALARM permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(
                    this,
                    "For precise notifications, please allow 'Schedule exact alarms' in App settings.",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
                return
            } else {
                // Explicitly check SCHEDULE_EXACT_ALARM permission using checkSelfPermission (for Lint)
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
                    Log.w("CreateHabitActivity", "SCHEDULE_EXACT_ALARM permission not explicitly granted, but canScheduleExactAlarms() is true. This is unexpected.")
                    // In theory, we shouldn't reach here if canScheduleExactAlarms() is true, but for extra robustness, maybe handle it.
                    Toast.makeText(this, "Exact alarm scheduling might be restricted.", Toast.LENGTH_SHORT).show()
                    return; // Stop scheduling if explicit check fails (though unlikely)
                }
            }
        }


        // --- Test Notification Scheduling (10 seconds and repeat every 10 seconds for *5 minutes*) ---
        val startTimeMillis = System.currentTimeMillis() + 10000 // 10 seconds from now
        val intervalMillis: Long = 10000 // 10 seconds
        val endTimeMillis = startTimeMillis + (5 * 60 * 1000) // *5 minutes* from start time
        val notificationCount = (5 * 60 * 1000) / 10000 // Number of notifications in 5 minutes

        for (i in 0..notificationCount) {
            val triggerTime = startTimeMillis + (i * intervalMillis)

            val intent = Intent(this, HabitBroadcastReceiver::class.java).apply {
                action = "com.example.nfchabittracker.HABIT_NOTIFICATION"
                putExtra("habitName", habit.name)
                putExtra("notificationMessage", habit.notificationMessage)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                generateTestNotificationId(habit.name, i),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    alarmManager.setExactAndAllowWhileIdle( // Try setting exact alarm
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } catch (e: SecurityException) { // Handle SecurityException just in case (less likely now with permission checks)
                    Log.e("CreateHabitActivity", "SecurityException while scheduling exact alarm", e)
                    Toast.makeText(this, "Alarm scheduling restricted: ${e.message}", Toast.LENGTH_LONG).show();
                    return; // Stop scheduling if exception occurs
                }
            } else {
                try {
                    alarmManager.setExact( // Try setting exact alarm (fallback for older versions)
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } catch (e: SecurityException) { // Handle SecurityException for older versions as well
                    Log.e("CreateHabitActivity", "SecurityException while scheduling exact alarm", e)
                    Toast.makeText(this, "Alarm scheduling restricted: ${e.message}", Toast.LENGTH_LONG).show();
                    return; // Stop scheduling if exception occurs
                }
            }
        }
        // --- End of Test Notification Scheduling ---

        /*  --- Original Weekly Scheduling (commented out for testing) ---
        habit.daysOfWeek.forEach { dayOfWeek ->
            // ... (original weekly scheduling code - no changes here) ...
        }
        --- End of Original Weekly Scheduling ---
        */
    }

    // Modified notification ID for testing
    private fun generateTestNotificationId(habitName: String, notificationIndex: Int): Int {
        return (habitName + "test" + notificationIndex.toString()).hashCode()
    }
}