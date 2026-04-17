package com.example.tsumaps.ui

import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsumaps.R
import com.example.tsumaps.domain.algorithms.AntColonyTsp
import com.example.tsumaps.domain.algorithms.ClusteringAlgorithm
import com.example.tsumaps.domain.algorithms.decisionTree.CSVParser
import com.example.tsumaps.domain.algorithms.decisionTree.DecisionTree
import com.example.tsumaps.domain.algorithms.decisionTree.DecisionTreeNode
import com.example.tsumaps.domain.debug.WalkableOverlay
import com.example.tsumaps.domain.map.MapGrid
import com.example.tsumaps.domain.map.MapMarker
import com.example.tsumaps.domain.map.MapMarkerManager
import com.example.tsumaps.domain.map.MarkerType
import com.example.tsumaps.domain.map.MapWithMarkers
import com.example.tsumaps.domain.models.Place
import com.example.tsumaps.domain.models.PlaceStorage
import com.example.tsumaps.domain.models.Sample
import com.example.tsumaps.domain.path.PathManager
import com.example.tsumaps.domain.path.PathPointsOverlay
import com.example.tsumaps.ui.components.TsuHeader
import com.example.tsumaps.ui.dialogs.AntColonyDialog
import com.example.tsumaps.ui.dialogs.ClusteringDialog
import com.example.tsumaps.ui.dialogs.DecisionTreeMenuDialog
import com.example.tsumaps.ui.dialogs.GoodsPickerDialog
import com.example.tsumaps.ui.dialogs.PathBuildOverlayDialog
import com.example.tsumaps.ui.dialogs.PlaceDetailDialog
import com.example.tsumaps.ui.panel.AlgorithmExpandedContent
import com.example.tsumaps.ui.panel.BottomControlSurface
import com.example.tsumaps.ui.theme.TsuBrand
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter

@Composable
fun TsuMapsApp() {
    val context = LocalActivity.current ?: LocalContext.current
    var initError by remember { mutableStateOf<String?>(null) }
    var uiReady by remember { mutableStateOf(false) }

    var mapGrid by remember { mutableStateOf<MapGrid?>(null) }
    var markerManager by remember { mutableStateOf<MapMarkerManager?>(null) }
    var pathManager by remember { mutableStateOf<PathManager?>(null) }
    var placeStorage by remember { mutableStateOf<PlaceStorage?>(null) }

    var decisionTree by remember { mutableStateOf<DecisionTree?>(null) }

    LaunchedEffect(Unit) {
        try {
            val grid = MapGrid(context, pathScale = MapGrid.DEFAULT_PATH_SCALE)
            withContext(Dispatchers.Default) {
                grid.buildFromImage(R.drawable.map_for_grid)
            }

            val pm = PathManager(grid)
            val ps = PlaceStorage().also { storage ->
                populateCampusPlaces(storage)
                populateCampusAttractions(storage)
                pm.updatePlaces(storage.places.associateBy { it.id })
            }

            val mm = MapMarkerManager(grid).also {
                it.clearMarkers()
                ps.places.forEach { place -> it.addPlaceMarker(place) }
                ps.attractions.forEach { a -> it.addAttractionMarker(a) }
            }

            val samples = CSVParser().parseAsset(context, "decision_tree_samples.csv")
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
            Log.e("TsuMapsApp", "Init error: ${e.message}", e)
            initError = e.message ?: "Unknown error"
        }
    }

    if (!uiReady) {
        Surface(modifier = Modifier.fillMaxSize(), color = TsuBrand.Header) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = initError?.let { "Ошибка инициализации: $it" } ?: "Загрузка карты…",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
        return
    }

    val grid = mapGrid!!
    val mm = markerManager!!
    val pm = pathManager!!
    val ps = placeStorage!!

    val startPoint = pm.startPoint
    val endPoint = pm.endPoint
    val path = pm.path
    val message = pm.message
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
    var dtQueue by remember { mutableStateOf("medium") }
    var dtWeather by remember { mutableStateOf("any") }
    val scope = rememberCoroutineScope()
    var showPathBuildOverlay by remember { mutableStateOf(false) }
    var pathBuildJob by remember { mutableStateOf<Job?>(null) }
    var pathBuildAlgorithmTitle by remember { mutableStateOf("") }
    var pathBuildFooterOverride by remember { mutableStateOf<String?>(null) }
    var pathBuildHideSecondaryLine by remember { mutableStateOf(false) }

    var algorithmsInfo by remember { mutableStateOf<String?>(null) }
    var markerAccentByKey by remember { mutableStateOf<Map<String, Color>>(emptyMap()) }

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
                            context.contentResolver.openInputStream(uri)
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

    Box(modifier = Modifier.fillMaxSize()) {

        MapWithMarkers(
            mapImageRes = R.drawable.map,
            mapGrid = grid,
            markerManager = mm,
            onMapDoubleTap = { point ->
                Log.d("TsuMapsApp", "Tap: $point")
                selectedPlace = null
                pm.onDoubleTap(point)
            },
            onMarkerClick = { marker ->
                Log.d("Marker", "Clicked: ${marker.name}")
                selectedPlace = marker.place
            },
            onMapTransform = {
                isBottomPanelExpanded = false
            },
            markerAccentColor = { marker ->
                val key = marker.id?.toString()
                    ?: "${marker.name}@${marker.position.x},${marker.position.y}"
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            TsuHeader(
                onSettingsClick = { showSettings = !showSettings }
            )
            if (showSettings) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, end = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        modifier = Modifier.widthIn(max = 320.dp),
                        color = TsuBrand.PanelSurface,
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
            }
        }

        BottomControlSurface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .fillMaxWidth(0.94f)
                .widthIn(max = 420.dp),
            isBottomPanelExpanded = isBottomPanelExpanded,
            onToggleBottomPanel = { isBottomPanelExpanded = !isBottomPanelExpanded },
            onSwipeExpand = { isBottomPanelExpanded = true },
            onSwipeCollapse = { isBottomPanelExpanded = false }
        ) {
            AlgorithmExpandedContent(
                message = message,
                currentMode = currentMode,
                selectedGoods = selectedGoods,
                algorithmsInfo = algorithmsInfo,
                scope = scope,
                pm = pm,
                tempSelectedGoods = tempSelectedGoods,
                relevantGoodsPlaces = relevantGoodsPlaces,
                onShowGoodsPicker = { showGoodsPicker = true },
                onOpenDecisionTree = { showDecisionTreeMenu = true },
                onOpenClustering = { showClusteringMenu = true },
                onOpenAco = { showAcoMenu = true }
            )
        }

        selectedPlace?.let { place ->
            PlaceDetailDialog(
                place = place,
                timeFormatter = timeFormatter,
                onDismiss = { selectedPlace = null },
                onBuildRoute = {
                    pathBuildJob?.cancel()
                    pathBuildAlgorithmTitle = "A* — маршрут до «${place.name}»"
                    pathBuildFooterOverride = null
                    pathBuildHideSecondaryLine = false
                    showPathBuildOverlay = true
                    pathBuildJob = scope.launch {
                        try {
                            val built = pm.buildPathToPlace(place)
                            if (built) selectedPlace = null
                        } catch (e: CancellationException) {
                            pm.notifyPathBuildCancelled()
                            throw e
                        } finally {
                            showPathBuildOverlay = false
                            pathBuildJob = null
                        }
                    }
                }
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 72.dp),
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        pathBuildJob?.cancel()
                        pathBuildAlgorithmTitle = when (currentMode) {
                            "simple" -> "A* — кратчайший путь между двумя точками"
                            "goods" -> when (pm.selectedAlgorithm) {
                                PathManager.SearchAlgorithm.Astar ->
                                    "Жадный выбор магазинов (A* по карте)"
                                PathManager.SearchAlgorithm.GenericAlgorithm ->
                                    "Генетический алгоритм — маршрут по товарам"
                            }
                            else -> "Построение маршрута"
                        }
                        pathBuildFooterOverride = null
                        pathBuildHideSecondaryLine =
                            currentMode == "goods" &&
                                pm.selectedAlgorithm == PathManager.SearchAlgorithm.GenericAlgorithm
                        showPathBuildOverlay = true
                        pathBuildJob = scope.launch {
                            try {
                                pm.buildPath()
                            } catch (e: CancellationException) {
                                pm.notifyPathBuildCancelled()
                                throw e
                            } finally {
                                showPathBuildOverlay = false
                                pathBuildJob = null
                            }
                        }
                    },
                    enabled = pm.hasTwoPoints(),
                    modifier = Modifier.width(54.dp),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                        disabledContainerColor = Color(0xFF1B5E20).copy(alpha = 0.45f)
                    )
                ) {
                    Text("\u25B6", color = Color.White, fontSize = 15.sp)
                }
                Button(
                    onClick = {
                        markerAccentByKey = emptyMap()
                        scope.launch { pm.reset() }
                    },
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .width(54.dp),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC62828)
                    )
                ) {
                    Text("\u21BA", color = Color.White, fontSize = 17.sp)
                }
            }
        }

        if (showGoodsPicker) {
            GoodsPickerDialog(
                allGoods = allGoods,
                tempSelectedGoods = tempSelectedGoods,
                onDismiss = { showGoodsPicker = false },
                onApply = {
                    scope.launch {
                        if (tempSelectedGoods.isNotEmpty()) {
                            pm.setGoodsRequest(tempSelectedGoods.toSet())
                        }
                        showGoodsPicker = false
                    }
                },
                onClearSelection = { tempSelectedGoods.clear() }
            )
        }

        if (showClusteringMenu) {
            ClusteringDialog(
                onDismiss = { showClusteringMenu = false },
                onRun = { k ->
                    scope.launch {
                        val shopMarkers = mm.markers.filter { it.type == MarkerType.SHOP }
                        if (shopMarkers.isEmpty()) {
                            algorithmsInfo =
                                "Выберите заведения на карте или добавьте маркеры типа «магазин» — точки для кластеризации."
                            showClusteringMenu = false
                            return@launch
                        }
                        pathBuildAlgorithmTitle = "K-means — кластеризация заведений (k=$k)"
                        pathBuildFooterOverride =
                            "Сравнение евклидовой метрики и A* по карте…"
                        pathBuildHideSecondaryLine = false
                        showPathBuildOverlay = true
                        val palette = listOf(
                            Color(0xFF22C55E),
                            Color(0xFF3B82F6),
                            Color(0xFFF97316),
                            Color(0xFFE11D48),
                            Color(0xFFA855F7)
                        )
                        val result = try {
                            withContext(Dispatchers.Default) {
                                ClusteringAlgorithm(shopMarkers, grid).performClusteringComparison(k = k)
                            }
                        } finally {
                            showPathBuildOverlay = false
                        }
                        val markerKey: (MapMarker) -> String = { mk ->
                            mk.id?.toString() ?: "${mk.name}@${mk.position.x},${mk.position.y}"
                        }
                        val byMarker = result.aStarResult.clusters
                            .flatMap { (clusterId, ms) ->
                                ms.map { m ->
                                    markerKey(m) to palette[clusterId.coerceIn(0, palette.lastIndex)]
                                }
                            }
                            .toMap()
                            .toMutableMap()
                        markerAccentByKey = byMarker
                        val shiftSample = result.changedMarkers.entries
                            .take(5)
                            .joinToString("\n") { (mk, p) ->
                                "· ${mk.name}: евклид кластер ${p.first} → A* кластер ${p.second}"
                            }
                        algorithmsInfo = buildString {
                            append("✅ Зоны еды: k=$k, K-means.\n")
                            append("Кластеров: ${result.aStarResult.clusters.count { it.value.isNotEmpty() }}")
                            append(", сместились между метриками: ${result.changedMarkers.size}.\n")
                            if (shiftSample.isNotEmpty()) append(shiftSample).append("\n")
                            if (result.changedMarkers.size > 5) append("…")
                        }
                        showClusteringMenu = false
                    }
                }
            )
        }

        if (showAcoMenu) {
            AntColonyDialog(
                attractions = ps.attractions,
                onDismiss = { showAcoMenu = false },
                onRun = { chosen ->
                    scope.launch {
                        if (chosen.isEmpty()) {
                            algorithmsInfo = "❌ Отметьте хотя бы одну достопримечательность."
                            return@launch
                        }
                        val start = pm.startPoint
                        if (start == null) {
                            algorithmsInfo =
                                "❌ Укажите стартовую точку (двойной тап на карте в режиме простого маршрута или поиска по товарам)."
                            return@launch
                        }
                        pathBuildAlgorithmTitle = "Муравьиный алгоритм (TSP, ACO)"
                        pathBuildFooterOverride = "Построение тура по выбранным вершинам…"
                        pathBuildHideSecondaryLine = false
                        showPathBuildOverlay = true
                        val result = try {
                            withContext(Dispatchers.Default) {
                                AntColonyTsp(
                                    grid = grid,
                                    userStart = start,
                                    attractions = chosen
                                ).run()
                            }
                        } finally {
                            showPathBuildOverlay = false
                        }

                        if (result.polyline.isEmpty() || result.orderedAttractions.isEmpty()) {
                            algorithmsInfo = "❌ Не удалось построить замкнутый тур (проверьте проходимость и координаты)."
                            showAcoMenu = false
                            return@launch
                        }

                        pm.setExternalTourPath(result.polyline, result.summaryRu())

                        val acoAccent = result.orderedAttractions.associate { att ->
                            val key = att.marker.id?.toString()
                                ?: "${att.marker.name}@${att.marker.position.x},${att.marker.position.y}"
                            key to Color(0xFFF59E0B)
                        }
                        markerAccentByKey = markerAccentByKey + acoAccent

                        algorithmsInfo = "✅ " + result.summaryRu()
                        showAcoMenu = false
                    }
                }
            )
        }

        if (showDecisionTreeMenu) {
            DecisionTreeMenuDialog(
                csvText = decisionTreeCsvText,
                onCsvTextChange = { decisionTreeCsvText = it },
                onPickCsvFile = { csvPicker.launch("text/*") },
                onDismiss = { showDecisionTreeMenu = false },
                decisionTree = decisionTree,
                decisionTreeBuiltText = decisionTreeBuiltText,
                decisionTreePathText = decisionTreePathText,
                dtLocation = dtLocation,
                onDtLocationChange = { dtLocation = it },
                dtBudget = dtBudget,
                onDtBudgetChange = { dtBudget = it },
                dtTime = dtTime,
                onDtTimeChange = { dtTime = it },
                dtFoodTypeUi = dtFoodTypeUi,
                onDtFoodTypeChange = { dtFoodTypeUi = it },
                dtQueue = dtQueue,
                onDtQueueChange = { dtQueue = it },
                dtWeather = dtWeather,
                onDtWeatherChange = { dtWeather = it },
                onInfo = { algorithmsInfo = it },
                onBuildTree = suspend {
                    val samples = withContext(Dispatchers.Default) {
                        CSVParser().parseCSVText(decisionTreeCsvText)
                    }
                    if (samples.isEmpty()) {
                        algorithmsInfo = "❌ CSV пустой или неверный формат"
                    } else {
                        val newTree = withContext(Dispatchers.Default) {
                            DecisionTree(root = DecisionTreeNode.Leaf("Unknown")).also {
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
                        }
                        decisionTree = newTree
                        decisionTreeBuiltText = withContext(Dispatchers.Default) {
                            newTree.prettyPrint()
                        }
                        decisionTreePathText = null
                        algorithmsInfo = "✅ Дерево построено. Записей: ${samples.size}"
                    }
                },
                onClassify = suspend {
                    val t = decisionTree
                    if (t != null) {
                        val r = withContext(Dispatchers.Default) {
                            t.classifyWithPath(
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
                        }
                        decisionTreePathText = buildString {
                            append("Результат: ").append(r.result).append("\n\nПуть по узлам:\n")
                            r.path.forEachIndexed { idx, step ->
                                append("${idx + 1}) ${step.attribute} = ${step.value} (ветки: ${step.availableBranches.joinToString()})\n")
                            }
                        }
                        algorithmsInfo = "✅ Дерево решений: ответ = ${r.result}"
                    }
                }
            )
        }

        if (showPathBuildOverlay) {
            PathBuildOverlayDialog(
                title = pathBuildAlgorithmTitle,
                footerLine = pathBuildFooterOverride ?: message,
                hideSecondaryLine = pathBuildHideSecondaryLine,
                onDismissRequest = { pathBuildJob?.cancel() }
            )
        }
    }
}
