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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.MaterialTheme
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
import com.example.aquallera.components.WaterStationCard
import com.example.aquallera.core.model.WaterStation
import com.example.aquallera.core.theme.AppBg
import com.example.aquallera.core.theme.MidnightBlue
import com.example.aquallera.core.theme.StatusCompleted
import com.example.aquallera.core.theme.StatusOnDelivery
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

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
                    onOrderNow = { onOrderNow(uiState.station!!.id) },
                    modifier = Modifier.padding(padding),
                )
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(text = "Station not found", color = androidx.compose.ui.graphics.Color.Gray, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun StoreDetailsContent(
    station: WaterStation,
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
        Spacer(modifier = Modifier.height(8.dp))
        BusinessHoursCard(station.businessHours)
        Spacer(modifier = Modifier.height(8.dp))
        ServicesCard(station.serviceTypes)
        if (!station.deliveryDays.isNullOrEmpty() || !station.deliveryHours.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            DeliveryInfoCard(station)
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
        PrimaryButton(
            text = "Order Now",
            onClick = onOrderNow,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            buttonWidth = 350,
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun HeaderSection(station: WaterStation) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = station.stationName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MidnightBlue,
        )
        if (station.address.isNotBlank()) {
            Text(
                text = station.address.ifBlank { station.city },
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        val (statusText, statusColor) = if (station.isOnline) "Open" to StatusCompleted else "Closed" to StatusOnDelivery
        Text(
            text = statusText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = statusColor,
        )
    }
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

            val todayName = LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)
            val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

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
            Spacer(modifier = Modifier.height(8.dp))
            if (serviceTypes.isEmpty()) {
                Text("No services listed", fontSize = 14.sp, color = Color.Gray)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    serviceTypes.forEach { service ->
                        Text(
                            text = service.replaceFirstChar { it.uppercase() },
                            fontSize = 13.sp,
                            color = Color.White,
                            modifier = Modifier
                                .background(MidnightBlue, RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryInfoCard(station: WaterStation) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Delivery Info", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MidnightBlue)
            Spacer(modifier = Modifier.height(8.dp))
            if (!station.deliveryDays.isNullOrEmpty()) {
                Text("Delivery Days: ${station.deliveryDays.joinToString(", ")}", fontSize = 14.sp, color = Color.DarkGray)
            }
            if (!station.deliveryHours.isNullOrEmpty()) {
                Text("Delivery Hours: ${station.deliveryHours.joinToString(", ")}", fontSize = 14.sp, color = Color.DarkGray)
            }
            Text("Delivery Radius: ${station.deliveryRadius} km", fontSize = 14.sp, color = Color.DarkGray)
            if (station.pricing_delivery_fee != null && station.pricing_delivery_fee > 0) {
                Text("Delivery Fee: ₱${"%.2f".format(station.pricing_delivery_fee)}", fontSize = 14.sp, color = Color.DarkGray)
            }
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
            Text("Pricing", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MidnightBlue)
            Spacer(modifier = Modifier.height(8.dp))

            if (station.pricing_gallon_pure != null) {
                PricingRow("Pure Water (Gallon)", station.pricing_gallon_pure)
            }
            if (station.pricing_gallon_mineral != null) {
                PricingRow("Mineral Water (Gallon)", station.pricing_gallon_mineral)
            }
            if (station.pricing_gallon_spring != null) {
                PricingRow("Spring Water (Gallon)", station.pricing_gallon_spring)
            }
            if (station.pricing_liter_spring != null) {
                PricingRow("Spring Water (Liter)", station.pricing_liter_spring)
            }
            if (station.pricing_gallon_pure == null && station.pricing_gallon_mineral == null
                && station.pricing_gallon_spring == null && station.pricing_liter_spring == null
            ) {
                Text("No pricing available", fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun PricingRow(label: String, price: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, fontSize = 14.sp, color = Color.DarkGray)
        Text(
            text = "₱${"%.2f".format(price)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
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
