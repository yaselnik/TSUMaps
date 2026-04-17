package com.example.tsumaps.domain.models

data class PlaceStorage(
    var places: MutableList<Place> = mutableListOf(),
    var goods: MutableSet<String> = mutableSetOf(),
    var attractions: MutableList<Attraction> = mutableListOf(),
) {
    fun addPlace(place: Place) {
        places.add(place)
        goods.addAll(place.menu)
    }

    fun addAttraction(attraction: Attraction) {
        attractions.add(attraction)
    }
}
