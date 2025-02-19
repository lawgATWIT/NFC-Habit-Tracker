package com.example.nfchabittracker

import android.app.*
import android.content.*
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
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
    private lateinit var hourNumberPicker: NumberPicker // NumberPickers for time
    private lateinit var minuteNumberPicker: NumberPicker // NumberPickers for time
    private lateinit var saveHabitButton: Button

    private var selectedHour = 0
    private var selectedMinute = 0
    private lateinit var sharedPreferences: SharedPreferences // lateinit property
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_habit)

        // Initialize sharedPreferences here in onCreate!
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
        hourNumberPicker = findViewById(R.id.hourNumberPicker) // Initialize NumberPickers
        minuteNumberPicker = findViewById(R.id.minuteNumberPicker) // Initialize NumberPickers
        saveHabitButton = findViewById(R.id.saveHabitButton)

        setupTimeNumberPickers() // Setup NumberPickers

        saveHabitButton.setOnClickListener {
            saveHabit()
        }
    }

    private fun setupTimeNumberPickers() {
        hourNumberPicker.minValue = 0
        hourNumberPicker.maxValue = 23
        minuteNumberPicker.minValue = 0
        minuteNumberPicker.maxValue = 59
        minuteNumberPicker.setFormatter { String.format("%02d", it) } // Format minutes to always show 2 digits

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


        val habit = Habit(habitName, notificationMessage, daysOfWeek, selectedHour, selectedMinute)
        saveHabitToSharedPreferences(habit)
        scheduleNotification(habit)

        Toast.makeText(this, "Habit saved!", Toast.LENGTH_SHORT).show()
        finish() // Go back to MainActivity
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

        habit.daysOfWeek.forEach { dayOfWeek ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.DAY_OF_WEEK, dayOfWeek)
                set(Calendar.HOUR_OF_DAY, habit.timeOfDayHour)
                set(Calendar.MINUTE, habit.timeOfDayMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // If the time is in the past, set it to next week
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 7)
                }
            }

            val intent = Intent(this, HabitBroadcastReceiver::class.java).apply {
                action = "com.example.nfchabittracker.HABIT_NOTIFICATION"
                putExtra("habitName", habit.name)
                putExtra("notificationMessage", habit.notificationMessage)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                generateNotificationId(habit.name, dayOfWeek), // Unique ID for each habit and day
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )


            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7, // Repeat weekly
                pendingIntent
            )
        }
    }

    private fun generateNotificationId(habitName: String, dayOfWeek: Int): Int {
        return (habitName + dayOfWeek.toString()).hashCode()
    }
}