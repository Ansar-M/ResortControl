package com.resort.control

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.appcompat.app.AppCompatDelegate
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

fun getDayInfo(currentDate: LocalDate, bookings: List<Booking>): DayInfo {
    val activeBookings = bookings.filter {
        !currentDate.isBefore(it.startDate) && !currentDate.isAfter(it.endDate)
    }
    if (activeBookings.isEmpty()) return DayInfo(DayState.NONE)
    if (activeBookings.size >= 2) {
        val ending = activeBookings.find { it.endDate == currentDate }
        val starting = activeBookings.find { it.startDate == currentDate }
        return DayInfo(DayState.OVERLAP, ending, starting)
    }
    val b = activeBookings.first()
    val state = when (currentDate) {
        b.startDate -> DayState.START
        b.endDate -> DayState.END
        else -> DayState.MIDDLE
    }
    return DayInfo(state, b)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingTimelineVisualizer(
    cabin: Cabin,
    onAddBooking: (Booking) -> Unit = {},
    onUpdateBooking: (Booking) -> Unit = {},
    onDeleteBooking: (Booking) -> Unit = {},
    onDeleteAllBookings: () -> Unit = {}
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var showDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var newBookingNote by remember { mutableStateOf("") }
    var selectedDayInfo by remember { mutableStateOf<DayInfo?>(null) }
    val bookings = cabin.bookings

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.calendar_title), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row {
                if (bookings.isNotEmpty()) {
                    IconButton(onClick = { showClearConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = stringResource(R.string.clear_all_bookings),
                            tint = Color.Red.copy(alpha = 0.6f)
                        )
                    }
                }
                IconButton(onClick = { showDialog = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.add_booking_desc))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) { Text(text = stringResource(R.string.prev_month)) }
            val appLocale = AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault()
            val monthName = currentMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, appLocale)
            Text(
                text = "${monthName.replaceFirstChar { it.uppercase(appLocale) }} ${currentMonth.year}",
                fontSize = 18.sp, fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) { Text(text = stringResource(R.string.next_month)) }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val daysOfWeek = if (AppCompatDelegate.getApplicationLocales().get(0)?.language == "ru") {
            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        } else {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Text(text = day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val daysInMonth = currentMonth.lengthOfMonth()
        val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value
        val emptyCellsCount = firstDayOfWeek - 1

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth().height(340.dp),
            userScrollEnabled = false // ОТКЛЮЧАЕМ ВНУТРЕННЮЮ ПРОКРУТКУ
        ) {
            items(emptyCellsCount) { Box(modifier = Modifier.height(48.dp)) }
            items(daysInMonth) { index ->
                val day = index + 1
                val currentDate = currentMonth.atDay(day)
                val dayInfo = getDayInfo(currentDate, bookings)

                CalendarDay(
                    day = day, dayInfo = dayInfo, onDayClick = { selectedDayInfo = dayInfo },
                    firstStripeColor = dayInfo.firstBooking?.color ?: Color.Transparent,
                    secondStripeColor = dayInfo.secondBooking?.color ?: dayInfo.firstBooking?.color ?: Color.Transparent
                )
            }
        }

        if (showDialog) {
            val dateRangePickerState = rememberDateRangePickerState()
            var errorMessage by remember { mutableStateOf<String?>(null) }
            val appLocale = AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault()
            val configuration = Configuration(LocalConfiguration.current).apply { setLocale(appLocale) }

            CompositionLocalProvider(LocalConfiguration provides configuration) {
                DatePickerDialog(
                    onDismissRequest = { showDialog = false },
                    confirmButton = {
                        val busyError = stringResource(R.string.dates_busy_error)
                        TextButton(
                            onClick = {
                                val startMillis = dateRangePickerState.selectedStartDateMillis
                                val endMillis = dateRangePickerState.selectedEndDateMillis
                                if (startMillis != null && endMillis != null) {
                                    val startDate = Instant.ofEpochMilli(startMillis).atZone(ZoneId.of("UTC")).toLocalDate()
                                    val endDate = Instant.ofEpochMilli(endMillis).atZone(ZoneId.of("UTC")).toLocalDate()

                                    val hasOverlap = bookings.any { startDate.isBefore(it.endDate) && endDate.isAfter(it.startDate) }
                                    if (hasOverlap) {
                                        errorMessage = busyError
                                    } else {
                                        errorMessage = null
                                        val palette = listOf(
                                            Color(0xFF42A5F5), // Blue
                                            Color(0xFFFFA726), // Orange
                                            Color(0xFF66BB6A), // Green
                                            Color(0xFFAB47BC), // Purple
                                            Color(0xFFEC407A), // Pink
                                            Color(0xFF26A69A), // Teal
                                            Color(0xFF5C6BC0), // Indigo
                                            Color(0xFF78909C), // Blue Grey
                                            Color(0xFFFF7043), // Deep Orange
                                            Color(0xFF8D6E63), // Brown
                                            Color(0xFFFFCA28), // Amber
                                            Color(0xFF26C6DA)  // Cyan
                                        )

                                        val usedColors = bookings.map { it.color }.toSet()
                                        val unusedColors = palette.filter { it !in usedColors }

                                        val finalColor = if (unusedColors.isNotEmpty()) {
                                            unusedColors.random()
                                        } else {
                                            // Если все цвета палитры уже использованы, выберем тот, который не встречается
                                            // в ближайших по времени бронированиях (в радиусе 30 дней)
                                            val conflictingColors = bookings.filter {
                                                val distStart = Math.abs(ChronoUnit.DAYS.between(it.endDate, startDate))
                                                val distEnd = Math.abs(ChronoUnit.DAYS.between(it.startDate, endDate))
                                                distStart <= 30 || distEnd <= 30
                                            }.map { it.color }.toSet()

                                            val safeColors = palette.filter { it !in conflictingColors }

                                            if (safeColors.isNotEmpty()) {
                                                safeColors.random()
                                            } else {
                                                // Крайний случай: просто избегаем цветов непосредственно прилегающих броней
                                                val adjacentColors = bookings.filter {
                                                    it.endDate == startDate || it.startDate == endDate
                                                }.map { it.color }.toSet()
                                                val minimalSafe = palette.filter { it !in adjacentColors }
                                                if (minimalSafe.isNotEmpty()) minimalSafe.random() else palette.random()
                                            }
                                        }

                                        val newBooking = Booking(
                                            id = "b_${System.currentTimeMillis()}",
                                            startDate = startDate,
                                            endDate = endDate,
                                            note = newBookingNote,
                                            color = finalColor
                                        )
                                        bookings.add(newBooking)
                                        onAddBooking(newBooking)
                                        newBookingNote = ""
                                        showDialog = false
                                    }
                                }
                            },
                            enabled = dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null
                        ) { Text(stringResource(R.string.add_button)) }
                    },
                    dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.cancel_button)) } }
                ) {
                    Column {
                        OutlinedTextField(
                            value = newBookingNote, 
                            onValueChange = { newBookingNote = it },
                            label = { Text(stringResource(R.string.booking_note_label)) }, 
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                            singleLine = true
                        )
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!, 
                                color = Color.Red, 
                                fontWeight = FontWeight.Bold, 
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                fontSize = 12.sp
                            )
                        }
                        DateRangePicker(
                            state = dateRangePickerState,
                            modifier = Modifier.weight(1f),
                            title = {
                                Text(
                                    text = stringResource(R.string.new_booking_title),
                                    modifier = Modifier.padding(start = 24.dp, top = 8.dp)
                                )
                            },
                            headline = {
                                DateRangePickerDefaults.DateRangePickerHeadline(
                                    selectedStartDateMillis = dateRangePickerState.selectedStartDateMillis,
                                    selectedEndDateMillis = dateRangePickerState.selectedEndDateMillis,
                                    displayMode = dateRangePickerState.displayMode,
                                    dateFormatter = DatePickerDefaults.dateFormatter(),
                                    modifier = Modifier.padding(start = 24.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        if (selectedDayInfo != null) {
            val bookingsToShow = listOfNotNull(selectedDayInfo?.firstBooking, selectedDayInfo?.secondBooking)
            AlertDialog(
                onDismissRequest = { selectedDayInfo = null },
                title = { Text(stringResource(R.string.day_bookings_title)) },
                text = {
                    Column {
                        bookingsToShow.forEach { booking ->
                            var editedNote by remember(booking) { mutableStateOf(booking.note) }
                            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                                Text("${stringResource(R.string.check_in_label)} ${booking.startDate}  |  ${stringResource(R.string.check_out_label)} ${booking.endDate}", fontWeight = FontWeight.SemiBold)
                                OutlinedTextField(
                                    value = editedNote,
                                    onValueChange = { newValue ->
                                        editedNote = newValue
                                        val index = bookings.indexOfFirst { it.id == booking.id }
                                        if (index != -1) {
                                            val updated = bookings[index].copy(note = newValue)
                                            bookings[index] = updated
                                            onUpdateBooking(updated)
                                        }
                                    },
                                    label = { Text(stringResource(R.string.note_label)) }, modifier = Modifier.fillMaxWidth()
                                )
                                TextButton(
                                    onClick = { 
                                        onDeleteBooking(booking)
                                        bookings.removeAll { it.id == booking.id }
                                        selectedDayInfo = null 
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                ) { Text(stringResource(R.string.delete_booking_button), color = Color.Red) }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { selectedDayInfo = null }) { Text(stringResource(R.string.close_button)) } }
            )
        }

        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text(stringResource(R.string.clear_all_bookings)) },
                text = { Text(stringResource(R.string.clear_bookings_confirm)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteAllBookings()
                            bookings.clear()
                            showClearConfirm = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) { Text(stringResource(R.string.delete_button)) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) {
                        Text(stringResource(R.string.cancel_button))
                    }
                }
            )
        }
    }
}

@Composable
fun CalendarDay(
    day: Int, dayInfo: DayInfo, onDayClick: () -> Unit = {},
    firstStripeColor: Color = Color(0xFF42A5F5).copy(alpha = 0.4f),
    secondStripeColor: Color = Color(0xFFFFA726).copy(alpha = 0.4f)
) {
    val state = dayInfo.state
    val backgroundShape = when (state) {
        DayState.START -> RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
        DayState.END -> RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50)
        else -> RectangleShape
    }

    Box(
        modifier = Modifier.height(48.dp).fillMaxWidth().clickable(enabled = state != DayState.NONE) { onDayClick() },
        contentAlignment = Alignment.Center
    ) {
        if (state == DayState.START || state == DayState.MIDDLE || state == DayState.END) {
            Box(modifier = Modifier.fillMaxWidth().height(36.dp).clip(backgroundShape).background(firstStripeColor))
        }
        if (state == DayState.OVERLAP) {
            Row(modifier = Modifier.fillMaxWidth().height(36.dp)) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50)).background(firstStripeColor))
                Spacer(modifier = Modifier.width(2.dp))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)).background(secondStripeColor))
            }
        }
        Text(text = day.toString(), color = Color.Black, fontWeight = FontWeight.Medium)
    }
}