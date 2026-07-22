package com.example.aquallera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aquallera.core.model.Order
import com.example.aquallera.core.theme.MidnightBlue
import com.example.aquallera.core.theme.OrderListBg
import com.example.aquallera.core.theme.StatusCancelled
import com.example.aquallera.core.theme.StatusCompleted
import com.example.aquallera.core.theme.StatusConfirmed
import com.example.aquallera.core.theme.StatusOnDelivery
import com.example.aquallera.core.theme.StatusPending
import com.example.aquallera.core.theme.StatusPreparing
import com.example.aquallera.core.theme.StatusReady
import com.example.aquallera.core.util.TimeUtils

@Composable
fun OrderTicketItem(
    order: Order,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = OrderListBg),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = order.stationName.ifBlank { "Order" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MidnightBlue,
                )
                StatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (order.referenceNumber.isNotBlank()) {
                Text(
                    text = "Ref: ${order.referenceNumber}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = TimeUtils.formatDateTime(order.createdAt),
                fontSize = 12.sp,
                color = Color.Gray,
            )

            if (order.grandTotal > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Total: ₱${"%.2f".format(order.grandTotal)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MidnightBlue,
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val color = when (status) {
        "Pending" -> StatusPending
        "Confirmed" -> StatusConfirmed
        "Preparing" -> StatusPreparing
        "On Delivery" -> StatusOnDelivery
        "Ready" -> StatusReady
        "Completed" -> StatusCompleted
        "Cancelled" -> StatusCancelled
        else -> Color.Gray
    }
    Text(
        text = status,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )
}
