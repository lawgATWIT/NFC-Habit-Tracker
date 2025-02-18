package com.example.nfc_habit_tracker;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class TimePickerFragment extends DialogFragment {

    // Variable to hold the listener
    private TimePickerDialog.OnTimeSetListener timeSetListener;

    // Set the time listener
    public void setTimeListener(TimePickerDialog.OnTimeSetListener listener) {
        this.timeSetListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Get the current time for default setting
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // Return the TimePickerDialog with the listener
        return new TimePickerDialog(getActivity(), timeSetListener, hour, minute, true);
    }
}
