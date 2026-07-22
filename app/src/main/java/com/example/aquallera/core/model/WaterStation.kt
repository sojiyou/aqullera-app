package com.example.aquallera.core.model

import com.google.firebase.database.PropertyName
import kotlin.math.*

data class WaterStation(

    val id: String = "",

    @get:PropertyName("isOnline")
    @set:PropertyName("isOnline")
    var online: Boolean = false,

    @PropertyName("status") val status: String = "pending",
    @PropertyName("stationName") val stationName: String = "",
    @PropertyName("ownerName") val ownerName: String = "",
    @PropertyName("email") val email: String = "",
    @PropertyName("phone") val phone: String = "",
    @PropertyName("address") val address: String = "",
    @PropertyName("city") val city: String = "",
    @PropertyName("state") val state: String = "",
    @PropertyName("zipCode") val zipCode: String = "",

    @PropertyName("latitude") val latitude: Double = 0.0,
    @PropertyName("longitude") val longitude: Double = 0.0,

    @PropertyName("pricing_gallon_pure") val pricing_gallon_pure: Double? = null,
    @PropertyName("pricing_liter_spring") val pricing_liter_spring: Double? = null,
    @PropertyName("pricing_gallon_mineral") val pricing_gallon_mineral: Double? = null,
    @PropertyName("pricing_gallon_spring") val pricing_gallon_spring: Double? = null,
    @PropertyName("pricing_delivery_fee") val pricing_delivery_fee: Double? = null,

    @PropertyName("deliveryRadius") val deliveryRadius: Long = 5L,
    @PropertyName("deliveryHours") val deliveryHours: List<String>? = null,
    @PropertyName("businessHours") val businessHours: Map<String, String> = emptyMap(),
    @PropertyName("serviceTypes") val serviceTypes: List<String> = emptyList(),
    @PropertyName("deliveryDays") val deliveryDays: List<String>? = null,
    @PropertyName("about") val about: String = "",
    @PropertyName("revokedAt") val revokedAt: Long? = null,
    @PropertyName("createdAt") val createdAt: String = "",
    @PropertyName("updatedAt") val updatedAt: String = "",
) {
    val isOnline: Boolean get() = online

    fun calculateDistanceTo(userLat: Double, userLon: Double): DistanceResult {
        if (latitude == 0.0 && longitude == 0.0) {
            return DistanceResult(meters = -1f, formatted = "Unknown", accuracyScore = 0.0)
        }
        val results = FloatArray(1)
        val distanceMeters = try {
            android.location.Location.distanceBetween(latitude, longitude, userLat, userLon, results)
            results[0]
        } catch (_: Exception) {
            haversine(latitude, longitude, userLat, userLon).toFloat()
        }
        return DistanceResult(
            meters = distanceMeters,
            formatted = formatDistance(distanceMeters.toDouble()),
            accuracyScore = calculateAccuracyScore(latitude, longitude),
        )
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun isWithinDeliveryRange(userLat: Double, userLon: Double): Boolean {
        val distance = calculateDistanceTo(userLat, userLon).meters
        return distance <= deliveryRadius * 1000
    }

    fun hasDeliveryService(): Boolean = serviceTypes.contains("delivery")

    private fun formatDistance(meters: Double): String = when {
        meters < 0 -> "Unknown"
        meters < 1000 -> "${meters.roundToInt()} m"
        meters < 10000 -> "${(meters / 1000).roundToDecimal(1)} km"
        else -> "${(meters / 1000).roundToInt()} km"
    }

    private fun calculateAccuracyScore(lat: Double, lon: Double): Double = when {
        lat == 0.0 || lon == 0.0 -> 0.0
        lat < -90 || lat > 90 || lon < -180 || lon > 180 -> 0.1
        else -> 1.0
    }

    private fun Double.roundToDecimal(places: Int): String = "%.${places}f".format(this)
    private fun Double.roundToInt(): Int = round(this).toInt()
}

data class DistanceResult(
    val meters: Float,
    val formatted: String,
    val accuracyScore: Double,
)
