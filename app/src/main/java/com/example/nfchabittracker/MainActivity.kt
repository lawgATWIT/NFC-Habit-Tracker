package com.example.nfchabittracker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.os.Build // Added import
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var newHabitButton: Button
    private lateinit var habitsListView: ListView
    private lateinit var emptyHabitListTextView: TextView
    private lateinit var habitList: MutableList<Habit>
    private lateinit var habitAdapter: HabitItemAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private val TAG = "MainActivity"
    private val alarmManager by lazy { getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        newHabitButton = findViewById(R.id.newHabitButton)
        habitsListView = findViewById(R.id.habitsListView)
        emptyHabitListTextView = findViewById(R.id.emptyHabitListTextView)

        sharedPreferences = getSharedPreferences("HabitPrefs", Context.MODE_PRIVATE)
        habitList = mutableListOf()
        loadHabits()
        setupHabitListView()

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device", Toast.LENGTH_LONG).show()
        } else if (!nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "NFC is disabled. Please enable it in settings.", Toast.LENGTH_LONG).show()
        }

        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMessages != null) {
                val messages = rawMessages.map { it as NdefMessage }
                processNdefMessages(messages)
            }
        }

        newHabitButton.setOnClickListener {
            val intent = Intent(this, CreateHabitActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadHabits()
        habitAdapter.notifyDataSetChanged()
        updateVisibility()
        enableForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        disableForegroundDispatch()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action || 
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) { // Added fallback for ACTION_TAG_DISCOVERED
            val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMessages != null) {
                val messages = rawMessages.map { it as NdefMessage }
                processNdefMessages(messages)
            } else {
                Toast.makeText(this, "NFC tag detected but no NDEF messages found.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processNdefMessages(messages: List<NdefMessage>) {
        for (message in messages) {
            for (record in message.records) {
                try {
                    if (record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT)) {
                        val payload = record.payload
                        val textEncoding = if ((payload[0].toInt() and 128) == 0) "UTF-8" else "UTF-16"
                        val languageCodeLength = payload[0].toInt() and 63
                        val text = String(
                            payload,
                            languageCodeLength + 1,
                            payload.size - languageCodeLength - 1,
                            charset(textEncoding)
                        )
                        handleNfcTagText(text)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing NDEF record: ${e.message}", e)
                    Toast.makeText(this, "Error reading NFC tag", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleNfcTagText(text: String) {
        Toast.makeText(this, "NFC Tag: $text", Toast.LENGTH_SHORT).show()
        snoozeHabitIfMatching(text) // Assuming NFC snoozes for 24 hours like before
    }

    private fun snoozeHabitIfMatching(habitName: String) {
        val habit = habitList.find { it.name == habitName }
        if (habit != null) {
            habit.snoozedUntil = System.currentTimeMillis() + 24 * 60 * 60 * 1000
            saveHabits()
            Toast.makeText(this, "'$habitName' postponed until next scheduled habit time.", Toast.LENGTH_SHORT).show()
            habitAdapter.notifyDataSetChanged()
        } else {
            Toast.makeText(this, "No habit found with name '$habitName'", Toast.LENGTH_SHORT).show()
        }
    }

    fun loadHabits() {
        val habitsJson = sharedPreferences.getString("habitList", null)
        habitList.clear()
        if (habitsJson != null) {
            val type = object : TypeToken<List<Habit>>() {}.type
            try {
                val loadedHabits: List<Habit> = gson.fromJson(habitsJson, type) ?: emptyList()
                val now = Calendar.getInstance()
                for (habit in loadedHabits) {
                    // Reset isSkippedToday and skipDayActivated if the day has changed
                    val lastSkipDay = sharedPreferences.getLong("skipDay_${habit.name}", 0L)
                    val calendarLastSkip = Calendar.getInstance().apply { timeInMillis = lastSkipDay }
                    if (calendarLastSkip.get(Calendar.DAY_OF_YEAR) != now.get(Calendar.DAY_OF_YEAR) ||
                        calendarLastSkip.get(Calendar.YEAR) != now.get(Calendar.YEAR)) {
                        habit.isSkippedToday = false
                        habit.skipDayActivated = false // Reset the flag for the new day
                    }
                    habitList.add(habit)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deserializing habits: ${e.message}", e)
                Toast.makeText(this, "Error loading habits, resetting data", Toast.LENGTH_SHORT).show()
                sharedPreferences.edit().clear().apply()
            }
        }
        // Reschedule notifications on load, considering skipped days
        rescheduleAllHabitNotifications()
    }

    fun saveHabits() {
        try {
            val habitsJson = gson.toJson(habitList)
            sharedPreferences.edit().putString("habitList", habitsJson).apply()
            habitList.forEach { habit ->
                if (habit.isSkippedToday) {
                    sharedPreferences.edit().putLong("skipDay_${habit.name}", System.currentTimeMillis()).apply()
                } else {
                    sharedPreferences.edit().remove("skipDay_${habit.name}").apply()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving habits: ${e.message}", e)
            Toast.makeText(this, "Error saving habits", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupHabitListView() {
        habitAdapter = HabitItemAdapter(this, habitList)
        habitsListView.adapter = habitAdapter
        updateVisibility()
    }

    fun updateVisibility() {
        if (habitList.isEmpty()) {
            habitsListView.visibility = View.GONE
            emptyHabitListTextView.visibility = View.VISIBLE
        } else {
            habitsListView.visibility = View.VISIBLE
            emptyHabitListTextView.visibility = View.GONE
        }
    }

    private fun enableForegroundDispatch() {
        nfcAdapter?.let { adapter ->
            val pendingIntent = PendingIntent.getActivity(
                this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val intentFiltersArray = arrayOf(
                IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                    addDataType("text/plain")
                },
                IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED) // Added fallback filter
            )
            adapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, null)
        }
    }

    private fun disableForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(this)
    }

    private fun rescheduleAllHabitNotifications() {
        for (habit in habitList) {
            if (!habit.isSkippedToday) {
                scheduleHabitNotifications(habit)
            }
        }
    }

    private fun scheduleHabitNotifications(habit: Habit) {
        for (day in habit.daysOfWeek) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, day)
                set(Calendar.HOUR_OF_DAY, habit.timeOfDayHour)
                set(Calendar.MINUTE, habit.timeOfDayMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis < System.currentTimeMillis()) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            val intent = Intent(this, HabitBroadcastReceiver::class.java).apply {
                action = "com.example.nfchabittracker.HABIT_NOTIFICATION"
                putExtra("habitName", habit.name)
                putExtra("notificationMessage", habit.notificationMessage)
            }
            val requestCode = (habit.name + day.toString()).hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } else {
                    Log.w(TAG, "Cannot schedule exact alarm for '${habit.name}'. Exact alarm permission not granted.")
                    // Optionally, you can inform the user or use a less precise alarm
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent) // Fallback
                }
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
            Log.d(TAG, "Scheduled notification for '${habit.name}' on day $day at ${calendar.time}")
        }
    }
}