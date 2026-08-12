package com.example.aquallera.createorder.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PendingOrderData(
    val stationId: String = "",
    val stationName: String = "",
    val pureWaterQty: Int = 0,
    val springWaterQty: Int = 0,
    val mineralWaterQty: Int = 0,
    val pureWaterPrice: Double = 0.0,
    val springWaterPrice: Double = 0.0,
    val mineralWaterPrice: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val waterSubtotal: Double = 0.0,
    val transactionFee: Double = 0.0,
    val grandTotal: Double = 0.0,
    val deliveryAddress: String = "",
    val locationDetails: String = "",
    val additionalDetails: String = "",
    val paymentMethod: String = "Cash on Delivery",
    val orderType: String = "Delivery",
    val preferredDate: String = "",
    val preferredTime: String = "",
    val deliveryLatitude: Double = 0.0,
    val deliveryLongitude: Double = 0.0,
)

/**
 * Holds the in-progress order between the Create Order and Confirmation screens.
 *
 * In-memory only: an order abandoned before confirmation is intentionally lost rather than
 * persisted, so nothing half-finished can be submitted after the app restarts.
 */
@Singleton
class PendingOrderStore @Inject constructor() {

    private val _data = MutableStateFlow(PendingOrderData())
    val data: StateFlow<PendingOrderData> = _data.asStateFlow()

    /** Stores the order awaiting confirmation, replacing any previous one. */
    fun set(order: PendingOrderData) {
        _data.value = order
    }

    /** Discards the pending order, called once it has been submitted. */
    fun clear() {
        _data.value = PendingOrderData()
    }
}
