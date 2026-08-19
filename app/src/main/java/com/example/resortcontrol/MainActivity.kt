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
import com.example.resortcontrol.sync.SyncRepository
import com.example.resortcontrol.sync.SyncWorker
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
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
import java.time.LocalDate

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
                    val syncRepository = remember { SyncRepository(resortDao) }
                    
                    LaunchedEffect(user) {
                        android.util.Log.d("MainActivity", "User state changed: ${user?.email}")
                        user?.let {
                            // Trigger immediate pull from cloud when user is detected
                            scope.launch {
                                syncRepository.pullFromCloud(Firebase.auth.currentUser?.uid ?: "")
                            }
                        }
                    }

                    var isSigningIn by remember { mutableStateOf(false) }

                    if (user == null) {
                        SignInScreen(
                            isSigningIn = isSigningIn,
                            onSignInClick = {
                                isSigningIn = true
                                scope.launch {
                                    try {
                                        authManager.signInWithGoogle(webClientId)
                                    } catch (e: Exception) {
                                        android.util.Log.e("MainActivity", "Sign in failed", e)
                                    } finally {
                                        isSigningIn = false
                                    }
                                }
                            }
                        )
                    } else {
                        // REACTIVE DATA OBSERVATION
                        val cabinEntities by resortDao.getAllCabinsFlow().collectAsState(initial = emptyList())
                        val bookingEntities by resortDao.getAllBookingsFlow().collectAsState(initial = emptyList())
                        val centerEntities by resortDao.getAllCentersFlow().collectAsState(initial = emptyList())
                        
                        var activeCenter by rememberSaveable { mutableStateOf("") }
                        
                        val centers = remember(centerEntities, activeCenter) {
                            if (centerEntities.isEmpty()) {
                                listOf(Center("Recreation Center", true))
                            } else {
                                centerEntities.map { Center(it.name, it.name == activeCenter || (activeCenter == "" && centerEntities.first().name == it.name)) }
                            }
                        }
                        
                        // Set default active center if none selected
                        LaunchedEffect(centerEntities) {
                            if (activeCenter == "") {
                                if (centerEntities.isNotEmpty()) {
                                    activeCenter = centerEntities.first().name
                                } else {
                                    // Create default if no centers exist
                                    resortDao.insertCenter(CenterEntity("Recreation Center"))
                                    activeCenter = "Recreation Center"
                                }
                            }
                        }

                        val cabins = remember(cabinEntities, bookingEntities, activeCenter) {
                            cabinEntities
                                .filter { it.centerName == activeCenter }
                                .map { entity ->
                                    val cabinBookings = bookingEntities
                                        .filter { it.cabinId == entity.id }
                                        .map { b ->
                                            Booking(b.id, LocalDate.ofEpochDay(b.startDate), LocalDate.ofEpochDay(b.endDate), b.note, Color(b.colorArgb))
                                        }
                                    Cabin(
                                        entity.id, entity.number, entity.photos, entity.hasShower, entity.hasToilet,
                                        entity.roomsCount, entity.bedsCount, entity.comment, entity.price, entity.currency
                                    ).apply {
                                        bookings.addAll(cabinBookings)
                                    }
                                }
                        }

                        LaunchedEffect(Unit) {
                            scheduleSync()
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
                                },
                                onAddCenter = { name ->
                                    scope.launch {
                                        resortDao.insertCenter(CenterEntity(name))
                                        scheduleSync()
                                    }
                                },
                                onEditCenter = { center, newName ->
                                    scope.launch {
                                        resortDao.updateCenterName(center.name, newName)
                                        if (activeCenter == center.name) {
                                            activeCenter = newName
                                        }
                                        scheduleSync()
                                    }
                                },
                                onDeleteCenter = { center ->
                                    scope.launch {
                                        resortDao.softDeleteCenter(center.name)
                                        if (activeCenter == center.name) {
                                            activeCenter = ""
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
                                        scheduleSync()
                                    }
                                },
                                onDeleteCabin = { cabin ->
                                    scope.launch {
                                        resortDao.softDeleteCabin(cabin.id)
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
                                                    newBooking.id, selectedCabin.id, newBooking.startDate.toEpochDay(),
                                                    newBooking.endDate.toEpochDay(), newBooking.note, newBooking.color.toArgb(),
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
fun SignInScreen(isSigningIn: Boolean, onSignInClick: () -> Unit) {
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
            
            if (isSigningIn) {
                CircularProgressIndicator(color = Color(0xFF1B4332))
                Spacer(Modifier.height(16.dp))
                Text("Connecting to Google...", color = Color.Gray, fontSize = 14.sp)
            } else {
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
}
