package com.example.tsumaps.domain.models

import com.example.tsumaps.domain.map.MapMarker

data class ClusteringResult(
    val clusters: Map<Int, List<MapMarker>>
)


data class ClusteringComparisonResult(
    val euclideanResult: ClusteringResult,
    val aStarResult: ClusteringResult,
    val changedMarkers: Map<MapMarker, Pair<Int, Int>>
)