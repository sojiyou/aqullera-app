package com.example.aquallera.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aquallera.core.model.Order
import com.example.aquallera.core.theme.MidnightBlue

@Composable
fun ReceiptCard(
    order: Order,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Order Summary",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MidnightBlue,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            if (order.pureWaterQty > 0) {
                ReceiptLine("Pure Water", order.pureWaterQty, order.pureWaterPrice, order.pureWaterTotal)
            }
            if (order.springWaterQty > 0) {
                ReceiptLine("Spring Water", order.springWaterQty, order.springWaterPrice, order.springWaterTotal)
            }
            if (order.mineralWaterQty > 0) {
                ReceiptLine("Mineral Water", order.mineralWaterQty, order.mineralWaterPrice, order.mineralWaterTotal)
            }

            if (order.waterSubtotal > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                ReceiptSummary("Subtotal", order.waterSubtotal)
            }

            if (order.deliveryFee > 0) {
                ReceiptSummary("Delivery Fee", order.deliveryFee)
            }

            if (order.transactionFee > 0) {
                ReceiptSummary("Transaction Fee", order.transactionFee)
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Total",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MidnightBlue,
                )
                Text(
                    text = "₱${"%.2f".format(order.grandTotal)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MidnightBlue,
                )
            }
        }
    }
}

@Composable
private fun ReceiptLine(label: String, qty: Int, price: Double, total: Double) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, fontSize = 14.sp, color = Color.DarkGray)
            Text(
                text = "₱${"%.2f".format(price)} x $qty",
                fontSize = 14.sp,
                color = Color.DarkGray,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = "₱${"%.2f".format(total)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MidnightBlue,
            )
        }
    }
}

@Composable
private fun ReceiptSummary(label: String, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, fontSize = 14.sp, color = Color.DarkGray)
        Text(
            text = "₱${"%.2f".format(amount)}",
            fontSize = 14.sp,
            color = Color.DarkGray,
        )
    }
}
