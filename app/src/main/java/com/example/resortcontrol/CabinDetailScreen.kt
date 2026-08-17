package com.example.resortcontrol

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage

@Composable
fun CabinDetailScreen(
    cabin: Cabin,
    onAddBooking: (Booking) -> Unit = {},
    onDeleteBooking: (Booking) -> Unit = {},
    onUpdateCabin: (Cabin) -> Unit = {}
) {
    var currentCabin by remember { mutableStateOf(cabin) }
    val photos = remember { mutableStateListOf(*currentCabin.photos.toTypedArray()) }
    var selectedPhotoForView by remember { mutableStateOf<String?>(null) }

    var showEditFeaturesDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditPriceDialog by remember { mutableStateOf(false) } // НОВОЕ: Состояние для окна изменения цены

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> 
            if (uri != null) { 
                photos.add(uri.toString()) 
                val updated = currentCabin.copy(photos = photos.toList())
                currentCabin = updated
                onUpdateCabin(updated)
            } 
        }
    )

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "${stringResource(R.string.cabin_prefix)} ${currentCabin.number}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { showEditNameDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_name_desc), tint = Color.Gray)
                }
            }

            // КЛИКАБЕЛЬНЫЙ БЛОК ЦЕНЫ
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showEditPriceDialog = true } // Открывает окно цены по клику
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(R.string.price_label)} ${currentCabin.price} ${currentCabin.currency} ${stringResource(R.string.per_day)}",
                    fontSize = 18.sp,
                    color = Color(0xFF388E3C),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit_price_desc),
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(photos.size + 1) { index ->
                if (index < photos.size) {
                    val currentPhoto = photos[index]
                    Box(
                        modifier = Modifier
                            .size(width = 300.dp, height = 200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF1F0EA))
                    ) {
                        AsyncImage(
                            model = currentPhoto, 
                            contentDescription = stringResource(R.string.photo_desc), 
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clickable { selectedPhotoForView = currentPhoto }
                        )
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f)
                        ) {
                            IconButton(
                                onClick = { 
                                    photos.removeAt(index) 
                                    val updated = currentCabin.copy(photos = photos.toList())
                                    currentCabin = updated
                                    onUpdateCabin(updated)
                                },
                                modifier = Modifier.size(32.dp)
                            ) { Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(width = 300.dp, height = 200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF1F0EA))
                            .clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center
                    ) { 
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color(0xFF1B4332))
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.add_photo_desc), color = Color(0xFF1B4332), fontWeight = FontWeight.Medium, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // FEATURES SECTION
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.section_amenities), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showEditFeaturesDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureTag("🚪 ${currentCabin.roomsCount} " + stringResource(R.string.rooms_suffix))
                FeatureTag("🛏 ${currentCabin.bedsCount} " + stringResource(R.string.sleeping_spaces_suffix))
                if (currentCabin.hasShower) FeatureTag("🚿 " + stringResource(R.string.shower_label))
                if (currentCabin.hasToilet) FeatureTag("🚽 " + stringResource(R.string.toilet_label))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = currentCabin.comment,
            onValueChange = { newComment ->
                val updated = currentCabin.copy(comment = newComment)
                currentCabin = updated
                onUpdateCabin(updated)
            },
            label = { Text(stringResource(R.string.admin_note_label)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
        BookingTimelineVisualizer(
            cabin = currentCabin,
            onAddBooking = onAddBooking,
            onDeleteBooking = onDeleteBooking
        )
        Spacer(modifier = Modifier.height(32.dp))
    }

    // Окно редактирования названия
    if (showEditNameDialog) {
        var nameInput by remember { mutableStateOf(currentCabin.number) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false }, title = { Text(stringResource(R.string.edit_name_title)) },
            text = {
                OutlinedTextField(
                    value = nameInput, onValueChange = { nameInput = it },
                    label = { Text(stringResource(R.string.edit_name_hint)) }, modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val updated = currentCabin.copy(number = nameInput)
                    currentCabin = updated
                    onUpdateCabin(updated)
                    showEditNameDialog = false
                }) { Text(stringResource(R.string.save_button)) }
            },
            dismissButton = { TextButton(onClick = { showEditNameDialog = false }) { Text(stringResource(R.string.cancel_button)) } }
        )
    }

    // НОВОЕ: Окно редактирования цены и валюты
    if (showEditPriceDialog) {
        var priceInput by remember { mutableStateOf(currentCabin.price.toString()) }
        var currencyInput by remember { mutableStateOf(currentCabin.currency) }
        var expandedCurrency by remember { mutableStateOf(false) }
        val currenciesList = listOf("₸", "₽", "$", "€")

        AlertDialog(
            onDismissRequest = { showEditPriceDialog = false },
            title = { Text(stringResource(R.string.edit_price_desc)) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text(stringResource(R.string.price_per_day_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        singleLine = true
                    )
                    Box {
                        TextButton(onClick = { expandedCurrency = true }) {
                            Text(currencyInput, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = expandedCurrency, onDismissRequest = { expandedCurrency = false }) {
                            currenciesList.forEach { curr ->
                                DropdownMenuItem(
                                    text = { Text(curr, fontSize = 18.sp) },
                                    onClick = { currencyInput = curr; expandedCurrency = false }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val updated = currentCabin.copy(
                        price = priceInput.toIntOrNull() ?: 0,
                        currency = currencyInput
                    )
                    currentCabin = updated
                    onUpdateCabin(updated)
                    showEditPriceDialog = false
                }) { Text(stringResource(R.string.save_button)) }
            },
            dismissButton = { TextButton(onClick = { showEditPriceDialog = false }) { Text(stringResource(R.string.cancel_button)) } }
        )
    }

    // Окно характеристик (теперь БЕЗ поля цены)
    if (showEditFeaturesDialog) {
        var roomsInput by remember { mutableStateOf(currentCabin.roomsCount.toString()) }
        var bedsInput by remember { mutableStateOf(currentCabin.bedsCount.toString()) }
        var hasShowerInput by remember { mutableStateOf(currentCabin.hasShower) }
        var hasToiletInput by remember { mutableStateOf(currentCabin.hasToilet) }

        AlertDialog(
            onDismissRequest = { showEditFeaturesDialog = false }, title = { Text(stringResource(R.string.edit_features_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = roomsInput, onValueChange = { roomsInput = it },
                        label = { Text(stringResource(R.string.rooms_count_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = bedsInput, onValueChange = { bedsInput = it },
                        label = { Text(stringResource(R.string.beds_count_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = hasShowerInput, onCheckedChange = { hasShowerInput = it }); Text(stringResource(R.string.has_shower_label)) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = hasToiletInput, onCheckedChange = { hasToiletInput = it }); Text(stringResource(R.string.has_toilet_label)) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val updated = currentCabin.copy(
                        roomsCount = roomsInput.toIntOrNull() ?: 0,
                        bedsCount = bedsInput.toIntOrNull() ?: 0,
                        hasShower = hasShowerInput,
                        hasToilet = hasToiletInput
                    )
                    currentCabin = updated
                    onUpdateCabin(updated)
                    showEditFeaturesDialog = false
                }) { Text(stringResource(R.string.save_button)) }
            },
            dismissButton = { TextButton(onClick = { showEditFeaturesDialog = false }) { Text(stringResource(R.string.cancel_button)) } }
        )
    }

    if (selectedPhotoForView != null) {
        Dialog(
            onDismissRequest = { selectedPhotoForView = null }, properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                var scale by remember { mutableStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }
                AsyncImage(
                    model = selectedPhotoForView, contentDescription = "Zoomed photo", contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            if (scale > 1f) { offset += pan } else { offset = Offset.Zero }
                        }
                    }.graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                )
                IconButton(
                    onClick = { selectedPhotoForView = null }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                ) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_button), tint = Color.White) }
            }
        }
    }
}

@Composable
fun FeatureTag(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFE0EFE5),
        contentColor = Color(0xFF1B4332)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

