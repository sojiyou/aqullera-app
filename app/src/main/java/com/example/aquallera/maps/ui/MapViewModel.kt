package com.example.aquallera.maps.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquallera.core.model.WaterStation
import com.example.aquallera.stations.data.StationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StationFilter(val label: String) {
    ALL("All"),
    ONLINE("Online"),
    OFFLINE("Offline"),
    PENDING("Pending"),
}

data class MapUiState(
    val stations: List<WaterStation> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val statusFilter: StationFilter = StationFilter.ALL,
    val selectedStation: WaterStation? = null,
    val userLat: Double? = null,
    val userLng: Double? = null,
    val flyToStationId: String? = null,
) {
    val filteredStations: List<WaterStation>
        get() = stations.filter { s ->
            when (statusFilter) {
                StationFilter.ALL -> true
                StationFilter.ONLINE -> s.status == "approved" && s.isOnline
                StationFilter.OFFLINE -> s.status == "approved" && !s.isOnline
                StationFilter.PENDING -> s.status != "approved"
            }
        }
}

@HiltViewModel
class MapViewModel @Inject constructor(
    private val stationRepository: StationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadStations()
    }

    private fun loadStations() {
        viewModelScope.launch {
            stationRepository.getAllStationsFlow().collect { stations ->
                _uiState.update { it.copy(stations = stations, loading = false) }
            }
        }
    }

    fun setFilter(filter: StationFilter) {
        _uiState.update { it.copy(statusFilter = filter) }
    }

    fun selectStation(station: WaterStation?) {
        _uiState.update { it.copy(selectedStation = station) }
    }

    fun setUserLocation(lat: Double, lng: Double) {
        _uiState.update { it.copy(userLat = lat, userLng = lng) }
    }

    fun flyToStation(stationId: String) {
        _uiState.update { it.copy(flyToStationId = stationId) }
    }

    fun clearFlyToStation() {
        _uiState.update { it.copy(flyToStationId = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun retry() {
        // no-op: flow auto-refreshes
    }
}
