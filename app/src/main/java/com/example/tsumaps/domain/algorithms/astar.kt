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
    fun isWalkable(point: Point): Boolean
}

class Astar(val grid: MapGrid) {

    fun calculatePath(start: Point?, end: Point?): Double {
        val cellPath = findCellPath(start, end)
        if (cellPath.size < 2) return 0.0
        val scale = grid.pathScale.coerceAtLeast(1).toDouble()
        var dist = 0.0
        for (i in 0 until cellPath.size - 1) {
            val dx = abs(cellPath[i + 1].x - cellPath[i].x)
            val dy = abs(cellPath[i + 1].y - cellPath[i].y)
            dist += if (dx != 0 && dy != 0) scale * sqrt(2.0) else scale * 1.0
        }
        return dist
    }

    fun findPath(start: Point?, end: Point?): List<Point> {
        val cellPath = findCellPath(start, end)
        if (cellPath.isEmpty()) return emptyList()
        return cellPath.map { grid.cellToPixelCenter(it) }
    }

    private fun findCellPath(start: Point?, end: Point?): List<Point> {
        if (start == null || end == null) return emptyList()

        val startCell = grid.nearestWalkableCellFromPixel(start) ?: return emptyList()
        val endCell = grid.nearestWalkableCellFromPixel(end) ?: return emptyList()

        if (!grid.isWalkableCell(startCell) || !grid.isWalkableCell(endCell)) return emptyList()

        val queue = PriorityQueue<Cell>(compareBy { it.f })
        val visited = mutableSetOf<Point>()
        val nodes = mutableMapOf<Point, Cell>()

        val startCellNode = Cell(startCell, 0.0, heuristic(startCell, endCell), null, true)
        queue.add(startCellNode)
        nodes[startCell] = startCellNode

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

        while (!queue.isEmpty()) {
            val current = queue.poll()
            val currentPoint = current.point

            if (currentPoint in visited) continue

            if (currentPoint == endCell) {
                return reconstructPath(nodes, currentPoint)
            }

            visited.add(currentPoint)

            for (dir in directions) {
                val neighborPoint = Point(currentPoint.x + dir.x, currentPoint.y + dir.y)

                if (neighborPoint in visited) continue

                if (!grid.isWalkableCell(neighborPoint)) continue

                if (dir.x != 0 && dir.y != 0) {
                    val first = Point(currentPoint.x, currentPoint.y + dir.y)
                    val second = Point(currentPoint.x + dir.x, currentPoint.y)
                    if (!grid.isWalkableCell(first) || !grid.isWalkableCell(second)) continue
                }

                val newG = current.g + if (dir.x * dir.x == 0) 1.0 else sqrt(2.0)
                val neighborNode = nodes.getOrPut(neighborPoint) { Cell(neighborPoint) }

                if (newG < neighborNode.g) {
                    neighborNode.parent = currentPoint
                    neighborNode.g = newG
                    neighborNode.h = heuristic(neighborPoint, endCell)
                    neighborNode.f = neighborNode.g + neighborNode.h

                    queue.add(neighborNode)
                }
            }
        }

        return emptyList()
    }

    fun reconstructPath(nodes: Map<Point, Cell>, end: Point): List<Point> {
        val path = mutableListOf<Point>()
        var current: Point? = end

        while (current != null) {
            path.add(current)
            current = nodes[current]?.parent
        }

        return path.reversed()
    }

    fun heuristic(a: Point, b: Point): Double {
        val dx = abs(a.x - b.x)
        val dy = abs(a.y - b.y)

        return max(dx, dy) + (sqrt(2.0) - 1) * min(dx, dy)
    }
}
