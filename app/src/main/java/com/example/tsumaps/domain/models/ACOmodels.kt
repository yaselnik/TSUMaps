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