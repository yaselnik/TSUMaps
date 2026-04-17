package com.example.tsumaps.domain.path

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntSize
import com.example.tsumaps.domain.map.MapGrid
import com.example.tsumaps.domain.models.Point
import kotlin.math.roundToInt

@Composable
fun PathPointsOverlay(
    startPoint: Point?,
    endPoint: Point?,
    path: List<Point>?,
    mapGrid: MapGrid,
    zoom: Float,
    pan: Offset,
    imageActualSize: IntSize
) {
    if (startPoint == null && endPoint == null && (path == null || path.isEmpty())) return

    val mapWidth = mapGrid.getWidth()
    val mapHeight = mapGrid.getHeight()

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
            if (path != null && path.isNotEmpty()) {
                val pathPoints = mutableListOf<Offset>()

                for (point in path) {
                    val imageX = (point.x.toFloat() / mapWidth) * imageActualSize.width
                    val imageY = (point.y.toFloat() / mapHeight) * imageActualSize.height
                    pathPoints.add(Offset(imageX, imageY))
                }

                if (pathPoints.size >= 2) {
                    val drawPath = Path()
                    drawPath.moveTo(pathPoints.first().x, pathPoints.first().y)

                    for (i in 1 until pathPoints.size) {
                        drawPath.lineTo(pathPoints[i].x, pathPoints[i].y)
                    }

                    drawPath(
                        path = drawPath,
                        color = Color(0xFF2196F3),
                        style = Stroke(width = 8f * zoom.coerceAtMost(2f))
                    )
                }
            }

            if (startPoint != null) {
                val imageX = (startPoint.x.toFloat() / mapWidth) * imageActualSize.width
                val imageY = (startPoint.y.toFloat() / mapHeight) * imageActualSize.height

                drawCircle(
                    color = Color(0xFF4CAF50),
                    radius = 16f,
                    center = Offset(imageX, imageY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 8f,
                    center = Offset(imageX, imageY)
                )
            }

            if (endPoint != null) {
                val imageX = (endPoint.x.toFloat() / mapWidth) * imageActualSize.width
                val imageY = (endPoint.y.toFloat() / mapHeight) * imageActualSize.height

                drawCircle(
                    color = Color(0xFFF44336),
                    radius = 16f,
                    center = Offset(imageX, imageY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 8f,
                    center = Offset(imageX, imageY)
                )
            }
        }
    }
}