package com.example.aquallera

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class OrdersActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var ordersContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: LinearLayout
    private lateinit var listViewContainer: LinearLayout
    private lateinit var detailViewContainer: LinearLayout
    private lateinit var detailContent: LinearLayout
    private lateinit var btnCancelOrder: Button
    private lateinit var btnBack: TextView
    private lateinit var paginationLayout: LinearLayout

    private var allOrders = mutableListOf<Order>()
    private var stationStatuses = mutableMapOf<String, Map<String, Any?>>()
    private var filterStatus = "all"
    private var currentPage = 1
    private val PAGE_SIZE = 5
    private var selectedOrder: Order? = null
    private var cancellingOrderId: String? = null
    private var userPhone: String = ""

    private var ordersListener: ValueEventListener? = null
    private var stationsListener: ValueEventListener? = null
    private var ordersQuery: Query? = null
    private var stationsRef: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orders)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://aquallera-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        ordersContainer = findViewById(R.id.ordersContainer)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        listViewContainer = findViewById(R.id.listViewContainer)
        detailViewContainer = findViewById(R.id.detailViewContainer)
        detailContent = findViewById(R.id.detailContent)
        btnCancelOrder = findViewById(R.id.btnCancelOrder)
        btnBack = findViewById(R.id.btnBack)
        paginationLayout = findViewById(R.id.paginationLayout)

        tvEmptyState.visibility = View.GONE
        listViewContainer.visibility = View.VISIBLE

        btnBack.setOnClickListener {
            selectedOrder = null
            showListView()
        }

        btnCancelOrder.setOnClickListener {
            selectedOrder?.let { cancelOrder(it) }
        }

        setupBottomNavigation()
        setupFilterTabs()
        startRealtimeListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        ordersListener?.let { ordersQuery?.removeEventListener(it) }
        stationsListener?.let { stationsRef?.removeEventListener(it) }
    }

    // -------------------------------------------------------------------------
    // Real-time listeners
    // -------------------------------------------------------------------------

    private fun startRealtimeListeners() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login to view orders", Toast.LENGTH_SHORT).show()
            showEmptyState("Please login to view your orders")
            return
        }

        progressBar.visibility = View.VISIBLE

        // Listen to water stations for revoked detection
        setupStationListener()

        // Listen to orders in real-time
        setupOrdersListener(currentUser.uid)

        // Read user phone
        updateUserPhone(currentUser.uid)
    }

    private fun setupOrdersListener(userId: String) {
        ordersQuery = database.child("orders")

        ordersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                progressBar.visibility = View.GONE
                allOrders.clear()

                for (child in snapshot.children) {
                    try {
                        val order = child.getValue(Order::class.java)
                        if (order != null) {
                            val orderUserId = order.userId.ifEmpty { order.customerId }
                            if (orderUserId == userId) {
                                allOrders.add(order.copy(orderId = child.key ?: order.orderId))
                            }
                        } else {
                            try {
                                val manualOrder = parseOrderManually(child)
                                val orderUserId = manualOrder.userId.ifEmpty { manualOrder.customerId }
                                if (orderUserId == userId) {
                                    allOrders.add(manualOrder)
                                }
                            } catch (e: Exception) {
                                Log.e("OrdersDebug", "Manual parse failed: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        try {
                            val manualOrder = parseOrderManually(child)
                            val orderUserId = manualOrder.userId.ifEmpty { manualOrder.customerId }
                            if (orderUserId == userId) {
                                allOrders.add(manualOrder)
                            }
                        } catch (e2: Exception) {
                            Log.e("OrdersDebug", "Parse failed: ${e2.message}")
                        }
                    }
                }

                allOrders.sortByDescending { it.createdAt }

                if (allOrders.isNotEmpty()) {
                    tvEmptyState.visibility = View.GONE
                    listViewContainer.visibility = View.VISIBLE
                }

                renderOrders()
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@OrdersActivity, "Failed to load orders: ${error.message}", Toast.LENGTH_SHORT).show()
                showEmptyState("Failed to load orders")
            }
        }

        ordersQuery?.addValueEventListener(ordersListener!!)
    }

    private fun setupStationListener() {
        stationsRef = database.child("waterStations")
        stationsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                stationStatuses.clear()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val status = child.child("status").getValue(String::class.java)
                    val revokedAt = child.child("revokedAt").getValue(Long::class.java)
                    stationStatuses[id] = mapOf("status" to status, "revokedAt" to revokedAt)
                }
                autoCancelRevokedOrders()
                renderOrders()
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        stationsRef?.addValueEventListener(stationsListener!!)
    }

    private fun updateUserPhone(uid: String) {
        database.child("users").child(uid).child("number")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userPhone = snapshot.getValue(String::class.java) ?: ""
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // -------------------------------------------------------------------------
    // Revoked station detection
    // -------------------------------------------------------------------------

    private fun isRevoked(stationId: String): Boolean {
        val s = stationStatuses[stationId] ?: return false
        val status = s["status"] as? String
        val revokedAt = s["revokedAt"] as? Long
        return status == "deletion_pending" || (revokedAt != null && revokedAt > 0)
    }

    private fun autoCancelRevokedOrders() {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        allOrders.filter { order ->
            val s = (order.status ?: "").lowercase()
            isRevoked(order.stationId) && (s == "pending" || s == "confirmed")
        }.forEach { order ->
            val updates = mapOf(
                "status" to "cancelled",
                "cancellationReason" to "Station no longer active",
                "updatedAt" to isoFormat.format(Date())
            )
            database.child("orders").child(order.orderId).updateChildren(updates)
        }
    }

    // -------------------------------------------------------------------------
    // Filter tabs
    // -------------------------------------------------------------------------

    private fun setupFilterTabs() {
        val filterTabsLayout = findViewById<LinearLayout>(R.id.filterTabsLayout)
        val tabs = listOf("All", "Pending", "Confirmed", "Preparing", "On Delivery", "Ready", "Completed", "Cancelled")
        val tabValues = listOf("all", "pending", "confirmed", "preparing", "on_delivery", "ready", "completed", "cancelled")

        tabs.forEachIndexed { index, label ->
            val tab = TextView(this).apply {
                text = label
                textSize = 12f
                setPadding(32, 12, 32, 12)
                setTextColor(Color.WHITE)
                setBackgroundColor(resources.getColor(R.color.midnight_blue, null))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 8, 0) }
                setOnClickListener {
                    filterStatus = tabValues[index]
                    currentPage = 1
                    renderOrders()
                    updateTabStyles(filterTabsLayout, index)
                }
            }
            filterTabsLayout.addView(tab)
        }
        updateTabStyles(filterTabsLayout, 0)
    }

    private fun updateTabStyles(container: LinearLayout, activeIndex: Int) {
        for (i in 0 until container.childCount) {
            val tab = container.getChildAt(i) as? TextView ?: continue
            if (i == activeIndex) {
                tab.setTextColor(Color.WHITE)
                tab.alpha = 1.0f
                tab.setBackgroundColor(resources.getColor(R.color.midnight_blue, null))
            } else {
                tab.setTextColor(Color.parseColor("#999999"))
                tab.alpha = 0.8f
                tab.setBackgroundColor(resources.getColor(R.color.input_bg, null))
            }
        }
    }

    // -------------------------------------------------------------------------
    // Render orders (central method)
    // -------------------------------------------------------------------------

    private fun renderOrders() {
        val filteredOrders = if (filterStatus == "all") {
            allOrders
        } else {
            allOrders.filter { (it.status ?: "").lowercase() == filterStatus }
        }

        val totalPages = (filteredOrders.size + PAGE_SIZE - 1) / PAGE_SIZE
        if (currentPage > totalPages && totalPages > 0) currentPage = totalPages

        val paginatedOrders = filteredOrders.drop((currentPage - 1) * PAGE_SIZE).take(PAGE_SIZE)

        ordersContainer.removeAllViews()

        if (allOrders.isEmpty()) {
            showEmptyState("No orders yet. Place your first order!")
            paginationLayout.visibility = View.GONE
            return
        }

        if (filteredOrders.isEmpty()) {
            tvEmptyState.visibility = View.GONE
            ordersContainer.visibility = View.VISIBLE

            val noMatch = TextView(this).apply {
                text = "No ${filterLabelForValue(filterStatus)} Orders"
                textSize = 15f
                setTextColor(Color.parseColor("#666666"))
                gravity = Gravity.CENTER
                setPadding(0, 48, 0, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            ordersContainer.addView(noMatch)
            paginationLayout.visibility = View.GONE
            return
        }

        tvEmptyState.visibility = View.GONE
        ordersContainer.visibility = View.VISIBLE

        for (order in paginatedOrders) {
            addOrderTicket(order)
        }

        addPaginationControls(totalPages)
    }

    private fun filterLabelForValue(value: String): String {
        return when (value) {
            "pending" -> "Pending"
            "confirmed" -> "Confirmed"
            "preparing" -> "Preparing"
            "on_delivery" -> "On Delivery"
            "ready" -> "Ready"
            "completed" -> "Completed"
            "cancelled" -> "Cancelled"
            else -> ""
        }
    }

    // -------------------------------------------------------------------------
    // Pagination
    // -------------------------------------------------------------------------

    private fun addPaginationControls(totalPages: Int) {
        paginationLayout.removeAllViews()

        if (totalPages <= 1) {
            paginationLayout.visibility = View.GONE
            return
        }

        paginationLayout.visibility = View.VISIBLE

        val prevBtn = Button(this).apply {
            text = "Previous"
            textSize = 12f
            isEnabled = currentPage > 1
            alpha = if (currentPage > 1) 1.0f else 0.4f
            setBackgroundColor(resources.getColor(R.color.midnight_blue, null))
            setTextColor(Color.WHITE)
            setOnClickListener {
                currentPage = (currentPage - 1).coerceAtLeast(1)
                renderOrders()
            }
        }

        val pageInfo = TextView(this).apply {
            text = "Page $currentPage of $totalPages"
            textSize = 12f
            setPadding(16, 0, 16, 0)
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#666666"))
        }

        val nextBtn = Button(this).apply {
            text = "Next"
            textSize = 12f
            isEnabled = currentPage < totalPages
            alpha = if (currentPage < totalPages) 1.0f else 0.4f
            setBackgroundColor(resources.getColor(R.color.midnight_blue, null))
            setTextColor(Color.WHITE)
            setOnClickListener {
                currentPage = (currentPage + 1).coerceAtMost(totalPages)
                renderOrders()
            }
        }

        paginationLayout.addView(prevBtn)
        paginationLayout.addView(pageInfo)
        paginationLayout.addView(nextBtn)
    }

    // -------------------------------------------------------------------------
    // Ticket rendering
    // -------------------------------------------------------------------------

    private fun addOrderTicket(order: Order) {
        try {
            val ticketView = LayoutInflater.from(this)
                .inflate(R.layout.order_ticket_item, ordersContainer, false)

            val tvStationName = ticketView.findViewById<TextView>(R.id.tvStationName)
            val tvOrderStatus = ticketView.findViewById<TextView>(R.id.tvOrderStatus)
            val tvOrderDate = ticketView.findViewById<TextView>(R.id.tvOrderDate)
            val tvTicketNumber = ticketView.findViewById<TextView>(R.id.tvTicketNumber)
            val tvOrderItems = ticketView.findViewById<TextView>(R.id.tvOrderItems)
            val tvOrderPrice = ticketView.findViewById<TextView>(R.id.tvOrderPrice)
            val tvRevokedWarning = ticketView.findViewById<TextView>(R.id.tvRevokedWarning)

            tvStationName.text = order.stationName ?: "Unknown Station"
            tvOrderStatus.text = order.status ?: "Pending"
            tvOrderDate.text = "Date: ${order.date ?: "N/A"}"
            tvTicketNumber.text = "Ref #: ${order.referenceNumber ?: "N/A"}"
            tvOrderItems.text = "Items: ${buildItemsSummary(order)}"
            tvOrderPrice.text = "₱${String.format("%.2f", order.grandTotal)}"

            setStatusStyle(tvOrderStatus, order.status ?: "Pending")

            if (isRevoked(order.stationId)) {
                tvRevokedWarning.visibility = View.VISIBLE
            } else {
                tvRevokedWarning.visibility = View.GONE
            }

            ticketView.setOnClickListener {
                selectedOrder = order
                showDetailView(order)
            }

            ordersContainer.addView(ticketView)

        } catch (e: Exception) {
            Log.e("OrdersDebug", "Error creating order ticket: ${e.message}")
        }
    }

    private fun buildItemsSummary(order: Order): String {
        val parts = mutableListOf<String>()
        if (order.pureWaterQty > 0) parts.add("${order.pureWaterQty} Pure")
        if (order.springWaterQty > 0) parts.add("${order.springWaterQty} Spring")
        if (order.mineralWaterQty > 0) parts.add("${order.mineralWaterQty} Mineral")
        return if (parts.isNotEmpty()) parts.joinToString(", ") else "No items"
    }

    // -------------------------------------------------------------------------
    // Detail View
    // -------------------------------------------------------------------------

    private fun showDetailView(order: Order) {
        listViewContainer.visibility = View.GONE
        tvEmptyState.visibility = View.GONE
        detailViewContainer.visibility = View.VISIBLE

        detailContent.removeAllViews()

        // Revoked warning
        if (isRevoked(order.stationId)) {
            val revokedTag = TextView(this).apply {
                text = "⚠ Station Revoked"
                textSize = 13f
                setTextColor(Color.WHITE)
                setPadding(20, 10, 20, 10)
                setBackgroundColor(resources.getColor(R.color.status_cancelled, null))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 12) }
            }
            detailContent.addView(revokedTag)
        }

        // Station name
        detailContent.addView(buildDetailLabel(order.stationName ?: "Unknown Station", 20f, true))

        // Status
        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 8) }
        }
        val statusPill = TextView(this).apply {
            text = order.status ?: "Pending"
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(20, 6, 20, 6)
            setStatusStyle(this, order.status ?: "Pending")
        }
        statusRow.addView(statusPill)
        if (order.orderType.isNotEmpty()) {
            val typeTag = TextView(this).apply {
                text = "  •  ${order.orderType}"
                textSize = 12f
                setTextColor(Color.parseColor("#666666"))
                setPadding(8, 0, 0, 0)
            }
            statusRow.addView(typeTag)
        }
        detailContent.addView(statusRow)

        // Divider
        detailContent.addView(buildDivider())

        // Date & Time
        if (order.date.isNotEmpty()) {
            detailContent.addView(buildDetailRow("Date", order.date))
        }
        if (order.time.isNotEmpty()) {
            detailContent.addView(buildDetailRow("Time", order.time))
        }
        if (order.referenceNumber.isNotEmpty()) {
            detailContent.addView(buildDetailRow("Reference #", order.referenceNumber))
        }

        detailContent.addView(buildDivider())

        // Items breakdown
        if (order.pureWaterQty > 0) {
            detailContent.addView(buildDetailRow("Pure Water × ${order.pureWaterQty}", "₱${String.format("%.2f", order.pureWaterTotal)}"))
        }
        if (order.springWaterQty > 0) {
            detailContent.addView(buildDetailRow("Spring Water × ${order.springWaterQty}", "₱${String.format("%.2f", order.springWaterTotal)}"))
        }
        if (order.mineralWaterQty > 0) {
            detailContent.addView(buildDetailRow("Mineral Water × ${order.mineralWaterQty}", "₱${String.format("%.2f", order.mineralWaterTotal)}"))
        }

        // Subtotal
        detailContent.addView(buildDivider())
        detailContent.addView(buildDetailRow("Water Subtotal", "₱${String.format("%.2f", order.waterSubtotal)}"))

        if (order.deliveryFee > 0) {
            detailContent.addView(buildDetailRow("Delivery Fee", "₱${String.format("%.2f", order.deliveryFee)}"))
        }
        if (order.transactionFee > 0) {
            detailContent.addView(buildDetailRow("Transaction Fee", "₱${String.format("%.2f", order.transactionFee)}"))
        }

        // Grand Total
        detailContent.addView(buildDivider())
        detailContent.addView(buildDetailRow("Grand Total", "₱${String.format("%.2f", order.grandTotal)}", bold = true))

        // Payment Method
        if (order.paymentMethod.isNotEmpty()) {
            detailContent.addView(buildDetailRow("Payment", order.paymentMethod))
        }

        // Delivery Address
        if (order.deliveryAddress.isNotEmpty()) {
            detailContent.addView(buildDivider())
            detailContent.addView(buildDetailLabel("Delivery Address", 14f, true))
            detailContent.addView(buildDetailLabel(order.deliveryAddress, 13f, false))
        }

        // Additional Details
        if (order.additionalDetails.isNotEmpty()) {
            detailContent.addView(buildDivider())
            detailContent.addView(buildDetailLabel("Additional Details", 14f, true))
            detailContent.addView(buildDetailLabel(order.additionalDetails, 13f, false))
        }

        // Cancel button visibility
        val selStatus = (order.status ?: "").lowercase()
        val canCancel = selStatus == "pending" || selStatus == "confirmed"
        if (canCancel) {
            btnCancelOrder.visibility = View.VISIBLE
            if (cancellingOrderId == order.orderId) {
                btnCancelOrder.isEnabled = false
                btnCancelOrder.text = "Cancelling..."
            } else {
                btnCancelOrder.isEnabled = true
                btnCancelOrder.text = "Cancel Order"
            }
        } else {
            btnCancelOrder.visibility = View.GONE
        }
    }

    private fun showListView() {
        detailViewContainer.visibility = View.GONE
        detailContent.removeAllViews()
        listViewContainer.visibility = View.VISIBLE
        renderOrders()
    }

    // -------------------------------------------------------------------------
    // Detail view helpers
    // -------------------------------------------------------------------------

    private fun buildDetailLabel(text: String, sizeSp: Float, bold: Boolean): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = sizeSp
            setTextColor(Color.parseColor("#333333"))
            if (bold) setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 4, 0, 4) }
        }
    }

    private fun buildDetailRow(label: String, value: String, bold: Boolean = false): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 6, 0, 6) }

            addView(TextView(this@OrdersActivity).apply {
                this.text = label
                textSize = 13f
                setTextColor(Color.parseColor("#666666"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                if (bold) setTypeface(null, Typeface.BOLD)
            })

            addView(TextView(this@OrdersActivity).apply {
                this.text = value
                textSize = 13f
                setTextColor(Color.parseColor("#333333"))
                if (bold) {
                    setTypeface(null, Typeface.BOLD)
                    textSize = 15f
                    setTextColor(resources.getColor(R.color.midnight_blue, null))
                }
                gravity = Gravity.END
            })
        }
    }

    private fun buildDivider(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply { setMargins(0, 8, 0, 8) }
            setBackgroundColor(Color.parseColor("#CCCCCC"))
        }
    }

    // -------------------------------------------------------------------------
    // Cancel order
    // -------------------------------------------------------------------------

    private fun cancelOrder(order: Order) {
        cancellingOrderId = order.orderId
        selectedOrder?.let { showDetailView(it) }

        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val updates = mapOf(
            "status" to "cancelled",
            "updatedAt" to isoFormat.format(Date())
        )
        database.child("orders").child(order.orderId).updateChildren(updates)
            .addOnSuccessListener {
                cancellingOrderId = null
                selectedOrder = null
                showListView()
                Toast.makeText(this, "Order cancelled", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                cancellingOrderId = null
                Toast.makeText(this, "Failed to cancel order", Toast.LENGTH_SHORT).show()
                selectedOrder?.let { showDetailView(it) }
            }
    }

    // -------------------------------------------------------------------------
    // Status colors
    // -------------------------------------------------------------------------

    private fun setStatusStyle(textView: TextView, status: String) {
        try {
            val bgRes = when (status.lowercase()) {
                "pending" -> R.drawable.status_pending_bg
                "confirmed" -> R.drawable.status_confirmed_bg
                "preparing" -> R.drawable.status_preparing_bg
                "on_delivery", "out for delivery" -> R.drawable.status_on_delivery_bg
                "ready" -> R.drawable.status_ready_bg
                "delivered", "completed" -> R.drawable.status_completed_bg
                "cancelled" -> R.drawable.status_cancelled_background
                else -> R.drawable.status_pending_bg
            }
            textView.setBackgroundResource(bgRes)
            textView.setTextColor(Color.WHITE)
        } catch (e: Exception) {
            Log.e("OrdersDebug", "Error setting status style: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // Manual parse fallback
    // -------------------------------------------------------------------------

    private fun parseOrderManually(snapshot: DataSnapshot): Order {
        return Order(
            orderId = snapshot.child("orderId").getValue(String::class.java) ?: snapshot.key ?: "",
            stationId = snapshot.child("stationId").getValue(String::class.java) ?: "",
            stationName = snapshot.child("stationName").getValue(String::class.java) ?: "",
            customerId = snapshot.child("customerId").getValue(String::class.java) ?: "",
            customerName = snapshot.child("customerName").getValue(String::class.java) ?: "",
            orderType = snapshot.child("orderType").getValue(String::class.java) ?: "",
            date = snapshot.child("date").getValue(String::class.java) ?: "",
            time = snapshot.child("time").getValue(String::class.java) ?: "",
            pureWaterQty = snapshot.child("pureWaterQty").getValue(Int::class.java) ?: 0,
            springWaterQty = snapshot.child("springWaterQty").getValue(Int::class.java) ?: 0,
            mineralWaterQty = snapshot.child("mineralWaterQty").getValue(Int::class.java) ?: 0,
            pureWaterPrice = snapshot.child("pureWaterPrice").getValue(Double::class.java) ?: 0.0,
            springWaterPrice = snapshot.child("springWaterPrice").getValue(Double::class.java) ?: 0.0,
            mineralWaterPrice = snapshot.child("mineralWaterPrice").getValue(Double::class.java) ?: 0.0,
            pureWaterTotal = snapshot.child("pureWaterTotal").getValue(Double::class.java) ?: 0.0,
            springWaterTotal = snapshot.child("springWaterTotal").getValue(Double::class.java) ?: 0.0,
            mineralWaterTotal = snapshot.child("mineralWaterTotal").getValue(Double::class.java) ?: 0.0,
            waterSubtotal = snapshot.child("waterSubtotal").getValue(Double::class.java) ?: 0.0,
            deliveryFee = snapshot.child("deliveryFee").getValue(Double::class.java) ?: 0.0,
            transactionFee = snapshot.child("transactionFee").getValue(Double::class.java) ?: 0.0,
            grandTotal = snapshot.child("grandTotal").getValue(Double::class.java) ?: 0.0,
            locationDetails = snapshot.child("locationDetails").getValue(String::class.java) ?: "",
            deliveryAddress = snapshot.child("deliveryAddress").getValue(String::class.java) ?: "",
            deliveryLatitude = snapshot.child("deliveryLatitude").getValue(Double::class.java) ?: 0.0,
            deliveryLongitude = snapshot.child("deliveryLongitude").getValue(Double::class.java) ?: 0.0,
            additionalDetails = snapshot.child("additionalDetails").getValue(String::class.java) ?: "",
            paymentMethod = snapshot.child("paymentMethod").getValue(String::class.java) ?: "Cash on Delivery",
            status = snapshot.child("status").getValue(String::class.java) ?: "Pending",
            createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis(),
            referenceNumber = snapshot.child("referenceNumber").getValue(String::class.java) ?: ""
        )
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private fun showEmptyState(message: String) {
        val msgView = tvEmptyState.findViewById<TextView>(R.id.tvEmptyMessage)
        msgView.text = message
        tvEmptyState.findViewById<TextView>(R.id.tvEmptySubtitle).visibility = View.GONE
        tvEmptyState.visibility = View.VISIBLE
        listViewContainer.visibility = View.GONE
        detailViewContainer.visibility = View.GONE
    }

    // -------------------------------------------------------------------------
    // Bottom Nav
    // -------------------------------------------------------------------------

    private fun setupBottomNavigation() {
        val navMap = findViewById<LinearLayout>(R.id.navMap)
        val navOrders = findViewById<LinearLayout>(R.id.navOrder)
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        setActiveTab(navOrders)

        navMap.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }

        navOrders.setOnClickListener {
            selectedOrder = null
            showListView()
            setActiveTab(navOrders)
        }

        navProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setActiveTab(activeTab: LinearLayout) {
        val allTabs = listOf(
            findViewById<LinearLayout>(R.id.navMap),
            findViewById<LinearLayout>(R.id.navOrder),
            findViewById<LinearLayout>(R.id.navProfile)
        )

        allTabs.forEach { tab ->
            val textView = when (tab.id) {
                R.id.navMap -> findViewById<TextView>(R.id.tvMap)
                R.id.navOrder -> findViewById<TextView>(R.id.tvOrder)
                R.id.navProfile -> findViewById<TextView>(R.id.tvProfile)
                else -> null
            }

            if (tab == activeTab) {
                textView?.setTextColor(Color.WHITE)
                textView?.alpha = 1.0f
            } else {
                textView?.setTextColor(ContextCompat.getColor(this, R.color.light_yellow))
                textView?.alpha = 0.6f
            }
            tab.background = null
        }
    }
}
