package com.example.aquallera.createorder.ui

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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aquallera.components.AqualleraInput
import com.example.aquallera.components.LoadingIndicator
import com.example.aquallera.components.PrimaryButton
import com.example.aquallera.core.theme.AppBg
import com.example.aquallera.core.theme.MidnightBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    viewModel: CreateOrderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Order", color = AppBg) },
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
                    if (state.station != null) {
                        Text(
                            text = state.station!!.stationName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MidnightBlue,
                            modifier = Modifier.padding(16.dp),
                        )
                    }

                    if (state.hasPureWater) {
                        ProductCard(
                            label = "Pure Water (Gallon)",
                            price = state.pureWaterPrice,
                            qty = state.pureWaterQty,
                            onQtyChange = { viewModel.onPureWaterQtyChanged(it) },
                        )
                    }
                    if (state.hasSpringWater) {
                        ProductCard(
                            label = "Spring Water (Gallon)",
                            price = state.springWaterPrice,
                            qty = state.springWaterQty,
                            onQtyChange = { viewModel.onSpringWaterQtyChanged(it) },
                        )
                    }
                    if (state.hasMineralWater) {
                        ProductCard(
                            label = "Mineral Water (Gallon)",
                            price = state.mineralWaterPrice,
                            qty = state.mineralWaterQty,
                            onQtyChange = { viewModel.onMineralWaterQtyChanged(it) },
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AqualleraInput(
                        label = "Delivery Address",
                        value = state.deliveryAddress,
                        onValueChange = { viewModel.onDeliveryAddressChanged(it) },
                        type = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        required = true,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AqualleraInput(
                        label = "Location Details",
                        value = state.locationDetails,
                        onValueChange = { viewModel.onLocationDetailsChanged(it) },
                        type = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AqualleraInput(
                        label = "Additional Details",
                        value = state.additionalDetails,
                        onValueChange = { viewModel.onAdditionalDetailsChanged(it) },
                        type = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        TotalRow("Subtotal", state.waterSubtotal)
                        TotalRow("Delivery Fee", state.deliveryFee)
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

                    Spacer(modifier = Modifier.height(16.dp))
                    PrimaryButton(
                        text = "Next",
                        onClick = {
                            if (viewModel.proceedToConfirmation()) onNext()
                        },
                        enabled = state.hasAnyItem && (state.deliveryAddress.isNotBlank() || state.locationDetails.isNotBlank()),
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
private fun ProductCard(
    label: String,
    price: Double,
    qty: Int,
    onQtyChange: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MidnightBlue)
                Text(text = "₱${"%.2f".format(price)}/gal", fontSize = 13.sp, color = Color.Gray)
            }
            QuantitySelector(qty = qty, onQtyChange = onQtyChange)
            Text(
                text = "₱${"%.2f".format(qty * price)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MidnightBlue,
                modifier = Modifier.width(70.dp).padding(start = 8.dp),
            )
        }
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
            Text(text = "−", color = AppBg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = "$qty",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MidnightBlue,
            modifier = Modifier.padding(horizontal = 12.dp),
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
private fun TotalRow(label: String, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, fontSize = 14.sp, color = Color.DarkGray)
        Text(text = "₱${"%.2f".format(amount)}", fontSize = 14.sp, color = Color.DarkGray)
    }
}
