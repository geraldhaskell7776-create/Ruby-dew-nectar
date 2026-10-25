package com.rubydew.nectar.ui.species

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.rubydew.nectar.domain.Region
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/** Center of the lower 48 states, used before a ZIP code is set. */
private val US_CENTER = GeoPoint(39.5, -98.35)

/** Free OpenStreetMap view with one pin per logged sighting. No photos involved. */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun SightingMap(
    region: Region?,
    sightings: List<SightingItem>,
    onMapReady: (MapView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(3.5)
            controller.setCenter(US_CENTER)
            // Let the map handle drags instead of the scrolling page around it.
            setOnTouchListener { v, _ ->
                v.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
        }
    }

    LaunchedEffect(region) {
        if (region != null) {
            mapView.controller.setZoom(if (region == Region.HAWAII) 7.0 else 5.5)
            mapView.controller.setCenter(GeoPoint(region.centerLat, region.centerLon))
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView.also(onMapReady) },
        update = { map ->
            map.overlays.removeAll { it is Marker }
            sightings.forEach { s ->
                map.overlays.add(
                    Marker(map).apply {
                        position = GeoPoint(s.latitude, s.longitude)
                        title = s.speciesName
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                )
            }
            map.invalidate()
        },
        modifier = modifier,
    )
}
