package com.example.nfc_habit_tracker;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class HabitListActivity extends AppCompatActivity {

    private ListView habitListView;
    private Button addHabitButton;
    private ArrayAdapter<String> habitAdapter;
    private HabitDatabase habitDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_list);

        habitListView = findViewById(R.id.habitListView);
        addHabitButton = findViewById(R.id.addHabitButton);

        // Initialize HabitDatabase
        habitDatabase = new HabitDatabase(this);

        // Load the list of habits
        List<String> habitList = habitDatabase.loadHabits();

        // Set up the adapter to display the habits
        habitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, habitList);
        habitListView.setAdapter(habitAdapter);

        // Set up the Add Habit button
        addHabitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // Open the HabitCreationActivity to add a new habit
                    Intent intent = new Intent(HabitListActivity.this, HabitCreationActivity.class);
                    startActivityForResult(intent, 1);
                } catch (Exception e) {
                    // Log the error and show a toast message
                    e.printStackTrace();
                    Toast.makeText(HabitListActivity.this, "Error opening HabitCreationActivity", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            String newHabit = data.getStringExtra("newHabit");
            if (newHabit != null && !newHabit.isEmpty()) {
                // Add the new habit to the database
                habitDatabase.addHabit(newHabit);

                // Refresh the list view
                List<String> updatedHabitList = habitDatabase.loadHabits();
                habitAdapter.clear();
                habitAdapter.addAll(updatedHabitList);
                habitAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(HabitListActivity.this, "No habit name entered", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
