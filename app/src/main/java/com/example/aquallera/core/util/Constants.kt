package com.example.aquallera.core.util

object Constants {
    const val TRANSACTION_FEE_RATE = 0.02
    const val DEFAULT_DELIVERY_FEE = 50.0

    const val ORDER_STATUS_PENDING = "Pending"
    const val ORDER_STATUS_CONFIRMED = "Confirmed"
    const val ORDER_STATUS_PREPARING = "Preparing"
    const val ORDER_STATUS_ON_DELIVERY = "On Delivery"
    const val ORDER_STATUS_READY = "Ready"
    const val ORDER_STATUS_COMPLETED = "Completed"
    const val ORDER_STATUS_CANCELLED = "Cancelled"

    const val MAP_MIN_ZOOM = 11.0
    const val MAP_MAX_ZOOM = 18.0

    val BAGUIO_CENTER = doubleArrayOf(120.5931, 16.4164)
    val BAGUIO_BOUNDS_SOUTH_WEST = doubleArrayOf(120.5200, 16.3600)
    val BAGUIO_BOUNDS_NORTH_EAST = doubleArrayOf(120.6700, 16.4600)

    const val EMAILJS_SERVICE_ID = "service_6q0e89w"
    const val EMAILJS_TEMPLATE_ID = "template_2rk5qyq"
    const val EMAILJS_PUBLIC_KEY = "fpu4u65UlHZOE96yR"
    const val EMAILJS_API_URL = "https://api.emailjs.com/api/v1.0/email/send"

    const val MAPBOX_TOKEN = "pk.eyJ1Ijoic29qaXlvdSIsImEiOiJjbWk4ZXRybmgwYmxkMmlweHNiNHhnM3V2In0.zIlAkkuMeFdQV9FPR6OgkQ"
}
