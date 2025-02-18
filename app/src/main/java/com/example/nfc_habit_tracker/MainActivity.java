package com.example.nfc_habit_tracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ListView habitListView;
    private Button addHabitButton;
    private ArrayList<String> habitList;
    private ArrayAdapter<String> habitAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        habitListView = findViewById(R.id.habitListView);
        addHabitButton = findViewById(R.id.addHabitButton);

        // Initialize habit list
        habitList = new ArrayList<>();
        habitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, habitList);
        habitListView.setAdapter(habitAdapter);

        // Add habit button listener
        addHabitButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HabitCreationActivity.class);
            startActivityForResult(intent, 1);
        });
    }

    // Handle result from HabitCreationActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            String newHabit = data.getStringExtra("newHabit");
            if (newHabit != null && !newHabit.isEmpty()) {
                habitList.add(newHabit);  // Add new habit to the list
                habitAdapter.notifyDataSetChanged();  // Refresh the ListView
            }
        } else {
            Toast.makeText(this, "Error while adding habit!", Toast.LENGTH_SHORT).show();
        }
    }
}
