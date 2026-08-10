package com.example.resortcontrol

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import java.time.LocalDate

data class Cabin(
    val id: String,
    val number: String,
    val photos: List<String>,
    val hasShower: Boolean,
    val hasToilet: Boolean,
    val roomsCount: Int,
    val bedsCount: Int,
    val comment: String = "",
    val price: Int = 0,
    val currency: String = "₸", // НОВОЕ ПОЛЕ
    val bookings: SnapshotStateList<Booking> = mutableStateListOf()
)

data class Booking(
    val id: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val note: String = "",
    val color: Color = Color(0xFF42A5F5)
)

enum class DayState { NONE, START, MIDDLE, END, OVERLAP }

data class DayInfo(
    val state: DayState,
    val firstBooking: Booking? = null,
    val secondBooking: Booking? = null
)