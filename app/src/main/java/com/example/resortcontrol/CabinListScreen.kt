package com.example.resortcontrol

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.time.LocalDate

@Composable
fun CabinListScreen(cabins: List<Cabin>, onCabinClick: (Cabin) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }

    var minRoomsInput by remember { mutableStateOf("") }
    var minBedsInput by remember { mutableStateOf("") }
    var maxPriceInput by remember { mutableStateOf("") }
    var reqShower by remember { mutableStateOf(false) }
    var reqToilet by remember { mutableStateOf(false) }

    val filteredCabins = cabins.filter { cabin ->
        val matchName = "Домик ${cabin.number}".contains(searchQuery, ignoreCase = true) ||
                cabin.number.contains(searchQuery, ignoreCase = true)

        val minRooms = minRoomsInput.toIntOrNull() ?: 0
        val minBeds = minBedsInput.toIntOrNull() ?: 0
        val maxPrice = maxPriceInput.toIntOrNull() ?: Int.MAX_VALUE

        val matchRooms = cabin.roomsCount >= minRooms
        val matchBeds = cabin.bedsCount >= minBeds
        val matchPrice = cabin.price <= maxPrice
        val matchShower = if (reqShower) cabin.hasShower else true
        val matchToilet = if (reqToilet) cabin.hasToilet else true

        matchName && matchRooms && matchBeds && matchPrice && matchShower && matchToilet
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Поиск...") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = { showFilterDialog = true }, modifier = Modifier.padding(start = 8.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "Фильтры", tint = Color.Gray)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredCabins) { cabin ->
                CabinCard(cabin = cabin, onClick = { onCabinClick(cabin) })
            }
        }
    }

    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Фильтры") },
            text = {
                Column {
                    OutlinedTextField(
                        value = maxPriceInput, onValueChange = { maxPriceInput = it },
                        label = { Text("Макс. цена") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = minRoomsInput, onValueChange = { minRoomsInput = it },
                        label = { Text("Мин. комнат") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = minBedsInput, onValueChange = { minBedsInput = it },
                        label = { Text("Мин. мест") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = reqShower, onCheckedChange = { reqShower = it }); Text("Обязательно душ") }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = reqToilet, onCheckedChange = { reqToilet = it }); Text("Обязательно туалет") }
                }
            },
            confirmButton = { TextButton(onClick = { showFilterDialog = false }) { Text("Применить") } },
            dismissButton = {
                TextButton(onClick = { minRoomsInput = ""; minBedsInput = ""; maxPriceInput = ""; reqShower = false; reqToilet = false }) { Text("Сбросить") }
            }
        )
    }
}

@Composable
fun CabinCard(cabin: Cabin, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            Box(modifier = Modifier.size(140.dp).background(Color.LightGray)) {
                if (cabin.photos.isNotEmpty()) {
                    AsyncImage(
                        model = cabin.photos.first(), contentDescription = "Главное фото",
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("Нет фото", color = Color.DarkGray, modifier = Modifier.align(Alignment.Center), fontSize = 12.sp)
                }
            }

            Column(
                modifier = Modifier.padding(12.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Домик ${cabin.number}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${cabin.price} ${cabin.currency}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val today = LocalDate.now()
                    val currentBooking = cabin.bookings.find { !today.isBefore(it.startDate) && !today.isAfter(it.endDate) }
                    val nextBooking = cabin.bookings.filter { it.startDate.isAfter(today) }.minByOrNull { it.startDate }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentBooking != null) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.Red)); Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Занят до ${currentBooking.endDate}", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        } else if (nextBooking != null) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF388E3C))); Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Свободен до ${nextBooking.startDate}", color = Color(0xFF388E3C), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF388E3C))); Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Свободен", color = Color(0xFF388E3C), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (cabin.comment.isNotEmpty()) {
                    Text(text = "📝 ${cabin.comment}", color = Color.DarkGray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FeatureChip(text = "🚪 ${cabin.roomsCount}")
                    FeatureChip(text = "🛏 ${cabin.bedsCount}")
                    if (cabin.hasShower) FeatureChip(text = "🚿")
                    if (cabin.hasToilet) FeatureChip(text = "🚽")
                }
            }
        }
    }
}