package com.example.nfc_habit_tracker;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "habits")
public class Habit {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "habit_name")
    public String name;

    @ColumnInfo(name = "days_of_week")
    public String days; // Store days as comma-separated values (e.g., "Mon,Wed,Fri")

    @ColumnInfo(name = "frequency")
    public int frequency; // Frequency per day (1-3)

    @ColumnInfo(name = "times")
    public String times; // Store times as comma-separated values (e.g., "08:00,13:00,19:00")
}
