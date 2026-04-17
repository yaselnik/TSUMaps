package com.example.tsumaps.domain.path

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.tsumaps.domain.algorithms.Astar
import com.example.tsumaps.domain.algorithms.GenericAlgorithm
import com.example.tsumaps.domain.map.MapGrid
import com.example.tsumaps.domain.models.Place
import com.example.tsumaps.domain.models.Point
import com.example.tsumaps.domain.models.UserRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

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

    var message by mutableStateOf("\u0414\u0432\u043E\u0439\u043D\u043E\u0439 \u0442\u0430\u043F: \u043F\u043E\u0441\u0442\u0430\u0432\u044C 2 \u0442\u043E\u0447\u043A\u0438")
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
        message = "\u0410\u043B\u0433\u043E\u0440\u0438\u0442\u043C: ${algorithm.name}\n${baseModeHint()}"
    }

    fun setGoodsRequest(goods: Set<String>) {
        requiredGoods = goods
        mode = "goods"
        reset()
        message = "\u041F\u043E\u0438\u0441\u043A \u043C\u0430\u0433\u0430\u0437\u0438\u043D\u043E\u0432 \u0441: ${goods.joinToString()}\n\u0414\u0432\u043E\u0439\u043D\u043E\u0439 \u0442\u0430\u043F \u0434\u043B\u044F \u0441\u0442\u0430\u0440\u0442\u0430"
    }

    fun enterGoodsMode() {
        mode = "goods"
        reset()
        message = if (requiredGoods.isEmpty()) {
            "\u0412\u044B\u0431\u0435\u0440\u0438 \u0442\u043E\u0432\u0430\u0440\u044B \u0438 \u043F\u043E\u0441\u0442\u0430\u0432\u044C \u0441\u0442\u0430\u0440\u0442\u043E\u0432\u0443\u044E \u0442\u043E\u0447\u043A\u0443"
        } else {
            "\u0422\u043E\u0432\u0430\u0440\u044B: ${requiredGoods.joinToString()}\n\u0414\u0432\u043E\u0439\u043D\u043E\u0439 \u0442\u0430\u043F \u0434\u043B\u044F \u0441\u0442\u0430\u0440\u0442\u0430"
        }
    }

    fun clearGoodsRequest() {
        requiredGoods = emptySet()
        mode = "simple"
        reset()
        message = "\u0412\u044B\u0431\u043E\u0440 \u0442\u043E\u0432\u0430\u0440\u043E\u0432 \u043E\u0447\u0438\u0449\u0435\u043D.\n\u0414\u0432\u043E\u0439\u043D\u043E\u0439 \u0442\u0430\u043F: \u043F\u043E\u0441\u0442\u0430\u0432\u044C 2 \u0442\u043E\u0447\u043A\u0438"
    }

    fun setSimpleMode() {
        mode = "simple"
        reset()
        message = "\u0414\u0432\u043E\u0439\u043D\u043E\u0439 \u0442\u0430\u043F: \u043F\u043E\u0441\u0442\u0430\u0432\u044C 2 \u0442\u043E\u0447\u043A\u0438"
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
            message = "\u0422\u043E\u0447\u043A\u0430 \u0432 \u043D\u0435\u043F\u0440\u043E\u0445\u043E\u0434\u0438\u043C\u043E\u0439 \u043E\u0431\u043B\u0430\u0441\u0442\u0438.\n\u041F\u043E\u043F\u0440\u043E\u0431\u0443\u0439 \u0432\u044B\u0431\u0440\u0430\u0442\u044C \u0434\u0440\u0443\u0433\u043E\u0435 \u043C\u0435\u0441\u0442\u043E"
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
                message = "\u0422\u043E\u0447\u043A\u0430 1 \u0443\u0441\u0442\u0430\u043D\u043E\u0432\u043B\u0435\u043D\u0430\n\u0422\u0435\u043F\u0435\u0440\u044C \u0432\u044B\u0431\u0435\u0440\u0438 \u0442\u043E\u0447\u043A\u0443 2"
            }

            endPoint == null -> {
                endPoint = walkablePoint
                path = null
                message = "\u0422\u043E\u0447\u043A\u0430 2 \u0443\u0441\u0442\u0430\u043D\u043E\u0432\u043B\u0435\u043D\u0430\n\u041D\u0430\u0436\u043C\u0438 \u00AB\u0417\u0430\u043F\u0443\u0441\u0442\u0438\u0442\u044C\u00BB"
            }

            else -> {
                reset()
                startPoint = walkablePoint
                message = "\u0422\u043E\u0447\u043A\u0430 1 \u0443\u0441\u0442\u0430\u043D\u043E\u0432\u043B\u0435\u043D\u0430\n\u0422\u0435\u043F\u0435\u0440\u044C \u0432\u044B\u0431\u0435\u0440\u0438 \u0442\u043E\u0447\u043A\u0443 2"
            }
        }
    }

    private fun handleGoodsMode(walkablePoint: Point) {
        if (startPoint == null) {
            startPoint = walkablePoint
            path = null
            message = "\u0421\u0442\u0430\u0440\u0442\u043E\u0432\u0430\u044F \u0442\u043E\u0447\u043A\u0430 \u0443\u0441\u0442\u0430\u043D\u043E\u0432\u043B\u0435\u043D\u0430\n\u041D\u0430\u0436\u043C\u0438 \u00AB\u0417\u0430\u043F\u0443\u0441\u0442\u0438\u0442\u044C\u00BB \u0434\u043B\u044F \u043F\u043E\u0438\u0441\u043A\u0430 \u043C\u0430\u0440\u0448\u0440\u0443\u0442\u0430 \u043F\u043E \u0442\u043E\u0432\u0430\u0440\u0430\u043C"
        } else {
            reset()
            startPoint = walkablePoint
            message = "\u0421\u0442\u0430\u0440\u0442\u043E\u0432\u0430\u044F \u0442\u043E\u0447\u043A\u0430 \u043E\u0431\u043D\u043E\u0432\u043B\u0435\u043D\u0430\n\u041D\u0430\u0436\u043C\u0438 \u00AB\u0417\u0430\u043F\u0443\u0441\u0442\u0438\u0442\u044C\u00BB \u0434\u043B\u044F \u043F\u043E\u0438\u0441\u043A\u0430 \u043C\u0430\u0440\u0448\u0440\u0443\u0442\u0430"
        }
    }

    suspend fun buildPath(): Boolean {
        val modeSnapshot = mode
        val startSnapshot = startPoint
        val endSnapshot = endPoint
        val requiredSnapshot = requiredGoods
        val placesSnapshot = availablePlaces
        val algorithmSnapshot = selectedAlgorithm

        val computed = withContext(Dispatchers.Default) {
            when (modeSnapshot) {
                "simple" -> computeSimplePath(startSnapshot, endSnapshot)
                "goods" -> computeGoodsPath(startSnapshot, requiredSnapshot, placesSnapshot, algorithmSnapshot)
                else -> ComputationResult(null, "\u041D\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043D\u044B\u0439 \u0440\u0435\u0436\u0438\u043C", false)
            }
        }

        return withContext(Dispatchers.Main) {
            path = computed.path
            message = computed.message
            computed.success
        }
    }

    fun notifyPathBuildCancelled() {
        message = "\u041F\u043E\u0441\u0442\u0440\u043E\u0435\u043D\u0438\u0435 \u043C\u0430\u0440\u0448\u0440\u0443\u0442\u0430 \u043E\u0442\u043C\u0435\u043D\u0435\u043D\u043E"
    }

    suspend fun buildPathToPlace(place: Place): Boolean {
        val start = startPoint ?: run {
            message = "\u0421\u043D\u0430\u0447\u0430\u043B\u0430 \u0432\u044B\u0431\u0435\u0440\u0438 \u0441\u0442\u0430\u0440\u0442\u043E\u0432\u0443\u044E \u0442\u043E\u0447\u043A\u0443 \u0434\u0432\u043E\u0439\u043D\u044B\u043C \u0442\u0430\u043F\u043E\u043C"
            return false
        }

        val destination = withContext(Dispatchers.Default) {
            if (mapGrid.isWalkable(place.location)) {
                place.location
            } else {
                mapGrid.findNearestWalkable(place.location, maxRadius = 50)
            }
        } ?: run {
            message = "\u0423 \u043C\u0435\u0441\u0442\u0430 \u043D\u0435\u0442 \u0434\u043E\u0441\u0442\u0443\u043F\u043D\u043E\u0439 \u043F\u0440\u043E\u0445\u043E\u0434\u0438\u043C\u043E\u0439 \u0442\u043E\u0447\u043A\u0438 \u0440\u044F\u0434\u043E\u043C"
            return false
        }

        mode = "simple"
        endPoint = destination

        val result = withContext(Dispatchers.Default) {
            Astar(mapGrid).findPath(start, destination)
        }

        return if (result.isNotEmpty()) {
            path = result
            message = "\u041F\u0443\u0442\u044C \u0434\u043E \u00AB${place.name}\u00BB \u043F\u043E\u0441\u0442\u0440\u043E\u0435\u043D\n\u0414\u043B\u0438\u043D\u0430: ${result.size} \u0442\u043E\u0447\u0435\u043A"
            true
        } else {
            path = null
            message = "\u041D\u0435 \u0443\u0434\u0430\u043B\u043E\u0441\u044C \u043F\u043E\u0441\u0442\u0440\u043E\u0438\u0442\u044C \u043F\u0443\u0442\u044C \u0434\u043E \u00AB${place.name}\u00BB"
            false
        }
    }

    private data class ComputationResult(
        val path: List<Point>?,
        val message: String,
        val success: Boolean
    )

    private fun computeSimplePath(start: Point?, end: Point?): ComputationResult {
        val safeStart = start ?: return ComputationResult(null, "\u0421\u043D\u0430\u0447\u0430\u043B\u0430 \u043F\u043E\u0441\u0442\u0430\u0432\u044C \u0442\u043E\u0447\u043A\u0443 1", false)
        val safeEnd = end ?: return ComputationResult(null, "\u0421\u043D\u0430\u0447\u0430\u043B\u0430 \u043F\u043E\u0441\u0442\u0430\u0432\u044C \u0442\u043E\u0447\u043A\u0443 2", false)

        if (!mapGrid.isWalkable(safeStart)) {
            return ComputationResult(null, "\u0422\u043E\u0447\u043A\u0430 1 \u0432 \u043D\u0435\u043F\u0440\u043E\u0445\u043E\u0434\u0438\u043C\u043E\u0439 \u0437\u043E\u043D\u0435", false)
        }

        if (!mapGrid.isWalkable(safeEnd)) {
            return ComputationResult(null, "\u0422\u043E\u0447\u043A\u0430 2 \u0432 \u043D\u0435\u043F\u0440\u043E\u0445\u043E\u0434\u0438\u043C\u043E\u0439 \u0437\u043E\u043D\u0435", false)
        }

        Log.d("PathManager", "A* path search: $safeStart -> $safeEnd")
        val result = Astar(mapGrid).findPath(safeStart, safeEnd)

        return if (result.isNotEmpty()) {
            ComputationResult(result, "\u041F\u0443\u0442\u044C \u043D\u0430\u0439\u0434\u0435\u043D\n\u0414\u043B\u0438\u043D\u0430: ${result.size} \u0442\u043E\u0447\u0435\u043A", true)
        } else {
            ComputationResult(null, "\u041F\u0443\u0442\u044C \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D\n\u0422\u043E\u0447\u043A\u0438 \u043D\u0435\u0434\u043E\u0441\u0442\u0438\u0436\u0438\u043C\u044B", false)
        }
    }

    private suspend fun computeGoodsPath(
        start: Point?,
        required: Set<String>,
        places: Map<Int, Place>,
        algorithm: SearchAlgorithm
    ): ComputationResult {
        val safeStart = start ?: return ComputationResult(null, "\u0421\u043D\u0430\u0447\u0430\u043B\u0430 \u043F\u043E\u0441\u0442\u0430\u0432\u044C \u0441\u0442\u0430\u0440\u0442\u043E\u0432\u0443\u044E \u0442\u043E\u0447\u043A\u0443", false)

        if (required.isEmpty()) {
            return ComputationResult(null, "\u041D\u0435 \u0432\u044B\u0431\u0440\u0430\u043D\u044B \u043D\u0443\u0436\u043D\u044B\u0435 \u0442\u043E\u0432\u0430\u0440\u044B", false)
        }

        if (places.isEmpty()) {
            return ComputationResult(null, "\u041D\u0435\u0442 \u0434\u043E\u0441\u0442\u0443\u043F\u043D\u044B\u0445 \u043C\u0430\u0433\u0430\u0437\u0438\u043D\u043E\u0432", false)
        }

        val relevantPlaces = places.filter { (_, place) ->
            place.menu.any { required.contains(it) }
        }

        if (relevantPlaces.isEmpty()) {
            return ComputationResult(null, "\u041D\u0435\u0442 \u043C\u0430\u0433\u0430\u0437\u0438\u043D\u043E\u0432 \u0441 \u043D\u0443\u0436\u043D\u044B\u043C\u0438 \u0442\u043E\u0432\u0430\u0440\u0430\u043C\u0438", false)
        }

        val bestRoute = when (algorithm) {
            SearchAlgorithm.Astar -> buildGreedyRouteByAstar(safeStart, relevantPlaces, required)
            SearchAlgorithm.GenericAlgorithm -> {
                try {
                    val userRequest = UserRequest(choice = required)
                    GenericAlgorithm(
                        startPoint = safeStart,
                        places = relevantPlaces.toMutableMap(),
                        request = userRequest,
                        grid = mapGrid,
                        populationSize = 100,
                        generations = 200,
                        tournamentSize = 5,
                        mutationRate = 0.2,
                        elitismCount = 5
                    ).evolve()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("PathManager", "Genetic algorithm failed", e)
                    return ComputationResult(
                        null,
                        "\u041e\u0448\u0438\u0431\u043a\u0430 \u0433\u0435\u043d\u0435\u0442\u0438\u0447\u0435\u0441\u043a\u043e\u0433\u043e \u0430\u043b\u0433\u043e\u0440\u0438\u0442\u043c\u0430: ${e.message ?: ""}",
                        false
                    )
                }
            }
        }

        return if (bestRoute.isNotEmpty()) {
            val fullPath = buildFullPath(safeStart, bestRoute, relevantPlaces)
            ComputationResult(
                fullPath,
                "\u041C\u0430\u0440\u0448\u0440\u0443\u0442 \u043D\u0430\u0439\u0434\u0435\u043D (${algorithm.name})\n\u0427\u0435\u0440\u0435\u0437 ${bestRoute.size} \u043C\u0430\u0433\u0430\u0437\u0438\u043D\u043E\u0432\n\u0421\u043E\u0431\u0440\u0430\u043D\u043E \u0442\u043E\u0432\u0430\u0440\u043E\u0432: ${required.size}",
                true
            )
        } else {
            ComputationResult(null, "\u041D\u0435 \u0443\u0434\u0430\u043B\u043E\u0441\u044C \u043D\u0430\u0439\u0442\u0438 \u043E\u043F\u0442\u0438\u043C\u0430\u043B\u044C\u043D\u044B\u0439 \u043C\u0430\u0440\u0448\u0440\u0443\u0442", false)
        }
    }

    private fun buildFullPath(start: Point, route: List<Int>, places: Map<Int, Place>): List<Point> {
        val fullPath = mutableListOf<Point>()
        var currentPoint = start

        for (placeId in route) {
            val targetPoint = places[placeId]?.location ?: continue
            val segment = Astar(mapGrid).findPath(currentPoint, targetPoint)
            if (segment.isNotEmpty()) {
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

    fun setExternalTourPath(polyline: List<Point>, caption: String) {
        if (polyline.isEmpty()) {
            path = null
            message = caption
            return
        }
        path = polyline
        startPoint = polyline.first()
        endPoint = polyline.last()
        message = caption
    }

    fun reset() {
        startPoint = null
        endPoint = null
        path = null
        message = baseModeHint()
        Log.d("PathManager", "Reset points")
    }

    fun hasTwoPoints(): Boolean = when (mode) {
        "simple" -> startPoint != null && endPoint != null
        "goods" -> startPoint != null
        else -> false
    }

    private fun baseModeHint(): String {
        return if (mode == "simple") {
            "\u0414\u0432\u043E\u0439\u043D\u043E\u0439 \u0442\u0430\u043F: \u043F\u043E\u0441\u0442\u0430\u0432\u044C 2 \u0442\u043E\u0447\u043A\u0438"
        } else {
            "\u0414\u0432\u043E\u0439\u043D\u043E\u0439 \u0442\u0430\u043F: \u043F\u043E\u0441\u0442\u0430\u0432\u044C \u0441\u0442\u0430\u0440\u0442\u043E\u0432\u0443\u044E \u0442\u043E\u0447\u043A\u0443"
        }
    }

    private suspend fun buildGreedyRouteByAstar(
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
            yield()
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
