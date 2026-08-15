package com.example.resortcontrol

import androidx.compose.ui.res.stringResource
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
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

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ResortControlTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

                    val cabins = remember {
                        mutableStateListOf(
                            Cabin("1", "№12 VIP", emptyList(), true, true, 2, 5, "", 15000, "₸"),
                            Cabin("2", "№8 Стандарт", emptyList(), true, false, 1, 2, "", 8000, "₸"),
                            Cabin("3", "№3 Эконом", emptyList(), false, false, 1, 4, "", 5000, "₸")
                        )
                    }

                    var selectedCabinId by rememberSaveable { mutableStateOf<String?>(null) }
                    val selectedCabin = cabins.find { it.id == selectedCabinId }

                    if (selectedCabinId == null || selectedCabin == null) {
                        CabinListScreen(
                            cabins = cabins,
                            onCabinClick = { clickedCabin ->
                                selectedCabinId = clickedCabin.id
                            },
                            // НОВОЕ: Обработчик добавления нового домика
                            onAddCabin = { newCabin ->
                                cabins.add(newCabin)
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

                            CabinDetailScreen(cabin = selectedCabin)
                        }
                    }
                }
            }
        }
    }
}