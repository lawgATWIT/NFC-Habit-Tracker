package com.example.nfc_habit_tracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {

    private ArrayList<Habit> habitList;

    public HabitAdapter(ArrayList<Habit> habitList) {
        this.habitList = habitList;
    }

    @Override
    public HabitViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate habit item layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.habit_list_item, parent, false);
        return new HabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(HabitViewHolder holder, int position) {
        // Set habit name, days, and time on the item view
        Habit currentHabit = habitList.get(position);
        holder.habitNameTextView.setText(currentHabit.getName());

        // Format days of the week into a string
        StringBuilder daysString = new StringBuilder();
        for (String day : currentHabit.getDays()) {
            daysString.append(day).append(" ");
        }
        holder.habitDaysTextView.setText(daysString.toString().trim());
        holder.habitTimeTextView.setText(currentHabit.getTime());
    }

    @Override
    public int getItemCount() {
        return habitList.size();
    }

    public static class HabitViewHolder extends RecyclerView.ViewHolder {

        public TextView habitNameTextView;
        public TextView habitDaysTextView;
        public TextView habitTimeTextView;

        public HabitViewHolder(View itemView) {
            super(itemView);

            habitNameTextView = itemView.findViewById(R.id.habitNameTextView);
            habitDaysTextView = itemView.findViewById(R.id.habitDaysTextView);
            habitTimeTextView = itemView.findViewById(R.id.habitTimeTextView);
        }
    }
}
