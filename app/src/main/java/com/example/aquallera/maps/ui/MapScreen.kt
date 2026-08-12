package com.example.aquallera.maps.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aquallera.R
import com.example.aquallera.components.LoadingIndicator
import com.example.aquallera.components.WaterStationCard
import com.example.aquallera.core.model.WaterStation
import com.example.aquallera.core.theme.AppBg
import com.example.aquallera.core.theme.MidnightBlue
import com.example.aquallera.core.util.Constants
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import android.util.Log
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.math.abs

/** Map tab: Mapbox map with station markers, status filter and a bottom station panel. */
@Composable
fun MapScreen(
    onStationTap: (stationId: String) -> Unit = {},
    onOrderNow: (stationId: String) -> Unit = {},
    viewModel: MapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var panelCollapseCounter by remember { mutableStateOf(0) }
    var flyToUserTrigger by remember { mutableStateOf(0) }

    val onUserLocationResult: (Double, Double) -> Unit = { lat, lng ->
        viewModel.setUserLocation(lat, lng)
        flyToUserTrigger++
    }

    val onUserLocationError: (String) -> Unit = { message ->
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions.all { it.value }) {
            getUserLocation(context, onUserLocationResult, onUserLocationError)
        } else {
            onUserLocationError("Location permission is needed to show where you are.")
        }
    }

    if (uiState.loading) {
        LoadingIndicator(message = "Loading stations...")
        return
    }

    uiState.error?.let { error ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = error,
                color = MidnightBlue.copy(alpha = 0.7f),
                fontSize = 16.sp,
            )
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapboxMapView(
            stations = uiState.stations,
            onStationSelected = { station -> viewModel.selectStation(station) },
            selectedStation = uiState.selectedStation,
            onDismissStation = { viewModel.selectStation(null) },
            flyToStationId = uiState.flyToStationId,
            onFlyToCompleted = { viewModel.clearFlyToStation() },
            flyToUserTrigger = flyToUserTrigger,
            userLat = uiState.userLat,
            userLng = uiState.userLng,
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(start = 12.dp, end = 12.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Spacer(modifier = Modifier.width(4.dp))
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.logo_no_name),
                    contentDescription = "Aquallera Logo",
                    modifier = Modifier.size(48.dp),
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = "Aquallera",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MidnightBlue,
                    )
                    Text(
                        text = "\"Clean Water, Anytime, Anywhere.\"",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 72.dp, end = 16.dp),
        ) {
            LocationButton(
                onClick = {
                    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (hasFine == PackageManager.PERMISSION_GRANTED || hasCoarse == PackageManager.PERMISSION_GRANTED) {
                        getUserLocation(context, onUserLocationResult, onUserLocationError)
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            )
                        )
                    }
                },
            )
        }

        uiState.selectedStation?.let { station ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 180.dp, start = 16.dp, end = 16.dp)
                    .widthIn(max = 260.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(0.5.dp, Color.LightGray, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = station.stationName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MidnightBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (station.address.isNotBlank()) {
                            Text(
                                text = station.address,
                                fontSize = 10.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "View",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MidnightBlue)
                            .clickable { onStationTap(station.id) }
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "\u2715",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clickable { viewModel.selectStation(null) }
                            .padding(4.dp),
                    )
                }
            }
        }

        StationBottomPanel(
            stations = uiState.filteredStations,
            statusFilter = uiState.statusFilter,
            userLat = uiState.userLat,
            userLng = uiState.userLng,
            onStationTap = onStationTap,
            onFilterChange = { viewModel.setFilter(it) },
            onViewOnMap = { station ->
                panelCollapseCounter++
                viewModel.selectStation(station)
                viewModel.flyToStation(station.id)
            },
            onOrderNow = { onOrderNow(it) },
            collapseCounter = panelCollapseCounter,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun LocationButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, Color.LightGray, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MidnightBlue),
            )
            repeat(4) { i ->
                val angle = i * 90.0
                Box(
                    modifier = Modifier
                        .size(1.dp, 6.dp)
                        .background(MidnightBlue)
                        .align(
                            when (i) {
                                0 -> Alignment.TopCenter
                                1 -> Alignment.CenterEnd
                                2 -> Alignment.BottomCenter
                                else -> Alignment.CenterStart
                            }
                        ),
                )
            }
        }
    }
}

@Composable
private fun StationFilterDropdown(
    selected: StationFilter,
    onSelect: (StationFilter) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selected.label,
                fontSize = 12.sp,
                color = MidnightBlue,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "\u25BC", fontSize = 8.sp, color = MidnightBlue)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            StationFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = filter.label,
                            fontSize = 13.sp,
                            color = if (filter == selected) MidnightBlue else Color.DarkGray,
                            fontWeight = if (filter == selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onSelect(filter)
                        expanded = false
                    },
                )
            }
        }
    }
}


@Composable
private fun StationBottomPanel(
    stations: List<WaterStation>,
    statusFilter: StationFilter,
    userLat: Double?,
    userLng: Double?,
    onStationTap: (String) -> Unit,
    onFilterChange: (StationFilter) -> Unit,
    onViewOnMap: (WaterStation) -> Unit = {},
    onOrderNow: (String) -> Unit = {},
    collapseCounter: Int = 0,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp

    val fullHeight = screenHeightDp * 0.85f
    val halfHeight = fullHeight * 0.6f
    val peekHeight = 60.dp

    var sheetHeight by remember { mutableStateOf(peekHeight) }

    LaunchedEffect(collapseCounter) {
        if (collapseCounter > 0) {
            sheetHeight = peekHeight
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(sheetHeight)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(AppBg)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val currentPx = with(density) { sheetHeight.toPx() }
                        val newPx = (currentPx - dragAmount).coerceIn(
                            with(density) { peekHeight.toPx() },
                            with(density) { fullHeight.toPx() },
                        )
                        sheetHeight = with(density) { newPx.toDp() }
                    },
                    onDragEnd = {
                        val currentPx = with(density) { sheetHeight.toPx() }
                        val anchors = listOf(
                            with(density) { peekHeight.toPx() } to peekHeight,
                            with(density) { halfHeight.toPx() } to halfHeight,
                            with(density) { fullHeight.toPx() } to fullHeight,
                        )
                        sheetHeight = anchors.minBy { abs(it.first - currentPx) }.second
                    },
                    onDragCancel = {
                        val currentPx = with(density) { sheetHeight.toPx() }
                        val anchors = listOf(
                            with(density) { peekHeight.toPx() } to peekHeight,
                            with(density) { halfHeight.toPx() } to halfHeight,
                            with(density) { fullHeight.toPx() } to fullHeight,
                        )
                        sheetHeight = anchors.minBy { abs(it.first - currentPx) }.second
                    },
                )
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.LightGray),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "All Water Stations",
                color = MidnightBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            StationFilterDropdown(
                selected = statusFilter,
                onSelect = onFilterChange,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            if (stations.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No stations match this filter",
                            color = MidnightBlue.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                        )
                    }
                }
            } else {
                items(stations, key = { it.id }) { station ->
                    WaterStationCard(
                    station = station,
                    userLat = userLat,
                    userLng = userLng,
                    onTap = { onStationTap(station.id) },
                    onViewOnMap = { onViewOnMap(station) },
                    onOrderNow = { onOrderNow(station.id) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
                }
            }
        }
    }
}

@Composable
private fun MapboxMapView(
    stations: List<WaterStation>,
    onStationSelected: (WaterStation) -> Unit,
    selectedStation: WaterStation? = null,
    onDismissStation: () -> Unit = {},
    flyToStationId: String? = null,
    onFlyToCompleted: () -> Unit = {},
    flyToUserTrigger: Int = 0,
    userLat: Double? = null,
    userLng: Double? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val managerRef = remember { mutableStateOf<PointAnnotationManager?>(null) }
    val userManagerRef = remember { mutableStateOf<PointAnnotationManager?>(null) }
    val styleReady = remember { mutableStateOf(false) }

    /** Station-marker scale (relative to the 96px source bitmap) for the current zoom. */
    fun iconSizeForZoom(zoom: Double): Double {
        val sizePx = maxOf(18.0, minOf(52.0, 22.0 + (zoom - 10.0) * 4.5))
        return sizePx / 96.0
    }

    /** User-marker scale (relative to the 96px source bitmap) for the current zoom. */
    fun userIconSizeForZoom(zoom: Double): Double {
        val sizePx = maxOf(16.0, minOf(48.0, 20.0 + (zoom - 10.0) * 3.5))
        return sizePx / 96.0
    }

    /** Station-label size in density-independent points; hidden until the map is zoomed in. */
    fun textSizeForZoom(zoom: Double): Double = when {
        zoom < 13 -> 0.0
        zoom < 15 -> 8.0
        zoom < 17 -> 10.0
        else -> 12.0
    }

    /** Renders a vector drawable into a bitmap so it can be used as a map annotation image. */
    fun drawableToBitmap(drawableRes: Int, widthPx: Int, heightPx: Int): Bitmap? {
        val drawable = context.resources.getDrawable(drawableRes, context.theme) ?: return null
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    LaunchedEffect(Unit) {
        val mapboxMap = mapView.mapboxMap
        mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
            val markerBitmap = drawableToBitmap(R.drawable.ic_station_marker, 96, 96)
            if (markerBitmap != null) {
                style.addImage("water_station_icon", markerBitmap)
            }
            val userMarkerBitmap = drawableToBitmap(R.drawable.ic_user_location, 96, 96)
            if (userMarkerBitmap != null) {
                style.addImage("user_location_icon", userMarkerBitmap)
            }
            managerRef.value = mapView.annotations.createPointAnnotationManager()
            userManagerRef.value = mapView.annotations.createPointAnnotationManager()
            styleReady.value = true
            mapView.attribution?.enabled = false
            mapView.logo?.enabled = false
            mapView.compass?.enabled = false
            mapView.scalebar?.enabled = false
        }
        mapboxMap.subscribeCameraChanged { cameraChanged ->
            val zoom = cameraChanged.cameraState.zoom
            managerRef.value?.let { mgr ->
                val iSize = iconSizeForZoom(zoom)
                val tSize = textSizeForZoom(zoom)
                mgr.annotations.forEach { a ->
                    a.iconSize = iSize
                    a.textSize = tSize
                }
                mgr.update(mgr.annotations)
            }
            userManagerRef.value?.let { mgr ->
                val uSize = userIconSizeForZoom(zoom)
                mgr.annotations.forEach { a ->
                    a.iconSize = uSize
                }
                mgr.update(mgr.annotations)
            }
        }
    }

    LaunchedEffect(stations, styleReady.value) {
        if (!styleReady.value) return@LaunchedEffect
        val manager = managerRef.value ?: return@LaunchedEffect
        manager.delete(manager.annotations.toList())
        val zoom = mapView.mapboxMap.cameraState.zoom
        val stationMap = mutableMapOf<PointAnnotation, WaterStation>()
        val annotations = stations.mapNotNull { station ->
            if (station.latitude == 0.0 && station.longitude == 0.0) null
            else {
                val point = Point.fromLngLat(station.longitude, station.latitude)
                PointAnnotationOptions()
                    .withPoint(point)
                    .withIconImage("water_station_icon")
                    .withIconSize(iconSizeForZoom(zoom))
                    .withTextField(station.stationName)
                    .withTextSize(textSizeForZoom(zoom))
                    .withTextColor("#191970")
                    .withTextHaloColor("#FFFFFF")
                    .withTextHaloWidth(1.0)
                    .withTextOffset(listOf(0.0, -3.5))
            }
        }
        val created = manager.create(annotations)
        created.forEachIndexed { i, ann ->
            stationMap[ann] = stations[i]
        }
        manager.addClickListener { ann ->
            stationMap[ann]?.let { onStationSelected(it) }
            true
        }
    }

    LaunchedEffect(userLat, userLng, styleReady.value) {
        if (!styleReady.value) return@LaunchedEffect
        val mgr = userManagerRef.value ?: return@LaunchedEffect
        mgr.delete(mgr.annotations.toList())
        if (userLat != null && userLng != null) {
            val zoom = mapView.mapboxMap.cameraState.zoom
            val point = Point.fromLngLat(userLng, userLat)
            val annotation = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage("user_location_icon")
                .withIconSize(userIconSizeForZoom(zoom))
            mgr.create(annotation)
        }
    }

    LaunchedEffect(flyToStationId) {
        val id = flyToStationId ?: return@LaunchedEffect
        val station = stations.find { it.id == id } ?: return@LaunchedEffect
        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(station.longitude, station.latitude))
                .zoom(15.0)
                .build()
        )
        onFlyToCompleted()
    }

    LaunchedEffect(flyToUserTrigger) {
        if (flyToUserTrigger > 0 && userLat != null && userLng != null) {
            mapView.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(userLng, userLat))
                    .zoom(14.0)
                    .build()
            )
        }
    }

    AndroidView(
        factory = {
            mapView.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(Constants.BAGUIO_CENTER[0], Constants.BAGUIO_CENTER[1]))
                    .zoom(12.0)
                    .build()
            )
            mapView
        },
        modifier = modifier,
    )
}

/**
 * Fetches the device's current position, one shot per call.
 *
 * Asks for a genuinely fresh fix first (maxUpdateAge = 0) and only falls back to the cached
 * one if that fails — the previous order round-tripped through `getLastLocation()`, which
 * could hand back a fix from hours ago or from another app entirely.
 *
 * Note: emulators have no GPS hardware. They report whatever position is configured under
 * Extended Controls > Location, so this can only ever show a simulated spot there.
 */
fun getUserLocation(
    context: Context,
    onResult: (Double, Double) -> Unit,
    onError: (String) -> Unit = {},
) {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (!LocationManagerCompat.isLocationEnabled(lm)) {
        onError("Location is turned off. Turn it on in Settings to see where you are.")
        return
    }

    val client = LocationServices.getFusedLocationProviderClient(context)
    val request = CurrentLocationRequest.Builder()
        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        .setDurationMillis(15_000)
        .setMaxUpdateAgeMillis(0)
        .build()

    try {
        client.getCurrentLocation(request, null)
            .addOnSuccessListener { fresh ->
                if (fresh != null) {
                    Log.i("MapScreen", "Fresh fix: ${fresh.latitude},${fresh.longitude} acc=${fresh.accuracy}")
                    onResult(fresh.latitude, fresh.longitude)
                    return@addOnSuccessListener
                }
                // No fresh fix within the window — fall back to the last known one.
                client.lastLocation
                    .addOnSuccessListener { cached ->
                        if (cached != null) {
                            Log.i("MapScreen", "Cached fix: ${cached.latitude},${cached.longitude}")
                            onResult(cached.latitude, cached.longitude)
                        } else {
                            onError("Couldn't get a GPS fix. Try again somewhere with a clearer view of the sky.")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w("MapScreen", "lastLocation failed", e)
                        onError("Couldn't get your location. Please try again.")
                    }
            }
            .addOnFailureListener { e ->
                Log.w("MapScreen", "getCurrentLocation failed", e)
                onError("Couldn't get your location. Please try again.")
            }
    } catch (e: SecurityException) {
        Log.w("MapScreen", "Missing location permission", e)
        onError("Location permission is needed to show where you are.")
    }
}