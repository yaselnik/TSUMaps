package com.example.tsumaps.domain.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import com.example.tsumaps.domain.models.Point
import kotlin.math.min

class MapGrid(
    private val context: Context,
    val pathScale: Int = DEFAULT_PATH_SCALE
) {

    private var imageWidth = 1288
    private var imageHeight = 1571

    private var gridW = 0
    private var gridH = 0

    private lateinit var grid: Array<BooleanArray>

    fun buildFromImage(imageResId: Int) {
        val options = BitmapFactory.Options().apply {
            inScaled = false
        }
        val bitmap = BitmapFactory.decodeResource(context.resources, imageResId, options)
            ?: run {
                Log.e("MapGrid", "Не удалось декодировать изображение $imageResId")
                return
            }

        imageWidth = bitmap.width
        imageHeight = bitmap.height

        val s = pathScale.coerceAtLeast(1)
        gridW = (imageWidth + s - 1) / s
        gridH = (imageHeight + s - 1) / s

        grid = Array(gridH) { gy ->
            BooleanArray(gridW) { gx ->
                isBlockWalkableMajority(bitmap, gx, gy, s)
            }
        }

        var walkableCells = 0
        for (row in grid) {
            walkableCells += row.count { it }
        }

        bitmap.recycle()

        val totalCells = gridW.toLong() * gridH
        val percentage = (walkableCells.toDouble() / totalCells) * 100

        Log.d("MapGrid", "Карта: ${imageWidth}x$imageHeight px, pathScale=$s → сетка ${gridW}x$gridH")
        Log.d("MapGrid", "Проходимых ячеек: $walkableCells из $totalCells (%.2f%%)".format(percentage))
    }

    private fun isBlockWalkableMajority(bitmap: Bitmap, gx: Int, gy: Int, s: Int): Boolean {
        val x0 = gx * s
        val y0 = gy * s
        val x1 = min((gx + 1) * s, bitmap.width)
        val y1 = min((gy + 1) * s, bitmap.height)
        var green = 0
        var n = 0
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                n++
                if (checkPixel(bitmap.getPixel(x, y))) green++
            }
        }
        if (n == 0) return false
        return green * 2 >= n
    }

    private fun checkPixel(pixel: Int): Boolean {
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)

        return green > red + 30 && green > blue + 30 && green > 80
    }

    fun getGridWidth(): Int = gridW
    fun getGridHeight(): Int = gridH

    fun getWidth(): Int = imageWidth
    fun getHeight(): Int = imageHeight

    fun pixelToCell(p: Point): Point {
        val s = pathScale.coerceAtLeast(1)
        return Point(
            (p.x / s).coerceIn(0, (gridW - 1).coerceAtLeast(0)),
            (p.y / s).coerceIn(0, (gridH - 1).coerceAtLeast(0))
        )
    }

    fun cellToPixelCenter(c: Point): Point {
        val s = pathScale.coerceAtLeast(1)
        val cx = c.x.coerceIn(0, gridW - 1)
        val cy = c.y.coerceIn(0, gridH - 1)
        return Point(
            (cx * s + s / 2).coerceIn(0, imageWidth - 1),
            (cy * s + s / 2).coerceIn(0, imageHeight - 1)
        )
    }

    fun isWalkableCell(c: Point): Boolean {
        return c.x in 0 until gridW &&
            c.y in 0 until gridH &&
            grid[c.y][c.x]
    }

    fun isWalkable(point: Point): Boolean {
        return point.x in 0 until imageWidth &&
            point.y in 0 until imageHeight &&
            isWalkableCell(pixelToCell(point))
    }

    fun percentToPixel(xPercent: Float, yPercent: Float): Point {
        val x = (imageWidth * xPercent.coerceIn(0f, 1f)).toInt().coerceIn(0, imageWidth - 1)
        val y = (imageHeight * yPercent.coerceIn(0f, 1f)).toInt().coerceIn(0, imageHeight - 1)
        return Point(x, y)
    }

    fun pixelToPercent(point: Point): Pair<Float, Float> {
        return Pair(
            point.x.toFloat() / imageWidth,
            point.y.toFloat() / imageHeight
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

    fun nearestWalkableCellFromPixel(p: Point): Point? {
        val px = findNearestWalkable(p, maxRadius = 100) ?: return null
        return pixelToCell(px)
    }

    fun getPixelColor(point: Point, imageResId: Int): Triple<Int, Int, Int>? {
        if (point.x !in 0 until imageWidth || point.y !in 0 until imageHeight) return null

        val options = BitmapFactory.Options().apply { inScaled = false }
        val bitmap = BitmapFactory.decodeResource(context.resources, imageResId, options)
        val pixel = bitmap.getPixel(point.x, point.y)
        bitmap.recycle()

        return Triple(Color.red(pixel), Color.green(pixel), Color.blue(pixel))
    }

    companion object {
        const val DEFAULT_PATH_SCALE = 2
    }
}
