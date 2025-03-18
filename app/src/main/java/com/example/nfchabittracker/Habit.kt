package com.example.nfchabittracker

import java.io.Serializable

data class Habit(
    val name: String,
    val notificationMessage: String,
    val daysOfWeek: Set<Int>,
    val timeOfDayHour: Int,
    val timeOfDayMinute: Int,
    var snoozedUntil: Long? = null
) : Serializable