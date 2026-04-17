package com.example.tsumaps

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsumaps.domain.debug.WalkableOverlay
import com.example.tsumaps.domain.algorithms.AntColonyOptimization
import com.example.tsumaps.domain.algorithms.ClusteringAlgorithm
import com.example.tsumaps.domain.algorithms.decisionTree.CSVParser
import com.example.tsumaps.domain.algorithms.decisionTree.DecisionTree
import com.example.tsumaps.domain.algorithms.decisionTree.DecisionTreeNode
import com.example.tsumaps.domain.map.MapGrid
import com.example.tsumaps.domain.map.MapMarker
import com.example.tsumaps.domain.map.MapMarkerManager
import com.example.tsumaps.domain.map.MarkerType
import com.example.tsumaps.domain.map.MapWithMarkers
import com.example.tsumaps.domain.path.PathManager
import com.example.tsumaps.domain.path.PathPointsOverlay
import com.example.tsumaps.domain.models.CoworkingLocation
import com.example.tsumaps.domain.models.Place
import com.example.tsumaps.domain.models.PlaceStorage
import com.example.tsumaps.domain.models.Sample
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.example.tsumaps.domain.models.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var initError by remember { mutableStateOf<String?>(null) }
            var uiReady by remember { mutableStateOf(false) }

            var mapGrid by remember { mutableStateOf<MapGrid?>(null) }
            var markerManager by remember { mutableStateOf<MapMarkerManager?>(null) }
            var pathManager by remember { mutableStateOf<PathManager?>(null) }
            var placeStorage by remember { mutableStateOf<PlaceStorage?>(null) }

            var decisionTree by remember { mutableStateOf<DecisionTree?>(null) }

            LaunchedEffect(Unit) {
                try {
                    val grid = MapGrid(this@MainActivity)
                    withContext(Dispatchers.Default) {
                        grid.buildFromImage(R.drawable.map_for_grid)
                    }

                    val pm = PathManager(grid)
                    val ps = PlaceStorage().also { storage ->
                        addAllGoods(storage)
                        pm.updatePlaces(storage.places.associateBy { it.id })
                    }

                    val mm = MapMarkerManager(grid).also {
                        it.clearMarkers()
                        ps.places.forEach { place -> it.addPlaceMarker(place) }
                    }

                    val samples = CSVParser().parseAsset(this@MainActivity, "decision_tree_samples.csv")
                    val tree = DecisionTree(root = DecisionTreeNode.Leaf("Unknown")).also {
                        it.buildTree(
                            samples = samples,
                            attributes = listOf(
                                "location",
                                "budget",
                                "time_available",
                                "food_type",
                                "queue_tolerance",
                                "weather"
                            )
                        )
                    }

                    mapGrid = grid
                    pathManager = pm
                    placeStorage = ps
                    markerManager = mm
                    decisionTree = tree
                    uiReady = true
                } catch (e: Exception) {
                    Log.e("MainActivity", "Init error: ${e.message}", e)
                    initError = e.message ?: "Unknown error"
                }
            }

            if (!uiReady) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0B1220)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = initError?.let { "Ошибка инициализации: $it" } ?: "Загрузка карты…",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
                return@setContent
            }

            val grid = mapGrid!!
            val mm = markerManager!!
            val pm = pathManager!!
            val ps = placeStorage!!
            val tree = decisionTree

            val startPoint = pm.startPoint
            val endPoint = pm.endPoint
            val path = pm.path
            val message = pm.message
            val selectedAlgorithm = pm.selectedAlgorithm
            val currentMode = pm.mode
            val selectedGoods = pm.requiredGoods

            var showWalkable by remember { mutableStateOf(false) }
            var showSettings by remember { mutableStateOf(false) }
            var isBottomPanelExpanded by remember { mutableStateOf(false) }
            var selectedPlace by remember { mutableStateOf<Place?>(null) }
            var showGoodsPicker by remember { mutableStateOf(false) }
            val tempSelectedGoods = remember { mutableStateListOf<String>() }
            val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
            val allGoods = remember(ps.goods) { ps.goods.toList().sorted() }
            val relevantGoodsPlaces = remember(selectedGoods, ps.places) {
                if (selectedGoods.isEmpty()) {
                    emptyList()
                } else {
                    ps.places.filter { place ->
                        place.menu.any { it in selectedGoods }
                    }
                }
            }

            var dtBudget by remember { mutableStateOf("medium") }
            var dtTime by remember { mutableStateOf("short") }
            var dtFoodType by remember { mutableStateOf("grocery") }
            var dtQueue by remember { mutableStateOf("medium") }
            var dtWeather by remember { mutableStateOf("any") }
            val scope = rememberCoroutineScope()

            var algorithmsInfo by remember { mutableStateOf<String?>(null) }
            var markerAccentByKey by remember { mutableStateOf<Map<String, Color>>(emptyMap()) }

            var showPathMenu by remember { mutableStateOf(false) }
            var showDecisionTreeMenu by remember { mutableStateOf(false) }
            var showClusteringMenu by remember { mutableStateOf(false) }
            var showAcoMenu by remember { mutableStateOf(false) }

            var decisionTreeCsvText by remember { mutableStateOf("") }
            var decisionTreeBuiltText by remember { mutableStateOf<String?>(null) }
            var decisionTreePathText by remember { mutableStateOf<String?>(null) }
            var dtLocation by remember { mutableStateOf("main_building") }
            var dtFoodTypeUi by remember { mutableStateOf("coffee") }

            val csvPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent(),
                onResult = { uri ->
                    if (uri != null) {
                        scope.launch {
                            try {
                                val content = withContext(Dispatchers.IO) {
                                    contentResolver.openInputStream(uri)
                                        ?.bufferedReader()
                                        ?.use { it.readText() }
                                } ?: ""
                                decisionTreeCsvText = content
                                algorithmsInfo = "✅ CSV загружен из файла (${content.lines().size} строк)"
                            } catch (e: Exception) {
                                algorithmsInfo = "❌ Не удалось прочитать CSV: ${e.message}"
                            }
                        }
                    }
                }
            )

            fun guessFoodTypeFromGoods(goods: Set<String>): String {
                val g = goods.map { it.lowercase() }.toSet()
                return when {
                    g.any { "кофе" in it } -> "coffee"
                    g.any { "блин" in it } -> "food"
                    g.any { it.contains("первое") || it.contains("второе") || it.contains("пельмени") } -> "meal"
                    g.any { it.contains("руч") || it.contains("карандаш") || it.contains("хоз") } -> "stationery"
                    g.isNotEmpty() -> "grocery"
                    else -> "grocery"
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {

                MapWithMarkers(
                    mapImageRes = R.drawable.map,
                    mapGrid = grid,
                    markerManager = mm,
                    onMapDoubleTap = { point ->
                        Log.d("MainActivity", "Tap: $point")
                        selectedPlace = null
                        pm.onDoubleTap(point)
                    },
                    onMarkerClick = { marker ->
                        Log.d("Marker", "Clicked: ${marker.name}")
                        selectedPlace = marker.place
                    },
                    onMapTransform = {
                        isBottomPanelExpanded = false
                    }
                    ,
                    markerAccentColor = { marker ->
                        val key = marker.id?.toString() ?: marker.name
                        markerAccentByKey[key]
                    }
                ) { zoom, pan, imageActualSize, _ ->

                    WalkableOverlay(
                        mapGrid = grid,
                        zoom = zoom,
                        pan = pan,
                        imageActualSize = imageActualSize,
                        enabled = showWalkable
                    )

                    PathPointsOverlay(
                        startPoint = startPoint,
                        endPoint = endPoint,
                        path = path,
                        mapGrid = grid,
                        zoom = zoom,
                        pan = pan,
                        imageActualSize = imageActualSize
                    )
                }

                    IconButton(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        onClick = { showSettings = !showSettings }
                    ) {
                        Text(
                            text = "\u2699",
                            color = Color.White,
                            fontSize = 24.sp
                        )
                    }

                    if (showSettings) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 68.dp, end = 16.dp)
                                .widthIn(max = 320.dp),
                            color = Color(0xFF1F2937).copy(alpha = 0.94f),
                            shape = RoundedCornerShape(16.dp),
                            shadowElevation = 10.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Настройки",
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = showWalkable,
                                        onCheckedChange = { showWalkable = it }
                                    )
                                    Text(
                                        text = "Показать проходимость",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .fillMaxWidth(0.94f)
                            .widthIn(max = 420.dp)
                            .pointerInput(Unit) {
                                var totalDrag = 0f
                                detectVerticalDragGestures(
                                    onVerticalDrag = { _, dragAmount ->
                                        totalDrag += dragAmount
                                    },
                                    onDragEnd = {
                                        when {
                                            totalDrag < -30f -> isBottomPanelExpanded = true
                                            totalDrag > 30f -> isBottomPanelExpanded = false
                                        }
                                        totalDrag = 0f
                                    },
                                    onDragCancel = { totalDrag = 0f }
                                )
                            },
                        color = Color(0xFF111827).copy(alpha = 0.82f),
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
                                    onClick = { isBottomPanelExpanded = !isBottomPanelExpanded },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(
                                            0xFF374151
                                        )
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
                                    Button(
                                        onClick = { showPathMenu = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B5563))
                                    ) { Text("Маршрут — поиск пути") }

                                    Button(
                                        onClick = { showDecisionTreeMenu = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                                    ) { Text("Дерево решений — куда пойти поесть") }

                                    Button(
                                        onClick = { showClusteringMenu = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                                    ) { Text("Кластеризация — зоны еды") }

                                    Button(
                                        onClick = { showAcoMenu = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                                    ) { Text("Муравьиный алгоритм — коворкинг") }

                                    Button(
                                        onClick = {
                                            markerAccentByKey = emptyMap()
                                            algorithmsInfo = "ℹ️ Подсветка алгоритмов очищена."
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                                    ) { Text("Очистить подсветку") }

                                    if (algorithmsInfo != null) {
                                        Surface(
                                            color = Color(0xFF0B1220).copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = algorithmsInfo!!,
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
                                                onClick = { pm.clearGoodsRequest() },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(
                                                        0xFF4B5563
                                                    )
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
                                                            verticalArrangement = Arrangement.spacedBy(
                                                                3.dp
                                                            )
                                                        ) {
                                                            Text(
                                                                text = place.name,
                                                                color = Color.White,
                                                                fontSize = 13.sp
                                                            )
                                                            Text(
                                                                text = timing,
                                                                color = if (isPlaceOpenNow(place)) Color(
                                                                    0xFF86EFAC
                                                                ) else Color(0xFFFCA5A5),
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
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { pm.setSimpleMode() },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(
                                                    0xFF4B5563
                                                )
                                            )
                                        ) {
                                            Text("Обычный")
                                        }
                                        Button(
                                            onClick = {
                                                tempSelectedGoods.clear()
                                                tempSelectedGoods.addAll(pm.requiredGoods)
                                                showGoodsPicker = true
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(
                                                    0xFF2563EB
                                                )
                                            )
                                        ) {
                                            Text("По товарам")
                                        }
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    pm.buildPath()
                                                }
                                            },
                                            enabled = pm.hasTwoPoints(),
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(
                                                    0xFF2E7D32
                                                )
                                            )
                                        ) {
                                            Text("Запустить")
                                        }

                                        Button(
                                            onClick = { pm.reset() },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(
                                                    0xFFC62828
                                                )
                                            )
                                        ) {
                                            Text("Сбросить")
                                        }
                                    }

                                    if (currentMode == "goods") {
                                        Button(
                                            onClick = {
                                                val inferredFoodType = guessFoodTypeFromGoods(selectedGoods)
                                                dtFoodType = inferredFoodType

                                                val recommendation = tree?.classify(
                                                    Sample(
                                                        location = "campus",
                                                        budget = dtBudget,
                                                        timeAvailable = dtTime,
                                                        foodType = dtFoodType,
                                                        queueTolerance = dtQueue,
                                                        weather = dtWeather,
                                                        recommendedPlace = ""
                                                    )
                                                ) ?: "Unknown"

                                                val place = ps.places.firstOrNull { it.name == recommendation }
                                                if (place != null) {
                                                    selectedPlace = place
                                                } else {
                                                    Log.w("DecisionTree", "No place found for recommendation: $recommendation")
                                                }
                                            },
                                            enabled = tree != null,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF7C3AED)
                                            )
                                        ) {
                                            Text("Рекомендовать место (дерево)")
                                        }
                                    }
                                }
                            }
                        }

                        selectedPlace?.let { place ->
                            AlertDialog(
                                onDismissRequest = { selectedPlace = null },
                                containerColor = Color(0xFF0F172A),
                                text = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = place.name,
                                            color = Color.White,
                                            fontSize = 18.sp
                                        )
                                        Text(
                                            text = "Товары: ${place.menu.joinToString()}",
                                            color = Color(0xFFC7D2FE),
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Открытие: ${place.openingAt.format(timeFormatter)}",
                                            color = Color(0xFFBFDBFE),
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Закрытие: ${place.closingAt.format(timeFormatter)}",
                                            color = Color(0xFFFECACA),
                                            fontSize = 12.sp
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val built = pm.buildPathToPlace(place)
                                                if (built) selectedPlace = null
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(
                                                0xFF2563EB
                                            )
                                        )
                                    ) {
                                        Text("Маршрут")
                                    }
                                },
                                dismissButton = {
                                    Button(
                                        onClick = { selectedPlace = null },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(
                                                0xFF334155
                                            )
                                        )
                                    ) {
                                        Text("Закрыть")
                                    }
                                }
                            )
                        }
                    }

                    if (showGoodsPicker) {
                        AlertDialog(
                            onDismissRequest = { showGoodsPicker = false },
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
                                        onClick = { tempSelectedGoods.clear() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(
                                                0xFF334155
                                            )
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
                                                            if (good !in tempSelectedGoods) tempSelectedGoods.add(
                                                                good
                                                            )
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
                                    onClick = {
                                        if (tempSelectedGoods.isNotEmpty()) {
                                            pm.setGoodsRequest(tempSelectedGoods.toSet())
                                        }
                                        showGoodsPicker = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(
                                            0xFF2563EB
                                        )
                                    )
                                ) {
                                    Text("Применить")
                                }
                            },
                            dismissButton = {
                                Button(
                                    onClick = { showGoodsPicker = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(
                                            0xFF334155
                                        )
                                    )
                                ) {
                                    Text("Отмена")
                                }
                            }
                        )
                    }

                    if (showPathMenu) {
                        AlertDialog(
                            onDismissRequest = { showPathMenu = false },
                            containerColor = Color(0xFF0F172A),
                            title = { Text("Маршрут — поиск пути", color = Color.White) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Выберите алгоритм поиска маршрута", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = selectedAlgorithm == PathManager.SearchAlgorithm.Astar,
                                            onClick = { pm.setSearchAlgorithm(PathManager.SearchAlgorithm.Astar) },
                                            label = { Text("A*") }
                                        )
                                        FilterChip(
                                            selected = selectedAlgorithm == PathManager.SearchAlgorithm.GenericAlgorithm,
                                            onClick = { pm.setSearchAlgorithm(PathManager.SearchAlgorithm.GenericAlgorithm) },
                                            label = { Text("Генетический") }
                                        )
                                    }
                                    Text(
                                        "Поставьте точки (двойной тап) и нажмите «Запустить» в панели управления.",
                                        color = Color(0xFF9FB1C4),
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showPathMenu = false }) { Text("Закрыть") }
                            }
                        )
                    }

                    if (showClusteringMenu) {
                        var k by remember { mutableStateOf(3) }
                        AlertDialog(
                            onDismissRequest = { showClusteringMenu = false },
                            containerColor = Color(0xFF0F172A),
                            title = { Text("Кластеризация — зоны еды", color = Color.White) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("K-means. Подсветит магазины обводкой по кластерам.", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(selected = k == 2, onClick = { k = 2 }, label = { Text("k=2") })
                                        FilterChip(selected = k == 3, onClick = { k = 3 }, label = { Text("k=3") })
                                        FilterChip(selected = k == 4, onClick = { k = 4 }, label = { Text("k=4") })
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val shopMarkers = mm.markers.filter { it.type == MarkerType.SHOP }
                                            val palette = listOf(
                                                Color(0xFF22C55E),
                                                Color(0xFF3B82F6),
                                                Color(0xFFF97316),
                                                Color(0xFFE11D48),
                                                Color(0xFFA855F7)
                                            )
                                            val result = withContext(Dispatchers.Default) {
                                                ClusteringAlgorithm(shopMarkers, grid).performClusteringComparison(k = k)
                                            }
                                            val byMarker = result.aStarResult.clusters
                                                .flatMap { (clusterId, markers) ->
                                                    markers.map { m ->
                                                        val key = m.id?.toString() ?: m.name
                                                        key to palette[clusterId % palette.size]
                                                    }
                                                }
                                                .toMap()
                                            markerAccentByKey = byMarker
                                            algorithmsInfo =
                                                "✅ Кластеризация: k=$k, A* метрика.\n" +
                                                        "Кластеров: ${result.aStarResult.clusters.size}, " +
                                                        "разошлись с евклидовой: ${result.changedMarkers.size} точек."
                                            showClusteringMenu = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                                ) { Text("Запустить") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showClusteringMenu = false }) { Text("Отмена") }
                            }
                        )
                    }

                    if (showAcoMenu) {
                        var students by remember { mutableStateOf(20) }
                        AlertDialog(
                            onDismissRequest = { showAcoMenu = false },
                            containerColor = Color(0xFF0F172A),
                            title = { Text("Муравьиный алгоритм — коворкинг", color = Color.White) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Распределяет группу по локациям (пример бонусного варианта).", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(selected = students == 10, onClick = { students = 10 }, label = { Text("10") })
                                        FilterChip(selected = students == 20, onClick = { students = 20 }, label = { Text("20") })
                                        FilterChip(selected = students == 30, onClick = { students = 30 }, label = { Text("30") })
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val start = pm.startPoint ?: Point(grid.getWidth() / 2, grid.getHeight() / 2)
                                            val coworkingMarkers = listOf(
                                                MapMarker(id = -101, name = "Коворкинг A", position = Point(320, 755), type = MarkerType.BUILDING),
                                                MapMarker(id = -102, name = "Коворкинг B", position = Point(255, 890), type = MarkerType.BUILDING),
                                                MapMarker(id = -103, name = "Коворкинг C", position = Point(715, 1430), type = MarkerType.BUILDING)
                                            )

                                            coworkingMarkers.forEach { m ->
                                                val exists = mm.markers.any { it.id == m.id || it.name == m.name }
                                                if (!exists) mm.addMarker(m)
                                            }

                                            val locations = listOf(
                                                CoworkingLocation(coworkingMarkers[0], capacity = 10, comfort = 0.9),
                                                CoworkingLocation(coworkingMarkers[1], capacity = 8, comfort = 0.8),
                                                CoworkingLocation(coworkingMarkers[2], capacity = 12, comfort = 0.7)
                                            )
                                            val result = withContext(Dispatchers.Default) {
                                                AntColonyOptimization(
                                                    startPoint = start,
                                                    studentCount = students,
                                                    allLocations = locations,
                                                    grid = grid
                                                ).run()
                                            }

                                            val acoAccent = result.distribution.entries.associate { (loc, count) ->
                                                val key = loc.marker.id?.toString() ?: loc.marker.name
                                                key to (if (count > 0) Color(0xFFF59E0B) else Color(0xFF64748B))
                                            }
                                            markerAccentByKey = markerAccentByKey + acoAccent

                                            val summary = result.distribution.entries
                                                .sortedByDescending { it.value }
                                                .joinToString(separator = "\n") { (loc, count) ->
                                                    "• ${loc.marker.name}: $count/${loc.capacity}"
                                                }
                                            algorithmsInfo =
                                                "✅ ACO: студентов=$students\n" +
                                                        "Неразмещено: ${result.unassignedStudents}\n" +
                                                        summary
                                            showAcoMenu = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                                ) { Text("Запустить") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAcoMenu = false }) { Text("Отмена") }
                            }
                        )
                    }

                    if (showDecisionTreeMenu) {
                        LaunchedEffect(showDecisionTreeMenu) {
                            if (showDecisionTreeMenu && decisionTreeCsvText.isBlank()) {
                                decisionTreeCsvText = withContext(Dispatchers.IO) {
                                    assets.open("decision_tree_samples.csv").bufferedReader().use { it.readText() }
                                }
                            }
                        }

                        AlertDialog(
                            onDismissRequest = { showDecisionTreeMenu = false },
                            containerColor = Color(0xFF0F172A),
                            title = { Text("Дерево решений — куда пойти поесть", color = Color.White) },
                            text = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 420.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("Шаг 1: загрузить CSV", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { csvPicker.launch("text/*") },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                                        ) { Text("Выбрать файл") }
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    decisionTreeCsvText = withContext(Dispatchers.IO) {
                                                        assets.open("decision_tree_samples.csv").bufferedReader().use { it.readText() }
                                                    }
                                                    algorithmsInfo = "✅ Загружен CSV по умолчанию (из фото)"
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569))
                                        ) { Text("По умолчанию") }
                                    }

                                    Text("CSV текст:", color = Color(0xFF9FB1C4), fontSize = 11.sp)
                                    TextField(
                                        value = decisionTreeCsvText,
                                        onValueChange = { decisionTreeCsvText = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 140.dp, max = 220.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                    )

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val samples = CSVParser().parseCSVText(decisionTreeCsvText)
                                                if (samples.isEmpty()) {
                                                    algorithmsInfo = "❌ CSV пустой или неверный формат"
                                                    return@launch
                                                }
                                                val newTree = DecisionTree(root = DecisionTreeNode.Leaf("Unknown")).also {
                                                    it.buildTree(
                                                        samples = samples,
                                                        attributes = listOf(
                                                            "location",
                                                            "budget",
                                                            "time_available",
                                                            "food_type",
                                                            "queue_tolerance",
                                                            "weather"
                                                        )
                                                    )
                                                }
                                                decisionTree = newTree
                                                decisionTreeBuiltText = newTree.prettyPrint()
                                                decisionTreePathText = null
                                                algorithmsInfo = "✅ Дерево построено. Записей: ${samples.size}"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
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
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilterChip(selected = dtLocation == "main_building", onClick = { dtLocation = "main_building" }, label = { Text("main_building") })
                                            FilterChip(selected = dtLocation == "second_building", onClick = { dtLocation = "second_building" }, label = { Text("second_building") })
                                            FilterChip(selected = dtLocation == "campus_center", onClick = { dtLocation = "campus_center" }, label = { Text("campus_center") })
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilterChip(selected = dtBudget == "low", onClick = { dtBudget = "low" }, label = { Text("budget: low") })
                                            FilterChip(selected = dtBudget == "medium", onClick = { dtBudget = "medium" }, label = { Text("budget: medium") })
                                            FilterChip(selected = dtBudget == "high", onClick = { dtBudget = "high" }, label = { Text("budget: high") })
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilterChip(selected = dtTime == "very_short", onClick = { dtTime = "very_short" }, label = { Text("time: very_short") })
                                            FilterChip(selected = dtTime == "short", onClick = { dtTime = "short" }, label = { Text("time: short") })
                                            FilterChip(selected = dtTime == "medium", onClick = { dtTime = "medium" }, label = { Text("time: medium") })
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilterChip(selected = dtFoodTypeUi == "snack", onClick = { dtFoodTypeUi = "snack" }, label = { Text("snack") })
                                            FilterChip(selected = dtFoodTypeUi == "coffee", onClick = { dtFoodTypeUi = "coffee" }, label = { Text("coffee") })
                                            FilterChip(selected = dtFoodTypeUi == "full_meal", onClick = { dtFoodTypeUi = "full_meal" }, label = { Text("full_meal") })
                                            FilterChip(selected = dtFoodTypeUi == "pancakes", onClick = { dtFoodTypeUi = "pancakes" }, label = { Text("pancakes") })
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilterChip(selected = dtQueue == "low", onClick = { dtQueue = "low" }, label = { Text("queue: low") })
                                            FilterChip(selected = dtQueue == "medium", onClick = { dtQueue = "medium" }, label = { Text("queue: medium") })
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilterChip(selected = dtWeather == "good", onClick = { dtWeather = "good" }, label = { Text("weather: good") })
                                            FilterChip(selected = dtWeather == "bad", onClick = { dtWeather = "bad" }, label = { Text("weather: bad") })
                                        }

                                        Button(
                                            onClick = {
                                                val t = decisionTree ?: return@Button
                                                val r = t.classifyWithPath(
                                                    Sample(
                                                        location = dtLocation,
                                                        budget = dtBudget,
                                                        timeAvailable = dtTime,
                                                        foodType = dtFoodTypeUi,
                                                        queueTolerance = dtQueue,
                                                        weather = dtWeather,
                                                        recommendedPlace = ""
                                                    )
                                                )
                                                decisionTreePathText = buildString {
                                                    append("Результат: ").append(r.result).append("\n\nПуть по узлам:\n")
                                                    r.path.forEachIndexed { idx, step ->
                                                        append("${idx + 1}) ${step.attribute} = ${step.value} (ветки: ${step.availableBranches.joinToString()})\n")
                                                    }
                                                }
                                                algorithmsInfo = "✅ Дерево решений: ответ = ${r.result}"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
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
                            },
                            confirmButton = {
                                TextButton(onClick = { showDecisionTreeMenu = false }) { Text("Закрыть") }
                            }
                        )
                    }
                }
            }
    }

    private fun addAllGoods(placeStorage: PlaceStorage) {
        placeStorage.addPlace(
            Place(
                id = 1,
                name = "Ярче",
                location = Point(255, 260),
                menu = setOf("Фрукты", "Овощи", "Мясо", "Курица", "Рыба", "Алкоголь"),
                openingAt = LocalTime.of(7, 0),
                closingAt = LocalTime.of(23, 0)
            )
        )
        placeStorage.addPlace(
            Place(
                id = 2,
                name = "Пятёрочка",
                location = Point(715, 1430),
                menu = setOf("Фрукты", "Овощи", "Мясо", "Курица", "Рыба", "Алкоголь"),
                openingAt = LocalTime.of(0, 0),
                closingAt = LocalTime.of(0, 0)
            )
        )
        placeStorage.addPlace(
            Place(
                id = 3,
                name = "Абрикос",
                location = Point(5, 560),
                menu = setOf("Сладкое", "Закуски", "Алкоголь"),
                openingAt = LocalTime.of(8, 0),
                closingAt = LocalTime.of(23, 0)
            )
        )
        placeStorage.addPlace(
            Place(
                id = 4,
                name = "Пилад",
                location = Point(135, 435),
                menu = setOf("Сладкое", "Закуски", "Пельмени"),
                openingAt = LocalTime.of(0, 0),
                closingAt = LocalTime.of(0, 0)
            )
        )
        placeStorage.addPlace(
            Place(
                id = 5,
                name = "Ярче",
                location = Point(835, 540),
                menu = setOf("Кола", "Бутерброд", "Кофе", "Фрукты", "Овощи", "Мясо", "Курица", "Рыба", "Алкоголь"),
                openingAt = LocalTime.of(8, 0),
                closingAt = LocalTime.of(22, 0)
            )
        )
        placeStorage.addPlace(
            Place(
                id = 6,
                name = "Ярче",
                location = Point(900, 1155),
                menu = setOf("Кола", "Бутерброд", "Кофе", "Фрукты", "Овощи", "Мясо", "Курица", "Рыба", "Алкоголь"),
                openingAt = LocalTime.of(7, 0),
                closingAt = LocalTime.of(23, 0)
            )
        )
        placeStorage.addPlace(
            Place(
                id = 7,
                name = "Fix Price",
                location = Point(705, 1430),
                menu = setOf("Ручки", "Карандаши", "Хоз.товары"),
                openingAt = LocalTime.of(9, 0),
                closingAt = LocalTime.of(21, 0)
            )
        )
        placeStorage.addPlace(
            Place(
                id = 8,
                name = "Сибирские блины",
                location = Point(785, 1430),
                menu = setOf("Мясной блин", "Блин с ветчиной и сыром", "Сладкий блин"),
                openingAt = LocalTime.of(0, 0),
                closingAt = LocalTime.of(0, 0)
            )
        )
        placeStorage.addPlace(
            Place(
                id = 9,
                name = "Xo bakery",
                location = Point(255, 890),
                menu = setOf("Кофе"),
                openingAt = LocalTime.of(8, 0),
                closingAt = LocalTime.of(19, 0)
            )
        )
        placeStorage.addPlace(
            Place(
                id = 10,
                name = "Сибирские блины",
                location = Point(360, 765),
                menu = setOf("Мясной блин", "Блин с ветчиной и сыром", "Сладкий блин"),
                openingAt = LocalTime.of(9, 0),
                closingAt = LocalTime.of(20, 0)
            )
        )
        placeStorage.addPlace(
            Place(
                id = 11,
                name = "Столовая ЦК",
                location = Point(360, 765),
                menu = setOf("Первое блюдо", "Второе блюдо"),
                openingAt = LocalTime.of(8, 0),
                closingAt = LocalTime.of(18, 0)
            )
        )
        placeStorage.addPlace(
            Place(
                id = 12,
                name = "Столовая Минутка",
                location = Point(383, 780),
                menu = setOf("Первое блюдо", "Второе блюдо"),
                openingAt = LocalTime.of(8, 0),
                closingAt = LocalTime.of(18, 0)
            )
        )
        placeStorage.addPlace(
            Place(
                id = 13,
                name = "Starbooks",
                location = Point(320, 755),
                menu = setOf("Кофе"),
                openingAt = LocalTime.of(8, 0),
                closingAt = LocalTime.of(18, 0)
            )
        )
        placeStorage.addPlace(
            Place(
                id = 14,
                name = "Сыр-Бор",
                location = Point(315, 855),
                menu = setOf("Первое блюдо", "Второе блюдо"),
                openingAt = LocalTime.of(8, 0),
                closingAt = LocalTime.of(18, 0)
            )
        )
        placeStorage.addPlace(
            Place(
                id = 15,
                name = "Наш",
                location = Point(835, 760),
                menu = setOf("Фрукты", "Овощи", "Мясо", "Курица", "Рыба", "Алкоголь"),
                openingAt = LocalTime.of(0, 0),
                closingAt = LocalTime.of(0, 0)
            )
        )
        placeStorage.addPlace(
            Place(
                id = 16,
                name = "Укромное местечко",
                location = Point(485, 400),
                menu = setOf("Первое блюдо", "Второе блюдо"),
                openingAt = LocalTime.of(9, 0),
                closingAt = LocalTime.of(17, 0)
            )
        )
        placeStorage.addPlace(
            Place(
                id = 17,
                name = "Harat's pub",
                location = Point(304, 290),
                menu = setOf("Алкоголь"),
                openingAt = LocalTime.of(12, 0),
                closingAt = LocalTime.of(2, 0)
            )
        )

        Log.d("PLACES", "Все места: ${placeStorage.places}")
        Log.d("GOODS", "Все товары: ${placeStorage.goods}")
    }

    private fun isPlaceOpenNow(place: Place, now: LocalTime = LocalTime.now()): Boolean {
        return !now.isBefore(place.openingAt) && now.isBefore(place.closingAt)
    }

    private fun getPlaceTimingInfo(place: Place, now: LocalTime = LocalTime.now()): String {
        val openNow = isPlaceOpenNow(place, now)
        return if (openNow) {
            val minutes = minutesUntil(now, place.closingAt)
            "Открыт(а), закроется через ${formatMinutes(minutes)}"
        } else {
            val minutes = if (now.isBefore(place.openingAt)) {
                minutesUntil(now, place.openingAt)
            } else {
                minutesUntil(now, place.openingAt.plusHours(24))
            }
            "Закрыт(а), откроется через ${formatMinutes(minutes)}"
        }
    }

    private fun minutesUntil(from: LocalTime, to: LocalTime): Int {
        val fromMinutes = from.hour * 60 + from.minute
        val toMinutes = to.hour * 60 + to.minute
        var diff = toMinutes - fromMinutes
        if (diff < 0) diff += 24 * 60
        return diff
    }

    private fun formatMinutes(totalMinutes: Int): String {
        val minutes = totalMinutes.absoluteValue
        val hoursPart = minutes / 60
        val minutePart = minutes % 60
        return if (hoursPart > 0) {
            "${hoursPart}ч ${minutePart}м"
        } else {
            "${minutePart}м"
        }
    }

//    private fun addAllMarkers() {
//        markerManager.addMarker("Университет", 0.50f, 0.50f, MarkerType.UNIVERSITY)
//        markerManager.addMarker("Главный корпус", 0.35f, 0.45f, MarkerType.BUILDING)
//        markerManager.addMarker("Библиотека", 0.55f, 0.40f, MarkerType.BUILDING)
//        markerManager.addMarker("Спорткомплекс", 0.60f, 0.65f, MarkerType.BUILDING)
//        markerManager.addMarker("Столовая", 0.45f, 0.55f, MarkerType.BUILDING)
//        markerManager.addMarker("Парк", 0.20f, 0.70f, MarkerType.PARK)
//        markerManager.addMarker("Магазин", 0.42f, 0.58f, MarkerType.SHOP)
//        markerManager.addMarker("Кофейный автомат", 0.48f, 0.52f, MarkerType.VENDING)
//    }
}