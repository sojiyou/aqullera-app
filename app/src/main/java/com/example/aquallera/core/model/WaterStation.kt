package com.example.aquallera.core.model

import com.google.firebase.database.PropertyName
import kotlin.math.*

/** A water station record from `/waterStations`; keys use the admin site's exact field names. */
data class WaterStation(

    val id: String = "",

    @PropertyName("online") var isOnline: Boolean = false,

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
    /** Water types the station actually offers ("pure"/"spring"/"mineral"); empty means all three. */
    @PropertyName("waterTypes") val waterTypes: List<String>? = null,
    @PropertyName("businessPermitDocuments") val businessPermitDocuments: Map<String, Any>? = null,
    @PropertyName("about") val about: String = "",
    @PropertyName("revokedAt") val revokedAt: Long? = null,
    @PropertyName("createdAt") val createdAt: String = "",
    @PropertyName("updatedAt") val updatedAt: String = "",
) {
    /** Mirrors the PWA: an absent or empty waterTypes list means the station offers all three. */
    fun offers(type: String): Boolean =
        waterTypes.isNullOrEmpty() || waterTypes.any { it.equals(type, ignoreCase = true) }

    /**
     * Spring water price, preferring the per-litre field the admin site writes and falling back to
     * the per-gallon one.
     *
     * Kept here rather than at each call site: the store page, the station card and the order form
     * previously derived this separately, and the order form checked only the per-gallon field —
     * so a station priced per litre appeared to sell spring water but could not be ordered it.
     */
    val springPrice: Double?
        get() = pricing_liter_spring ?: pricing_gallon_spring

    /** Keys of uploaded permit documents (those with a filename or base64 payload). */
    val verifiedDocumentKeys: List<String>
        get() = businessPermitDocuments.orEmpty()
            .filterValues { doc ->
                (doc as? Map<*, *>)?.let { it["filename"] != null || it["base64"] != null } == true
            }
            .keys.toList()

    /** Straight-line distance to the user, using Android's location API with a fallback. */
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

/** Distance to a station in meters, a human-readable label, and a trust score for the lat/lng. */
data class DistanceResult(
    val meters: Float,
    val formatted: String,
    val accuracyScore: Double,
)
