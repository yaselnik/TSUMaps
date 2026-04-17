package com.example.tsumaps.domain.algorithms

import com.example.tsumaps.domain.map.MapGrid
import com.example.tsumaps.domain.models.AntColonyTspResult
import com.example.tsumaps.domain.models.Attraction
import com.example.tsumaps.domain.models.Point
import kotlin.math.pow
import kotlin.random.Random

class AntColonyTsp(
    private val grid: MapGrid,
    private val userStart: Point,
    private val attractions: List<Attraction>,
    private val antCount: Int = 40,
    private val iterations: Int = 120,
    private val alpha: Double = 1.0,
    private val beta: Double = 3.0,
    private val evaporation: Double = 0.5,
    private val depositQ: Double = 100.0,
    private val initialPheromone: Double = 0.05,
    private val elitismWeight: Double = 2.0
) {
    private val astar = Astar(grid)

    fun run(): AntColonyTspResult {
        if (attractions.isEmpty()) {
            return AntColonyTspResult(
                orderedAttractions = emptyList(),
                totalCost = 0.0,
                polyline = emptyList(),
                tourNodeIndices = emptyList(),
                generationsRun = 0
            )
        }

        val start = grid.findNearestWalkable(userStart, 120) ?: userStart
        val nodePoints = ArrayList<Point>(1 + attractions.size).apply {
            add(start)
            attractions.forEach { a ->
                add(grid.findNearestWalkable(a.marker.position, 120) ?: a.marker.position)
            }
        }
        val n = attractions.size
        val nodes = n + 1

        val dist = Array(nodes) { i ->
            DoubleArray(nodes) { j ->
                if (i == j) 0.0
                else edgeLength(nodePoints[i], nodePoints[j])
            }
        }

        val tau = Array(nodes) { DoubleArray(nodes) { initialPheromone } }

        var bestTour: List<Int>? = null
        var bestLen = Double.POSITIVE_INFINITY
        var genDone = 0

        repeat(iterations) { gen ->
            genDone = gen + 1
            val iterationTours = ArrayList<Pair<List<Int>, Double>>(antCount)

            repeat(antCount) {
                val tour = constructTour(n, dist, tau)
                val len = tourLength(tour, dist)
                if (len.isFinite() && len < bestLen) {
                    bestLen = len
                    bestTour = tour.toList()
                }
                iterationTours.add(tour to len)
            }

            for (i in 0 until nodes) {
                for (j in 0 until nodes) {
                    if (i != j) tau[i][j] *= (1.0 - evaporation)
                }
            }

            for ((tour, len) in iterationTours) {
                if (len.isFinite() && len > 0) {
                    deposit(tau, tour, depositQ / len)
                }
            }

            bestTour?.let { deposit(tau, it, elitismWeight * depositQ / bestLen.coerceAtLeast(1e-9)) }
        }

        val tour = bestTour ?: return AntColonyTspResult(
            orderedAttractions = emptyList(),
            totalCost = Double.POSITIVE_INFINITY,
            polyline = emptyList(),
            tourNodeIndices = emptyList(),
            generationsRun = genDone
        )

        val ordered = tour.drop(1).dropLast(1).map { attractions[it - 1] }
        val polyline = buildPolyline(tour, nodePoints)
        return AntColonyTspResult(
            orderedAttractions = ordered,
            totalCost = bestLen,
            polyline = polyline,
            tourNodeIndices = tour,
            generationsRun = genDone
        )
    }

    private fun edgeLength(a: Point, b: Point): Double {
        val way = astar.findPath(a, b)
        if (way.size < 2) return Double.POSITIVE_INFINITY
        return astar.calculatePath(a, b).coerceAtLeast(1e-6)
    }

    private fun constructTour(
        nAttr: Int,
        dist: Array<DoubleArray>,
        tau: Array<DoubleArray>
    ): List<Int> {
        val unvisited = (1..nAttr).toMutableSet()
        val tour = mutableListOf(0)
        var current = 0
        while (unvisited.isNotEmpty()) {
            val candidates = unvisited.toList()
            val weights = candidates.map { j ->
                val d = dist[current][j]
                val t = tau[current][j].coerceAtLeast(1e-12)
                val comfort = attractions[j - 1].comfort.coerceIn(0.01, 100.0)
                val eta = if (d.isFinite() && d < 1e200) comfort / d else 0.0
                t.pow(alpha) * eta.pow(beta)
            }
            val sum = weights.sum()
            val next = if (sum <= 0 || weights.any { it.isNaN() }) {
                candidates.random()
            } else {
                var r = Random.nextDouble() * sum
                var pick = candidates[0]
                for (idx in candidates.indices) {
                    r -= weights[idx]
                    if (r <= 0) {
                        pick = candidates[idx]
                        break
                    }
                }
                pick
            }
            tour.add(next)
            unvisited.remove(next)
            current = next
        }
        tour.add(0)
        return tour
    }

    private fun tourLength(tour: List<Int>, dist: Array<DoubleArray>): Double {
        var s = 0.0
        for (k in 0 until tour.size - 1) {
            val leg = dist[tour[k]][tour[k + 1]]
            if (leg.isInfinite()) return Double.POSITIVE_INFINITY
            s += leg
        }
        return s
    }

    private fun deposit(tau: Array<DoubleArray>, tour: List<Int>, amount: Double) {
        if (amount.isNaN() || amount <= 0) return
        for (k in 0 until tour.size - 1) {
            val i = tour[k]
            val j = tour[k + 1]
            tau[i][j] += amount
            tau[j][i] += amount
        }
    }

    private fun buildPolyline(tour: List<Int>, nodePoints: List<Point>): List<Point> {
        val full = mutableListOf<Point>()
        for (k in 0 until tour.size - 1) {
            val a = nodePoints[tour[k]]
            val b = nodePoints[tour[k + 1]]
            val segment = astar.findPath(a, b)
            if (segment.isEmpty()) continue
            if (full.isEmpty()) full.addAll(segment) else full.addAll(segment.drop(1))
        }
        return full
    }
}
