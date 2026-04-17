package com.example.tsumaps.ui.panel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsumaps.domain.models.Place
import com.example.tsumaps.domain.path.PathManager
import com.example.tsumaps.ui.getPlaceTimingInfo
import com.example.tsumaps.ui.isPlaceOpenNow
import com.example.tsumaps.ui.theme.TsuBrand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun BottomControlSurface(
    modifier: Modifier = Modifier,
    isBottomPanelExpanded: Boolean,
    onToggleBottomPanel: () -> Unit,
    onSwipeExpand: () -> Unit,
    onSwipeCollapse: () -> Unit,
    expandedContent: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        when {
                            totalDrag < -30f -> onSwipeExpand()
                            totalDrag > 30f -> onSwipeCollapse()
                        }
                        totalDrag = 0f
                    },
                    onDragCancel = { totalDrag = 0f }
                )
            },
        color = TsuBrand.PanelSurface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBottomPanelExpanded) "Панель управления" else "",
                    color = Color.White,
                    fontSize = 12.sp
                )
                Button(
                    onClick = onToggleBottomPanel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF374151)
                    )
                ) {
                    Text(if (isBottomPanelExpanded) "▼" else "▲")
                }
            }
            AnimatedVisibility(
                visible = isBottomPanelExpanded,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
            ) {
                expandedContent()
            }
        }
    }
}

@Composable
fun AlgorithmExpandedContent(
    message: String,
    currentMode: String,
    selectedGoods: Set<String>,
    algorithmsInfo: String?,
    scope: CoroutineScope,
    pm: PathManager,
    tempSelectedGoods: SnapshotStateList<String>,
    relevantGoodsPlaces: List<Place>,
    onShowGoodsPicker: () -> Unit,
    onOpenDecisionTree: () -> Unit,
    onOpenClustering: () -> Unit,
    onOpenAco: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = message,
            color = Color.White,
            fontSize = 13.sp
        )

        Text(
            text = "Алгоритмы",
            color = Color(0xFFE5E7EB),
            fontSize = 13.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        pm.setSearchAlgorithm(PathManager.SearchAlgorithm.Astar)
                        pm.clearGoodsRequest()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B5563))
            ) { Text("A*") }
            Button(
                onClick = {
                    scope.launch {
                        pm.setSearchAlgorithm(PathManager.SearchAlgorithm.GenericAlgorithm)
                        tempSelectedGoods.clear()
                        tempSelectedGoods.addAll(pm.requiredGoods)
                        onShowGoodsPicker()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = TsuBrand.AccentBlue)
            ) { Text("Генетический алгоритм") }
        }

        Button(
            onClick = onOpenDecisionTree,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
        ) { Text("Дерево решений — куда пойти поесть") }

        Button(
            onClick = onOpenClustering,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
        ) { Text("Кластеризация — зоны еды") }

        Button(
            onClick = onOpenAco,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
        ) { Text("Муравьиный алгоритм — TSP") }

        if (algorithmsInfo != null) {
            Surface(
                color = Color(0xFF0B1220).copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = algorithmsInfo,
                    color = Color(0xFFE5E7EB),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
        if (currentMode == "goods") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedGoods.isEmpty()) {
                        "Товары: не выбраны"
                    } else {
                        "Товары: ${selectedGoods.joinToString()}"
                    },
                    color = Color(0xFFC7D2FE),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        scope.launch { pm.clearGoodsRequest() }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4B5563)
                    )
                ) {
                    Text("Сброс товаров")
                }
            }

            if (selectedGoods.isNotEmpty()) {
                Text(
                    text = "Подходящие магазины",
                    color = Color(0xFFE5E7EB),
                    fontSize = 13.sp
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    relevantGoodsPlaces.forEach { place ->
                        val timing = getPlaceTimingInfo(place)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF1F2937),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = place.name,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = timing,
                                    color = if (isPlaceOpenNow(place)) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Товары: ${
                                        place.menu.filter { it in selectedGoods }
                                            .joinToString()
                                    }",
                                    color = Color(0xFFBFDBFE),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
