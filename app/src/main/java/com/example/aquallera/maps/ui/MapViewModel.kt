package com.example.aquallera.maps.ui

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The All/Online/Offline/Pending choices on the map's filter bar. */
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

/** Backs the map: station list, status filter, selection and the user-location marker. */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val stationRepository: StationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadStations()
    }

    /**
     * Subscribes to the station list. Any previous subscription is cancelled first so [retry]
     * cannot leave two collectors running.
     */
    private fun loadStations() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            stationRepository.getAllStationsFlow()
                .catch { e ->
                    _uiState.update {
                        it.copy(loading = false, error = e.message ?: "Failed to load water stations")
                    }
                }
                .collect { stations ->
                    _uiState.update { it.copy(stations = stations, loading = false, error = null) }
                }
        }
    }

    /** Switches the All/Online/Offline/Pending filter. Filtering is local, so no refetch. */
    fun setFilter(filter: StationFilter) {
        _uiState.update { it.copy(statusFilter = filter) }
    }

    /** Selects the station shown in the map callout, or clears it when null. */
    fun selectStation(station: WaterStation?) {
        _uiState.update { it.copy(selectedStation = station) }
    }

    /** Records the resolved device position, which draws the blue marker. */
    fun setUserLocation(lat: Double, lng: Double) {
        _uiState.update { it.copy(userLat = lat, userLng = lng) }
    }

    /** Requests a camera move to a station; the map clears the request once handled. */
    fun flyToStation(stationId: String) {
        _uiState.update { it.copy(flyToStationId = stationId) }
    }

    /** Acknowledges a completed camera move so it does not repeat on recomposition. */
    fun clearFlyToStation() {
        _uiState.update { it.copy(flyToStationId = null) }
    }

    /** Re-subscribes after a load failure — the Retry button in the error state calls this. */
    fun retry() {
        loadStations()
    }
}
