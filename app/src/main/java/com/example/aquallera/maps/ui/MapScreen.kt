package com.example.aquallera.maps.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.location.LocationManager
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
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
import com.example.aquallera.core.theme.MistGray
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
import kotlin.math.abs

@Composable
fun MapScreen(
    onStationTap: (stationId: String) -> Unit = {},
    onOrderNow: (stationId: String) -> Unit = {},
    viewModel: MapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showHowToOrder by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions.all { it.value }) {
            getUserLocation(context) { lat, lng ->
                viewModel.setUserLocation(lat, lng)
            }
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
            flyToStationId = uiState.flyToStationId,
            onFlyToCompleted = { viewModel.clearFlyToStation() },
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
            Text(
                text = "How to Order",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MidnightBlue)
                    .clickable { showHowToOrder = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
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
                        getUserLocation(context) { lat, lng ->
                            viewModel.setUserLocation(lat, lng)
                        }
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
            StationMiniInfoCard(
                station = station,
                onView = { onStationTap(station.id) },
                onDismiss = { viewModel.selectStation(null) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = 16.dp, end = 16.dp, top = 124.dp),
            )
        }

        if (uiState.filteredStations.isNotEmpty()) {
            StationBottomPanel(
                stations = uiState.filteredStations,
                statusFilter = uiState.statusFilter,
                userLat = uiState.userLat,
                userLng = uiState.userLng,
                onStationTap = onStationTap,
                onFilterChange = { viewModel.setFilter(it) },
                onViewOnMap = { station ->
                    viewModel.selectStation(station)
                    viewModel.flyToStation(station.id)
                },
                onOrderNow = { onOrderNow(it) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No water stations available",
                    color = MidnightBlue.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                )
            }
        }
    }

    if (showHowToOrder) {
        HowToOrderDialog(onClose = { showHowToOrder = false })
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
private fun StationMiniInfoCard(
    station: WaterStation,
    onView: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.stationName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MidnightBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (station.address.isNotBlank()) {
                    Text(
                        text = station.address,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "View",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MidnightBlue)
                    .clickable(onClick = onView)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "\u2715",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(4.dp),
            )
        }
    }
}

@Composable
private fun FilterChipsRow(
    selected: StationFilter,
    onSelect: (StationFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StationFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) MidnightBlue else MistGray)
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    text = filter.label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.White else Color.DarkGray,
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
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp

    val fullHeight = screenHeightDp * 0.85f
    val halfHeight = fullHeight * 0.6f
    val peekHeight = 60.dp

    var sheetHeight by remember { mutableStateOf(peekHeight) }

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
            FilterChipsRow(
                selected = statusFilter,
                onSelect = onFilterChange,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
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

@Composable
private fun MapboxMapView(
    stations: List<WaterStation>,
    onStationSelected: (WaterStation) -> Unit,
    flyToStationId: String? = null,
    onFlyToCompleted: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val managerRef = remember { mutableStateOf<PointAnnotationManager?>(null) }
    val styleReady = remember { mutableStateOf(false) }

    fun iconSizeForZoom(zoom: Double): Double {
        val sizePx = maxOf(16.0, minOf(48.0, 20.0 + (zoom - 10.0) * 4.0))
        return sizePx / 32.0
    }

    fun textSizeForZoom(zoom: Double): Double = when {
        zoom < 13 -> 0.0
        zoom < 15 -> 8.0
        zoom < 17 -> 10.0
        else -> 12.0
    }

    fun adjustMarkerBitmap(original: Bitmap, contrast: Float = 1.4f, brightness: Float = 0.0f): Bitmap {
        val result = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val cm = ColorMatrix().apply {
            val t = 0.5f * (1f - contrast) + brightness
            set(floatArrayOf(
                contrast, 0f, 0f, 0f, t,
                0f, contrast, 0f, 0f, t,
                0f, 0f, contrast, 0f, t,
                0f, 0f, 0f, 1f, 0f,
            ))
        }
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(original, 0f, 0f, paint)
        return result
    }

    LaunchedEffect(Unit) {
        val mapboxMap = mapView.mapboxMap
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
            val original = BitmapFactory.decodeResource(context.resources, R.drawable.ic_station_marker)
            val markerBitmap = original?.let { adjustMarkerBitmap(Bitmap.createScaledBitmap(it, 96, 96, true)) }
            if (markerBitmap != null) {
                style.addImage("water_station_icon", markerBitmap)
            }
            managerRef.value = mapView.annotations.createPointAnnotationManager()
            styleReady.value = true
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
                    .withTextColor("#424242")
                    .withTextHaloColor("#00000000")
                    .withTextHaloWidth(0.0)
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
        if (stations.isNotEmpty() && flyToStationId == null) {
            mapView.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(stations.first().longitude, stations.first().latitude))
                    .zoom(12.0)
                    .build()
            )
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

@Composable
private fun HowToOrderDialog(onClose: () -> Unit) {
    val steps = listOf(
        HowToStep(1, "Select a Water Station", "Browse stations on the map or list below. Tap a station card to see details."),
        HowToStep(2, "Choose Water Type and Quantity", "Select Pure, Spring, or Mineral water. Adjust quantities using + and - buttons."),
        HowToStep(3, "Select Pickup or Delivery", "Choose Delivery (with location) or Pickup. Your address will be used for delivery."),
        HowToStep(4, "Confirm Your Order", "Review your order summary, then tap Confirm. Payment is Cash on Delivery."),
        HowToStep(5, "Track Your Order", "Monitor your order status in the Orders tab. You'll get notified on status changes."),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MidnightBlue)
                .clickable(enabled = false) { }
                .padding(20.dp),
        ) {
            Text(
                text = "How to Order Water",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            steps.forEach { step ->
                Row(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(com.example.aquallera.core.theme.LightYellow),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${step.number}",
                            fontWeight = FontWeight.Bold,
                            color = MidnightBlue,
                            fontSize = 14.sp,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = step.title,
                            color = MistGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                        Text(
                            text = step.description,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                        )
                    }
                }
            }
            Text(
                text = "Got It!",
                color = MidnightBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MistGray)
                    .clickable(onClick = onClose)
                    .padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

private data class HowToStep(val number: Int, val title: String, val description: String)

fun getUserLocation(context: Context, onResult: (Double, Double) -> Unit) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        var location: android.location.Location? = null
        if (hasGps) {
            try {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } catch (_: SecurityException) { }
        }
        if (location == null && hasNetwork) {
            try {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (_: SecurityException) { }
        }
        if (location != null) {
            onResult(location.latitude, location.longitude)
        }
    } catch (_: Exception) { }
}
