package com.example.nfc_habit_tracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class HabitAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final List<String> habits;

    public HabitAdapter(Context context, List<String> habits) {
        super(context, 0, habits);
        this.context = context;
        this.habits = habits;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        String habit = habits.get(position);
        TextView textView = convertView.findViewById(android.R.id.text1);
        textView.setText(habit);

        return convertView;
    }
}
