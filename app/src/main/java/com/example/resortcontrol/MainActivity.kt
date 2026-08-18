package com.example.resortcontrol

import androidx.compose.ui.res.stringResource
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.resortcontrol.ui.theme.ResortControlTheme
import com.example.resortcontrol.auth.AuthManager
import com.example.resortcontrol.sync.SyncWorker
import androidx.work.*
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager
    private val webClientId = "595010678626-9hn7s3seimgi1gcsjmge1ieoa4ef76i5.apps.googleusercontent.com" // Replace with real ID

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        val resortDao = database.resortDao()
        authManager = AuthManager(this)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            ResortControlTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val scope = rememberCoroutineScope()
                    val user by authManager.currentUser.collectAsState()
                    val cabins = remember { mutableStateListOf<Cabin>() }
                    val centers = remember { mutableStateListOf<Center>() }
                    var activeCenter by rememberSaveable { mutableStateOf("Recreation Center") }

                    if (user == null) {
                        SignInScreen(onSignInClick = {
                            scope.launch {
                                try {
                                    authManager.signInWithGoogle(webClientId)
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Sign in failed", e)
                                }
                            }
                        })
                    } else {
                        LaunchedEffect(Unit) {
                            val centerEntities = resortDao.getAllCenters()
                            centers.clear()
                            if (centerEntities.isEmpty()) {
                                resortDao.insertCenter(CenterEntity("Recreation Center"))
                                centers.add(Center("Recreation Center", true))
                            } else {
                                centers.addAll(centerEntities.map { Center(it.name, it.name == activeCenter) })
                            }
                            loadCabins(resortDao, activeCenter, cabins)
                            scheduleSync()
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
                                user = user!!,
                                centers = centers,
                                activeCenter = activeCenter,
                                windowSizeClass = windowSizeClass,
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
                                        scheduleSync()
                                    }
                                },
                                onEditCenter = { center, newName ->
                                    scope.launch {
                                        resortDao.updateCenterName(center.name, newName)
                                        // Update local state
                                        val index = centers.indexOfFirst { it.name == center.name }
                                        if (index != -1) {
                                            centers[index] = centers[index].copy(name = newName)
                                        }
                                        if (activeCenter == center.name) {
                                            activeCenter = newName
                                        }
                                        scheduleSync()
                                    }
                                },
                                onSignOut = {
                                    scope.launch {
                                        authManager.signOut()
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
                                                newCabin.bedsCount, newCabin.comment, newCabin.price, newCabin.currency,
                                                isSynced = false
                                            )
                                        )
                                        cabins.add(newCabin)
                                        scheduleSync()
                                    }
                                },
                                onDeleteCabin = { cabin ->
                                    scope.launch {
                                        resortDao.softDeleteCabin(cabin.id)
                                        cabins.removeAll { it.id == cabin.id }
                                        scheduleSync()
                                    }
                                },
                                onDeleteCenter = { center ->
                                    scope.launch {
                                        resortDao.softDeleteCenter(center.name)
                                        centers.removeAll { it.name == center.name }
                                        if (activeCenter == center.name) {
                                            activeCenter = centers.firstOrNull()?.name ?: ""
                                        }
                                        scheduleSync()
                                    }
                                }
                            )
                        } else {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextButton(onClick = { selectedCabinId = null }) {
                                        Text("< ${stringResource(R.string.cancel_button)}", fontSize = 16.sp)
                                    }
                                    
                                    // Local Sync Status Indicator
                                    val isSynced = selectedCabin.bookings.all { true } // Dummy check for now
                                    Icon(
                                        Icons.Default.CloudSync, 
                                        contentDescription = "Sync",
                                        tint = if (isSynced) Color(0xFF4CAF50) else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                CabinDetailScreen(
                                    cabin = selectedCabin,
                                    windowSizeClass = windowSizeClass,
                                    onAddBooking = { newBooking ->
                                        scope.launch {
                                            resortDao.insertBooking(
                                                BookingEntity(
                                                    newBooking.id, selectedCabin.id, newBooking.startDate,
                                                    newBooking.endDate, newBooking.note, newBooking.color.toArgb(),
                                                    isSynced = false
                                                )
                                            )
                                            scheduleSync()
                                        }
                                    },
                                    onDeleteBooking = { booking ->
                                        scope.launch {
                                            resortDao.softDeleteBooking(booking.id)
                                            // Handle cloud deletion if needed
                                            scheduleSync()
                                        }
                                    },
                                    onUpdateCabin = { updatedCabin ->
                                        scope.launch {
                                            resortDao.insertCabin(
                                                CabinEntity(
                                                    updatedCabin.id, updatedCabin.number, activeCenter, updatedCabin.photos,
                                                    updatedCabin.hasShower, updatedCabin.hasToilet, updatedCabin.roomsCount,
                                                    updatedCabin.bedsCount, updatedCabin.comment, updatedCabin.price, updatedCabin.currency,
                                                    isSynced = false
                                                )
                                            )
                                            val index = cabins.indexOfFirst { it.id == updatedCabin.id }
                                            if (index != -1) {
                                                cabins[index] = updatedCabin
                                            }
                                            scheduleSync()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "CloudSync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}

@Composable
fun SignInScreen(onSignInClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ResortControl",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B4332)
            )
            Spacer(Modifier.height(8.dp))
            Text(text = "Property management made easy", color = Color.Gray)
            Spacer(Modifier.height(48.dp))
            Button(
                onClick = onSignInClick,
                modifier = Modifier.fillMaxWidth(0.7f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B4332))
            ) {
                Text(stringResource(R.string.sign_in_google))
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
