package com.example.nfchabittracker

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var newHabitButton: Button
    private lateinit var habitsListView: ListView
    private lateinit var emptyHabitListTextView: TextView
    private lateinit var habitList: MutableList<Habit>
    private lateinit var habitAdapter: HabitItemAdapter // Custom Adapter - lateinit, initialize it!
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private val dayFormat = SimpleDateFormat("EEE", Locale.getDefault()) // Abbreviated day format

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        newHabitButton = findViewById(R.id.newHabitButton)
        habitsListView = findViewById(R.id.habitsListView)
        emptyHabitListTextView = findViewById(R.id.emptyHabitListTextView)

        sharedPreferences = getSharedPreferences("HabitPrefs", Context.MODE_PRIVATE)

        // Initialize habitList *before* setting up adapter
        habitList = mutableListOf() // Initialize habitList here as empty list initially

        setupHabitListView() // Initialize habitAdapter *first*


        newHabitButton.setOnClickListener {
            val intent = Intent(this, CreateHabitActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() { // Changed from onStart() to onResume()
        super.onResume()
        loadHabits() // Reload habits every time the activity resumes and becomes visible
        setupHabitListView()
    }


    private fun loadHabits() {
        val habitsJson = sharedPreferences.getString("habitList", null)
        habitList.clear() // Clear the list before loading new data
        if (habitsJson != null) {
            val type = object : TypeToken<List<Habit>>() {}.type
            val loadedHabits: List<Habit> = gson.fromJson(habitsJson, type) ?: emptyList()
            habitList.addAll(loadedHabits) // Add loaded habits to the list
        }
        updateVisibility()
        habitAdapter.notifyDataSetChanged() // *Now* notify adapter after loading data
    }

    private fun saveHabits() {
        val habitsJson = gson.toJson(habitList)
        sharedPreferences.edit().putString("habitList", habitsJson).apply()
    }

    private fun setupHabitListView() {
        habitAdapter = HabitItemAdapter(this, habitList) // Initialize habitAdapter here!
        habitsListView.adapter = habitAdapter
        habitAdapter.notifyDataSetChanged() // Notify adapter after setting it up initially (optional here)
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

    // Custom ArrayAdapter to display Habit details
    private inner class HabitItemAdapter(context: Context, habits: List<Habit>) :
        ArrayAdapter<Habit>(context, R.layout.habit_item, habits) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.habit_item, parent, false)
            val habit = getItem(position) ?: return view // Safe unwrap

            val nameTextView = view.findViewById<TextView>(R.id.habitNameTextView)
            val daysTextView = view.findViewById<TextView>(R.id.habitDaysTextView)
            val timeTextView = view.findViewById<TextView>(R.id.habitTimeTextView)

            nameTextView.text = habit.name

            // Format days of the week
            val daysStringBuilder = StringBuilder()
            val sortedDays = habit.daysOfWeek.sorted() // Sort days for consistent display
            for (day in sortedDays) {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_WEEK, day)
                daysStringBuilder.append(dayFormat.format(calendar.time)).append(" ") // Abbreviated day name
            }
            daysTextView.text = "Days: ${daysStringBuilder.toString().trim()}"

            // Format time of day
            val formattedTime = String.format("%02d:%02d", habit.timeOfDayHour, habit.timeOfDayMinute)
            timeTextView.text = "Time: $formattedTime"

            return view
        }
    }
}