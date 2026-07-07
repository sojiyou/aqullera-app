package com.example.aquallera

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object EmailService {

    private const val API_URL = "https://api.emailjs.com/api/v1.0/email/send"
    private const val SERVICE_ID = "service_6q0e89w"
    private const val TEMPLATE_ID = "template_2rk5qyq"
    private const val PUBLIC_KEY = "fpu4u65UlHZOE96yR"

    suspend fun sendOrderConfirmation(
        toEmail: String,
        customerName: String,
        stationName: String,
        orderType: String,
        orderDate: String,
        orderTime: String,
        referenceNumber: String,
        total: String,
        status: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val params = JSONObject().apply {
                put("to_email", toEmail)
                put("customer_name", customerName)
                put("station_name", stationName)
                put("order_type", orderType)
                put("order_date", orderDate)
                put("order_time", orderTime)
                put("reference_number", referenceNumber)
                put("total", total)
                put("status", status)
            }

            val body = JSONObject().apply {
                put("service_id", SERVICE_ID)
                put("template_id", TEMPLATE_ID)
                put("user_id", PUBLIC_KEY)
                put("template_params", params)
            }

            val url = URL(API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(body.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            connection.disconnect()

            responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }
}
