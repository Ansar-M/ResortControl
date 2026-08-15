package com.example.resortcontrol

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import java.time.ZoneId
import java.time.Instant
import java.util.Locale
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.core.os.LocaleListCompat
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.filled.DateRange
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.tooling.preview.Preview
import com.example.resortcontrol.ui.theme.ResortControlTheme
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CabinListScreen(
    cabins: List<Cabin>,
    onCabinClick: (Cabin) -> Unit,
    onAddCabin: (Cabin) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    var minRoomsInput by rememberSaveable { mutableStateOf("") }
    var minBedsInput by rememberSaveable { mutableStateOf("") }
    var maxPriceInput by rememberSaveable { mutableStateOf("") }
    var reqShower by rememberSaveable { mutableStateOf(false) }
    var reqToilet by rememberSaveable { mutableStateOf(false) }

    var filterStartEpoch by rememberSaveable { mutableStateOf<Long?>(null) }
    var filterEndEpoch by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val filteredCabins = cabins.filter { cabin ->
        val matchName = stringResource(R.string.cabin_prefix).let { prefix -> "$prefix ${cabin.number}" }.contains(searchQuery, ignoreCase = true) ||
                cabin.number.contains(searchQuery, ignoreCase = true)

        val minRooms = minRoomsInput.toIntOrNull() ?: 0
        val minBeds = minBedsInput.toIntOrNull() ?: 0
        val maxPrice = maxPriceInput.toIntOrNull() ?: Int.MAX_VALUE

        val matchRooms = cabin.roomsCount >= minRooms
        val matchBeds = cabin.bedsCount >= minBeds
        val matchPrice = cabin.price <= maxPrice
        val matchShower = if (reqShower) cabin.hasShower else true
        val matchToilet = if (reqToilet) cabin.hasToilet else true

        val matchDates = if (filterStartEpoch != null && filterEndEpoch != null) {
            val startDate = Instant.ofEpochMilli(filterStartEpoch!!).atZone(ZoneId.of("UTC")).toLocalDate()
            val endDate = Instant.ofEpochMilli(filterEndEpoch!!).atZone(ZoneId.of("UTC")).toLocalDate()
            
            cabin.bookings.none { booking ->
                startDate.isBefore(booking.endDate) && endDate.isAfter(booking.startDate)
            }
        } else {
            true
        }

        matchName && matchRooms && matchBeds && matchPrice && matchShower && matchToilet && matchDates
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.header_title),
                    modifier = Modifier.padding(16.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B4332)
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Color(0xFFF1F1F1))
                Spacer(Modifier.height(16.dp))
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_properties)) },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_bookings)) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_settings)) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    ) {
        Scaffold(
            containerColor = Color(0xFFF8F9FA),
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { scope.launch { drawerState.open() } },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.content_desc_menu), tint = Color(0xFF1B4332))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(0.5f, fill = false)) {
                        Text(
                            text = stringResource(R.string.header_title),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B4332),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.header_subtitle),
                            fontSize = 9.sp,
                            color = Color(0xFF9E9E9E),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(40.dp)
                            .background(Color(0xFFF1F0EA), RoundedCornerShape(8.dp)),
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFF1E3932)),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = Color.Black),
                        decorationBox = { innerTextField ->
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            stringResource(R.string.search_placeholder),
                                            color = Color.Gray,
                                            fontSize = 13.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = { showFilterDialog = true },
                        modifier = Modifier.height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF333333),
                            containerColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.filters_button), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.filters_button), fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1B4332),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.add_property), fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = stringResource(R.string.property_count, filteredCabins.size, cabins.size),
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    val applicationLocales = AppCompatDelegate.getApplicationLocales()
                    val currentLocale = if (!applicationLocales.isEmpty) {
                        applicationLocales.get(0)?.language ?: "en"
                    } else {
                        "en"
                    }

                    Button(
                        onClick = {
                            val nextLocale = if (currentLocale == "en") "ru" else "en"
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(nextLocale))
                        },
                        modifier = Modifier.height(36.dp).width(72.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1B4332),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (currentLocale == "en") "EN / RU" else "RU / EN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    items(filteredCabins) { cabin ->
                        CabinGridCard(cabin = cabin, onClick = { onCabinClick(cabin) })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var newName by remember { mutableStateOf("") }
        var newPrice by remember { mutableStateOf("") }
        var newCurrency by remember { mutableStateOf("₸") }
        var expandedCurrency by remember { mutableStateOf(false) }
        var newRooms by remember { mutableStateOf("") }
        var newBeds by remember { mutableStateOf("") }
        var newShower by remember { mutableStateOf(false) }
        var newToilet by remember { mutableStateOf(false) }

        val currenciesList = listOf("₸", "₽", "$", "€")

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.add_cabin_title)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = newName, onValueChange = { newName = it },
                        label = { Text(stringResource(R.string.cabin_name_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newPrice, onValueChange = { newPrice = it },
                            label = { Text(stringResource(R.string.price_per_day_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).padding(end = 8.dp), singleLine = true
                        )
                        Box {
                            TextButton(onClick = { expandedCurrency = true }) {
                                Text(newCurrency, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            DropdownMenu(expanded = expandedCurrency, onDismissRequest = { expandedCurrency = false }) {
                                currenciesList.forEach { curr ->
                                    DropdownMenuItem(text = { Text(curr) }, onClick = { newCurrency = curr; expandedCurrency = false })
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newRooms, onValueChange = { newRooms = it },
                        label = { Text(stringResource(R.string.rooms_count_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newBeds, onValueChange = { newBeds = it },
                        label = { Text(stringResource(R.string.beds_count_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = newShower, onCheckedChange = { newShower = it }); Text(stringResource(R.string.has_shower_label)) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = newToilet, onCheckedChange = { newToilet = it }); Text(stringResource(R.string.has_toilet_label)) }
                }
            },
            confirmButton = {
                val noNumber = stringResource(R.string.no_number)
                TextButton(
                    onClick = {
                        val newCabin = Cabin(
                            id = System.currentTimeMillis().toString(),
                            number = newName.ifBlank { noNumber }, photos = emptyList(), hasShower = newShower, hasToilet = newToilet,
                            roomsCount = newRooms.toIntOrNull() ?: 1, bedsCount = newBeds.toIntOrNull() ?: 1, comment = "",
                            price = newPrice.toIntOrNull() ?: 0, currency = newCurrency
                        )
                        onAddCabin(newCabin)
                        showAddDialog = false
                    },
                    enabled = newName.isNotBlank()
                ) { Text(stringResource(R.string.add_button)) }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.cancel_button)) } }
        )
    }

    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false }, title = { Text(stringResource(R.string.filters_title)) },
            text = {
                Column {
                    OutlinedTextField(value = maxPriceInput, onValueChange = { maxPriceInput = it }, label = { Text(stringResource(R.string.max_price_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                    OutlinedTextField(value = minRoomsInput, onValueChange = { minRoomsInput = it }, label = { Text(stringResource(R.string.min_rooms_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                    OutlinedTextField(value = minBedsInput, onValueChange = { minBedsInput = it }, label = { Text(stringResource(R.string.min_beds_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = reqShower, onCheckedChange = { reqShower = it }); Text(stringResource(R.string.req_shower_label)) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = reqToilet, onCheckedChange = { reqToilet = it }); Text(stringResource(R.string.req_toilet_label)) }

                    Spacer(modifier = Modifier.height(16.dp))

                    val dateRangeText = if (filterStartEpoch != null && filterEndEpoch != null) {
                        val formatter = DateTimeFormatter.ofPattern("dd.MM")
                        val start = Instant.ofEpochMilli(filterStartEpoch!!).atZone(ZoneId.of("UTC")).toLocalDate().format(formatter)
                        val end = Instant.ofEpochMilli(filterEndEpoch!!).atZone(ZoneId.of("UTC")).toLocalDate().format(formatter)
                        "$start - $end"
                    } else {
                        stringResource(R.string.all_dates_label)
                    }

                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1B4332))
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(stringResource(R.string.filter_dates_label), fontSize = 11.sp, color = Color.Gray)
                            Text(dateRangeText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFilterDialog = false }) { Text(stringResource(R.string.apply_button)) } },
            dismissButton = { 
                TextButton(onClick = { 
                    minRoomsInput = ""; minBedsInput = ""; maxPriceInput = ""; reqShower = false; reqToilet = false
                    filterStartEpoch = null; filterEndEpoch = null
                }) { Text(stringResource(R.string.reset_button)) } 
            }
        )
    }

    if (showDatePicker) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = filterStartEpoch,
            initialSelectedEndDateMillis = filterEndEpoch
        )
        val appLocale = AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault()
        val configuration = Configuration(LocalConfiguration.current).apply { setLocale(appLocale) }

        CompositionLocalProvider(LocalConfiguration provides configuration) {
            AlertDialog(
                onDismissRequest = { showDatePicker = false },
                modifier = Modifier.fillMaxHeight(0.9f),
                title = { Text(stringResource(R.string.select_dates_title)) },
                text = {
                    DateRangePicker(state = dateRangePickerState, modifier = Modifier.fillMaxSize())
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            filterStartEpoch = dateRangePickerState.selectedStartDateMillis
                            filterEndEpoch = dateRangePickerState.selectedEndDateMillis
                            showDatePicker = false
                        }
                    ) { Text(stringResource(R.string.apply_button)) }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            filterStartEpoch = null
                            filterEndEpoch = null
                            showDatePicker = false
                        }
                    ) { Text(stringResource(R.string.clear_button)) }
                }
            )
        }
    }
}



@Composable
fun CabinGridCard(cabin: Cabin, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE8E6E1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .background(Color(0xFFF1F0EA))
            ) {
                if (cabin.photos.isNotEmpty()) {
                    AsyncImage(
                        model = cabin.photos.first(),
                        contentDescription = cabin.number,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search, // Placeholder icon
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            cabin.number,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }


                val today = LocalDate.now()
                val formatter = DateTimeFormatter.ofPattern("dd.MM")
                val currentBooking = cabin.bookings.find { !today.isBefore(it.startDate) && !today.isAfter(it.endDate) }
                val nextBooking = cabin.bookings.filter { it.startDate.isAfter(today) }.minByOrNull { it.startDate }

                val (statusText, statusColor) = when {
                    currentBooking != null -> {
                        stringResource(R.string.status_unavailable_until, currentBooking.endDate.format(formatter)) to Color(0xFFC96A47)
                    }
                    nextBooking != null -> {
                        stringResource(R.string.status_available_until, nextBooking.startDate.format(formatter)) to Color(0xFF4CAF50)
                    }
                    else -> {
                        stringResource(R.string.status_available) to Color(0xFF4CAF50)
                    }
                }

                Surface(
                    modifier = Modifier.padding(12.dp).align(Alignment.TopEnd),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(statusColor, CircleShape)
                        )
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF333333)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.padding(12.dp).align(Alignment.BottomEnd),
                    shape = CircleShape,
                    color = Color(0xFF1B4332).copy(alpha = 0.9f)
                ) {
                    Text(
                        text = "$${cabin.price}${stringResource(R.string.per_night)}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = cabin.number,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CabinFeatureChip(text = "🚪${cabin.roomsCount}")
                    CabinFeatureChip(text = "🛏${cabin.bedsCount}")
                    if (cabin.hasShower) CabinFeatureChip(text = "🚿")
                    if (cabin.hasToilet) CabinFeatureChip(text = "🚽")
                }
            }
        }
    }
}

@Composable
fun CabinFeatureChip(text: String) {
    Surface(
        shape = RoundedCornerShape(100),
        color = Color(0xFFE0EFE5),
        contentColor = Color(0xFF1B4332)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}





@Preview(showBackground = true, widthDp = 1200)
@Composable
fun CabinListScreenPreview() {
    val cabins = remember {
        listOf(
            Cabin("1", "Cedar Ridge Cabin", emptyList(), true, true, 2, 6, "", 185, "$", 
                bookings = mutableStateListOf(
                    Booking("b0", LocalDate.now().minusDays(1), LocalDate.now().plusDays(2))
                )
            ),
            Cabin("2", "Lakefront Manor", emptyList(), true, false, 5, 12, "", 340, "$",
                bookings = mutableStateListOf(
                    Booking("b1", LocalDate.now().plusDays(3), LocalDate.now().plusDays(5))
                )
            ),
            Cabin("3", "Pine Hollow Cottage", emptyList(), true, true, 1, 4, "", 120, "$"),
            Cabin("4", "Birchwood Villa", emptyList(), true, true, 7, 16, "", 520, "$"),
            Cabin("5", "Meadow View House", emptyList(), true, true, 4, 8, "", 210, "$"),
            Cabin("6", "Riverside Cabin", emptyList(), true, true, 2, 5, "", 155, "$")
        )
    }
    ResortControlTheme {
        CabinListScreen(
            cabins = cabins,
            onCabinClick = {},
            onAddCabin = {}
        )
    }
}



