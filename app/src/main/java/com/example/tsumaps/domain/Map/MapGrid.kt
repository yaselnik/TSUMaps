package com.example.tsumaps.domain.map

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import com.example.tsumaps.domain.models.Point

class MapGrid(private val context: Context) {

    private var width = 1288
    private var height = 1571
    private lateinit var grid: Array<BooleanArray>

    fun buildFromImage(imageResId: Int) {
        val options = BitmapFactory.Options().apply {
            inScaled = false
        }
        val bitmap = BitmapFactory.decodeResource(context.resources, imageResId, options)

        width = bitmap.width
        height = bitmap.height

        grid = Array(height) { BooleanArray(width) }

        var greenCount = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val isWalkable = checkPixel(pixel)
                grid[y][x] = isWalkable
                if (isWalkable) greenCount++
            }
        }

        bitmap.recycle()

        val totalCells = width.toLong() * height
        val percentage = (greenCount.toDouble() / totalCells) * 100

        Log.d("MapGrid", "Размер: ${width}x$height = $totalCells ячеек")
        Log.d("MapGrid", "Проходимых (зеленых): $greenCount")
        Log.d("MapGrid", "Непроходимых: ${totalCells - greenCount}")
        Log.d("MapGrid", "Процент проходимости: %.2f%%".format(percentage))
    }

    private fun checkPixel(pixel: Int): Boolean {
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)

        return green > red + 30 && green > blue + 30 && green > 80
    }

    fun isWalkable(point: Point): Boolean {
        return point.x in 0 until width &&
                point.y in 0 until height &&
                grid[point.y][point.x]
    }

    fun getWidth(): Int = width
    fun getHeight(): Int = height

    fun percentToPixel(xPercent: Float, yPercent: Float): Point {
        val x = (width * xPercent.coerceIn(0f, 1f)).toInt().coerceIn(0, width - 1)
        val y = (height * yPercent.coerceIn(0f, 1f)).toInt().coerceIn(0, height - 1)
        return Point(x, y)
    }

    fun pixelToPercent(point: Point): Pair<Float, Float> {
        return Pair(
            point.x.toFloat() / width,
            point.y.toFloat() / height
        )
    }

    fun findNearestWalkable(point: Point, maxRadius: Int = 50): Point? {
        if (isWalkable(point)) return point

        for (radius in 1..maxRadius) {
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    val checkPoint = Point(point.x + dx, point.y + dy)
                    if (isWalkable(checkPoint)) {
                        return checkPoint
                    }
                }
            }
        }

        return null
    }

    fun getPixelColor(point: Point, imageResId: Int): Triple<Int, Int, Int>? {
        if (point.x !in 0 until width || point.y !in 0 until height) return null

        val options = BitmapFactory.Options().apply { inScaled = false }
        val bitmap = BitmapFactory.decodeResource(context.resources, imageResId, options)
        val pixel = bitmap.getPixel(point.x, point.y)
        bitmap.recycle()

        return Triple(Color.red(pixel), Color.green(pixel), Color.blue(pixel))
    }
}