package com.example.nfchabittracker

import java.io.Serializable

data class Habit(
    val name: String,
    val notificationMessage: String,
    val daysOfWeek: Set<Int>,
    val timeOfDayHour: Int,
    val timeOfDayMinute: Int,
    var snoozedUntil: Long = 0L,
    var isSkippedToday: Boolean = false,
    var isSnoozeLoopActive: Boolean = false,
    var lastSnoozeStartTime: Long = 0L, // To track when the last snooze was initiated
    val activeSnoozeRequestCodes: MutableList<Int> = mutableListOf(), // To store request codes of active snooze notifications
    var skipDayActivated: Boolean = false // Flag to immediately stop notifications for the day
) : Serializable