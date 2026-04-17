package com.example.tsumaps.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsumaps.domain.models.Attraction
import com.example.tsumaps.ui.theme.tsuHeaderButtonColors

@Composable
fun AntColonyDialog(
    attractions: List<Attraction>,
    onDismiss: () -> Unit,
    onRun: (chosen: List<Attraction>) -> Unit
) {
    val acoSelections = remember(attractions) {
        mutableStateListOf<Boolean>().also { list ->
            repeat(attractions.size) { list.add(true) }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = { Text("Муравьиный алгоритм — TSP по достопримечательностям", color = Color.White) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Старт — текущая точка на карте (двойной тап в режиме маршрута). Отметьте вершины тура.",
                    color = Color(0xFFB0BEC5),
                    fontSize = 12.sp
                )
                if (attractions.isEmpty()) {
                    Text("Нет достопримечательностей в данных (addAllAttractions).", color = Color(0xFFF87171), fontSize = 12.sp)
                } else {
                    attractions.forEachIndexed { index, att ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = acoSelections.getOrElse(index) { true },
                                onCheckedChange = { v ->
                                    while (acoSelections.size <= index) acoSelections.add(true)
                                    acoSelections[index] = v
                                }
                            )
                            Text(att.marker.name, color = Color(0xFFE5E7EB), fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val chosen = attractions.filterIndexed { i, _ ->
                        acoSelections.getOrElse(i) { true }
                    }
                    onRun(chosen)
                },
                enabled = attractions.isNotEmpty(),
                colors = tsuHeaderButtonColors()
            ) { Text("Запустить") }
        },
        dismissButton = {
            Button(onClick = onDismiss, colors = tsuHeaderButtonColors()) { Text("Отмена") }
        }
    )
}
