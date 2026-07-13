package com.example.aquallera

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class OrderConfirmationActivity : AppCompatActivity() {

    private var transactionFee: Double = 0.0

    // Receipt header
    private lateinit var tvStationName: TextView
    private lateinit var tvReferenceNumber: TextView
    private lateinit var tvOrderTypeBadge: TextView

    // Customer details
    private lateinit var tvCustomerName: TextView
    private lateinit var tvCustomerPhone: TextView
    private lateinit var tvOrderDate: TextView
    private lateinit var tvOrderTime: TextView
    private lateinit var tvDeliveryAddress: TextView
    private lateinit var tvAdditionalDetails: TextView
    private lateinit var layoutAddress: LinearLayout
    private lateinit var layoutNotes: LinearLayout

    // Order items
    private lateinit var layoutPureWater: LinearLayout
    private lateinit var tvPureWaterName: TextView
    private lateinit var tvPureWaterQty: TextView
    private lateinit var tvPureWaterAmount: TextView
    private lateinit var layoutSpringWater: LinearLayout
    private lateinit var tvSpringWaterName: TextView
    private lateinit var tvSpringWaterQty: TextView
    private lateinit var tvSpringWaterAmount: TextView
    private lateinit var layoutMineralWater: LinearLayout
    private lateinit var tvMineralWaterName: TextView
    private lateinit var tvMineralWaterQty: TextView
    private lateinit var tvMineralWaterAmount: TextView

    // Summary
    private lateinit var tvSubtotal: TextView
    private lateinit var tvDeliveryFee: TextView
    private lateinit var tvTransactionFee: TextView
    private lateinit var tvGrandTotal: TextView
    private lateinit var layoutDeliveryFee: LinearLayout
    private lateinit var layoutTransactionFee: LinearLayout

    // Buttons
    private lateinit var btnConfirmOrder: Button
    private lateinit var btnEditOrder: Button
    private lateinit var btnBack: ImageButton

    // Data
    private var currentStation: WaterStation? = null
    private lateinit var orderTotal: CreateOrderActivity.OrderTotal
    private var orderDate: String = ""
    private var orderTime: String = ""
    private var orderType: String = ""
    private var userAddress: String = ""
    private var manualAddress: String = ""
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0
    private var additionalDetails: String = ""
    private var referenceNumber: String = ""
    private var customerPhone: String = ""

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_confirmation)

        try {
            auth = FirebaseAuth.getInstance()
            database = FirebaseDatabase.getInstance(
                "https://aquallera-default-rtdb.asia-southeast1.firebasedatabase.app"
            )

            getIntentData()
            initializeViews()
            setupClickListeners()
            displayOrderDetails()

        } catch (e: Exception) {
            Toast.makeText(this, "App Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getIntentData() {
        try {
            currentStation = intent.getSerializableExtra("WATER_STATION") as? WaterStation
            orderTotal = intent.getSerializableExtra("ORDER_TOTAL") as CreateOrderActivity.OrderTotal
            orderDate = intent.getStringExtra("ORDER_DATE") ?: ""
            orderTime = intent.getStringExtra("ORDER_TIME") ?: ""
            orderType = intent.getStringExtra("ORDER_TYPE") ?: ""
            userAddress = intent.getStringExtra("USER_ADDRESS") ?: ""
            manualAddress = intent.getStringExtra("MANUAL_ADDRESS") ?: ""
            userLatitude = intent.getDoubleExtra("USER_LATITUDE", 0.0)
            userLongitude = intent.getDoubleExtra("USER_LONGITUDE", 0.0)
            additionalDetails = intent.getStringExtra("ADDITIONAL_DETAILS") ?: ""
            customerPhone = intent.getStringExtra("CONTACT_NUMBER") ?: ""
            transactionFee = orderTotal.transactionFee
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeViews() {
        tvStationName = findViewById(R.id.tvStationName)
        tvReferenceNumber = findViewById(R.id.tvReferenceNumber)
        tvOrderTypeBadge = findViewById(R.id.tvOrderTypeBadge)

        tvCustomerName = findViewById(R.id.tvCustomerName)
        tvCustomerPhone = findViewById(R.id.tvCustomerPhone)
        tvOrderDate = findViewById(R.id.tvOrderDate)
        tvOrderTime = findViewById(R.id.tvOrderTime)
        tvDeliveryAddress = findViewById(R.id.tvDeliveryAddress)
        tvAdditionalDetails = findViewById(R.id.tvAdditionalDetails)
        layoutAddress = findViewById(R.id.layoutAddress)
        layoutNotes = findViewById(R.id.layoutNotes)

        layoutPureWater = findViewById(R.id.layoutPureWater)
        tvPureWaterName = findViewById(R.id.tvPureWaterName)
        tvPureWaterQty = findViewById(R.id.tvPureWaterQty)
        tvPureWaterAmount = findViewById(R.id.tvPureWaterAmount)
        layoutSpringWater = findViewById(R.id.layoutSpringWater)
        tvSpringWaterName = findViewById(R.id.tvSpringWaterName)
        tvSpringWaterQty = findViewById(R.id.tvSpringWaterQty)
        tvSpringWaterAmount = findViewById(R.id.tvSpringWaterAmount)
        layoutMineralWater = findViewById(R.id.layoutMineralWater)
        tvMineralWaterName = findViewById(R.id.tvMineralWaterName)
        tvMineralWaterQty = findViewById(R.id.tvMineralWaterQty)
        tvMineralWaterAmount = findViewById(R.id.tvMineralWaterAmount)

        tvSubtotal = findViewById(R.id.tvSubtotal)
        tvDeliveryFee = findViewById(R.id.tvDeliveryFee)
        tvTransactionFee = findViewById(R.id.tvTransactionFee)
        tvGrandTotal = findViewById(R.id.tvGrandTotal)
        layoutDeliveryFee = findViewById(R.id.layoutDeliveryFee)
        layoutTransactionFee = findViewById(R.id.layoutTransactionFee)

        btnConfirmOrder = findViewById(R.id.btnConfirmOrder)
        btnEditOrder = findViewById(R.id.btnEditOrder)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupClickListeners() {
        btnConfirmOrder.setOnClickListener { saveOrderToFirebase() }

        btnEditOrder.setOnClickListener { finish() }

        btnBack.setOnClickListener { finish() }
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun displayOrderDetails() {
        try {
            tvStationName.text = currentStation?.stationName ?: "Water Station"
            tvReferenceNumber.text = "Generating..."
            tvOrderTypeBadge.text = orderType

            val currentUser = auth.currentUser
            val customerName = currentUser?.displayName ?: "Customer"
            tvCustomerName.text = customerName
            tvCustomerPhone.text = customerPhone

            tvOrderDate.text = orderDate
            tvOrderTime.text = if (orderTime.isNotEmpty()) formatTime(orderTime) else orderTime

            if (orderType == "Delivery") {
                val displayAddress = when {
                    manualAddress.isNotEmpty() -> manualAddress
                    userAddress.isNotEmpty() && !userAddress.contains("Lat:") -> userAddress
                    else -> "Address will be provided during delivery"
                }
                tvDeliveryAddress.text = displayAddress
                layoutAddress.visibility = View.VISIBLE
            } else {
                layoutAddress.visibility = View.GONE
            }

            displayOrderItems()

            tvSubtotal.text = "₱${String.format("%.2f", orderTotal.waterSubtotal)}"
            tvTransactionFee.text = "₱${String.format("%.2f", transactionFee)}"
            layoutTransactionFee.visibility = View.VISIBLE

            if (orderTotal.deliveryFee > 0 && orderType == "Delivery") {
                tvDeliveryFee.text = "₱${String.format("%.2f", orderTotal.deliveryFee)}"
                layoutDeliveryFee.visibility = View.VISIBLE
            } else {
                layoutDeliveryFee.visibility = View.GONE
            }

            val grandTotal = orderTotal.waterSubtotal +
                    (if (orderType == "Delivery") orderTotal.deliveryFee else 0.0) +
                    transactionFee
            tvGrandTotal.text = "₱${String.format("%.2f", grandTotal)}"

            findViewById<TextView>(R.id.tvPaymentMethod)?.text = "Payment Method: Cash on Delivery"

            if (additionalDetails.isNotEmpty()) {
                tvAdditionalDetails.text = additionalDetails
                layoutNotes.visibility = View.VISIBLE
            } else {
                layoutNotes.visibility = View.GONE
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("DefaultLocale")
    private fun displayOrderItems() {
        if (orderTotal.pureWaterQty > 0) {
            tvPureWaterName.text = "Pure Water"
            tvPureWaterQty.text = orderTotal.pureWaterQty.toString()
            tvPureWaterAmount.text = "₱${String.format("%.2f", orderTotal.pureWaterTotal)}"
            layoutPureWater.visibility = View.VISIBLE
        } else {
            layoutPureWater.visibility = View.GONE
        }

        if (orderTotal.springWaterQty > 0) {
            tvSpringWaterName.text = "Spring Water"
            tvSpringWaterQty.text = orderTotal.springWaterQty.toString()
            tvSpringWaterAmount.text = "₱${String.format("%.2f", orderTotal.springWaterTotal)}"
            layoutSpringWater.visibility = View.VISIBLE
        } else {
            layoutSpringWater.visibility = View.GONE
        }

        if (orderTotal.mineralWaterQty > 0) {
            tvMineralWaterName.text = "Mineral Water"
            tvMineralWaterQty.text = orderTotal.mineralWaterQty.toString()
            tvMineralWaterAmount.text = "₱${String.format("%.2f", orderTotal.mineralWaterTotal)}"
            layoutMineralWater.visibility = View.VISIBLE
        } else {
            layoutMineralWater.visibility = View.GONE
        }
    }

    private fun formatTime(time: String): String {
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

    private fun generateReferenceNumber(callback: (String) -> Unit) {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val counterRef = database.reference.child("orderCounter").child(dateStr)

        val stationPrefix = currentStation?.stationName
            ?.filter { it.isLetter() }
            ?.take(3)
            ?.uppercase()
            ?: "WTR"

        counterRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                var currentCount = currentData.getValue(Int::class.java) ?: 0
                currentCount++
                currentData.value = currentCount
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    Log.e("OrderConfirmation", "Failed to generate reference: ${error.message}")
                    callback("$stationPrefix-${dateStr}-0000")
                } else {
                    val count = currentData?.getValue(Int::class.java) ?: 0
                    val counterString = count.toString().padStart(4, '0')
                    val orderId = "$stationPrefix-$dateStr-$counterString"
                    callback(orderId)
                }
            }
        })
    }

    private fun createOrderObject(customerId: String, orderId: String): Order {
        val user = auth.currentUser
        val customerName = user?.displayName ?: "Customer"

        val grandTotal = orderTotal.waterSubtotal +
                (if (orderType == "Delivery") orderTotal.deliveryFee else 0.0) +
                transactionFee

        val displayAddress = if (manualAddress.isNotEmpty()) {
            manualAddress
        } else {
            userAddress
        }

        return Order(
            orderId = orderId,
            stationId = currentStation?.id ?: "",
            stationName = currentStation?.stationName ?: "",
            customerId = customerId,
            userId = customerId,
            userEmail = user?.email ?: "",
            customerName = customerName,
            customerPhone = customerPhone,
            orderType = orderType,
            date = orderDate,
            time = orderTime,
            pureWaterQty = orderTotal.pureWaterQty,
            springWaterQty = orderTotal.springWaterQty,
            mineralWaterQty = orderTotal.mineralWaterQty,
            pureWaterPrice = orderTotal.pureWaterPrice,
            springWaterPrice = orderTotal.springWaterPrice,
            mineralWaterPrice = orderTotal.mineralWaterPrice,
            pureWaterTotal = orderTotal.pureWaterTotal,
            springWaterTotal = orderTotal.springWaterTotal,
            mineralWaterTotal = orderTotal.mineralWaterTotal,
            waterSubtotal = orderTotal.waterSubtotal,
            deliveryFee = if (orderType == "Delivery") orderTotal.deliveryFee else 0.0,
            transactionFee = transactionFee,
            grandTotal = grandTotal,
            locationDetails = displayAddress,
            deliveryAddress = if (orderType == "Delivery") displayAddress else "",
            deliveryLatitude = if (orderType == "Delivery") userLatitude else 0.0,
            deliveryLongitude = if (orderType == "Delivery") userLongitude else 0.0,
            additionalDetails = additionalDetails,
            paymentMethod = "Cash on Delivery",
            status = "pending",
            createdAt = System.currentTimeMillis(),
            updatedAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()),
            referenceNumber = referenceNumber
        )
    }

    @SuppressLint("SetTextI18n")
    private fun saveOrderToFirebase() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login to continue", Toast.LENGTH_SHORT).show()
            return
        }

        btnConfirmOrder.isEnabled = false
        btnConfirmOrder.text = "Processing..."

        try {
            generateReferenceNumber { orderId ->
                referenceNumber = orderId
                tvReferenceNumber.text = "Ref: $referenceNumber"

                val order = createOrderObject(currentUser.uid, orderId)

                database.reference.child("orders").child(orderId)
                    .setValue(order)
                    .addOnSuccessListener {
                        // Send confirmation email (fire-and-forget, catch silently)
                        lifecycleScope.launch {
                            try {
                                EmailService.sendOrderConfirmation(
                                    toEmail = currentUser.email ?: "",
                                    customerName = order.customerName,
                                    stationName = order.stationName,
                                    orderType = order.orderType,
                                    orderDate = order.date,
                                    orderTime = order.time,
                                    referenceNumber = order.referenceNumber,
                                    total = String.format("%.2f", order.grandTotal),
                                    status = order.status
                                )
                            } catch (e: Exception) {
                                // Email failure does not block order placement
                            }
                        }
                        navigateToOrderSuccess(order)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save order: ${e.message}", Toast.LENGTH_SHORT).show()
                        btnConfirmOrder.isEnabled = true
                        btnConfirmOrder.text = "Confirm Order"
                    }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            btnConfirmOrder.isEnabled = true
            btnConfirmOrder.text = "Confirm Order"
        }
    }

    private fun navigateToOrderSuccess(order: Order) {
        try {
            val intent = Intent(this, OrderSuccessActivity::class.java)
            intent.putExtra("ORDER_ID", order.orderId)
            intent.putExtra("REFERENCE_NUMBER", order.referenceNumber)
            intent.putExtra("STATION_NAME", order.stationName)
            intent.putExtra("CUSTOMER_NAME", order.customerName)
            intent.putExtra("ORDER_DATE", order.date)
            intent.putExtra("ORDER_TIME", order.time)
            intent.putExtra("ORDER_TYPE", order.orderType)
            intent.putExtra("GRAND_TOTAL", order.grandTotal)
            intent.putExtra("TRANSACTION_FEE", order.transactionFee)
            intent.putExtra("ORDER_STATUS", order.status)

            intent.putExtra("PURE_WATER_QTY", order.pureWaterQty)
            intent.putExtra("SPRING_WATER_QTY", order.springWaterQty)
            intent.putExtra("MINERAL_WATER_QTY", order.mineralWaterQty)
            intent.putExtra("PURE_WATER_PRICE", order.pureWaterPrice)
            intent.putExtra("SPRING_WATER_PRICE", order.springWaterPrice)
            intent.putExtra("MINERAL_WATER_PRICE", order.mineralWaterPrice)
            intent.putExtra("PURE_WATER_TOTAL", order.pureWaterTotal)
            intent.putExtra("SPRING_WATER_TOTAL", order.springWaterTotal)
            intent.putExtra("MINERAL_WATER_TOTAL", order.mineralWaterTotal)
            intent.putExtra("WATER_SUBTOTAL", order.waterSubtotal)
            intent.putExtra("DELIVERY_FEE", order.deliveryFee)

            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
