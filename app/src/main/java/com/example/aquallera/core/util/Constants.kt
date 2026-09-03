package com.example.aquallera.core.util

/** Shared fee and third-party constants. */
object Constants {
    const val TRANSACTION_FEE_RATE = 0.02
    const val DEFAULT_DELIVERY_FEE = 50.0

    val BAGUIO_CENTER = doubleArrayOf(120.5931, 16.4164)

    const val EMAILJS_API_URL = "https://api.emailjs.com/api/v1.0/email/send"

    // EmailJS account 2 — used by the 6-digit OTP verification flow (matches PWA)
    const val EMAILJS_VERIFICATION_PUBLIC_KEY = "iq8pJ8vc0CW3xnPJ-"
    const val EMAILJS_VERIFICATION_SERVICE_ID = "service_hgrfj7d"
    const val EMAILJS_VERIFICATION_TEMPLATE_ID = "template_boi1jmq"

    const val MAPBOX_TOKEN = "pk.eyJ1Ijoic29qaXlvdSIsImEiOiJjbWk4ZXRybmgwYmxkMmlweHNiNHhnM3V2In0.zIlAkkuMeFdQV9FPR6OgkQ"
}
