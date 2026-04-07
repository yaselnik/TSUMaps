package com.example.tsumaps.domain.path

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.tsumaps.domain.algorithms.Astar
import com.example.tsumaps.domain.algorithms.GenericAlgorithm
import com.example.tsumaps.domain.map.MapGrid
import com.example.tsumaps.domain.models.Point

class PathManager(private val mapGrid: MapGrid) {

    var startPoint by mutableStateOf<Point?>(null)
        private set

    var endPoint by mutableStateOf<Point?>(null)
        private set

    var path by mutableStateOf<List<Point>?>(null)
        private set

    var message by mutableStateOf("Двойной тап: поставь 2 точки")
        private set

    fun onDoubleTap(point: Point) {
        Log.d("PathManager", "Double tap at: $point")

        val walkablePoint = if (mapGrid.isWalkable(point)) {
            point
        } else {
            mapGrid.findNearestWalkable(point, maxRadius = 50)
        }

        if (walkablePoint == null) {
            message = "⚠️ Точка в непроходимой области!\nПопробуй выбрать другое место"
            return
        }

        when {
            startPoint == null -> {
                startPoint = walkablePoint
                path = null
                message = "✅ Точка 1 установлена\nТеперь выбери точку 2"
            }

            endPoint == null -> {
                endPoint = walkablePoint
                path = null
                message = "✅ Точка 2 установлена\nНажми «Запустить»"
            }

            else -> {
                reset()
                startPoint = walkablePoint
                message = "✅ Точка 1 установлена\nТеперь выбери точку 2"
            }
        }
    }

    fun buildPath(): Boolean {
        val s = startPoint ?: run {
            message = "❌ Сначала поставь точку 1"
            return false
        }
        val e = endPoint ?: run {
            message = "❌ Сначала поставь точку 2"
            return false
        }

        if (!mapGrid.isWalkable(s)) {
            message = "❌ Точка 1 в непроходимой зоне"
            Log.e("PathManager", "Точка 1 не проходима: $s")
            return false
        }

        if (!mapGrid.isWalkable(e)) {
            message = "❌ Точка 2 в непроходимой зоне"
            Log.e("PathManager", "Точка 2 не проходима: $e")
            return false
        }

        Log.d("PathManager", "Поиск пути: $s -> $e")
        val result = Astar(mapGrid).findPath(s, e)

        return if (result != null) {
            path = result
            message = "✅ Путь найден!\nДлина: ${result.size} точек"
            true
        } else {
            path = null
            message = "❌ Путь не найден\nТочки недостижимы друг от друга"
            false
        }
    }

    fun reset() {
        startPoint = null
        endPoint = null
        path = null
        message = "Двойной тап: поставь 2 точки"
        Log.d("PathManager", "Сброс всех точек")
    }

    fun hasTwoPoints(): Boolean = startPoint != null && endPoint != null
}