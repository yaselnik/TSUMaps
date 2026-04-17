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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsumaps.domain.models.Attraction
import com.example.tsumaps.domain.models.Place
import com.example.tsumaps.domain.models.Point
import com.example.tsumaps.R
import com.example.tsumaps.ui.theme.tsuHeaderButtonColors
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity

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
    SHOP,
    ATTRACTION
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

    fun addAttractionMarker(attraction: Attraction) {
        addMarker(attraction.marker)
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
    markerAccentColor: (MapMarker) -> Color? = { null },
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

    data class MarkerCluster(
        val markers: List<MapMarker>,
        val center: Offset
    )

    fun toImageOffset(marker: MapMarker): Offset {
        val x = (marker.position.x.toFloat() / mapWidth) * imageActualSize.width
        val y = (marker.position.y.toFloat() / mapHeight) * imageActualSize.height
        return Offset(x, y)
    }

    fun clusterMarkers(markers: List<MapMarker>, zoomLevel: Float): List<MarkerCluster> {
        if (imageActualSize.width == 0 || imageActualSize.height == 0) return emptyList()

        val clusterRadiusPx = clusterRadiusForZoom(zoomLevel, maxZoom)
        val remaining = markers.toMutableList()
        val clusters = mutableListOf<MarkerCluster>()

        while (remaining.isNotEmpty()) {
            val seed = remaining.removeAt(0)
            val seedPos = toImageOffset(seed)

            val grouped = mutableListOf(seed)
            var i = 0
            while (i < remaining.size) {
                val m = remaining[i]
                val p = toImageOffset(m)
                val d = hypot((p.x - seedPos.x).toDouble(), (p.y - seedPos.y).toDouble()).toFloat()
                if (d <= clusterRadiusPx) {
                    grouped.add(m)
                    remaining.removeAt(i)
                } else {
                    i++
                }
            }

            val center = if (grouped.size == 1) {
                seedPos
            } else {
                val xs = grouped.map { toImageOffset(it).x }
                val ys = grouped.map { toImageOffset(it).y }
                Offset(xs.average().toFloat(), ys.average().toFloat())
            }

            clusters.add(MarkerCluster(grouped, center))
        }

        return clusters
    }

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

            val clusters = remember(markerManager.markers, zoom, imageActualSize) {
                clusterMarkers(markerManager.markers, zoom)
            }

            val density = LocalDensity.current
            fun dpToPx(dp: Float): Float = with(density) { dp.dp.toPx() }

            clusters.forEach { cluster ->
                val center = cluster.center
                val isCluster = cluster.markers.size > 1

                val layoutSizeDp = if (isCluster) {
                    clusterBubbleLayoutDp(cluster.markers.size)
                } else {
                    SINGLE_MARKER_LAYOUT_DP
                }
                val pinVisualScale = mapPinScreenScale(zoom, maxZoom)
                val sizePx = dpToPx(layoutSizeDp)
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (center.x - sizePx / 2f).roundToInt(),
                                y = (center.y - sizePx / 2f).roundToInt()
                            )
                        }
                        .size(layoutSizeDp.dp)
                        .graphicsLayer {
                            scaleX = pinVisualScale
                            scaleY = pinVisualScale
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        }
                ) {
                    if (isCluster) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .clickable {
                                    val newZoom = (zoom + 1.0f).coerceIn(minZoom, maxZoom)
                                    val targetOffsetX = (containerSize.width / 2f) - center.x * newZoom
                                    val targetOffsetY = (containerSize.height / 2f) - center.y * newZoom
                                    zoom = newZoom
                                    offset = clampOffset(Offset(targetOffsetX, targetOffsetY), zoom)
                                },
                            shape = CircleShape,
                            color = MarkerColors.clusterFill,
                            border = BorderStroke(1.dp, MarkerColors.clusterRim),
                            shadowElevation = markerClusterShadowDp(zoom, maxZoom)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cluster.markers.size.toString(),
                                    color = MarkerColors.clusterLabel,
                                    fontSize = clusterCountFontSp(zoom, maxZoom),
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.15.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        val marker = cluster.markers.first()
                        val accent = markerAccentColor(marker)
                        val shell = markerShellColor(zoom, maxZoom)
                        val borderStroke = if (accent != null) {
                            BorderStroke(1.5.dp, accent.copy(alpha = 0.92f))
                        } else {
                            BorderStroke(1.dp, MarkerColors.markerRim)
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .clickable { onMarkerClick(marker) },
                            shape = CircleShape,
                            color = shell,
                            border = borderStroke,
                            shadowElevation = markerShadowDp(zoom, maxZoom)
                        ) {
                            Box(
                                modifier = Modifier.padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = markerIconRes(marker.type)),
                                    contentDescription = marker.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
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
                    modifier = Modifier.width(54.dp),
                    colors = tsuHeaderButtonColors()
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
                        .width(54.dp),
                    colors = tsuHeaderButtonColors()
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

private const val SINGLE_MARKER_LAYOUT_DP = 36f

private fun zoomFraction(zoom: Float, maxZoom: Float): Float =
    ((zoom - 1f) / (maxZoom - 1f).coerceAtLeast(0.001f)).coerceIn(0f, 1f)

private fun mapPinScreenScale(zoom: Float, maxZoom: Float): Float {
    val t = zoomFraction(zoom, maxZoom)
    val wantOnScreen = (1f - 0.22f * t).coerceIn(0.72f, 1f)
    return wantOnScreen / zoom.coerceAtLeast(0.01f)
}

private fun clusterBubbleLayoutDp(clusterCount: Int): Float {
    val n = clusterCount.coerceAtMost(9)
    return (28 + n * 2).toFloat().coerceIn(26f, 46f)
}

private fun clusterRadiusForZoom(zoomLevel: Float, maxZoom: Float): Float {
    val z = zoomLevel.coerceIn(1f, maxZoom.coerceAtLeast(1.001f))
    val t = zoomFraction(zoomLevel, maxZoom)
    val base = 54f / z.pow(1.55f)
    val nearMax = t * t
    val radius = base * (1f - 0.92f * nearMax) + 0.12f * nearMax
    return radius.coerceIn(0.12f, 52f)
}

private object MarkerColors {
    val clusterFill = Color(0xE62E333D)
    val clusterRim = Color(0x38FFFFFF)
    val clusterLabel = Color(0xFFEEF0F3)
    val shellBase = Color(0xFFF6F7F9)
    val markerRim = Color(0x22000000)
}

private fun markerShellColor(zoom: Float, maxZoom: Float): Color {
    val t = zoomFraction(zoom, maxZoom)
    return MarkerColors.shellBase.copy(alpha = 0.84f + t * 0.12f)
}

private fun markerShadowDp(zoom: Float, maxZoom: Float) =
    (3.2f + zoomFraction(zoom, maxZoom) * 2.4f).dp

private fun markerClusterShadowDp(zoom: Float, maxZoom: Float) =
    (4f + zoomFraction(zoom, maxZoom) * 2f).dp

private fun clusterCountFontSp(zoom: Float, maxZoom: Float) =
    (13f - zoomFraction(zoom, maxZoom) * 3f).coerceIn(9.5f, 13f).sp

private fun markerIconRes(type: MarkerType): Int =
    when (type) {
        MarkerType.SHOP -> R.drawable.shop_icon
        else -> R.drawable.sight_icon
    }