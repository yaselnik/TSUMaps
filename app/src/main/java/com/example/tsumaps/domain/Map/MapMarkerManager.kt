package com.example.tsumaps.domain.map

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsumaps.domain.models.Place
import com.example.tsumaps.domain.models.Point
import com.example.tsumaps.R
import kotlin.math.roundToInt

data class MapMarker(
    val id: Int? = null,
    val name: String,
    val position: Point,
    val type: MarkerType,
    val place: Place? = null
)

enum class MarkerType {
    BUILDING,
    PARK,
    ROAD,
    UNIVERSITY,
    VENDING,
    SHOP
}

class MapMarkerManager(
    private val mapGrid: MapGrid
) {
    private val _markers = mutableStateListOf<MapMarker>()
    val markers: List<MapMarker> = _markers

    fun addMarker(marker: MapMarker) {
        _markers.add(marker)
    }

    fun addMarker(name: String, point: Point, type: MarkerType) {
        addMarker(
            MapMarker(
                name = name,
                position = point,
                type = type
            )
        )
    }

    fun addPlaceMarker(place: Place) {
        addMarker(
            MapMarker(
                id = place.id,
                name = place.name,
                position = place.location,
                type = MarkerType.SHOP,
                place = place
            )
        )
    }

    fun addMarker(name: String, xPercent: Float, yPercent: Float, type: MarkerType) {
        val pixelPoint = mapGrid.percentToPixel(xPercent, yPercent)
        addMarker(
            MapMarker(
                name = name,
                position = pixelPoint,
                type = type
            )
        )
    }

    fun removeMarker(name: String) {
        _markers.removeAll { it.name == name }
    }

    fun clearMarkers() {
        _markers.clear()
    }
}

@Composable
fun MapWithMarkers(
    mapImageRes: Int,
    mapGrid: MapGrid,
    markerManager: MapMarkerManager,
    onMapDoubleTap: (Point) -> Unit = {},
    onMarkerClick: (MapMarker) -> Unit = {},
    onMapTransform: () -> Unit = {},
    content: @Composable (zoom: Float, offset: Offset, imageActualSize: IntSize, containerSize: IntSize) -> Unit = { _, _, _, _ -> }
) {
    var zoom by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageActualSize by remember { mutableStateOf(IntSize.Zero) }
    val minZoom = 1f
    val maxZoom = 5f

    val mapWidth = mapGrid.getWidth()
    val mapHeight = mapGrid.getHeight()

    fun clampOffset(newOffset: Offset, currentZoom: Float): Offset {
        if (containerSize.width == 0 || containerSize.height == 0) return newOffset

        val scaledWidth = imageActualSize.width * currentZoom
        val scaledHeight = imageActualSize.height * currentZoom

        val minX = -(scaledWidth - containerSize.width)
        val maxX = 0f

        val minY = -(scaledHeight - containerSize.height)
        val maxY = 0f

        val clampedX = when {
            scaledWidth <= containerSize.width -> 0f
            newOffset.x < minX -> minX
            newOffset.x > maxX -> maxX
            else -> newOffset.x
        }

        val clampedY = when {
            scaledHeight <= containerSize.height -> 0f
            newOffset.y < minY -> minY
            newOffset.y > maxY -> maxY
            else -> newOffset.y
        }

        Log.d("MapDebug", "Zoom: $currentZoom, Scaled: ${scaledWidth}x${scaledHeight}")
        Log.d("MapDebug", "Container: ${containerSize.width}x${containerSize.height}")
        Log.d("MapDebug", "Min/Max X: $minX to $maxX, Requested: ${newOffset.x}, Clamped: $clampedX")
        Log.d("MapDebug", "Min/Max Y: $minY to $maxY, Requested: ${newOffset.y}, Clamped: $clampedY")

        return Offset(clampedX, clampedY)
    }

    fun screenToMap(screenX: Float, screenY: Float): Point? {
        val relativeX = (screenX - offset.x) / zoom
        val relativeY = (screenY - offset.y) / zoom

        if (relativeX < 0 || relativeX > imageActualSize.width ||
            relativeY < 0 || relativeY > imageActualSize.height) {
            return null
        }

        val mapX = ((relativeX / imageActualSize.width) * mapWidth).toInt().coerceIn(0, mapWidth - 1)
        val mapY = ((relativeY / imageActualSize.height) * mapHeight).toInt().coerceIn(0, mapHeight - 1)

        return Point(mapX, mapY)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        screenToMap(tapOffset.x, tapOffset.y)?.let { mapPoint ->
                            onMapDoubleTap(mapPoint)
                            Log.d("Map", "Double tap at $mapPoint")
                        }
                    }
                )
            }
            .pointerInput(containerSize, imageActualSize) {
                detectTransformGestures { centroid, pan, zoomChange, _ ->
                    if (pan != Offset.Zero || zoomChange != 1f) {
                        onMapTransform()
                    }

                    val oldZoom = zoom
                    val newZoom = (zoom * zoomChange).coerceIn(minZoom, maxZoom)

                    val focusX = (centroid.x - offset.x) / oldZoom
                    val focusY = (centroid.y - offset.y) / oldZoom

                    var newOffsetX = centroid.x - focusX * newZoom
                    var newOffsetY = centroid.y - focusY * newZoom

                    newOffsetX += pan.x
                    newOffsetY += pan.y

                    zoom = newZoom
                    offset = clampOffset(Offset(newOffsetX, newOffsetY), zoom)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = zoom,
                    scaleY = zoom,
                    translationX = offset.x,
                    translationY = offset.y,
                    transformOrigin = TransformOrigin(0f, 0f)
                )
        ) {
            Image(
                painter = painterResource(id = mapImageRes),
                contentDescription = "Карта",
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        imageActualSize = it
                        offset = clampOffset(offset, zoom)
                        Log.d("MapDebug", "Image size: ${it.width}x${it.height}")
                    },
                contentScale = ContentScale.FillBounds
            )

            markerManager.markers.forEach { marker ->
                val markerX = (marker.position.x.toFloat() / mapWidth) * imageActualSize.width
                val markerY = (marker.position.y.toFloat() / mapHeight) * imageActualSize.height

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (markerX - 15).roundToInt(),
                                y = (markerY - 15).roundToInt()
                            )
                        }
                        .size(30.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .clickable {
                                onMarkerClick(marker)
                            },
                        color = if (marker.type == MarkerType.SHOP) {
                            Color.White.copy(alpha = 0.42f)
                        } else {
                            getMarkerColor(marker.type)
                        },
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (marker.type == MarkerType.SHOP) {
                                Image(
                                    painter = painterResource(id = R.drawable.shop_icon),
                                    contentDescription = "Иконка магазина",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(3.dp),
                                    alpha = 1f,
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text(
                                    text = getMarkerIcon(marker.type),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        content(zoom, offset, imageActualSize, containerSize)

        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(12.dp),
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        val newZoom = (zoom + 0.25f).coerceIn(minZoom, maxZoom)
                        zoom = newZoom
                        offset = clampOffset(offset, zoom)
                    },
                    modifier = Modifier.width(54.dp)
                ) {
                    Text("+")
                }
                Button(
                    onClick = {
                        val newZoom = (zoom - 0.25f).coerceIn(minZoom, maxZoom)
                        zoom = newZoom
                        offset = clampOffset(offset, zoom)
                    },
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .width(54.dp)
                ) {
                    Text("-")
                }
                Text(
                    text = "${(zoom * 100).roundToInt()}%",
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

private fun getMarkerColor(type: MarkerType): Color {
    return when (type) {
        MarkerType.BUILDING -> Color(0xFF2196F3)
        MarkerType.PARK -> Color(0xFF4CAF50)
        MarkerType.ROAD -> Color(0xFF9E9E9E)
        MarkerType.UNIVERSITY -> Color(0xFF9C27B0)
        MarkerType.VENDING -> Color(0xFFFF9800)
        MarkerType.SHOP -> Color(0xFFF44336)
    }
}

private fun getMarkerIcon(type: MarkerType): String {
    return when (type) {
        MarkerType.BUILDING -> "🏛️"
        MarkerType.PARK -> "🌳"
        MarkerType.ROAD -> "🛣️"
        MarkerType.UNIVERSITY -> "🎓"
        MarkerType.VENDING -> "☕"
        MarkerType.SHOP -> "🛒"
    }
}