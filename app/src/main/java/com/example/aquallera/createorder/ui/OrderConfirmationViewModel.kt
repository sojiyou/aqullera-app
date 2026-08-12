package com.example.aquallera.createorder.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquallera.auth.data.AuthRepository
import com.example.aquallera.core.model.Order
import com.example.aquallera.createorder.data.PendingOrderStore
import com.example.aquallera.orders.data.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ConfirmationUiState(
    val confirming: Boolean = false,
    val confirmed: Boolean = false,
    val orderId: String = "",
    val error: String? = null,
)

/** Backs the confirmation screen, turning the pending order into a stored order. */
@HiltViewModel
class OrderConfirmationViewModel @Inject constructor(
    private val pendingOrderStore: PendingOrderStore,
    private val orderRepository: OrderRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfirmationUiState())
    val uiState: StateFlow<ConfirmationUiState> = _uiState.asStateFlow()

    val pendingOrder = pendingOrderStore.data

    /** Writes the order and reports the new id, or an error, through the UI state. */
    fun confirmOrder() {
        val data = pendingOrderStore.data.value
        if (data.stationId.isBlank()) return

        val user = authRepository.getCurrentUser() ?: run {
            _uiState.value = _uiState.value.copy(error = "You must be logged in")
            return
        }

        _uiState.value = _uiState.value.copy(confirming = true, error = null)

        viewModelScope.launch {
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
            val phone = authRepository.getUserPhone(user.uid)
            val order = Order(
                stationId = data.stationId,
                stationName = data.stationName,
                customerId = user.uid,
                userId = user.uid,
                userEmail = user.email ?: "",
                customerName = user.displayName ?: "",
                customerPhone = phone,
                orderType = data.orderType,
                date = data.preferredDate.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) },
                time = data.preferredTime.ifBlank { SimpleDateFormat("h:mm a", Locale.US).format(Date()) },
                pureWaterQty = data.pureWaterQty,
                springWaterQty = data.springWaterQty,
                mineralWaterQty = data.mineralWaterQty,
                pureWaterPrice = data.pureWaterPrice,
                springWaterPrice = data.springWaterPrice,
                mineralWaterPrice = data.mineralWaterPrice,
                pureWaterTotal = data.pureWaterQty * data.pureWaterPrice,
                springWaterTotal = data.springWaterQty * data.springWaterPrice,
                mineralWaterTotal = data.mineralWaterQty * data.mineralWaterPrice,
                waterSubtotal = data.waterSubtotal,
                deliveryFee = data.deliveryFee,
                transactionFee = data.transactionFee,
                grandTotal = data.grandTotal,
                deliveryAddress = data.deliveryAddress,
                locationDetails = data.locationDetails,
                additionalDetails = data.additionalDetails,
                deliveryLatitude = data.deliveryLatitude,
                deliveryLongitude = data.deliveryLongitude,
                paymentMethod = data.paymentMethod,
                createdAt = now,
                updatedAt = now,
            )

            orderRepository.createOrder(order).fold(
                onSuccess = { orderId ->
                    pendingOrderStore.clear()
                    _uiState.value = ConfirmationUiState(confirmed = true, orderId = orderId)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        confirming = false,
                        error = e.message ?: "Failed to create order",
                    )
                },
            )
        }
    }
}
