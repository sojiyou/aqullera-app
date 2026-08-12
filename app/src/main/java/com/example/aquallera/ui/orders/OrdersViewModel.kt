package com.example.aquallera.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquallera.auth.data.AuthRepository
import com.example.aquallera.core.model.Order
import com.example.aquallera.orders.data.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrdersUiState(
    val orders: List<Order> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val filterStatus: String = "all",
    val selectedOrderId: String? = null,
)

/** Backs the Orders tab: status filtering, order selection and cancellation. */
@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadOrders()
    }

    /**
     * Cancels any orders whose station was revoked, then subscribes to this user's orders so
     * status changes made on the admin site appear without reopening the tab. Any previous
     * subscription is cancelled first so [retry] cannot leave two collectors running.
     */
    private fun loadOrders() {
        val user = authRepository.getCurrentUser() ?: run {
            _uiState.value = OrdersUiState(error = "Not logged in", loading = false)
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            orderRepository.autoCancelRevokedOrdersForUser(user.uid)
            orderRepository.getOrdersByUserFlow(user.uid)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = e.message ?: "Failed to load your orders",
                    )
                }
                .collect { orders ->
                    _uiState.value = _uiState.value.copy(orders = orders, loading = false, error = null)
                }
        }
    }

    /** Orders matching the active status tab; all orders when the tab is "all". */
    val filteredOrders: List<Order>
        get() {
            val state = _uiState.value
            return if (state.filterStatus == "all") state.orders
            else state.orders.filter { it.status.equals(state.filterStatus, ignoreCase = true) }
        }

    /** Switches the status tab. Filtering is local, so no refetch is needed. */
    fun setFilterStatus(status: String) {
        _uiState.value = _uiState.value.copy(filterStatus = status)
    }

    /** Opens an order's receipt, or returns to the list when [orderId] is null. */
    fun selectOrder(orderId: String?) {
        _uiState.value = _uiState.value.copy(selectedOrderId = orderId)
    }

    /**
     * Marks an order cancelled. No local state update is needed — the order flow re-emits, which
     * is what keeps the receipt's Cancel button from lingering after a successful cancel.
     */
    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            orderRepository.updateOrderStatus(orderId, "Cancelled")
        }
    }

    /** Reloads after a failure — the Retry button in the error state calls this. */
    fun retry() {
        loadOrders()
    }
}
