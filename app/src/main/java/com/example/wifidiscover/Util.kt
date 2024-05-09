package com.example.wifidiscover

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getFormattedDateTime(): String {
    val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    return "$currentTime $currentDate"
}