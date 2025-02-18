package com.example.nfc_habit_tracker;

import java.util.ArrayList;

public class Habit {

    private String name;
    private ArrayList<String> days;
    private String time;

    public Habit(String name, ArrayList<String> days, String time) {
        this.name = name;
        this.days = days;
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public ArrayList<String> getDays() {
        return days;
    }

    public String getTime() {
        return time;
    }
}
