package com.example.aquallera.createorder.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aquallera.components.PrimaryButton
import com.example.aquallera.core.theme.AppBg
import com.example.aquallera.core.theme.MidnightBlue
import com.example.aquallera.core.theme.StatusCompleted

/** Post-order confirmation with the receipt summary and next-step buttons. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderSuccessScreen(
    orderId: String,
    onViewOrders: () -> Unit,
    onBackToMap: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Placed", color = AppBg) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBlue),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                modifier = Modifier.size(80.dp),
                tint = StatusCompleted,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Order Placed Successfully!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MidnightBlue,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (orderId.isNotBlank()) {
                Text(
                    text = "Order ID: $orderId",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            PrimaryButton(
                text = "View My Orders",
                onClick = onViewOrders,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                buttonWidth = 350,
            )
            Spacer(modifier = Modifier.height(12.dp))
            PrimaryButton(
                text = "Back to Map",
                onClick = onBackToMap,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                buttonWidth = 350,
            )
        }
    }
}
