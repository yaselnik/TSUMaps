package com.example.tsumaps.domain.models

import com.example.tsumaps.domain.map.MapMarker

data class Attraction(
    val marker: MapMarker,
    val comfort: Double = 1.0,
    val capacity: Int = 1
)
