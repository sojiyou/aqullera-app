package com.example.aquallera.store.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquallera.core.model.WaterStation
import com.example.aquallera.stations.data.StationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoreUiState(
    val station: WaterStation? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class StoreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val stationRepository: StationRepository,
) : ViewModel() {

    private val stationId: String = savedStateHandle.get<String>("stationId") ?: ""

    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    init {
        if (stationId.isBlank()) {
            _uiState.value = StoreUiState(error = "Invalid station ID", loading = false)
        } else {
            viewModelScope.launch {
                stationRepository.getStationByIdFlow(stationId).collect { station ->
                    _uiState.value = StoreUiState(station = station, loading = false)
                }
            }
        }
    }

    fun retry() {
        // no-op: flow auto-refreshes
    }
}
