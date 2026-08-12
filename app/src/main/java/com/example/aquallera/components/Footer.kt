package com.example.aquallera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aquallera.core.theme.FooterBg

/** Brand footer with contact details, shared by the landing and about screens. */
@Composable
fun Footer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FooterBg)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(
            text = "AQUA-LLERA",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Contact Us:",
            color = Color.White,
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "aquallera.main@gmail.com",
                color = Color.White,
                fontSize = 12.sp,
            )
            Text(
                text = "2600, Baguio City Philippines",
                color = Color.White,
                fontSize = 12.sp,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "0927-7263-218",
                color = Color.White,
                fontSize = 12.sp,
            )
            Text(
                text = "\u00A9 2025 Aqua-llera. All rights reserved.",
                color = Color.White,
                fontSize = 12.sp,
            )
        }
    }
}
