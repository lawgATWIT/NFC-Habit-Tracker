package com.example.nfc_habit_tracker;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class HabitDatabase {

    private static final String PREFS_NAME = "habit_prefs";
    private static final String HABIT_KEY = "habit_list";
    private SharedPreferences sharedPreferences;

    public HabitDatabase(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Add a new habit to the list
    public void addHabit(String habitName) {
        List<String> habitList = loadHabits();
        habitList.add(habitName);
        saveHabits(habitList);
    }

    // Get all habits
    public List<String> loadHabits() {
        String habitsString = sharedPreferences.getString(HABIT_KEY, "");
        List<String> habits = new ArrayList<>();

        if (!habitsString.isEmpty()) {
            String[] habitArray = habitsString.split(",");
            for (String habit : habitArray) {
                habits.add(habit);
            }
        }

        return habits;
    }

    // Save the updated habit list
    private void saveHabits(List<String> habitList) {
        StringBuilder habitsString = new StringBuilder();
        for (String habit : habitList) {
            habitsString.append(habit).append(",");
        }
        sharedPreferences.edit().putString(HABIT_KEY, habitsString.toString()).apply();
    }
}
