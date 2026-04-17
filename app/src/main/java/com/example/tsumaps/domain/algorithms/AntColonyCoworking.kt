package com.example.tsumaps.domain.algorithms

import android.util.Log
import com.example.tsumaps.domain.map.MapGrid
import com.example.tsumaps.domain.models.AntColonyResult
import com.example.tsumaps.domain.models.CoworkingLocation
import com.example.tsumaps.domain.models.Point
import kotlin.math.pow
import kotlin.random.Random

internal data class Ant(
    val id: Int,
    var currentPosition: Point,
    val path: MutableList<Point> = mutableListOf(currentPosition),
    val visited: MutableSet<Point> = mutableSetOf(currentPosition),
    var hasSettled: Boolean = false,
    var pathCost: Double = 0.0
) {
    fun moveTo(newPosition: Point, moveCost: Double) {
        currentPosition = newPosition
        path.add(newPosition)
        visited.add(newPosition)
        pathCost += moveCost
    }

    fun reset(startPoint: Point) {
        currentPosition = startPoint
        path.clear()
        path.add(startPoint)
        visited.clear()
        visited.add(startPoint)
        hasSettled = false
        pathCost = 0.0
    }
}

class AntColonyOptimization(
    private val startPoint: Point,
    private val studentCount: Int,
    private val allLocations: List<CoworkingLocation>,
    private val grid: MapGrid,
    private val generations: Int = 50,
    private val alpha: Double = 1.0,
    private val beta: Double = 5.0,
    private val evaporationRate: Double = 0.25,
    private val pheromoneDeposit: Double = 100.0,
    private val elitismFactor: Double = 3.0
) {
    private val pheromones = mutableMapOf<Pair<Point, Point>, Double>()
    private val astar = Astar(grid)
    private val distanceCache = mutableMapOf<Pair<Point, Point>, Double>()

    private var globalBestResult: AntColonyResult? = null
    private var globalBestScore: Double = Double.MIN_VALUE

    fun run(): AntColonyResult {
        var noImprovementCounter = 0
        val walkableStart = grid.findNearestWalkable(startPoint, 100) ?: startPoint

        for (gen in 0 until generations) {
            val ants = List(studentCount) { Ant(it, walkableStart) }
            val currentCapacities = allLocations.associateWith { it.capacity }.toMutableMap()

            constructSolutionsSequentially(ants, currentCapacities)

            val currentResult = buildResultFromAnts(ants)
            val currentScore = evaluateSolution(currentResult)

            if (currentScore > globalBestScore) {
                globalBestScore = currentScore
                globalBestResult = currentResult
                noImprovementCounter = 0
                Log.i("ACO", "Поколение $gen: Новое лучшее решение! Счет: $currentScore, Неразмещено: ${currentResult.unassignedStudents}")
            } else {
                noImprovementCounter++
            }

            updatePheromones(ants)

            if (noImprovementCounter > 10) {
                Log.i("ACO", "Нет улучшений в течение 10 поколений. Завершение.")
                break
            }
        }
        return globalBestResult ?: AntColonyResult(emptyMap(), emptyMap(), studentCount)
    }

    private fun constructSolutionsSequentially(ants: List<Ant>, capacities: MutableMap<CoworkingLocation, Int>) {
        val maxPathLength = (grid.getWidth() + grid.getHeight()) * 1.5

        for (ant in ants) {
            while (!ant.hasSettled && ant.path.size < maxPathLength) {
                val move = chooseNextMove(ant, capacities)
                if (move == null) {
                    ant.hasSettled = true
                    continue
                }

                val moveCost = if (move.x != ant.currentPosition.x && move.y != ant.currentPosition.y) 1.414 else 1.0
                ant.moveTo(move, moveCost)

                val reachedLocation = allLocations.find { it.marker.position == ant.currentPosition }
                if (reachedLocation != null && capacities.getOrDefault(reachedLocation, 0) > 0) {
                    ant.hasSettled = true
                    capacities[reachedLocation] = capacities.getValue(reachedLocation) - 1
                }
            }
            if (!ant.hasSettled) ant.hasSettled = true
        }
    }

    private fun chooseNextMove(ant: Ant, capacities: Map<CoworkingLocation, Int>): Point? {
        val neighbors = getWalkableNeighbors(ant.currentPosition).filter { it !in ant.visited }
        if (neighbors.isEmpty()) return null

        val probabilities = mutableMapOf<Point, Double>()
        var totalDesirability = 0.0

        for (neighbor in neighbors) {
            val pheromone = pheromones.getOrDefault(Pair(ant.currentPosition, neighbor), 1.0).pow(alpha)
            val heuristic = calculateHeuristic(neighbor, capacities).pow(beta)
            val desirability = pheromone * heuristic

            probabilities[neighbor] = desirability
            totalDesirability += desirability
        }

        if (totalDesirability == 0.0) return neighbors.randomOrNull()

        val randomValue = Random.nextDouble() * totalDesirability
        var cumulative = 0.0
        for ((point, prob) in probabilities) {
            cumulative += prob
            if (randomValue <= cumulative) return point
        }
        return neighbors.lastOrNull()
    }

    private fun calculateHeuristic(point: Point, capacities: Map<CoworkingLocation, Int>): Double {
        var totalScent = 0.0
        capacities.forEach { (loc, capacity) ->
            if (capacity > 0) {
                val dist = getDistance(point, loc.marker.position)
                if (dist > 0) {
                    totalScent += loc.comfort / dist
                }
            }
        }
        return totalScent + 0.1
    }

    private fun getDistance(from: Point, to: Point): Double {
        return distanceCache.getOrPut(from to to) {
            astar.calculatePath(from, to).takeIf { it > 0 } ?: Double.MAX_VALUE
        }
    }

    private fun updatePheromones(ants: List<Ant>) {
        pheromones.forEach { (edge, value) -> pheromones[edge] = value * (1 - evaporationRate) }

        ants.filter { it.hasSettled && it.pathCost > 0 && allLocations.any { loc -> loc.marker.position == it.currentPosition } }.forEach { ant ->
            val location = allLocations.first { it.marker.position == ant.currentPosition }
            val pheromoneToAdd = pheromoneDeposit * (location.comfort / ant.pathCost)
            depositOnPath(ant.path, pheromoneToAdd)
        }

        globalBestResult?.paths?.values?.forEach { path ->
            val pathCost = path.size
            if (pathCost > 0) {
                val pheromoneToAdd = pheromoneDeposit * elitismFactor / pathCost
                depositOnPath(path, pheromoneToAdd)
            }
        }
    }

    private fun depositOnPath(path: List<Point>, amount: Double) {
        for (i in 0 until path.size - 1) {
            val edge = Pair(path[i], path[i + 1])
            val reverseEdge = Pair(path[i + 1], path[i])
            pheromones[edge] = pheromones.getOrDefault(edge, 0.0) + amount
            pheromones[reverseEdge] = pheromones.getOrDefault(reverseEdge, 0.0) + amount
        }
    }

    private fun evaluateSolution(result: AntColonyResult): Double {
        val comfortScore = result.distribution.entries.sumOf { (loc, count) -> loc.comfort * count }
        val penalty = result.unassignedStudents * 200.0
        return comfortScore - penalty
    }

    private fun buildResultFromAnts(ants: List<Ant>): AntColonyResult {
        val distribution = mutableMapOf<CoworkingLocation, Int>()
        val paths = mutableMapOf<CoworkingLocation, List<Point>>()
        val settledAnts = ants.filter { it.hasSettled && allLocations.any { loc -> loc.marker.position == it.currentPosition } }

        settledAnts.forEach { ant ->
            val location = allLocations.first { it.marker.position == ant.currentPosition }
            distribution[location] = distribution.getOrDefault(location, 0) + 1
            if (!paths.containsKey(location)) paths[location] = ant.path
        }
        return AntColonyResult(distribution, paths, studentCount - settledAnts.size)
    }

    private fun getWalkableNeighbors(point: Point): List<Point> =
        listOf(Point(0,1), Point(0,-1), Point(1,0), Point(-1,0),
            Point(1,1), Point(1,-1), Point(-1,1), Point(-1,-1))
            .map { Point(point.x + it.x, point.y + it.y) }
            .filter { grid.isWalkable(it) }

    fun getPheromoneHeatmap(): Map<Point, Double> {
        val heatmap = mutableMapOf<Point, Double>().withDefault { 0.0 }
        pheromones.forEach { (edge, value) ->
            heatmap[edge.first] = heatmap.getValue(edge.first) + value
            heatmap[edge.second] = heatmap.getValue(edge.second) + value
        }
        return heatmap
    }
}