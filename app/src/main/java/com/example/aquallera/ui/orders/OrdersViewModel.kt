package com.example.aquallera.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquallera.auth.data.AuthRepository
import com.example.aquallera.core.model.Order
import com.example.aquallera.orders.data.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrdersUiState(
    val orders: List<Order> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val expandedOrderId: String? = null,
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    private fun loadOrders() {
        val user = authRepository.getCurrentUser() ?: run {
            _uiState.value = OrdersUiState(error = "Not logged in", loading = false)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            orderRepository.getOrdersByUser(user.uid).fold(
                onSuccess = { orders ->
                    _uiState.value = _uiState.value.copy(orders = orders, loading = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(loading = false, error = e.message)
                },
            )
        }
    }

    fun toggleExpand(orderId: String) {
        _uiState.value = _uiState.value.copy(
            expandedOrderId = if (_uiState.value.expandedOrderId == orderId) null else orderId,
        )
    }

    fun retry() {
        loadOrders()
    }
}
