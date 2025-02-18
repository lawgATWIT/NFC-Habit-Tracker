package com.example.nfc_habit_tracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class HabitCreationActivity extends AppCompatActivity {

    private EditText habitNameEditText;
    private Button saveHabitButton;
    private LinearLayout daysLayout;
    private LinearLayout timeInputLayout;
    private String[] daysOfWeek = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private boolean[] selectedDays = new boolean[7];
    private String selectedTime = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_creation);

        habitNameEditText = findViewById(R.id.habitNameEditText);
        saveHabitButton = findViewById(R.id.saveHabitButton);
        daysLayout = findViewById(R.id.daysLayout);
        timeInputLayout = findViewById(R.id.timeInputsLayout);

        // Dynamically add checkboxes for each day of the week
        for (int i = 0; i < daysOfWeek.length; i++) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(daysOfWeek[i]);
            final int index = i;  // Capture the index for the listener
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> selectedDays[index] = isChecked);
            daysLayout.addView(checkBox);
        }

        // Use TimePickerDialog to set the time for the habit
        Button setTimeButton = new Button(this);
        setTimeButton.setText("Set Habit Time");
        setTimeButton.setOnClickListener(v -> showTimePickerDialog());
        timeInputLayout.addView(setTimeButton);

        // Save habit button listener
        saveHabitButton.setOnClickListener(v -> {
            String habitName = habitNameEditText.getText().toString().trim();

            if (habitName.isEmpty()) {
                Toast.makeText(this, "Please enter a habit name", Toast.LENGTH_SHORT).show();
                return;
            }

            StringBuilder selectedDaysStr = new StringBuilder();
            for (int i = 0; i < selectedDays.length; i++) {
                if (selectedDays[i]) {
                    if (selectedDaysStr.length() > 0) selectedDaysStr.append(", ");
                    selectedDaysStr.append(daysOfWeek[i]);
                }
            }

            if (selectedDaysStr.length() == 0) {
                Toast.makeText(this, "Please select at least one day", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedTime.isEmpty()) {
                Toast.makeText(this, "Please set a time for your habit", Toast.LENGTH_SHORT).show();
                return;
            }

            // Return the habit details back to the MainActivity
            String newHabit = habitName + " (" + selectedDaysStr.toString() + ") - Time: " + selectedTime;
            Intent resultIntent = new Intent();
            resultIntent.putExtra("newHabit", newHabit);
            setResult(RESULT_OK, resultIntent);
            finish();  // Close the activity and return to MainActivity
        });
    }

    private void showTimePickerDialog() {
        // Open a TimePickerDialog to select the time for the habit
        TimePickerFragment timePicker = new TimePickerFragment();
        timePicker.setTimeListener((view, hourOfDay, minute) -> {
            selectedTime = String.format("%02d:%02d", hourOfDay, minute); // Format the time
            Toast.makeText(HabitCreationActivity.this, "Time Set: " + selectedTime, Toast.LENGTH_SHORT).show();
        });
        timePicker.show(getSupportFragmentManager(), "timePicker");
    }
}
