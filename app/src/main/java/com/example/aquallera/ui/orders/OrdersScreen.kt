package com.example.aquallera.ui.orders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.aquallera.core.model.Order
import com.example.aquallera.core.theme.AppBg
import com.example.aquallera.core.theme.MidnightBlue
import com.example.aquallera.core.theme.StatusCancelled
import com.example.aquallera.core.theme.StatusCompleted
import com.example.aquallera.core.theme.StatusConfirmed
import com.example.aquallera.core.theme.StatusOnDelivery
import com.example.aquallera.core.theme.StatusPending
import com.example.aquallera.core.theme.StatusPreparing
import com.example.aquallera.core.theme.StatusReady

@Composable
fun OrdersScreen(
    viewModel: OrdersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    when {
        state.loading -> LoadingIndicator()
        state.error != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.error ?: "", color = Color.Red, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    PrimaryButton(text = "Retry", onClick = { viewModel.retry() })
                }
            }
        }
        state.orders.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "My Orders",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MidnightBlue,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No orders yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MidnightBlue.copy(alpha = 0.5f),
                    )
                }
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBg)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        text = "My Orders",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MidnightBlue,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                items(state.orders, key = { it.orderId }) { order ->
                    OrderCard(
                        order = order,
                        expanded = state.expandedOrderId == order.orderId,
                        onToggle = { viewModel.toggleExpand(order.orderId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: Order,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
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
                    text = order.stationName.ifBlank { "Station" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MidnightBlue,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(status = order.status)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = order.date.ifBlank { order.createdAt.let { "" } },
                    fontSize = 13.sp,
                    color = Color.Gray,
                )
                if (order.time.isNotBlank()) {
                    Text(text = order.time, fontSize = 13.sp, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val itemCount = listOf(
                    order.pureWaterQty,
                    order.springWaterQty,
                    order.mineralWaterQty,
                ).count { it > 0 }
                Text(
                    text = "$itemCount item(s)",
                    fontSize = 13.sp,
                    color = Color.Gray,
                )
                Text(
                    text = "₱${"%.2f".format(order.grandTotal)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MidnightBlue,
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    if (order.referenceNumber.isNotBlank()) {
                        DetailRow("Reference", order.referenceNumber)
                    }
                    if (order.pureWaterQty > 0) {
                        DetailRow("Pure Water", "${order.pureWaterQty} × ₱${"%.2f".format(order.pureWaterPrice)}")
                    }
                    if (order.springWaterQty > 0) {
                        DetailRow("Spring Water", "${order.springWaterQty} × ₱${"%.2f".format(order.springWaterPrice)}")
                    }
                    if (order.mineralWaterQty > 0) {
                        DetailRow("Mineral Water", "${order.mineralWaterQty} × ₱${"%.2f".format(order.mineralWaterPrice)}")
                    }
                    if (order.deliveryAddress.isNotBlank()) {
                        DetailRow("Deliver to", order.deliveryAddress)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "Pending" -> StatusPending to Color.Black
        "Confirmed" -> StatusConfirmed to Color.White
        "Preparing" -> StatusPreparing to Color.White
        "On Delivery" -> StatusOnDelivery to Color.White
        "Ready" -> StatusReady to Color.White
        "Completed" -> StatusCompleted to Color.White
        "Cancelled" -> StatusCancelled to Color.White
        else -> Color.Gray to Color.White
    }
    Text(
        text = status,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = textColor,
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.weight(0.35f))
        Text(
            text = value,
            fontSize = 13.sp,
            color = MidnightBlue,
            modifier = Modifier.weight(0.65f),
            maxLines = 2,
        )
    }
}
