package com.example.tsumaps.domain.models

import java.time.LocalTime


data class Place(
    val id: Int,
    val name: String,
    val location: Point,
    val menu: Set<String>,
    val openingAt: LocalTime,
    val closingAt: LocalTime
)

