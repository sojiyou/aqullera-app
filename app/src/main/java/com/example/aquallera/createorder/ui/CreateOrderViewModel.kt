package com.example.aquallera.createorder.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquallera.core.model.WaterStation
import com.example.aquallera.core.util.Constants
import com.example.aquallera.createorder.data.PendingOrderData
import com.example.aquallera.createorder.data.PendingOrderStore
import com.example.aquallera.stations.data.StationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateOrderUiState(
    val station: WaterStation? = null,
    val pureWaterQty: Int = 0,
    val springWaterQty: Int = 0,
    val mineralWaterQty: Int = 0,
    val deliveryAddress: String = "",
    val locationDetails: String = "",
    val additionalDetails: String = "",
    val loading: Boolean = true,
    val error: String? = null,
) {
    val hasPureWater: Boolean get() = station?.pricing_gallon_pure != null && station!!.pricing_gallon_pure > 0
    val hasSpringWater: Boolean get() = station?.pricing_gallon_spring != null && station!!.pricing_gallon_spring > 0
    val hasMineralWater: Boolean get() = station?.pricing_gallon_mineral != null && station!!.pricing_gallon_mineral > 0

    val pureWaterPrice: Double get() = station?.pricing_gallon_pure ?: 0.0
    val springWaterPrice: Double get() = station?.pricing_gallon_spring ?: 0.0
    val mineralWaterPrice: Double get() = station?.pricing_gallon_mineral ?: 0.0

    val pureWaterTotal: Double get() = pureWaterQty * pureWaterPrice
    val springWaterTotal: Double get() = springWaterQty * springWaterPrice
    val mineralWaterTotal: Double get() = mineralWaterQty * mineralWaterPrice
    val waterSubtotal: Double get() = pureWaterTotal + springWaterTotal + mineralWaterTotal
    val deliveryFee: Double get() = station?.pricing_delivery_fee ?: Constants.DEFAULT_DELIVERY_FEE
    val transactionFee: Double get() = waterSubtotal * Constants.TRANSACTION_FEE_RATE
    val grandTotal: Double get() = waterSubtotal + deliveryFee + transactionFee

    val hasAnyItem: Boolean get() = pureWaterQty > 0 || springWaterQty > 0 || mineralWaterQty > 0
}

@HiltViewModel
class CreateOrderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val stationRepository: StationRepository,
    private val pendingOrderStore: PendingOrderStore,
) : ViewModel() {

    private val stationId: String = savedStateHandle.get<String>("stationId") ?: ""

    private val _uiState = MutableStateFlow(CreateOrderUiState())
    val uiState: StateFlow<CreateOrderUiState> = _uiState.asStateFlow()

    init {
        loadStation()
    }

    private fun loadStation() {
        if (stationId.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Invalid station", loading = false)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            stationRepository.getStationById(stationId).fold(
                onSuccess = { station ->
                    _uiState.value = _uiState.value.copy(
                        station = station,
                        loading = false,
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(loading = false, error = e.message ?: "Failed to load station")
                },
            )
        }
    }

    fun onPureWaterQtyChanged(qty: Int) { _uiState.value = _uiState.value.copy(pureWaterQty = maxOf(0, qty)) }
    fun onSpringWaterQtyChanged(qty: Int) { _uiState.value = _uiState.value.copy(springWaterQty = maxOf(0, qty)) }
    fun onMineralWaterQtyChanged(qty: Int) { _uiState.value = _uiState.value.copy(mineralWaterQty = maxOf(0, qty)) }
    fun onDeliveryAddressChanged(v: String) { _uiState.value = _uiState.value.copy(deliveryAddress = v) }
    fun onLocationDetailsChanged(v: String) { _uiState.value = _uiState.value.copy(locationDetails = v) }
    fun onAdditionalDetailsChanged(v: String) { _uiState.value = _uiState.value.copy(additionalDetails = v) }

    fun retry() { loadStation() }

    fun proceedToConfirmation(): Boolean {
        val s = _uiState.value
        if (!s.hasAnyItem) return false
        if (s.deliveryAddress.isBlank() && s.locationDetails.isBlank()) return false

        pendingOrderStore.set(
            PendingOrderData(
                stationId = stationId,
                stationName = s.station?.stationName ?: "",
                pureWaterQty = s.pureWaterQty,
                springWaterQty = s.springWaterQty,
                mineralWaterQty = s.mineralWaterQty,
                pureWaterPrice = s.pureWaterPrice,
                springWaterPrice = s.springWaterPrice,
                mineralWaterPrice = s.mineralWaterPrice,
                deliveryFee = s.deliveryFee,
                waterSubtotal = s.waterSubtotal,
                transactionFee = s.transactionFee,
                grandTotal = s.grandTotal,
                deliveryAddress = s.deliveryAddress,
                locationDetails = s.locationDetails,
                additionalDetails = s.additionalDetails,
            )
        )
        return true
    }
}
