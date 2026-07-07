package com.example.aquallera

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class OrdersActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var ordersContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orders)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://aquallera-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        ordersContainer = findViewById(R.id.ordersContainer)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        tvEmptyState.visibility = View.GONE
        ordersContainer.visibility = View.VISIBLE

        setupBottomNavigation()
        loadUserOrders()
    }

    // -------------------------------------------------------------------------
    // Loading
    // -------------------------------------------------------------------------

    private fun loadUserOrders() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "Please login to view orders", Toast.LENGTH_SHORT).show()
            showEmptyState("Please login to view your orders")
            return
        }

        val userId = currentUser.uid
        progressBar.visibility = View.VISIBLE
        ordersContainer.removeAllViews()

        database.child("orders")
            .orderByChild("customerId")
            .equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    progressBar.visibility = View.GONE

                    if (dataSnapshot.exists()) {
                        val ordersList = mutableListOf<Order>()

                        for (orderSnapshot in dataSnapshot.children) {
                            try {
                                val order = orderSnapshot.getValue(Order::class.java)
                                if (order != null) {
                                    ordersList.add(order)
                                } else {
                                    try {
                                        val manualOrder = parseOrderManually(orderSnapshot)
                                        ordersList.add(manualOrder)
                                    } catch (e2: Exception) {
                                        Log.e("OrdersDebug", "Manual parse failed: ${e2.message}")
                                    }
                                }
                            } catch (e: Exception) {
                                try {
                                    val manualOrder = parseOrderManually(orderSnapshot)
                                    ordersList.add(manualOrder)
                                } catch (e2: Exception) {
                                    Log.e("OrdersDebug", "Parse failed: ${e2.message}")
                                }
                            }
                        }

                        val sortedOrders = ordersList.sortedByDescending { it.createdAt }

                        if (sortedOrders.isNotEmpty()) {
                            tvEmptyState.visibility = View.GONE
                            ordersContainer.visibility = View.VISIBLE
                            for (order in sortedOrders) {
                                addOrderTicket(order)
                            }
                        } else {
                            showEmptyState("No orders found")
                        }
                    } else {
                        showEmptyState("No orders yet. Place your first order!")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@OrdersActivity,
                        "Failed to load orders: ${databaseError.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    showEmptyState("Failed to load orders")
                }
            })
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

            tvStationName.text = order.stationName ?: "Unknown Station"
            tvOrderStatus.text = order.status ?: "Pending"
            tvOrderDate.text = "Date: ${order.date ?: "N/A"}"
            tvTicketNumber.text = "Ref #: ${order.referenceNumber ?: "N/A"}"
            tvOrderItems.text = "Items: ${buildItemsSummary(order)}"
            tvOrderPrice.text = "₱${String.format("%.2f", order.grandTotal)}"

            setStatusStyle(tvOrderStatus, order.status ?: "Pending")

            ticketView.setOnClickListener {
                val intent = Intent(this, OrderSuccessActivity::class.java)
                intent.putExtra("FROM_ORDERS_LIST", true)
                intent.putExtra("ORDER_ID", order.orderId ?: "")
                intent.putExtra("REFERENCE_NUMBER", order.referenceNumber ?: "")
                intent.putExtra("STATION_NAME", order.stationName ?: "")
                intent.putExtra("CUSTOMER_NAME", order.customerName ?: "")
                intent.putExtra("ORDER_DATE", order.date ?: "")
                intent.putExtra("ORDER_TIME", order.time ?: "")
                intent.putExtra("ORDER_TYPE", order.orderType ?: "")
                intent.putExtra("TRANSACTION_FEE", order.transactionFee ?: 0.0)
                intent.putExtra("GRAND_TOTAL", order.grandTotal ?: 0.0)
                intent.putExtra("ORDER_STATUS", order.status ?: "Pending")
                intent.putExtra("PURE_WATER_QTY", order.pureWaterQty)
                intent.putExtra("SPRING_WATER_QTY", order.springWaterQty)
                intent.putExtra("MINERAL_WATER_QTY", order.mineralWaterQty)
                startActivity(intent)
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
        ordersContainer.visibility = View.GONE
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
            loadUserOrders()
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
