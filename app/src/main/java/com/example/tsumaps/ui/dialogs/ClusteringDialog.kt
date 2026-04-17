package com.example.tsumaps.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsumaps.ui.theme.tsuHeaderButtonColors

@Composable
fun ClusteringDialog(
    onDismiss: () -> Unit,
    onRun: (k: Int) -> Unit
) {
    var k by remember { mutableIntStateOf(3) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = { Text("Кластеризация — зоны еды", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "K-means по точкам заведений. Цвет обводки — зона (кластер) по пешеходной метрике (A* по карте). " +
                        "Бонус: сравнение с евклидовой метрикой — заведения, сменившие кластер, подсвечиваются янтарным.",
                    color = Color(0xFFB0BEC5),
                    fontSize = 12.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = k == 2, onClick = { k = 2 }, label = { Text("k=2") })
                    FilterChip(selected = k == 3, onClick = { k = 3 }, label = { Text("k=3") })
                    FilterChip(selected = k == 4, onClick = { k = 4 }, label = { Text("k=4") })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onRun(k) },
                colors = tsuHeaderButtonColors()
            ) { Text("Запустить") }
        },
        dismissButton = {
            Button(onClick = onDismiss, colors = tsuHeaderButtonColors()) { Text("Отмена") }
        }
    )
}
