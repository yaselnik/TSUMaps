package com.example.tsumaps

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsumaps.domain.debug.WalkableOverlay
import com.example.tsumaps.domain.map.MapGrid
import com.example.tsumaps.domain.map.MapMarkerManager
import com.example.tsumaps.domain.map.MapWithMarkers
import com.example.tsumaps.domain.path.PathManager
import com.example.tsumaps.domain.path.PathPointsOverlay
import com.example.tsumaps.domain.models.Place
import com.example.tsumaps.domain.models.PlaceStorage
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.example.tsumaps.domain.models.Point
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity() {

    private lateinit var mapGrid: MapGrid
    private lateinit var markerManager: MapMarkerManager
    private lateinit var pathManager: PathManager
    private lateinit var placeStorage: PlaceStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            mapGrid = MapGrid(this)
            mapGrid.buildFromImage(R.drawable.map_for_grid)

            markerManager = MapMarkerManager(mapGrid)
            pathManager = PathManager(mapGrid)
            placeStorage = PlaceStorage()

//            addAllMarkers()
            placeStorage = PlaceStorage()
            addAllGoods()
            pathManager.updatePlaces(placeStorage.places.associateBy { it.id })
            addPlaceMarkers()

            setContent {
                val startPoint = pathManager.startPoint
                val endPoint = pathManager.endPoint
                val path = pathManager.path
                val message = pathManager.message
                val selectedAlgorithm = pathManager.selectedAlgorithm
                val currentMode = pathManager.mode
                val selectedGoods = pathManager.requiredGoods

                var showWalkable by remember { mutableStateOf(false) }
                var showSettings by remember { mutableStateOf(false) }
                var isBottomPanelExpanded by remember { mutableStateOf(false) }
                var selectedPlace by remember { mutableStateOf<Place?>(null) }
                var showGoodsPicker by remember { mutableStateOf(false) }
                val tempSelectedGoods = remember { mutableStateListOf<String>() }
                val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
                val allGoods = remember(placeStorage.goods) { placeStorage.goods.toList().sorted() }
                val relevantGoodsPlaces = remember(selectedGoods, placeStorage.places) {
                    if (selectedGoods.isEmpty()) {
                        emptyList()
                    } else {
                        placeStorage.places.filter { place ->
                            place.menu.any { it in selectedGoods }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {

                    MapWithMarkers(
                        mapImageRes = R.drawable.map,
                        mapGrid = mapGrid,
                        markerManager = markerManager,
                        onMapDoubleTap = { point ->
                            Log.d("MainActivity", "Tap: $point")
                            selectedPlace = null
                            pathManager.onDoubleTap(point)
                        },
                        onMarkerClick = { marker ->
                            Log.d("Marker", "Clicked: ${marker.name}")
                            selectedPlace = marker.place
                        },
                        onMapTransform = {
                            isBottomPanelExpanded = false
                        }
                    ) { zoom, pan, imageActualSize, containerSize ->

                        WalkableOverlay(
                            mapGrid = mapGrid,
                            zoom = zoom,
                            pan = pan,
                            imageActualSize = imageActualSize,
                            enabled = showWalkable
                        )

                        PathPointsOverlay(
                            startPoint = startPoint,
                            endPoint = endPoint,
                            path = path,
                            mapGrid = mapGrid,
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
                                Text(
                                    text = "Алгоритм поиска",
                                    color = Color(0xFFB0BEC5),
                                    fontSize = 12.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = selectedAlgorithm == PathManager.SearchAlgorithm.Astar,
                                        onClick = { pathManager.setSearchAlgorithm(PathManager.SearchAlgorithm.Astar) },
                                        label = { Text("Astar") }
                                    )
                                    FilterChip(
                                        selected = selectedAlgorithm == PathManager.SearchAlgorithm.GenericAlgorithm,
                                        onClick = { pathManager.setSearchAlgorithm(PathManager.SearchAlgorithm.GenericAlgorithm) },
                                        label = { Text("GenericAlgorithm") }
                                    )
                                }
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
                                Text(
                                    text = "Зум: pinch или кнопки справа",
                                    color = Color(0xFF9FB1C4),
                                    fontSize = 11.sp
                                )
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
                                                onClick = { pathManager.clearGoodsRequest() },
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
                                            onClick = { pathManager.setSimpleMode() },
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
                                                tempSelectedGoods.addAll(pathManager.requiredGoods)
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
                                            onClick = { pathManager.buildPath() },
                                            enabled = pathManager.hasTwoPoints(),
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
                                            onClick = { pathManager.reset() },
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
                                            val built = pathManager.buildPathToPlace(place)
                                            if (built) selectedPlace = null
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
                                            pathManager.setGoodsRequest(tempSelectedGoods.toSet())
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
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error: ${e.message}", e)
        }
    }

    private fun addAllGoods() {
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

    private fun addPlaceMarkers() {
        markerManager.clearMarkers()
        placeStorage.places.forEach { place ->
            markerManager.addPlaceMarker(place)
        }
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