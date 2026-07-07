package com.example.aquallera

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class OrderSuccessActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_success)

        initializeViews()
        setupClickListeners()
        displayOrderDetails()
    }

    private fun initializeViews() {
        // Views are bound via displayOrderDetails
    }

    private fun setupClickListeners() {
        findViewById<Button>(R.id.btnBackToMaps).setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.btnViewOrders).setOnClickListener {
            startActivity(Intent(this, OrdersActivity::class.java))
            finish()
        }
    }

    private fun displayOrderDetails() {
        val referenceNumber = intent.getStringExtra("REFERENCE_NUMBER") ?: "N/A"
        val stationName = intent.getStringExtra("STATION_NAME") ?: "N/A"
        val orderType = intent.getStringExtra("ORDER_TYPE") ?: "N/A"
        val date = intent.getStringExtra("ORDER_DATE") ?: "N/A"
        val grandTotal = intent.getDoubleExtra("GRAND_TOTAL", 0.0)

        findViewById<TextView>(R.id.tvReferenceNumber).text = referenceNumber
        findViewById<TextView>(R.id.tvStationName).text = stationName
        findViewById<TextView>(R.id.tvOrderType).text = orderType
        findViewById<TextView>(R.id.tvOrderDate).text = date
        findViewById<TextView>(R.id.tvGrandTotal).text = "₱${String.format("%.2f", grandTotal)}"
    }
}
