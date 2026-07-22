package com.example.aquallera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aquallera.core.model.WaterStation
import com.example.aquallera.core.theme.MidnightBlue
import com.example.aquallera.core.theme.OrderListBg
import com.example.aquallera.core.theme.StatusCancelled
import com.example.aquallera.core.theme.StatusCompleted
import com.example.aquallera.core.theme.StatusOnDelivery

@Composable
fun WaterStationCard(
    station: WaterStation,
    userLat: Double? = null,
    userLng: Double? = null,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val distanceText = remember(station, userLat, userLng) {
        if (userLat != null && userLng != null) {
            station.calculateDistanceTo(userLat, userLng).formatted
        } else ""
    }

    val purePrice = station.pricing_gallon_pure
    val springPrice = station.pricing_liter_spring ?: station.pricing_gallon_spring
    val mineralPrice = station.pricing_gallon_mineral
    val hasPrices = purePrice != null || springPrice != null || mineralPrice != null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = station.stationName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MidnightBlue,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(8.dp))
                OnlineOfflineBadge(isOnline = station.isOnline, status = station.status)
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (station.address.isNotBlank()) {
                Text(
                    text = station.address,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 2,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (distanceText.isNotBlank()) {
                Text(
                    text = "${distanceText} away",
                    fontSize = 12.sp,
                    color = Color.Gray,
                )
            }

            if (hasPrices) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(OrderListBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        purePrice?.let {
                            PriceBadge(label = "Pure", price = it)
                        }
                        springPrice?.let {
                            PriceBadge(label = "Spring", price = it)
                        }
                        mineralPrice?.let {
                            PriceBadge(label = "Mineral", price = it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceBadge(label: String, price: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray,
        )
        Text(
            text = "₱${"%.2f".format(price)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MidnightBlue,
        )
    }
}

@Composable
private fun OnlineOfflineBadge(isOnline: Boolean, status: String) {
    val isApproved = status == "approved"
    val (dotColor, text) = when {
        !isApproved -> StatusCancelled to "Pending"
        isOnline -> StatusCompleted to "Online"
        else -> StatusOnDelivery to "Offline"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = dotColor,
        )
    }
}
