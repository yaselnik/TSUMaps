package com.example.tsumaps.domain.algorithms
//
//import android.content.Context
//import android.graphics.*
//import android.os.Bundle
//import android.util.AttributeSet
//import android.view.View
//import android.widget.Button
//import android.widget.EditText
//import android.widget.TextView
//import androidx.fragment.app.Fragment
//import com.example.tsumaps.R
//import com.example.tsumaps.domain.algorithms.Astar
//import com.example.tsumaps.domain.algorithms.Grid
//import kotlin.math.abs
//import kotlin.math.sqrt
//
//data class ClusterPoint(
//    val x: Double,
//    val y: Double,
//    val name: String= ""
//)
//
//data class Cluster(
//    val id: Int,
//    val centroid: ClusterPoint,
//    val points: MutableList<ClusterPoint> = mutableListOf(),
//    val color: Int
//)
//
//data class ClusterResult(
//    val metricName: String,
//    val clusters: List<Clusters>
//)
//
//interface DistanceMetric {
//    fun name(): String
//    fun distance(p1: ClusterPoint, p2: ClusterPoint): Double
//}
//
//class EuclidDistance : DistanceMetric {
//    override fun name() = "Евклидова"
//    override fun distance(p1: ClusterPoint, p2: ClusterPoint): Double {
//       val dx = p1.x - p2.x
//       val dy = p1.y - p2.y
//       return sqrt(dx * dx + dy * dy)
//    }
//}
//
//class ManhattanDistance : DistanceMetric {
//    override fun name() = "Манхэттенская"
//    override fun distance(p1: ClusterPoint, p2: ClusterPoint): Double {
//        return abs(p1.x - p2.x) + abs(p1.y - p2.y)
//    }
//}
//
//class AstarDistance(
//    private val grid: Grid?,
//    private val astar: Astar?
//) : DistanceMetric {
//    override fun name() = "A*"
//    override fun distance(p1: ClusterPoint, p2: ClusterPoint): Double {
//        if (grid == null || astar == null) {
//            return abs(p1.x - p2.x) + abs(p1.y - p2.y)
//        }
//
//        val start = com.example.tsumaps.domain.models.Point(p1.x.toInt(), p1.y.toInt())
//        val end = com.example.tsumaps.domain.models.Point(p2.x.toInt(), p2.y.toInt())
//
//        if (!grid.isWalkable(start) || !grid.isWalkable(end)) {
//            return (abs(p1.x - p2.x) + abs(p1.y - p2.y)) * 1.5
//        }
//
//        val path = astar.findPath(start, end)
//
//        if (path.isEmpty()) {
//            return (abs(p1.x - p2.x) + abs(p1.y - p2.y)) * 2.0
//        }
//
//        var length = 0.0
//        for (i in 0 until path.size - 1) {
//            val dx = abs(path[i + 1].x - path[i].x)
//            val dy = abs(path[i + 1].y - path[i].y)
//
//            if (dx != 0 && dy != 0) {
//                length += sqrt(2.0)
//            } else {
//                length += 1.0
//            }
//        }
//
//        return length
//    }
//}