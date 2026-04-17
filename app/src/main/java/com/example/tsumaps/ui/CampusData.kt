package com.example.tsumaps.ui

import android.util.Log
import com.example.tsumaps.domain.map.MapMarker
import com.example.tsumaps.domain.map.MarkerType
import com.example.tsumaps.domain.models.Attraction
import com.example.tsumaps.domain.models.Place
import com.example.tsumaps.domain.models.PlaceStorage
import com.example.tsumaps.domain.models.Point
import java.time.LocalTime

fun populateCampusPlaces(placeStorage: PlaceStorage) {
    placeStorage.addPlace(
        Place(
            id = 1,
            name = "Ярче",
            location = Point(255, 260),
            menu = setOf("Фрукты", "Овощи", "Мясо", "Курица", "Рыба", "Алкоголь"),
            openingAt = LocalTime.of(7, 0),
            closingAt = LocalTime.of(23, 0)
        )
    )
    placeStorage.addPlace(
        Place(
            id = 2,
            name = "Пятёрочка",
            location = Point(715, 1430),
            menu = setOf("Фрукты", "Овощи", "Мясо", "Курица", "Рыба", "Алкоголь"),
            openingAt = LocalTime.of(0, 0),
            closingAt = LocalTime.of(0, 0)
        )
    )
    placeStorage.addPlace(
        Place(
            id = 3,
            name = "Абрикос",
            location = Point(5, 560),
            menu = setOf("Сладкое", "Закуски", "Алкоголь"),
            openingAt = LocalTime.of(8, 0),
            closingAt = LocalTime.of(23, 0)
        )
    )
    placeStorage.addPlace(
        Place(
            id = 4,
            name = "Пилад",
            location = Point(135, 435),
            menu = setOf("Сладкое", "Закуски", "Пельмени"),
            openingAt = LocalTime.of(0, 0),
            closingAt = LocalTime.of(0, 0)
        )
    )
    placeStorage.addPlace(
        Place(
            id = 5,
            name = "Ярче",
            location = Point(835, 540),
            menu = setOf("Кола", "Бутерброд", "Кофе", "Фрукты", "Овощи", "Мясо", "Курица", "Рыба", "Алкоголь"),
            openingAt = LocalTime.of(8, 0),
            closingAt = LocalTime.of(22, 0)
        )
    )
    placeStorage.addPlace(
        Place(
            id = 6,
            name = "Ярче",
            location = Point(900, 1155),
            menu = setOf("Кола", "Бутерброд", "Кофе", "Фрукты", "Овощи", "Мясо", "Курица", "Рыба", "Алкоголь"),
            openingAt = LocalTime.of(7, 0),
            closingAt = LocalTime.of(23, 0)
        )
    )
    placeStorage.addPlace(
        Place(
            id = 7,
            name = "Fix Price",
            location = Point(705, 1430),
            menu = setOf("Ручки", "Карандаши", "Хоз.товары"),
            openingAt = LocalTime.of(9, 0),
            closingAt = LocalTime.of(21, 0)
        )
    )
    placeStorage.addPlace(
        Place(
            id = 8,
            name = "Сибирские блины",
            location = Point(785, 1430),
            menu = setOf("Мясной блин", "Блин с ветчиной и сыром", "Сладкий блин"),
            openingAt = LocalTime.of(0, 0),
            closingAt = LocalTime.of(0, 0)
        )
    )
    placeStorage.addPlace(
        Place(
            id = 9,
            name = "Xo bakery",
            location = Point(255, 890),
            menu = setOf("Кофе"),
            openingAt = LocalTime.of(8, 0),
            closingAt = LocalTime.of(19, 0)
        )
    )
    placeStorage.addPlace(
        Place(
            id = 10,
            name = "Сибирские блины",
            location = Point(360, 765),
            menu = setOf("Мясной блин", "Блин с ветчиной и сыром", "Сладкий блин"),
            openingAt = LocalTime.of(9, 0),
            closingAt = LocalTime.of(20, 0)
        )
    )
    placeStorage.addPlace(
        Place(
            id = 11,
            name = "Столовая ЦК",
            location = Point(360, 765),
            menu = setOf("Первое блюдо", "Второе блюдо"),
            openingAt = LocalTime.of(8, 0),
            closingAt = LocalTime.of(18, 0)
        )
    )
    placeStorage.addPlace(
        Place(
            id = 12,
            name = "Столовая Минутка",
            location = Point(383, 780),
            menu = setOf("Первое блюдо", "Второе блюдо"),
            openingAt = LocalTime.of(8, 0),
            closingAt = LocalTime.of(18, 0)
        )
    )
    placeStorage.addPlace(
        Place(
            id = 13,
            name = "Starbooks",
            location = Point(320, 755),
            menu = setOf("Кофе"),
            openingAt = LocalTime.of(8, 0),
            closingAt = LocalTime.of(18, 0)
        )
    )
    placeStorage.addPlace(
        Place(
            id = 14,
            name = "Сыр-Бор",
            location = Point(315, 855),
            menu = setOf("Первое блюдо", "Второе блюдо"),
            openingAt = LocalTime.of(8, 0),
            closingAt = LocalTime.of(18, 0)
        )
    )
    placeStorage.addPlace(
        Place(
            id = 15,
            name = "Наш",
            location = Point(835, 760),
            menu = setOf("Фрукты", "Овощи", "Мясо", "Курица", "Рыба", "Алкоголь"),
            openingAt = LocalTime.of(0, 0),
            closingAt = LocalTime.of(0, 0)
        )
    )
    placeStorage.addPlace(
        Place(
            id = 16,
            name = "Укромное местечко",
            location = Point(485, 400),
            menu = setOf("Первое блюдо", "Второе блюдо"),
            openingAt = LocalTime.of(9, 0),
            closingAt = LocalTime.of(17, 0)
        )
    )
    placeStorage.addPlace(
        Place(
            id = 17,
            name = "Harat's pub",
            location = Point(304, 290),
            menu = setOf("Алкоголь"),
            openingAt = LocalTime.of(12, 0),
            closingAt = LocalTime.of(2, 0)
        )
    )

    Log.d("PLACES", "Все места: ${placeStorage.places}")
    Log.d("GOODS", "Все товары: ${placeStorage.goods}")
}

fun populateCampusAttractions(placeStorage: PlaceStorage) {
    placeStorage.addAttraction(
        Attraction(
            marker = MapMarker(
                id = 18,
                name = "Флоринский и Менделеев",
                position = Point(487, 796),
                type = MarkerType.ATTRACTION
            ),
            comfort = 1.0,
            capacity = 1
        )
    )
    placeStorage.addAttraction(
        Attraction(
            marker = MapMarker(
                id = 21,
                name = "Памятник павшим за Родину сотрудникам и студентам ТГУ",
                position = Point(517, 897),
                type = MarkerType.ATTRACTION
            ),
            comfort = 1.0,
            capacity = 1
        )
    )
    placeStorage.addAttraction(
        Attraction(
            marker = MapMarker(
                id = 22,
                name = "Арт-объект Это было навсегда, пока не закончилось",
                position = Point(402, 762),
                type = MarkerType.ATTRACTION
            ),
            comfort = 1.0,
            capacity = 1
        )
    )
    placeStorage.addAttraction(
        Attraction(
            marker = MapMarker(
                id = 23,
                name = "Птицы что близ рая летают",
                position = Point(536, 574),
                type = MarkerType.ATTRACTION
            ),
            comfort = 1.0,
            capacity = 1
        )
    )
    placeStorage.addAttraction(
        Attraction(
            marker = MapMarker(
                id = 24,
                name = "Памятник революционерам",
                position = Point(683, 333),
                type = MarkerType.ATTRACTION
            ),
            comfort = 1.0,
            capacity = 1
        )
    )
    placeStorage.addAttraction(
        Attraction(
            marker = MapMarker(
                id = 24,
                name = "Декоративное сооружение Белка и медведь",
                position = Point(841, 243),
                type = MarkerType.ATTRACTION
            ),
            comfort = 1.0,
            capacity = 1
        )
    )
    placeStorage.addAttraction(
        Attraction(
            marker = MapMarker(
                id = 25,
                name = "Памятник Студенчеству Томска",
                position = Point(684, 188),
                type = MarkerType.ATTRACTION
            ),
            comfort = 1.0,
            capacity = 1
        )
    )
    Log.d("ATTRACTIONS", "Все достопримечательности: ${placeStorage.attractions}")
}
