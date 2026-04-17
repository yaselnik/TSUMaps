package com.example.tsumaps.ui

import com.example.tsumaps.domain.models.Place
import java.time.LocalTime
import kotlin.math.absoluteValue

fun isPlaceOpenNow(place: Place, now: LocalTime = LocalTime.now()): Boolean {
    return !now.isBefore(place.openingAt) && now.isBefore(place.closingAt)
}

fun getPlaceTimingInfo(place: Place, now: LocalTime = LocalTime.now()): String {
    val openNow = isPlaceOpenNow(place, now)
    return if (openNow) {
        val minutes = minutesUntil(now, place.closingAt)
        "Открыт(а), закроется через ${formatMinutes(minutes)}"
    } else {
        val minutes = if (now.isBefore(place.openingAt)) {
            minutesUntil(now, place.openingAt)
        } else {
            minutesUntil(now, place.openingAt.plusHours(24))
        }
        "Закрыт(а), откроется через ${formatMinutes(minutes)}"
    }
}

private fun minutesUntil(from: LocalTime, to: LocalTime): Int {
    val fromMinutes = from.hour * 60 + from.minute
    val toMinutes = to.hour * 60 + to.minute
    var diff = toMinutes - fromMinutes
    if (diff < 0) diff += 24 * 60
    return diff
}

private fun formatMinutes(totalMinutes: Int): String {
    val minutes = totalMinutes.absoluteValue
    val hoursPart = minutes / 60
    val minutePart = minutes % 60
    return if (hoursPart > 0) {
        "${hoursPart}ч ${minutePart}м"
    } else {
        "${minutePart}м"
    }
}
