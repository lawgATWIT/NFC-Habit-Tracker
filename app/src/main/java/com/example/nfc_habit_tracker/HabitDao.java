package com.example.nfc_habit_tracker;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HabitDao {

    @Insert
    void insert(Habit habit);

    @Query("SELECT * FROM habits")
    List<Habit> getAllHabits();
}
