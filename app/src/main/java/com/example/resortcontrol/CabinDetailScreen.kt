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
import coil.compose.AsyncImage

@Composable
fun CabinDetailScreen(cabin: Cabin) {
    var currentCabin by remember { mutableStateOf(cabin) }
    val photos = remember { mutableStateListOf(*currentCabin.photos.toTypedArray()) }
    var selectedPhotoForView by remember { mutableStateOf<String?>(null) }

    var showEditFeaturesDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditPriceDialog by remember { mutableStateOf(false) } // НОВОЕ: Состояние для окна изменения цены

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) { photos.add(uri.toString()) } }
    )

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Домик ${currentCabin.number}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { showEditNameDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Изменить название", tint = Color.Gray)
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
                    text = "Цена: ${currentCabin.price} ${currentCabin.currency} / сутки",
                    fontSize = 18.sp,
                    color = Color(0xFF388E3C),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Изменить цену",
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(photos.size + 1) { index ->
                if (index < photos.size) {
                    val currentPhoto = photos[index]
                    Box(
                        modifier = Modifier.size(width = 280.dp, height = 180.dp).clip(RoundedCornerShape(12.dp)).background(Color.LightGray)
                    ) {
                        AsyncImage(
                            model = currentPhoto, contentDescription = "Фото", contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clickable { selectedPhotoForView = currentPhoto }
                        )
                        IconButton(
                            onClick = { photos.removeAt(index) },
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(32.dp).background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(50))
                        ) { Text("X", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                } else {
                    Box(
                        modifier = Modifier.size(width = 280.dp, height = 180.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE0E0E0))
                            .clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center
                    ) { Icon(imageVector = Icons.Default.Add, contentDescription = "Добавить фото", modifier = Modifier.size(48.dp), tint = Color.Gray) }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureChip(text = "🚪 ${currentCabin.roomsCount}")
                FeatureChip(text = "🛏 ${currentCabin.bedsCount}")
                if (currentCabin.hasShower) FeatureChip(text = "🚿")
                if (currentCabin.hasToilet) FeatureChip(text = "🚽")
            }
            IconButton(onClick = { showEditFeaturesDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Редактировать характеристики", tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = currentCabin.comment,
            onValueChange = { newComment ->
                currentCabin = currentCabin.copy(comment = newComment)
            },
            label = { Text("Заметка администратора") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
        BookingTimelineVisualizer(cabin = currentCabin)
        Spacer(modifier = Modifier.height(32.dp))
    }

    // Окно редактирования названия
    if (showEditNameDialog) {
        var nameInput by remember { mutableStateOf(currentCabin.number) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false }, title = { Text("Название домика") },
            text = {
                OutlinedTextField(
                    value = nameInput, onValueChange = { nameInput = it },
                    label = { Text("Введите название/номер") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    currentCabin = currentCabin.copy(number = nameInput)
                    showEditNameDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showEditNameDialog = false }) { Text("Отмена") } }
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
            title = { Text("Изменить цену") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text("Цена за сутки") },
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
                    currentCabin = currentCabin.copy(
                        price = priceInput.toIntOrNull() ?: 0,
                        currency = currencyInput
                    )
                    showEditPriceDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showEditPriceDialog = false }) { Text("Отмена") } }
        )
    }

    // Окно характеристик (теперь БЕЗ поля цены)
    if (showEditFeaturesDialog) {
        var roomsInput by remember { mutableStateOf(currentCabin.roomsCount.toString()) }
        var bedsInput by remember { mutableStateOf(currentCabin.bedsCount.toString()) }
        var hasShowerInput by remember { mutableStateOf(currentCabin.hasShower) }
        var hasToiletInput by remember { mutableStateOf(currentCabin.hasToilet) }

        AlertDialog(
            onDismissRequest = { showEditFeaturesDialog = false }, title = { Text("Характеристики домика") },
            text = {
                Column {
                    OutlinedTextField(
                        value = roomsInput, onValueChange = { roomsInput = it },
                        label = { Text("Количество комнат") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = bedsInput, onValueChange = { bedsInput = it },
                        label = { Text("Спальные места") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = hasShowerInput, onCheckedChange = { hasShowerInput = it }); Text("Наличие душа") }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = hasToiletInput, onCheckedChange = { hasToiletInput = it }); Text("Наличие туалета") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    currentCabin = currentCabin.copy(
                        roomsCount = roomsInput.toIntOrNull() ?: 0,
                        bedsCount = bedsInput.toIntOrNull() ?: 0,
                        hasShower = hasShowerInput,
                        hasToilet = hasToiletInput
                    )
                    showEditFeaturesDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showEditFeaturesDialog = false }) { Text("Отмена") } }
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
                ) { Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White) }
            }
        }
    }
}

@Composable
fun FeatureChip(text: String) {
    Box(
        modifier = Modifier.background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
    ) { Text(text = text, fontSize = 14.sp) }
}