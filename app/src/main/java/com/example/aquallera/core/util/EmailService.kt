package com.example.aquallera.core.util

import com.example.aquallera.core.model.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object EmailService {

    suspend fun sendOrderConfirmation(order: Order): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("service_id", Constants.EMAILJS_SERVICE_ID)
                put("template_id", Constants.EMAILJS_TEMPLATE_ID)
                put("user_id", Constants.EMAILJS_PUBLIC_KEY)
                put("template_params", JSONObject().apply {
                    put("to_email", order.userEmail)
                    put("customer_name", order.customerName)
                    put("station_name", order.stationName)
                    put("order_type", order.orderType)
                    put("order_date", order.date)
                    put("order_time", order.time)
                    put("reference_number", order.referenceNumber)
                    put("total", String.format("%.2f", order.grandTotal))
                    put("status", order.status)
                })
            }

            val url = URL(Constants.EMAILJS_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode == 200
        } catch (_: Exception) {
            false
        }
    }
}
