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
)

@Singleton
class PendingOrderStore @Inject constructor() {

    private val _data = MutableStateFlow(PendingOrderData())
    val data: StateFlow<PendingOrderData> = _data.asStateFlow()

    fun set(order: PendingOrderData) {
        _data.value = order
    }

    fun clear() {
        _data.value = PendingOrderData()
    }
}
