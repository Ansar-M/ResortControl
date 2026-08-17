package com.example.resortcontrol

import androidx.compose.ui.res.stringResource
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.resortcontrol.ui.theme.ResortControlTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        val resortDao = database.resortDao()

        setContent {
            ResortControlTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val scope = rememberCoroutineScope()
                    val cabins = remember { mutableStateListOf<Cabin>() }
                    val centers = remember { mutableStateListOf<Center>() }
                    var activeCenter by rememberSaveable { mutableStateOf("Recreation Center") }
                    val currentUser = remember { 
                        UserAccount("Admin User", "admin@resortcontrol.com", null) 
                    }

                    LaunchedEffect(Unit) {
                        val centerEntities = resortDao.getAllCenters()
                        if (centerEntities.isEmpty()) {
                            resortDao.insertCenter(CenterEntity("Recreation Center"))
                            centers.add(Center("Recreation Center", true))
                        } else {
                            centers.addAll(centerEntities.map { Center(it.name, it.name == activeCenter) })
                        }
                        
                        loadCabins(resortDao, activeCenter, cabins)
                    }

                    // Reload cabins when active center changes
                    LaunchedEffect(activeCenter) {
                        loadCabins(resortDao, activeCenter, cabins)
                    }

                    var selectedCabinId by rememberSaveable { mutableStateOf<String?>(null) }
                    val selectedCabin = cabins.find { it.id == selectedCabinId }

                    if (selectedCabinId == null || selectedCabin == null) {
                        CabinListScreen(
                            cabins = cabins,
                            user = currentUser,
                            centers = centers,
                            activeCenter = activeCenter,
                            onCenterSelect = { center ->
                                activeCenter = center.name
                                centers.indices.forEach { i ->
                                    centers[i] = centers[i].copy(isActive = centers[i].name == center.name)
                                }
                            },
                            onAddCenter = { name ->
                                scope.launch {
                                    resortDao.insertCenter(CenterEntity(name))
                                    centers.add(Center(name, false))
                                }
                            },
                            onCabinClick = { clickedCabin ->
                                selectedCabinId = clickedCabin.id
                            },
                            onAddCabin = { newCabin ->
                                scope.launch {
                                    resortDao.insertCabin(
                                        CabinEntity(
                                            newCabin.id, newCabin.number, activeCenter, newCabin.photos,
                                            newCabin.hasShower, newCabin.hasToilet, newCabin.roomsCount,
                                            newCabin.bedsCount, newCabin.comment, newCabin.price, newCabin.currency
                                        )
                                    )
                                    cabins.add(newCabin)
                                }
                            }
                        )
                    } else {
                        Column {
                            TextButton(
                                onClick = { selectedCabinId = null },
                                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                            ) {
                                Text("< ${stringResource(R.string.cancel_button)}", fontSize = 16.sp)
                            }

                            CabinDetailScreen(
                                cabin = selectedCabin,
                                onAddBooking = { newBooking ->
                                    scope.launch {
                                        resortDao.insertBooking(
                                            BookingEntity(
                                                newBooking.id, selectedCabin.id, newBooking.startDate,
                                                newBooking.endDate, newBooking.note, newBooking.color.toArgb()
                                            )
                                        )
                                    }
                                },
                                onDeleteBooking = { booking ->
                                    scope.launch {
                                        resortDao.deleteBooking(booking.id)
                                    }
                                },
                                onUpdateCabin = { updatedCabin ->
                                    scope.launch {
                                        resortDao.insertCabin(
                                            CabinEntity(
                                                updatedCabin.id, updatedCabin.number, activeCenter, updatedCabin.photos,
                                                updatedCabin.hasShower, updatedCabin.hasToilet, updatedCabin.roomsCount,
                                                updatedCabin.bedsCount, updatedCabin.comment, updatedCabin.price, updatedCabin.currency
                                            )
                                        )
                                        // Update the list to reflect changes in the UI
                                        val index = cabins.indexOfFirst { it.id == updatedCabin.id }
                                        if (index != -1) {
                                            cabins[index] = updatedCabin
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadCabins(resortDao: ResortDao, centerName: String, cabinsList: MutableList<Cabin>) {
        val cabinEntities = resortDao.getAllCabins().filter { it.centerName == centerName }
        val bookingEntities = resortDao.getAllBookings()

        val mappedCabins = cabinEntities.map { entity ->
            val cabinBookings = bookingEntities
                .filter { it.cabinId == entity.id }
                .map { b ->
                    Booking(b.id, b.startDate, b.endDate, b.note, Color(b.colorArgb))
                }
            Cabin(
                entity.id, entity.number, entity.photos, entity.hasShower, entity.hasToilet,
                entity.roomsCount, entity.bedsCount, entity.comment, entity.price, entity.currency
            ).apply {
                bookings.addAll(cabinBookings)
            }
        }
        cabinsList.clear()
        cabinsList.addAll(mappedCabins)
    }
}
