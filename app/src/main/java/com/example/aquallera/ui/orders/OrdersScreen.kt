package com.example.aquallera.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aquallera.components.GallonSwapNote
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

private val filterTabs = listOf(
    "all" to "All",
    "pending" to "Pending",
    "confirmed" to "Confirmed",
    "preparing" to "Preparing",
    "on_delivery" to "On Delivery",
    "ready" to "Ready",
    "completed" to "Completed",
    "cancelled" to "Cancelled",
)

/** Order history with status tabs and a receipt view for a selected order. */
@Composable
fun OrdersScreen(
    viewModel: OrdersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    if (state.selectedOrderId != null) {
        val order = state.orders.find { it.orderId == state.selectedOrderId }
        if (order != null) {
            OrderReceiptScreen(
                order = order,
                onBack = { viewModel.selectOrder(null) },
                onCancel = { viewModel.cancelOrder(order.orderId) },
            )
            return
        }
    }

    val orders = viewModel.filteredOrders

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
                item {
                    FilterChipsRow(
                        selected = state.filterStatus,
                        onSelect = { viewModel.setFilterStatus(it) },
                    )
                }
                if (orders.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No ${filterTabs.find { it.first == state.filterStatus }?.second ?: ""} orders",
                                color = MidnightBlue.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                            )
                        }
                    }
                } else {
                    items(orders, key = { it.orderId }) { order ->
                        OrderTicketCard(
                            order = order,
                            onClick = { viewModel.selectOrder(order.orderId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        filterTabs.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) MidnightBlue else Color(0xFFECEFF1))
                    .clickable { onSelect(value) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.White else Color.DarkGray,
                )
            }
        }
    }
}

@Composable
private fun OrderTicketCard(
    order: Order,
    onClick: () -> Unit,
) {
    val qtyText = listOfNotNull(
        order.pureWaterQty.takeIf { it > 0 },
        order.springWaterQty.takeIf { it > 0 },
        order.mineralWaterQty.takeIf { it > 0 },
    ).joinToString(" + ") { it.toString() }.ifBlank { "N/A" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFECEFF1))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(
            text = order.stationName.ifBlank { "Unknown Station" },
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MidnightBlue,
        )
        Spacer(modifier = Modifier.height(4.dp))
        StatusBadge(status = order.status)
        Spacer(modifier = Modifier.height(2.dp))
        HorizontalDivider(color = MidnightBlue.copy(alpha = 0.3f), thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Date: ${order.date.ifBlank { order.createdAt.take(10) }}",
            fontSize = 12.sp,
            color = MidnightBlue,
        )
        Text(
            text = "Ref #: ${order.referenceNumber.ifBlank { "N/A" }}",
            fontSize = 12.sp,
            color = MidnightBlue,
        )
        Text(
            text = "Items: $qtyText",
            fontSize = 12.sp,
            color = MidnightBlue,
        )
        Text(
            text = "₱${"%.2f".format(order.grandTotal)}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MidnightBlue,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun OrderReceiptScreen(
    order: Order,
    onBack: () -> Unit,
    onCancel: () -> Unit = {},
) {
    var showCancelConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MidnightBlue)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "\u2190",
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(end = 8.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Order Details",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OrderReceiptCard(order = order)
            val canCancel = order.status == "Pending" || order.status == "Confirmed"
            if (canCancel) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEF5350))
                        .clickable { showCancelConfirm = true }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Cancel Order",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }

    if (showCancelConfirm) {
        CancelOrderDialog(
            onDismiss = { showCancelConfirm = false },
            onConfirm = { showCancelConfirm = false; onCancel() },
        )
    }
}

@Composable
private fun CancelOrderDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cancel Order",
                color = Color(0xFFB71C1C),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = "Are you sure you want to cancel this order? This action cannot be undone.",
                fontSize = 14.sp,
                color = Color.Gray,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Cancel Order", color = Color(0xFFEF5350), fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Order", color = MidnightBlue, fontWeight = FontWeight.Medium)
            }
        },
        containerColor = Color.White,
    )
}

@Composable
private fun OrderReceiptCard(order: Order) {
    val lineItems = listOfNotNull(
        if (order.pureWaterQty > 0) Triple("Pure Water", order.pureWaterQty, order.pureWaterTotal) else null,
        if (order.springWaterQty > 0) Triple("Spring Water", order.springWaterQty, order.springWaterTotal) else null,
        if (order.mineralWaterQty > 0) Triple("Mineral Water", order.mineralWaterQty, order.mineralWaterTotal) else null,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "AQUA-LLERA",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MidnightBlue,
            )
            Text(
                text = "Order Receipt",
                fontSize = 12.sp,
                color = Color.Gray,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = order.stationName.ifBlank { "Unknown Station" },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MidnightBlue,
            )
            if (order.referenceNumber.isNotBlank()) {
                Text(
                    text = "Ref: ${order.referenceNumber}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            StatusBadge(status = order.status)
        }

        if (order.orderType.equals("Delivery", ignoreCase = true)) {
            GallonSwapNote()
            Spacer(modifier = Modifier.height(16.dp))
        }

        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Item", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray, modifier = Modifier.weight(2f))
            Text("Qty", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
            Text("Amount", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
        }
        Spacer(modifier = Modifier.height(8.dp))

        lineItems.forEach { (name, qty, total) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(name, fontSize = 14.sp, color = Color.DarkGray, modifier = Modifier.weight(2f))
                Text(qty.toString(), fontSize = 14.sp, color = Color.Gray, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
                Text("₱${"%.2f".format(total)}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Subtotal", fontSize = 14.sp, color = Color.Gray)
            Text("₱${"%.2f".format(order.waterSubtotal)}", fontSize = 14.sp, color = Color.DarkGray)
        }
        if (order.deliveryFee > 0) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Delivery Fee", fontSize = 14.sp, color = Color.Gray)
                Text("₱${"%.2f".format(order.deliveryFee)}", fontSize = 14.sp, color = Color.DarkGray)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Transaction Fee", fontSize = 14.sp, color = Color.Gray)
            Text("₱${"%.2f".format(order.transactionFee)}", fontSize = 14.sp, color = Color.DarkGray)
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MidnightBlue.copy(alpha = 0.3f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MidnightBlue)
            Text("₱${"%.2f".format(order.grandTotal)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MidnightBlue)
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))

        if (order.customerName.isNotBlank()) {
            DetailReceiptRow("Customer", order.customerName)
        }
        if (order.customerPhone.isNotBlank()) {
            DetailReceiptRow("Contact", order.customerPhone)
        }
        if (order.deliveryAddress.isNotBlank()) {
            DetailReceiptRow("Address", order.deliveryAddress)
        }
        if (order.paymentMethod.isNotBlank()) {
            DetailReceiptRow("Payment", order.paymentMethod)
        }
        if (order.date.isNotBlank()) {
            DetailReceiptRow("Date", order.date)
        }
        if (order.time.isNotBlank()) {
            DetailReceiptRow("Time", order.time)
        }
        if (order.additionalDetails.isNotBlank()) {
            DetailReceiptRow("Notes", order.additionalDetails)
        }
    }
}

@Composable
private fun DetailReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(
            text = value,
            fontSize = 12.sp,
            color = MidnightBlue,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(max = 200.dp),
        )
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
