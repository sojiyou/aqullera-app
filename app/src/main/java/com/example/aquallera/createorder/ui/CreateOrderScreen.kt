package com.example.aquallera.createorder.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aquallera.components.GallonSwapNote
import com.example.aquallera.components.LoadingIndicator
import com.example.aquallera.components.PrimaryButton
import com.example.aquallera.core.theme.AppBg
import com.example.aquallera.core.theme.InputBg
import com.example.aquallera.core.theme.MidnightBlue
import com.example.aquallera.core.util.TimeUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

/** Create Order form: quantities, address autocomplete, schedule, and the summary preview. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    viewModel: CreateOrderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    if (state.showPreview) {
        OrderPreviewScreen(
            state = state,
            onBackToEdit = { viewModel.setShowPreview(false) },
            onProceed = {
                if (viewModel.proceedToConfirmation()) onNext()
            },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Order", color = AppBg) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppBg)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBlue),
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingIndicator()
            state.error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.error ?: "", color = Color.Red, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        PrimaryButton(text = "Retry", onClick = { viewModel.retry() })
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .background(AppBg)
                        .verticalScroll(rememberScrollState()),
                ) {
                    StationCard(state.station?.stationName ?: "")

                    OrderTypeCard(
                        availableTypes = state.availableOrderTypes,
                        selectedType = state.orderType,
                        onTypeChanged = { viewModel.onOrderTypeChanged(it) },
                        showGallonSwapNote = state.orderType == "Delivery",
                    )

                    QuantitiesCard(state, viewModel)

                    if (state.orderType == "Delivery") {
                        DeliveryAddressCard(state, viewModel)
                    }

                    PreferredDateCard(state, viewModel)

                    if (state.orderType == "Delivery" && !state.station?.deliveryHours.isNullOrEmpty()) {
                        DeliveryTimeCard(state, viewModel)
                    }

                    if (state.orderType == "Pickup") {
                        PickupTimeCard(state, viewModel)
                    }

                    AdditionalDetailsCard(state, viewModel)

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(8.dp))

                    FeeBreakdown(state)

                    Spacer(modifier = Modifier.height(16.dp))
                    PrimaryButton(
                        text = "Preview Order",
                        onClick = { viewModel.setShowPreview(true) },
                        enabled = state.hasAnyItem,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        buttonWidth = 350,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun StationCard(name: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Station", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MidnightBlue)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = name, fontSize = 14.sp, color = Color.DarkGray)
        }
    }
}

@Composable
private fun OrderTypeCard(
    availableTypes: List<String>,
    selectedType: String,
    onTypeChanged: (String) -> Unit,
    showGallonSwapNote: Boolean = false,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Order Type", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MidnightBlue)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                availableTypes.forEach { type ->
                    val isSelected = type == selectedType
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MidnightBlue else InputBg)
                            .clickable { onTypeChanged(type) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = type,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) Color.White else Color.DarkGray,
                        )
                    }
                }
            }
            if (showGallonSwapNote) {
                Spacer(modifier = Modifier.height(8.dp))
                GallonSwapNote()
            }
        }
    }
}

@Composable
private fun QuantitiesCard(state: CreateOrderUiState, viewModel: CreateOrderViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Select Quantities", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MidnightBlue)
            Spacer(modifier = Modifier.height(8.dp))
            if (state.hasPureWater) {
                QuantityRow("Pure", state.pureWaterPrice, state.pureWaterQty) { viewModel.onPureWaterQtyChanged(it) }
            }
            if (state.hasSpringWater) {
                QuantityRow("Spring", state.springWaterPrice, state.springWaterQty) { viewModel.onSpringWaterQtyChanged(it) }
            }
            if (state.hasMineralWater) {
                QuantityRow("Mineral", state.mineralWaterPrice, state.mineralWaterQty) { viewModel.onMineralWaterQtyChanged(it) }
            }
        }
    }
}

@Composable
private fun QuantityRow(label: String, price: Double, qty: Int, onQtyChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MidnightBlue)
            Text(text = "₱${"%.2f".format(price)} / Gallon", fontSize = 12.sp, color = Color.Gray)
        }
        QuantitySelector(qty = qty, onQtyChange = onQtyChange)
    }
}

@Composable
private fun QuantitySelector(qty: Int, onQtyChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (qty > 0) MidnightBlue else MidnightBlue.copy(alpha = 0.3f))
                .clickable(enabled = qty > 0) { onQtyChange(qty - 1) },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "\u2212", color = AppBg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = "$qty",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MidnightBlue,
            modifier = Modifier.padding(horizontal = 12.dp).width(24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MidnightBlue)
                .clickable { onQtyChange(qty + 1) },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "+", color = AppBg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DeliveryAddressCard(state: CreateOrderUiState, viewModel: CreateOrderViewModel) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Delivery Address", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MidnightBlue)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = state.addressSearch,
                    onValueChange = { viewModel.onAddressSearchChanged(it) },
                    placeholder = { Text("Type your address or use GPS...", fontSize = 14.sp, color = Color.Gray) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = InputBg,
                        unfocusedContainerColor = InputBg,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MidnightBlue)
                        .clickable {
                            com.example.aquallera.maps.ui.getUserLocation(
                                context = context,
                                onResult = { lat, lng -> viewModel.onLocationResult(lat, lng) },
                                onError = { msg ->
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                },
                            )
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("GPS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            if (state.addressSearch.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\u2715",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { viewModel.clearAddress() }
                        .padding(4.dp),
                )
            }

            if (state.isSearchingAddress) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Searching...", fontSize = 12.sp, color = Color.Gray)
            }

            if (state.suggestions.isNotEmpty() && state.selectedLat == null) {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        state.suggestions.forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectSuggestion(suggestion) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Column {
                                    Text(
                                        text = suggestion.text,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MidnightBlue,
                                    )
                                    Text(
                                        text = suggestion.placeName,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferredDateCard(state: CreateOrderUiState, viewModel: CreateOrderViewModel) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Preferred Date", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MidnightBlue)
            Spacer(modifier = Modifier.height(8.dp))

            if (state.orderType == "Delivery" && state.deliveryDates.isNotEmpty()) {
                Text("Select a delivery date", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(6.dp))
                val dates = state.deliveryDates
                val selected = state.preferredDate
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    dates.forEach { dateStr ->
                        val display = formatDateDisplay(dateStr)
                        val isSelected = dateStr == selected
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MidnightBlue else InputBg)
                                .clickable { viewModel.onPreferredDateChanged(dateStr) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = display,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color.DarkGray,
                            )
                        }
                    }
                }
                if (dates.isEmpty()) {
                    Text("No available delivery dates at this time.", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                val displayDate = if (state.preferredDate.isNotBlank()) state.preferredDate else "Select date"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(InputBg)
                        .clickable {
                            val cal = Calendar.getInstance()
                            if (state.preferredDate.isNotBlank()) {
                                try {
                                    val parts = state.preferredDate.split("-")
                                    if (parts.size == 3) {
                                        cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                                    }
                                } catch (_: Exception) {}
                            }
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val selected = LocalDate.of(year, month + 1, day).format(DateTimeFormatter.ISO_LOCAL_DATE)
                                    viewModel.onPreferredDateChanged(selected)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH),
                            ).show()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = displayDate,
                        fontSize = 14.sp,
                        color = if (state.preferredDate.isNotBlank()) MidnightBlue else Color.Gray,
                    )
                    Text(
                        text = "\uD83D\uDCC5",
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeliveryTimeCard(state: CreateOrderUiState, viewModel: CreateOrderViewModel) {
    val hours = state.station?.deliveryHours ?: emptyList()

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Delivery Time", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MidnightBlue)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Select a delivery time slot", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                hours.forEach { time ->
                    val display = TimeUtils.to12Hour(time)
                    val isSelected = time == state.preferredTime
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MidnightBlue else InputBg)
                            .clickable { viewModel.onPreferredTimeChanged(time) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = display,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color.DarkGray,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PickupTimeCard(state: CreateOrderUiState, viewModel: CreateOrderViewModel) {
    val slots = state.pickupTimeSlots

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Pickup Time", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MidnightBlue)
            Spacer(modifier = Modifier.height(8.dp))
            if (slots.isEmpty()) {
                Text("No available pickup times", fontSize = 14.sp, color = Color.Gray)
            } else {
                val selectedValue = state.preferredTime
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    slots.forEach { (display, value) ->
                        val isSelected = value == selectedValue
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MidnightBlue else InputBg)
                                .clickable { viewModel.onPreferredTimeChanged(value) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = display,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color.DarkGray,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdditionalDetailsCard(state: CreateOrderUiState, viewModel: CreateOrderViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Additional Details", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MidnightBlue)
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = state.additionalDetails,
                onValueChange = { viewModel.onAdditionalDetailsChanged(it) },
                placeholder = { Text("e.g., Leave at gate, landmark, etc.", fontSize = 14.sp, color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = InputBg,
                    unfocusedContainerColor = InputBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
private fun FeeBreakdown(state: CreateOrderUiState) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        TotalRow("Subtotal", state.waterSubtotal)
        if (state.orderType == "Delivery") {
            TotalRow("Delivery Fee", state.deliveryFee)
        }
        TotalRow("Transaction Fee", state.transactionFee)
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Total", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MidnightBlue)
            Text(
                "₱${"%.2f".format(state.grandTotal)}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MidnightBlue,
            )
        }
    }
}

@Composable
private fun TotalRow(label: String, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, fontSize = 14.sp, color = Color.DarkGray)
        Text(text = "₱${"%.2f".format(amount)}", fontSize = 14.sp, color = Color.DarkGray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderPreviewScreen(
    state: CreateOrderUiState,
    onBackToEdit: () -> Unit,
    onProceed: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Preview", color = MidnightBlue) },
                navigationIcon = {
                    IconButton(onClick = onBackToEdit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MidnightBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBg),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(AppBg),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PreviewCard("Order Summary") {
                    Text("Station: ${state.station?.stationName ?: ""}", fontSize = 14.sp, color = Color.DarkGray)
                }

                val items = buildList {
                    if (state.pureWaterQty > 0) add("Pure water — ${state.pureWaterQty} Gallon(s) at ₱${"%.2f".format(state.pureWaterPrice)} each")
                    if (state.springWaterQty > 0) add("Spring water — ${state.springWaterQty} Gallon(s) at ₱${"%.2f".format(state.springWaterPrice)} each")
                    if (state.mineralWaterQty > 0) add("Mineral water — ${state.mineralWaterQty} Gallon(s) at ₱${"%.2f".format(state.mineralWaterPrice)} each")
                }
                if (items.isNotEmpty()) {
                    PreviewCard("Items") {
                        items.forEach { item ->
                            Text(item, fontSize = 13.sp, color = Color.DarkGray)
                        }
                    }
                }

                PreviewCard("Order Type") {
                    Text(state.orderType, fontSize = 14.sp, color = Color.DarkGray)
                }

                if (state.deliveryAddress.isNotBlank()) {
                    PreviewCard("Delivery Address") {
                        Text(state.deliveryAddress, fontSize = 14.sp, color = Color.DarkGray)
                    }
                }

                if (state.preferredDate.isNotBlank()) {
                    PreviewCard("Preferred Date") {
                        Text(state.preferredDate, fontSize = 14.sp, color = Color.DarkGray)
                    }
                }

                if (state.preferredTime.isNotBlank()) {
                    PreviewCard("Preferred Time") {
                        Text(TimeUtils.to12Hour(state.preferredTime), fontSize = 14.sp, color = Color.DarkGray)
                    }
                }

                if (state.additionalDetails.isNotBlank()) {
                    PreviewCard("Instructions") {
                        Text(state.additionalDetails, fontSize = 14.sp, color = Color.DarkGray)
                    }
                }

                PreviewCard("Payment Breakdown") {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        TotalRow("Subtotal", state.waterSubtotal)
                        if (state.orderType == "Delivery") {
                            TotalRow("Delivery Fee", state.deliveryFee)
                        }
                        TotalRow("Transaction Fee", state.transactionFee)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Grand Total", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MidnightBlue)
                            Text(
                                "₱${"%.2f".format(state.grandTotal)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MidnightBlue,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                androidx.compose.material3.OutlinedButton(
                    onClick = onBackToEdit,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MidnightBlue),
                ) {
                    Text("Edit Order", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                androidx.compose.material3.Button(
                    onClick = onProceed,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MidnightBlue, contentColor = AppBg),
                ) {
                    Text("Proceed", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun PreviewCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MidnightBlue)
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

private fun formatDateDisplay(dateStr: String): String {
    return try {
        val d = LocalDate.parse(dateStr)
        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        "${dayNames[d.dayOfWeek.value % 7]}, ${months[d.monthValue - 1]} ${d.dayOfMonth}"
    } catch (_: Exception) { dateStr }
}
