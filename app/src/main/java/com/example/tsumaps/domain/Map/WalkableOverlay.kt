package com.example.tsumaps.domain.debug

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntSize
import com.example.tsumaps.domain.map.MapGrid
import com.example.tsumaps.domain.models.Point

@Composable
fun WalkableOverlay(
    mapGrid: MapGrid,
    zoom: Float,
    pan: Offset,
    imageActualSize: IntSize,
    enabled: Boolean = true
) {
    if (!enabled) return

    val mapWidth = mapGrid.getWidth()
    val mapHeight = mapGrid.getHeight()
    val step = 20

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = zoom,
                scaleY = zoom,
                translationX = pan.x,
                translationY = pan.y,
                transformOrigin = TransformOrigin(0f, 0f)
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (y in 0 until mapHeight step step) {
                for (x in 0 until mapWidth step step) {
                    val point = Point(x, y)

                    if (mapGrid.isWalkable(point)) {
                        val imageX = (x.toFloat() / mapWidth) * imageActualSize.width
                        val imageY = (y.toFloat() / mapHeight) * imageActualSize.height

                        drawCircle(
                            color = Color.Green.copy(alpha = 0.3f),
                            radius = (step / 2).toFloat(),
                            center = Offset(imageX, imageY)
                        )
                    }
                }
            }
        }
    }
}