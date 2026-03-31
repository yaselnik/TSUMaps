package com.example.tsumapsss

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.github.chrisbanes.photoview.PhotoView

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: PhotoView
    private lateinit var markersContainer: FrameLayout
    private lateinit var mapGrid: MapGrid
    private lateinit var markerManager: MapMarkerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapPhotoView)
        markersContainer = findViewById(R.id.markersContainer)

        mapGrid = MapGrid(this)
        mapGrid.buildFromImage(R.drawable.map_for_grid)

        markerManager = MapMarkerManager(mapView, markersContainer, mapGrid)

        addAllMarkers()

        mapView.setOnScaleChangeListener { _, _, _ ->
            markerManager.updateAllMarkersPosition()
        }

        mapView.setOnMatrixChangeListener {
            markerManager.updateAllMarkersPosition()
        }
    }

    private fun addAllMarkers() {
        markerManager.addMarker(MapMarker("Университет", 0.50f, 0.50f, MarkerType.UNIVERSITY))
        markerManager.addMarker(MapMarker("Главный корпус", 0.35f, 0.45f, MarkerType.BUILDING))
        markerManager.addMarker(MapMarker("Библиотека", 0.55f, 0.40f, MarkerType.BUILDING))
        markerManager.addMarker(MapMarker("Спорткомплекс", 0.60f, 0.65f, MarkerType.BUILDING))
        markerManager.addMarker(MapMarker("Столовая", 0.45f, 0.55f, MarkerType.BUILDING))
        markerManager.addMarker(MapMarker("Парк", 0.20f, 0.70f, MarkerType.PARK))
        markerManager.addMarker(MapMarker("Магазин", 0.42f, 0.58f, MarkerType.SHOP))
        markerManager.addMarker(MapMarker("Кофейный автомат", 0.48f, 0.52f, MarkerType.VENDING))
    }
}