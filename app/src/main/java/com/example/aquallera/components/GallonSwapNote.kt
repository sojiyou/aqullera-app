package com.example.aquallera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Reminder shown on delivery orders that gallons are swapped one-for-one. */
@Composable
fun GallonSwapNote(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFDBEAFE), RoundedCornerShape(8.dp))
            .padding(12.dp),
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Note: ") }
                append(
                    "Please have your empty water gallons ready for pickup when the delivery truck " +
                        "arrives. Delivered gallons are swapped one-for-one with your empty ones — " +
                        "any gallon not returned will be charged."
                )
            },
            fontSize = 12.sp,
            color = Color.Gray,
            lineHeight = 17.sp,
        )
    }
}
