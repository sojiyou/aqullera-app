package com.example.aquallera.store.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aquallera.components.LoadingIndicator
import com.example.aquallera.components.PrimaryButton
import com.example.aquallera.core.model.WaterStation
import com.example.aquallera.core.theme.AppBg
import com.example.aquallera.core.theme.MidnightBlue
import com.example.aquallera.core.theme.OrderListBg
import com.example.aquallera.core.theme.StatusCompleted
import com.example.aquallera.core.theme.StatusOnDelivery
import com.example.aquallera.core.util.TimeUtils

/** Station details: info, hours, prices and the order/pickup entry point. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreDetailsScreen(
    onBack: () -> Unit,
    onOrderNow: (stationId: String) -> Unit,
    viewModel: StoreViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Station Details", color = AppBg) },
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
            uiState.loading -> LoadingIndicator()
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = uiState.error ?: "", color = Color.Red, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        PrimaryButton(text = "Retry", onClick = { viewModel.retry() })
                    }
                }
            }
            uiState.station != null -> {
                StoreDetailsContent(
                    station = uiState.station!!,
                    onBack = onBack,
                    onOrderNow = { onOrderNow(uiState.station!!.id) },
                    modifier = Modifier.padding(padding),
                )
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(text = "Station not found", color = Color.Gray, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun StoreDetailsContent(
    station: WaterStation,
    onBack: () -> Unit,
    onOrderNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBg)
            .verticalScroll(rememberScrollState()),
    ) {
        HeaderSection(station)
        val documentKeys = station.verifiedDocumentKeys
        if (documentKeys.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            VerifiedDocumentsCard(documentKeys)
        }
        Spacer(modifier = Modifier.height(8.dp))
        BusinessHoursCard(station.businessHours)
        Spacer(modifier = Modifier.height(8.dp))
        ServicesCard(station.serviceTypes)
        if (station.serviceTypes.contains("delivery") &&
            (!station.deliveryDays.isNullOrEmpty() || !station.deliveryHours.isNullOrEmpty())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            DeliveryScheduleCard(station.deliveryDays, station.deliveryHours)
        }
        Spacer(modifier = Modifier.height(8.dp))
        PricingCard(station)
        if (station.about.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            AboutCard(station.about)
        }
        Spacer(modifier = Modifier.height(8.dp))
        ContactCard(station)
        Spacer(modifier = Modifier.height(16.dp))
        ActionButtons(
            isApproved = station.status == "approved",
            onBack = onBack,
            onOrderNow = onOrderNow,
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun HeaderSection(station: WaterStation) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MidnightBlue)
            .padding(16.dp),
    ) {
        Text(
            text = station.stationName,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AppBg,
        )
        if (station.address.isNotBlank()) {
            Text(
                text = station.address,
                fontSize = 14.sp,
                color = AppBg.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val isOnline = station.isOnline
            StatusBadge(
                text = if (isOnline) "Online" else "Offline",
                color = if (isOnline) StatusCompleted else StatusOnDelivery,
            )
            val isApproved = station.status == "approved"
            StatusBadge(
                text = if (isApproved) "Approved" else "Pending",
                color = if (isApproved) StatusCompleted else StatusOnDelivery,
            )
        }
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White,
        modifier = Modifier
            .background(color, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
private fun BusinessHoursCard(hours: Map<String, String>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Business Hours", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MidnightBlue)
            Spacer(modifier = Modifier.height(8.dp))

            if (hours.containsKey("open") && hours.containsKey("close")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Business Hours",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MidnightBlue,
                    )
                    Text(
                        text = "${TimeUtils.to12Hour(hours["open"] ?: "")} - ${TimeUtils.to12Hour(hours["close"] ?: "")}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MidnightBlue,
                    )
                }
            } else {
                val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                val todayName = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")[
                    java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) - 1
                ]

                days.forEach { day ->
                    val time = hours[day] ?: "Closed"
                    val isToday = day == todayName
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = day.take(3),
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp,
                            color = if (isToday) MidnightBlue else Color.DarkGray,
                        )
                        Text(
                            text = time,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp,
                            color = if (isToday) MidnightBlue else Color.DarkGray,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServicesCard(serviceTypes: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Offered Services", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MidnightBlue)
            Spacer(modifier = Modifier.height(4.dp))
            if (serviceTypes.isEmpty()) {
                Text("No services listed", fontSize = 14.sp, color = Color.Gray)
            } else {
                val subtitle = when {
                    serviceTypes.contains("delivery") && serviceTypes.contains("pickup") -> "Pickup & Delivery"
                    serviceTypes.contains("delivery") -> "Delivery Only"
                    serviceTypes.contains("pickup") -> "Pickup Only"
                    else -> ""
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    serviceTypes.forEach { service ->
                        Text(
                            text = service.replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MidnightBlue,
                            modifier = Modifier
                                .background(OrderListBg, RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VerifiedDocumentsCard(documentKeys: List<String>) {
    val labels = mapOf(
        "businessPermit" to "BP",
        "dtiSecRegistration" to "DTI",
        "fdaLto" to "FDA",
        "sanitaryPermit" to "SP",
    )
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Verified Documents", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MidnightBlue)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                documentKeys.forEach { key ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = labels[key] ?: key,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2E7D32),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeliveryScheduleCard(deliveryDays: List<String>?, deliveryHours: List<String>?) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Delivery Schedule", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MidnightBlue)
            Spacer(modifier = Modifier.height(8.dp))

            if (!deliveryDays.isNullOrEmpty()) {
                Text("Days", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                ChipRow(deliveryDays.map { it.replaceFirstChar { c -> c.uppercase() } })
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!deliveryHours.isNullOrEmpty()) {
                Text("Hours", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                ChipRow(deliveryHours.map { TimeUtils.to12Hour(it) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(labels: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MidnightBlue,
                modifier = Modifier
                    .background(OrderListBg, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun PricingCard(station: WaterStation) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Price List", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MidnightBlue)
            Spacer(modifier = Modifier.height(10.dp))

            val cells = listOfNotNull(
                if (station.offers("pure")) "Pure (Gallon)" to station.pricing_gallon_pure else null,
                if (station.offers("spring")) "Spring (Gallon)" to station.springPrice else null,
                if (station.offers("mineral")) "Mineral (Gallon)" to station.pricing_gallon_mineral else null,
            )

            if (cells.any { it.second != null }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    cells.forEach { (label, price) ->
                        PricingCell(
                            label = label,
                            price = price,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                Text("No pricing available", fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun PricingCell(
    label: String,
    price: Double?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(OrderListBg, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "₱${"%.2f".format(price ?: 0.0)}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MidnightBlue,
        )
    }
}

@Composable
private fun AboutCard(about: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("About", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MidnightBlue)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = about, fontSize = 14.sp, color = Color.DarkGray)
        }
    }
}

@Composable
private fun ContactCard(station: WaterStation) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Contact", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MidnightBlue)
            Spacer(modifier = Modifier.height(8.dp))
            if (station.phone.isNotBlank()) {
                Text("Phone: ${station.phone}", fontSize = 14.sp, color = Color.DarkGray)
            }
            if (station.email.isNotBlank()) {
                Text("Email: ${station.email}", fontSize = 14.sp, color = Color.DarkGray)
            }
        }
    }
}

@Composable
private fun ActionButtons(
    isApproved: Boolean,
    onBack: () -> Unit,
    onOrderNow: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MidnightBlue),
        ) {
            Text(
                text = "Return",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Button(
            onClick = onOrderNow,
            enabled = isApproved,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MidnightBlue,
                contentColor = AppBg,
                disabledContainerColor = MidnightBlue.copy(alpha = 0.5f),
                disabledContentColor = AppBg.copy(alpha = 0.7f),
            ),
        ) {
            Text(
                text = "Order Now",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
