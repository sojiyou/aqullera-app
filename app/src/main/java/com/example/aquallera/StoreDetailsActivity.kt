package com.example.aquallera

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class StoreDetailsActivity : AppCompatActivity() {

    private lateinit var tvStoreName: TextView
    private lateinit var tvAddress: TextView
    private lateinit var btnOrder: Button
    private lateinit var btnReturn: Button
    private lateinit var btnBack: TextView

    private lateinit var badgesContainer: LinearLayout
    private lateinit var businessHoursContainer: LinearLayout
    private lateinit var tvServicesSummary: TextView
    private lateinit var servicesChipsContainer: LinearLayout
    private lateinit var deliveryHoursCard: LinearLayout
    private lateinit var deliveryChipsContainer: LinearLayout
    private lateinit var tvPurePrice: TextView
    private lateinit var tvSpringPrice: TextView
    private lateinit var tvMineralPrice: TextView
    private lateinit var aboutCard: LinearLayout
    private lateinit var tvAbout: TextView
    private lateinit var tvOnlineStatus: TextView
    private lateinit var deliveryDaysCard: LinearLayout
    private lateinit var chipGroupDeliveryDays: com.google.android.material.chip.ChipGroup

    private var currentStation: WaterStation? = null

    private val daysOfWeek = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store_details)

        initializeViews()
        setupClickListeners()

        val station = intent.getSerializableExtra("WATER_STATION") as? WaterStation
        if (station != null) {
            currentStation = station
            displayStationDetails(station)
        } else {
            Toast.makeText(this, "Error loading station details", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeViews() {
        tvStoreName = findViewById(R.id.tvStoreName)
        tvAddress = findViewById(R.id.tvAddress)
        btnOrder = findViewById(R.id.btnOrder)
        btnReturn = findViewById(R.id.btnReturn)
        btnBack = findViewById(R.id.btnBack)

        badgesContainer = findViewById(R.id.badgesContainer)
        businessHoursContainer = findViewById(R.id.businessHoursContainer)
        tvServicesSummary = findViewById(R.id.tvServicesSummary)
        servicesChipsContainer = findViewById(R.id.servicesChipsContainer)
        deliveryHoursCard = findViewById(R.id.deliveryHoursCard)
        deliveryChipsContainer = findViewById(R.id.deliveryChipsContainer)
        tvPurePrice = findViewById(R.id.tvPurePrice)
        tvSpringPrice = findViewById(R.id.tvSpringPrice)
        tvMineralPrice = findViewById(R.id.tvMineralPrice)
        aboutCard = findViewById(R.id.aboutCard)
        tvAbout = findViewById(R.id.tvAbout)
        tvOnlineStatus = findViewById(R.id.tvOnlineStatus)
        deliveryDaysCard = findViewById(R.id.deliveryDaysCard)
        chipGroupDeliveryDays = findViewById(R.id.chipGroupDeliveryDays)
    }

    private fun displayStationDetails(station: WaterStation) {
        tvStoreName.text = station.stationName

        val addressParts = listOfNotNull(
            station.address.takeIf { it.isNotEmpty() },
            station.city.takeIf { it.isNotEmpty() },
            station.state.takeIf { it.isNotEmpty() },
            station.zipCode.takeIf { it.isNotEmpty() }
        )
        tvAddress.text = addressParts.joinToString(", ").ifEmpty { "Address not available" }

        displayStatusBadges(station)
        displayBusinessHours(station)
        displayServices(station)
        displayDeliveryHours(station)
        displayDeliveryDays(station)
        displayPrices(station)
        displayAbout(station)
    }

    private fun displayStatusBadges(station: WaterStation) {
        badgesContainer.removeAllViews()
        badgesContainer.visibility = View.VISIBLE

        val status = station.status.lowercase()
        val isApproved = status == "approved"

        val statusBadge = createBadge(
            if (isApproved) "Approved" else "Pending",
            if (isApproved) R.drawable.status_completed_bg else R.drawable.status_preparing_bg
        )
        badgesContainer.addView(statusBadge)

        // Online/Offline badge
        val isOnline = station.isOnline
        if (isApproved) {
            tvOnlineStatus.visibility = View.VISIBLE
            if (isOnline) {
                tvOnlineStatus.text = "● Online"
                tvOnlineStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            } else {
                tvOnlineStatus.text = "● Offline"
                tvOnlineStatus.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
            }
        } else {
            tvOnlineStatus.visibility = View.GONE
        }
    }

    private fun displayBusinessHours(station: WaterStation) {
        businessHoursContainer.removeAllViews()

        val todayIndex = getTodayIndex()
        val hoursEntries = if (station.businessHours.isNotEmpty()) {
            station.businessHours
        } else {
            mapOf(
                "Monday" to "7:00 AM - 7:00 PM",
                "Tuesday" to "7:00 AM - 7:00 PM",
                "Wednesday" to "7:00 AM - 7:00 PM",
                "Thursday" to "7:00 AM - 7:00 PM",
                "Friday" to "7:00 AM - 7:00 PM",
                "Saturday" to "7:00 AM - 7:00 PM",
                "Sunday" to "7:00 AM - 7:00 PM"
            )
        }

        val sortedDays = daysOfWeek.filter { it in hoursEntries }

        for ((index, day) in sortedDays.withIndex()) {
            val hours = hoursEntries[day] ?: "Closed"
            val isToday = index == todayIndex || (sortedDays.size > todayIndex && sortedDays[todayIndex] == day)

            val row = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setPadding(0, 6, 0, 6) }
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val dayText = TextView(this).apply {
                text = day
                textSize = 14f
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val hoursText = TextView(this).apply {
                text = hours
                textSize = 14f
                setTextColor(resources.getColor(R.color.midnight_blue, null))
                if (isToday) setTypeface(null, android.graphics.Typeface.BOLD)
            }

            row.addView(dayText)
            row.addView(hoursText)
            businessHoursContainer.addView(row)
        }
    }

    private fun displayServices(station: WaterStation) {
        servicesChipsContainer.removeAllViews()

        val services = station.serviceTypes
        if (services.isEmpty()) {
            tvServicesSummary.text = "No services listed"
            return
        }

        val hasDelivery = services.contains("delivery")
        val hasPickup = services.contains("pickup")

        tvServicesSummary.text = when {
            hasDelivery && hasPickup -> "Pickup & Delivery"
            hasDelivery -> "Delivery Only"
            hasPickup -> "Pickup Only"
            else -> services.joinToString(", ")
        }

        for (service in services) {
            val chip = createServiceChip(service)
            servicesChipsContainer.addView(chip)
        }
    }

    private fun displayDeliveryHours(station: WaterStation) {
        val hasDelivery = station.serviceTypes.contains("delivery")
        val deliveryHours = station.deliveryHours

        if (hasDelivery && !deliveryHours.isNullOrEmpty()) {
            deliveryHoursCard.visibility = View.VISIBLE
            deliveryChipsContainer.removeAllViews()

            deliveryHours.forEach { time ->
                val chip = createDeliveryTimeChip(time)
                deliveryChipsContainer.addView(chip)
            }
        } else {
            deliveryHoursCard.visibility = View.GONE
        }
    }

    private fun displayDeliveryDays(station: WaterStation) {
        val deliveryDays = station.deliveryDays
        if (station.serviceTypes.contains("delivery") && !deliveryDays.isNullOrEmpty()) {
            deliveryDaysCard.visibility = View.VISIBLE
            chipGroupDeliveryDays.removeAllViews()
            val dayLabels = mapOf(
                "monday" to "Mon", "tuesday" to "Tue", "wednesday" to "Wed",
                "thursday" to "Thu", "friday" to "Fri", "saturday" to "Sat", "sunday" to "Sun"
            )
            deliveryDays.forEach { day ->
                val chip = com.google.android.material.chip.Chip(this).apply {
                    text = dayLabels[day.lowercase()] ?: day.take(3).capitalize()
                    isClickable = false
                    setChipBackgroundColorResource(R.color.midnight_blue)
                    setTextColor(resources.getColor(android.R.color.white, null))
                }
                chipGroupDeliveryDays.addView(chip)
            }
        } else {
            deliveryDaysCard.visibility = View.GONE
        }
    }

    private fun displayPrices(station: WaterStation) {
        val purePrice = station.pricing_gallon_pure
        val springPrice = station.pricing_liter_spring ?: station.pricing_gallon_spring
        val mineralPrice = station.pricing_gallon_mineral

        tvPurePrice.text = if (purePrice != null) "₱${String.format("%.2f", purePrice)}" else "—"
        tvSpringPrice.text = if (springPrice != null) "₱${String.format("%.2f", springPrice)}" else "—"
        tvMineralPrice.text = if (mineralPrice != null) "₱${String.format("%.2f", mineralPrice)}" else "—"
    }

    private fun displayAbout(station: WaterStation) {
        val aboutText = station.about
        if (!aboutText.isNullOrBlank()) {
            aboutCard.visibility = View.VISIBLE
            tvAbout.text = aboutText
        } else {
            aboutCard.visibility = View.GONE
        }
    }

    // Helper methods

    private fun createBadge(text: String, bgRes: Int): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 11f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundResource(bgRes)
            setPadding(
                dpToPx(8),
                dpToPx(2),
                dpToPx(8),
                dpToPx(2)
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dpToPx(6) }
        }
    }

    private fun createServiceChip(service: String): TextView {
        return TextView(this).apply {
            text = service.replaceFirstChar { it.uppercase() }
            textSize = 12f
            setTextColor(resources.getColor(R.color.midnight_blue, null))
            setBackgroundResource(R.drawable.status_pending_bg)
            setPadding(
                dpToPx(12),
                dpToPx(4),
                dpToPx(12),
                dpToPx(4)
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dpToPx(6) }
        }
    }

    private fun createDeliveryTimeChip(time: String): TextView {
        return TextView(this).apply {
            text = formatTime(time)
            textSize = 12f
            setTextColor(resources.getColor(R.color.midnight_blue, null))
            setBackgroundResource(R.drawable.status_pending_bg)
            setPadding(
                dpToPx(12),
                dpToPx(4),
                dpToPx(12),
                dpToPx(4)
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dpToPx(6) }
        }
    }

    private fun formatTime(time: String): String {
        return try {
            val parts = time.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toInt()
                val minute = parts[1]
                when {
                    hour == 0 -> "12:$minute AM"
                    hour < 12 -> "$hour:$minute AM"
                    hour == 12 -> "12:$minute PM"
                    else -> "${hour - 12}:$minute PM"
                }
            } else {
                time
            }
        } catch (e: Exception) {
            time
        }
    }

    private fun getTodayIndex(): Int {
        val cal = java.util.Calendar.getInstance()
        val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
        return when (dayOfWeek) {
            java.util.Calendar.SUNDAY -> 0
            java.util.Calendar.MONDAY -> 1
            java.util.Calendar.TUESDAY -> 2
            java.util.Calendar.WEDNESDAY -> 3
            java.util.Calendar.THURSDAY -> 4
            java.util.Calendar.FRIDAY -> 5
            java.util.Calendar.SATURDAY -> 6
            else -> -1
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun setupClickListeners() {
        btnOrder.setOnClickListener {
            currentStation?.let { navigateToCreateOrder(it) }
        }

        btnReturn.setOnClickListener {
            finish()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun navigateToCreateOrder(station: WaterStation) {
        try {
            val intent = Intent(this, CreateOrderActivity::class.java)
                .putExtra("WATER_STATION", station)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open order page", Toast.LENGTH_SHORT).show()
        }
    }
}
