package com.example.tsumaps.domain.models

import com.example.tsumaps.domain.map.MapMarker

data class CoworkingLocation(
    val marker: MapMarker,
    val capacity: Int,
    val comfort: Double
)

data class AntColonyResult(
    val distribution: Map<CoworkingLocation, Int>,
    val paths: Map<CoworkingLocation, List<Point>>,
    val unassignedStudents: Int
)

data class AntColonyTspResult(
    val orderedAttractions: List<Attraction>,
    val totalCost: Double,
    val polyline: List<Point>,
    val tourNodeIndices: List<Int>,
    val generationsRun: Int
) {
    fun summaryRu(): String = buildString {
        if (orderedAttractions.isEmpty()) {
            append("Нет маршрута (нет точек или нет пути по карте).")
            return@buildString
        }
        append("Муравьиный алгоритм (TSP), длина ≈ ")
        append("%.1f".format(totalCost))
        append("\nПорядок обхода: ")
        append(orderedAttractions.joinToString(" → ") { it.marker.name })
        append("\nПоколений: ").append(generationsRun)
    }
}