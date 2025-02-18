package com.example.nfc_habit_tracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class HabitCreationActivity extends AppCompatActivity {

    private EditText habitNameEditText;
    private CheckBox mondayCheckBox, tuesdayCheckBox, wednesdayCheckBox, thursdayCheckBox, fridayCheckBox, saturdayCheckBox, sundayCheckBox;
    private TimePicker timePicker;
    private Button saveHabitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_creation);

        habitNameEditText = findViewById(R.id.habitNameEditText);
        mondayCheckBox = findViewById(R.id.mondayCheckBox);
        tuesdayCheckBox = findViewById(R.id.tuesdayCheckBox);
        wednesdayCheckBox = findViewById(R.id.wednesdayCheckBox);
        thursdayCheckBox = findViewById(R.id.thursdayCheckBox);
        fridayCheckBox = findViewById(R.id.fridayCheckBox);
        saturdayCheckBox = findViewById(R.id.saturdayCheckBox);
        sundayCheckBox = findViewById(R.id.sundayCheckBox);
        timePicker = findViewById(R.id.timePicker);
        saveHabitButton = findViewById(R.id.saveHabitButton);

        saveHabitButton.setOnClickListener(v -> {
            // Get the habit name
            String habitName = habitNameEditText.getText().toString().trim();

            // Get the selected days
            ArrayList<String> habitDays = new ArrayList<>();
            if (mondayCheckBox.isChecked()) habitDays.add("Monday");
            if (tuesdayCheckBox.isChecked()) habitDays.add("Tuesday");
            if (wednesdayCheckBox.isChecked()) habitDays.add("Wednesday");
            if (thursdayCheckBox.isChecked()) habitDays.add("Thursday");
            if (fridayCheckBox.isChecked()) habitDays.add("Friday");
            if (saturdayCheckBox.isChecked()) habitDays.add("Saturday");
            if (sundayCheckBox.isChecked()) habitDays.add("Sunday");

            // Get the selected time
            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();
            String habitTime = String.format("%02d:%02d", hour, minute);

            // Check if the habit name is empty
            if (habitName.isEmpty()) {
                Toast.makeText(HabitCreationActivity.this, "Please enter a habit name", Toast.LENGTH_SHORT).show();
                return;
            }

            // Send data back to MainActivity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("habitName", habitName);
            resultIntent.putStringArrayListExtra("habitDays", habitDays);
            resultIntent.putExtra("habitTime", habitTime);
            setResult(RESULT_OK, resultIntent);

            // Finish activity and return to MainActivity
            finish();
        });
    }
}
