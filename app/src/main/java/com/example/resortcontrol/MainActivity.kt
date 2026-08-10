package com.example.resortcontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

                    val cabins = remember {
                        mutableStateListOf(
                            Cabin("1", "№12 VIP", emptyList(), true, true, 2, 5, "", 15000, "₸"),
                            Cabin("2", "№8 Стандарт", emptyList(), true, false, 1, 2, "", 8000, "₸"),
                            Cabin("3", "№3 Эконом", emptyList(), false, false, 1, 4, "", 5000, "₸")
                        )
                    }

                    var selectedCabin by remember { mutableStateOf<Cabin?>(null) }

                    if (selectedCabin == null) {
                        CabinListScreen(
                            cabins = cabins,
                            onCabinClick = { clickedCabin ->
                                selectedCabin = clickedCabin
                            }
                        )
                    } else {
                        Column {
                            TextButton(
                                onClick = { selectedCabin = null },
                                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                            ) {
                                Text("< Назад к списку", fontSize = 16.sp)
                            }

                            // Вызов без лишних параметров
                            CabinDetailScreen(cabin = selectedCabin!!)
                        }
                    }
                }
            }
        }
    }
}