package com.example.tsumaps.domain.models

data class Sample(
    val location: String,
    val budget: String,
    val timeAvailable: String,
    val foodType: String,
    val queueTolerance: String,
    val weather: String,
    val recommendedPlace: String
)