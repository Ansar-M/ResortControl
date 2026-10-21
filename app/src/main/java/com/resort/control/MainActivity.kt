package com.resort.control

import androidx.compose.ui.res.stringResource
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import com.resort.control.ui.theme.ResortControlTheme
import com.resort.control.auth.AuthManager
import com.resort.control.sync.SyncRepository
import com.resort.control.sync.SyncWorker
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import androidx.work.*
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.TimeUnit
import java.time.LocalDate
import androidx.lifecycle.asFlow

class MainActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager
    // TODO: Replace with your actual Web Client ID from Firebase Console
    private val webClientId = "595010678626-9hn7s3seimgi1gcsjmge1ieoa4ef76i5.apps.googleusercontent.com" 

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
                    var isInitialSyncing by remember { mutableStateOf(false) }

                    val workInfos by WorkManager.getInstance(applicationContext)
                        .getWorkInfosForUniqueWorkLiveData("CloudSync")
                        .asFlow()
                        .collectAsState(initial = emptyList())
                    
                    val syncStatus = remember(workInfos) {
                        val info = workInfos.firstOrNull()
                        when (info?.state) {
                            WorkInfo.State.RUNNING -> "SYNCING"
                            WorkInfo.State.ENQUEUED -> "PENDING"
                            WorkInfo.State.FAILED -> "ERROR"
                            WorkInfo.State.BLOCKED -> "PENDING"
                            else -> "IDLE"
                        }
                    }
                    
                    LaunchedEffect(user) {
                        val currentUser = user
                        android.util.Log.d("MainActivity", "User state changed: ${currentUser?.email}")
                        if (currentUser != null) {
                            isInitialSyncing = true
                            scope.launch {
                                try {
                                    val error = syncRepository.pullFromCloud(currentUser.uid)
                                    if (error != null) {
                                        android.widget.Toast.makeText(applicationContext, "Сбой загрузки: $error", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Initial pull failed", e)
                                    android.widget.Toast.makeText(applicationContext, "Критическая ошибка: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                } finally {
                                    isInitialSyncing = false
                                }
                            }
                        }
                    }

                    var isSigningIn by remember { mutableStateOf(false) }
                    var signInError by remember { mutableStateOf<String?>(null) }

                    if (user == null) {
                        SignInScreen(
                            isSigningIn = isSigningIn,
                            errorMessage = signInError,
                            onSignInClick = {
                                isSigningIn = true
                                signInError = null
                                scope.launch {
                                    try {
                                        authManager.signInWithGoogle(webClientId)
                                    } catch (e: Exception) {
                                        android.util.Log.e("MainActivity", "Sign in failed", e)
                                        signInError = e.localizedMessage ?: "Unknown error"
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
                            centerEntities.map { Center(it.name, it.name == activeCenter || (activeCenter == "" && centerEntities.first().name == it.name)) }
                        }
                        
                        // Set default active center if none selected
                        LaunchedEffect(centerEntities, isInitialSyncing) {
                            if (!isInitialSyncing && activeCenter == "") {
                                if (centerEntities.isNotEmpty()) {
                                    activeCenter = centerEntities.first().name
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

                        if (isInitialSyncing) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color(0xFF1B4332))
                                    Spacer(Modifier.height(16.dp))
                                    Text(stringResource(R.string.syncing_data), color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                        } else if (selectedCabinId == null || selectedCabin == null) {
                            CabinListScreen(
                                cabins = cabins,
                                user = user!!,
                                centers = centers,
                                activeCenter = activeCenter,
                                windowSizeClass = windowSizeClass,
                                syncStatus = syncStatus,
                                onRefresh = {
                                    android.util.Log.d("MainActivity", "Manual refresh triggered")
                                    scheduleSync()
                                },
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
                                onResetAllData = {
                                    scope.launch {
                                        try {
                                            isInitialSyncing = true
                                            val uid = Firebase.auth.currentUser?.uid ?: ""
                                            if (uid.isNotEmpty()) {
                                                syncRepository.clearAllCloudData(uid)
                                            }
                                            withContext(Dispatchers.IO) {
                                                database.clearAllTables()
                                            }
                                            activeCenter = ""
                                            selectedCabinId = null
                                            android.util.Log.d("MainActivity", "All data reset successfully")
                                        } catch (e: Exception) {
                                            android.util.Log.e("MainActivity", "Failed to reset all data", e)
                                        } finally {
                                            isInitialSyncing = false
                                        }
                                    }
                                },
                                onSignOut = {
                                    scope.launch {
                                        try {
                                            android.util.Log.d("MainActivity", "User requested sign out")
                                            authManager.signOut()
                                            
                                            // CRITICAL: Clear all local tables on logout to prevent data leak 
                                            // and ensure clean state for the next user.
                                            withContext(Dispatchers.IO) {
                                                database.clearAllTables()
                                            }
                                            
                                            activeCenter = ""
                                            selectedCabinId = null
                                            android.util.Log.d("MainActivity", "Sign out complete: user cleared and DB wiped")
                                        } catch (e: Exception) {
                                            android.util.Log.e("MainActivity", "Error during sign out process", e)
                                        }
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
                            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(
                                        onClick = { selectedCabinId = null },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color(0xFF1B4332).copy(alpha = 0.08f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = Color(0xFF1B4332),
                                            modifier = Modifier.size(20.dp)
                                        )
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
                                    onUpdateBooking = { updatedBooking ->
                                        scope.launch {
                                            resortDao.insertBooking(
                                                BookingEntity(
                                                    updatedBooking.id, selectedCabin.id, updatedBooking.startDate.toEpochDay(),
                                                    updatedBooking.endDate.toEpochDay(), updatedBooking.note, updatedBooking.color.toArgb(),
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
                                    onDeleteAllBookings = { cabin ->
                                        scope.launch {
                                            resortDao.markBookingsDeletedByCabin(cabin.id, System.currentTimeMillis())
                                            scheduleSync()
                                        }
                                    },
                                    onDeletePhoto = { photoUrl ->
                                        scope.launch {
                                            resortDao.insertPendingDeletion(PendingDeletionEntity(photoUrl))
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
                                    },
                                    onDeleteCabin = { cabinToDelete ->
                                        scope.launch {
                                            resortDao.softDeleteCabin(cabinToDelete.id)
                                            selectedCabinId = null
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
fun SignInScreen(isSigningIn: Boolean, errorMessage: String?, onSignInClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(
                text = "ResortControl",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B4332)
            )
            
            if (errorMessage != null) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = Color.Red.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

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
