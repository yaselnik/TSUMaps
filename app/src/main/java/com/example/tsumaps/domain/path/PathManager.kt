package com.example.tsumaps.domain.path

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.tsumaps.domain.algorithms.Astar
import com.example.tsumaps.domain.algorithms.GenericAlgorithm
import com.example.tsumaps.domain.map.MapGrid
import com.example.tsumaps.domain.models.Point
import com.example.tsumaps.domain.models.Place
import com.example.tsumaps.domain.models.UserRequest
import java.time.LocalTime

class PathManager(private val mapGrid: MapGrid) {

    enum class SearchAlgorithm {
        Astar,
        GenericAlgorithm
    }

    var startPoint by mutableStateOf<Point?>(null)
        private set

    var endPoint by mutableStateOf<Point?>(null)
        private set

    var path by mutableStateOf<List<Point>?>(null)
        private set

    var message by mutableStateOf("Двойной тап: поставь 2 точки")
        private set

    var mode by mutableStateOf("simple")
        private set

    var requiredGoods by mutableStateOf(setOf<String>())
        private set

    var availablePlaces by mutableStateOf<Map<Int, Place>>(emptyMap())
        private set

    var selectedAlgorithm by mutableStateOf(SearchAlgorithm.Astar)
        private set

    fun setSearchAlgorithm(algorithm: SearchAlgorithm) {
        selectedAlgorithm = algorithm
        message = "Алгоритм: ${algorithm.name}\n${baseModeHint()}"
    }

    fun setGoodsRequest(goods: Set<String>) {
        requiredGoods = goods
        mode = "goods"
        reset()
        message = "✅ Поиск магазинов с: ${goods.joinToString()}\nДвойной тап для старта"
    }

    fun clearGoodsRequest() {
        requiredGoods = emptySet()
        mode = "simple"
        reset()
        message = "Выбор товаров очищен.\nДвойной тап: поставь 2 точки"
    }

    fun setSimpleMode() {
        mode = "simple"
        reset()
        message = "Двойной тап: поставь 2 точки"
    }

    fun updatePlaces(places: Map<Int, Place>) {
        availablePlaces = places
    }

    fun onDoubleTap(point: Point) {
        Log.d("PathManager", "Double tap at: $point, mode: $mode")

        val walkablePoint = if (mapGrid.isWalkable(point)) {
            point
        } else {
            mapGrid.findNearestWalkable(point, maxRadius = 50)
        }

        if (walkablePoint == null) {
            message = "⚠️ Точка в непроходимой области!\nПопробуй выбрать другое место"
            return
        }

        when (mode) {
            "simple" -> handleSimpleMode(walkablePoint)
            "goods" -> handleGoodsMode(walkablePoint)
        }
    }

    private fun handleSimpleMode(walkablePoint: Point) {
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

    private fun handleGoodsMode(walkablePoint: Point) {
        if (startPoint == null) {
            startPoint = walkablePoint
            path = null
            message = "✅ Стартовая точка установлена\nНажми «Запустить» для поиска маршрута по товарам"
        } else {
            reset()
            startPoint = walkablePoint
            message = "✅ Стартовая точка обновлена\nНажми «Запустить» для поиска маршрута"
        }
    }

    fun buildPath(): Boolean {
        return when (mode) {
            "simple" -> buildSimplePath()
            "goods" -> buildGoodsPath()
            else -> false
        }
    }

    fun buildPathToPlace(place: Place): Boolean {
        val s = startPoint ?: run {
            message = "❌ Сначала выбери стартовую точку двойным тапом"
            return false
        }

        val destination = if (mapGrid.isWalkable(place.location)) {
            place.location
        } else {
            mapGrid.findNearestWalkable(place.location, maxRadius = 50)
        } ?: run {
            message = "❌ У магазина нет доступной проходимой точки рядом"
            return false
        }

        mode = "simple"
        endPoint = destination

        val result = Astar(mapGrid).findPath(s, destination)
        return if (result.isNotEmpty()) {
            path = result
            message = "✅ Путь до «${place.name}» построен\nДлина: ${result.size} точек"
            true
        } else {
            path = null
            message = "❌ Не удалось построить путь до «${place.name}»"
            false
        }
    }

    private fun buildSimplePath(): Boolean {
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
            return false
        }

        if (!mapGrid.isWalkable(e)) {
            message = "❌ Точка 2 в непроходимой зоне"
            return false
        }

        Log.d("PathManager", "Поиск пути A*: $s -> $e")
        val result = Astar(mapGrid).findPath(s, e)

        return if (result != null) {
            path = result
            message = "✅ Путь найден!\nДлина: ${result.size} точек"
            true
        } else {
            path = null
            message = "❌ Путь не найден\nТочки недостижимы"
            false
        }
    }

    private fun buildGoodsPath(): Boolean {
        val s = startPoint ?: run {
            message = "❌ Сначала поставь стартовую точку"
            return false
        }

        if (requiredGoods.isEmpty()) {
            message = "❌ Не выбраны нужные товары"
            return false
        }

        if (availablePlaces.isEmpty()) {
            message = "❌ Нет доступных магазинов"
            return false
        }

        val relevantPlaces = availablePlaces.filter { (_, place) ->
            place.menu.any { requiredGoods.contains(it) }
        }

        if (relevantPlaces.isEmpty()) {
            message = "❌ Нет магазинов с нужными товарами"
            return false
        }

        Log.d("PathManager", "Поиск маршрута через ${relevantPlaces.size} магазинов. Алгоритм: $selectedAlgorithm")

        val bestRoute = when (selectedAlgorithm) {
            SearchAlgorithm.Astar -> buildGreedyRouteByAstar(s, relevantPlaces, requiredGoods)
            SearchAlgorithm.GenericAlgorithm -> {
                val userRequest = UserRequest(choice = requiredGoods)
                val geneticAlgorithm = GenericAlgorithm(
                    startPoint = s,
                    places = relevantPlaces.toMutableMap(),
                    request = userRequest,
                    grid = mapGrid,
                    populationSize = 100,
                    generations = 200,
                    tournamentSize = 5,
                    mutationRate = 0.2,
                    elitismCount = 5
                )
                geneticAlgorithm.evolve()
            }
        }
        Log.d("PathManager", "Route ids: $bestRoute")

        return if (bestRoute.isNotEmpty()) {
            val fullPath = buildFullPath(s, bestRoute, relevantPlaces)
            path = fullPath
            message = "✅ Маршрут найден (${selectedAlgorithm.name})!\nЧерез ${bestRoute.size} магазинов\n" +
                    "Собрано товаров: ${requiredGoods.size}"
            true
        } else {
            path = null
            message = "❌ Не удалось найти оптимальный маршрут"
            false
        }
    }

    private fun buildFullPath(
        start: Point,
        route: List<Int>,
        places: Map<Int, Place>
    ): List<Point> {
        val fullPath = mutableListOf<Point>()
        var currentPoint = start

        for (placeId in route) {
            val targetPoint = places[placeId]?.location ?: continue
            val segment = Astar(mapGrid).findPath(currentPoint, targetPoint)
            if (segment != null) {
                if (fullPath.isEmpty()) {
                    fullPath.addAll(segment)
                } else {
                    fullPath.addAll(segment.drop(1))
                }
            }
            currentPoint = targetPoint
        }

        return fullPath
    }

    fun reset() {
        startPoint = null
        endPoint = null
        path = null
        message = baseModeHint()
        Log.d("PathManager", "Сброс всех точек")
    }

    fun hasTwoPoints(): Boolean = when (mode) {
        "simple" -> startPoint != null && endPoint != null
        "goods" -> startPoint != null
        else -> false
    }

    private fun baseModeHint(): String {
        return if (mode == "simple") {
            "Двойной тап: поставь 2 точки"
        } else {
            "Двойной тап: поставь стартовую точку"
        }
    }

    private fun buildGreedyRouteByAstar(
        start: Point,
        relevantPlaces: Map<Int, Place>,
        required: Set<String>
    ): List<Int> {
        val astar = Astar(mapGrid)
        val remainingGoods = required.toMutableSet()
        val unvisited = relevantPlaces.toMutableMap()
        val route = mutableListOf<Int>()
        var current = start

        while (remainingGoods.isNotEmpty() && unvisited.isNotEmpty()) {
            val next = unvisited.minByOrNull { (_, place) ->
                val pathLen = astar.findPath(current, place.location).size
                if (pathLen == 0) Int.MAX_VALUE else pathLen
            } ?: break

            val place = next.value
            val segment = astar.findPath(current, place.location)
            if (segment.isEmpty()) {
                unvisited.remove(next.key)
                continue
            }

            route.add(next.key)
            remainingGoods.removeAll(place.menu)
            current = place.location
            unvisited.remove(next.key)
        }

        return if (remainingGoods.isEmpty()) route else emptyList()
    }
}