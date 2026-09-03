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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class MapboxSuggestion(
    val text: String,
    val placeName: String,
    val latitude: Double,
    val longitude: Double,
)

data class CreateOrderUiState(
    val station: WaterStation? = null,
    val pureWaterQty: Int = 0,
    val springWaterQty: Int = 0,
    val mineralWaterQty: Int = 0,
    val deliveryAddress: String = "",
    val additionalDetails: String = "",
    val loading: Boolean = true,
    val error: String? = null,
    val orderType: String = "Delivery",
    val preferredDate: String = "",
    val preferredTime: String = "",
    val addressSearch: String = "",
    val suggestions: List<MapboxSuggestion> = emptyList(),
    val isSearchingAddress: Boolean = false,
    val selectedLat: Double? = null,
    val selectedLng: Double? = null,
    val showPreview: Boolean = false,
) {
    val hasPureWater: Boolean get() = station?.pricing_gallon_pure != null && station!!.pricing_gallon_pure > 0 && station!!.offers("pure")
    val hasSpringWater: Boolean get() = (station?.springPrice ?: 0.0) > 0 && station!!.offers("spring")
    val hasMineralWater: Boolean get() = station?.pricing_gallon_mineral != null && station!!.pricing_gallon_mineral > 0 && station!!.offers("mineral")

    val pureWaterPrice: Double get() = station?.pricing_gallon_pure ?: 0.0
    val springWaterPrice: Double get() = station?.springPrice ?: 0.0
    val mineralWaterPrice: Double get() = station?.pricing_gallon_mineral ?: 0.0

    val pureWaterTotal: Double get() = pureWaterQty * pureWaterPrice
    val springWaterTotal: Double get() = springWaterQty * springWaterPrice
    val mineralWaterTotal: Double get() = mineralWaterQty * mineralWaterPrice
    val waterSubtotal: Double get() = pureWaterTotal + springWaterTotal + mineralWaterTotal
    val deliveryFee: Double get() = if (orderType == "Delivery") (station?.pricing_delivery_fee ?: Constants.DEFAULT_DELIVERY_FEE) else 0.0
    val transactionFee: Double get() = (waterSubtotal + deliveryFee) * Constants.TRANSACTION_FEE_RATE
    val grandTotal: Double get() = waterSubtotal + deliveryFee + transactionFee

    val hasAnyItem: Boolean get() = pureWaterQty > 0 || springWaterQty > 0 || mineralWaterQty > 0

    val availableOrderTypes: List<String>
        get() {
            val types = station?.serviceTypes ?: emptyList()
            if (types.isEmpty()) return listOf("Delivery", "Pickup")
            return types.map { it.replaceFirstChar { c -> c.uppercase() } }
        }

    val deliveryDates: List<String>
        get() {
            val days = station?.deliveryDays ?: return emptyList()
            if (days.isEmpty()) return emptyList()
            val dayMap = listOf("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday")
            val results = mutableListOf<String>()
            var date = LocalDate.now()
            for (i in 0 until 30) {
                if (results.size >= 14) break
                val dayName = dayMap[date.dayOfWeek.value % 7]
                if (days.any { it.equals(dayName, ignoreCase = true) }) {
                    results.add(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                }
                date = date.plusDays(1)
            }
            return results
        }

    val pickupTimeSlots: List<Pair<String, String>>
        get() {
            val bh = station?.businessHours ?: return emptyList()
            val dateStr = if (preferredDate.isNotBlank()) preferredDate else LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
            val dayNames = listOf("SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")
            val dayOfWeek = try {
                LocalDate.parse(dateStr).dayOfWeek.value % 7
            } catch (_: Exception) { return emptyList() }
            val dayKey = days[dayOfWeek]
            val hoursStr = bh[dayKey] ?: return emptyList()
            val parts = hoursStr.split(" - ")
            if (parts.size != 2) return emptyList()
            val parseTime = fun(s: String): Pair<Int, Int>? {
                val regex = Regex("""(\d{1,2}):(\d{2})\s*(AM|PM)""", RegexOption.IGNORE_CASE)
                val m = regex.find(s.trim()) ?: return null
                var h = m.groupValues[1].toIntOrNull() ?: return null
                val min = m.groupValues[2].toIntOrNull() ?: return null
                val ampm = m.groupValues[3].uppercase()
                if (ampm == "PM" && h != 12) h += 12
                if (ampm == "AM" && h == 12) h = 0
                return h to min
            }
            val open = parseTime(parts[0]) ?: return emptyList()
            val close = parseTime(parts[1]) ?: return emptyList()
            val slots = mutableListOf<Pair<String, String>>()
            var h = open.first
            var m = open.second
            while (h < close.first || (h == close.first && m < close.second)) {
                val value = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
                val hour12 = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
                val period = if (h < 12) "AM" else "PM"
                val display = "$hour12:${m.toString().padStart(2, '0')} $period"
                slots.add(display to value)
                m += 30
                if (m >= 60) { h++; m = 0 }
            }
            return slots
        }
}

/**
 * Backs the Create Order form: water quantities, Mapbox address autocomplete, delivery
 * scheduling and the live price totals.
 */
@HiltViewModel
class CreateOrderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val stationRepository: StationRepository,
    private val pendingOrderStore: PendingOrderStore,
) : ViewModel() {

    private val stationId: String = savedStateHandle.get<String>("stationId") ?: ""

    private val _uiState = MutableStateFlow(CreateOrderUiState())
    val uiState: StateFlow<CreateOrderUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadStation()
    }

    /** One-shot fetch of the station, which also picks the default order type and date. */
    private fun loadStation() {
        if (stationId.isBlank()) {
            _uiState.update { it.copy(error = "Invalid station", loading = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            stationRepository.getStationById(stationId).fold(
                onSuccess = { station ->
                    val types = station?.serviceTypes ?: emptyList()
                    val defaultType = when {
                        types.contains("delivery") && !types.contains("pickup") -> "Delivery"
                        types.contains("pickup") && !types.contains("delivery") -> "Pickup"
                        else -> "Delivery"
                    }
                    val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    _uiState.update {
                        it.copy(
                            station = station,
                            loading = false,
                            orderType = defaultType,
                            preferredDate = today,
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(loading = false, error = e.message ?: "Failed to load station") }
                },
            )
        }
    }

    fun onPureWaterQtyChanged(qty: Int) { _uiState.update { it.copy(pureWaterQty = maxOf(0, qty)) } }
    fun onSpringWaterQtyChanged(qty: Int) { _uiState.update { it.copy(springWaterQty = maxOf(0, qty)) } }
    fun onMineralWaterQtyChanged(qty: Int) { _uiState.update { it.copy(mineralWaterQty = maxOf(0, qty)) } }
    fun onAdditionalDetailsChanged(v: String) { _uiState.update { it.copy(additionalDetails = v) } }

    /** Switches Delivery/Pickup, which also toggles the delivery fee in the totals. */
    fun onOrderTypeChanged(type: String) {
        _uiState.update { it.copy(orderType = type) }
    }

    fun onPreferredDateChanged(date: String) {
        _uiState.update { it.copy(preferredDate = date) }
    }

    fun onPreferredTimeChanged(time: String) {
        _uiState.update { it.copy(preferredTime = time) }
    }

    /** Fires a debounced Mapbox autocomplete search on every keystroke. */
    fun onAddressSearchChanged(query: String) {
        _uiState.update { it.copy(addressSearch = query, deliveryAddress = query, selectedLat = null, selectedLng = null) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400)
            if (query.isNotBlank()) searchAddress(query)
        }
    }

    /** Picks an autocomplete result as the delivery address. */
    fun selectSuggestion(suggestion: MapboxSuggestion) {
        _uiState.update {
            it.copy(
                addressSearch = suggestion.placeName,
                deliveryAddress = suggestion.placeName,
                selectedLat = suggestion.latitude,
                selectedLng = suggestion.longitude,
                suggestions = emptyList(),
            )
        }
    }

    /** Clears the address fields and any pending autocomplete results. */
    fun clearAddress() {
        _uiState.update {
            it.copy(addressSearch = "", deliveryAddress = "", selectedLat = null, selectedLng = null, suggestions = emptyList())
        }
    }

    /** Reverse-geocodes a device-location pick into an address. */
    fun onLocationResult(lat: Double, lng: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingAddress = true) }
            val address = reverseGeocode(lat, lng)
            _uiState.update {
                it.copy(
                    addressSearch = address,
                    deliveryAddress = address,
                    selectedLat = lat,
                    selectedLng = lng,
                    suggestions = emptyList(),
                    isSearchingAddress = false,
                )
            }
        }
    }

    /** Toggles the order summary preview. */
    fun setShowPreview(show: Boolean) {
        _uiState.update { it.copy(showPreview = show) }
    }

    /** Reloads the station after a failure — the Retry button calls this. */
    fun retry() { loadStation() }

    /** Validates a non-empty order and hands it to the pending store; false when empty. */
    fun proceedToConfirmation(): Boolean {
        val s = _uiState.value
        if (!s.hasAnyItem) return false

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
                additionalDetails = s.additionalDetails,
                orderType = s.orderType,
                preferredDate = s.preferredDate,
                preferredTime = s.preferredTime,
                deliveryLatitude = s.selectedLat ?: 0.0,
                deliveryLongitude = s.selectedLng ?: 0.0,
            )
        )
        return true
    }

    /** Queries Mapbox Places and surfaces up to 8 suggestions in the UI state. */
    private suspend fun searchAddress(query: String) {
        withContext(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isSearchingAddress = true) }
                val url = URL("https://api.mapbox.com/geocoding/v5/mapbox.places/${java.net.URLEncoder.encode(query, "UTF-8")}.json?access_token=${Constants.MAPBOX_TOKEN}&country=PH&types=address,place,poi,locality,neighborhood,district&language=en&limit=8&autocomplete=true&fuzzyMatch=true")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                val json = JSONObject(response)
                val features = json.optJSONArray("features") ?: JSONArray()
                val suggestions = mutableListOf<MapboxSuggestion>()
                for (i in 0 until features.length()) {
                    val feature = features.getJSONObject(i)
                    val text = feature.optString("text", "")
                    val placeName = feature.optString("place_name", "")
                    val center = feature.optJSONArray("center")
                    val lng = center?.optDouble(0, 0.0) ?: 0.0
                    val lat = center?.optDouble(1, 0.0) ?: 0.0
                    suggestions.add(MapboxSuggestion(text = text, placeName = placeName, latitude = lat, longitude = lng))
                }
                _uiState.update { it.copy(suggestions = suggestions, isSearchingAddress = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(suggestions = emptyList(), isSearchingAddress = false) }
            }
        }
    }

    /** Resolves a coordinate to a place name, falling back to its raw lat/lng. */
    private suspend fun reverseGeocode(lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.mapbox.com/geocoding/v5/mapbox.places/$lng,$lat.json?access_token=${Constants.MAPBOX_TOKEN}&limit=1")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val response = reader.readText()
            reader.close()
            val json = JSONObject(response)
            val features = json.optJSONArray("features")
            if (features != null && features.length() > 0) {
                features.getJSONObject(0).optString("place_name", "${"%.6f".format(lat)}, ${"%.6f".format(lng)}")
            } else {
                "${"%.6f".format(lat)}, ${"%.6f".format(lng)}"
            }
        } catch (_: Exception) {
            "${"%.6f".format(lat)}, ${"%.6f".format(lng)}"
        }
    }
}
