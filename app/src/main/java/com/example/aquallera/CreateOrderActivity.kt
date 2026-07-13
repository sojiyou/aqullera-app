package com.example.aquallera

import android.app.DatePickerDialog
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class CreateOrderActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var tvWaterStationName: TextView
    private lateinit var tvSelectedDate: TextView
    private lateinit var btnDelivery: Button
    private lateinit var btnPickup: Button
    private lateinit var btnGetLocation: Button
    private lateinit var etAddressSearch: EditText
    private lateinit var etAdditionalDetails: EditText
    private lateinit var btnPreviewOrder: Button
    private lateinit var btnEditOrder: Button
    private lateinit var btnProceed: Button
    private lateinit var btnBack: ImageButton

    // Water quantity inputs
    private lateinit var etGallonPure: EditText
    private lateinit var etLiterSpring: EditText
    private lateinit var etGallonMineral: EditText

    // Water price displays
    private lateinit var tvPureWaterPrice: TextView
    private lateinit var tvSpringWaterPrice: TextView
    private lateinit var tvMineralWaterPrice: TextView

    // Quantity control buttons
    private lateinit var btnIncreaseGallonPure: Button
    private lateinit var btnDecreaseGallonPure: Button
    private lateinit var btnIncreaseLiterSpring: Button
    private lateinit var btnDecreaseLiterSpring: Button
    private lateinit var btnIncreaseGallonMineral: Button
    private lateinit var btnDecreaseGallonMineral: Button

    // Preview elements
    private lateinit var previewContent: ScrollView
    private lateinit var formContent: ScrollView
    private lateinit var bottomActions: LinearLayout
    private lateinit var tvPreviewStation: TextView
    private lateinit var tvPreviewPure: TextView
    private lateinit var tvPreviewSpring: TextView
    private lateinit var tvPreviewMineral: TextView
    private lateinit var tvPreviewOrderType: TextView
    private lateinit var previewAddressCard: LinearLayout
    private lateinit var tvPreviewAddress: TextView
    private lateinit var tvPreviewDate: TextView
    private lateinit var tvPreviewTime: TextView
    private lateinit var previewInstructionsCard: LinearLayout
    private lateinit var tvPreviewInstructions: TextView
    private lateinit var tvPreviewSubtotal: TextView
    private lateinit var previewDeliveryFeeRow: LinearLayout
    private lateinit var tvPreviewDeliveryFee: TextView
    private lateinit var tvPreviewTransactionFee: TextView
    private lateinit var tvPreviewGrandTotal: TextView

    // Email warning
    private lateinit var emailWarningCard: LinearLayout
    private lateinit var btnResendVerification: TextView

    // Address autocomplete
    private lateinit var suggestionsContainer: LinearLayout
    private lateinit var btnClearAddress: ImageButton
    private lateinit var tvSelectedAddress: TextView
    private lateinit var tvSearchStatus: TextView
    private lateinit var addressCard: LinearLayout

    // Time slots
    private lateinit var timeSlotsCard: LinearLayout
    private lateinit var timeSlotsContainer: LinearLayout

    // Data
    private var currentStation: WaterStation? = null
    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0
    private var userAddress: String = ""
    private var manualAddress: String = ""
    private var isLocationDetected: Boolean = false
    private var isDelivery: Boolean = true

    // Prices from station
    private var pureWaterPrice: Double = 0.0
    private var springWaterPrice: Double = 0.0
    private var mineralWaterPrice: Double = 0.0
    private var deliveryFee: Double = 0.0
    private val transactionFeeRate = 0.02

    // Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Firebase Auth
    private lateinit var auth: FirebaseAuth
    private var emailVerified: Boolean = true
    private var verifSending: Boolean = false

    // Address autocomplete
    private val suggestions = mutableListOf<Pair<String, String>>()
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    // Order Limiter
    private val MAX_WATER_QUANTITY = 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_order)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        auth = FirebaseAuth.getInstance()

        currentStation = intent.getSerializableExtra("WATER_STATION") as? WaterStation

        initializeViews()
        setupClickListeners()
        setupQuantityControls()
        setupDateSelection()
        loadStationPrices()
        updateWaterStationName()
        checkEmailVerification()
        prefillContactNumber()
        updateDeliveryUi()
    }

    private fun initializeViews() {
        tvWaterStationName = findViewById(R.id.tvWaterStationName)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        btnDelivery = findViewById(R.id.btnDelivery)
        btnPickup = findViewById(R.id.btnPickup)
        btnGetLocation = findViewById(R.id.btnGetLocation)
        etAddressSearch = findViewById(R.id.etAddressSearch)
        etAdditionalDetails = findViewById(R.id.additionalDetailsInput)
        btnPreviewOrder = findViewById(R.id.btnPreviewOrder)
        btnEditOrder = findViewById(R.id.btnEditOrder)
        btnProceed = findViewById(R.id.btnProceed)
        btnBack = findViewById(R.id.btnBack)

        etGallonPure = findViewById(R.id.etGallonPure)
        etLiterSpring = findViewById(R.id.etLiterSpring)
        etGallonMineral = findViewById(R.id.etGallonMineral)

        tvPureWaterPrice = findViewById(R.id.tvPureWaterPrice)
        tvSpringWaterPrice = findViewById(R.id.tvSpringWaterPrice)
        tvMineralWaterPrice = findViewById(R.id.tvMineralWaterPrice)

        btnIncreaseGallonPure = findViewById(R.id.btnIncreaseGallonPure)
        btnDecreaseGallonPure = findViewById(R.id.btnDecreaseGallonPure)
        btnIncreaseLiterSpring = findViewById(R.id.btnIncreaseLiterSpring)
        btnDecreaseLiterSpring = findViewById(R.id.btnDecreaseLiterSpring)
        btnIncreaseGallonMineral = findViewById(R.id.btnIncreaseGallonMineral)
        btnDecreaseGallonMineral = findViewById(R.id.btnDecreaseGallonMineral)

        formContent = findViewById(R.id.formContent)
        previewContent = findViewById(R.id.previewContent)
        bottomActions = findViewById(R.id.bottomActions)
        tvPreviewStation = findViewById(R.id.tvPreviewStation)
        tvPreviewPure = findViewById(R.id.tvPreviewPure)
        tvPreviewSpring = findViewById(R.id.tvPreviewSpring)
        tvPreviewMineral = findViewById(R.id.tvPreviewMineral)
        tvPreviewOrderType = findViewById(R.id.tvPreviewOrderType)
        previewAddressCard = findViewById(R.id.previewAddressCard)
        tvPreviewAddress = findViewById(R.id.tvPreviewAddress)
        tvPreviewDate = findViewById(R.id.tvPreviewDate)
        tvPreviewTime = findViewById(R.id.tvPreviewTime)
        previewInstructionsCard = findViewById(R.id.previewInstructionsCard)
        tvPreviewInstructions = findViewById(R.id.tvPreviewInstructions)
        tvPreviewSubtotal = findViewById(R.id.tvPreviewSubtotal)
        previewDeliveryFeeRow = findViewById(R.id.previewDeliveryFeeRow)
        tvPreviewDeliveryFee = findViewById(R.id.tvPreviewDeliveryFee)
        tvPreviewTransactionFee = findViewById(R.id.tvPreviewTransactionFee)
        tvPreviewGrandTotal = findViewById(R.id.tvPreviewGrandTotal)

        emailWarningCard = findViewById(R.id.emailWarningCard)
        btnResendVerification = findViewById(R.id.btnResendVerification)

        suggestionsContainer = findViewById(R.id.suggestionsContainer)
        btnClearAddress = findViewById(R.id.btnClearAddress)
        tvSelectedAddress = findViewById(R.id.tvSelectedAddress)
        tvSearchStatus = findViewById(R.id.tvSearchStatus)
        addressCard = findViewById(R.id.addressCard)

        timeSlotsCard = findViewById(R.id.timeSlotsCard)
        timeSlotsContainer = findViewById(R.id.timeSlotsContainer)
    }

    // -------------------------------------------------------------------------
    // Email Verification
    // -------------------------------------------------------------------------

    private fun checkEmailVerification() {
        val user = auth.currentUser
        if (user != null) {
            user.reload().addOnCompleteListener {
                emailVerified = user.isEmailVerified
                emailWarningCard.visibility = if (emailVerified) View.GONE else View.VISIBLE
                updatePreviewButtonState()
            }
        }
    }

    private fun handleResendVerification() {
        if (verifSending) return
        verifSending = true
        btnResendVerification.text = "Sending..."

        auth.currentUser?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                verifSending = false
                btnResendVerification.text = "Resend verification email"
                if (task.isSuccessful) {
                    Toast.makeText(this, "Verification email sent", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to send: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // -------------------------------------------------------------------------
    // Prefill
    // -------------------------------------------------------------------------

    private fun prefillContactNumber() {
        val userId = auth.currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance("https://aquallera-default-rtdb.asia-southeast1.firebasedatabase.app")
        database.reference.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val number = when {
                        snapshot.hasChild("number") -> snapshot.child("number").getValue(String::class.java)
                        snapshot.hasChild("phone") -> snapshot.child("phone").getValue(String::class.java)
                        snapshot.hasChild("phoneNumber") -> snapshot.child("phoneNumber").getValue(String::class.java)
                        snapshot.hasChild("contactNumber") -> snapshot.child("contactNumber").getValue(String::class.java)
                        else -> null
                    }
                    // No contact number input in new layout — phone is saved with order
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("CreateOrder", "Failed to fetch user number: ${error.message}")
                }
            })
    }

    // -------------------------------------------------------------------------
    // Station / Prices
    // -------------------------------------------------------------------------

    private fun loadStationPrices() {
        currentStation?.let { station ->
            pureWaterPrice = station.pricing_gallon_pure ?: 0.0
            springWaterPrice = station.pricing_liter_spring ?: 0.0
            mineralWaterPrice = station.pricing_gallon_mineral ?: 0.0
            deliveryFee = station.pricing_delivery_fee ?: 50.0

            tvPureWaterPrice.text = "₱${String.format("%.2f", pureWaterPrice)} / gallon"
            tvSpringWaterPrice.text = "₱${String.format("%.2f", springWaterPrice)} / liter"
            tvMineralWaterPrice.text = "₱${String.format("%.2f", mineralWaterPrice)} / gallon"

            if (isDelivery && !station.deliveryDays.isNullOrEmpty()) {
                val availableDates = getAvailableDeliveryDates()
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
                if (availableDates.isNotEmpty() && (selectedDate.isEmpty() || selectedDate == todayStr)) {
                    selectedDate = availableDates[0]
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val displaySdf = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
                    try {
                        val date = sdf.parse(selectedDate)
                        if (date != null) tvSelectedDate.text = displaySdf.format(date)
                    } catch (e: Exception) {}
                }
            }
        }
    }

    private fun updateWaterStationName() {
        currentStation?.let { station ->
            tvWaterStationName.text = station.stationName
        }
    }

    // -------------------------------------------------------------------------
    // Order Type Toggle
    // -------------------------------------------------------------------------

    private fun updateDeliveryUi() {
        if (isDelivery) {
            btnDelivery.background = ContextCompat.getDrawable(this, R.drawable.toggle_btn_active)
            btnDelivery.setTextColor(Color.WHITE)
            btnPickup.background = ContextCompat.getDrawable(this, R.drawable.toggle_btn_inactive)
            btnPickup.setTextColor(Color.parseColor("#666666"))
            addressCard.visibility = View.VISIBLE
            if (selectedDate.isNotEmpty() && currentStation?.deliveryHours?.isNotEmpty() == true) {
                timeSlotsCard.visibility = View.VISIBLE
                updateTimeSlots()
            } else {
                timeSlotsCard.visibility = View.GONE
            }
        } else {
            btnPickup.background = ContextCompat.getDrawable(this, R.drawable.toggle_btn_active)
            btnPickup.setTextColor(Color.WHITE)
            btnDelivery.background = ContextCompat.getDrawable(this, R.drawable.toggle_btn_inactive)
            btnDelivery.setTextColor(Color.parseColor("#666666"))
            addressCard.visibility = View.GONE
            if (selectedDate.isNotEmpty() && currentStation?.businessHours?.isNotEmpty() == true) {
                timeSlotsCard.visibility = View.VISIBLE
                updateTimeSlots()
            } else {
                timeSlotsCard.visibility = View.GONE
            }
        }
    }

    // -------------------------------------------------------------------------
    // Date Selection
    // -------------------------------------------------------------------------

    private fun setupDateSelection() {
        val calendar = Calendar.getInstance()
        val internalFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val displayFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
        selectedDate = internalFormat.format(calendar.time)
        tvSelectedDate.text = displayFormat.format(calendar.time)

        tvSelectedDate.setOnClickListener {
            if (isDelivery && !currentStation?.deliveryDays.isNullOrEmpty()) {
                showDeliveryDateDropdown()
            } else {
                showDatePickerDialog()
            }
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val cal = Calendar.getInstance()
                cal.set(selectedYear, selectedMonth, selectedDay)
                val internalFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val displayFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
                selectedDate = internalFormat.format(cal.time)
                tvSelectedDate.text = displayFormat.format(cal.time)
                selectedTime = ""
                updateTimeSlots()
            },
            year, month, day
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
            show()
        }
    }

    // -------------------------------------------------------------------------
    // Delivery Days Filtering
    // -------------------------------------------------------------------------

    private fun getAvailableDeliveryDates(): List<String> {
        val deliveryDays = currentStation?.deliveryDays ?: return emptyList()
        if (deliveryDays.isEmpty()) return emptyList()

        val dayNames = listOf("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday")
        val results = mutableListOf<String>()
        val calendar = Calendar.getInstance()

        for (i in 0 until 30) {
            if (results.size >= 14) break
            val dayName = dayNames[calendar.get(Calendar.DAY_OF_WEEK) - 1]
            if (deliveryDays.contains(dayName)) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                results.add(sdf.format(calendar.time))
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return results
    }

    private fun showDeliveryDateDropdown() {
        val dates = getAvailableDeliveryDates()
        if (dates.isEmpty()) {
            showDatePickerDialog()
            return
        }

        val displayDates = dates.map { dateStr ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val displaySdf = SimpleDateFormat("EEE, MMM d", Locale.US)
            try {
                val date = sdf.parse(dateStr)
                if (date != null) displaySdf.format(date) else dateStr
            } catch (e: Exception) {
                dateStr
            }
        }.toTypedArray()

        val currentIndex = dates.indexOf(selectedDate).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Select Delivery Date")
            .setSingleChoiceItems(displayDates, currentIndex) { dialog, which ->
                selectedDate = dates[which]
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val displaySdf = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
                try {
                    val date = sdf.parse(selectedDate)
                    if (date != null) tvSelectedDate.text = displaySdf.format(date)
                } catch (e: Exception) {
                    tvSelectedDate.text = selectedDate
                }
                selectedTime = ""
                updateTimeSlots()
                dialog.dismiss()
            }
            .show()
    }

    // -------------------------------------------------------------------------
    // Pickup Time Slots
    // -------------------------------------------------------------------------

    private fun generatePickupTimeSlots(): List<Pair<String, String>> {
        val slots = mutableListOf<Pair<String, String>>()
        val businessHours = currentStation?.businessHours ?: return slots

        val dayMap = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val calendar = Calendar.getInstance()
        if (selectedDate.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val date = sdf.parse(selectedDate)
                if (date != null) {
                    calendar.time = date
                }
            } catch (e: Exception) {}
        }
        val dayName = dayMap[calendar.get(Calendar.DAY_OF_WEEK) - 1]

        val hoursStr = businessHours[dayName] ?: ""
        var openH = 6; var openM = 0; var closeH = 21; var closeM = 0

        if (hoursStr.contains(" - ")) {
            val parts = hoursStr.split(" - ")
            val openTime = parseTime12h(parts.getOrNull(0) ?: "")
            val closeTime = parseTime12h(parts.getOrNull(1) ?: "")
            if (openTime != null) { openH = openTime.first; openM = openTime.second }
            if (closeTime != null) { closeH = closeTime.first; closeM = closeTime.second }
        } else if (businessHours.containsKey("open")) {
            val openStr = businessHours["open"] ?: ""
            val closeStr = businessHours["close"] ?: ""
            val openTime = parseTime24h(openStr)
            val closeTime = parseTime24h(closeStr)
            if (openTime != null) { openH = openTime.first; openM = openTime.second }
            if (closeTime != null) { closeH = closeTime.first; closeM = closeTime.second }
        }

        var h = openH; var m = openM
        while (h < closeH || (h == closeH && m < closeM)) {
            val value = String.format("%02d:%02d", h, m)
            val hour12 = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
            val period = if (h < 12) "AM" else "PM"
            val display = String.format("%d:%02d %s", hour12, m, period)
            slots.add(Pair(display, value))
            m += 30
            if (m >= 60) { h++; m = 0 }
        }
        return slots
    }

    private fun parseTime12h(timeStr: String): Pair<Int, Int>? {
        try {
            val cleaned = timeStr.trim()
            val parts = cleaned.split(":")
            if (parts.size < 2) return null
            var hour = parts[0].toIntOrNull() ?: return null
            val minStr = parts[1].replace(Regex("[^0-9]"), "")
            val min = minStr.toIntOrNull() ?: 0
            val isPM = cleaned.uppercase().contains("PM")
            val isAM = cleaned.uppercase().contains("AM")
            if (isPM && hour != 12) hour += 12
            if (isAM && hour == 12) hour = 0
            return Pair(hour, min)
        } catch (e: Exception) { return null }
    }

    private fun parseTime24h(timeStr: String): Pair<Int, Int>? {
        try {
            val parts = timeStr.trim().split(":")
            if (parts.size < 2) return null
            return Pair(parts[0].toIntOrNull() ?: 0, parts[1].toIntOrNull() ?: 0)
        } catch (e: Exception) { return null }
    }

    // -------------------------------------------------------------------------
    // Time Slots
    // -------------------------------------------------------------------------

    private fun updateTimeSlots() {
        timeSlotsContainer.removeAllViews()

        if (isDelivery) {
            val hours = currentStation?.deliveryHours ?: emptyList()
            if (hours.isEmpty()) {
                timeSlotsCard.visibility = View.GONE
                return
            }
            timeSlotsCard.visibility = View.VISIBLE
            updateTimeSlotsTitle("🚚 Delivery Time", "Select a delivery time slot")

            var currentRow: LinearLayout? = null
            var countInRow = 0

            hours.forEachIndexed { index, time ->
                if (countInRow == 0) {
                    currentRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                }

                val btn = Button(this)
                val displayTime = formatTimeSlot(time)
                btn.text = displayTime
                btn.textSize = 12f
                btn.setTypeface(btn.typeface, android.graphics.Typeface.BOLD)
                btn.minWidth = 0
                btn.minHeight = 0
                btn.setPadding(16, 10, 16, 10)
                btn.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    36.dpToPx()
                ).apply {
                    setMargins(0, 0, 8.dpToPx(), 8.dpToPx())
                }
                btn.setBackgroundResource(R.drawable.toggle_btn_inactive)
                btn.setTextColor(Color.parseColor("#666666"))

                btn.setOnClickListener {
                    selectedTime = time
                    updateTimeSlotSelection()
                }

                currentRow?.addView(btn)
                countInRow++

                if (countInRow >= 3 || index == hours.size - 1) {
                    currentRow?.let { timeSlotsContainer.addView(it) }
                    countInRow = 0
                }
            }

            if (selectedTime.isNotEmpty()) {
                updateTimeSlotSelection()
            }
        } else {
            val slots = generatePickupTimeSlots()
            if (slots.isEmpty()) {
                timeSlotsCard.visibility = View.GONE
                return
            }
            timeSlotsCard.visibility = View.VISIBLE
            updateTimeSlotsTitle("🏪 Pickup Time", "Select a pickup time slot")

            var currentRow: LinearLayout? = null
            var countInRow = 0

            slots.forEachIndexed { index, (display, value) ->
                if (countInRow == 0) {
                    currentRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                }

                val btn = Button(this)
                btn.text = display
                btn.textSize = 12f
                btn.setTypeface(btn.typeface, android.graphics.Typeface.BOLD)
                btn.minWidth = 0
                btn.minHeight = 0
                btn.setPadding(16, 10, 16, 10)
                btn.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    36.dpToPx()
                ).apply {
                    setMargins(0, 0, 8.dpToPx(), 8.dpToPx())
                }
                btn.setBackgroundResource(R.drawable.toggle_btn_inactive)
                btn.setTextColor(Color.parseColor("#666666"))

                btn.setOnClickListener {
                    selectedTime = value
                    updateTimeSlotSelection()
                }

                currentRow?.addView(btn)
                countInRow++

                if (countInRow >= 3 || index == slots.size - 1) {
                    currentRow?.let { timeSlotsContainer.addView(it) }
                    countInRow = 0
                }
            }

            if (selectedTime.isNotEmpty()) {
                updateTimeSlotSelection()
            }
        }
    }

    private fun updateTimeSlotsTitle(title: String, subtitle: String) {
        val card = timeSlotsCard
        val titleView = card.getChildAt(0) as? TextView
        val subtitleView = card.getChildAt(1) as? TextView
        titleView?.text = title
        subtitleView?.text = subtitle
    }

    private fun updateTimeSlotSelection() {
        for (i in 0 until timeSlotsContainer.childCount) {
            val row = timeSlotsContainer.getChildAt(i) as? LinearLayout ?: continue
            for (j in 0 until row.childCount) {
                val btn = row.getChildAt(j) as? Button ?: continue
                val displayTime = btn.text.toString()
                val isSelected = if (isDelivery) {
                    displayTime == formatTimeSlot(selectedTime)
                } else {
                    displayTime == formatTimeSlot(selectedTime)
                }
                if (isSelected) {
                    btn.setBackgroundResource(R.drawable.toggle_btn_active)
                    btn.setTextColor(Color.WHITE)
                } else {
                    btn.setBackgroundResource(R.drawable.toggle_btn_inactive)
                    btn.setTextColor(Color.parseColor("#666666"))
                }
            }
        }
    }

    private fun formatTimeSlot(time: String): String {
        return try {
            val parts = time.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toInt()
                val minute = parts[1].toInt()
                val amPm = if (hour < 12) "AM" else "PM"
                val displayHour = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                String.format("%d:%02d %s", displayHour, minute, amPm)
            } else time
        } catch (e: Exception) {
            time
        }
    }

    // -------------------------------------------------------------------------
    // Address Autocomplete
    // -------------------------------------------------------------------------

    private fun setupAddressAutocomplete() {
        etAddressSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                btnClearAddress.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE

                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                if (query.length < 3) {
                    suggestionsContainer.removeAllViews()
                    suggestionsContainer.visibility = View.GONE
                    return
                }

                searchRunnable = Runnable { searchAddress(query) }
                searchHandler.postDelayed(searchRunnable!!, 400)
            }
        })

        btnClearAddress.setOnClickListener {
            etAddressSearch.setText("")
            userAddress = ""
            tvSelectedAddress.visibility = View.GONE
        }
    }

    private fun searchAddress(query: String) {
        val token = getString(R.string.mapbox_access_token)
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val urlStr = "https://api.mapbox.com/geocoding/v5/mapbox.places/$encodedQuery.json" +
                "?access_token=$token&country=PH&types=address,place,poi,locality,neighborhood,district" +
                "&language=en&limit=8&autocomplete=true&fuzzyMatch=true"

        Thread {
            try {
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val features = json.getJSONArray("features")
                val results = mutableListOf<Pair<String, String>>()

                for (i in 0 until features.length()) {
                    val feature = features.getJSONObject(i)
                    val placeName = feature.getString("place_name")
                    val text = feature.getString("text")
                    results.add(Pair(text, placeName))
                }

                runOnUiThread { showSuggestions(results) }
            } catch (e: Exception) {
                runOnUiThread {
                    tvSearchStatus.text = "Search failed"
                    tvSearchStatus.visibility = View.VISIBLE
                }
            }
        }.start()
    }

    private fun showSuggestions(results: List<Pair<String, String>>) {
        suggestionsContainer.removeAllViews()
        suggestions.clear()
        suggestions.addAll(results)

        if (results.isEmpty()) {
            suggestionsContainer.visibility = View.GONE
            return
        }

        suggestionsContainer.visibility = View.VISIBLE
        results.forEachIndexed { index, (text, placeName) ->
            val item = LinearLayout(this)
            item.orientation = LinearLayout.HORIZONTAL
            item.gravity = android.view.Gravity.CENTER_VERTICAL
            item.setPadding(12, 10, 12, 10)
            item.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            if (index < results.size - 1) {
                item.setBackgroundColor(Color.parseColor("#FFFFFF"))
                val divider = View(this)
                divider.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                )
                divider.setBackgroundColor(Color.parseColor("#F0F0F0"))
                suggestionsContainer.addView(divider)
            }

            val icon = TextView(this)
            icon.text = "📍"
            icon.textSize = 14f
            icon.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 8, 0) }

            val textContainer = LinearLayout(this)
            textContainer.orientation = LinearLayout.VERTICAL
            textContainer.layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

            val title = TextView(this)
            title.text = text
            title.textSize = 12f
            title.setTextColor(Color.parseColor("#333333"))
            title.maxLines = 1
            title.ellipsize = android.text.TextUtils.TruncateAt.END

            val subtitle = TextView(this)
            subtitle.text = placeName
            subtitle.textSize = 10f
            subtitle.setTextColor(Color.parseColor("#999999"))
            subtitle.maxLines = 1
            subtitle.ellipsize = android.text.TextUtils.TruncateAt.END

            textContainer.addView(title)
            textContainer.addView(subtitle)

            item.addView(icon)
            item.addView(textContainer)
            item.setOnClickListener {
                selectAddress(placeName)
            }

            suggestionsContainer.addView(item)
        }
    }

    private fun selectAddress(placeName: String) {
        etAddressSearch.setText(placeName)
        userAddress = placeName
        tvSelectedAddress.text = placeName
        tvSelectedAddress.visibility = View.VISIBLE
        suggestionsContainer.removeAllViews()
        suggestionsContainer.visibility = View.GONE
    }

    // -------------------------------------------------------------------------
    // Click Listeners
    // -------------------------------------------------------------------------

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        btnDelivery.setOnClickListener {
            isDelivery = true
            updateDeliveryUi()
            if (selectedDate.isNotEmpty()) updateTimeSlots()
        }

        btnPickup.setOnClickListener {
            isDelivery = false
            updateDeliveryUi()
        }

        btnGetLocation.setOnClickListener { getCurrentLocation() }

        btnPreviewOrder.setOnClickListener {
            if (validateOrder()) {
                showPreview()
            }
        }

        btnEditOrder.setOnClickListener { hidePreview() }

        btnProceed.setOnClickListener { navigateToOrderConfirmation() }

        btnResendVerification.setOnClickListener { handleResendVerification() }

        setupAddressAutocomplete()
    }

    // -------------------------------------------------------------------------
    // Location
    // -------------------------------------------------------------------------

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            tvSearchStatus.text = "Getting location..."
            tvSearchStatus.visibility = View.VISIBLE

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        userLatitude = it.latitude
                        userLongitude = it.longitude
                        isLocationDetected = true
                        reverseGeocode(it.latitude, it.longitude)
                        Toast.makeText(this, "✅ Location detected!", Toast.LENGTH_SHORT).show()
                    } ?: run {
                        isLocationDetected = false
                        tvSearchStatus.text = "Unable to get location. Enable GPS."
                        Toast.makeText(this, "Unable to get location", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener {
                    isLocationDetected = false
                    tvSearchStatus.text = "Failed: ${it.message}"
                }
        } catch (e: SecurityException) {
            isLocationDetected = false
            tvSearchStatus.text = "Permission required"
        }
    }

    private fun reverseGeocode(lat: Double, lng: Double) {
        val token = getString(R.string.mapbox_access_token)
        val urlStr = "https://api.mapbox.com/geocoding/v5/mapbox.places/$lng,$lat.json" +
                "?access_token=$token&country=PH&types=address,place,locality&language=en"

        Thread {
            try {
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val features = json.getJSONArray("features")
                if (features.length() > 0) {
                    val placeName = features.getJSONObject(0).getString("place_name")
                    runOnUiThread {
                        selectAddress(placeName)
                        tvSearchStatus.text = "Location detected"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvSearchStatus.text = "Reverse geocode failed"
                }
            }
        }.start()
    }

    // -------------------------------------------------------------------------
    // Quantity Controls
    // -------------------------------------------------------------------------

    private fun setupQuantityControls() {
        btnIncreaseGallonPure.setOnClickListener {
            val current = etGallonPure.text.toString().toIntOrNull() ?: 0
            if (current < MAX_WATER_QUANTITY) etGallonPure.setText((current + 1).toString())
            else Toast.makeText(this, "Maximum of $MAX_WATER_QUANTITY gallons allowed", Toast.LENGTH_SHORT).show()
        }
        btnDecreaseGallonPure.setOnClickListener {
            val current = etGallonPure.text.toString().toIntOrNull() ?: 0
            if (current > 0) etGallonPure.setText((current - 1).toString())
        }

        btnIncreaseLiterSpring.setOnClickListener {
            val current = etLiterSpring.text.toString().toIntOrNull() ?: 0
            if (current < MAX_WATER_QUANTITY) etLiterSpring.setText((current + 1).toString())
            else Toast.makeText(this, "Maximum of $MAX_WATER_QUANTITY liters allowed", Toast.LENGTH_SHORT).show()
        }
        btnDecreaseLiterSpring.setOnClickListener {
            val current = etLiterSpring.text.toString().toIntOrNull() ?: 0
            if (current > 0) etLiterSpring.setText((current - 1).toString())
        }

        btnIncreaseGallonMineral.setOnClickListener {
            val current = etGallonMineral.text.toString().toIntOrNull() ?: 0
            if (current < MAX_WATER_QUANTITY) etGallonMineral.setText((current + 1).toString())
            else Toast.makeText(this, "Maximum of $MAX_WATER_QUANTITY gallons allowed", Toast.LENGTH_SHORT).show()
        }
        btnDecreaseGallonMineral.setOnClickListener {
            val current = etGallonMineral.text.toString().toIntOrNull() ?: 0
            if (current > 0) etGallonMineral.setText((current - 1).toString())
        }

        setupTextWatcher(etGallonPure)
        setupTextWatcher(etLiterSpring)
        setupTextWatcher(etGallonMineral)
    }

    private fun setupTextWatcher(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.let { text ->
                    if (text.isNotEmpty()) {
                        val value = text.toIntOrNull()
                        when {
                            value == null || value < 0 -> {
                                editText.setText("0")
                                editText.setSelection(1)
                            }
                            value > MAX_WATER_QUANTITY -> {
                                editText.setText(MAX_WATER_QUANTITY.toString())
                                editText.setSelection(MAX_WATER_QUANTITY.toString().length)
                                Toast.makeText(editText.context, "Maximum quantity is $MAX_WATER_QUANTITY", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        })
    }

    // -------------------------------------------------------------------------
    // Date Display Helper
    // -------------------------------------------------------------------------

    private fun formatSelectedDateForDisplay(): String {
        if (selectedDate.isEmpty()) return selectedDate
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val displaySdf = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
            val date = sdf.parse(selectedDate)
            if (date != null) displaySdf.format(date) else selectedDate
        } catch (e: Exception) {
            selectedDate
        }
    }

    // -------------------------------------------------------------------------
    // Preview Toggle
    // -------------------------------------------------------------------------

    private fun showPreview() {
        val total = calculateOrderTotal()

        tvPreviewStation.text = "Station: ${currentStation?.stationName ?: ""}"
        tvPreviewPure.text = if (total.pureWaterQty > 0) "pure water — ${total.pureWaterQty} gallon(s) at ₱${String.format("%.2f", total.pureWaterPrice)} each" else ""
        tvPreviewSpring.text = if (total.springWaterQty > 0) "spring water — ${total.springWaterQty} liter(s) at ₱${String.format("%.2f", total.springWaterPrice)} each" else ""
        tvPreviewMineral.text = if (total.mineralWaterQty > 0) "mineral water — ${total.mineralWaterQty} gallon(s) at ₱${String.format("%.2f", total.mineralWaterPrice)} each" else ""
        tvPreviewOrderType.text = if (isDelivery) "Delivery" else "Pickup"

        if (isDelivery && userAddress.isNotEmpty()) {
            previewAddressCard.visibility = View.VISIBLE
            tvPreviewAddress.text = userAddress
        } else {
            previewAddressCard.visibility = View.GONE
        }

        tvPreviewDate.text = "Date: ${formatSelectedDateForDisplay()}"
        tvPreviewTime.text = if (selectedTime.isNotEmpty()) "Time: ${formatTimeSlot(selectedTime)}" else "Time: Not set"

        val instructions = etAdditionalDetails.text.toString().trim()
        if (instructions.isNotEmpty()) {
            previewInstructionsCard.visibility = View.VISIBLE
            tvPreviewInstructions.text = instructions
        } else {
            previewInstructionsCard.visibility = View.GONE
        }

        tvPreviewSubtotal.text = "₱${String.format("%.2f", total.waterSubtotal)}"

        if (isDelivery && total.deliveryFee > 0) {
            previewDeliveryFeeRow.visibility = View.VISIBLE
            tvPreviewDeliveryFee.text = "₱${String.format("%.2f", total.deliveryFee)}"
        } else {
            previewDeliveryFeeRow.visibility = View.GONE
        }

        tvPreviewTransactionFee.text = "₱${String.format("%.2f", total.transactionFee)}"
        tvPreviewGrandTotal.text = "₱${String.format("%.2f", total.grandTotal)}"

        formContent.visibility = View.GONE
        previewContent.visibility = View.VISIBLE
        btnPreviewOrder.visibility = View.GONE
        bottomActions.visibility = View.VISIBLE
    }

    private fun hidePreview() {
        formContent.visibility = View.VISIBLE
        previewContent.visibility = View.GONE
        btnPreviewOrder.visibility = View.VISIBLE
        bottomActions.visibility = View.GONE
    }

    // -------------------------------------------------------------------------
    // Order Total Calculation
    // -------------------------------------------------------------------------

    private fun calculateOrderTotal(): OrderTotal {
        val pureWaterQty = etGallonPure.text.toString().toIntOrNull() ?: 0
        val springWaterQty = etLiterSpring.text.toString().toIntOrNull() ?: 0
        val mineralWaterQty = etGallonMineral.text.toString().toIntOrNull() ?: 0

        val pureWaterTotal = pureWaterQty * pureWaterPrice
        val springWaterTotal = springWaterQty * springWaterPrice
        val mineralWaterTotal = mineralWaterQty * mineralWaterPrice

        val waterSubtotal = pureWaterTotal + springWaterTotal + mineralWaterTotal
        val totalDeliveryFee = if (isDelivery) deliveryFee else 0.0
        val transactionFee = (waterSubtotal + totalDeliveryFee) * transactionFeeRate
        val grandTotal = waterSubtotal + totalDeliveryFee + transactionFee

        return OrderTotal(
            pureWaterQty = pureWaterQty,
            pureWaterPrice = pureWaterPrice,
            pureWaterTotal = pureWaterTotal,
            springWaterQty = springWaterQty,
            springWaterPrice = springWaterPrice,
            springWaterTotal = springWaterTotal,
            mineralWaterQty = mineralWaterQty,
            mineralWaterPrice = mineralWaterPrice,
            mineralWaterTotal = mineralWaterTotal,
            waterSubtotal = waterSubtotal,
            deliveryFee = totalDeliveryFee,
            transactionFee = transactionFee,
            grandTotal = grandTotal
        )
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    private fun validateOrder(): Boolean {
        if (!emailVerified) {
            Toast.makeText(this, "Please verify your email to place orders", Toast.LENGTH_LONG).show()
            return false
        }

        val pureWaterQty = etGallonPure.text.toString().toIntOrNull() ?: 0
        val springWaterQty = etLiterSpring.text.toString().toIntOrNull() ?: 0
        val mineralWaterQty = etGallonMineral.text.toString().toIntOrNull() ?: 0

        if (pureWaterQty == 0 && springWaterQty == 0 && mineralWaterQty == 0) {
            Toast.makeText(this, "Please select at least one type of water", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Please select a preferred date", Toast.LENGTH_SHORT).show()
            return false
        }

        if (isDelivery && currentStation?.deliveryHours?.isNotEmpty() == true && selectedTime.isEmpty()) {
            Toast.makeText(this, "Please select a delivery time slot", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!isDelivery && currentStation?.businessHours?.isNotEmpty() == true && selectedTime.isEmpty()) {
            Toast.makeText(this, "Please select a pickup time slot", Toast.LENGTH_SHORT).show()
            return false
        }

        if (isDelivery) {
            manualAddress = etAddressSearch.text.toString().trim()
            val hasManualAddress = manualAddress.isNotEmpty()

            if (!hasManualAddress && userAddress.isEmpty()) {
                Toast.makeText(this,
                    "Delivery address is required.\nPlease type your address or tap 📍 for GPS.",
                    Toast.LENGTH_LONG).show()
                return false
            }
        }

        return true
    }

    private fun updatePreviewButtonState() {
        btnPreviewOrder.isEnabled = emailVerified
        btnPreviewOrder.alpha = if (emailVerified) 1.0f else 0.5f
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    private fun navigateToOrderConfirmation() {
        val orderTotal = calculateOrderTotal()
        manualAddress = etAddressSearch.text.toString().trim()
        if (manualAddress.isNotEmpty()) userAddress = manualAddress

        val contactNumber = auth.currentUser?.phoneNumber ?: ""

        val intent = Intent(this, OrderConfirmationActivity::class.java)
        intent.putExtra("WATER_STATION", currentStation)
        intent.putExtra("ORDER_DATE", selectedDate)
        intent.putExtra("ORDER_TIME", selectedTime)
        intent.putExtra("ORDER_TYPE", if (isDelivery) "Delivery" else "Pickup")
        intent.putExtra("USER_LATITUDE", userLatitude)
        intent.putExtra("USER_LONGITUDE", userLongitude)
        intent.putExtra("USER_ADDRESS", userAddress)
        intent.putExtra("MANUAL_ADDRESS", manualAddress)
        intent.putExtra("ADDITIONAL_DETAILS", etAdditionalDetails.text.toString())
        intent.putExtra("ORDER_TOTAL", orderTotal)
        intent.putExtra("CONTACT_NUMBER", contactNumber)
        intent.putExtra("PURE_WATER_QTY", orderTotal.pureWaterQty)
        intent.putExtra("SPRING_WATER_QTY", orderTotal.springWaterQty)
        intent.putExtra("MINERAL_WATER_QTY", orderTotal.mineralWaterQty)
        startActivity(intent)
    }

    // -------------------------------------------------------------------------
    // Data class
    // -------------------------------------------------------------------------

    data class OrderTotal(
        val pureWaterQty: Int,
        val pureWaterPrice: Double,
        val pureWaterTotal: Double,
        val springWaterQty: Int,
        val springWaterPrice: Double,
        val springWaterTotal: Double,
        val mineralWaterQty: Int,
        val mineralWaterPrice: Double,
        val mineralWaterTotal: Double,
        val waterSubtotal: Double,
        val deliveryFee: Double,
        val transactionFee: Double,
        val grandTotal: Double
    ) : java.io.Serializable

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()
}
