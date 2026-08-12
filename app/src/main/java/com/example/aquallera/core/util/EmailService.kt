package com.example.aquallera.core.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/** Sends the 6-digit verification emails through the EmailJS REST API. */
object EmailService {

    /** Emails [code] to [email]; true only when EmailJS responds 200. */
    suspend fun sendVerificationCode(email: String, code: String): Boolean = withContext(Dispatchers.IO) {
        post(
            JSONObject().apply {
                put("service_id", Constants.EMAILJS_VERIFICATION_SERVICE_ID)
                put("template_id", Constants.EMAILJS_VERIFICATION_TEMPLATE_ID)
                put("user_id", Constants.EMAILJS_VERIFICATION_PUBLIC_KEY)
                put("template_params", JSONObject().apply {
                    put("to_email", email)
                    put("code", code)
                    put("app_name", "Aquallera")
                })
            }
        )
    }

    private fun post(payload: JSONObject): Boolean {
        return try {
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
