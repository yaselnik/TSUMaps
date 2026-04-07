package com.example.tsumaps.domain.algorithms

import com.example.tsumaps.domain.models.Point
import com.example.tsumaps.domain.models.Cell
import com.example.tsumaps.domain.map.MapGrid
import java.util.PriorityQueue
import kotlin.math.sqrt
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

interface Grid {
    fun isWalkable(point: Point) : Boolean
}

class Astar (val grid: MapGrid) {
    fun calculatePath(start: Point?, end: Point?) : Double {
        val path = findPath(start, end)
        var dist = 0.0
        for (i in 0..path.size - 1) {
            if (path[i].x * path[i + 1].x != 0 && path[i].y * path[i + 1].y  != 0) {
                dist += sqrt(2.0);
            }
            else dist += 1.0
        }

        return dist
    }

    fun findPath(start: Point?, end: Point?) : List<Point> {
        if (start == null || end == null) return emptyList()
        if (!grid.isWalkable(start) || !grid.isWalkable(end)) return emptyList()

        val queue = PriorityQueue<Cell>(compareBy { it.f })
        val visited = mutableSetOf<Point>()
        val nodes = mutableMapOf<Point, Cell>()

        val startCell = Cell(start, 0.0,
            heuristic(start, end), null, true)
        queue.add(startCell)
        nodes[start] = startCell

        val directions = listOf(
            Point(0, 1),
            Point(0, -1),
            Point(1, 0),
            Point(1, 1),
            Point(1, -1),
            Point(-1, 0),
            Point(-1, 1),
            Point(-1, -1),
        )

        while(!queue.isEmpty()) {
            val current = queue.poll()
            val currentPoint = current.point

            if (currentPoint in visited) continue

            if (currentPoint == end) {
                return reconstructPath(nodes, currentPoint)
            }

            visited.add(currentPoint)

            for (dir in directions) {
                val neighborPoint = Point(currentPoint.x + dir.x, currentPoint.y + dir.y)

                if (neighborPoint in visited) continue;

                if (!grid.isWalkable(neighborPoint)) continue;

                if (dir.x != 0 && dir.y != 0) {
                    val first = Point(currentPoint.x, currentPoint.y + dir.y)
                    val second = Point(currentPoint.x + dir.x, currentPoint.y)
                    if (!grid.isWalkable(first) || !grid.isWalkable(second)) continue
                }

                val newG = current.g + if (dir.x * dir.x == 0) 1.0 else sqrt(2.0)
                val neighborNode = nodes.getOrPut(neighborPoint) { Cell(neighborPoint) }

                if (newG < neighborNode.g) {
                    neighborNode.parent = currentPoint
                    neighborNode.g = newG
                    neighborNode.h = heuristic(neighborPoint, end)
                    neighborNode.f = neighborNode.g + neighborNode.h

                    queue.add(neighborNode)
                }
            }
        }

        return emptyList()
    }

    fun reconstructPath (nodes: Map<Point, Cell>, end: Point): List<Point> {
        val path = mutableListOf<Point>()
        var current: Point? = end

        while (current != null) {
            path.add(current)
            current = nodes[current]?.parent
        }

        return path.reversed()
    }

    fun heuristic (a: Point, b: Point): Double {
        val dx = abs(a.x - b.x)
        val dy = abs(a.y - b.y)

        return max(dx, dy) + (sqrt(2.0) - 1) * min(dx, dy)
    }
}