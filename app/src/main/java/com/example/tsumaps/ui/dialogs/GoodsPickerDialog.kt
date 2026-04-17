package com.example.tsumaps.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsumaps.ui.theme.TsuBrand

@Composable
fun GoodsPickerDialog(
    allGoods: List<String>,
    tempSelectedGoods: SnapshotStateList<String>,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onClearSelection: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Выберите товары",
                    color = Color.White,
                    fontSize = 18.sp
                )
                Text(
                    text = "Выбранные товары используются для поиска подходящих магазинов",
                    color = Color(0xFF9FB1C4),
                    fontSize = 11.sp
                )
                Button(
                    onClick = onClearSelection,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF334155)
                    )
                ) {
                    Text("Очистить выбор")
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    allGoods.forEach { good ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = good in tempSelectedGoods,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (good !in tempSelectedGoods) tempSelectedGoods.add(good)
                                    } else {
                                        tempSelectedGoods.remove(good)
                                    }
                                }
                            )
                            Text(
                                text = good,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TsuBrand.AccentBlue
                )
            ) {
                Text("Применить")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF334155)
                )
            ) {
                Text("Отмена")
            }
        }
    )
}
