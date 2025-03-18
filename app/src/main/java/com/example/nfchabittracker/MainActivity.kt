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

        nfcAdapter?.let { adapter ->
            val pendingIntent = PendingIntent.getActivity(
                this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE
            )
            val intentFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                addDataType("text/plain")
            }
            val intentFiltersArray = arrayOf(intentFilter)
            adapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, null)
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMessages != null) {
                val messages = rawMessages.map { it as NdefMessage }
                processNdefMessages(messages)
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
        snoozeHabitIfMatching(text)
    }

    private fun snoozeHabitIfMatching(habitName: String) {
        val habit = habitList.find { it.name == habitName }
        if (habit != null) {
            habit.snoozedUntil = System.currentTimeMillis() + 24 * 60 * 60 * 1000
            saveHabits()
            Toast.makeText(this, "Habit '$habitName' snoozed for 24 hours", Toast.LENGTH_SHORT).show()
            habitAdapter.notifyDataSetChanged()
        } else {
            Toast.makeText(this, "No habit found with name '$habitName'", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadHabits() {
        val habitsJson = sharedPreferences.getString("habitList", null)
        habitList.clear()
        if (habitsJson != null) {
            val type = object : TypeToken<List<Habit>>() {}.type
            try {
                val loadedHabits: List<Habit> = gson.fromJson(habitsJson, type) ?: emptyList()
                habitList.addAll(loadedHabits)
            } catch (e: Exception) {
                Log.e(TAG, "Error deserializing habits: ${e.message}", e)
                Toast.makeText(this, "Error loading habits, resetting data", Toast.LENGTH_SHORT).show()
                sharedPreferences.edit().clear().apply()
            }
        }
    }

    private fun saveHabits() {
        try {
            val habitsJson = gson.toJson(habitList)
            sharedPreferences.edit().putString("habitList", habitsJson).apply()
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

    private fun updateVisibility() {
        if (habitList.isEmpty()) {
            habitsListView.visibility = View.GONE
            emptyHabitListTextView.visibility = View.VISIBLE
        } else {
            habitsListView.visibility = View.VISIBLE
            emptyHabitListTextView.visibility = View.GONE
        }
    }

    private inner class HabitItemAdapter(context: Context, habits: List<Habit>) :
        ArrayAdapter<Habit>(context, R.layout.habit_item, habits) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.habit_item, parent, false)
            val habit = getItem(position) ?: return view

            val nameTextView = view.findViewById<TextView>(R.id.habitNameTextView)
            val daysTextView = view.findViewById<TextView>(R.id.habitDaysTextView)
            val timeTextView = view.findViewById<TextView>(R.id.habitTimeTextView)
            val snoozeButton = view.findViewById<Button>(R.id.snoozeButton)
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
            val formattedTime = String.format("%02d:%02d", habit.timeOfDayHour, habit.timeOfDayMinute)
            timeTextView?.text = "Time: $formattedTime"

            snoozeButton?.setOnClickListener {
                habit.snoozedUntil = System.currentTimeMillis() + 24 * 60 * 60 * 1000
                saveHabits()
                Toast.makeText(context, "Notifications for '${habit.name}' snoozed for 24 hours", Toast.LENGTH_SHORT).show()
                notifyDataSetChanged()
            }

            deleteButton?.setOnClickListener {
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                for (i in 0 until 5) {
                    val intent = Intent(context, HabitBroadcastReceiver::class.java).apply {
                        action = "com.example.nfchabittracker.HABIT_NOTIFICATION"
                        putExtra("habitName", habit.name)
                        putExtra("notificationMessage", habit.notificationMessage)
                    }
                    val requestCode = (habit.name + i.toString()).hashCode()
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    alarmManager.cancel(pendingIntent)
                }
                habitList.remove(habit)
                saveHabits()
                notifyDataSetChanged()
                updateVisibility()
            }

            return view
        }
    }
}