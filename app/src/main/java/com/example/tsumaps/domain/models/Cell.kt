package com.example.tsumaps.domain.models

data class Cell(
    val point: Point,
    var g: Double = Double.POSITIVE_INFINITY,
    var h: Double = 0.0,
    var parent: Point? = null,
    var isWalkable: Boolean = false
) {
    var f: Double = g + h
        set(value) {
            field = value
        }

    fun updateF() {
        f = g + h
    }
}