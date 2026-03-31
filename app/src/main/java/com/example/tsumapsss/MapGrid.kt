package com.example.tsumapsss

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color

class MapGrid(private val context: Context) {

    private val width = 1000
    private val height = 1571

    private val grid = Array(height) { IntArray(width) }

    fun buildFromImage(imageResId: Int) {
        val bitmap = BitmapFactory.decodeResource(context.resources, imageResId)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                grid[y][x] = if (isGreen(pixel)) 1 else 0
            }
        }

        bitmap.recycle()

        val walkableCount = countWalkableCells()
        android.util.Log.d("MapGrid", "Заполнено ${width}x$height = ${width * height} ячеек")
        android.util.Log.d("MapGrid", "Проходимых клеток: $walkableCount")
    }

    private fun isGreen(pixel: Int): Boolean {
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)

        return green > red + 30 && green > blue + 30 && green > 80
    }

    private fun countWalkableCells(): Int {
        var count = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (grid[y][x] == 1) count++
            }
        }
        return count
    }

    fun isWalkable(x: Int, y: Int): Boolean {
        return if (x in 0 until width && y in 0 until height) {
            grid[y][x] == 1
        } else {
            false
        }
    }

    fun getCell(x: Int, y: Int): Int {
        return if (x in 0 until width && y in 0 until height) {
            grid[y][x]
        } else {
            0
        }
    }

    fun getWidth(): Int = width

    fun getHeight(): Int = height

    fun markerToPixel(xPercent: Float, yPercent: Float): Pair<Int, Int> {
        val pixelX = (xPercent * width).toInt().coerceIn(0, width - 1)
        val pixelY = (yPercent * height).toInt().coerceIn(0, height - 1)
        return Pair(pixelX, pixelY)
    }

    fun pixelToMarker(x: Int, y: Int): Pair<Float, Float> {
        return Pair(
            x.toFloat() / width,
            y.toFloat() / height
        )
    }
}