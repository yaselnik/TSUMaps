package com.example.tsumaps

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.tsumaps.domain.models.Point

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
            addAllGoods();

            setContent {
                val startPoint = pathManager.startPoint
                val endPoint = pathManager.endPoint
                val path = pathManager.path
                val message = pathManager.message

                var showWalkable by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {

                    MapWithMarkers(
                        mapImageRes = R.drawable.map,
                        mapGrid = mapGrid,
                        markerManager = markerManager,
                        onMapDoubleTap = { point ->
                            Log.d("MainActivity", "Tap: $point")
                            pathManager.onDoubleTap(point)
                        },
                        onMarkerClick = { marker ->
                            Log.d("Marker", "Clicked: ${marker.name}")
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

                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = message,
                                color = Color.White,
                                fontSize = 16.sp
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Checkbox(
                                    checked = showWalkable,
                                    onCheckedChange = { showWalkable = it }
                                )
                                Text(
                                    text = "Проходимость",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { pathManager.buildPath() },
                                enabled = pathManager.hasTwoPoints()
                            ) {
                                Text("Запустить")
                            }

                            Button(onClick = { pathManager.reset() }) {
                                Text("Сбросить")
                            }
                        }
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
                id = 123,
                name = "Ярче",
                location = Point(500, 500),
                menu = setOf("apple", "banana", "orange"),
                openingAt = LocalTime.of(9, 0),
                closingAt = LocalTime.of(21, 0)
            )
        )
        Log.d("PLACES", "Все места: ${placeStorage.places}")
        Log.d("GOODS", "Все товары: ${placeStorage.goods}")
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