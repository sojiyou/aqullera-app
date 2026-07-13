package com.example.aquallera

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location

class MapActivity : AppCompatActivity() {

    private lateinit var tvSelectedStationName: TextView
    private lateinit var tvSelectedStationAddress: TextView
    private lateinit var btnViewStationDetails: Button
    private lateinit var btnDeselectStation: TextView
    private lateinit var mapView: MapView
    private lateinit var btnHowToOrder: Button
    private lateinit var btnUserLocation: ImageView
    private lateinit var recyclerViewStations: RecyclerView
    private lateinit var selectedStationCard: LinearLayout
    private lateinit var bottomSheet: LinearLayout
    private lateinit var dragHandle: LinearLayout
    private lateinit var emptyState: LinearLayout

    private var selectedStation: WaterStation? = null
    private lateinit var stationAdapter: WaterStationAdapter
    private val stationsList = mutableListOf<WaterStation>()
    private var allStationsList = mutableListOf<WaterStation>()
    private var currentFilter = "all"

    private var notificationListener: NotificationListener? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation: Location? = null

    private val markerDataList = mutableListOf<MarkerData>()
    private var currentZoom = MIN_ZOOM
    private var pointAnnotationManager: PointAnnotationManager? = null

    // Bottom sheet drag
    private var sheetStartY = 0f
    private var sheetOffset = 0f
    private var sheetHeight = 0f
    private var parentHeight = 0f
    private val SNAP_PCT_15 = 0.15f
    private val SNAP_PCT_55 = 0.55f
    private val SNAP_PCT_85 = 0.85f

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001

        private const val MIN_ZOOM = 11.0
        private const val MAX_ZOOM = 18.0

        private val BAGUIO_BOUNDS = CoordinateBounds(
            Point.fromLngLat(120.5200, 16.3600),
            Point.fromLngLat(120.6700, 16.4600)
        )
        private val BAGUIO_CENTER = Point.fromLngLat(120.593, 16.412)
    }

    data class MarkerData(
        val station: WaterStation,
        val annotationId: String
    )

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initializeViews()
        setupRecyclerView()
        setupMap()
        setupClickListeners()
        setupBottomNavigation()
        setupBottomSheet()
        getUserLocation()

        notificationListener = NotificationListener(this)
        notificationListener?.start()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationListener?.stop()
        mapView.onDestroy()
    }

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    private fun initializeViews() {
        tvSelectedStationName = findViewById(R.id.tvSelectedStationName)
        tvSelectedStationAddress = findViewById(R.id.tvSelectedStationAddress)
        btnViewStationDetails = findViewById(R.id.btnViewStationDetails)
        btnDeselectStation = findViewById(R.id.btnDeselectStation)
        mapView = findViewById(R.id.mapView)
        btnHowToOrder = findViewById(R.id.btnHowToOrder)
        btnUserLocation = findViewById(R.id.btnUserLocation)
        recyclerViewStations = findViewById(R.id.recyclerViewStations)
        selectedStationCard = findViewById(R.id.selectedStationCard)
        bottomSheet = findViewById(R.id.bottomSheet)
        dragHandle = findViewById(R.id.dragHandle)
        emptyState = findViewById(R.id.emptyState)

        selectedStationCard.visibility = View.GONE

        val spinnerStatusFilter = findViewById<Spinner>(R.id.spinnerStatusFilter)
        val filterOptions = arrayOf("All", "Online", "Offline", "Pending")
        val filterValues = arrayOf("all", "online", "offline", "pending")
        val filterAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, filterOptions)
        spinnerStatusFilter.adapter = filterAdapter
        spinnerStatusFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentFilter = filterValues[position]
                applyFilter()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {
        stationAdapter = WaterStationAdapter(
            stations = stationsList,
            userLocation = userLocation,
            onViewOnMapClick = { station ->
                selectedStation = station
                zoomToStation(station)
                showSelectedStationCard(station)
                animateSheetTo(SNAP_PCT_15)
            }
        )

        recyclerViewStations.apply {
            layoutManager = LinearLayoutManager(this@MapActivity)
            adapter = stationAdapter
            isNestedScrollingEnabled = true
            setHasFixedSize(false)
        }
    }

    private fun setupMap() {
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { style ->
            addCustomIconsToStyle(style)

            mapView.getMapboxMap().setBounds(
                CameraBoundsOptions.Builder()
                    .bounds(BAGUIO_BOUNDS)
                    .minZoom(MIN_ZOOM)
                    .maxZoom(MAX_ZOOM)
                    .build()
            )

            mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(BAGUIO_CENTER)
                    .zoom(MIN_ZOOM)
                    .build()
            )

            mapView.getMapboxMap().addOnCameraChangeListener {
                val newZoom = mapView.getMapboxMap().cameraState.zoom
                if (kotlin.math.abs(newZoom - currentZoom) > 0.5) {
                    currentZoom = newZoom
                    updateMarkerSizes()
                }
            }

            if (hasLocationPermission()) {
                enableLocationTracking()
            } else {
                requestLocationPermission()
            }

            fetchWaterStations()
        }
    }

    private fun setupClickListeners() {
        btnHowToOrder.setOnClickListener { showHowToOrderDialog() }

        btnViewStationDetails.setOnClickListener {
            val station = selectedStation
            if (station != null) {
                val intent = Intent(this, StoreDetailsActivity::class.java)
                    .putExtra("WATER_STATION", station)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please select a water station first", Toast.LENGTH_SHORT).show()
            }
        }

        btnDeselectStation.setOnClickListener {
            selectedStation = null
            selectedStationCard.visibility = View.GONE
        }

        btnUserLocation.setOnClickListener {
            if (hasLocationPermission()) {
                getUserLocationAndFlyTo()
            } else {
                requestLocationPermission()
            }
        }
    }

    private fun setupBottomNavigation() {
        val navMap = findViewById<LinearLayout>(R.id.navMap)
        val navOrders = findViewById<LinearLayout>(R.id.navOrder)
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        setActiveTab(navMap)

        navMap.setOnClickListener { setActiveTab(navMap) }

        navOrders.setOnClickListener {
            startActivity(Intent(this, OrdersActivity::class.java))
            finish()
        }

        navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    // -------------------------------------------------------------------------
    // Bottom Sheet
    // -------------------------------------------------------------------------

    private fun setupBottomSheet() {
        bottomSheet.post {
            sheetHeight = bottomSheet.height.toFloat()
            parentHeight = (bottomSheet.parent as View).height.toFloat()

            val initialHeight = (parentHeight * SNAP_PCT_55).toInt()
            bottomSheet.layoutParams.height = initialHeight
            bottomSheet.requestLayout()

            dragHandle.setOnTouchListener { _, event ->
                handleSheetDrag(event)
                true
            }
        }
    }

    private fun handleSheetDrag(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                sheetStartY = event.rawY
                sheetOffset = parentHeight - bottomSheet.height
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaY = event.rawY - sheetStartY
                val newHeight = (parentHeight - (sheetOffset + deltaY)).toInt()
                    .coerceIn((parentHeight * 0.1f).toInt(), parentHeight)
                bottomSheet.layoutParams.height = newHeight
                bottomSheet.requestLayout()
            }
            MotionEvent.ACTION_UP -> {
                val currentHeight = bottomSheet.height.toFloat()
                val snapHeight = findNearestSnap(currentHeight)
                animateSheetTo(snapHeight / parentHeight)
            }
        }
    }

    private fun findNearestSnap(currentHeight: Float): Float {
        val ratios = listOf(SNAP_PCT_15, SNAP_PCT_55, SNAP_PCT_85)
        val heights = ratios.map { it * parentHeight }
        return heights.minByOrNull { kotlin.math.abs(it - currentHeight) } ?: (parentHeight * SNAP_PCT_55)
    }

    private fun animateSheetTo(ratio: Float) {
        val targetHeight = (parentHeight * ratio).toInt()
        val startHeight = bottomSheet.height

        if (startHeight == targetHeight) return

        ValueAnimator.ofInt(startHeight, targetHeight).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                bottomSheet.layoutParams.height = anim.animatedValue as Int
                bottomSheet.requestLayout()
            }
            start()
        }
    }

    // -------------------------------------------------------------------------
    // Location
    // -------------------------------------------------------------------------

    private fun getUserLocation() {
        if (!hasLocationPermission()) return
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    userLocation = location
                    stationAdapter.updateUserLocation(location)
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Location error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getUserLocationAndFlyTo() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    userLocation = location
                    stationAdapter.updateUserLocation(location)
                    Toast.makeText(this, "Finding your location...", Toast.LENGTH_SHORT).show()
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        mapView.getMapboxMap().setCamera(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(location.longitude, location.latitude))
                                .zoom(14.0)
                                .build()
                        )
                    }, 2000)
                } else {
                    Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Location error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun enableLocationTracking() {
        try {
            mapView.location.updateSettings {
                enabled = true
                pulsingEnabled = true
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not enable location tracking", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                enableLocationTracking()
                getUserLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Map markers
    // -------------------------------------------------------------------------

    private fun iconSizeForZoom(zoom: Double): Double {
        val sizePx = maxOf(16.0, minOf(48.0, 20.0 + (zoom - 10.0) * 4.0))
        return sizePx / 32.0
    }

    private fun textSizeForZoom(zoom: Double) = when {
        zoom < 13 -> 0.0
        zoom < 15 -> 8.0
        zoom < 17 -> 10.0
        else -> 12.0
    }

    private fun addCustomIconsToStyle(style: Style) {
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_water_drop) ?: return
        val bitmap = drawableToBitmap(drawable, 96, 96)
        style.addImage("water_station_icon", bitmap, false)
    }

    private fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun getOrCreateAnnotationManager(): PointAnnotationManager {
        return pointAnnotationManager ?: mapView.annotations
            .createPointAnnotationManager()
            .also { manager ->
                pointAnnotationManager = manager
                manager.addClickListener { clicked ->
                    val markerData = markerDataList.find { it.annotationId == clicked.id.toString() }
                    markerData?.let {
                        selectedStation = it.station
                        showSelectedStationCard(it.station)
                        zoomToStation(it.station)
                    }
                    true
                }
            }
    }

    private fun addWaterStationMarker(station: WaterStation) {
        if (station.latitude == 0.0 || station.longitude == 0.0) return

        val manager = getOrCreateAnnotationManager()
        val annotation = manager.create(
            PointAnnotationOptions()
                .withPoint(Point.fromLngLat(station.longitude, station.latitude))
                .withIconImage("water_station_icon")
                .withIconSize(iconSizeForZoom(currentZoom))
                .withTextField(station.stationName)
                .withTextSize(textSizeForZoom(currentZoom))
                .withTextColor("#424242")
                .withTextHaloColor("#00000000")
                .withTextHaloWidth(0.0)
                .withTextOffset(listOf(0.0, -3.5))
        )

        markerDataList.add(MarkerData(station, annotation.id.toString()))
    }

    private fun updateMarkerSizes() {
        val manager = pointAnnotationManager ?: return
        val iconSize = iconSizeForZoom(currentZoom)
        val textSize = textSizeForZoom(currentZoom)

        manager.annotations.forEach { annotation ->
            annotation.iconSize = iconSize
            annotation.textSize = textSize
        }
        manager.update(manager.annotations)
    }

    private fun zoomToStation(station: WaterStation) {
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(station.longitude, station.latitude))
                .zoom(15.0)
                .build()
        )
    }

    private fun showSelectedStationCard(station: WaterStation) {
        tvSelectedStationName.text = station.stationName
        tvSelectedStationAddress.text = station.address.ifEmpty { "Address not available" }
        selectedStationCard.visibility = View.VISIBLE
    }

    // -------------------------------------------------------------------------
    // Firebase
    // -------------------------------------------------------------------------

    private fun fetchWaterStations() {
        val waterStationsRef = FirebaseConfig.getDatabaseReference().child("waterStations")

        waterStationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allStationsList.clear()
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val id = child.key ?: continue
                        val station = child.getValue(WaterStation::class.java)?.copy(id = id) ?: continue
                        allStationsList.add(station)
                    }
                }

                val hasStations = allStationsList.isNotEmpty()
                emptyState.visibility = if (hasStations) View.GONE else View.VISIBLE
                recyclerViewStations.visibility = if (hasStations) View.VISIBLE else View.GONE

                applyFilter()
            }

            override fun onCancelled(error: DatabaseError) {
                emptyState.visibility = View.VISIBLE
                recyclerViewStations.visibility = View.GONE
                Toast.makeText(
                    this@MapActivity,
                    "Failed to load stations: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun applyFilter() {
        val filtered = allStationsList.filter { station ->
            when (currentFilter) {
                "all" -> true
                "online" -> station.status == "approved" && station.isOnline
                "offline" -> station.status == "approved" && !station.isOnline
                "pending" -> station.status != "approved"
                else -> true
            }
        }
        stationAdapter.updateStations(filtered)

        markerDataList.clear()
        pointAnnotationManager?.deleteAll()
        pointAnnotationManager = null
        filtered.forEach { addWaterStationMarker(it) }
        updateMarkerSizes()
    }

    // -------------------------------------------------------------------------
    // UI helpers
    // -------------------------------------------------------------------------

    private fun showHowToOrderDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_how_to_order)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.findViewById<Button>(R.id.btnCloseDialog).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun setActiveTab(activeTab: LinearLayout) {
        val tabs = mapOf(
            R.id.navMap to R.id.tvMap,
            R.id.navOrder to R.id.tvOrder,
            R.id.navProfile to R.id.tvProfile
        )

        tabs.forEach { (tabId, textId) ->
            val tab = findViewById<LinearLayout>(tabId)
            val text = findViewById<TextView>(textId)
            val isActive = tab == activeTab

            if (isActive) {
                text.setTextColor(Color.WHITE)
                text.alpha = 1.0f
            } else {
                text.setTextColor(ContextCompat.getColor(this, R.color.light_yellow))
                text.alpha = 0.6f
            }
            tab.background = null
        }
    }
}
