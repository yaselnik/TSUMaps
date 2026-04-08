package com.example.tsumaps.domain.algorithms

import android.util.Log
import com.example.tsumaps.domain.map.MapGrid
import com.example.tsumaps.domain.map.MapMarker
import com.example.tsumaps.domain.models.ClusteringComparisonResult
import com.example.tsumaps.domain.models.ClusteringResult
import com.example.tsumaps.domain.models.Point
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

class ClusteringAlgorithm(
    private val markers: List<MapMarker>,
    private val grid: MapGrid
) {
    private val astar = Astar(grid)
    private val aStarDistanceMatrix = mutableMapOf<Pair<Point, Point>, Double>()

    init {
        Log.d("Clustering", "Начинаю предрасчет A* расстояний для ${markers.size} маркеров...")
        for (i in markers.indices) {
            for (j in i + 1 until markers.size) {
                val p1 = markers[i].position
                val p2 = markers[j].position

                val walkableP1 = grid.findNearestWalkable(p1, 100)
                val walkableP2 = grid.findNearestWalkable(p2, 100)

                if (walkableP1 != null && walkableP2 != null) {
                    val distance = astar.calculatePath(walkableP1, walkableP2)
                    aStarDistanceMatrix[p1 to p2] = distance
                    aStarDistanceMatrix[p2 to p1] = distance
                } else {
                    aStarDistanceMatrix[p1 to p2] = Double.MAX_VALUE
                    aStarDistanceMatrix[p2 to p1] = Double.MAX_VALUE
                    Log.w("Clustering", "Не удалось найти проходимый путь между ${markers[i].name} и ${markers[j].name}")
                }
            }
        }
        Log.d("Clustering", "Предрасчет A* расстояний завершен.")
    }

    private fun euclideanDistance(p1: Point, p2: Point): Double {
        return sqrt((p1.x - p2.x).toDouble().pow(2) + (p1.y - p2.y).toDouble().pow(2))
    }

    private fun getAStarDistance(p1: Point, p2: Point): Double {
        if (p1 == p2) return 0.0
        return aStarDistanceMatrix[p1 to p2] ?: Double.MAX_VALUE
    }

    fun performClusteringComparison(k: Int): ClusteringComparisonResult {
        if (markers.size < k) {
            Log.e("Clustering", "Количество маркеров (${markers.size}) меньше, чем k ($k).")
            val emptyResult = ClusteringResult(emptyMap())
            return ClusteringComparisonResult(emptyResult, emptyResult, emptyMap())
        }

        Log.d("Clustering", "Запуск кластеризации с евклидовой метрикой...")
        val euclideanResult = runKMeans(k) { p1, p2 -> euclideanDistance(p1, p2) }

        Log.d("Clustering", "Запуск кластеризации с A* метрикой...")
        val aStarResult = runKMeans(k) { p1, p2 -> getAStarDistance(p1, p2) }

        Log.d("Clustering", "Сравнение результатов...")
        val changedMarkers = findChangedMarkers(euclideanResult, aStarResult)

        return ClusteringComparisonResult(euclideanResult, aStarResult, changedMarkers)
    }

    private fun runKMeans(
        k: Int,
        maxIterations: Int = 100,
        distanceMetric: (Point, Point) -> Double
    ): ClusteringResult {
        var centroids = markers.shuffled(Random).take(k).map { it.position }
        var assignments = mutableMapOf<Int, MutableList<MapMarker>>()

        for (i in 0 until maxIterations) {
            val newAssignments = mutableMapOf<Int, MutableList<MapMarker>>()
            for (idx in 0 until k) { newAssignments[idx] = mutableListOf() }

            for (marker in markers) {
                val closestCentroidIndex = centroids.indices.minByOrNull {
                    distanceMetric(marker.position, centroids[it])
                } ?: 0
                newAssignments[closestCentroidIndex]?.add(marker)
            }

            if (assignments.toString() == newAssignments.toString()) {
                Log.d("KMeans", "Алгоритм сошелся на итерации $i")
                break
            }
            assignments = newAssignments

            val newCentroids = mutableListOf<Point>()
            for (clusterIndex in 0 until k) {
                val clusterMarkers = assignments[clusterIndex]
                if (clusterMarkers.isNullOrEmpty()) {
                    newCentroids.add(markers.random(Random).position)
                } else {
                    val avgX = clusterMarkers.map { it.position.x }.average().toInt()
                    val avgY = clusterMarkers.map { it.position.y }.average().toInt()
                    newCentroids.add(Point(avgX, avgY))
                }
            }
            centroids = newCentroids
        }

        return ClusteringResult(assignments)
    }

    private fun findChangedMarkers(
        result1: ClusteringResult,
        result2: ClusteringResult
    ): Map<MapMarker, Pair<Int, Int>> {
        val map1 = result1.clusters.flatMap { (id, markers) -> markers.map { it to id } }.toMap()
        val map2 = result2.clusters.flatMap { (id, markers) -> markers.map { it to id } }.toMap()

        val changed = mutableMapOf<MapMarker, Pair<Int, Int>>()
        for (marker in markers) {
            val cluster1 = map1[marker]
            val cluster2 = map2[marker]
            if (cluster1 != null && cluster2 != null && cluster1 != cluster2) {
                changed[marker] = cluster1 to cluster2
            }
        }
        return changed
    }
}