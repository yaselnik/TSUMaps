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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    suspend fun buildPath(): Boolean {
        val modeSnapshot = mode
        val s = startPoint
        val e = endPoint
        val required = requiredGoods
        val placesSnapshot = availablePlaces
        val algorithmSnapshot = selectedAlgorithm

        val computed = withContext(Dispatchers.Default) {
            when (modeSnapshot) {
                "simple" -> computeSimplePath(s, e)
                "goods" -> computeGoodsPath(s, required, placesSnapshot, algorithmSnapshot)
                else -> ComputationResult(null, "❌ Неизвестный режим", false)
            }
        }

        return withContext(Dispatchers.Main) {
            path = computed.path
            message = computed.message
            computed.success
        }
    }

    suspend fun buildPathToPlace(place: Place): Boolean {
        val s = startPoint ?: run {
            message = "❌ Сначала выбери стартовую точку двойным тапом"
            return false
        }

        val destination = withContext(Dispatchers.Default) {
            if (mapGrid.isWalkable(place.location)) {
                place.location
            } else {
                mapGrid.findNearestWalkable(place.location, maxRadius = 50)
            }
        } ?: run {
            message = "❌ У магазина нет доступной проходимой точки рядом"
            return false
        }

        mode = "simple"
        endPoint = destination

        val result = withContext(Dispatchers.Default) {
            Astar(mapGrid).findPath(s, destination)
        }

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

    private data class ComputationResult(
        val path: List<Point>?,
        val message: String,
        val success: Boolean
    )

    private fun computeSimplePath(
        s: Point?,
        e: Point?
    ): ComputationResult {
        val start = s ?: return ComputationResult(null, "❌ Сначала поставь точку 1", false)
        val end = e ?: return ComputationResult(null, "❌ Сначала поставь точку 2", false)

        if (!mapGrid.isWalkable(start)) {
            return ComputationResult(null, "❌ Точка 1 в непроходимой зоне", false)
        }

        if (!mapGrid.isWalkable(end)) {
            return ComputationResult(null, "❌ Точка 2 в непроходимой зоне", false)
        }

        Log.d("PathManager", "Поиск пути A*: $start -> $end")
        val result = Astar(mapGrid).findPath(start, end)

        return if (result != null) {
            ComputationResult(result, "✅ Путь найден!\nДлина: ${result.size} точек", true)
        } else {
            ComputationResult(null, "❌ Путь не найден\nТочки недостижимы", false)
        }
    }

    private fun computeGoodsPath(
        s: Point?,
        required: Set<String>,
        places: Map<Int, Place>,
        algorithm: SearchAlgorithm
    ): ComputationResult {
        val start = s ?: return ComputationResult(null, "❌ Сначала поставь стартовую точку", false)

        if (required.isEmpty()) {
            return ComputationResult(null, "❌ Не выбраны нужные товары", false)
        }

        if (places.isEmpty()) {
            return ComputationResult(null, "❌ Нет доступных магазинов", false)
        }

        val relevantPlaces = places.filter { (_, place) ->
            place.menu.any { required.contains(it) }
        }

        if (relevantPlaces.isEmpty()) {
            return ComputationResult(null, "❌ Нет магазинов с нужными товарами", false)
        }

        Log.d("PathManager", "Поиск маршрута через ${relevantPlaces.size} магазинов. Алгоритм: $algorithm")

        val bestRoute = when (algorithm) {
            SearchAlgorithm.Astar -> buildGreedyRouteByAstar(start, relevantPlaces, required)
            SearchAlgorithm.GenericAlgorithm -> {
                val userRequest = UserRequest(choice = required)
                val geneticAlgorithm = GenericAlgorithm(
                    startPoint = start,
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
            val fullPath = buildFullPath(start, bestRoute, relevantPlaces)
            ComputationResult(
                fullPath,
                "✅ Маршрут найден (${algorithm.name})!\nЧерез ${bestRoute.size} магазинов\nСобрано товаров: ${required.size}",
                true
            )
        } else {
            ComputationResult(null, "❌ Не удалось найти оптимальный маршрут", false)
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