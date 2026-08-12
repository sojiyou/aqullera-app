package com.example.aquallera.store.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquallera.core.model.WaterStation
import com.example.aquallera.stations.data.StationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoreUiState(
    val station: WaterStation? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

/** Backs Store Details, observing one station so admin-side edits appear live. */
@HiltViewModel
class StoreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val stationRepository: StationRepository,
) : ViewModel() {

    private val stationId: String = savedStateHandle.get<String>("stationId") ?: ""

    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        observeStation()
    }

    /**
     * Subscribes to this station's record so admin-side edits (online/offline, pricing) appear
     * without reopening the screen. Cancels any previous subscription first.
     */
    private fun observeStation() {
        if (stationId.isBlank()) {
            _uiState.value = StoreUiState(error = "Invalid station ID", loading = false)
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = StoreUiState(loading = true)
            stationRepository.getStationByIdFlow(stationId)
                .catch { e ->
                    _uiState.value = StoreUiState(
                        loading = false,
                        error = e.message ?: "Failed to load this station",
                    )
                }
                .collect { station ->
                    _uiState.value = StoreUiState(station = station, loading = false)
                }
        }
    }

    /** Re-subscribes after a load failure — the Retry button in the error state calls this. */
    fun retry() {
        observeStation()
    }
}
