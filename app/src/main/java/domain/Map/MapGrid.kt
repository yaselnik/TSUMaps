package domain.Map

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import com.example.tsumaps.domain.models.Point

class MapGrid(private val context: Context) {

    private val width = 1288
    private val height = 1571

    private val grid = Array(height) { IntArray(width) }

    fun buildFromImage(imageResId: Int) {
        val bitmap = BitmapFactory.decodeResource(context.resources, imageResId)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                grid[y][x] = if (isWalkablePixel(pixel)) 1 else 0
            }
        }

        bitmap.recycle()

        val walkableCount = countWalkableCells()
        Log.d("MapGrid", "Заполнено ${width}x$height = ${width * height} ячеек")
        Log.d("MapGrid", "Проходимых клеток: $walkableCount")
    }

    private fun isWalkablePixel(pixel: Int): Boolean {
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

    fun isWalkable(point: Point): Boolean {
        return if (point.x in 0 until width && point.y in 0 until height) {
            grid[point.y][point.x] == 1
        } else {
            false
        }
    }

    fun isWalkable(x: Int, y: Int): Boolean {
        return isWalkable(Point(x, y))
    }

    fun getCell(point: Point): Int {
        return if (point.x in 0 until width && point.y in 0 until height) {
            grid[point.y][point.x]
        } else {
            0
        }
    }

    fun getCell(x: Int, y: Int): Int {
        return getCell(Point(x, y))
    }

    fun getWidth(): Int = width

    fun getHeight(): Int = height

    fun markerToPixel(xPercent: Float, yPercent: Float): Point {
        val pixelX = (xPercent * width).toInt().coerceIn(0, width - 1)
        val pixelY = (yPercent * height).toInt().coerceIn(0, height - 1)
        return Point(pixelX, pixelY)
    }

    fun pixelToMarker(point: Point): Pair<Float, Float> {
        return Pair(
            point.x.toFloat() / width,
            point.y.toFloat() / height
        )
    }

    fun pixelToMarker(x: Int, y: Int): Pair<Float, Float> {
        return pixelToMarker(Point(x, y))
    }
}