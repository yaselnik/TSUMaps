package com.example.tsumaps.ui.dialogs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.tsumaps.domain.algorithms.decisionTree.DecisionTree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.tsumaps.ui.theme.tsuHeaderButtonColors

@Composable
fun DecisionTreeMenuDialog(
    csvText: String,
    onCsvTextChange: (String) -> Unit,
    onPickCsvFile: () -> Unit,
    onDismiss: () -> Unit,
    decisionTree: DecisionTree?,
    decisionTreeBuiltText: String?,
    decisionTreePathText: String?,
    dtLocation: String,
    onDtLocationChange: (String) -> Unit,
    dtBudget: String,
    onDtBudgetChange: (String) -> Unit,
    dtTime: String,
    onDtTimeChange: (String) -> Unit,
    dtFoodTypeUi: String,
    onDtFoodTypeChange: (String) -> Unit,
    dtQueue: String,
    onDtQueueChange: (String) -> Unit,
    dtWeather: String,
    onDtWeatherChange: (String) -> Unit,
    onInfo: (String) -> Unit,
    onBuildTree: suspend () -> Unit,
    onClassify: suspend () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (csvText.isBlank()) {
            onCsvTextChange(
                withContext(Dispatchers.IO) {
                    context.assets.open("decision_tree_samples.csv").bufferedReader().use { it.readText() }
                }
            )
        }
    }

    val bodyScroll = rememberScrollState()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Дерево решений — куда пойти поесть",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .heightIn(max = 420.dp)
                        .verticalScroll(bodyScroll),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Шаг 1: загрузить CSV", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onPickCsvFile,
                            modifier = Modifier.weight(1f),
                            colors = tsuHeaderButtonColors()
                        ) { Text("Выбрать файл") }
                        Button(
                            onClick = {
                                scope.launch {
                                    onCsvTextChange(
                                        withContext(Dispatchers.IO) {
                                            context.assets.open("decision_tree_samples.csv").bufferedReader()
                                                .use { it.readText() }
                                        }
                                    )
                                    onInfo("✅ Загружен CSV по умолчанию (из фото)")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = tsuHeaderButtonColors()
                        ) { Text("По умолчанию") }
                    }

                    Text("CSV текст:", color = Color(0xFF9FB1C4), fontSize = 11.sp)
                    TextField(
                        value = csvText,
                        onValueChange = onCsvTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 220.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                    )

                    Button(
                        onClick = { scope.launch { onBuildTree() } },
                        modifier = Modifier.fillMaxWidth(),
                        colors = tsuHeaderButtonColors()
                    ) { Text("Шаг 2: построить дерево") }

                    decisionTreeBuiltText?.let { treeText ->
                        Text("Построенное дерево:", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                        Surface(color = Color(0xFF0B1220).copy(alpha = 0.6f), shape = RoundedCornerShape(12.dp)) {
                            Text(
                                text = treeText,
                                color = Color(0xFFE5E7EB),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Text("Шаг 3: ввод параметров", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                        chipRow {
                            FilterChip(selected = dtLocation == "main_building", onClick = { onDtLocationChange("main_building") }, label = { Text("main_building") })
                            FilterChip(selected = dtLocation == "second_building", onClick = { onDtLocationChange("second_building") }, label = { Text("second_building") })
                            FilterChip(selected = dtLocation == "campus_center", onClick = { onDtLocationChange("campus_center") }, label = { Text("campus_center") })
                        }
                        chipRow {
                            FilterChip(selected = dtBudget == "low", onClick = { onDtBudgetChange("low") }, label = { Text("budget: low") })
                            FilterChip(selected = dtBudget == "medium", onClick = { onDtBudgetChange("medium") }, label = { Text("budget: medium") })
                            FilterChip(selected = dtBudget == "high", onClick = { onDtBudgetChange("high") }, label = { Text("budget: high") })
                        }
                        chipRow {
                            FilterChip(selected = dtTime == "very_short", onClick = { onDtTimeChange("very_short") }, label = { Text("time: very_short") })
                            FilterChip(selected = dtTime == "short", onClick = { onDtTimeChange("short") }, label = { Text("time: short") })
                            FilterChip(selected = dtTime == "medium", onClick = { onDtTimeChange("medium") }, label = { Text("time: medium") })
                        }
                        chipRow {
                            FilterChip(selected = dtFoodTypeUi == "snack", onClick = { onDtFoodTypeChange("snack") }, label = { Text("snack") })
                            FilterChip(selected = dtFoodTypeUi == "coffee", onClick = { onDtFoodTypeChange("coffee") }, label = { Text("coffee") })
                            FilterChip(selected = dtFoodTypeUi == "full_meal", onClick = { onDtFoodTypeChange("full_meal") }, label = { Text("full_meal") })
                            FilterChip(selected = dtFoodTypeUi == "pancakes", onClick = { onDtFoodTypeChange("pancakes") }, label = { Text("pancakes") })
                        }
                        chipRow {
                            FilterChip(selected = dtQueue == "low", onClick = { onDtQueueChange("low") }, label = { Text("queue: low") })
                            FilterChip(selected = dtQueue == "medium", onClick = { onDtQueueChange("medium") }, label = { Text("queue: medium") })
                        }
                        chipRow {
                            FilterChip(selected = dtWeather == "good", onClick = { onDtWeatherChange("good") }, label = { Text("weather: good") })
                            FilterChip(selected = dtWeather == "bad", onClick = { onDtWeatherChange("bad") }, label = { Text("weather: bad") })
                        }

                        Button(
                            onClick = {
                                if (decisionTree != null) {
                                    scope.launch { onClassify() }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = tsuHeaderButtonColors()
                        ) { Text("Шаг 4: ответ + путь") }

                        decisionTreePathText?.let { pathText ->
                            Surface(color = Color(0xFF0B1220).copy(alpha = 0.6f), shape = RoundedCornerShape(12.dp)) {
                                Text(
                                    text = pathText,
                                    color = Color(0xFFE5E7EB),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = onDismiss, colors = tsuHeaderButtonColors()) {
                        Text("Закрыть")
                    }
                }
            }
        }
    }
}

@Composable
private fun chipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}
