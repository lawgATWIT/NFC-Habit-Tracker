package com.example.nfc_habit_tracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private RecyclerView habitRecyclerView;
    private HabitAdapter habitAdapter;
    private ArrayList<Habit> habitList;
    private Button addNewHabitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        habitRecyclerView = findViewById(R.id.habitRecyclerView);
        addNewHabitButton = findViewById(R.id.addNewHabitButton);

        // Initialize habit list and adapter
        habitList = new ArrayList<>();
        habitAdapter = new HabitAdapter(habitList);

        // Set up RecyclerView with the HabitAdapter
        habitRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        habitRecyclerView.setAdapter(habitAdapter);

        // Button click listener to add a new habit
        addNewHabitButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HabitCreationActivity.class);
            startActivityForResult(intent, 1);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == 1) {
            // Retrieve the habit data from HabitCreationActivity
            String habitName = data.getStringExtra("habitName");
            ArrayList<String> habitDays = data.getStringArrayListExtra("habitDays");
            String habitTime = data.getStringExtra("habitTime");

            // Add the new habit to the list and notify the adapter
            habitList.add(new Habit(habitName, habitDays, habitTime));
            habitAdapter.notifyDataSetChanged(); // Update the RecyclerView
        }
    }
}
