package com.example.aquallera.createorder.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aquallera.components.PrimaryButton
import com.example.aquallera.components.ReceiptCard
import com.example.aquallera.core.theme.AppBg
import com.example.aquallera.core.theme.MidnightBlue
import com.example.aquallera.core.model.Order
import com.example.aquallera.core.util.TimeUtils

/** Review screen that writes the pending order and reports the new id. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderConfirmationScreen(
    onBack: () -> Unit,
    onOrderConfirmed: (orderId: String) -> Unit,
    viewModel: OrderConfirmationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val pendingOrder by viewModel.pendingOrder.collectAsState()

    LaunchedEffect(state.confirmed, state.orderId) {
        if (state.confirmed && state.orderId.isNotBlank()) {
            onOrderConfirmed(state.orderId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirm Order", color = AppBg) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppBg)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBlue),
            )
        },
    ) { padding ->
        if (pendingOrder.stationId.isBlank()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(text = "No order data available", color = androidx.compose.ui.graphics.Color.Gray, fontSize = 16.sp)
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = pendingOrder.stationName,
                modifier = Modifier.padding(16.dp),
                color = MidnightBlue,
            )

            val order = Order(
                stationName = pendingOrder.stationName,
                pureWaterQty = pendingOrder.pureWaterQty,
                springWaterQty = pendingOrder.springWaterQty,
                mineralWaterQty = pendingOrder.mineralWaterQty,
                pureWaterPrice = pendingOrder.pureWaterPrice,
                springWaterPrice = pendingOrder.springWaterPrice,
                mineralWaterPrice = pendingOrder.mineralWaterPrice,
                pureWaterTotal = pendingOrder.pureWaterQty * pendingOrder.pureWaterPrice,
                springWaterTotal = pendingOrder.springWaterQty * pendingOrder.springWaterPrice,
                mineralWaterTotal = pendingOrder.mineralWaterQty * pendingOrder.mineralWaterPrice,
                waterSubtotal = pendingOrder.waterSubtotal,
                deliveryFee = pendingOrder.deliveryFee,
                transactionFee = pendingOrder.transactionFee,
                grandTotal = pendingOrder.grandTotal,
            )

            ReceiptCard(order = order, modifier = Modifier.padding(horizontal = 16.dp))

            InfoCard(title = "Order Type") {
                Text(pendingOrder.orderType, fontSize = 14.sp, color = Color.DarkGray)
            }

            if (pendingOrder.preferredDate.isNotBlank()) {
                InfoCard(title = "Preferred Date") {
                    Text(pendingOrder.preferredDate, fontSize = 14.sp, color = Color.DarkGray)
                }
            }

            if (pendingOrder.preferredTime.isNotBlank()) {
                InfoCard(title = "Preferred Time") {
                    Text(TimeUtils.to12Hour(pendingOrder.preferredTime), fontSize = 14.sp, color = Color.DarkGray)
                }
            }

            if (pendingOrder.deliveryAddress.isNotBlank()) {
                InfoCard(title = "Delivery Address") {
                    Text(pendingOrder.deliveryAddress, fontSize = 14.sp, color = Color.DarkGray)
                    if (pendingOrder.additionalDetails.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(pendingOrder.additionalDetails, fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                text = if (state.confirming) "Confirming..." else "Confirm Order",
                onClick = { viewModel.confirmOrder() },
                loading = state.confirming,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                buttonWidth = 350,
            )
            if (state.error != null) {
                Text(
                    text = state.error ?: "",
                    color = androidx.compose.ui.graphics.Color.Red,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = AppBg),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MidnightBlue)
            Spacer(modifier = Modifier.height(2.dp))
            content()
        }
    }
}
