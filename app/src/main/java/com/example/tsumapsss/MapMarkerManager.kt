package com.example.tsumapsss

import android.graphics.Matrix
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import com.github.chrisbanes.photoview.PhotoView

data class MapMarker(
    val name: String,
    val x: Float,
    val y: Float,
    val type: MarkerType
)

enum class MarkerType {
    BUILDING,
    PARK,
    ROAD,
    UNIVERSITY,
    VENDING,
    SHOP
}

class MapMarkerManager(
    private val photoView: PhotoView,
    private val markersContainer: FrameLayout,
    private val mapGrid: MapGrid
) {

    private val markers = mutableListOf<MapMarker>()
    private val markerViews = mutableListOf<ImageView>()

    fun addMarker(marker: MapMarker) {
        markers.add(marker)

        val markerView = ImageView(markersContainer.context).apply {
            setImageResource(getMarkerIcon(marker.type))

            val size = dpToPx(40)
            layoutParams = FrameLayout.LayoutParams(size, size)

            setOnClickListener {
                showInfo(marker.name)
            }
        }

        markersContainer.addView(markerView)
        markerViews.add(markerView)

        markerView.post {
            updateMarkerPosition(markerView, marker)
        }
    }

    fun updateAllMarkersPosition() {
        for (i in markers.indices) {
            updateMarkerPosition(markerViews[i], markers[i])
        }
    }

    private fun updateMarkerPosition(markerView: ImageView, marker: MapMarker) {
        val containerWidth = markersContainer.width
        val containerHeight = markersContainer.height

        if (containerWidth > 0 && containerHeight > 0) {
            val mapPixelX = marker.x * mapGrid.getWidth()
            val mapPixelY = marker.y * mapGrid.getHeight()

            val matrix = Matrix()
            photoView.getSuppMatrix(matrix)
            val values = FloatArray(9)
            photoView.imageMatrix.getValues(values)

            val scale  = values[Matrix.MSCALE_X]
            val transX = values[Matrix.MTRANS_X]
            val transY = values[Matrix.MTRANS_Y]

            val screenX = mapPixelX * scale + transX
            val screenY = mapPixelY * scale + transY

            markerView.translationX = screenX - dpToPx(20)
            markerView.translationY = screenY - dpToPx(20)
        }
    }

    fun getMarkerPixelPosition(marker: MapMarker): Pair<Int, Int> {
        return mapGrid.markerToPixel(marker.x, marker.y)
    }

    fun isMarkerOnWalkablePath(marker: MapMarker): Boolean {
        val (pixelX, pixelY) = getMarkerPixelPosition(marker)
        return mapGrid.isWalkable(pixelX, pixelY)
    }

    private fun getMarkerIcon(type: MarkerType): Int {
        return when (type) {
            MarkerType.BUILDING    -> android.R.drawable.ic_menu_edit
            MarkerType.PARK        -> android.R.drawable.ic_menu_gallery
            MarkerType.ROAD        -> android.R.drawable.ic_menu_directions
            MarkerType.UNIVERSITY  -> android.R.drawable.ic_menu_info_details
            MarkerType.VENDING     -> android.R.drawable.ic_menu_manage
            MarkerType.SHOP        -> android.R.drawable.ic_menu_view
        }
    }

    private fun showInfo(name: String) {
        Toast.makeText(markersContainer.context, name, Toast.LENGTH_SHORT).show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * markersContainer.context.resources.displayMetrics.density).toInt()
    }
}
